package com.stephanofer.networkplayersettingszmenu.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClearStyleButtonLoader extends ButtonLoader {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PlayerStyleService styleService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;
    private final StyleButtonKind kind;

    public ClearStyleButtonLoader(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PlayerStyleService styleService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig,
        final StyleButtonKind kind,
        final String loaderName
    ) {
        super(plugin, loaderName);
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.styleService = styleService;
        this.messages = messages;
        this.settingsConfig = settingsConfig;
        this.kind = kind;
    }

    @Override
    public Button load(final YamlConfiguration configuration, final String path, final DefaultButtonValue defaultButtonValue) {
        defaultButtonValue.setPermanent(true);
        return new ClearStyleButton(this.plugin, this.settingsService, this.styleService, this.messages, this.settingsConfig, this.kind);
    }
}
