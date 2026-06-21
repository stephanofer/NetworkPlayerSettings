package com.stephanofer.networkplayersettings.settings.event;

import com.stephanofer.networkplayersettings.settings.api.SettingKey;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

public final class PlayerSettingChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final SettingKey settingKey;
    private final String oldValue;
    private final String newValue;
    private final String oldResolvedValue;
    private final String newResolvedValue;

    public PlayerSettingChangeEvent(
        final UUID playerId,
        final SettingKey settingKey,
        final String oldValue,
        final String newValue,
        @Nullable final String oldResolvedValue,
        @Nullable final String newResolvedValue
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.settingKey = Objects.requireNonNull(settingKey, "settingKey");
        this.oldValue = oldValue == null ? "" : oldValue;
        this.newValue = newValue == null ? "" : newValue;
        this.oldResolvedValue = oldResolvedValue;
        this.newResolvedValue = newResolvedValue;
    }

    public UUID playerId() {
        return this.playerId;
    }

    public SettingKey settingKey() {
        return this.settingKey;
    }

    public String oldValue() {
        return this.oldValue;
    }

    public String newValue() {
        return this.newValue;
    }

    public @Nullable String oldResolvedValue() {
        return this.oldResolvedValue;
    }

    public @Nullable String newResolvedValue() {
        return this.newResolvedValue;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
