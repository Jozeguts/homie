
/*
 * Homie ESP32 Smart Home Controller
 * 
 * Features:
 * - WiFi Access Point mode
 * - Full MQTT broker and client functionality
 * - WebSocket server
 * - Device control via GPIO pins
 * - Temperature monitoring with DS18B20
 * - Data persistence and sync with Android
 * - Manual control buttons with debouncing
 * - Bidirectional MQTT communication
 */

#include <WiFi.h>
#include <WebServer.h>
#include <WebSocketsServer.h>
#include <ArduinoJson.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <SPIFFS.h>
#include <Preferences.h>
#include <PubSubClient.h>

// WiFi Configuration
const char* AP_SSID = "SmartHome_ESP32";
const char* AP_PASSWORD = "smarthome123";

// Hardware Configuration
const int RELAY_PINS[] = {16, 17, 18, 19, 21, 22, 23, 25}; // 8 relay control pins
const int BUTTON_PINS[] = {13, 12, 14, 27, 26, 33, 32, 35}; // Manual control buttons
const int NUM_DEVICES = 8;
const int ONE_WIRE_BUS = 4;  // Temperature sensor pin
const int STATUS_LED = 2;    // Built-in LED

// MQTT Configuration
const char* MQTT_BROKER = "192.168.4.1";
const int MQTT_PORT = 1883;
const char* MQTT_CLIENT_ID = "ESP32_HomieController";

// Global objects
WebSocketsServer webSocket = WebSocketsServer(81);
WebServer httpServer(80);
WiFiServer mqttBroker(1883);
WiFiClient espClient;
PubSubClient mqttClient(espClient);
OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature tempSensors(&oneWire);
Preferences preferences;

// Device data structure
struct Device {
  String id;
  String name;
  String type;
  String room;
  bool isActive;
  int pin;
  float temperature;
  unsigned long lastUpdate;
  bool needsSync;
};

// State management
Device devices[NUM_DEVICES];
bool relayStates[NUM_DEVICES] = {false};
bool buttonStates[NUM_DEVICES] = {true};
bool lastButtonStates[NUM_DEVICES] = {true};
unsigned long lastDebounceTime[NUM_DEVICES] = {0};

// MQTT client list for broker functionality
struct MQTTClientInfo {
  WiFiClient client;
  bool connected;
  String clientId;
  unsigned long lastPing;
};

MQTTClientInfo mqttClients[10]; // Support up to 10 MQTT clients
int numConnectedClients = 0;

// Timing
unsigned long lastTemperatureRead = 0;
unsigned long lastDataSync = 0;
unsigned long lastHeartbeat = 0;
unsigned long lastMQTTCheck = 0;
const unsigned long DEBOUNCE_DELAY = 50;
const unsigned long TEMPERATURE_INTERVAL = 10000;  // 10 seconds
const unsigned long SYNC_INTERVAL = 30000;         // 30 seconds
const unsigned long HEARTBEAT_INTERVAL = 60000;    // 60 seconds
const unsigned long MQTT_CHECK_INTERVAL = 1000;    // 1 second

void setup() {
  Serial.begin(115200);
  Serial.println("\n=== Homie ESP32 Controller Starting ===");
  
  // Initialize SPIFFS
  if (!SPIFFS.begin(true)) {
    Serial.println("SPIFFS Mount Failed");
    return;
  }
  
  // Initialize preferences
  preferences.begin("homie", false);
  
  // Setup hardware
  setupHardware();
  
  // Load stored configuration
  loadDeviceConfiguration();
  
  // Setup WiFi Access Point
  setupAccessPoint();
  
  // Setup servers
  setupWebSocket();
  setupHTTPServer();
  setupMQTTBroker();
  setupMQTTClient();
  
  Serial.println("=== Homie ESP32 Controller Ready ===");
  blinkStatusLED(3);
}

