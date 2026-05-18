package com.stephanofer.networkplayersettings.repository;

import com.stephanofer.networkplatform.database.DatabaseService;
import com.stephanofer.networkplayersettings.api.LanguagePreference;
import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.api.SettingKey;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SqlPlayerSettingsRepository implements PlayerSettingsRepository {

    private final DatabaseService databaseService;
    private final String tableName;

    public SqlPlayerSettingsRepository(final DatabaseService databaseService) {
        this.databaseService = Objects.requireNonNull(databaseService, "databaseService");
        this.tableName = this.databaseService.config().tablePrefix() + "player_settings";
    }

    @Override
    public RepositoryLoadResult loadOrCreate(final UUID playerId) {
        return this.databaseService.withConnection(connection -> loadOrCreate(connection, playerId));
    }

    @Override
    public PlayerSettingsSnapshot load(final UUID playerId) {
        return this.databaseService.withConnection(connection -> new PlayerSettingsSnapshot(playerId, readValues(connection, playerId)));
    }

    @Override
    public CompletableFuture<PlayerSettingsSnapshot> loadAsync(final UUID playerId) {
        return this.databaseService.queryAsync(connection -> new PlayerSettingsSnapshot(playerId, readValues(connection, playerId)));
    }

    @Override
    public CompletableFuture<RepositoryLoadResult> loadOrCreateAsync(final UUID playerId) {
        return this.databaseService.queryAsync(connection -> loadOrCreate(connection, playerId));
    }

    @Override
    public void upsert(final UUID playerId, final SettingKey key, final String value) {
        this.databaseService.useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + this.tableName + " (player_uuid, setting_key, setting_value) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP"
            )) {
                statement.setBytes(1, toBytes(playerId));
                statement.setString(2, key.storageKey());
                statement.setString(3, value);
                statement.executeUpdate();
            }
        });
    }

    @Override
    public CompletableFuture<Void> upsertAsync(final UUID playerId, final SettingKey key, final String value) {
        return this.databaseService.executeAsync(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO " + this.tableName + " (player_uuid, setting_key, setting_value) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP"
            )) {
                statement.setBytes(1, toBytes(playerId));
                statement.setString(2, key.storageKey());
                statement.setString(3, value);
                statement.executeUpdate();
            }
        });
    }

    private static byte[] toBytes(final UUID uuid) {
        final ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private RepositoryLoadResult loadOrCreate(final Connection connection, final UUID playerId) throws Exception {
        final EnumMap<SettingKey, String> values = readValues(connection, playerId);
        boolean createdDefault = values.isEmpty();
        createdDefault |= ensureDefault(connection, playerId, values, SettingKey.LANGUAGE, LanguagePreference.AUTO.storageValue());
        return new RepositoryLoadResult(new PlayerSettingsSnapshot(playerId, values), createdDefault);
    }

    private boolean ensureDefault(
        final Connection connection,
        final UUID playerId,
        final EnumMap<SettingKey, String> values,
        final SettingKey key,
        final String defaultValue
    ) throws Exception {
        if (values.containsKey(key)) {
            return false;
        }

        upsert(connection, playerId, key, defaultValue);
        values.put(key, defaultValue);
        return true;
    }

    private EnumMap<SettingKey, String> readValues(final Connection connection, final UUID playerId) throws Exception {
        final EnumMap<SettingKey, String> values = new EnumMap<>(SettingKey.class);
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT setting_key, setting_value FROM " + this.tableName + " WHERE player_uuid = ?"
        )) {
            statement.setBytes(1, toBytes(playerId));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    final SettingKey key = SettingKey.fromStorageKey(resultSet.getString("setting_key"));
                    if (key != null) {
                        values.put(key, resultSet.getString("setting_value"));
                    }
                }
            }
        }
        return values;
    }

    private void upsert(final Connection connection, final UUID playerId, final SettingKey key, final String value) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
            "INSERT INTO " + this.tableName + " (player_uuid, setting_key, setting_value) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value), updated_at = CURRENT_TIMESTAMP"
        )) {
            statement.setBytes(1, toBytes(playerId));
            statement.setString(2, key.storageKey());
            statement.setString(3, value);
            statement.executeUpdate();
        }
    }
}
