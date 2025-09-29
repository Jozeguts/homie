
package com.example.homie.repository;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.homie.model.Device;
import com.example.homie.model.Room;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

public class ESP32Repository {
    private static final String PREFS_NAME = "homie_esp32_data";
    private static final String KEY_DEVICES = "esp32_devices";
    private static final String KEY_ROOMS = "esp32_rooms";
    private static final String KEY_LAST_SYNC = "last_esp32_sync";
    private static final String KEY_TEMPERATURE_DATA = "temperature_data";
    private static final String KEY_CONNECTION_STATE = "esp32_connection_state";

    // Drawable resource constants (you'll need to ensure these exist in your
    // res/drawable folder)
    private static final int R_drawable_ic_lightbulb = android.R.drawable.ic_dialog_info;
    private static final int R_drawable_ic_settings = android.R.drawable.ic_menu_preferences;
    private static final int R_drawable_ic_home = android.R.drawable.ic_menu_view;

    private final SharedPreferences preferences;
    private final Gson gson;

    public ESP32Repository(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    // Device management
    public void saveDevices(List<Device> devices) {
        String json = gson.toJson(devices);
        preferences.edit().putString(KEY_DEVICES, json).apply();
    }

    public List<Device> loadDevices() {
        String json = preferences.getString(KEY_DEVICES, null);
        if (json != null) {
            Type listType = new TypeToken<List<Device>>() {
            }.getType();
            List<Device> devices = gson.fromJson(json, listType);
            return devices != null ? devices : createDefaultDevices();
        }
        return createDefaultDevices();
    }

    public void saveDevice(Device device) {
        List<Device> devices = loadDevices();
        boolean found = false;

        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getId().equals(device.getId())) {
                devices.set(i, device);
                found = true;
                break;
            }
        }

        if (!found) {
            devices.add(device);
        }

