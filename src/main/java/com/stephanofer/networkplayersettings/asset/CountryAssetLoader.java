package com.stephanofer.networkplayersettings.asset;

import com.stephanofer.networkplayersettings.api.CountryAsset;
import com.stephanofer.networkplayersettings.api.CountryFlag;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class CountryAssetLoader {

    private static final String RUNTIME_PATH = "assets/countries.yml";
    private final Supplier<InputStream> bundledCatalogSupplier;

    public CountryAssetLoader(final Supplier<InputStream> bundledCatalogSupplier) {
        this.bundledCatalogSupplier = Objects.requireNonNull(bundledCatalogSupplier, "bundledCatalogSupplier");
    }

    public CountryAssetCatalog load(final Path dataFolder) {
        Objects.requireNonNull(dataFolder, "dataFolder");

        final Path catalogPath = dataFolder.resolve(RUNTIME_PATH);
        try {
            copyBundledCatalogIfMissing(catalogPath);
            return parseCatalog(catalogPath);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to load country catalog at " + catalogPath, exception);
        }
    }

    private void copyBundledCatalogIfMissing(final Path catalogPath) throws IOException {
        if (Files.exists(catalogPath)) {
            return;
        }

        Files.createDirectories(catalogPath.getParent());
        try (InputStream inputStream = this.bundledCatalogSupplier.get()) {
            if (inputStream == null) {
                throw new IllegalStateException("Bundled country catalog resource is missing for " + catalogPath);
            }
            Files.copy(inputStream, catalogPath);
        }
    }

    private CountryAssetCatalog parseCatalog(final Path catalogPath) {
        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(catalogPath.toFile());
        final ConfigurationSection countriesSection = yaml.getConfigurationSection("countries");
        if (countriesSection == null || countriesSection.getKeys(false).isEmpty()) {
            throw invalidCatalog(catalogPath, "missing countries section");
        }

        final List<CountryAsset> assets = countriesSection.getKeys(false).stream()
            .map(countryKey -> mapCountryAsset(catalogPath, countriesSection, countryKey))
            .toList();

        try {
            return new CountryAssetCatalog(assets);
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(catalogPath, exception.getMessage(), exception);
        }
    }

    private CountryAsset mapCountryAsset(final Path catalogPath, final ConfigurationSection countriesSection, final String countryKey) {
        final ConfigurationSection countrySection = countriesSection.getConfigurationSection(countryKey);
        if (countrySection == null) {
            throw invalidCatalog(catalogPath, "country entry is malformed: " + countryKey);
        }

        final String normalizedCode = normalizeCanonicalCode(countryKey, catalogPath);
        final String displayName = countrySection.getString("name", "");
        final String base64 = countrySection.getString("head-texture-base64", "");
        if (displayName == null || displayName.trim().isEmpty()) {
            throw invalidCatalog(catalogPath, normalizedCode + ": name must not be blank");
        }
        validateBase64(base64, normalizedCode, catalogPath);

        final Set<String> aliases = new LinkedHashSet<>(countrySection.getStringList("aliases"));
        try {
            return new CountryAsset(normalizedCode, displayName, base64, aliases);
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(catalogPath, normalizedCode + ": " + exception.getMessage(), exception);
        }
    }

    private static String normalizeCanonicalCode(final String countryKey, final Path catalogPath) {
        final String normalizedCode = countryKey == null ? "" : countryKey.trim().toUpperCase();
        if (!CountryFlag.isIsoAlpha2(normalizedCode)) {
            throw invalidCatalog(catalogPath, "invalid country code: " + countryKey);
        }
        return normalizedCode;
    }

    private static void validateBase64(final String base64, final String code, final Path catalogPath) {
        if (base64 == null || base64.trim().isEmpty()) {
            throw invalidCatalog(catalogPath, code + ": head-texture-base64 must not be blank");
        }
        try {
            Base64.getDecoder().decode(base64.trim());
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(catalogPath, code + ": invalid base64 head texture", exception);
        }
    }

    private static IllegalStateException invalidCatalog(final Path catalogPath, final String rule) {
        return new IllegalStateException("Invalid country catalog at " + catalogPath + ": " + rule);
    }

    private static IllegalStateException invalidCatalog(final Path catalogPath, final String rule, final Exception cause) {
        return new IllegalStateException("Invalid country catalog at " + catalogPath + ": " + rule, cause);
    }
}
