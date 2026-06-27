package com.stephanofer.networkplayersettingszmenu.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class StyleFilterButtonLoader extends ButtonLoader {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PluginMessages messages;
    private final StyleButtonKind kind;

    public StyleFilterButtonLoader(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PluginMessages messages,
        final StyleButtonKind kind,
        final String loaderName
    ) {
        super(plugin, loaderName);
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.messages = messages;
        this.kind = kind;
    }

    @Override
    public Button load(final YamlConfiguration configuration, final String path, final DefaultButtonValue defaultButtonValue) {
        return new StyleFilterButton(this.plugin, this.settingsService, this.messages, this.kind);
    }
}
