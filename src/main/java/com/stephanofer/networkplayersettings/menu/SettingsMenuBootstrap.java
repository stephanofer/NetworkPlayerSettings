package com.stephanofer.networkplayersettings.menu;

import com.hera.craftkit.zmenu.ZMenuIntegration;
import com.stephanofer.networkplayersettings.NetworkPlayerSettingsPlugin;
import java.util.Objects;

public final class SettingsMenuBootstrap {

    private final NetworkPlayerSettingsPlugin plugin;
    private final ZMenuIntegration zmenu;

    public SettingsMenuBootstrap(final NetworkPlayerSettingsPlugin plugin, final ZMenuIntegration zmenu) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.zmenu = Objects.requireNonNull(zmenu, "zmenu");
    }

    public void load() {
        this.zmenu.bootstrap()
            .buttons(registry -> registry.button(new LanguageButtonLoader(this.plugin)))
            .defaultInventories("inventories/language.yml")
            .inventories("inventories")
            .dialogs("dialogs")
            .load();
    }
}
