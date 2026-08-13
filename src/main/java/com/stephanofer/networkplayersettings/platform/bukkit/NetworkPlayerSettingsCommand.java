package com.stephanofer.networkplayersettings.platform.bukkit;

import com.stephanofer.networkplayersettings.NetworkPlayerSettingsPlugin;
import java.util.List;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class NetworkPlayerSettingsCommand implements CommandExecutor, TabCompleter {

    private static final String RELOAD_PERMISSION = "networkplayersettings.command.reload";

    private final NetworkPlayerSettingsPlugin plugin;

    public NetworkPlayerSettingsCommand(final NetworkPlayerSettingsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public boolean onCommand(
        final CommandSender sender,
        final Command command,
        final String label,
        final String[] arguments
    ) {
        if (arguments.length != 1 || !arguments[0].equalsIgnoreCase("reload")) {
            sender.sendRichMessage("<yellow>Usage: /" + label + " reload");
            return true;
        }
        if (!sender.hasPermission(RELOAD_PERMISSION)) {
            sender.sendRichMessage("<red>You do not have permission to use this command.");
            return true;
        }

        try {
            this.plugin.reloadRuntimeResources();
            sender.sendRichMessage("<green>Reloaded country assets and style catalogs. Startup-only configuration was not changed.");
        } catch (final RuntimeException exception) {
            this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to reload runtime resources", exception);
            sender.sendRichMessage("<red>Reload failed. The previous runtime resources remain active. Check the console.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(
        final CommandSender sender,
        final Command command,
        final String alias,
        final String[] arguments
    ) {
        if (arguments.length == 1 && sender.hasPermission(RELOAD_PERMISSION)
            && "reload".startsWith(arguments[0].toLowerCase(java.util.Locale.ROOT))) {
            return List.of("reload");
        }
        return List.of();
    }
}
