package com.stephanofer.networkplayersettingszmenu.config;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record ZMenuPluginConfig(
    CommandSection command,
    SettingsSection settings
) {

    public static ZMenuPluginConfig load(final JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        final FileConfiguration config = plugin.getConfig();

        return new ZMenuPluginConfig(
            new CommandSection(
                config.getString("command.name", "globalsettings"),
                config.getStringList("command.aliases"),
                CommandTargetType.fromConfig(config.getString("command.open.type", "menu")),
                config.getString("command.open.key", "language")
            ),
            new SettingsSection(config.getLong("settings.language-change-cooldown-millis", 750L))
        );
    }

    public record CommandSection(
        String name,
        List<String> aliases,
        CommandTargetType openTargetType,
        String openTargetKey
    ) {
        public CommandSection {
            name = normalize(name, "globalsettings");
            final String normalizedName = name;
            aliases = aliases == null
                ? List.of("settings", "prefs")
                : aliases.stream()
                    .map(alias -> alias == null ? "" : alias.trim())
                    .filter(alias -> !alias.isBlank())
                    .filter(alias -> !alias.equalsIgnoreCase(normalizedName))
                    .toList();
            openTargetType = openTargetType == null ? CommandTargetType.MENU : openTargetType;
            openTargetKey = normalize(openTargetKey, "language");
        }

        private static String normalize(final String raw, final String fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            return raw.trim();
        }
    }

    public enum CommandTargetType {
        MENU,
        DIALOG;

        public static CommandTargetType fromConfig(final String raw) {
            if (raw != null && raw.equalsIgnoreCase("dialog")) {
                return DIALOG;
            }
            return MENU;
        }
    }

    public record SettingsSection(long languageChangeCooldownMillis) {
    }
}
