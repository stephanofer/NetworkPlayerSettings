package com.stephanofer.networkplayersettings.event;

import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class PlayerSettingsReadyEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PlayerSettingsSnapshot snapshot;
    private final Language resolvedLanguage;

    public PlayerSettingsReadyEvent(
        final Player player,
        final PlayerSettingsSnapshot snapshot,
        final Language resolvedLanguage
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.resolvedLanguage = Objects.requireNonNull(resolvedLanguage, "resolvedLanguage");
    }

    public Player player() {
        return this.player;
    }

    public PlayerSettingsSnapshot snapshot() {
        return this.snapshot;
    }

    public Language resolvedLanguage() {
        return this.resolvedLanguage;
    }

    public String countryCode() {
        return this.snapshot.countryCode();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
