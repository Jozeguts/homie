package com.example.homie.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.homie.model.Device;
import com.example.homie.model.Room;
import com.example.homie.R;
import java.util.ArrayList;
import java.util.List;

public class SmartHomeRepository {
    private static SmartHomeRepository instance;
    private MutableLiveData<List<Device>> devicesLiveData;
    private MutableLiveData<List<Room>> roomsLiveData;
    private List<Device> devices;
    private List<Room> rooms;

    private SmartHomeRepository() {
        devicesLiveData = new MutableLiveData<>();
        roomsLiveData = new MutableLiveData<>();
        initializeData();
    }

    public static SmartHomeRepository getInstance() {
        if (instance == null) {
            instance = new SmartHomeRepository();
        }
        return instance;
    }

    private void initializeData() {
        // Initialize rooms
        rooms = new ArrayList<>();
        rooms.add(new Room("1", "Living Room", R.drawable.ic_home));
        rooms.add(new Room("2", "Kitchen", R.drawable.ic_home));
        rooms.add(new Room("3", "Bedroom", R.drawable.ic_home));
        rooms.add(new Room("4", "Bathroom", R.drawable.ic_home));
        rooms.add(new Room("5", "Garage", R.drawable.ic_home));
        rooms.add(new Room("6", "Office", R.drawable.ic_home));
        rooms.add(new Room("7", "Dining Room", R.drawable.ic_home));
        rooms.add(new Room("8", "Balcony", R.drawable.ic_home));

        // Initialize devices
        devices = new ArrayList<>();
        devices.add(new Device("1", "Living Room Light", "light", "Living Room", true, R.drawable.ic_lightbulb));
        devices.add(new Device("2", "Kitchen Light", "light", "Kitchen", false, R.drawable.ic_lightbulb));
        devices.add(new Device("3", "Bedroom Light", "light", "Bedroom", true, R.drawable.ic_lightbulb));
        devices.add(new Device("4", "Main Thermostat", "thermostat", "Living Room", true, R.drawable.ic_thermostat));
        devices.add(new Device("5", "Front Door Lock", "lock", "Living Room", true, R.drawable.ic_lock));
        devices.add(new Device("6", "Kitchen Speaker", "speaker", "Kitchen", false, R.drawable.ic_devices));
        devices.add(new Device("7", "Bathroom Heater", "heater", "Bathroom", false, R.drawable.ic_thermostat));
        devices.add(new Device("8", "Garage Door", "door", "Garage", false, R.drawable.ic_lock));
        devices.add(new Device("9", "Office Lamp", "light", "Office", true, R.drawable.ic_lightbulb));
        devices.add(new Device("10", "Dining Room Speaker", "speaker", "Dining Room", false, R.drawable.ic_devices));
        devices.add(new Device("11", "Balcony Light", "light", "Balcony", true, R.drawable.ic_lightbulb));
        devices.add(new Device("12", "Security Camera", "camera", "Garage", true, R.drawable.ic_devices));
        devices.add(new Device("13", "Smart Plug", "plug", "Office", false, R.drawable.ic_devices));
        devices.add(new Device("14", "Humidifier", "humidifier", "Bedroom", false, R.drawable.ic_thermostat));
        devices.add(new Device("15", "Coffee Maker", "appliance", "Kitchen", true, R.drawable.ic_devices));

        // Set temperature for thermostat
        devices.get(3).setTemperature(72.0);

        updateRoomCounts();
        devicesLiveData.setValue(devices);
        roomsLiveData.setValue(rooms);
    }

    private void updateRoomCounts() {
        for (Room room : rooms) {
            int deviceCount = 0;
            int activeCount = 0;
            for (Device device : devices) {
                if (device.getRoom().equals(room.getName())) {
                    deviceCount++;
                    if (device.isActive()) {
                        activeCount++;
                    }
                }
            }
            room.setDeviceCount(deviceCount);
            room.setActiveDeviceCount(activeCount);
        }
    }

    public LiveData<List<Device>> getDevices() {
        return devicesLiveData;
    }

    public LiveData<List<Room>> getRooms() {
        return roomsLiveData;
    }

    public List<Device> getActiveDevices() {
        List<Device> activeDevices = new ArrayList<>();
        for (Device device : devices) {
            if (device.isActive()) {
                activeDevices.add(device);
            }
        }
        return activeDevices;
    }

    public List<Device> getDevicesByRoom(String roomName) {
        List<Device> roomDevices = new ArrayList<>();
        for (Device device : devices) {
            if (device.getRoom().equals(roomName)) {
                roomDevices.add(device);
            }
        }
        return roomDevices;
    }

    public void toggleDevice(String deviceId) {
        for (Device device : devices) {
            if (device.getId().equals(deviceId)) {
                device.toggle();
                break;
            }
        }
        updateRoomCounts();
        devicesLiveData.setValue(devices);
        roomsLiveData.setValue(rooms);
    }

    public void addDevice(Device device) {
        devices.add(device);
    }

    public void removeDevice(String deviceId) {
        devices.removeIf(device -> device.getId().equals(deviceId));
    }

    public List<Device> getAllDevices() {
        return new ArrayList<>(devices);
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public Device getDeviceById(String id) {
        for (Device device : devices) {
            if (device.getId().equals(id)) {
                return device;
            }
        }
        return null;
    }

    public Room getRoomById(String id) {
        for (Room room : rooms) {
            if (room.getId().equals(id)) {
                return room;
            }
        }
        return null;
    }

    public void updateDevice(Device updatedDevice) {
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getId().equals(updatedDevice.getId())) {
                devices.set(i, updatedDevice);
                break;
            }
        }
        updateRoomCounts();
        devicesLiveData.setValue(devices);
    }

    public void updateRoom(Room updatedRoom) {
        for (int i = 0; i < rooms.size(); i++) {
            if (rooms.get(i).getId().equals(updatedRoom.getId())) {
                rooms.set(i, updatedRoom);
                break;
            }
        }
        updateRoomCounts();
        roomsLiveData.setValue(rooms);
    }

}
