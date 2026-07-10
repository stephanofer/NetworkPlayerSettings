package com.stephanofer.networkplayersettings.assets.country;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stephanofer.networkplayersettings.assets.api.CountryAsset;
import com.stephanofer.networkplayersettings.assets.api.CountryFlagService;
import com.stephanofer.networkplayersettings.assets.api.NetworkAssetService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.settings.api.SettingKey;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettings.settings.language.LanguagePreference;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

class DefaultCountryFlagServiceTest {

    private static final String UNKNOWN_TEXTURE = "unknown-texture";
    private static final String PERU_TEXTURE = "peru-texture";

    @Test
    void returnsPlayerCountryTextureWhenFlagIsEnabled() {
        final UUID playerId = UUID.randomUUID();
        final CountryFlagService service = service(playerId, "PE", true);

        assertEquals(PERU_TEXTURE, service.headTextureValue(playerId));
        assertEquals("<craftkit_head:" + PERU_TEXTURE + ">", service.miniMessageTag(playerId));
    }

    @Test
    void returnsEmptyPlayerValuesWhenFlagIsDisabled() {
        final UUID playerId = UUID.randomUUID();
        final CountryFlagService service = service(playerId, "PE", false);

        assertEquals("", service.headTextureValue(playerId));
        assertEquals("", service.miniMessageTag(playerId));
        assertEquals(Component.empty(), service.flag(playerId));
    }

    @Test
    void treatsUnknownCountryAsRenderableAsset() {
        final UUID playerId = UUID.randomUUID();
        final CountryFlagService service = service(playerId, "XX", true);

        assertEquals(UNKNOWN_TEXTURE, service.headTextureValue(playerId));
        assertEquals("<craftkit_head:" + UNKNOWN_TEXTURE + ">", service.miniMessageTag(playerId));
    }

    @Test
    void countryHelpersIgnorePlayerVisibility() {
        final UUID playerId = UUID.randomUUID();
        final CountryFlagService service = service(playerId, "PE", false);

        assertEquals(PERU_TEXTURE, service.headTextureValueForCountry("peru"));
        assertEquals("<craftkit_head:" + PERU_TEXTURE + ">", service.miniMessageTagForCountry("PE"));
    }

    @Test
    void flagComponentUsesTexturesProfileProperty() {
        final UUID playerId = UUID.randomUUID();
        final CountryFlagService service = service(playerId, "PE", true);

        final ObjectComponent component = assertInstanceOf(ObjectComponent.class, service.flag(playerId));
        final PlayerHeadObjectContents contents = assertInstanceOf(PlayerHeadObjectContents.class, component.contents());

        assertEquals(1, contents.profileProperties().size());
        assertEquals("textures", contents.profileProperties().getFirst().name());
        assertEquals(PERU_TEXTURE, contents.profileProperties().getFirst().value());
        assertTrue(contents.hat());
    }

    @Test
    void resolvesOfflineFlagFromLoadedSettings() {
        final UUID playerId = UUID.randomUUID();
        final CountryFlagService service = service(playerId, "PE", true);

        final ObjectComponent component = assertInstanceOf(ObjectComponent.class, service.flagAsync(playerId).join());
        final PlayerHeadObjectContents contents = assertInstanceOf(PlayerHeadObjectContents.class, component.contents());

        assertEquals(PERU_TEXTURE, contents.profileProperties().getFirst().value());
    }

    @Test
    void hidesOfflineFlagWhenLoadedSettingsDisableIt() {
        final UUID playerId = UUID.randomUUID();
        final CountryFlagService service = service(playerId, "PE", false);

        assertEquals(Component.empty(), service.flagAsync(playerId).join());
    }

    private static CountryFlagService service(final UUID playerId, final String countryCode, final boolean showFlag) {
        final CountryAsset unknown = new CountryAsset("XX", "Unknown", UNKNOWN_TEXTURE, Set.of("unknown"));
        final CountryAsset peru = new CountryAsset("PE", "Peru", PERU_TEXTURE, Set.of("peru"));
        final NetworkAssetService assetService = new DefaultNetworkAssetService(new CountryAssetCatalog(List.of(unknown, peru)));
        return new DefaultCountryFlagService(new FixedPlayerSettingsService(playerId, countryCode, showFlag), assetService);
    }

    private static final class FixedPlayerSettingsService implements PlayerSettingsService {

        private final UUID playerId;
        private final String countryCode;
        private final boolean showFlag;

        private FixedPlayerSettingsService(final UUID playerId, final String countryCode, final boolean showFlag) {
            this.playerId = playerId;
            this.countryCode = countryCode;
            this.showFlag = showFlag;
        }

        @Override
        public CompletableFuture<PlayerSettingsSnapshot> load(final UUID playerId) {
            return CompletableFuture.completedFuture(getCachedOrDefault(playerId));
        }

        @Override
        public Optional<PlayerSettingsSnapshot> cached(final UUID playerId) {
            return Optional.of(getCachedOrDefault(playerId));
        }

        @Override
        public PlayerSettingsSnapshot getCachedOrDefault(final UUID playerId) {
            return new PlayerSettingsSnapshot(this.playerId, Map.of(
                SettingKey.DETECTED_COUNTRY, this.countryCode,
                SettingKey.SHOW_COUNTRY_FLAG, Boolean.toString(this.showFlag)
            ));
        }

        @Override
        public Language resolvedLanguage(final Player player) {
            return Language.ENGLISH;
        }

        @Override
        public LanguagePreference languagePreference(final UUID playerId) {
            return LanguagePreference.AUTO;
        }

        @Override
        public String countryCode(final UUID playerId) {
            return this.countryCode;
        }

        @Override
        public boolean showCountryFlag(final UUID playerId) {
            return this.showFlag;
        }

        @Override
        public CompletableFuture<Void> setLanguage(final UUID playerId, final LanguagePreference preference) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> setCountryOverride(final UUID playerId, final String countryCode) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> clearCountryOverride(final UUID playerId) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> setShowCountryFlag(final UUID playerId, final boolean enabled) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> setSetting(final UUID playerId, final SettingKey key, final String value) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Optional<String> getSetting(final UUID playerId, final SettingKey key) {
            return getCachedOrDefault(playerId).setting(key);
        }

        @Override
        public boolean isReady(final UUID playerId) {
            return true;
        }
    }
}
