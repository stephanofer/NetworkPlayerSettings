package com.stephanofer.networkplayersettings.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.service.DefaultPlayerSettingsService;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final DefaultPlayerSettingsService settingsService;
    private final PluginConfig config;

    public PlayerConnectionListener(final DefaultPlayerSettingsService settingsService, final PluginConfig config) {
        this.settingsService = settingsService;
        this.config = config;
    }

    @EventHandler
    public void onAsyncPlayerConnectionConfigure(final AsyncPlayerConnectionConfigureEvent event) {
        final PlayerProfile profile = event.getConnection().getProfile();
        final UUID playerId = profile.getId();
        if (playerId == null) {
            return;
        }

        final InetSocketAddress clientAddress = event.getConnection().getClientAddress();
        this.settingsService.preloadForConnection(playerId, clientAddress == null ? null : clientAddress.getAddress());
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(final AsyncPlayerPreLoginEvent event) {
        if (this.settingsService.cached(event.getUniqueId()).isEmpty()) {
            this.settingsService.preloadForLogin(event.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        this.settingsService.handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerLocaleChange(final PlayerLocaleChangeEvent event) {
        this.settingsService.handleLocaleChange(event.getPlayer(), event.locale().toString());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        this.settingsService.evict(event.getPlayer().getUniqueId(), this.config.settings().cacheCleanupOnQuit());
    }
}
