package com.shmuelzon.HomeAssistantFloorPlan;

import com.eteks.sweethome3d.model.Home;

public class Settings {
    private static final String PROPERTY_PREFIX = "com.shmuelzon.HomeAssistantFloorPlan.";

    private Home home;

    public Settings(Home home) {
        this.home = home;
    }

    public String get(String name, String defaultValue) {
        String value = home.getProperty(PROPERTY_PREFIX + name);
        if (value == null)
            return defaultValue;
        return value;
    }

    public String get(String name) {
        return get(name, null);
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        return Boolean.valueOf(get(name, String.valueOf(defaultValue)));
    }

    public int getInteger(String name, int defaultValue) {
        return Integer.valueOf(get(name, String.valueOf(defaultValue)));
    }

    public long getLong(String name, long defaultValue) {
        return Long.parseLong(get(name, String.valueOf(defaultValue)));
    }

    public void set(String name, String value) {
        String oldValue = get(name);

        if (oldValue == value)
            return;
        home.setProperty(PROPERTY_PREFIX + name, value);
        home.setModified(true);
    }

    public void setBoolean(String name, boolean value) {
        set(name, String.valueOf(value));
    }

    public void setInteger(String name, int value) {
        set(name, String.valueOf(value));
    }

    public void setLong(String name, long value) {
        set(name, String.valueOf(value));
    }
};
