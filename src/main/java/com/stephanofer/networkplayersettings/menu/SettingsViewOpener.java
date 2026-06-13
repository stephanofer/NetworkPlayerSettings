package com.stephanofer.networkplayersettings.menu;

import com.hera.craftkit.zmenu.ZMenuIntegration;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import fr.maxlego08.menu.api.DialogInventory;
import fr.maxlego08.menu.api.DialogManager;
import fr.maxlego08.menu.api.Inventory;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SettingsViewOpener {

    private final JavaPlugin plugin;
    private final ZMenuIntegration zmenu;
    private final Logger logger;

    public SettingsViewOpener(
        final JavaPlugin plugin,
        final ZMenuIntegration zmenu,
        final Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.zmenu = Objects.requireNonNull(zmenu, "zmenu");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public boolean open(final Player player, final PluginConfig.CommandSection commandConfig) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(commandConfig, "commandConfig");

        return switch (commandConfig.openTargetType()) {
            case MENU -> openMenu(player, commandConfig.openTargetKey());
            case DIALOG -> openDialog(player, commandConfig.openTargetKey());
        };
    }

    private boolean openMenu(final Player player, final String targetKey) {
        final Optional<Inventory> inventory = this.zmenu.inventories().getInventory(this.plugin, targetKey);
        if (inventory.isEmpty()) {
            return false;
        }

        this.zmenu.open(player, targetKey);
        return true;
    }

    private boolean openDialog(final Player player, final String targetKey) {
        final Optional<DialogManager> dialogs = this.zmenu.dialogs();
        if (dialogs.isEmpty()) {
            this.logger.warning("Dialog target '" + targetKey + "' was requested, but zMenu did not expose DialogManager.");
            return false;
        }

        final DialogManager dialogManager = dialogs.get();
        final Optional<DialogInventory> dialog = dialogManager.getDialog(this.plugin, targetKey);
        if (dialog.isEmpty()) {
            return false;
        }

        dialogManager.openDialog(player, dialog.get());
        return true;
    }
}
