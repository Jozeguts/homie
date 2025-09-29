/*
 * Homie ESP32 Smart Home Controller - WebSocket Only
 * 
 * Features:
 * - WiFi Access Point at 192.168.4.1
 * - WebSocket server on port 81
 * - HTTP server on port 80 (optional, can be removed)
 * - Relay control via WebSocket
 * - DHT11 temperature/humidity monitoring (on pin 4)
 * - Device config persistence
 * - Manual override buttons (debounced)
 * - Status LED feedback
 * - Real-time updates to all WebSocket clients
 */
/*
 * Homie ESP32 Smart Home Controller - WebSocket Only
 * 
 * Extra logging for debugging and traceability
 */

#include <WiFi.h>
#include <WebServer.h>
#include <WebSocketsServer.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <SPIFFS.h>
#include <Preferences.h>

// === CUSTOM PIN MAPPING ===
#define DHTPIN 4
const int DHTTYPE = DHT11;

// Relays
const int RELAY_SECURITY_LIGHT      = 5;
const int RELAY_LIVING_ROOM_LIGHT   = 18;
const int RELAY_KITCHEN_LIGHT       = 19;
const int RELAY_BEDROOM_LIGHT       = 21;
const int RELAY_LIVING_ROOM_FAN     = 22;

// Unused relays (for expansion)
const int RELAY_UNUSED_1            = 16;
const int RELAY_UNUSED_2            = 17;
const int RELAY_UNUSED_3            = 23;

// Buttons (active low, pull-up enabled)
const int BUTTON_SECURITY_LIGHT     = 13;
const int BUTTON_LIVING_ROOM_LIGHT  = 12;
const int BUTTON_KITCHEN_LIGHT      = 14;
const int BUTTON_BEDROOM_LIGHT      = 27;
const int BUTTON_LIVING_ROOM_FAN    = 26;

// Other hardware
const int STATUS_LED                = 2;
const int NUM_DEVICES               = 8;

// WiFi Configuration
const char* AP_SSID = "SmartHome_ESP32";
const char* AP_PASSWORD = "smarthome123";

// Pin Arrays
const int RELAY_PINS[NUM_DEVICES] = {
  RELAY_SECURITY_LIGHT,
  RELAY_LIVING_ROOM_LIGHT,
  RELAY_KITCHEN_LIGHT,
  RELAY_BEDROOM_LIGHT,
  RELAY_LIVING_ROOM_FAN,
  RELAY_UNUSED_1,
  RELAY_UNUSED_2,
  RELAY_UNUSED_3
};

const int BUTTON_PINS[NUM_DEVICES] = {
  BUTTON_SECURITY_LIGHT,
  BUTTON_LIVING_ROOM_LIGHT,
  BUTTON_KITCHEN_LIGHT,
  BUTTON_BEDROOM_LIGHT,
  BUTTON_LIVING_ROOM_FAN,
  -1,
  -1,
  -1
};

// Global Objects
DHT dht(DHTPIN, DHTTYPE);
WebSocketsServer webSocket(81);
WebServer httpServer(80);
Preferences preferences;

// Device Structure
struct Device {
  String id;
  String name;
  String type;
  String room;
  bool isActive;
  int pin;
  float temperature;
  float humidity;
  unsigned long lastUpdate;
};

Device devices[NUM_DEVICES];

// Button Debounce
bool buttonStates[NUM_DEVICES] = {true};
bool lastButtonStates[NUM_DEVICES] = {true};
unsigned long lastDebounceTime[NUM_DEVICES] = {0};

// Timing Variables
unsigned long lastDHTRead = 0;
unsigned long lastDataSync = 0;
unsigned long lastHeartbeat = 0;
unsigned long lastPerformance = 0;

// Intervals (ms)
const unsigned long DEBOUNCE_DELAY = 50;
const unsigned long DHT_INTERVAL = 10000;
const unsigned long SYNC_INTERVAL = 30000;
const unsigned long HEARTBEAT_INTERVAL = 60000;
const unsigned long PERFORMANCE_INTERVAL = 45000;


void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n=== Homie ESP32 Controller Boot ===");
  Serial.printf("Firmware v1.0 built on %s %s\n", __DATE__, __TIME__);

  if (!SPIFFS.begin(true)) {
    Serial.println("[ERROR] SPIFFS mount failed");
  } else {
    Serial.println("SPIFFS mounted successfully");
  }

  preferences.begin("homie", false);
  Serial.println("Preferences storage ready");

  setupHardware();
  loadDeviceConfiguration();

  Serial.println("Starting Access Point...");
  setupAccessPoint();

  Serial.println("Starting WebSocket server...");
  setupWebSocket();

  Serial.println("Starting HTTP server...");
  setupHTTPServer();

  Serial.println("=== Homie ESP32 Ready ===");
  blinkStatusLED(3);
  logAllDeviceStates();
}


