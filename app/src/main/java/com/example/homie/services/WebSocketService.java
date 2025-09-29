package com.example.homie.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.example.homie.model.Device;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";
    private static final String WS_URL = "ws://192.168.4.1:81";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WebSocketClient webSocketClient;
    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);
    private final MutableLiveData<List<Device>> deviceUpdates = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Float>> temperatureData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> syncInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> connectionStatus = new MutableLiveData<>(0);
    private final Map<String, Device> deviceCache = new HashMap<>();
    private final Gson gson = new Gson();
    private final IBinder binder = new WebSocketBinder();

    public class WebSocketBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WebSocketService created");
        connect();
    }

    private void connect() {
        executorService.execute(() -> {
            try {
                URI uri = URI.create(WS_URL);
                webSocketClient = new WebSocketClient(uri) {
                    @Override
                    public void onOpen(ServerHandshake handshakedata) {
                        Log.d(TAG, "WebSocket opened: " + handshakedata.getHttpStatusMessage());
                        mainHandler.post(() -> {
                            connected.setValue(true);
                            connectionStatus.setValue(2);
                            requestInitialData();
                        });
                    }

                    @Override
                    public void onMessage(String message) {
                        try {
                            handleWebSocketMessage(message);
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing WebSocket message", e);
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        Log.d(TAG, "WebSocket closed: " + reason + " (code: " + code + "), remote: " + remote);
                        mainHandler.post(() -> {
                            connected.setValue(false);
                            connectionStatus.setValue(0);
                        });
                        // reconnect with delay
                        executorService.execute(() -> {
                            try {
                                Thread.sleep(3000);
                                connect();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception ex) {
                        Log.e(TAG, "WebSocket error", ex);
                        mainHandler.post(() -> {
                            connected.setValue(false);
                            connectionStatus.setValue(-1);
                        });
                    }
                };
                webSocketClient.connect();
            } catch (Exception e) {
                Log.e(TAG, "Failed to create WebSocket client", e);
                mainHandler.post(() -> {
                    connected.setValue(false);
                    connectionStatus.setValue(-1);
                });
            }
        });
    }

    public void requestInitialData() {
        sendCommand("get_initial_data", null);
    }

    public void toggleDevice(String deviceId, boolean state) {
        JsonObject payload = new JsonObject();
        payload.addProperty("device_id", deviceId);
        payload.addProperty("state", state);
        sendCommand("toggle_device", payload);
    }

    public void updateDevice(Device device) {
        JsonObject payload = new JsonObject();
        payload.addProperty("device_id", device.getId());
        payload.addProperty("name", device.getName());
        payload.addProperty("type", device.getType());
        payload.addProperty("room", device.getRoom());
        sendCommand("update_device", payload);
    }

    private void sendCommand(String type, JsonObject data) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            try {
                JsonObject message = new JsonObject();
                message.addProperty("type", type);
                if (data != null) {
                    // Use a fixed "data" field for consistency with parsing
                    message.add("data", data);
                }
                webSocketClient.send(message.toString());
                Log.d(TAG, "Sent WebSocket message: " + message.toString());
            } catch (Exception e) {
                Log.e(TAG, "Failed to send WebSocket message", e);
            }
        } else {
            Log.w(TAG, "WebSocket not open. Cannot send: " + type);
        }
    }

    private void handleWebSocketMessage(String message) {
        try {
            JsonObject obj = JsonParser.parseString(message).getAsJsonObject();
            if (!obj.has("type"))
                return;
            String type = obj.get("type").getAsString();
            switch (type) {
                case "initial_devices":
                    handleInitialDevices(obj);
                    break;
                case "device_state":
                    handleDeviceState(obj);
                    break;
                case "temperature":
                    handleTemperature(obj);
                    break;
                case "heartbeat":
                    Log.d(TAG, "Received heartbeat from ESP32");
                    break;
                case "performance":
                    Log.d(TAG, "Performance update: " + obj);
                    break;
                default:
                    Log.d(TAG, "Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing WebSocket message: " + message, e);
        }
    }

    private void handleInitialDevices(JsonObject obj) {
        try {
            List<Device> devices = new ArrayList<>();
            var devicesArray = obj.getAsJsonArray("devices");
            if (devicesArray != null) {
                for (var element : devicesArray) {
                    JsonObject dev = element.getAsJsonObject();
                    Device device = new Device(
                            dev.get("id").getAsString(),
                            dev.get("name").getAsString(),
                            dev.get("type").getAsString(),
                            dev.get("room").getAsString(),
                            dev.get("isActive").getAsBoolean());
                    if (dev.has("pin"))
                        device.setPin(dev.get("pin").getAsInt());
                    double temp = dev.has("temperature") ? dev.get("temperature").getAsDouble() : Double.NaN;
                    device.setTemperature(temp);
                    if (dev.has("lastUpdate"))
                        device.setLastUpdate(dev.get("lastUpdate").getAsLong());
                    deviceCache.put(device.getId(), device);
                    devices.add(device);
                }
            }
            mainHandler.post(() -> deviceUpdates.setValue(new ArrayList<>(devices)));
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse initial devices", e);
        }
    }

    private void handleDeviceState(JsonObject obj) {
        try {
            String id = obj.get("id").getAsString();
            boolean state = obj.get("isActive").getAsBoolean();
            Device device = deviceCache.get(id);
            if (device != null) {
                device.setActive(state);
                device.setLastUpdate(System.currentTimeMillis());
                deviceCache.put(id, device);
                mainHandler.post(() -> deviceUpdates.setValue(new ArrayList<>(deviceCache.values())));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle device state update", e);
        }
    }

    private void handleTemperature(JsonObject obj) {
        try {
            String id = obj.get("id").getAsString();
            float temp = obj.get("temperature").getAsFloat();
            Map<String, Float> data = new HashMap<>();
            data.put(id, temp);
            mainHandler.post(() -> temperatureData.setValue(data));
            Device device = deviceCache.get(id);
            if (device != null) {
                device.setTemperature((double) temp);
                device.setLastUpdate(System.currentTimeMillis());
                deviceCache.put(id, device);
                mainHandler.post(() -> deviceUpdates.setValue(new ArrayList<>(deviceCache.values())));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle temperature update", e);
        }
    }

    public MutableLiveData<Boolean> getConnected() {
        return connected;
    }

    public MutableLiveData<List<Device>> getDeviceUpdates() {
        return deviceUpdates;
    }

    public MutableLiveData<Map<String, Float>> getTemperatureData() {
        return temperatureData;
    }

    public MutableLiveData<Boolean> getSyncInProgress() {
        return syncInProgress;
    }

    public MutableLiveData<Integer> getConnectionStatus() {
        return connectionStatus;
    }

    public boolean isConnected() {
        return Boolean.TRUE.equals(connected.getValue());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
        executorService.shutdown();
        Log.d(TAG, "WebSocketService destroyed");
    }
}
