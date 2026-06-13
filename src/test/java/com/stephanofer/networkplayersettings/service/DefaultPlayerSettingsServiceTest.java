package com.stephanofer.networkplayersettings.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.stephanofer.networkplayersettings.api.CountryFlag;
import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.LanguagePreference;
import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.api.SettingKey;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.country.GeoIpCountryResolver;
import com.stephanofer.networkplayersettings.country.GeoIpCountryResolver.CountryLookup;
import com.stephanofer.networkplayersettings.event.PlayerSettingChangeEvent;
import com.stephanofer.networkplayersettings.language.LanguageResolver;
import com.stephanofer.networkplayersettings.repository.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.repository.RepositoryLoadResult;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
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

    @Test
    void manualCountryOverrideDoesNotUpdateCacheWhenPersistenceFails() {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "AR"
        )), true);
        final DefaultPlayerSettingsService service = serviceWith(repository, GeoIpCountryResolver.disabled(LOGGER), pluginConfig());
        service.preloadForLogin(playerId);

        final CompletableFuture<Void> mutation = service.setCountryOverride(playerId, "BR");

        assertThrows(CompletionException.class, mutation::join);
        assertEquals("AR", service.countryCode(playerId));
        assertEquals(List.of(new PersistedSetting(SettingKey.COUNTRY_OVERRIDE, "BR")), repository.asyncUpserts);
    }

    @Test
    void setLanguageUpdatesCacheOnlyAfterPersistenceCompletes() {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE
        )));
        final QueuedMainThreadExecutor mainThreadExecutor = new QueuedMainThreadExecutor();
        final List<PlayerSettingChangeEvent> events = new ArrayList<>();
        final DefaultPlayerSettingsService service = serviceWith(
            repository,
            GeoIpCountryResolver.disabled(LOGGER),
            localeDetectionDisabledConfig(),
            mainThreadExecutor,
            events
        );
        service.preloadForLogin(playerId);
        final CompletableFuture<Void> persistence = repository.enqueueAsyncUpsert();

        final CompletableFuture<Void> mutation = service.setLanguage(playerId, LanguagePreference.SPANISH);

        assertFalse(mutation.isDone());
        assertEquals(LanguagePreference.AUTO, service.languagePreference(playerId));
        assertEquals(List.of(), events);
        persistence.complete(null);
        assertFalse(mutation.isDone());
        assertEquals(LanguagePreference.AUTO, service.languagePreference(playerId));
        assertEquals(List.of(), events);
        mainThreadExecutor.runNext();
        mutation.join();
        assertEquals(LanguagePreference.SPANISH, service.languagePreference(playerId));
        assertSettingChangeEvent(
            events,
            playerId,
            SettingKey.LANGUAGE,
            LanguagePreference.AUTO.storageValue(),
            LanguagePreference.SPANISH.storageValue(),
            Language.ENGLISH.code(),
            Language.SPANISH.code()
        );
    }

    @Test
    void setLanguageDoesNotUpdateCacheWhenPersistenceFails() {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE
        )));
        final QueuedMainThreadExecutor mainThreadExecutor = new QueuedMainThreadExecutor();
        final List<PlayerSettingChangeEvent> events = new ArrayList<>();
        final DefaultPlayerSettingsService service = serviceWith(
            repository,
            GeoIpCountryResolver.disabled(LOGGER),
            localeDetectionDisabledConfig(),
            mainThreadExecutor,
            events
        );
        service.preloadForLogin(playerId);
        final CompletableFuture<Void> persistence = repository.enqueueAsyncUpsert();

        final CompletableFuture<Void> mutation = service.setLanguage(playerId, LanguagePreference.SPANISH);
        persistence.completeExceptionally(new IllegalStateException("async persistence failed"));

        assertThrows(CompletionException.class, mutation::join);
        assertEquals(LanguagePreference.AUTO, service.languagePreference(playerId));
        assertEquals(List.of(), events);
    }

    @Test
    void setCountryOverrideUpdatesCacheOnlyAfterPersistenceCompletes() {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "AR"
        )));
        final QueuedMainThreadExecutor mainThreadExecutor = new QueuedMainThreadExecutor();
        final List<PlayerSettingChangeEvent> events = new ArrayList<>();
        final DefaultPlayerSettingsService service = serviceWith(
            repository,
            GeoIpCountryResolver.disabled(LOGGER),
            pluginConfig(),
            mainThreadExecutor,
            events
        );
        service.preloadForLogin(playerId);
        final CompletableFuture<Void> persistence = repository.enqueueAsyncUpsert();

        final CompletableFuture<Void> mutation = service.setCountryOverride(playerId, "BR");

        assertFalse(mutation.isDone());
        assertEquals("AR", service.countryCode(playerId));
        assertEquals(List.of(), events);
        persistence.complete(null);
        assertFalse(mutation.isDone());
        assertEquals("AR", service.countryCode(playerId));
        assertEquals(List.of(), events);
        mainThreadExecutor.runNext();
        mutation.join();
        assertEquals("BR", service.countryCode(playerId));
        assertSettingChangeEvent(events, playerId, SettingKey.COUNTRY_OVERRIDE, "", "BR", "AR", "BR");
    }

    @Test
    void setCountryOverrideDoesNotUpdateCacheWhenPersistenceFails() {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "AR"
        )));
        final QueuedMainThreadExecutor mainThreadExecutor = new QueuedMainThreadExecutor();
        final List<PlayerSettingChangeEvent> events = new ArrayList<>();
        final DefaultPlayerSettingsService service = serviceWith(
            repository,
            GeoIpCountryResolver.disabled(LOGGER),
            pluginConfig(),
            mainThreadExecutor,
            events
        );
        service.preloadForLogin(playerId);
        final CompletableFuture<Void> persistence = repository.enqueueAsyncUpsert();

        final CompletableFuture<Void> mutation = service.setCountryOverride(playerId, "BR");
        persistence.completeExceptionally(new IllegalStateException("async persistence failed"));

        assertThrows(CompletionException.class, mutation::join);
        assertEquals("AR", service.countryCode(playerId));
        assertEquals(List.of(), events);
    }

    @Test
    void clearCountryOverrideUpdatesCacheOnlyAfterPersistenceCompletes() {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "AR",
            SettingKey.COUNTRY_OVERRIDE, "BR"
        )));
        final QueuedMainThreadExecutor mainThreadExecutor = new QueuedMainThreadExecutor();
        final List<PlayerSettingChangeEvent> events = new ArrayList<>();
        final DefaultPlayerSettingsService service = serviceWith(
            repository,
            GeoIpCountryResolver.disabled(LOGGER),
            pluginConfig(),
            mainThreadExecutor,
            events
        );
        service.preloadForLogin(playerId);
        final CompletableFuture<Void> persistence = repository.enqueueAsyncUpsert();

        final CompletableFuture<Void> mutation = service.clearCountryOverride(playerId);

        assertFalse(mutation.isDone());
        assertEquals("BR", service.countryCode(playerId));
        assertEquals(List.of(), events);
        persistence.complete(null);
        assertFalse(mutation.isDone());
        assertEquals("BR", service.countryCode(playerId));
        assertEquals(List.of(), events);
        mainThreadExecutor.runNext();
        mutation.join();
        assertEquals("AR", service.countryCode(playerId));
        assertSettingChangeEvent(events, playerId, SettingKey.COUNTRY_OVERRIDE, "BR", "", "BR", "AR");
    }

    @Test
    void clearCountryOverrideDoesNotUpdateCacheWhenPersistenceFails() {
        final UUID playerId = UUID.randomUUID();
        final RecordingPlayerSettingsRepository repository = new RecordingPlayerSettingsRepository(snapshotWith(playerId, Map.of(
            SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue(),
            SettingKey.DETECTED_COUNTRY, "AR",
            SettingKey.COUNTRY_OVERRIDE, "BR"
        )));
        final QueuedMainThreadExecutor mainThreadExecutor = new QueuedMainThreadExecutor();
        final List<PlayerSettingChangeEvent> events = new ArrayList<>();
        final DefaultPlayerSettingsService service = serviceWith(
            repository,
            GeoIpCountryResolver.disabled(LOGGER),
            pluginConfig(),
            mainThreadExecutor,
            events
        );
        service.preloadForLogin(playerId);
        final CompletableFuture<Void> persistence = repository.enqueueAsyncUpsert();

        final CompletableFuture<Void> mutation = service.clearCountryOverride(playerId);
        persistence.completeExceptionally(new IllegalStateException("async persistence failed"));

        assertThrows(CompletionException.class, mutation::join);
        assertEquals("BR", service.countryCode(playerId));
        assertEquals(List.of(), events);
    }

    private static DefaultPlayerSettingsService serviceWith(final PlayerSettingsSnapshot snapshot) {
        return serviceWith(new RecordingPlayerSettingsRepository(snapshot), GeoIpCountryResolver.disabled(LOGGER), pluginConfig());
    }

    private static DefaultPlayerSettingsService serviceWith(
        final PlayerSettingsRepository repository,
        final GeoIpCountryResolver countryResolver,
        final PluginConfig config
    ) {
        return serviceWith(repository, countryResolver, config, Runnable::run, new ArrayList<>());
    }

    private static DefaultPlayerSettingsService serviceWith(
        final PlayerSettingsRepository repository,
        final GeoIpCountryResolver countryResolver,
        final PluginConfig config,
        final Consumer<Runnable> mainThreadExecutor,
        final List<PlayerSettingChangeEvent> events
    ) {
        return new DefaultPlayerSettingsService(
            repository,
            new LanguageResolver(Language.ENGLISH),
            config,
            countryResolver,
            LOGGER,
            mainThreadExecutor,
            events::add
        );
    }

    private static void assertSettingChangeEvent(
        final List<PlayerSettingChangeEvent> events,
        final UUID playerId,
        final SettingKey settingKey,
        final String oldValue,
        final String newValue,
        final String oldResolvedValue,
        final String newResolvedValue
    ) {
        assertEquals(1, events.size());
        final PlayerSettingChangeEvent event = events.get(0);
        assertEquals(playerId, event.playerId());
        assertEquals(settingKey, event.settingKey());
        assertEquals(oldValue, event.oldValue());
        assertEquals(newValue, event.newValue());
        assertEquals(oldResolvedValue, event.oldResolvedValue());
        assertEquals(newResolvedValue, event.newResolvedValue());
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

    private static final class QueuedMainThreadExecutor implements Consumer<Runnable> {

        private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

        @Override
        public void accept(final Runnable task) {
            this.tasks.add(task);
        }

        private void runNext() {
            this.tasks.removeFirst().run();
        }
    }

    private static final class RecordingPlayerSettingsRepository implements PlayerSettingsRepository {

        private PlayerSettingsSnapshot snapshot;
        private final boolean failAsyncUpserts;
        private final List<PersistedSetting> syncUpserts = new ArrayList<>();
        private final List<PersistedSetting> asyncUpserts = new ArrayList<>();
        private final List<CompletableFuture<Void>> queuedAsyncUpserts = new ArrayList<>();

        private RecordingPlayerSettingsRepository(final PlayerSettingsSnapshot snapshot) {
            this(snapshot, false);
        }

        private RecordingPlayerSettingsRepository(final PlayerSettingsSnapshot snapshot, final boolean failAsyncUpserts) {
            this.snapshot = snapshot;
            this.failAsyncUpserts = failAsyncUpserts;
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
            if (this.failAsyncUpserts) {
                return CompletableFuture.failedFuture(new IllegalStateException("async persistence failed"));
            }
            if (!this.queuedAsyncUpserts.isEmpty()) {
                final CompletableFuture<Void> persistence = this.queuedAsyncUpserts.remove(0);
                return persistence.thenRun(() -> updateSnapshot(key, value));
            }
            updateSnapshot(key, value);
            return CompletableFuture.completedFuture(null);
        }

        private CompletableFuture<Void> enqueueAsyncUpsert() {
            final CompletableFuture<Void> persistence = new CompletableFuture<>();
            this.queuedAsyncUpserts.add(persistence);
            return persistence;
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
