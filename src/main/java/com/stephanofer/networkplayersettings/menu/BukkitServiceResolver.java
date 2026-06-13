package com.stephanofer.networkplayersettings.menu;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BukkitServiceResolver {

    private final JavaPlugin plugin;
    private final ServicesManager servicesManager;

    public BukkitServiceResolver(final JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.servicesManager = this.plugin.getServer().getServicesManager();
    }

    public <T> T requireService(final Class<T> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass");

        final T service = findService(serviceClass).orElse(null);
        if (service == null) {
            this.plugin.getLogger().severe("No se pudo encontrar el servicio crítico: " + serviceClass.getSimpleName());
            throw new IllegalStateException("Missing required service: " + serviceClass.getSimpleName());
        }
        return service;
    }

    public <T> Optional<T> findService(final Class<T> serviceClass) {
        Objects.requireNonNull(serviceClass, "serviceClass");

        final RegisteredServiceProvider<T> provider = this.servicesManager.getRegistration(serviceClass);
        if (provider == null || provider.getProvider() == null) {
            return Optional.empty();
        }
        return Optional.of(provider.getProvider());
    }
}
