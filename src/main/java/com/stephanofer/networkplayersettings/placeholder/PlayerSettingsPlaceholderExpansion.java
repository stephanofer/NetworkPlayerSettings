package com.stephanofer.networkplayersettings.placeholder;

import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PlayerSettingsPlaceholderExpansion extends PlaceholderExpansion {

    private final PlayerSettingsService settingsService;
    private final Duration cacheTtl;
    private final String version;
    private final ConcurrentHashMap<String, CachedValue> cache = new ConcurrentHashMap<>();

    public PlayerSettingsPlaceholderExpansion(
        final PlayerSettingsService settingsService,
        final Duration cacheTtl,
        final String version
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl");
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
        final long now = System.currentTimeMillis();

        final CachedValue cachedValue = this.cache.get(cacheKey);
        if (cachedValue != null && cachedValue.expiresAtMillis() >= now) {
            return cachedValue.value();
        }

        final String resolved = resolvePlaceholder(player, normalizedParam);
        if (resolved != null && !this.cacheTtl.isZero() && !this.cacheTtl.isNegative()) {
            this.cache.put(cacheKey, new CachedValue(resolved, now + this.cacheTtl.toMillis()));
        }
        return resolved;
    }

    private @Nullable String resolvePlaceholder(final OfflinePlayer player, final String normalizedParam) {
        if (normalizedParam.equals("language")) {
            if (player != null && player.isOnline() && player.getPlayer() != null) {
                return this.settingsService.resolvedLanguage(player.getPlayer()).code();
            }
            return Language.ENGLISH.code();
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
            return Language.ENGLISH.displayName(Language.ENGLISH);
        }

        if (normalizedParam.equals("country")) {
            if (player == null || player.getUniqueId() == null) {
                return "XX";
            }
            return this.settingsService.countryCode(player.getUniqueId());
        }

        return null;
    }

    private record CachedValue(String value, long expiresAtMillis) {
    }
}
