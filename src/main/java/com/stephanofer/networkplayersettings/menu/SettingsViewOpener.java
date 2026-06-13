package com.stephanofer.networkplayersettings.menu;

import com.stephanofer.networkplayersettings.config.PluginConfig;
import fr.maxlego08.menu.api.DialogInventory;
import fr.maxlego08.menu.api.DialogManager;
import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.exceptions.DialogException;
import fr.maxlego08.menu.api.exceptions.InventoryException;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class SettingsViewOpener {

    private final JavaPlugin plugin;
    private final InventoryManager inventoryManager;
    private final DialogManager dialogManager;
    private final Logger logger;

    public SettingsViewOpener(
        final JavaPlugin plugin,
        final InventoryManager inventoryManager,
        final DialogManager dialogManager,
        final Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.inventoryManager = Objects.requireNonNull(inventoryManager, "inventoryManager");
        this.dialogManager = Objects.requireNonNull(dialogManager, "dialogManager");
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

    public void loadConfiguredTarget(final PluginConfig.CommandSection commandConfig) throws InventoryException, DialogException {
        Objects.requireNonNull(commandConfig, "commandConfig");

        switch (commandConfig.openTargetType()) {
            case MENU -> loadMenuTarget(commandConfig.openTargetKey());
            case DIALOG -> loadDialogTarget(commandConfig.openTargetKey());
        }
    }

    public void loadMenuTarget(final String targetKey) throws InventoryException {
        loadMenu(targetKey);
    }

    public void loadDialogTarget(final String targetKey) throws DialogException, InventoryException {
        loadDialog(targetKey);
    }

    private boolean openMenu(final Player player, final String targetKey) {
        Optional<Inventory> inventory = this.inventoryManager.getInventory(this.plugin, targetKey);
        if (inventory.isEmpty()) {
            inventory = tryLoadMenu(targetKey);
        }
        inventory.ifPresent(loaded -> this.inventoryManager.openInventory(player, loaded));
        return inventory.isPresent();
    }

    private boolean openDialog(final Player player, final String targetKey) {
        Optional<DialogInventory> dialog = this.dialogManager.getDialog(this.plugin, targetKey);
        if (dialog.isEmpty()) {
            dialog = tryLoadDialog(targetKey);
        }
        dialog.ifPresent(loaded -> this.dialogManager.openDialog(player, loaded));
        return dialog.isPresent();
    }

    private Optional<Inventory> tryLoadMenu(final String targetKey) {
        try {
            return Optional.of(loadMenu(targetKey));
        } catch (final InventoryException exception) {
            this.logger.log(Level.WARNING, "Failed to load zMenu inventory '" + targetKey + "'.", exception);
            return Optional.empty();
        }
    }

    private Optional<DialogInventory> tryLoadDialog(final String targetKey) {
        try {
            return Optional.of(loadDialog(targetKey));
        } catch (final DialogException | InventoryException exception) {
            this.logger.log(Level.WARNING, "Failed to load zMenu dialog '" + targetKey + "'.", exception);
            return Optional.empty();
        }
    }

    private Inventory loadMenu(final String targetKey) throws InventoryException {
        return this.inventoryManager.loadInventory(this.plugin, resolvePluginFile("inventories/" + targetKey + ".yml"));
    }

    private DialogInventory loadDialog(final String targetKey) throws DialogException, InventoryException {
        return this.dialogManager.loadInventory(this.plugin, resolvePluginFile("dialogs/" + targetKey + ".yml"));
    }

    private File resolvePluginFile(final String relativePath) {
        return this.plugin.getDataFolder().toPath().resolve(relativePath).toFile();
    }
}
