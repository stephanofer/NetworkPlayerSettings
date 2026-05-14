package com.stephanofer.networkplayersettings.menu;

import com.stephanofer.networkplayersettings.NetworkPlayerSettingsPlugin;
import com.stephanofer.networkplayersettings.api.LanguagePreference;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import org.bukkit.configuration.file.YamlConfiguration;

public final class LanguageButtonLoader extends ButtonLoader {

    private final NetworkPlayerSettingsPlugin plugin;

    public LanguageButtonLoader(final NetworkPlayerSettingsPlugin plugin) {
        super(plugin, "NPS_LANGUAGE");
        this.plugin = plugin;
    }

    @Override
    public Button load(final YamlConfiguration configuration, final String path, final DefaultButtonValue defaultButtonValue) {
        final String languageValue = configuration.getString(path + ".language", LanguagePreference.AUTO.storageValue());
        return new LanguageButton(this.plugin, LanguagePreference.fromStorage(languageValue));
    }
}
