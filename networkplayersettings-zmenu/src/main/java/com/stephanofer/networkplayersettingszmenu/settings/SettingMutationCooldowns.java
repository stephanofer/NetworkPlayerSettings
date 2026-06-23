package com.stephanofer.networkplayersettingszmenu.settings;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import java.time.Duration;
import java.util.UUID;

public final class SettingMutationCooldowns {

    private static final Cache<UUID, Cooldown> COOLDOWNS = Caffeine.newBuilder()
        .maximumSize(10_000L)
        .expireAfter(Expiry.creating((UUID playerId, Cooldown cooldown) -> Duration.ofMillis(cooldown.durationMillis())))
        .build();

    private SettingMutationCooldowns() {
    }

    public static Cooldown get(final UUID playerId) {
        return COOLDOWNS.getIfPresent(playerId);
    }

    public static void put(final UUID playerId, final long durationMillis) {
        if (durationMillis <= 0L) {
            return;
        }
        COOLDOWNS.put(playerId, new Cooldown(System.currentTimeMillis() + durationMillis, durationMillis));
    }

    public static void clear(final UUID playerId) {
        COOLDOWNS.invalidate(playerId);
    }

    public static void clearAll() {
        COOLDOWNS.invalidateAll();
    }

    public record Cooldown(long expiresAtMillis, long durationMillis) {
    }
}
