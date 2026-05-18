package com.stephanofer.networkplayersettings.api;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class CountryAsset {

    private final String code;
    private final String displayName;
    private final String headTextureBase64;
    private final Set<String> aliases;

    public CountryAsset(final String code, final String displayName, final String headTextureBase64, final Set<String> aliases) {
        this.code = normalizeCode(code);
        this.displayName = requireNonBlank(displayName, "displayName");
        this.headTextureBase64 = requireNonBlank(headTextureBase64, "headTextureBase64");
        this.aliases = normalizeAliases(aliases);
    }

    public String code() {
        return this.code;
    }

    public String displayName() {
        return this.displayName;
    }

    public String headTextureBase64() {
        return this.headTextureBase64;
    }

    public Set<String> aliases() {
        return this.aliases;
    }

    private static String normalizeCode(final String rawCode) {
        Objects.requireNonNull(rawCode, "code");
        final String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        if (!CountryFlag.isIsoAlpha2(normalized)) {
            throw new IllegalArgumentException("country code must be ISO alpha-2: " + rawCode);
        }
        return normalized;
    }

    private static String requireNonBlank(final String value, final String fieldName) {
        Objects.requireNonNull(value, fieldName);
        final String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static Set<String> normalizeAliases(final Set<String> rawAliases) {
        Objects.requireNonNull(rawAliases, "aliases");
        final Set<String> normalized = new LinkedHashSet<>();
        for (final String alias : rawAliases) {
            final String normalizedAlias = requireNonBlank(alias, "alias").toLowerCase(Locale.ROOT);
            normalized.add(normalizedAlias);
        }
        return Set.copyOf(normalized);
    }
}
