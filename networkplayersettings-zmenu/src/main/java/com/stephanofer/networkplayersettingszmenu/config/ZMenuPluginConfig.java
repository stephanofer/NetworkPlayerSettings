package com.stephanofer.networkplayersettingszmenu.config;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record ZMenuPluginConfig(
    CommandSection command,
    SettingsSection settings
) {

    private static final Pattern COMMAND_TOKEN = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern TARGET_KEY = Pattern.compile("[a-zA-Z0-9_./-]+");

    public static ZMenuPluginConfig load(final JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        final FileConfiguration config = plugin.getConfig();
        final Logger logger = plugin.getLogger();

        return new ZMenuPluginConfig(
            new CommandSection(
                normalizeCommandToken(config.getString("command.name", "globalsettings"), "globalsettings", "command.name", logger),
                normalizeAliases(config.getStringList("command.aliases"), "globalsettings", logger),
                CommandTargetType.fromConfig(config.getString("command.open.type", "menu"), logger),
                normalizeTargetKey(config.getString("command.open.key", "language"), "command.open.key", logger)
            ),
            new SettingsSection(
                config.getLong("settings.language-change-cooldown-millis", 750L),
                config.getLong("settings.country-flag-toggle-cooldown-millis", 500L)
            )
        );
    }

    public record CommandSection(
        String name,
        List<String> aliases,
        CommandTargetType openTargetType,
        String openTargetKey
    ) {
        public CommandSection {
            final String normalizedName = name;
            aliases = aliases == null
                ? List.of("settings", "prefs")
                : aliases.stream()
                    .map(alias -> normalizeCommandToken(alias, "", "command.aliases", null))
                    .filter(alias -> !alias.isBlank())
                    .filter(alias -> !alias.equalsIgnoreCase(normalizedName))
                    .toList();
            if (aliases.isEmpty()) {
                aliases = List.of("settings", "prefs").stream()
                    .filter(alias -> !alias.equalsIgnoreCase(normalizedName))
                    .toList();
            }
            openTargetType = openTargetType == null ? CommandTargetType.MENU : openTargetType;
        }
    }

    public enum CommandTargetType {
        MENU,
        DIALOG;

        public static CommandTargetType fromConfig(final String raw, final Logger logger) {
            if (raw != null && raw.equalsIgnoreCase("dialog")) {
                return DIALOG;
            }
            if (raw != null && !raw.isBlank() && !raw.equalsIgnoreCase("menu") && logger != null) {
                logger.warning("Invalid command.open.type '" + raw + "'. Falling back to menu.");
            }
            return MENU;
        }
    }

    public record SettingsSection(
        long languageChangeCooldownMillis,
        long countryFlagToggleCooldownMillis
    ) {
        public SettingsSection {
            languageChangeCooldownMillis = Math.max(0L, languageChangeCooldownMillis);
            countryFlagToggleCooldownMillis = Math.max(0L, countryFlagToggleCooldownMillis);
        }
    }

    private static List<String> normalizeAliases(final List<String> rawAliases, final String commandName, final Logger logger) {
        final List<String> aliases = rawAliases.stream()
            .map(alias -> normalizeCommandToken(alias, "", "command.aliases", logger))
            .filter(alias -> !alias.isBlank())
            .filter(alias -> !alias.equalsIgnoreCase(commandName))
            .toList();
        if (!aliases.isEmpty()) {
            return aliases;
        }
        return List.of("settings", "prefs").stream()
            .filter(alias -> !alias.equalsIgnoreCase(commandName))
            .toList();
    }

    private static String normalizeCommandToken(final String raw, final String fallback, final String path, final Logger logger) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (COMMAND_TOKEN.matcher(normalized).matches()) {
            return normalized;
        }
        if (logger != null) {
            logger.warning("Invalid " + path + " value '" + raw + "'. Falling back to '" + fallback + "'.");
        }
        return fallback;
    }

    private static String normalizeTargetKey(final String raw, final String path, final Logger logger) {
        if (raw == null || raw.isBlank()) {
            return "language";
        }
        final String normalized = raw.trim();
        if (TARGET_KEY.matcher(normalized).matches()) {
            return normalized;
        }
        if (logger != null) {
            logger.warning("Invalid " + path + " value '" + raw + "'. Falling back to 'language'.");
        }
        return "language";
    }
}
