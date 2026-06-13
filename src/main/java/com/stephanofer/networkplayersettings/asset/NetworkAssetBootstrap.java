package com.stephanofer.networkplayersettings.asset;

import com.stephanofer.networkplayersettings.api.NetworkAssetService;
import dev.dejvokep.boostedyaml.YamlDocument;
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

    public NetworkAssetService initialize(final YamlDocument countryDocument, final ServicesManager servicesManager, final Plugin plugin) {
        Objects.requireNonNull(countryDocument, "countryDocument");
        Objects.requireNonNull(servicesManager, "servicesManager");
        Objects.requireNonNull(plugin, "plugin");

        final CountryAssetCatalog catalog = this.loader.load(countryDocument);
        final NetworkAssetService service = new DefaultNetworkAssetService(catalog);
        servicesManager.register(NetworkAssetService.class, service, plugin, ServicePriority.Normal);
        return service;
    }

    @FunctionalInterface
    interface CatalogLoader {
        CountryAssetCatalog load(YamlDocument countryDocument);
    }
}