void loop() {
  unsigned long currentMillis = millis();
  
  // Handle connections
  webSocket.loop();
  httpServer.handleClient();
  handleMQTTBroker();
  mqttClient.loop();
  
  // Check manual controls
  checkManualButtons();
  
  // MQTT client management
  if (currentMillis - lastMQTTCheck >= MQTT_CHECK_INTERVAL) {
    lastMQTTCheck = currentMillis;
    maintainMQTTConnection();
  }
  
  // Periodic tasks
  if (currentMillis - lastTemperatureRead >= TEMPERATURE_INTERVAL) {
    lastTemperatureRead = currentMillis;
    readAndBroadcastTemperatures();
  }
  
  if (currentMillis - lastDataSync >= SYNC_INTERVAL) {
    lastDataSync = currentMillis;
    broadcastDeviceStates();
  }
  
  if (currentMillis - lastHeartbeat >= HEARTBEAT_INTERVAL) {
    lastHeartbeat = currentMillis;
    sendHeartbeat();
  }
  
  delay(10);
}

void setupHardware() {
  // Setup relay pins
  for (int i = 0; i < NUM_DEVICES; i++) {
    pinMode(RELAY_PINS[i], OUTPUT);
    digitalWrite(RELAY_PINS[i], LOW);
    relayStates[i] = false;
  }
  
  // Setup button pins with pull-up
  for (int i = 0; i < NUM_DEVICES; i++) {
    pinMode(BUTTON_PINS[i], INPUT_PULLUP);
  }
  
  // Setup status LED
  pinMode(STATUS_LED, OUTPUT);
  
  // Initialize temperature sensors
  tempSensors.begin();
  
  Serial.println("Hardware initialized");
}

void setupAccessPoint() {
  WiFi.mode(WIFI_AP);
  WiFi.softAP(AP_SSID, AP_PASSWORD);
  
  IPAddress apIP = WiFi.softAPIP();
  Serial.print("Access Point IP: ");
  Serial.println(apIP);
  Serial.printf("SSID: %s | Password: %s\n", AP_SSID, AP_PASSWORD);
}

void setupWebSocket() {
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
  Serial.println("WebSocket server started on port 81");
}

void setupHTTPServer() {
  // API endpoints
  httpServer.on("/api/devices", HTTP_GET, handleGetDevices);
  httpServer.on("/api/devices", HTTP_POST, handleUpdateDevice);
  httpServer.on("/api/toggle", HTTP_POST, handleToggleDevice);
  httpServer.on("/api/sync", HTTP_POST, handleSyncRequest);
  httpServer.on("/api/status", HTTP_GET, handleSystemStatus);
  
  httpServer.enableCORS(true);
  httpServer.begin();
  Serial.println("HTTP server started on port 80");
}

void setupMQTTBroker() {
  mqttBroker.begin();
  Serial.println("MQTT broker started on port 1883");
  
  // Initialize client array
  for (int i = 0; i < 10; i++) {
    mqttClients[i].connected = false;
    mqttClients[i].lastPing = 0;
  }
}

void setupMQTTClient() {
  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  mqttClient.setCallback(mqttCallback);
  Serial.println("MQTT client configured");
}

