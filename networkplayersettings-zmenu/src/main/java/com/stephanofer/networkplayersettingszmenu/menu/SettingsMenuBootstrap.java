package com.stephanofer.networkplayersettingszmenu.menu;

import com.hera.craftkit.zmenu.ZMenuIntegration;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class SettingsMenuBootstrap {

    private final JavaPlugin plugin;
    private final ZMenuIntegration zmenu;
    private final PlayerSettingsService settingsService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;

    public SettingsMenuBootstrap(
        final JavaPlugin plugin,
        final ZMenuIntegration zmenu,
        final PlayerSettingsService settingsService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.zmenu = Objects.requireNonNull(zmenu, "zmenu");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.settingsConfig = Objects.requireNonNull(settingsConfig, "settingsConfig");
    }

    public void load() {
        this.zmenu.bootstrap()
            .buttons(registry -> registry.button(new LanguageButtonLoader(
                this.plugin,
                this.settingsService,
                this.messages,
                this.settingsConfig
            )))
            .defaultInventories("inventories/language.yml")
            .inventories("inventories")
            .dialogs("dialogs")
            .load();
    }
}
