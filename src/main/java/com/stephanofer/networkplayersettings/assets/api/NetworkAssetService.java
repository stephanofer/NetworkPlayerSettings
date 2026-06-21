package com.stephanofer.networkplayersettings.assets.api;

import java.util.Map;

public interface NetworkAssetService {

    CountryAsset countryAsset(String codeOrAlias);

    CountryAsset unknownCountryAsset();

    Map<String, CountryAsset> countryAssets();
}
