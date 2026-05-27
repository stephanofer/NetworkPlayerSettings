package com.stephanofer.networkplayersettings.api;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerSettingsSnapshot {

    private final UUID playerId;
    private final Map<SettingKey, String> values;

    public PlayerSettingsSnapshot(final UUID playerId, final Map<SettingKey, String> values) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        final EnumMap<SettingKey, String> copy = new EnumMap<>(SettingKey.class);
        copy.put(SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue());
        copy.put(SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE);
        copy.put(SettingKey.COUNTRY_OVERRIDE, "");
        values.forEach((key, value) -> copy.put(
            Objects.requireNonNull(key, "key"),
            value == null ? "" : value.trim()
        ));
        this.values = Collections.unmodifiableMap(copy);
    }

    public static PlayerSettingsSnapshot defaults(final UUID playerId) {
        return new PlayerSettingsSnapshot(playerId, Map.of(
            SettingKey.LANGUAGE, SettingKey.LANGUAGE.defaultValue(),
            SettingKey.DETECTED_COUNTRY, SettingKey.DETECTED_COUNTRY.defaultValue(),
            SettingKey.COUNTRY_OVERRIDE, SettingKey.COUNTRY_OVERRIDE.defaultValue()
        ));
    }

    public UUID playerId() {
        return this.playerId;
    }

    public Map<SettingKey, String> values() {
        return this.values;
    }

    public Optional<String> setting(final SettingKey key) {
        return Optional.ofNullable(this.values.get(key)).filter(value -> !value.isBlank());
    }

    public String valueOrDefault(final SettingKey key) {
        return this.values.getOrDefault(key, key.defaultValue());
    }

    public LanguagePreference languagePreference() {
        return LanguagePreference.fromStorage(valueOrDefault(SettingKey.LANGUAGE));
    }

    public String detectedCountryCode() {
        return CountryFlag.normalizeCode(valueOrDefault(SettingKey.DETECTED_COUNTRY));
    }

    public Optional<String> countryOverride() {
        return setting(SettingKey.COUNTRY_OVERRIDE)
            .filter(CountryFlag::isIsoAlpha2)
            .map(CountryFlag::normalizeCode)
            .filter(code -> !CountryFlag.UNKNOWN_CODE.equals(code));
    }

    public String countryCode() {
        return countryOverride().orElseGet(this::detectedCountryCode);
    }

    public PlayerSettingsSnapshot withSetting(final SettingKey key, final String value) {
        final EnumMap<SettingKey, String> copy = new EnumMap<>(this.values);
        copy.put(key, value == null ? "" : value.trim());
        return new PlayerSettingsSnapshot(this.playerId, copy);
    }
}
