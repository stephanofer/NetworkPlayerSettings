package com.stephanofer.networkplayersettings.api;

import java.util.Arrays;

public enum SettingKey {
    LANGUAGE("language", true, true, LanguagePreference.AUTO.storageValue()),
    DETECTED_LOCALE("detected_locale", true, false, ""),
    DETECTED_COUNTRY("detected_country", true, false, CountryFlag.UNKNOWN_CODE),
    COUNTRY_OVERRIDE("country_override", true, false, "");

    private final String storageKey;
    private final boolean persisted;
    private final boolean playerWritable;
    private final String defaultValue;

    SettingKey(final String storageKey, final boolean persisted, final boolean playerWritable, final String defaultValue) {
        this.storageKey = storageKey;
        this.persisted = persisted;
        this.playerWritable = playerWritable;
        this.defaultValue = defaultValue;
    }

    public String storageKey() {
        return this.storageKey;
    }

    public boolean persisted() {
        return this.persisted;
    }

    public boolean playerWritable() {
        return this.playerWritable;
    }

    public String defaultValue() {
        return this.defaultValue;
    }

    public static SettingKey fromStorageKey(final String key) {
        return Arrays.stream(values())
            .filter(value -> value.storageKey.equalsIgnoreCase(key))
            .findFirst()
            .orElse(null);
    }
}
