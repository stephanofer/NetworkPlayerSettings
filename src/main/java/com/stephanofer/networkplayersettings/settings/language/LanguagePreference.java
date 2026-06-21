package com.stephanofer.networkplayersettings.settings.language;

public enum LanguagePreference {
    AUTO("auto"),
    SPANISH("es"),
    ENGLISH("en");

    private final String storageValue;

    LanguagePreference(final String storageValue) {
        this.storageValue = storageValue;
    }

    public String storageValue() {
        return this.storageValue;
    }

    public static boolean isSupported(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return false;
        }

        for (final LanguagePreference preference : values()) {
            if (preference.storageValue.equalsIgnoreCase(rawValue.trim())) {
                return true;
            }
        }
        return false;
    }

    public static LanguagePreference fromStorage(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return AUTO;
        }

        for (final LanguagePreference preference : values()) {
            if (preference.storageValue.equalsIgnoreCase(rawValue.trim())) {
                return preference;
            }
        }

        return AUTO;
    }
}
