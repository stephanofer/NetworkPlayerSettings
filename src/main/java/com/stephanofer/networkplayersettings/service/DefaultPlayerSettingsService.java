package com.stephanofer.networkplayersettings.service;

import com.stephanofer.networkplayersettings.api.CountryFlag;
import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.LanguagePreference;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.api.SettingKey;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.country.GeoIpCountryResolver;
import com.stephanofer.networkplayersettings.event.PlayerSettingChangeEvent;
import com.stephanofer.networkplayersettings.event.PlayerSettingsReadyEvent;
import com.stephanofer.networkplayersettings.language.LanguageResolver;
import com.stephanofer.networkplayersettings.repository.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.repository.RepositoryLoadResult;
import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class DefaultPlayerSettingsService implements PlayerSettingsService {

    private final PlayerSettingsRepository repository;
    private final LanguageResolver languageResolver;
    private final PluginConfig config;
    private final GeoIpCountryResolver countryResolver;
    private final Logger logger;
    private final ConcurrentHashMap<UUID, PlayerSettingsSnapshot> cache = new ConcurrentHashMap<>();
    private final Set<UUID> readyPlayers = ConcurrentHashMap.newKeySet();

    public DefaultPlayerSettingsService(
        final PlayerSettingsRepository repository,
        final LanguageResolver languageResolver,
        final PluginConfig config,
        final GeoIpCountryResolver countryResolver,
        final Logger logger
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.languageResolver = Objects.requireNonNull(languageResolver, "languageResolver");
        this.config = Objects.requireNonNull(config, "config");
        this.countryResolver = Objects.requireNonNull(countryResolver, "countryResolver");
        this.logger = Objects.requireNonNull(logger, "logger");
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

            final String countryCode = this.countryResolver.resolveCountryCode(address);
            this.logger.info(
                "GeoIP debug for " + playerId
                    + ": address=" + (address == null ? "null" : address.getHostAddress())
                    + ", localOrPrivate=" + isLocalOrPrivateAddress(address)
                    + ", detectedCountry=" + countryCode
            );
            final PlayerSettingsSnapshot updated = updateDetectedCountrySnapshot(playerId, loadResult.snapshot(), countryCode, true);
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
        final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
        final String locale = this.languageResolver.normalizeLocale(player.locale());
        final PlayerSettingsSnapshot updated = updateDetectedLocaleSnapshot(playerId, previous, locale);
        final Language resolved = resolveLanguage(updated, locale);
        this.readyPlayers.add(playerId);
        new PlayerSettingsReadyEvent(player, updated, resolved).callEvent();
    }

    public void handleLocaleChange(final Player player, final String newLocaleRaw) {
        final UUID playerId = player.getUniqueId();
        final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
        final String oldLocale = previous.detectedLocale().orElse("");
        final String newLocale = this.languageResolver.normalizeLocale(newLocaleRaw);
        final Language oldResolved = resolveLanguage(previous, oldLocale);
        final PlayerSettingsSnapshot updated = updateDetectedLocaleSnapshot(playerId, previous, newLocale);
        final Language newResolved = resolveLanguage(updated, newLocale);

        if (previous.languagePreference() == LanguagePreference.AUTO && oldResolved != newResolved) {
            new PlayerSettingChangeEvent(
                playerId,
                SettingKey.LANGUAGE,
                previous.languagePreference().storageValue(),
                updated.languagePreference().storageValue(),
                oldResolved.code(),
                newResolved.code()
            ).callEvent();
        }
    }

    public void evict(final UUID playerId, final boolean clearCache) {
        this.readyPlayers.remove(playerId);
        if (clearCache) {
            this.cache.remove(playerId);
        }
    }

    @Override
    public CompletableFuture<PlayerSettingsSnapshot> load(final UUID playerId) {
        final PlayerSettingsSnapshot cached = this.cache.get(playerId);
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
        return Optional.ofNullable(this.cache.get(playerId));
    }

    @Override
    public PlayerSettingsSnapshot getCachedOrDefault(final UUID playerId) {
        return this.cache.getOrDefault(playerId, PlayerSettingsSnapshot.defaults(playerId));
    }

    @Override
    public Language resolvedLanguage(final Player player) {
        final PlayerSettingsSnapshot snapshot = getCachedOrDefault(player.getUniqueId());
        return resolveLanguage(snapshot, this.languageResolver.normalizeLocale(player.locale()));
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
    public CompletableFuture<Void> setLanguage(final UUID playerId, final LanguagePreference preference) {
        Objects.requireNonNull(preference, "preference");
        final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
        if (previous.languagePreference() == preference) {
            return CompletableFuture.completedFuture(null);
        }
        final String locale = currentLocale(playerId, previous);
        final Language oldResolved = resolveLanguage(previous, locale);
        final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.LANGUAGE, preference.storageValue());
        final Language newResolved = resolveLanguage(updated, locale);
        this.cache.put(playerId, updated);

        if (!previous.valueOrDefault(SettingKey.LANGUAGE).equals(updated.valueOrDefault(SettingKey.LANGUAGE)) || oldResolved != newResolved) {
            new PlayerSettingChangeEvent(
                playerId,
                SettingKey.LANGUAGE,
                previous.valueOrDefault(SettingKey.LANGUAGE),
                updated.valueOrDefault(SettingKey.LANGUAGE),
                oldResolved.code(),
                newResolved.code()
            ).callEvent();
        }

        return this.repository.upsertAsync(playerId, SettingKey.LANGUAGE, preference.storageValue())
            .exceptionally(throwable -> {
                this.logger.log(Level.SEVERE, "Failed to persist language setting for " + playerId, throwable);
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> setCountryOverride(final UUID playerId, final String countryCode) {
        final String normalizedCountry = CountryFlag.normalizeCode(countryCode);
        if (CountryFlag.UNKNOWN_CODE.equals(normalizedCountry)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("country override must be a real ISO-3166-1 alpha-2 code"));
        }

        final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
        final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.COUNTRY_OVERRIDE, normalizedCountry);
        this.cache.put(playerId, updated);

        if (!Objects.equals(previous.countryCode(), updated.countryCode())) {
            new PlayerSettingChangeEvent(
                playerId,
                SettingKey.COUNTRY_OVERRIDE,
                previous.valueOrDefault(SettingKey.COUNTRY_OVERRIDE),
                normalizedCountry,
                previous.countryCode(),
                updated.countryCode()
            ).callEvent();
        }

        return this.repository.upsertAsync(playerId, SettingKey.COUNTRY_OVERRIDE, normalizedCountry)
            .exceptionally(throwable -> {
                this.logger.log(Level.SEVERE, "Failed to persist country override for " + playerId, throwable);
                return null;
            });
    }

    @Override
    public CompletableFuture<Void> clearCountryOverride(final UUID playerId) {
        final PlayerSettingsSnapshot previous = getCachedOrDefault(playerId);
        final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.COUNTRY_OVERRIDE, "");
        this.cache.put(playerId, updated);

        if (!Objects.equals(previous.countryCode(), updated.countryCode())) {
            new PlayerSettingChangeEvent(
                playerId,
                SettingKey.COUNTRY_OVERRIDE,
                previous.valueOrDefault(SettingKey.COUNTRY_OVERRIDE),
                "",
                previous.countryCode(),
                updated.countryCode()
            ).callEvent();
        }

        return this.repository.upsertAsync(playerId, SettingKey.COUNTRY_OVERRIDE, "")
            .exceptionally(throwable -> {
                this.logger.log(Level.SEVERE, "Failed to clear country override for " + playerId, throwable);
                return null;
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

        return CompletableFuture.failedFuture(new IllegalArgumentException("unsupported setting: " + key.storageKey()));
    }

    @Override
    public Optional<String> getSetting(final UUID playerId, final SettingKey key) {
        return getCachedOrDefault(playerId).setting(key);
    }

    @Override
    public boolean isReady(final UUID playerId) {
        return this.readyPlayers.contains(playerId);
    }

    private PlayerSettingsSnapshot updateDetectedLocaleSnapshot(
        final UUID playerId,
        final PlayerSettingsSnapshot previous,
        final String locale
    ) {
        if (!this.config.settings().detectClientLocale()) {
            return previous;
        }

        if (Objects.equals(previous.detectedLocale().orElse(""), locale)) {
            this.cache.put(playerId, previous);
            return previous;
        }

        final PlayerSettingsSnapshot updated = previous.withSetting(SettingKey.DETECTED_LOCALE, locale);
        this.cache.put(playerId, updated);
        return updated;
    }

    private PlayerSettingsSnapshot updateDetectedCountrySnapshot(
        final UUID playerId,
        final PlayerSettingsSnapshot previous,
        final String countryCode,
        final boolean persist
    ) {
        final String normalizedCountry = CountryFlag.normalizeCode(countryCode);
        final boolean alreadyPersisted = previous.setting(SettingKey.DETECTED_COUNTRY).isPresent();
        final boolean unchanged = Objects.equals(previous.detectedCountryCode(), normalizedCountry);

        if (persist && (!alreadyPersisted || !unchanged)) {
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

    private static boolean isLocalOrPrivateAddress(final InetAddress address) {
        return address == null
            || address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress();
    }

    private String currentLocale(final UUID playerId, final PlayerSettingsSnapshot snapshot) {
        final Player onlinePlayer = Bukkit.getPlayer(playerId);
        if (onlinePlayer != null) {
            return this.languageResolver.normalizeLocale(onlinePlayer.locale());
        }
        return snapshot.detectedLocale().orElse("");
    }

    private Language resolveLanguage(final PlayerSettingsSnapshot snapshot, final String locale) {
        final String localeToUse = locale == null || locale.isBlank() ? snapshot.detectedLocale().orElse("") : locale;
        return this.languageResolver.resolve(snapshot.languagePreference(), localeToUse);
    }
}
