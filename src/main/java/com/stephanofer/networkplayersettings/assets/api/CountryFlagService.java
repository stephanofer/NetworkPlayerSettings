package com.stephanofer.networkplayersettings.assets.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public interface CountryFlagService {

    String COUNTRY_FLAG_TAG = "country_flag";

    CountryAsset asset(UUID playerId);

    CountryAsset assetForCountry(String countryCodeOrAlias);

    String headTextureValue(UUID playerId);

    String headTextureValueForCountry(String countryCodeOrAlias);

    String miniMessageTag(UUID playerId);

    String miniMessageTagForCountry(String countryCodeOrAlias);

    Component flag(UUID playerId);

    CompletableFuture<Component> flagAsync(UUID playerId);

    Component flagForCountry(String countryCodeOrAlias);

    TagResolver resolver(UUID playerId);

    TagResolver resolverForCountry(String countryCodeOrAlias);
}
