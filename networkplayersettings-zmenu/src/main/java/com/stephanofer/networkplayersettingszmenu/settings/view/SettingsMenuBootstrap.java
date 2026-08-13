package com.stephanofer.networkplayersettingszmenu.settings.view;

import com.hera.craftkit.zmenu.ZMenuIntegration;
import com.hera.craftkit.zmenu.ZMenuReloadPlan;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.settings.country.CountryFlagButtonLoader;
import com.stephanofer.networkplayersettingszmenu.settings.language.LanguageButtonLoader;
import com.stephanofer.networkplayersettingszmenu.settings.style.ClearStyleButtonLoader;
import com.stephanofer.networkplayersettingszmenu.settings.style.StyleButtonKind;
import com.stephanofer.networkplayersettingszmenu.settings.style.StyleFilterButtonLoader;
import com.stephanofer.networkplayersettingszmenu.settings.style.StylePatternButtonLoader;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class SettingsMenuBootstrap {

    private final JavaPlugin plugin;
    private final ZMenuIntegration zmenu;
    private final PlayerSettingsService settingsService;
    private final PlayerStyleService styleService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;

    public SettingsMenuBootstrap(
        final JavaPlugin plugin,
        final ZMenuIntegration zmenu,
        final PlayerSettingsService settingsService,
        final PlayerStyleService styleService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.zmenu = Objects.requireNonNull(zmenu, "zmenu");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.styleService = Objects.requireNonNull(styleService, "styleService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.settingsConfig = Objects.requireNonNull(settingsConfig, "settingsConfig");
    }

    public ZMenuReloadPlan load() {
        return this.zmenu.bootstrap()
            .buttons(registry -> {
                registry.button(new LanguageButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.messages,
                    this.settingsConfig
                ));
                registry.button(new CountryFlagButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.messages,
                    this.settingsConfig
                ));
                registry.button(new StylePatternButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.styleService,
                    this.messages,
                    this.settingsConfig,
                    StyleButtonKind.NICK,
                    "NPS_NICK_STYLE"
                ));
                registry.button(new StylePatternButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.styleService,
                    this.messages,
                    this.settingsConfig,
                    StyleButtonKind.CHAT,
                    "NPS_CHAT_STYLE"
                ));
                registry.button(new ClearStyleButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.styleService,
                    this.messages,
                    this.settingsConfig,
                    StyleButtonKind.NICK,
                    "NPS_CLEAR_NICK_STYLE"
                ));
                registry.button(new ClearStyleButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.styleService,
                    this.messages,
                    this.settingsConfig,
                    StyleButtonKind.CHAT,
                    "NPS_CLEAR_CHAT_STYLE"
                ));
                registry.button(new StyleFilterButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.messages,
                    StyleButtonKind.NICK,
                    "NPS_NICK_STYLE_FILTER"
                ));
                registry.button(new StyleFilterButtonLoader(
                    this.plugin,
                    this.settingsService,
                    this.messages,
                    StyleButtonKind.CHAT,
                    "NPS_CHAT_STYLE_FILTER"
                ));
            })
            .defaultInventories(
                "inventories/settings-main.yml",
                "inventories/language.yml",
                "inventories/nick-styles.yml",
                "inventories/chat-styles.yml"
            )
            .defaultDialogs("dialogs/language.yml")
            .inventories("inventories")
            .dialogs("dialogs")
            .load();
    }
}
