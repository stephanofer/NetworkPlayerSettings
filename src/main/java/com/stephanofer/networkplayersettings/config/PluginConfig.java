package com.stephanofer.networkplayersettings.config;

import com.stephanofer.networkplatform.database.DatabaseConfig;
import com.stephanofer.networkplatform.paper.config.ConfigDocument;
import com.stephanofer.networkplatform.paper.config.ConfigTemplate;
import com.stephanofer.networkplayersettings.api.Language;

public record PluginConfig(
    DatabaseSection database,
    SettingsSection settings,
    PlaceholderSection placeholderapi
) {

    public static final ConfigTemplate TEMPLATE = ConfigTemplate.builder("config.yml")
        .typed(PluginConfig.class, PluginConfig::fromDocument)
        .build();

    public static PluginConfig fromDocument(final ConfigDocument document) {
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
                document.getBoolean("settings.cache-cleanup-on-quit", true)
            ),
            new PlaceholderSection(
                document.getBoolean("placeholderapi.enabled", true),
                document.getLong("placeholderapi.cache-ttl-millis", 250L)
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
        public DatabaseConfig toDatabaseConfig() {
            return DatabaseConfig.mysql()
                .host(this.host)
                .port(this.port)
                .database(this.database)
                .username(this.username)
                .password(this.password)
                .tablePrefix(this.tablePrefix)
                .pool(pool -> {
                    pool.maximumPoolSize(this.maximumPoolSize);
                    pool.minimumIdle(this.minimumIdle);
                })
                .migrations(migrations -> migrations.enabled(this.migrationsEnabled))
                .build();
        }
    }

    public record SettingsSection(
        Language defaultLanguage,
        boolean detectClientLocale,
        boolean cacheCleanupOnQuit
    ) {
    }

    public record PlaceholderSection(
        boolean enabled,
        long cacheTtlMillis
    ) {
    }
}
