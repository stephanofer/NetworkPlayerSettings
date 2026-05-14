package com.stephanofer.networkplayersettings.listener;

import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.service.DefaultPlayerSettingsService;
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
    public void onAsyncPlayerPreLogin(final AsyncPlayerPreLoginEvent event) {
        this.settingsService.preloadForLogin(event.getUniqueId());
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
