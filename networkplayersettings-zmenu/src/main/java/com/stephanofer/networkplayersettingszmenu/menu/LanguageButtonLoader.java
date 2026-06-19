package com.stephanofer.networkplayersettingszmenu.menu;

import com.stephanofer.networkplayersettings.api.LanguagePreference;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class LanguageButtonLoader extends ButtonLoader {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;

    public LanguageButtonLoader(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig
    ) {
        super(plugin, "NPS_LANGUAGE");
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.messages = messages;
        this.settingsConfig = settingsConfig;
    }

    @Override
    public Button load(final YamlConfiguration configuration, final String path, final DefaultButtonValue defaultButtonValue) {
        final String languageValue = configuration.getString(path + ".language", LanguagePreference.AUTO.storageValue());
        return new LanguageButton(
            this.plugin,
            this.settingsService,
            this.messages,
            this.settingsConfig,
            LanguagePreference.fromStorage(languageValue)
        );
    }
}
