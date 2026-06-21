package com.stephanofer.networkplayersettings.assets.country;

import com.stephanofer.networkplayersettings.assets.api.CountryAsset;
import com.stephanofer.networkplayersettings.settings.country.CountryFlag;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class CountryAssetCatalog {

    private final Map<String, CountryAsset> canonicalAssets;
    private final Map<String, CountryAsset> aliasAssets;
    private final CountryAsset unknownCountryAsset;

    public CountryAssetCatalog(final Collection<CountryAsset> assets) {
        Objects.requireNonNull(assets, "assets");

        final Map<String, CountryAsset> canonical = new LinkedHashMap<>();
        final Map<String, CountryAsset> aliases = new LinkedHashMap<>();
        for (final CountryAsset asset : assets) {
            final CountryAsset previousByCode = canonical.putIfAbsent(asset.code(), asset);
            if (previousByCode != null) {
                throw new IllegalArgumentException("Duplicate country code: " + asset.code());
            }
        }

        for (final CountryAsset asset : assets) {
            for (final String alias : asset.aliases()) {
                if (canonical.containsKey(alias.toUpperCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Alias collides with country code: " + alias);
                }
                final CountryAsset previousByAlias = aliases.putIfAbsent(alias, asset);
                if (previousByAlias != null) {
                    throw new IllegalArgumentException("Duplicate country alias: " + alias);
                }
            }
        }

        this.unknownCountryAsset = canonical.get(CountryFlag.UNKNOWN_CODE);
        if (this.unknownCountryAsset == null) {
            throw new IllegalArgumentException("Country catalog requires XX fallback asset");
        }

        this.canonicalAssets = Map.copyOf(canonical);
        this.aliasAssets = Map.copyOf(aliases);
    }

    public CountryAsset find(final String codeOrAlias) {
        if (codeOrAlias == null) {
            return this.unknownCountryAsset;
        }

        final String trimmed = codeOrAlias.trim();
        if (trimmed.isEmpty()) {
            return this.unknownCountryAsset;
        }

        final CountryAsset byCode = this.canonicalAssets.get(trimmed.toUpperCase(Locale.ROOT));
        if (byCode != null) {
            return byCode;
        }

        return this.aliasAssets.getOrDefault(trimmed.toLowerCase(Locale.ROOT), this.unknownCountryAsset);
    }

    public CountryAsset unknownCountryAsset() {
        return this.unknownCountryAsset;
    }

    public Map<String, CountryAsset> countryAssets() {
        return this.canonicalAssets;
    }
}
