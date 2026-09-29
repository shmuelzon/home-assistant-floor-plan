package com.shmuelzon.HomeAssistantFloorPlan;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.Level;

public class Settings {
    private static final String PROPERTY_PREFIX = "com.shmuelzon.HomeAssistantFloorPlan.";

    private Home home;
    private String prefix;

    public Settings(Home home) {
        this(home, null);
    }

    /* Settings of a specific level are kept separately from those of other levels */
    public Settings(Home home, Level level) {
        this.home = home;
        this.prefix = PROPERTY_PREFIX + (level != null ? level.getId() + "." : "");
    }

    public String get(String name, String defaultValue) {
        String value = home.getProperty(prefix + name);
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

    public List<Long> getListLong(String name, List<Long> defaultValue) {
        String values = get(name);

        if (values == null)
            return defaultValue;
        return Arrays.stream(values.split(",")).map(Long::valueOf).collect(Collectors.toList());
    }

    /* Invalid values, e.g., saved by a different version, are removed and the default value is returned */
    public <T extends Enum<T>> T getEnum(Class<T> type, String name, T defaultValue) {
        try {
            return Enum.valueOf(type, get(name, defaultValue.name()));
        } catch (IllegalArgumentException e) {
            set(name, null);
        }
        return defaultValue;
    }

    public double getDouble(String name, double defaultValue) {
        return Double.parseDouble(get(name, String.valueOf(defaultValue)));
    }

    public void set(String name, String value) {
        String oldValue = get(name);

        if (oldValue != null && oldValue.equals(value))
            return;
        home.setProperty(prefix + name, value);
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

    public void setListLong(String name, List<Long> value) {
        set(name, String.join(",", value.stream().map(String::valueOf).collect(Collectors.toList())));
    }

    public void setDouble(String name, double value) {
        set(name, String.valueOf(value));
    }
};
