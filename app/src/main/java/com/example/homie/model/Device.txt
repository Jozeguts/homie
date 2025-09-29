
package com.example.homie.model;

import java.util.HashMap;
import java.util.Map;

public class Device {
    private String id;
    private String name;
    private String type;
    private String room;
    private String roomId;
    private boolean isActive;
    private int iconResource;
    private String status;
    private Double temperature;
    private int pin; // GPIO pin number on ESP32
    private long lastUpdate; // Timestamp of last update
    private boolean isOnline; // ESP32 connection status
    private float brightness; // For lights (0-100)
    private int speed; // For fans (0-5)
    private int volume; // For speakers (0-100)
    private String mqttTopic; // MQTT topic for this device
    private boolean isESP32Controlled; // Whether device is controlled by ESP32
    private String esp32DeviceId; // ID used by ESP32
    private Map<String, Object> esp32Properties; // Additional ESP32 properties

    // Constructor with basic parameters
    public Device(String id, String name, String type, String room, boolean isActive) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.room = room;
        this.roomId = room;
        this.isActive = isActive;
        this.status = isActive ? "On" : "Off";
        this.isOnline = false;
        this.lastUpdate = System.currentTimeMillis();
        this.brightness = 100.0f;
        this.speed = 1;
        this.volume = 50;
        this.temperature = null;
        this.pin = -1; // Invalid pin by default
        this.esp32Properties = new HashMap<>();
    }

    // Constructor with icon resource
    public Device(String id, String name, String type, String room, boolean isActive, int iconResource) {
        this(id, name, type, room, isActive);
        this.iconResource = iconResource;
    }

    // Constructor with pin number for ESP32 integration
    public Device(String id, String name, String type, String room, boolean isActive, int iconResource, int pin) {
        this(id, name, type, room, isActive, iconResource);
        this.pin = pin;
    }

    // Constructor for ESP32-controlled devices
    public Device(String id, String name, String type, String room, boolean isActive, int iconResource, int pin,
            String esp32DeviceId) {
        this(id, name, type, room, isActive, iconResource, pin);
        this.esp32DeviceId = esp32DeviceId;
        this.isESP32Controlled = true;
        this.mqttTopic = "homie/devices/" + esp32DeviceId + "/state";
        this.esp32Properties = new HashMap<>();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
        this.status = active ? "On" : "Off";
        this.lastUpdate = System.currentTimeMillis();

        // Add ESP32 sync flag if this is an ESP32-controlled device
        if (isESP32Controlled) {
            addEsp32Property("needsSync", true);
        }
    }

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public int getPin() {
        return pin;
    }

    public void setPin(int pin) {
        this.pin = pin;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public float getBrightness() {
        return brightness;
    }

    public void setBrightness(float brightness) {
        this.brightness = Math.max(0, Math.min(100, brightness));
        this.lastUpdate = System.currentTimeMillis();
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = Math.max(0, Math.min(5, speed));
        this.lastUpdate = System.currentTimeMillis();
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
        this.lastUpdate = System.currentTimeMillis();
    }

    public void toggle() {
        this.isActive = !this.isActive;
        this.status = this.isActive ? "On" : "Off";
        this.lastUpdate = System.currentTimeMillis();
    }

    // Helper methods
    public boolean isLight() {
        return type != null && type.toLowerCase().contains("light");
    }

    public boolean isFan() {
        return type != null && type.toLowerCase().contains("fan");
    }

    public boolean isThermostat() {
        return type != null && type.toLowerCase().contains("thermostat");
    }

    public boolean isSpeaker() {
        return type != null && (type.toLowerCase().contains("speaker") || type.toLowerCase().contains("audio"));
    }

    public boolean hasTemperatureSensor() {
        return temperature != null && temperature > -50.0; // Valid temperature reading
    }

    // ESP32-specific getters and setters
    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public boolean isESP32Controlled() {
        return isESP32Controlled;
    }

    public void setESP32Controlled(boolean esp32Controlled) {
        isESP32Controlled = esp32Controlled;
    }

    public String getEsp32DeviceId() {
        return esp32DeviceId;
    }

    public void setEsp32DeviceId(String esp32DeviceId) {
        this.esp32DeviceId = esp32DeviceId;
        if (esp32DeviceId != null) {
            this.mqttTopic = "homie/devices/" + esp32DeviceId + "/state";
        }
    }

    public Map<String, Object> getEsp32Properties() {
        return esp32Properties;
    }

    public void setEsp32Properties(Map<String, Object> esp32Properties) {
        this.esp32Properties = esp32Properties;
    }

    public void addEsp32Property(String key, Object value) {
        if (esp32Properties == null) {
            esp32Properties = new HashMap<>();
        }
        esp32Properties.put(key, value);
    }

    // Method to check if device needs ESP32 sync
    public boolean needsESP32Sync() {
        return isESP32Controlled && esp32Properties != null &&
                Boolean.TRUE.equals(esp32Properties.get("needsSync"));
    }

    // Method to mark device as synced with ESP32
    public void markESP32Synced() {
        if (esp32Properties != null) {
            esp32Properties.put("needsSync", false);
            esp32Properties.put("lastSyncTime", System.currentTimeMillis());
        }
    }

    // Method to get last ESP32 sync time
    public long getLastESP32SyncTime() {
        if (esp32Properties != null && esp32Properties.containsKey("lastSyncTime")) {
            return (Long) esp32Properties.get("lastSyncTime");
        }
        return 0;
    }

    // Enhanced status method for ESP32 devices
    public String getDetailedStatus() {
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append(isActive ? "On" : "Off");

        if (isActive) {
            if (isLight() && brightness < 100) {
                statusBuilder.append(" (").append((int) brightness).append("%)");
            } else if (isFan() && speed > 0) {
                statusBuilder.append(" (Speed ").append(speed).append(")");
            } else if (isSpeaker() && volume != 50) {
                statusBuilder.append(" (Vol ").append(volume).append("%)");
            }
        }

        if (hasTemperatureSensor()) {
            statusBuilder.append(" • ").append(String.format("%.1f°C", temperature));
        }

        if (isESP32Controlled) {
            if (!isOnline) {
                statusBuilder.append(" • ESP32 Offline");
            } else if (needsESP32Sync()) {
                statusBuilder.append(" • Syncing...");
            }
        } else if (!isOnline) {
            statusBuilder.append(" • Offline");
        }

        return statusBuilder.toString();
    }

    public long getTimeSinceLastUpdate() {
        return System.currentTimeMillis() - lastUpdate;
    }

    public boolean isRecentlyUpdated() {
        return getTimeSinceLastUpdate() < 30000; // Within last 30 seconds
    }

    @Override
    public String toString() {
        return "Device{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", room='" + room + '\'' +
                ", isActive=" + isActive +
                ", pin=" + pin +
                ", isOnline=" + isOnline +
                ", isESP32Controlled=" + isESP32Controlled +
                ", esp32DeviceId='" + esp32DeviceId + '\'' +
                ", lastUpdate=" + lastUpdate +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Device device = (Device) obj;
        return id != null ? id.equals(device.id) : device.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
