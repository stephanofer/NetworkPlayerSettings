package com.stephanofer.networkplayersettings.placeholder;

import com.stephanofer.networkplatform.hooks.placeholderapi.PlaceholderExpansionSpec;
import com.stephanofer.networkplatform.hooks.placeholderapi.PlaceholderService;
import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import java.time.Duration;
import java.util.Objects;
import org.bukkit.entity.Player;

public final class PlayerSettingsPlaceholderRegistrar {

    private final PlaceholderService placeholderService;
    private final PlayerSettingsService settingsService;
    private final Duration cacheTtl;
    private final String version;

    public PlayerSettingsPlaceholderRegistrar(
        final PlaceholderService placeholderService,
        final PlayerSettingsService settingsService,
        final Duration cacheTtl,
        final String version
    ) {
        this.placeholderService = Objects.requireNonNull(placeholderService, "placeholderService");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.cacheTtl = Objects.requireNonNull(cacheTtl, "cacheTtl");
        this.version = Objects.requireNonNull(version, "version");
    }

    public void register() {
        this.placeholderService.register(
            PlaceholderExpansionSpec.builder("playersettings")
                .author("Stephanofer")
                .version(this.version)
                .cachedPlaceholder("language", this.cacheTtl, (player, params) -> {
                    if (!(player instanceof Player onlinePlayer)) {
                        return Language.ENGLISH.code();
                    }
                    return this.settingsService.resolvedLanguage(onlinePlayer).code();
                })
                .cachedPlaceholder("language_preference", this.cacheTtl, (player, params) -> {
                    if (player == null) {
                        return "auto";
                    }
                    return this.settingsService.languagePreference(player.getUniqueId()).storageValue();
                })
                .cachedPlaceholder("language_name", this.cacheTtl, (player, params) -> {
                    if (!(player instanceof Player onlinePlayer)) {
                        return Language.ENGLISH.displayName(Language.ENGLISH);
                    }
                    final Language language = this.settingsService.resolvedLanguage(onlinePlayer);
                    return language.displayName(language);
                })
                .cachedPlaceholder("country", this.cacheTtl, (player, params) -> {
                    if (player == null) {
                        return "XX";
                    }
                    return this.settingsService.countryCode(player.getUniqueId());
                })
                .build()
        );
    }
}
