package com.example.homie.model;

public class SettingsItem {
    private int iconResource;
    private String title;
    private String description;
    private Class<?> targetActivity;

    public SettingsItem(int iconResource, String title, String description, Class<?> targetActivity) {
        this.iconResource = iconResource;
        this.title = title;
        this.description = description;
        this.targetActivity = targetActivity;
    }

    public int getIconResource() {
        return iconResource;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getTargetActivity() {
        return targetActivity;
    }
}