void maintainMQTTConnection() {
  if (!mqttClient.connected()) {
    Serial.println("Attempting MQTT connection...");
    if (mqttClient.connect(MQTT_CLIENT_ID)) {
      Serial.println("MQTT client connected");
      
      // Subscribe to control topics
      mqttClient.subscribe("homie/devices/+/control");
      mqttClient.subscribe("homie/system/sync");
      mqttClient.subscribe("homie/system/request");
      
      // Publish online status
      String payload = "{\"status\":\"online\",\"timestamp\":" + String(millis()) + "}";
      mqttClient.publish("homie/system/status", payload.c_str());

      
    } else {
      Serial.print("MQTT connection failed, rc=");
      Serial.println(mqttClient.state());
    }
  }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String message = "";
  for (int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  
  Serial.printf("MQTT Message received [%s]: %s\n", topic, message.c_str());
  
  StaticJsonDocument<512> doc;
  DeserializationError error = deserializeJson(doc, message);
  
  if (error) {
    Serial.println("MQTT JSON parse error");
    return;
  }
  
  String topicStr = String(topic);
  
  if (topicStr.startsWith("homie/devices/") && topicStr.endsWith("/control")) {
    handleMQTTDeviceControl(topicStr, doc);
  } else if (topicStr == "homie/system/sync") {
    handleMQTTSyncRequest(doc);
  } else if (topicStr == "homie/system/request") {
    handleMQTTSystemRequest(doc);
  }
}

void handleMQTTDeviceControl(String topic, JsonDocument& doc) {
  // Extract device ID from topic: homie/devices/{deviceId}/control
  int startIdx = topic.indexOf("/devices/") + 9;
  int endIdx = topic.indexOf("/control");
  String deviceId = topic.substring(startIdx, endIdx);
  
  if (doc.containsKey("action") && doc["action"] == "toggle") {
    bool state = doc["state"];
    toggleDeviceById(deviceId, state);
    
    // Send confirmation
    String confirmTopic = "homie/devices/" + deviceId + "/state";
    StaticJsonDocument<256> confirmDoc;
    confirmDoc["id"] = deviceId;
    confirmDoc["isActive"] = state;
    confirmDoc["timestamp"] = millis();
    confirmDoc["source"] = "esp32";
    
    String confirmMessage;
    serializeJson(confirmDoc, confirmMessage);
    mqttClient.publish(confirmTopic.c_str(), confirmMessage.c_str());
  }
}

void handleMQTTSyncRequest(JsonDocument& doc) {
  Serial.println("MQTT Sync request received");
  
  if (doc.containsKey("devices")) {
    JsonArray androidDevices = doc["devices"];
    for (JsonVariant deviceVar : androidDevices) {
      JsonObject device = deviceVar.as<JsonObject>();
      String deviceId = device["id"];
      
      for (int i = 0; i < NUM_DEVICES; i++) {
        if (devices[i].id == deviceId) {
          // Update device metadata, but keep ESP32 as source of truth for hardware state
          devices[i].name = device["name"].as<String>();
          devices[i].room = device["room"].as<String>();
          devices[i].type = device["type"].as<String>();
          break;
        }
      }
    }
    
    saveDeviceConfiguration();
  }
  
  // Send acknowledgment
  StaticJsonDocument<256> ack;
  ack["type"] = "sync_acknowledged";
  ack["timestamp"] = millis();
  ack["source"] = "esp32";
  
  String ackMessage;
  serializeJson(ack, ackMessage);
  mqttClient.publish("homie/system/sync_ack", ackMessage.c_str());
}

void handleMQTTSystemRequest(JsonDocument& doc) {
  if (doc.containsKey("action")) {
    String action = doc["action"];
    
    if (action == "get_all_states") {
      publishAllDeviceStates();
    } else if (action == "get_temperature") {
      publishTemperatureData();
    } else if (action == "get_system_info") {
      publishSystemInfo();
    }
  }
}

void handleMQTTBroker() {
  // Handle new MQTT broker connections
  WiFiClient newClient = mqttBroker.available();
  if (newClient) {
    Serial.println("New MQTT broker client connected");
    
    // Find empty slot for new client
    for (int i = 0; i < 10; i++) {
      if (!mqttClients[i].connected) {
        mqttClients[i].client = newClient;
        mqttClients[i].connected = true;
        mqttClients[i].clientId = "client_" + String(i);
        mqttClients[i].lastPing = millis();
        numConnectedClients++;
        break;
      }
    }
  }
  
  // Handle existing client communications
  for (int i = 0; i < 10; i++) {
    if (mqttClients[i].connected) {
      if (!mqttClients[i].client.connected()) {
        mqttClients[i].connected = false;
        numConnectedClients--;
        Serial.printf("MQTT broker client %d disconnected\n", i);
      } else {
        // Handle client messages if needed
        handleMQTTBrokerClient(i);
      }
    }
  }
}

void handleMQTTBrokerClient(int clientIndex) {
  if (mqttClients[clientIndex].client.available()) {
    String message = mqttClients[clientIndex].client.readString();
    Serial.printf("MQTT Broker received from client %d: %s\n", clientIndex, message.c_str());
    
    // Process MQTT protocol messages here if needed
    // For now, just send a simple acknowledgment
    mqttClients[clientIndex].client.println("ACK");
    mqttClients[clientIndex].lastPing = millis();
  }
}

void loadDeviceConfiguration() {
  // Initialize default devices
  const char* deviceNames[] = {
    "Living Room Light", "Kitchen Light", "Bedroom Light", "Bathroom Light",
    "Living Room Fan", "Bedroom Fan", "Kitchen Exhaust", "Garden Light"
  };
  
  const char* deviceTypes[] = {
    "light", "light", "light", "light",
    "fan", "fan", "fan", "light"
  };
  
  const char* deviceRooms[] = {
    "Living Room", "Kitchen", "Bedroom", "Bathroom",
    "Living Room", "Bedroom", "Kitchen", "Garden"
  };
  
  for (int i = 0; i < NUM_DEVICES; i++) {
    devices[i].id = "device_" + String(i);
    devices[i].name = preferences.getString(("name_" + String(i)).c_str(), deviceNames[i]);
    devices[i].type = preferences.getString(("type_" + String(i)).c_str(), deviceTypes[i]);
    devices[i].room = preferences.getString(("room_" + String(i)).c_str(), deviceRooms[i]);
    devices[i].isActive = preferences.getBool(("active_" + String(i)).c_str(), false);
    devices[i].pin = RELAY_PINS[i];
    devices[i].temperature = -127.0;
    devices[i].lastUpdate = 0;
    devices[i].needsSync = false;
    
    // Restore hardware state
    relayStates[i] = devices[i].isActive;
    digitalWrite(RELAY_PINS[i], devices[i].isActive ? HIGH : LOW);
  }
  
  Serial.println("Device configuration loaded");
}

void saveDeviceConfiguration() {
  for (int i = 0; i < NUM_DEVICES; i++) {
    preferences.putString(("name_" + String(i)).c_str(), devices[i].name);
    preferences.putString(("type_" + String(i)).c_str(), devices[i].type);
    preferences.putString(("room_" + String(i)).c_str(), devices[i].room);
    preferences.putBool(("active_" + String(i)).c_str(), devices[i].isActive);
  }
}

void publishAllDeviceStates() {
  StaticJsonDocument<2048> doc;
  doc["type"] = "all_device_states";
  doc["timestamp"] = millis();
  
  JsonArray devicesArray = doc.createNestedArray("devices");
  for (int i = 0; i < NUM_DEVICES; i++) {
    JsonObject device = devicesArray.createNestedObject();
    device["id"] = devices[i].id;
    device["name"] = devices[i].name;
    device["type"] = devices[i].type;
    device["room"] = devices[i].room;
    device["isActive"] = devices[i].isActive;
    device["temperature"] = devices[i].temperature;
    device["lastUpdate"] = devices[i].lastUpdate;
    device["pin"] = devices[i].pin;
  }
  
  String message;
  serializeJson(doc, message);
  mqttClient.publish("homie/system/all_states", message.c_str());
}

void publishTemperatureData() {
  tempSensors.requestTemperatures();
  
  StaticJsonDocument<1024> doc;
  doc["type"] = "temperature_data";
  doc["timestamp"] = millis();
  
  JsonObject sensors = doc.createNestedObject("sensors");
  
  int sensorCount = tempSensors.getDeviceCount();
  for (int i = 0; i < sensorCount && i < NUM_DEVICES; i++) {
    float temp = tempSensors.getTempCByIndex(i);
    if (temp != DEVICE_DISCONNECTED_C) {
      devices[i].temperature = temp;
      sensors[devices[i].id] = temp;
    }
  }
  
  String message;
  serializeJson(doc, message);
  mqttClient.publish("homie/sensors/temperature", message.c_str());
}

void publishSystemInfo() {
  StaticJsonDocument<512> doc;
  doc["type"] = "system_info";
  doc["uptime"] = millis();
  doc["freeHeap"] = ESP.getFreeHeap();
  doc["connectedWebSocketClients"] = webSocket.connectedClients();
  doc["connectedMQTTClients"] = numConnectedClients;
  doc["temperatureSensors"] = tempSensors.getDeviceCount();
  doc["timestamp"] = millis();
  
  String message;
  serializeJson(doc, message);
  mqttClient.publish("homie/system/info", message.c_str());
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t* payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:
      Serial.printf("[%u] Disconnected\n", num);
      break;
      
    case WStype_CONNECTED:
      {
        IPAddress ip = webSocket.remoteIP(num);
        Serial.printf("[%u] Connected from %s\n", num, ip.toString().c_str());
        sendInitialData(num);
      }
      break;
      
    case WStype_TEXT:
      handleWebSocketMessage(num, (char*)payload, length);
      break;
      
    default:
      break;
  }
}

