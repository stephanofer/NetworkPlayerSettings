package com.stephanofer.networkplayersettingszmenu.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public record ZMenuPluginConfig(
    CommandSection command,
    SettingsSection settings
) {

    private static final Pattern COMMAND_TOKEN = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern TARGET_KEY = Pattern.compile("[a-zA-Z0-9_./-]+");

    public static ZMenuPluginConfig fromDocument(final YamlDocument document, final Logger logger) {
        final long mutationCooldownMillis = resolveMutationCooldownMillis(document);

        return new ZMenuPluginConfig(
            new CommandSection(
                normalizeCommandToken(document.getString("command.name", "globalsettings"), "globalsettings", "command.name", logger),
                normalizeAliases(document.getStringList("command.aliases"), "globalsettings", logger),
                CommandTargetType.fromConfig(document.getString("command.open.type", "menu"), logger),
                normalizeTargetKey(document.getString("command.open.key", "language"), "command.open.key", logger)
            ),
            new SettingsSection(
                mutationCooldownMillis
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
                    .distinct()
                    .toList();
            if (aliases.isEmpty()) {
                aliases = List.of("settings", "prefs").stream()
                    .filter(alias -> !alias.equalsIgnoreCase(normalizedName))
                    .distinct()
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
        long mutationCooldownMillis
    ) {
        public SettingsSection {
            mutationCooldownMillis = Math.max(0L, mutationCooldownMillis);
        }
    }

    private static List<String> normalizeAliases(final List<String> rawAliases, final String commandName, final Logger logger) {
        final List<String> aliases = rawAliases.stream()
            .map(alias -> normalizeCommandToken(alias, "", "command.aliases", logger))
            .filter(alias -> !alias.isBlank())
            .filter(alias -> !alias.equalsIgnoreCase(commandName))
            .distinct()
            .toList();
        if (!aliases.isEmpty()) {
            return aliases;
        }
        return List.of("settings", "prefs").stream()
            .filter(alias -> !alias.equalsIgnoreCase(commandName))
            .distinct()
            .toList();
    }

    private static long resolveMutationCooldownMillis(final YamlDocument document) {
        final long unified = document.getLong("settings.mutation-cooldown-millis", Long.MIN_VALUE);
        if (unified != Long.MIN_VALUE) {
            return unified;
        }

        final long language = document.getLong("settings.language-change-cooldown-millis", 750L);
        final long countryFlag = document.getLong("settings.country-flag-toggle-cooldown-millis", 500L);
        return Math.max(language, countryFlag);
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
