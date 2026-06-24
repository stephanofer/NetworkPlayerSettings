package com.stephanofer.networkplayersettingszmenu.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public record ZMenuPluginConfig(
    CommandSection command,
    CommandSection nickStyleCommand,
    CommandSection chatStyleCommand,
    SettingsSection settings
) {

    private static final Pattern COMMAND_TOKEN = Pattern.compile("[a-z0-9_-]+");
    private static final Pattern TARGET_KEY = Pattern.compile("[a-zA-Z0-9_./-]+");

    public static ZMenuPluginConfig fromDocument(final YamlDocument document, final Logger logger) {
        final long mutationCooldownMillis = resolveMutationCooldownMillis(document);

        return new ZMenuPluginConfig(
            commandSection(document, logger, "command", "globalsettings", List.of("settings", "prefs"), "settings-main"),
            commandSection(document, logger, "nick-style-command", "nickstyle", List.of("nickstyles"), "nick-styles"),
            commandSection(document, logger, "chat-style-command", "chatstyle", List.of("chatstyles"), "chat-styles"),
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
                ? List.of()
                : aliases.stream()
                    .map(alias -> normalizeCommandToken(alias, "", "command.aliases", null))
                    .filter(alias -> !alias.isBlank())
                    .filter(alias -> !alias.equalsIgnoreCase(normalizedName))
                    .distinct()
                    .toList();
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

    private static CommandSection commandSection(
        final YamlDocument document,
        final Logger logger,
        final String path,
        final String fallbackName,
        final List<String> fallbackAliases,
        final String fallbackTargetKey
    ) {
        final String name = normalizeCommandToken(document.getString(path + ".name", fallbackName), fallbackName, path + ".name", logger);
        return new CommandSection(
            name,
            normalizeAliases(document.getStringList(path + ".aliases"), name, fallbackAliases, path + ".aliases", logger),
            CommandTargetType.fromConfig(document.getString(path + ".open.type", "menu"), logger),
            normalizeTargetKey(document.getString(path + ".open.key", fallbackTargetKey), path + ".open.key", logger)
        );
    }

    private static List<String> normalizeAliases(
        final List<String> rawAliases,
        final String commandName,
        final List<String> fallbackAliases,
        final String path,
        final Logger logger
    ) {
        final List<String> aliases = rawAliases.stream()
            .map(alias -> normalizeCommandToken(alias, "", path, logger))
            .filter(alias -> !alias.isBlank())
            .filter(alias -> !alias.equalsIgnoreCase(commandName))
            .distinct()
            .toList();
        if (!aliases.isEmpty()) {
            return aliases;
        }
        return fallbackAliases.stream()
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
            return "settings-main";
        }
        final String normalized = raw.trim();
        if (TARGET_KEY.matcher(normalized).matches()) {
            return normalized;
        }
        if (logger != null) {
            logger.warning("Invalid " + path + " value '" + raw + "'. Falling back to 'settings-main'.");
        }
        return "settings-main";
    }
}