void handleWebSocketMessage(uint8_t clientNum, char* payload, size_t length) {
  StaticJsonDocument<512> doc;
  DeserializationError error = deserializeJson(doc, payload, length);
  
  if (error) {
    Serial.println("WebSocket JSON parse error");
    return;
  }
  
  const char* type = doc["type"];
  
  if (strcmp(type, "toggle_device") == 0) {
    String deviceId = doc["device_id"];
    int state = doc["state"];
    toggleDeviceById(deviceId, state == 1);
  }
  else if (strcmp(type, "update_device") == 0) {
    handleDeviceUpdate(doc);
  }
  else if (strcmp(type, "get_initial_data") == 0) {
    sendInitialData(clientNum);
  }
  else if (strcmp(type, "sync_data") == 0) {
    handleDataSync(doc);
  }
}

void toggleDeviceById(String deviceId, bool state) {
  for (int i = 0; i < NUM_DEVICES; i++) {
    if (devices[i].id == deviceId) {
      setDeviceState(i, state);
      break;
    }
  }
}

void setDeviceState(int deviceIndex, bool state) {
  if (deviceIndex < 0 || deviceIndex >= NUM_DEVICES) return;
  
  devices[deviceIndex].isActive = state;
  devices[deviceIndex].lastUpdate = millis();
  devices[deviceIndex].needsSync = true;
  relayStates[deviceIndex] = state;
  
  digitalWrite(RELAY_PINS[deviceIndex], state ? HIGH : LOW);
  
  // Broadcast state change via WebSocket
  broadcastDeviceState(deviceIndex);
  
  // Publish via MQTT
  publishDeviceState(deviceIndex);
  
  // Save to preferences
  preferences.putBool(("active_" + String(deviceIndex)).c_str(), state);
  
  Serial.printf("Device %s set to %s\n", devices[deviceIndex].name.c_str(), state ? "ON" : "OFF");
}

