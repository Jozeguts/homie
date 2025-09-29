package com.example.homie.model;

import java.util.List;

public class Room {
    private String id;
    private String name;
    private int deviceCount;
    private int activeDeviceCount;
    private int iconResource;

    public Room(String id, String name, int iconResource) {
        this.id = id;
        this.name = name;
        this.iconResource = iconResource;
        this.deviceCount = 0;
        this.activeDeviceCount = 0;
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

    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }

    public int getActiveDeviceCount() {
        return activeDeviceCount;
    }

    public void setActiveDeviceCount(int activeDeviceCount) {
        this.activeDeviceCount = activeDeviceCount;
    }

    public int getIconResource() {
        return iconResource;
    }

    public void setIconResource(int iconResource) {
        this.iconResource = iconResource;
    }

    public String getStatusText() {
        return deviceCount + " devices • " + activeDeviceCount + " active";
    }

    public String getRoomId() {
        return id;
    }

}
