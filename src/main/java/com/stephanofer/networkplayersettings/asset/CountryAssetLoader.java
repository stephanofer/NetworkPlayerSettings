package com.stephanofer.networkplayersettings.asset;

import com.stephanofer.networkplayersettings.api.CountryAsset;
import com.stephanofer.networkplayersettings.api.CountryFlag;
import com.stephanofer.networkplatform.paper.config.ConfigService;
import com.stephanofer.networkplatform.paper.libs.boostedyaml.YamlDocument;
import com.stephanofer.networkplatform.paper.libs.boostedyaml.block.implementation.Section;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class CountryAssetLoader {

    private static final String CONFIG_PATH = "assets/countries.yml";

    public CountryAssetLoader() {
    }

    public CountryAssetCatalog load(final ConfigService configService) {
        Objects.requireNonNull(configService, "configService");
        final YamlDocument document = configService.load(CONFIG_PATH);
        return parseCatalog(document);
    }

    private CountryAssetCatalog parseCatalog(final YamlDocument document) {
        final Section countriesSection = document.getSection("countries");
        if (countriesSection == null || countriesSection.getKeys().isEmpty()) {
            throw invalidCatalog(document, "missing countries section");
        }

        final List<CountryAsset> assets = countriesSection.getKeys().stream()
            .map(countryKey -> mapCountryAsset(document, countriesSection, String.valueOf(countryKey)))
            .toList();

        try {
            return new CountryAssetCatalog(assets);
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(document, exception.getMessage(), exception);
        }
    }

    private CountryAsset mapCountryAsset(final YamlDocument document, final Section countriesSection, final String countryKey) {
        final Section countrySection = countriesSection.getSection(countryKey);
        if (countrySection == null) {
            throw invalidCatalog(document, "country entry is malformed: " + countryKey);
        }

        final String normalizedCode = normalizeCanonicalCode(countryKey, document);
        final String displayName = countrySection.getString("name", "");
        final String base64 = countrySection.getString("head-texture-base64", "");
        if (displayName == null || displayName.trim().isEmpty()) {
            throw invalidCatalog(document, normalizedCode + ": name must not be blank");
        }
        validateBase64(base64, normalizedCode, document);

        final Set<String> aliases = new LinkedHashSet<>(countrySection.getStringList("aliases"));
        try {
            return new CountryAsset(normalizedCode, displayName, base64, aliases);
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(document, normalizedCode + ": " + exception.getMessage(), exception);
        }
    }

    private static String normalizeCanonicalCode(final String countryKey, final YamlDocument document) {
        final String normalizedCode = countryKey == null ? "" : countryKey.trim().toUpperCase();
        if (!CountryFlag.isIsoAlpha2(normalizedCode)) {
            throw invalidCatalog(document, "invalid country code: " + countryKey);
        }
        return normalizedCode;
    }

    private static void validateBase64(final String base64, final String code, final YamlDocument document) {
        if (base64 == null || base64.trim().isEmpty()) {
            throw invalidCatalog(document, code + ": head-texture-base64 must not be blank");
        }
        try {
            Base64.getDecoder().decode(base64.trim());
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(document, code + ": invalid base64 head texture", exception);
        }
    }

    private static IllegalStateException invalidCatalog(final YamlDocument document, final String rule) {
        return new IllegalStateException("Invalid country catalog at " + document.getFile().toPath() + ": " + rule);
    }

    private static IllegalStateException invalidCatalog(final YamlDocument document, final String rule, final Exception cause) {
        return new IllegalStateException("Invalid country catalog at " + document.getFile().toPath() + ": " + rule, cause);
    }
}