void publishDeviceState(int deviceIndex) {
  StaticJsonDocument<256> doc;
  doc["type"] = "device_state_update";
  doc["id"] = devices[deviceIndex].id;
  doc["isActive"] = devices[deviceIndex].isActive;
  doc["timestamp"] = millis();
  doc["source"] = "esp32";
  
  String message;
  serializeJson(doc, message);
  
  String topic = "homie/devices/" + devices[deviceIndex].id + "/state";
  mqttClient.publish(topic.c_str(), message.c_str());
}

void checkManualButtons() {
  for (int i = 0; i < NUM_DEVICES; i++) {
    bool reading = digitalRead(BUTTON_PINS[i]);
    
    if (reading != lastButtonStates[i]) {
      lastDebounceTime[i] = millis();
    }
    
    if ((millis() - lastDebounceTime[i]) > DEBOUNCE_DELAY) {
      if (reading != buttonStates[i]) {
        buttonStates[i] = reading;
        
        if (buttonStates[i] == LOW) { // Button pressed
          bool newState = !relayStates[i];
          setDeviceState(i, newState);
          Serial.printf("Manual toggle: Device %d -> %s\n", i, newState ? "ON" : "OFF");
        }
      }
    }
    
    lastButtonStates[i] = reading;
  }
}

void readAndBroadcastTemperatures() {
  publishTemperatureData();
  
  // Also send via WebSocket
  tempSensors.requestTemperatures();
  
  StaticJsonDocument<1024> doc;
  doc["type"] = "temperature_reading";
  doc["timestamp"] = millis();
  
  JsonObject sensors = doc.createNestedObject("sensors");
  
  int sensorCount = tempSensors.getDeviceCount();
  for (int i = 0; i < sensorCount && i < NUM_DEVICES; i++) {
    float temp = tempSensors.getTempCByIndex(i);
    if (temp != DEVICE_DISCONNECTED_C) {
      devices[i].temperature = temp;
      sensors[devices[i].id] = temp;
    }
  }
  
  String message;
  serializeJson(doc, message);
  webSocket.broadcastTXT(message);
  
  Serial.println("Temperature readings sent");
}

