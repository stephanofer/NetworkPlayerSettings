package com.stephanofer.networkplayersettings.asset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stephanofer.networkplayersettings.api.CountryAsset;
import com.stephanofer.networkplayersettings.api.NetworkAssetService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NetworkAssetBootstrapTest {

    private static final String VALID_BASE64 = "eyJ0ZXh0dXJlcyI6e319";

    @TempDir
    Path tempDir;

    @Test
    void registersServiceProviderAfterCatalogLoadsExactlyOnceAndKeepsGameplayLookupsMemoryOnly() {
        final ServicesManager servicesManager = new RecordingServicesManager();
        final Plugin plugin = fakePlugin();
        final AtomicInteger loadCalls = new AtomicInteger();
        final NetworkAssetBootstrap bootstrap = new NetworkAssetBootstrap(dataFolder -> {
            loadCalls.incrementAndGet();
            return validCatalog();
        });

        final NetworkAssetService service = bootstrap.initialize(this.tempDir, servicesManager, plugin);
        service.countryAsset("AR");
        service.countryAsset("argentina");
        service.countryAsset("???");

        assertNotNull(service);
        assertSame(service, servicesManager.load(NetworkAssetService.class));
        assertSame(ServicePriority.Normal, servicesManager.getRegistration(NetworkAssetService.class).getPriority());
        assertEquals(1, loadCalls.get());
    }

    @Test
    void leavesServiceUnregisteredWhenCatalogLoadingFails() {
        final ServicesManager servicesManager = new RecordingServicesManager();
        final Plugin plugin = fakePlugin();
        final NetworkAssetBootstrap bootstrap = new NetworkAssetBootstrap(new CountryAssetLoader(invalidResource()));

        assertThrows(IllegalStateException.class, () -> bootstrap.initialize(this.tempDir, servicesManager, plugin));
        assertNull(servicesManager.load(NetworkAssetService.class));
    }

    @Test
    void leavesServiceUnregisteredWhenCatalogHasCollidingAliases() {
        final ServicesManager servicesManager = new RecordingServicesManager();
        final Plugin plugin = fakePlugin();
        final NetworkAssetBootstrap bootstrap = new NetworkAssetBootstrap(new CountryAssetLoader(resource("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: "%s"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: "%s"
                aliases: [shared]
              AM:
                name: Armenia
                head-texture-base64: "%s"
                aliases: [shared]
            """.formatted(VALID_BASE64, VALID_BASE64, VALID_BASE64))));

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> bootstrap.initialize(this.tempDir, servicesManager, plugin));

        assertTrue(error.getMessage().contains("shared"));
        assertNull(servicesManager.load(NetworkAssetService.class));
    }

    @Test
    void leavesServiceUnregisteredWhenCatalogHasInvalidBase64() {
        final ServicesManager servicesManager = new RecordingServicesManager();
        final Plugin plugin = fakePlugin();
        final NetworkAssetBootstrap bootstrap = new NetworkAssetBootstrap(new CountryAssetLoader(resource("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: "%s"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: "not-base64"
                aliases: [argentina]
            """.formatted(VALID_BASE64))));

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> bootstrap.initialize(this.tempDir, servicesManager, plugin));

        assertTrue(error.getMessage().contains("base64"));
        assertNull(servicesManager.load(NetworkAssetService.class));
    }

    private static Supplier<InputStream> invalidResource() {
        return () -> new ByteArrayInputStream(("""
            countries:
              AR:
                name: Argentina
                head-texture-base64: "%s"
                aliases: [argentina]
            """.formatted(VALID_BASE64)).getBytes(UTF_8));
    }

    private static Supplier<InputStream> resource(final String yaml) {
        return () -> new ByteArrayInputStream(yaml.getBytes(UTF_8));
    }

    private static CountryAssetCatalog validCatalog() {
        return new CountryAssetCatalog(List.of(
            new CountryAsset("XX", "Unknown", VALID_BASE64, Set.of("unknown")),
            new CountryAsset("AR", "Argentina", VALID_BASE64, Set.of("argentina"))
        ));
    }

    private static Plugin fakePlugin() {
        final InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> "NetworkPlayerSettingsTest";
            case "getLogger" -> Logger.getLogger("NetworkPlayerSettingsTest");
            case "isEnabled" -> true;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "NetworkPlayerSettingsTest";
            default -> null;
        };
        return (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[] { Plugin.class },
            handler
        );
    }

    private static final class RecordingServicesManager implements ServicesManager {

        private final Map<Class<?>, RegisteredServiceProvider<?>> registrations = new ConcurrentHashMap<>();

        @Override
        public <T> void register(final Class<T> service, final T provider, final Plugin plugin, final ServicePriority priority) {
            this.registrations.put(service, new RegisteredServiceProvider<>(service, provider, priority, plugin));
        }

        @Override
        public void unregisterAll(final Plugin plugin) {
            this.registrations.entrySet().removeIf(entry -> entry.getValue().getPlugin().equals(plugin));
        }

        @Override
        public void unregister(final Class<?> service, final Object provider) {
            this.registrations.computeIfPresent(service, (ignored, registration) -> registration.getProvider().equals(provider) ? null : registration);
        }

        @Override
        public void unregister(final Object provider) {
            this.registrations.entrySet().removeIf(entry -> entry.getValue().getProvider().equals(provider));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T load(final Class<T> service) {
            final RegisteredServiceProvider<?> registration = this.registrations.get(service);
            return registration == null ? null : (T) registration.getProvider();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> RegisteredServiceProvider<T> getRegistration(final Class<T> service) {
            return (RegisteredServiceProvider<T>) this.registrations.get(service);
        }

        @Override
        public List<RegisteredServiceProvider<?>> getRegistrations(final Plugin plugin) {
            return this.registrations.values().stream().filter(registration -> registration.getPlugin().equals(plugin)).toList();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Collection<RegisteredServiceProvider<T>> getRegistrations(final Class<T> service) {
            final RegisteredServiceProvider<?> registration = this.registrations.get(service);
            return registration == null ? List.of() : List.of((RegisteredServiceProvider<T>) registration);
        }

        @Override
        public Collection<Class<?>> getKnownServices() {
            return this.registrations.keySet();
        }

        @Override
        public <T> boolean isProvidedFor(final Class<T> service) {
            return this.registrations.containsKey(service);
        }
    }
}
