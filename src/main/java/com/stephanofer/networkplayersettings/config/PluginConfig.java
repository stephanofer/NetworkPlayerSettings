package com.stephanofer.networkplayersettings.config;

import com.hera.craftkit.database.DatabaseConfig;
import com.hera.craftkit.database.ExistingSchemaStrategy;
import com.hera.craftkit.database.MigrationConfig;
import com.hera.craftkit.database.PoolConfig;
import dev.dejvokep.boostedyaml.YamlDocument;
import com.stephanofer.networkplayersettings.settings.language.Language;

public record PluginConfig(
    DatabaseSection database,
    SettingsSection settings,
    GeoIpSection geoip,
    PlaceholderSection placeholderapi
) {

    public static PluginConfig fromDocument(final YamlDocument document) {
        return new PluginConfig(
            new DatabaseSection(
                document.getString("database.host", "127.0.0.1"),
                document.getInt("database.port", 3306),
                document.getString("database.database", "hera_network"),
                document.getString("database.username", "root"),
                document.getString("database.password", ""),
                document.getString("database.table-prefix", "nps_"),
                document.getInt("database.pool.maximum-pool-size", 5),
                document.getInt("database.pool.minimum-idle", 1),
                document.getBoolean("database.migrations.enabled", true)
            ),
            new SettingsSection(
                Language.fromCode(document.getString("settings.default-language", "en")),
                document.getBoolean("settings.detect-client-locale", true),
                document.getBoolean("settings.cache-cleanup-on-quit", true),
                document.getLong("settings.cache-maximum-size", 10_000L),
                document.getLong("settings.locale-cache-expire-after-access-millis", 600_000L)
            ),
            new GeoIpSection(
                document.getBoolean("geoip.enabled", true),
                document.getString("geoip.database-path", "GeoLite2-Country.mmdb")
            ),
            new PlaceholderSection(
                document.getBoolean("placeholderapi.enabled", true),
                document.getLong("placeholderapi.cache-ttl-millis", 250L),
                document.getLong("placeholderapi.cache-maximum-size", 50_000L)
            )
        );
    }

    public record DatabaseSection(
        String host,
        int port,
        String database,
        String username,
        String password,
        String tablePrefix,
        int maximumPoolSize,
        int minimumIdle,
        boolean migrationsEnabled
    ) {
        public DatabaseConfig toDatabaseConfig(final ClassLoader migrationClassLoader) {
            final DatabaseConfig.Builder builder = DatabaseConfig.builder()
                .host(this.host)
                .port(this.port)
                .database(this.database)
                .username(this.username)
                .password(this.password)
                .tablePrefix(this.tablePrefix)
                .pool(PoolConfig.builder()
                    .maximumPoolSize(this.maximumPoolSize)
                    .minimumIdle(this.minimumIdle)
                    .build());

            if (this.migrationsEnabled) {
                builder.migration(MigrationConfig.builder()
                    .existingSchemaStrategy(ExistingSchemaStrategy.BASELINE_AT_ZERO)
                    .classLoader(migrationClassLoader)
                    .build());
            } else {
                builder.migration(MigrationConfig.builder().enabled(false).build());
            }

            return builder.build();
        }
    }

    public record SettingsSection(
        Language defaultLanguage,
        boolean detectClientLocale,
        boolean cacheCleanupOnQuit,
        long cacheMaximumSize,
        long localeCacheExpireAfterAccessMillis
    ) {
        public SettingsSection {
            cacheMaximumSize = Math.max(1L, cacheMaximumSize);
            localeCacheExpireAfterAccessMillis = Math.max(0L, localeCacheExpireAfterAccessMillis);
        }
    }

    public record GeoIpSection(
        boolean enabled,
        String databasePath
    ) {
        public GeoIpSection {
            if (databasePath == null || databasePath.isBlank()) {
                databasePath = "GeoLite2-Country.mmdb";
            } else {
                databasePath = databasePath.trim();
            }
        }
    }

    public record PlaceholderSection(
        boolean enabled,
        long cacheTtlMillis,
        long cacheMaximumSize
    ) {
        public PlaceholderSection {
            cacheTtlMillis = Math.max(0L, cacheTtlMillis);
            cacheMaximumSize = Math.max(1L, cacheMaximumSize);
        }
    }
}
