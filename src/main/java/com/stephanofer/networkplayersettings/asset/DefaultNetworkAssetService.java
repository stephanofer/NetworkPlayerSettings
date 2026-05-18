package com.stephanofer.networkplayersettings.asset;

import com.stephanofer.networkplayersettings.api.CountryAsset;
import com.stephanofer.networkplayersettings.api.NetworkAssetService;
import java.util.Map;
import java.util.Objects;

public final class DefaultNetworkAssetService implements NetworkAssetService {

    private final CountryAssetCatalog catalog;

    public DefaultNetworkAssetService(final CountryAssetCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    @Override
    public CountryAsset countryAsset(final String codeOrAlias) {
        return this.catalog.find(codeOrAlias);
    }

    @Override
    public CountryAsset unknownCountryAsset() {
        return this.catalog.unknownCountryAsset();
    }

    @Override
    public Map<String, CountryAsset> countryAssets() {
        return this.catalog.countryAssets();
    }
}