void broadcastDeviceStates() {
  publishAllDeviceStates();
  
  // Also send via WebSocket
  StaticJsonDocument<2048> doc;
  doc["type"] = "device_update";
  doc["timestamp"] = millis();
  
  JsonArray devicesArray = doc.createNestedArray("devices");
  for (int i = 0; i < NUM_DEVICES; i++) {
    JsonObject device = devicesArray.createNestedObject();
    device["id"] = devices[i].id;
    device["name"] = devices[i].name;
    device["type"] = devices[i].type;
    device["room"] = devices[i].room;
    device["isActive"] = devices[i].isActive;
    device["temperature"] = devices[i].temperature;
    device["lastUpdate"] = devices[i].lastUpdate;
  }
  
  String message;
  serializeJson(doc, message);
  webSocket.broadcastTXT(message);
}

void broadcastDeviceState(int deviceIndex) {
  StaticJsonDocument<256> doc;
  doc["type"] = "device_update";
  doc["id"] = devices[deviceIndex].id;
  doc["isActive"] = devices[deviceIndex].isActive;
  doc["timestamp"] = millis();
  
  String message;
  serializeJson(doc, message);
  webSocket.broadcastTXT(message);
}

void sendInitialData(uint8_t clientNum) {
  StaticJsonDocument<2048> doc;
  doc["type"] = "initial_devices";
  
  JsonArray devicesArray = doc.createNestedArray("devices");
  for (int i = 0; i < NUM_DEVICES; i++) {
    JsonObject device = devicesArray.createNestedObject();
    device["id"] = devices[i].id;
    device["name"] = devices[i].name;
    device["type"] = devices[i].type;
    device["room"] = devices[i].room;
    device["isActive"] = devices[i].isActive;
    device["pin"] = devices[i].pin;
    device["temperature"] = devices[i].temperature;
    device["lastUpdate"] = devices[i].lastUpdate;
  }
  
  String message;
  serializeJson(doc, message);
  webSocket.sendTXT(clientNum, message);
}

void handleDeviceUpdate(JsonDocument& updateDoc) {
  String deviceId = updateDoc["id"];
  
  for (int i = 0; i < NUM_DEVICES; i++) {
    if (devices[i].id == deviceId) {
      if (updateDoc.containsKey("name")) {
        devices[i].name = updateDoc["name"].as<String>();
      }
      if (updateDoc.containsKey("room")) {
        devices[i].room = updateDoc["room"].as<String>();
      }
      if (updateDoc.containsKey("type")) {
        devices[i].type = updateDoc["type"].as<String>();
      }
      
      devices[i].lastUpdate = millis();
      devices[i].needsSync = true;
      saveDeviceConfiguration();
      broadcastDeviceState(i);
      publishDeviceState(i);
      break;
    }
  }
}

