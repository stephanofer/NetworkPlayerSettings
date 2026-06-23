package com.stephanofer.networkplayersettings.assets.country;

import com.stephanofer.networkplayersettings.assets.api.CountryAsset;
import com.stephanofer.networkplayersettings.assets.api.CountryFlagService;
import com.stephanofer.networkplayersettings.assets.api.NetworkAssetService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;

public final class DefaultCountryFlagService implements CountryFlagService {

    private static final String TEXTURES_PROPERTY = "textures";

    private final PlayerSettingsService settingsService;
    private final NetworkAssetService assetService;

    public DefaultCountryFlagService(final PlayerSettingsService settingsService, final NetworkAssetService assetService) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.assetService = Objects.requireNonNull(assetService, "assetService");
    }

    @Override
    public CountryAsset asset(final UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        return this.assetService.countryAsset(this.settingsService.countryCode(playerId));
    }

    @Override
    public CountryAsset assetForCountry(final String countryCodeOrAlias) {
        return this.assetService.countryAsset(countryCodeOrAlias);
    }

    @Override
    public String headTextureValue(final UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        if (!this.settingsService.showCountryFlag(playerId)) {
            return "";
        }
        return asset(playerId).headTextureBase64();
    }

    @Override
    public String headTextureValueForCountry(final String countryCodeOrAlias) {
        return assetForCountry(countryCodeOrAlias).headTextureBase64();
    }

    @Override
    public String miniMessageTag(final UUID playerId) {
        final String value = headTextureValue(playerId);
        return value.isEmpty() ? "" : miniMessageTagForTexture(value);
    }

    @Override
    public String miniMessageTagForCountry(final String countryCodeOrAlias) {
        return miniMessageTagForTexture(headTextureValueForCountry(countryCodeOrAlias));
    }

    @Override
    public Component flag(final UUID playerId) {
        final String value = headTextureValue(playerId);
        return value.isEmpty() ? Component.empty() : flagForTexture(value);
    }

    @Override
    public Component flagForCountry(final String countryCodeOrAlias) {
        return flagForTexture(headTextureValueForCountry(countryCodeOrAlias));
    }

    @Override
    public TagResolver resolver(final UUID playerId) {
        return TagResolver.resolver(COUNTRY_FLAG_TAG, Tag.selfClosingInserting(flag(playerId)));
    }

    @Override
    public TagResolver resolverForCountry(final String countryCodeOrAlias) {
        return TagResolver.resolver(COUNTRY_FLAG_TAG, Tag.selfClosingInserting(flagForCountry(countryCodeOrAlias)));
    }

    private static String miniMessageTagForTexture(final String value) {
        return "<craftkit_head:" + value + ">";
    }

    private static Component flagForTexture(final String value) {
        return Component.object(ObjectContents.playerHead()
            .profileProperty(PlayerHeadObjectContents.property(TEXTURES_PROPERTY, value))
            .build());
    }
}
