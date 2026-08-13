package com.stephanofer.networkplayersettings.platform.bukkit;

import com.stephanofer.networkplayersettings.assets.api.CountryAsset;
import com.stephanofer.networkplayersettings.assets.api.CountryFlagService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.api.StylePatternInfo;
import com.stephanofer.networkplayersettings.settings.event.PlayerSettingChangeEvent;
import com.stephanofer.networkplayersettings.settings.language.Language;
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
    private final PlayerStyleService styleService;
    private final CountryFlagService countryFlagService;
    private final Language defaultLanguage;
    private final boolean cacheEnabled;
    private final String version;
    private final Cache<String, String> cache;

    public PlayerSettingsPlaceholderExpansion(
        final PlayerSettingsService settingsService,
        final PlayerStyleService styleService,
        final CountryFlagService countryFlagService,
        final Language defaultLanguage,
        final Duration cacheTtl,
        final long cacheMaximumSize,
        final String version
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.styleService = Objects.requireNonNull(styleService, "styleService");
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
            return cachedLanguage(player).code();
        }

        if (normalizedParam.equals("language_preference")) {
            if (player == null || player.getUniqueId() == null) {
                return "auto";
            }
            return this.settingsService.languagePreference(player.getUniqueId()).storageValue();
        }

        if (normalizedParam.equals("language_name")) {
            final Language language = cachedLanguage(player);
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

        if (normalizedParam.equals("nick_style_id")) {
            return player == null || player.getUniqueId() == null
                ? ""
                : this.styleService.nickStyleId(player.getUniqueId()).orElse("");
        }

        if (normalizedParam.equals("nick_style_name")) {
            return playerId(player)
                .flatMap(playerId -> this.styleService.nickStyleId(playerId).flatMap(this.styleService::nickPattern))
                .map(StylePatternInfo::displayName)
                .orElse("");
        }

        if (normalizedParam.equals("nick_style_category")) {
            return playerId(player)
                .flatMap(playerId -> this.styleService.nickStyleId(playerId).flatMap(this.styleService::nickPattern))
                .map(StylePatternInfo::category)
                .orElse("");
        }

        if (normalizedParam.equals("nick_style_permission")) {
            return playerId(player)
                .flatMap(playerId -> this.styleService.nickStyleId(playerId).flatMap(this.styleService::nickPattern))
                .map(StylePatternInfo::permission)
                .orElse("");
        }

        if (normalizedParam.equals("nick_formatted") || normalizedParam.equals("nick_formatted_raw")) {
            return onlinePlayer(player)
                .map(this.styleService::formattedNickMiniMessage)
                .orElseGet(() -> player == null || player.getName() == null ? "" : player.getName());
        }

        if (normalizedParam.equals("chat_style_id")) {
            return player == null || player.getUniqueId() == null
                ? ""
                : this.styleService.chatStyleId(player.getUniqueId()).orElse("");
        }

        if (normalizedParam.equals("chat_style_name")) {
            return playerId(player)
                .flatMap(playerId -> this.styleService.chatStyleId(playerId).flatMap(this.styleService::chatPattern))
                .map(StylePatternInfo::displayName)
                .orElse("");
        }

        if (normalizedParam.equals("chat_style_category")) {
            return playerId(player)
                .flatMap(playerId -> this.styleService.chatStyleId(playerId).flatMap(this.styleService::chatPattern))
                .map(StylePatternInfo::category)
                .orElse("");
        }

        if (normalizedParam.equals("chat_style_permission")) {
            return playerId(player)
                .flatMap(playerId -> this.styleService.chatStyleId(playerId).flatMap(this.styleService::chatPattern))
                .map(StylePatternInfo::permission)
                .orElse("");
        }

        if (normalizedParam.equals("chat_preview")) {
            return playerId(player)
                .flatMap(playerId -> this.styleService.chatStyleId(playerId).map(this.styleService::chatPreviewMiniMessage))
                .orElse("");
        }

        return null;
    }

    private java.util.Optional<org.bukkit.entity.Player> onlinePlayer(final OfflinePlayer player) {
        if (player == null || !player.isOnline()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(player.getPlayer());
    }

    private java.util.Optional<UUID> playerId(final OfflinePlayer player) {
        if (player == null || player.getUniqueId() == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(player.getUniqueId());
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

    public void clearCache() {
        this.cache.invalidateAll();
    }

    private void invalidate(final UUID playerId) {
        final String prefix = playerId + ":";
        this.cache.asMap().keySet().removeIf(key -> key.startsWith(prefix));
    }

    private Language cachedLanguage(final OfflinePlayer player) {
        return playerId(player)
            .flatMap(this.settingsService::cachedResolvedLanguage)
            .orElse(this.defaultLanguage);
    }
}
