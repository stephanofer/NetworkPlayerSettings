package com.stephanofer.networkplayersettings.settings.language;

import java.util.Locale;
import java.util.Objects;

public final class LanguageResolver {

    private final Language defaultLanguage;

    public LanguageResolver(final Language defaultLanguage) {
        this.defaultLanguage = Objects.requireNonNull(defaultLanguage, "defaultLanguage");
    }

    public Language resolve(final LanguagePreference preference, final Locale locale) {
        return resolve(preference, locale == null ? null : locale.toString());
    }

    public Language resolve(final LanguagePreference preference, final String rawLocale) {
        Objects.requireNonNull(preference, "preference");
        return switch (preference) {
            case SPANISH -> Language.SPANISH;
            case ENGLISH -> Language.ENGLISH;
            case AUTO -> resolveLocale(rawLocale);
        };
    }

    public String normalizeLocale(final Locale locale) {
        return normalizeLocale(locale == null ? null : locale.toString());
    }

    public String normalizeLocale(final String rawLocale) {
        if (rawLocale == null || rawLocale.isBlank()) {
            return "";
        }
        return rawLocale.trim().replace('-', '_').toLowerCase(Locale.ROOT);
    }

    private Language resolveLocale(final String rawLocale) {
        final String locale = normalizeLocale(rawLocale);
        if (locale.startsWith("es_") || locale.equals("es")) {
            return Language.SPANISH;
        }
        if (locale.startsWith("en_") || locale.equals("en")) {
            return Language.ENGLISH;
        }
        return this.defaultLanguage;
    }
}
