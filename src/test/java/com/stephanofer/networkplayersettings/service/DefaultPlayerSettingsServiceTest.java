package com.stephanofer.networkplayersettings.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.stephanofer.networkplayersettings.api.CountryFlag;
import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.LanguagePreference;
import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.api.SettingKey;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.country.GeoIpCountryResolver;
import com.stephanofer.networkplayersettings.country.GeoIpCountryResolver.CountryLookup;
import com.stephanofer.networkplayersettings.language.LanguageResolver;
import com.stephanofer.networkplayersettings.repository.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.repository.RepositoryLoadResult;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
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

    @Test
    void updatesDetectedCountryWhenRealCountryChanges() throws Exception {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "PE"
        )));
        final DefaultPlayerSettingsService service = serviceWith(repository, resolver(CountryLookup.detected("AR")), geoIpEnabledConfig());

        service.preloadForConnection(playerId, InetAddress.getByName("8.8.8.8"));

        assertEquals("AR", service.countryCode(playerId));
        assertEquals(List.of(new PersistedSetting(SettingKey.DETECTED_COUNTRY, "AR")), repository.syncUpserts);
    }

    @Test
    void doesNotPersistDetectedCountryWhenRealCountryIsUnchanged() throws Exception {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "PE"
        )));
        final DefaultPlayerSettingsService service = serviceWith(repository, resolver(CountryLookup.detected("PE")), geoIpEnabledConfig());

        service.preloadForConnection(playerId, InetAddress.getByName("8.8.8.8"));

        assertEquals("PE", service.countryCode(playerId));
        assertFalse(repository.syncUpserts.stream().anyMatch(upsert -> upsert.key() == SettingKey.DETECTED_COUNTRY));
    }

    @Test
    void doesNotOverwriteRealCountryWithUnknownDetection() throws Exception {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "PE"
        )));
        final DefaultPlayerSettingsService service = serviceWith(
            repository,
            resolver(CountryLookup.unknown(GeoIpCountryResolver.UnknownReason.NON_PUBLIC_ADDRESS)),
            geoIpEnabledConfig()
        );

        service.preloadForConnection(playerId, InetAddress.getLoopbackAddress());

        assertEquals("PE", service.countryCode(playerId));
        assertFalse(repository.syncUpserts.stream().anyMatch(upsert -> upsert.key() == SettingKey.DETECTED_COUNTRY));
    }

    @Test
    void upgradesUnknownCountryWhenRealCountryIsDetected() throws Exception {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE
        )));
        final DefaultPlayerSettingsService service = serviceWith(repository, resolver(CountryLookup.detected("PE")), geoIpEnabledConfig());

        service.preloadForConnection(playerId, InetAddress.getByName("8.8.8.8"));

        assertEquals("PE", service.countryCode(playerId));
        assertEquals(List.of(new PersistedSetting(SettingKey.DETECTED_COUNTRY, "PE")), repository.syncUpserts);
    }

    @Test
    void resolvesAutoLanguageFromCurrentPlayerLocale() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerSettingsService service = serviceWith(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE
        )));

        service.preloadForLogin(playerId);

        assertEquals(Language.SPANISH, service.resolvedLanguage(player(playerId, Locale.forLanguageTag("es-AR"))));
    }

    @Test
    void manualLanguageIgnoresCurrentPlayerLocale() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerSettingsService service = serviceWith(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.SPANISH.storageValue(),
            SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE
        )));

        service.preloadForLogin(playerId);

        assertEquals(Language.SPANISH, service.resolvedLanguage(player(playerId, Locale.US)));
    }

    @Test
    void clientLocaleDetectionCanBeDisabledForAutoLanguage() {
        final UUID playerId = UUID.randomUUID();
        final DefaultPlayerSettingsService service = serviceWith(
            new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
                SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
                SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE
            ))),
            GeoIpCountryResolver.disabled(LOGGER),
            localeDetectionDisabledConfig()
        );

        service.preloadForLogin(playerId);

        assertEquals(Language.ENGLISH, service.resolvedLanguage(player(playerId, Locale.forLanguageTag("es-AR"))));
    }

    private static DefaultPlayerSettingsService serviceWith(final PlayerSettingsSnapshot snapshot) {
        return serviceWith(new RecordingPlayerSettingsRepository(snapshot), GeoIpCountryResolver.disabled(LOGGER), pluginConfig());
    }

    private static DefaultPlayerSettingsService serviceWith(
        final PlayerSettingsRepository repository,
        final GeoIpCountryResolver countryResolver,
        final PluginConfig config
    ) {
        return new DefaultPlayerSettingsService(
            repository,
            new LanguageResolver(Language.ENGLISH),
            config,
            countryResolver,
            LOGGER
        );
    }

    private static PlayerSettingsSnapshot snapshotWith(final UUID playerId, final Map<SettingKey, String> values) {
        return new PlayerSettingsSnapshot(playerId, values);
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

    private static PluginConfig geoIpEnabledConfig() {
        return new PluginConfig(
            new PluginConfig.DatabaseSection("127.0.0.1", 3306, "test", "root", "", "nps_", 1, 1, false),
            new PluginConfig.SettingsSection(Language.ENGLISH, true, true, 0L),
            new PluginConfig.GeoIpSection(true, "GeoLite2-Country.mmdb"),
            new PluginConfig.CommandSection("globalsettings", java.util.List.of(), PluginConfig.CommandTargetType.MENU, "language"),
            new PluginConfig.PlaceholderSection(false, 0L)
        );
    }

    private static PluginConfig localeDetectionDisabledConfig() {
        return new PluginConfig(
            new PluginConfig.DatabaseSection("127.0.0.1", 3306, "test", "root", "", "nps_", 1, 1, false),
            new PluginConfig.SettingsSection(Language.ENGLISH, false, true, 0L),
            new PluginConfig.GeoIpSection(false, "GeoLite2-Country.mmdb"),
            new PluginConfig.CommandSection("globalsettings", java.util.List.of(), PluginConfig.CommandTargetType.MENU, "language"),
            new PluginConfig.PlaceholderSection(false, 0L)
        );
    }

    private static GeoIpCountryResolver resolver(final CountryLookup lookup) {
        return new GeoIpCountryResolver(LOGGER, null) {
            @Override
            public boolean enabled() {
                return true;
            }

            @Override
            public CountryLookup resolveCountry(final java.net.InetAddress address) {
                return lookup;
            }
        };
    }

    private static Player player(final UUID playerId, final Locale locale) {
        return (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(),
            new Class<?>[] {Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> playerId;
                case "locale" -> locale;
                case "toString" -> "DefaultPlayerSettingsServiceTestPlayer";
                case "hashCode" -> playerId.hashCode();
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    private static final class RecordingPlayerSettingsRepository implements PlayerSettingsRepository {

        private PlayerSettingsSnapshot snapshot;
        private final List<PersistedSetting> syncUpserts = new ArrayList<>();
        private final List<PersistedSetting> asyncUpserts = new ArrayList<>();

        private RecordingPlayerSettingsRepository(final PlayerSettingsSnapshot snapshot) {
            this.snapshot = snapshot;
        }

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
            this.syncUpserts.add(new PersistedSetting(key, value));
            updateSnapshot(key, value);
        }

        @Override
        public CompletableFuture<Void> upsertAsync(final UUID playerId, final SettingKey key, final String value) {
            this.asyncUpserts.add(new PersistedSetting(key, value));
            updateSnapshot(key, value);
            return CompletableFuture.completedFuture(null);
        }

        private void updateSnapshot(final SettingKey key, final String value) {
            final EnumMap<SettingKey, String> values = new EnumMap<>(this.snapshot.values());
            values.put(key, value);
            this.snapshot = new PlayerSettingsSnapshot(this.snapshot.playerId(), values);
        }
    }

    private record PersistedSetting(SettingKey key, String value) {
    }
}