void loop() {
  unsigned long currentMillis = millis();

  webSocket.loop();
  httpServer.handleClient();

  handleButtons();

  if (currentMillis - lastDHTRead >= DHT_INTERVAL) {
    lastDHTRead = currentMillis;
    Serial.println("[SENSOR] Reading DHT11...");
    float temp = dht.readTemperature();
    float hum = dht.readHumidity();
    
    if (!isnan(temp) && !isnan(hum)) {
      devices[0].temperature = temp;
      devices[0].humidity = hum;
      devices[0].lastUpdate = currentMillis;
      sendSensorUpdate("dht11_sensor", temp, hum);
      Serial.printf("[SENSOR] DHT11 T=%.1f°C H=%.1f%%\n", temp, hum);
    } else {
      Serial.println("[SENSOR] Failed to read from DHT11");
    }
  }

  if (currentMillis - lastDataSync >= SYNC_INTERVAL) {
    lastDataSync = currentMillis;
    Serial.println("[SYNC] Sending all device states");
    sendAllDeviceStates();
  }

  if (currentMillis - lastHeartbeat >= HEARTBEAT_INTERVAL) {
    lastHeartbeat = currentMillis;
    Serial.println("[HEARTBEAT] Sending heartbeat");
    sendHeartbeat();
  }

  if (currentMillis - lastPerformance >= PERFORMANCE_INTERVAL) {
    lastPerformance = currentMillis;
    Serial.println("[PERFORMANCE] Sending system metrics");
    sendPerformance();
  }

  delay(10);
}


void setupHardware() {
  Serial.println("Initializing hardware...");
  for (int i = 0; i < NUM_DEVICES; i++) {
    pinMode(RELAY_PINS[i], OUTPUT);
    digitalWrite(RELAY_PINS[i], devices[i].isActive ? LOW : HIGH);
    Serial.printf("Relay %d initialized at pin %d, state=%s\n", i, RELAY_PINS[i], devices[i].isActive ? "OFF" : "ON");

    if (BUTTON_PINS[i] != -1) {
      pinMode(BUTTON_PINS[i], INPUT_PULLUP);
      Serial.printf("Button for device %d initialized at pin %d\n", i, BUTTON_PINS[i]);
    }
  }
  pinMode(STATUS_LED, OUTPUT);
  dht.begin();
  Serial.println("Hardware initialization complete");
}


void setupAccessPoint() {
  WiFi.softAP(AP_SSID, AP_PASSWORD);
  IPAddress apIP(192, 168, 4, 1);
  WiFi.softAPConfig(apIP, apIP, IPAddress(255, 255, 255, 0));
  Serial.printf("Access Point active. SSID=%s, Password=%s, IP=%s\n", 
                AP_SSID, AP_PASSWORD, WiFi.softAPIP().toString().c_str());
}


void setupWebSocket() {
  webSocket.begin();
  webSocket.onEvent([](uint8_t clientNum, WStype_t type, uint8_t* payload, size_t length) {
    if (type == WStype_CONNECTED) {
      Serial.printf("[WS] Client %u connected\n", clientNum);
      sendInitialData(clientNum);
    } else if (type == WStype_TEXT) {
      Serial.printf("[WS] Message from client %u: %s\n", clientNum, payload);
      handleWebSocketMessage(clientNum, (char*)payload, length);
    } else if (type == WStype_DISCONNECTED) {
      Serial.printf("[WS] Client %u disconnected\n", clientNum);
    }
  });
  Serial.println("WebSocket server running on port 81");
}


void setupHTTPServer() {
  httpServer.on("/api/devices", HTTP_GET, []() {
    Serial.println("[HTTP] GET /api/devices request");
    DynamicJsonDocument doc(2048);
    JsonArray arr = doc.createNestedArray("devices");
    for (int i = 0; i < NUM_DEVICES; i++) {
      JsonObject dev = arr.createNestedObject();
      dev["id"] = devices[i].id;
      dev["name"] = devices[i].name;
      dev["type"] = devices[i].type;
      dev["room"] = devices[i].room;
      dev["isActive"] = devices[i].isActive;
      dev["pin"] = devices[i].pin;
      dev["temperature"] = devices[i].temperature;
      dev["humidity"] = devices[i].humidity;
      dev["lastUpdate"] = devices[i].lastUpdate;
    }
    String res;
    serializeJson(doc, res);
    httpServer.send(200, "application/json", res);
    Serial.println("[HTTP] Device list sent");
  });

  httpServer.enableCORS(true);
  httpServer.begin();
  Serial.println("HTTP server running on port 80");
}