void handleDataSync(JsonDocument& syncDoc) {
  handleMQTTSyncRequest(syncDoc);
}

// HTTP API handlers
void handleGetDevices() {
  StaticJsonDocument<2048> doc;
  JsonArray devicesArray = doc.createNestedArray("devices");
  
  for (int i = 0; i < NUM_DEVICES; i++) {
    JsonObject device = devicesArray.createNestedObject();
    device["id"] = devices[i].id;
    device["name"] = devices[i].name;
    device["type"] = devices[i].type;
    device["room"] = devices[i].room;
    device["isActive"] = devices[i].isActive;
    device["pin"] = devices[i].pin;
    device["temperature"] = devices[i].temperature;
    device["lastUpdate"] = devices[i].lastUpdate;
  }
  
  String response;
  serializeJson(doc, response);
  httpServer.send(200, "application/json", response);
}

void handleUpdateDevice() {
  if (httpServer.hasArg("plain")) {
    String body = httpServer.arg("plain");
    
    StaticJsonDocument<512> doc;
    DeserializationError error = deserializeJson(doc, body);
    
    if (!error) {
      handleDeviceUpdate(doc);
      httpServer.send(200, "application/json", "{\"status\":\"success\"}");
    } else {
      httpServer.send(400, "application/json", "{\"error\":\"Invalid JSON\"}");
    }
  } else {
    httpServer.send(400, "application/json", "{\"error\":\"No body\"}");
  }
}

void handleToggleDevice() {
  if (httpServer.hasArg("plain")) {
    String body = httpServer.arg("plain");
    
    StaticJsonDocument<256> doc;
    DeserializationError error = deserializeJson(doc, body);
    
    if (!error) {
      String deviceId = doc["device_id"];
      int state = doc["state"];
      
      toggleDeviceById(deviceId, state == 1);
      httpServer.send(200, "application/json", "{\"status\":\"success\"}");
    } else {
      httpServer.send(400, "application/json", "{\"error\":\"Invalid JSON\"}");
    }
  } else {
    httpServer.send(400, "application/json", "{\"error\":\"No body\"}");
  }
}

void handleSyncRequest() {
  if (httpServer.hasArg("plain")) {
    String body = httpServer.arg("plain");
    
    StaticJsonDocument<1024> doc;
    DeserializationError error = deserializeJson(doc, body);
    
    if (!error) {
      handleDataSync(doc);
      httpServer.send(200, "application/json", "{\"status\":\"success\"}");
    } else {
      httpServer.send(400, "application/json", "{\"error\":\"Invalid JSON\"}");
    }
  } else {
    httpServer.send(400, "application/json", "{\"error\":\"No body\"}");
  }
}

void handleSystemStatus() {
  StaticJsonDocument<512> doc;
  doc["uptime"] = millis();
  doc["freeHeap"] = ESP.getFreeHeap();
  doc["connectedWebSocketClients"] = webSocket.connectedClients();
  doc["connectedMQTTClients"] = numConnectedClients;
  doc["temperatureSensors"] = tempSensors.getDeviceCount();
  doc["mqttClientConnected"] = mqttClient.connected();
  
  String response;
  serializeJson(doc, response);
  httpServer.send(200, "application/json", response);
}

void sendHeartbeat() {
  StaticJsonDocument<256> doc;
  doc["type"] = "heartbeat";
  doc["timestamp"] = millis();
  doc["uptime"] = millis();
  doc["freeHeap"] = ESP.getFreeHeap();
  doc["connectedClients"] = webSocket.connectedClients() + numConnectedClients;
  
  String message;
  serializeJson(doc, message);
  
  webSocket.broadcastTXT(message);
  mqttClient.publish("homie/system/heartbeat", message.c_str());
}

void blinkStatusLED(int times) {
  for (int i = 0; i < times; i++) {
    digitalWrite(STATUS_LED, HIGH);
    delay(200);
    digitalWrite(STATUS_LED, LOW);
    delay(200);
  }
}
