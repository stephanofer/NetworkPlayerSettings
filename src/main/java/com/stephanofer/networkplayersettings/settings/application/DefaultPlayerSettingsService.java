package com.stephanofer.networkplayersettings.settings.application;

import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.settings.api.SettingKey;
import com.stephanofer.networkplayersettings.settings.country.CountryFlag;
import com.stephanofer.networkplayersettings.settings.country.GeoIpCountryResolver;
import com.stephanofer.networkplayersettings.settings.country.GeoIpCountryResolver.CountryLookup;
import com.stephanofer.networkplayersettings.settings.event.PlayerSettingChangeEvent;
import com.stephanofer.networkplayersettings.settings.event.PlayerSettingsReadyEvent;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettings.settings.language.LanguagePreference;
import com.stephanofer.networkplayersettings.settings.language.LanguageResolver;
import com.stephanofer.networkplayersettings.settings.storage.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.settings.storage.RepositoryLoadResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class DefaultPlayerSettingsService implements PlayerSettingsService {

    private final PlayerSettingsRepository repository;
    private final LanguageResolver languageResolver;
    private final PluginConfig config;
    private final GeoIpCountryResolver countryResolver;
    private final Logger logger;
    private final Consumer<Runnable> mainThreadExecutor;
    private final Consumer<PlayerSettingChangeEvent> settingChangeEventDispatcher;
    private final Cache<UUID, PlayerSettingsSnapshot> cache;
    private final Cache<UUID, String> localeCache;
    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> mutationChains = new ConcurrentHashMap<>();
    private final Set<UUID> readyPlayers = ConcurrentHashMap.newKeySet();

    public DefaultPlayerSettingsService(
        final PlayerSettingsRepository repository,
        final LanguageResolver languageResolver,
        final PluginConfig config,
        final GeoIpCountryResolver countryResolver,
        final Logger logger,
        final JavaPlugin plugin
    ) {
        this(
            repository,
            languageResolver,
            config,
            countryResolver,
            logger,
            task -> plugin.getServer().getScheduler().runTask(plugin, task),
            PlayerSettingChangeEvent::callEvent
        );
    }

    DefaultPlayerSettingsService(
        final PlayerSettingsRepository repository,
        final LanguageResolver languageResolver,
        final PluginConfig config,
        final GeoIpCountryResolver countryResolver,
        final Logger logger,
        final Consumer<Runnable> mainThreadExecutor,
        final Consumer<PlayerSettingChangeEvent> settingChangeEventDispatcher
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.languageResolver = Objects.requireNonNull(languageResolver, "languageResolver");
        this.config = Objects.requireNonNull(config, "config");
        this.countryResolver = Objects.requireNonNull(countryResolver, "countryResolver");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
        this.settingChangeEventDispatcher = Objects.requireNonNull(settingChangeEventDispatcher, "settingChangeEventDispatcher");
        this.cache = Caffeine.newBuilder()
            .maximumSize(this.config.settings().cacheMaximumSize())
            .build();
        final Caffeine<Object, Object> localeCacheBuilder = Caffeine.newBuilder()
            .maximumSize(this.config.settings().cacheMaximumSize());
        if (this.config.settings().localeCacheExpireAfterAccessMillis() > 0L) {
            localeCacheBuilder.expireAfterAccess(Duration.ofMillis(this.config.settings().localeCacheExpireAfterAccessMillis()));
        }
        this.localeCache = localeCacheBuilder.build();
    }

    public void preloadForLogin(final UUID playerId) {
        try {
            final RepositoryLoadResult loadResult = this.repository.loadOrCreate(playerId);
            this.cache.put(playerId, loadResult.snapshot());
            this.readyPlayers.remove(playerId);
        } catch (final Exception exception) {
            this.logger.log(Level.SEVERE, "Failed to preload player settings for " + playerId + ". Falling back to defaults.", exception);
            this.cache.put(playerId, PlayerSettingsSnapshot.defaults(playerId));
            this.readyPlayers.remove(playerId);
        }
    }

    public void preloadForConnection(final UUID playerId, final InetAddress address) {
        try {
            final RepositoryLoadResult loadResult = this.repository.loadOrCreate(playerId);
            if (!this.countryResolver.enabled()) {
                this.cache.put(playerId, loadResult.snapshot());
                this.readyPlayers.remove(playerId);
                return;
            }

            final CountryLookup countryLookup = this.countryResolver.resolveCountry(address);
            final PlayerSettingsSnapshot updated = updateDetectedCountrySnapshot(playerId, loadResult.snapshot(), countryLookup, true);
            this.cache.put(playerId, updated);
            this.readyPlayers.remove(playerId);
        } catch (final Exception exception) {
            this.logger.log(Level.SEVERE, "Failed to preload player settings for " + playerId + ". Falling back to defaults.", exception);
            final PlayerSettingsSnapshot fallback = PlayerSettingsSnapshot.defaults(playerId)
                .withSetting(SettingKey.DETECTED_COUNTRY, CountryFlag.UNKNOWN_CODE);
            this.cache.put(playerId, fallback);
            this.readyPlayers.remove(playerId);
        }
    }

    public void handleJoin(final Player player) {
        final UUID playerId = player.getUniqueId();
        final PlayerSettingsSnapshot snapshot = getCachedOrDefault(playerId);
        final String locale = currentLocale(player);
        rememberLocale(playerId, locale);
        final Language resolved = resolveLanguage(snapshot, locale);
        this.readyPlayers.add(playerId);
        new PlayerSettingsReadyEvent(player, snapshot, resolved).callEvent();
    }

    public void handleLocaleChange(final Player player, final String newLocaleRaw) {
        final UUID playerId = player.getUniqueId();
        final PlayerSettingsSnapshot snapshot = getCachedOrDefault(playerId);
        final String oldLocale = Objects.requireNonNullElse(this.localeCache.getIfPresent(playerId), currentLocale(player));
        final String newLocale = this.languageResolver.normalizeLocale(newLocaleRaw);
        final Language oldResolved = resolveLanguage(snapshot, oldLocale);
        rememberLocale(playerId, newLocale);
        final Language newResolved = resolveLanguage(snapshot, newLocale);

        if (snapshot.languagePreference() == LanguagePreference.AUTO && oldResolved != newResolved) {
            dispatchSettingChangeEvent(new PlayerSettingChangeEvent(
                playerId,
                SettingKey.LANGUAGE,
                snapshot.languagePreference().storageValue(),
                snapshot.languagePreference().storageValue(),
                oldResolved.code(),
                newResolved.code()
            ));
        }
    }

    public void evict(final UUID playerId, final boolean clearCache) {
        this.readyPlayers.remove(playerId);
        this.localeCache.invalidate(playerId);
        if (clearCache) {
            this.cache.invalidate(playerId);
        }
        this.mutationChains.remove(playerId);
    }

    @Override
    public CompletableFuture<PlayerSettingsSnapshot> load(final UUID playerId) {
        final PlayerSettingsSnapshot cached = this.cache.getIfPresent(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return this.repository.loadOrCreateAsync(playerId)
            .thenApply(loadResult -> {
                this.cache.put(playerId, loadResult.snapshot());
                return loadResult.snapshot();
            });
    }

    @Override
    public Optional<PlayerSettingsSnapshot> cached(final UUID playerId) {
        return Optional.ofNullable(this.cache.getIfPresent(playerId));
    }

    @Override
    public PlayerSettingsSnapshot getCachedOrDefault(final UUID playerId) {
        final PlayerSettingsSnapshot cached = this.cache.getIfPresent(playerId);
        return cached == null ? PlayerSettingsSnapshot.defaults(playerId) : cached;
    }

    @Override
    public Language resolvedLanguage(final Player player) {
        final PlayerSettingsSnapshot snapshot = getCachedOrDefault(player.getUniqueId());
        return resolveLanguage(snapshot, currentLocale(player));
    }

    @Override
    public LanguagePreference languagePreference(final UUID playerId) {
        return getCachedOrDefault(playerId).languagePreference();
    }

    @Override
    public String countryCode(final UUID playerId) {
        return getCachedOrDefault(playerId).countryCode();
    }

    @Override
    public boolean showCountryFlag(final UUID playerId) {
        return getCachedOrDefault(playerId).showCountryFlag();
    }

    @Override
    public CompletableFuture<Void> setLanguage(final UUID playerId, final LanguagePreference preference) {
        Objects.requireNonNull(preference, "preference");
        return enqueueMutation(playerId, () -> {
            final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
            if (previous.languagePreference() == preference) {
                return CompletableFuture.completedFuture(null);
            }
            final String locale = currentLocale(playerId);
            final Language oldResolved = resolveLanguage(previous, locale);
            final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.LANGUAGE, preference.storageValue());
            final Language newResolved = resolveLanguage(updated, locale);

            return this.repository.upsertAsync(playerId, SettingKey.LANGUAGE, preference.storageValue())
                .whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    this.logger.log(Level.SEVERE, "Failed to persist language setting for " + playerId, throwable);
                })
                .thenCompose(unused -> runOnMainThread(() -> {
                    this.cache.put(playerId, updated);
                    if (!previous.valueOrDefault(SettingKey.LANGUAGE).equals(updated.valueOrDefault(SettingKey.LANGUAGE)) || oldResolved != newResolved) {
                        dispatchSettingChangeEvent(new PlayerSettingChangeEvent(
                            playerId,
                            SettingKey.LANGUAGE,
                            previous.valueOrDefault(SettingKey.LANGUAGE),
                            updated.valueOrDefault(SettingKey.LANGUAGE),
                            oldResolved.code(),
                            newResolved.code()
                        ));
                    }
                }));
        });
    }

    @Override
    public CompletableFuture<Void> setCountryOverride(final UUID playerId, final String countryCode) {
        final String normalizedCountry = CountryFlag.normalizeCode(countryCode);
        if (CountryFlag.UNKNOWN_CODE.equals(normalizedCountry)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("country override must be a real ISO-3166-1 alpha-2 code"));
        }

        return enqueueMutation(playerId, () -> {
            final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
            final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.COUNTRY_OVERRIDE, normalizedCountry);

            return this.repository.upsertAsync(playerId, SettingKey.COUNTRY_OVERRIDE, normalizedCountry)
                .whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    this.logger.log(Level.SEVERE, "Failed to persist country override for " + playerId, throwable);
                })
                .thenCompose(unused -> runOnMainThread(() -> {
                    this.cache.put(playerId, updated);
                    if (!Objects.equals(previous.countryCode(), updated.countryCode())) {
                        dispatchSettingChangeEvent(new PlayerSettingChangeEvent(
                            playerId,
                            SettingKey.COUNTRY_OVERRIDE,
                            previous.valueOrDefault(SettingKey.COUNTRY_OVERRIDE),
                            normalizedCountry,
                            previous.countryCode(),
                            updated.countryCode()
                        ));
                    }
                }));
        });
    }

    @Override
    public CompletableFuture<Void> clearCountryOverride(final UUID playerId) {
        return enqueueMutation(playerId, () -> {
            final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
            final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.COUNTRY_OVERRIDE, "");

            return this.repository.upsertAsync(playerId, SettingKey.COUNTRY_OVERRIDE, "")
                .whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    this.logger.log(Level.SEVERE, "Failed to clear country override for " + playerId, throwable);
                })
                .thenCompose(unused -> runOnMainThread(() -> {
                    this.cache.put(playerId, updated);
                    if (!Objects.equals(previous.countryCode(), updated.countryCode())) {
                        dispatchSettingChangeEvent(new PlayerSettingChangeEvent(
                            playerId,
                            SettingKey.COUNTRY_OVERRIDE,
                            previous.valueOrDefault(SettingKey.COUNTRY_OVERRIDE),
                            "",
                            previous.countryCode(),
                            updated.countryCode()
                        ));
                    }
                }));
        });
    }

    @Override
    public CompletableFuture<Void> setShowCountryFlag(final UUID playerId, final boolean enabled) {
        return enqueueMutation(playerId, () -> {
            final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
            if (previous.showCountryFlag() == enabled) {
                return CompletableFuture.completedFuture(null);
            }

            final String value = Boolean.toString(enabled);
            final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.SHOW_COUNTRY_FLAG, value);

            return this.repository.upsertAsync(playerId, SettingKey.SHOW_COUNTRY_FLAG, value)
                .whenComplete((unused, throwable) -> {
                    if (throwable == null) {
                        return;
                    }
                    this.logger.log(Level.SEVERE, "Failed to persist country flag visibility for " + playerId, throwable);
                })
                .thenCompose(unused -> runOnMainThread(() -> {
                    this.cache.put(playerId, updated);
                    dispatchSettingChangeEvent(new PlayerSettingChangeEvent(
                        playerId,
                        SettingKey.SHOW_COUNTRY_FLAG,
                        Boolean.toString(previous.showCountryFlag()),
                        value,
                        Boolean.toString(previous.showCountryFlag()),
                        value
                    ));
                }));
        });
    }

    @Override
    public CompletableFuture<Void> setSetting(final UUID playerId, final SettingKey key, final String value) {
        Objects.requireNonNull(key, "key");
        if (!key.playerWritable()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("setting is not player writable: " + key.storageKey()));
        }

        if (key == SettingKey.LANGUAGE) {
            if (!LanguagePreference.isSupported(value)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("unsupported language value: " + value));
            }
            return setLanguage(playerId, LanguagePreference.fromStorage(value));
        }

        if (key == SettingKey.SHOW_COUNTRY_FLAG) {
            final Optional<Boolean> enabled = parseBooleanSetting(value);
            if (enabled.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("unsupported boolean value: " + value));
            }
            return setShowCountryFlag(playerId, enabled.get());
        }

        return CompletableFuture.failedFuture(new IllegalArgumentException("unsupported setting: " + key.storageKey()));
    }

    private static Optional<Boolean> parseBooleanSetting(final String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "true" -> Optional.of(true);
            case "false" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    @Override
    public Optional<String> getSetting(final UUID playerId, final SettingKey key) {
        return getCachedOrDefault(playerId).setting(key);
    }

    @Override
    public boolean isReady(final UUID playerId) {
        return this.readyPlayers.contains(playerId);
    }

    private PlayerSettingsSnapshot updateDetectedCountrySnapshot(
        final UUID playerId,
        final PlayerSettingsSnapshot previous,
        final CountryLookup countryLookup,
        final boolean persist
    ) {
        final String previousCountry = previous.detectedCountryCode();
        final String normalizedCountry = countryLookup.countryCode();

        if (!countryLookup.detectedRealCountry() && !CountryFlag.UNKNOWN_CODE.equals(previousCountry)) {
            return previous;
        }

        final boolean unchanged = Objects.equals(previousCountry, normalizedCountry);

        if (persist && !unchanged) {
            try {
                this.repository.upsert(playerId, SettingKey.DETECTED_COUNTRY, normalizedCountry);
            } catch (final Exception exception) {
                this.logger.log(Level.WARNING, "Failed to persist detected country for " + playerId, exception);
            }
        }

        if (unchanged) {
            return previous;
        }

        return previous.withSetting(SettingKey.DETECTED_COUNTRY, normalizedCountry);
    }

    private String currentLocale(final UUID playerId) {
        if (!this.config.settings().detectClientLocale()) {
            return "";
        }
        final Player onlinePlayer = Bukkit.getPlayer(playerId);
        if (onlinePlayer != null) {
            return currentLocale(onlinePlayer);
        }
        final String cachedLocale = this.localeCache.getIfPresent(playerId);
        return cachedLocale == null ? "" : cachedLocale;
    }

    private CompletableFuture<Void> runOnMainThread(final Runnable task) {
        final CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            this.mainThreadExecutor.accept(() -> {
                try {
                    task.run();
                    result.complete(null);
                } catch (final Throwable throwable) {
                    result.completeExceptionally(throwable);
                }
            });
        } catch (final Throwable throwable) {
            result.completeExceptionally(throwable);
        }
        return result;
    }

    private CompletableFuture<Void> enqueueMutation(final UUID playerId, final Supplier<CompletableFuture<Void>> mutation) {
        final AtomicReference<CompletableFuture<Void>> queued = new AtomicReference<>();
        this.mutationChains.compute(playerId, (unused, previous) -> {
            final CompletableFuture<Void> base = previous == null
                ? CompletableFuture.completedFuture(null)
                : previous.handle((result, throwable) -> null);
            final CompletableFuture<Void> next = base.thenCompose(ignored -> mutation.get());
            queued.set(next);
            return next.whenComplete((result, throwable) -> this.mutationChains.remove(playerId, next));
        });
        return queued.get();
    }

    private void dispatchSettingChangeEvent(final PlayerSettingChangeEvent event) {
        this.settingChangeEventDispatcher.accept(event);
    }

    private String currentLocale(final Player player) {
        if (!this.config.settings().detectClientLocale()) {
            return "";
        }
        return this.languageResolver.normalizeLocale(player.locale());
    }

    private void rememberLocale(final UUID playerId, final String locale) {
        if (!this.config.settings().detectClientLocale() || locale == null || locale.isBlank()) {
            this.localeCache.invalidate(playerId);
            return;
        }
        this.localeCache.put(playerId, locale);
    }

    private Language resolveLanguage(final PlayerSettingsSnapshot snapshot, final String locale) {
        return this.languageResolver.resolve(snapshot.languagePreference(), locale);
    }
}
