package com.stephanofer.networkplayersettings.asset;

import com.stephanofer.networkplayersettings.api.NetworkAssetService;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;

public final class NetworkAssetBootstrap {

    private final CatalogLoader loader;

    public NetworkAssetBootstrap(final CountryAssetLoader loader) {
        this(loader::load);
    }

    NetworkAssetBootstrap(final CatalogLoader loader) {
        this.loader = Objects.requireNonNull(loader, "loader");
    }

    public NetworkAssetService initialize(final Path dataFolder, final ServicesManager servicesManager, final Plugin plugin) {
        Objects.requireNonNull(dataFolder, "dataFolder");
        Objects.requireNonNull(servicesManager, "servicesManager");
        Objects.requireNonNull(plugin, "plugin");

        final CountryAssetCatalog catalog = this.loader.load(dataFolder);
        final NetworkAssetService service = new DefaultNetworkAssetService(catalog);
        servicesManager.register(NetworkAssetService.class, service, plugin, ServicePriority.Normal);
        return service;
    }

    @FunctionalInterface
    interface CatalogLoader {
        CountryAssetCatalog load(Path dataFolder);
    }
}
