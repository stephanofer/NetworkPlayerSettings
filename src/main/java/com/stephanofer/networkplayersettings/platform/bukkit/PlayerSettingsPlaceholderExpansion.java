package com.stephanofer.networkplayersettings.platform.bukkit;

import com.stephanofer.networkplayersettings.assets.api.CountryAsset;
import com.stephanofer.networkplayersettings.assets.api.CountryFlagService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.event.PlayerSettingChangeEvent;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettings.settings.language.LanguagePreference;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerSettingsPlaceholderExpansion extends PlaceholderExpansion implements Listener {

    private final PlayerSettingsService settingsService;
    private final CountryFlagService countryFlagService;
    private final Language defaultLanguage;
    private final boolean cacheEnabled;
    private final String version;
    private final Cache<String, String> cache;

    public PlayerSettingsPlaceholderExpansion(
        final PlayerSettingsService settingsService,
        final CountryFlagService countryFlagService,
        final Language defaultLanguage,
        final Duration cacheTtl,
        final long cacheMaximumSize,
        final String version
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.countryFlagService = Objects.requireNonNull(countryFlagService, "countryFlagService");
        this.defaultLanguage = Objects.requireNonNull(defaultLanguage, "defaultLanguage");
        Objects.requireNonNull(cacheTtl, "cacheTtl");
        this.cacheEnabled = !cacheTtl.isZero() && !cacheTtl.isNegative();
        final Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder()
            .maximumSize(Math.max(1L, cacheMaximumSize));
        if (this.cacheEnabled) {
            cacheBuilder.expireAfterWrite(cacheTtl);
        }
        this.cache = cacheBuilder.build();
        this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public @NotNull String getAuthor() {
        return "Stephanofer";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "playersettings";
    }

    @Override
    public @NotNull String getVersion() {
        return this.version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(final OfflinePlayer player, final @NotNull String params) {
        final String normalizedParam = params.toLowerCase(java.util.Locale.ROOT);
        final UUID playerId = player == null ? null : player.getUniqueId();
        final String cacheKey = playerId == null ? "global:" + normalizedParam : playerId + ":" + normalizedParam;

        if (this.cacheEnabled) {
            final String cachedValue = this.cache.getIfPresent(cacheKey);
            if (cachedValue != null) {
                return cachedValue;
            }
        }

        final String resolved = resolvePlaceholder(player, normalizedParam);
        if (resolved != null && this.cacheEnabled) {
            this.cache.put(cacheKey, resolved);
        }
        return resolved;
    }

    private @Nullable String resolvePlaceholder(final OfflinePlayer player, final String normalizedParam) {
        if (normalizedParam.equals("language")) {
            if (player != null && player.isOnline() && player.getPlayer() != null) {
                return this.settingsService.resolvedLanguage(player.getPlayer()).code();
            }
            return offlineLanguage(player).code();
        }

        if (normalizedParam.equals("language_preference")) {
            if (player == null || player.getUniqueId() == null) {
                return "auto";
            }
            return this.settingsService.languagePreference(player.getUniqueId()).storageValue();
        }

        if (normalizedParam.equals("language_name")) {
            if (player != null && player.isOnline() && player.getPlayer() != null) {
                final Language language = this.settingsService.resolvedLanguage(player.getPlayer());
                return language.displayName(language);
            }
            final Language language = offlineLanguage(player);
            return language.displayName(language);
        }

        if (normalizedParam.equals("country")) {
            if (player == null || player.getUniqueId() == null) {
                return "XX";
            }
            return this.settingsService.countryCode(player.getUniqueId());
        }

        if (normalizedParam.equals("country_display_name")) {
            return countryAsset(player).displayName();
        }

        if (normalizedParam.equals("country_head_value")) {
            if (player == null || player.getUniqueId() == null) {
                return this.countryFlagService.headTextureValueForCountry("XX");
            }
            return this.countryFlagService.headTextureValue(player.getUniqueId());
        }

        if (normalizedParam.equals("country_head_tag")) {
            if (player == null || player.getUniqueId() == null) {
                return this.countryFlagService.miniMessageTagForCountry("XX");
            }
            return this.countryFlagService.miniMessageTag(player.getUniqueId());
        }

        if (normalizedParam.equals("show_country_flag")) {
            if (player == null || player.getUniqueId() == null) {
                return "true";
            }
            return Boolean.toString(this.settingsService.showCountryFlag(player.getUniqueId()));
        }

        return null;
    }

    private CountryAsset countryAsset(final OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return this.countryFlagService.assetForCountry("XX");
        }
        return this.countryFlagService.asset(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerSettingChange(final PlayerSettingChangeEvent event) {
        invalidate(event.playerId());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    private void invalidate(final UUID playerId) {
        final String prefix = playerId + ":";
        this.cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    private Language offlineLanguage(final OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return this.defaultLanguage;
        }
        final LanguagePreference preference = this.settingsService.languagePreference(player.getUniqueId());
        return switch (preference) {
            case SPANISH -> Language.SPANISH;
            case ENGLISH -> Language.ENGLISH;
            case AUTO -> this.defaultLanguage;
        };
    }
}
