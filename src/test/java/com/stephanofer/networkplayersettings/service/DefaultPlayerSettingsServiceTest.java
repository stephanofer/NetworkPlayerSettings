package com.stephanofer.networkplayersettings.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.stephanofer.networkplayersettings.api.CountryFlag;
import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.api.SettingKey;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.country.GeoIpCountryResolver;
import com.stephanofer.networkplayersettings.language.LanguageResolver;
import com.stephanofer.networkplayersettings.repository.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.repository.RepositoryLoadResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

class DefaultPlayerSettingsServiceTest {

    private static final Logger LOGGER = Logger.getLogger(DefaultPlayerSettingsServiceTest.class.getName());

    @Test
    void returnsUnknownCountryForUncachedPlayers() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerSettingsService service = serviceWith(PlayerSettingsSnapshot.defaults(playerId));

        assertEquals(CountryFlag.UNKNOWN_CODE, service.countryCode(playerId));
    }

    @Test
    void returnsDetectedCountryFromCachedSnapshot() {
        final UUID playerId = UUID.randomUUID();
        final PlayerSettingsSnapshot snapshot = PlayerSettingsSnapshot.defaults(playerId)
            .withSetting(SettingKey.DETECTED_COUNTRY, "ar");
        final DefaultPlayerSettingsService service = serviceWith(snapshot);

        service.preloadForLogin(playerId);

        assertEquals("AR", service.countryCode(playerId));
    }

    @Test
    void prefersCountryOverrideOverDetectedCountry() {
        final UUID playerId = UUID.randomUUID();
        final PlayerSettingsSnapshot snapshot = PlayerSettingsSnapshot.defaults(playerId)
            .withSetting(SettingKey.DETECTED_COUNTRY, "ar")
            .withSetting(SettingKey.COUNTRY_OVERRIDE, "BR");
        final DefaultPlayerSettingsService service = serviceWith(snapshot);

        service.preloadForLogin(playerId);

        assertEquals("BR", service.countryCode(playerId));
    }

    private static DefaultPlayerSettingsService serviceWith(final PlayerSettingsSnapshot snapshot) {
        return new DefaultPlayerSettingsService(
            new StubPlayerSettingsRepository(snapshot),
            new LanguageResolver(Language.ENGLISH),
            pluginConfig(),
            GeoIpCountryResolver.disabled(LOGGER),
            LOGGER
        );
    }

    private static PluginConfig pluginConfig() {
        return new PluginConfig(
            new PluginConfig.DatabaseSection("127.0.0.1", 3306, "test", "root", "", "nps_", 1, 1, false),
            new PluginConfig.SettingsSection(Language.ENGLISH, true, true, 0L),
            new PluginConfig.GeoIpSection(false, "GeoLite2-Country.mmdb"),
            new PluginConfig.CommandSection("globalsettings", java.util.List.of(), PluginConfig.CommandTargetType.MENU, "language"),
            new PluginConfig.PlaceholderSection(false, 0L)
        );
    }

    private record StubPlayerSettingsRepository(PlayerSettingsSnapshot snapshot) implements PlayerSettingsRepository {

        @Override
        public RepositoryLoadResult loadOrCreate(final UUID playerId) {
            return new RepositoryLoadResult(this.snapshot, false);
        }

        @Override
        public PlayerSettingsSnapshot load(final UUID playerId) {
            return this.snapshot;
        }

        @Override
        public CompletableFuture<PlayerSettingsSnapshot> loadAsync(final UUID playerId) {
            return CompletableFuture.completedFuture(this.snapshot);
        }

        @Override
        public CompletableFuture<RepositoryLoadResult> loadOrCreateAsync(final UUID playerId) {
            return CompletableFuture.completedFuture(new RepositoryLoadResult(this.snapshot, false));
        }

        @Override
        public void upsert(final UUID playerId, final SettingKey key, final String value) {
        }

        @Override
        public CompletableFuture<Void> upsertAsync(final UUID playerId, final SettingKey key, final String value) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