void handleWebSocketMessage(uint8_t clientNum, char* payload, size_t length) {
  StaticJsonDocument<512> doc;
  DeserializationError error = deserializeJson(doc, payload, length);
  if (error) {
    Serial.printf("[WS] JSON parse error: %s\n", error.c_str());
    return;
  }

  const char* type = doc["type"];
  Serial.printf("[WS] Handling type: %s\n", type);

  if (strcmp(type, "toggle_device") == 0) {
    String deviceId = doc["device_id"];
    bool state = doc["state"];
    Serial.printf("[WS] Toggle request for %s -> %s\n", deviceId.c_str(), state ? "ON" : "OFF");
    toggleDeviceById(deviceId, state);
  } 
  else if (strcmp(type, "update_device") == 0) {
    Serial.println("[WS] Device update request");
    JsonObject obj = doc.as<JsonObject>();
    handleDeviceUpdate(obj);
  } 
  else if (strcmp(type, "get_initial_data") == 0) {
    Serial.printf("[WS] Initial data request from client %u\n", clientNum);
    sendInitialData(clientNum);
  } 
  else if (strcmp(type, "sync_request") == 0) {
    Serial.printf("[WS] Sync request from client %u\n", clientNum);
    sendAllDeviceStates();
  }
}


void toggleDeviceById(String deviceId, bool state) {
  for (int i = 0; i < NUM_DEVICES; i++) {
    if (devices[i].id == deviceId) {
      setDeviceState(i, state);
      sendDeviceStateUpdate(i);
      Serial.printf("[DEVICE] %s toggled -> %s\n", devices[i].name.c_str(), state ? "ON" : "OFF");
      return;
    }
  }
  Serial.printf("[DEVICE] Unknown device ID: %s\n", deviceId.c_str());
}


void setDeviceState(int i, bool state) {
  devices[i].isActive = state;
  devices[i].lastUpdate = millis();
  digitalWrite(RELAY_PINS[i], state ? HIGH : LOW);
  saveDeviceConfiguration();
  Serial.printf("[DEVICE] State updated: %s (%s) -> %s\n", devices[i].name.c_str(), devices[i].id.c_str(), state ? "ON" : "OFF");
}


void sendDeviceStateUpdate(int i) {
  StaticJsonDocument<256> doc;
  doc["type"] = "device_state";
  doc["id"] = devices[i].id;
  doc["isActive"] = devices[i].isActive;
  doc["timestamp"] = millis();

  String payload;
  serializeJson(doc, payload);
  webSocket.broadcastTXT(payload);
  Serial.printf("[WS] Broadcast device state for %s\n", devices[i].id.c_str());
}


void sendSensorUpdate(String sensorId, float temp, float hum) {
  StaticJsonDocument<192> doc;
  doc["type"] = "sensor_update";
  doc["id"] = sensorId;
  doc["temperature"] = temp;
  doc["humidity"] = hum;
  doc["timestamp"] = millis();

  String payload;
  serializeJson(doc, payload);
  webSocket.broadcastTXT(payload);
  Serial.printf("[WS] Broadcast sensor update %s T=%.1f H=%.1f\n", sensorId.c_str(), temp, hum);
}


void sendInitialData(uint8_t clientNum) {
  DynamicJsonDocument doc(2048);
  doc["type"] = "initial_devices";
  JsonArray arr = doc.createNestedArray("devices");
  for (int i = 0; i < NUM_DEVICES; i++) {
    JsonObject dev = arr.createNestedObject();
    dev["id"] = devices[i].id;
    dev["name"] = devices[i].name;
    dev["type"] = devices[i].type;
    dev["room"] = devices[i].room;
    dev["isActive"] = devices[i].isActive;
    dev["pin"] = devices[i].pin;
    dev["temperature"] = devices[i].temperature;
    dev["humidity"] = devices[i].humidity;
    dev["lastUpdate"] = devices[i].lastUpdate;
  }
  doc["timestamp"] = millis();

  String payload;
  serializeJson(doc, payload);
  webSocket.sendTXT(clientNum, payload);
  Serial.printf("[WS] Sent initial data to client %u\n", clientNum);
}


void sendAllDeviceStates() {
  for (int i = 0; i < NUM_DEVICES; i++) {
    sendDeviceStateUpdate(i);
  }
  Serial.println("[SYNC] All device states broadcasted");
}


void handleDeviceUpdate(JsonObject doc) {
  String deviceId = doc["device_id"];
  const char* name = doc["name"];
  const char* type = doc["type"];
  const char* room = doc["room"];
  Serial.printf("[DEVICE] Update request for %s\n", deviceId.c_str());

  for (int i = 0; i < NUM_DEVICES; i++) {
    if (devices[i].id == deviceId) {
      devices[i].name = String(name);
      devices[i].type = String(type);
      devices[i].room = String(room);
      devices[i].lastUpdate = millis();
      saveDeviceConfiguration();
      sendDeviceStateUpdate(i);
      Serial.printf("[DEVICE] Updated metadata for %s\n", devices[i].id.c_str());
      return;
    }
  }
  Serial.printf("[DEVICE] Unknown device ID in update: %s\n", deviceId.c_str());
}


