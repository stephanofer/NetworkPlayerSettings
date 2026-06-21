package com.stephanofer.networkplayersettings.settings.api;

import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettings.settings.language.LanguagePreference;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public interface PlayerSettingsService {

    CompletableFuture<PlayerSettingsSnapshot> load(UUID playerId);

    Optional<PlayerSettingsSnapshot> cached(UUID playerId);

    PlayerSettingsSnapshot getCachedOrDefault(UUID playerId);

    Language resolvedLanguage(Player player);

    LanguagePreference languagePreference(UUID playerId);

    String countryCode(UUID playerId);

    CompletableFuture<Void> setLanguage(UUID playerId, LanguagePreference preference);

    CompletableFuture<Void> setCountryOverride(UUID playerId, String countryCode);

    CompletableFuture<Void> clearCountryOverride(UUID playerId);

    CompletableFuture<Void> setSetting(UUID playerId, SettingKey key, String value);

    Optional<String> getSetting(UUID playerId, SettingKey key);

    boolean isReady(UUID playerId);
}
