package com.stephanofer.networkplayersettings.repository;

import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.api.SettingKey;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerSettingsRepository {

    RepositoryLoadResult loadOrCreate(UUID playerId);

    PlayerSettingsSnapshot load(UUID playerId);

    CompletableFuture<PlayerSettingsSnapshot> loadAsync(UUID playerId);

    CompletableFuture<RepositoryLoadResult> loadOrCreateAsync(UUID playerId);

    void upsert(UUID playerId, SettingKey key, String value);

    CompletableFuture<Void> upsertAsync(UUID playerId, SettingKey key, String value);
}