void saveDeviceConfiguration() {
  for (int i = 0; i < NUM_DEVICES; i++) {
    preferences.putString(("name_" + String(i)).c_str(), devices[i].name);
    preferences.putString(("type_" + String(i)).c_str(), devices[i].type);
    preferences.putString(("room_" + String(i)).c_str(), devices[i].room);
    preferences.putBool(("active_" + String(i)).c_str(), devices[i].isActive);
  }
  Serial.println("[PREF] Device configuration saved");
}


void loadDeviceConfiguration() {
  const char* names[] = {
    "Security Light", "Living Room Light", "Kitchen Light", "Bedroom Light",
    "Living Room Fan", "Fan 1", "Exhaust Fan", "Garden Lights"
  };
  const char* types[] = {"light", "light", "light", "light", "fan", "fan", "fan", "light"};
  const char* rooms[] = {"Outside", "Living Room", "Kitchen", "Bedroom", "Living Room", "Living", "Kitchen", "Garden"};

  for (int i = 0; i < NUM_DEVICES; i++) {
    devices[i].id = "device_" + String(i);
    devices[i].name = preferences.getString(("name_" + String(i)).c_str(), names[i]);
    devices[i].type = preferences.getString(("type_" + String(i)).c_str(), types[i]);
    devices[i].room = preferences.getString(("room_" + String(i)).c_str(), rooms[i]);
    devices[i].isActive = preferences.getBool(("active_" + String(i)).c_str(), false);
    devices[i].pin = RELAY_PINS[i];
    devices[i].temperature = -127.0;
    devices[i].humidity = -1.0;
    devices[i].lastUpdate = 0;
    Serial.printf("[PREF] Loaded device %d: %s (%s) in %s, active=%s\n", 
                  i, devices[i].name.c_str(), devices[i].type.c_str(), devices[i].room.c_str(),
                  devices[i].isActive ? "true" : "false");
  }
}


void sendHeartbeat() {
  StaticJsonDocument<128> doc;
  doc["type"] = "heartbeat";
  doc["timestamp"] = millis();
  doc["source"] = "esp32";

  String payload;
  serializeJson(doc, payload);
  webSocket.broadcastTXT(payload);
  Serial.println("[WS] Broadcast heartbeat");
}


void sendPerformance() {
  StaticJsonDocument<256> doc;
  doc["type"] = "performance";
  doc["free_heap"] = ESP.getFreeHeap();
  doc["sketch_size"] = ESP.getSketchSize();
  doc["flash_size"] = ESP.getFlashChipSize();
  doc["cpu_freq"] = ESP.getCpuFreqMHz();
  doc["last_sync"] = millis();
  doc["source"] = "esp32";

  String payload;
  serializeJson(doc, payload);
  webSocket.broadcastTXT(payload);
  Serial.printf("[WS] Performance: heap=%u, freq=%u MHz\n", ESP.getFreeHeap(), ESP.getCpuFreqMHz());
}


void handleButtons() {
  unsigned long currentMillis = millis();
  for (int i = 0; i < NUM_DEVICES; i++) {
    if (BUTTON_PINS[i] == -1) continue;

    bool reading = digitalRead(BUTTON_PINS[i]);

    if (reading != lastButtonStates[i]) {
      lastDebounceTime[i] = currentMillis;
    }

    if ((currentMillis - lastDebounceTime[i]) > DEBOUNCE_DELAY) {
      if (reading != buttonStates[i]) {
        buttonStates[i] = reading;
        if (buttonStates[i] == LOW) {
          bool newState = !devices[i].isActive;
          setDeviceState(i, newState);
          sendDeviceStateUpdate(i);
          Serial.printf("[BUTTON] Manual toggle for %s -> %s\n", devices[i].name.c_str(), newState ? "ON" : "OFF");
        }
      }
    }

    lastButtonStates[i] = reading;
  }
}


void blinkStatusLED(int times) {
  for (int i = 0; i < times; i++) {
    digitalWrite(STATUS_LED, HIGH);
    delay(200);
    digitalWrite(STATUS_LED, LOW);
    delay(200);
  }
}

void logAllDeviceStates() {
  Serial.println("=== Device States on Boot ===");
  for (int i = 0; i < NUM_DEVICES; i++) {
    Serial.printf("Device %d [%s] on pin %d -> %s\n",
                  i,
                  devices[i].name.c_str(),
                  devices[i].pin,
                  devices[i].isActive ? "OFF" : "ON");
  }
  Serial.println("=============================");
}