        saveDevices(devices);
    }

    public Device getDeviceById(String deviceId) {
        List<Device> devices = loadDevices();
        for (Device device : devices) {
            if (device.getId().equals(deviceId)) {
                return device;
            }
        }
        return null;
    }

    // Room management
    public void saveRooms(List<Room> rooms) {
        String json = gson.toJson(rooms);
        preferences.edit().putString(KEY_ROOMS, json).apply();
    }

    public List<Room> loadRooms() {
        String json = preferences.getString(KEY_ROOMS, null);
        if (json != null) {
            Type listType = new TypeToken<List<Room>>() {
            }.getType();
            List<Room> rooms = gson.fromJson(json, listType);
            return rooms != null ? rooms : createDefaultRooms();
        }
        return createDefaultRooms();
    }

    // Temperature data
    public void saveTemperatureData(Map<String, Float> temperatureData) {
        String json = gson.toJson(temperatureData);
        preferences.edit()
                .putString(KEY_TEMPERATURE_DATA, json)
                .putLong("temp_timestamp", System.currentTimeMillis())
                .apply();
    }

    public Map<String, Float> loadTemperatureData() {
        String json = preferences.getString(KEY_TEMPERATURE_DATA, null);
        if (json != null) {
            Type mapType = new TypeToken<Map<String, Float>>() {
            }.getType();
            Map<String, Float> data = gson.fromJson(json, mapType);
            return data != null ? data : new HashMap<>();
        }
        return new HashMap<>();
    }

    // Connection state management
    public void saveConnectionState(boolean connected) {
        preferences.edit()
                .putBoolean(KEY_CONNECTION_STATE, connected)
                .putLong("connection_timestamp", System.currentTimeMillis())
                .apply();
    }

    public boolean isLastKnownConnected() {
        return preferences.getBoolean(KEY_CONNECTION_STATE, false);
    }

    public long getLastConnectionTimestamp() {
        return preferences.getLong("connection_timestamp", 0);
    }

    // Sync management
    public void setLastSyncTime(long timestamp) {
        preferences.edit().putLong(KEY_LAST_SYNC, timestamp).apply();
    }

    public long getLastSyncTime() {
        return preferences.getLong(KEY_LAST_SYNC, 0);
    }

    public boolean needsSync() {
        long lastSync = getLastSyncTime();
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastSync) > 300000; // 5 minutes
    }

    // Create default devices for ESP32
    private List<Device> createDefaultDevices() {
        List<Device> devices = new ArrayList<>();

        String[] names = {
                "Living Room Light", "Kitchen Light", "Bedroom Light", "Bathroom Light",
                "Living Room Fan", "Bedroom Fan", "Kitchen Exhaust", "Garden Light"
        };

        String[] types = { "light", "light", "light", "light", "fan", "fan", "fan", "light" };
        String[] rooms = { "Living Room", "Kitchen", "Bedroom", "Bathroom", "Living Room", "Bedroom", "Kitchen",
                "Garden" };
        int[] pins = { 16, 17, 18, 19, 21, 22, 23, 25 };
        int[] icons = { R_drawable_ic_lightbulb, R_drawable_ic_lightbulb, R_drawable_ic_lightbulb,
                R_drawable_ic_lightbulb,
                R_drawable_ic_settings, R_drawable_ic_settings, R_drawable_ic_settings, R_drawable_ic_lightbulb };

        for (int i = 0; i < names.length; i++) {
            String deviceId = "device_" + i;
            String esp32DeviceId = "device_" + i;

            Device device = new Device(deviceId, names[i], types[i], rooms[i], false, icons[i], pins[i], esp32DeviceId);
            device.setESP32Controlled(true);
            device.setOnline(false); // Initially offline until ESP32 connection is established
            devices.add(device);
        }

        return devices;
    }

    // Create default rooms

    private List<Room> createDefaultRooms() {
        List<Room> rooms = new ArrayList<>();

        rooms.add(new Room("living_room", "Living Room", R_drawable_ic_home));
        rooms.add(new Room("kitchen", "Kitchen", R_drawable_ic_home));
        rooms.add(new Room("bedroom", "Bedroom", R_drawable_ic_home));
        rooms.add(new Room("bathroom", "Bathroom", R_drawable_ic_home));
        rooms.add(new Room("garden", "Garden", R_drawable_ic_home));

        return rooms;
    }

    // Device filtering and search
    public List<Device> getDevicesByRoom(String roomName) {
        List<Device> allDevices = loadDevices();
        List<Device> roomDevices = new ArrayList<>();

        for (Device device : allDevices) {
            if (roomName.equals(device.getRoom())) {
                roomDevices.add(device);
            }
        }

        return roomDevices;
    }

    public List<Device> getDevicesByType(String deviceType) {
        List<Device> allDevices = loadDevices();
        List<Device> typeDevices = new ArrayList<>();

        for (Device device : allDevices) {
            if (deviceType.equalsIgnoreCase(device.getType())) {
                typeDevices.add(device);
            }
        }

        return typeDevices;
    }

    public List<Device> getActiveDevices() {
        List<Device> allDevices = loadDevices();
        List<Device> activeDevices = new ArrayList<>();

        for (Device device : allDevices) {
            if (device.isActive()) {
                activeDevices.add(device);
            }
        }

        return activeDevices;
    }

    public List<Device> getOnlineDevices() {
        List<Device> allDevices = loadDevices();
        List<Device> onlineDevices = new ArrayList<>();

        for (Device device : allDevices) {
            if (device.isOnline()) {
                onlineDevices.add(device);
            }
        }

        return onlineDevices;
    }

    // Statistics and analytics
    public Map<String, Integer> getRoomDeviceCounts() {
        List<Device> devices = loadDevices();
        Map<String, Integer> roomCounts = new HashMap<>();

        for (Device device : devices) {
            String room = device.getRoom();
            roomCounts.put(room, roomCounts.getOrDefault(room, 0) + 1);
        }

        return roomCounts;
    }

    public Map<String, Integer> getDeviceTypeCounts() {
        List<Device> devices = loadDevices();
        Map<String, Integer> typeCounts = new HashMap<>();

        for (Device device : devices) {
            String type = device.getType();
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        }

        return typeCounts;
    }

    public int getActiveDeviceCount() {
        return getActiveDevices().size();
    }

    public int getOnlineDeviceCount() {
        return getOnlineDevices().size();
    }

    // Utility methods
    public void clearAllData() {
        preferences.edit().clear().apply();
    }

    public boolean hasStoredData() {
        return preferences.contains(KEY_DEVICES);
    }

    public Map<String, Object> getAllDataForSync() {
        Map<String, Object> syncData = new HashMap<>();
        syncData.put("devices", loadDevices());
        syncData.put("rooms", loadRooms());
        syncData.put("temperatureData", loadTemperatureData());
        syncData.put("lastSync", getLastSyncTime());
        syncData.put("connectionState", isLastKnownConnected());
        syncData.put("timestamp", System.currentTimeMillis());
        syncData.put("source", "android_homie");
        return syncData;
    }

    // Bulk operations
    public void updateDevicesFromESP32(List<Device> esp32Devices) {
        List<Device> localDevices = loadDevices();
        boolean updated = false;

        for (Device esp32Device : esp32Devices) {
            for (int i = 0; i < localDevices.size(); i++) {
                Device localDevice = localDevices.get(i);
                if (localDevice.getId().equals(esp32Device.getId())) {
                    // Update state and connection info from ESP32
                    localDevice.setActive(esp32Device.isActive());
                    localDevice.setOnline(true);
                    localDevice.setLastUpdate(esp32Device.getLastUpdate());
                    if (esp32Device.getTemperature() != null) {
                        localDevice.setTemperature(esp32Device.getTemperature());
                    }
                    localDevice.markESP32Synced();
                    updated = true;
                    break;
                }
            }
        }

        if (updated) {
            saveDevices(localDevices);
        }
    }

    public void markAllDevicesOffline() {
        List<Device> devices = loadDevices();
        boolean updated = false;

        for (Device device : devices) {
            if (device.isESP32Controlled() && device.isOnline()) {
                device.setOnline(false);
                updated = true;
            }
        }

        if (updated) {
            saveDevices(devices);
        }
    }

    // Export/Import functionality
    public String exportData() {
        return gson.toJson(getAllDataForSync());
    }

    public boolean importData(String jsonData) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> data = gson.fromJson(jsonData, mapType);

            if (data.containsKey("devices")) {
                String devicesJson = gson.toJson(data.get("devices"));
                Type devicesType = new TypeToken<List<Device>>() {
                }.getType();
                List<Device> devices = gson.fromJson(devicesJson, devicesType);
                saveDevices(devices);
            }

            if (data.containsKey("rooms")) {
                String roomsJson = gson.toJson(data.get("rooms"));
                Type roomsType = new TypeToken<List<Room>>() {
                }.getType();
                List<Room> rooms = gson.fromJson(roomsJson, roomsType);
                saveRooms(rooms);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
