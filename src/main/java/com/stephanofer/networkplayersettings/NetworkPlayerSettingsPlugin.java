package com.stephanofer.networkplayersettings;

import com.hera.craftkit.database.Database;
import com.hera.craftkit.database.Databases;
import com.stephanofer.networkplayersettings.assets.api.CountryFlagService;
import com.stephanofer.networkplayersettings.assets.api.NetworkAssetService;
import com.stephanofer.networkplayersettings.assets.country.CountryAssetLoader;
import com.stephanofer.networkplayersettings.assets.country.DefaultCountryFlagService;
import com.stephanofer.networkplayersettings.assets.country.DefaultNetworkAssetService;
import com.stephanofer.networkplayersettings.assets.country.NetworkAssetBootstrap;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.platform.bukkit.NetworkPlayerSettingsCommand;
import com.stephanofer.networkplayersettings.platform.bukkit.PlayerConnectionListener;
import com.stephanofer.networkplayersettings.platform.bukkit.PlayerChatStyleListener;
import com.stephanofer.networkplayersettings.platform.bukkit.PlayerSettingsPlaceholderExpansion;
import com.stephanofer.networkplayersettings.platform.bukkit.PluginYamlLoader;
import com.stephanofer.networkplayersettings.settings.application.DefaultPlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.country.GeoIpCountryResolver;
import com.stephanofer.networkplayersettings.settings.language.LanguageResolver;
import com.stephanofer.networkplayersettings.settings.storage.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.settings.storage.SqlPlayerSettingsRepository;
import com.stephanofer.networkplayersettings.settings.style.DefaultPlayerStyleService;
import com.stephanofer.networkplayersettings.settings.style.StylePatternCatalog;
import com.stephanofer.networkplayersettings.settings.style.StylePatternCatalogLoader;
import com.stephanofer.networkplayersettings.settings.style.StylePatternRenderer;
import com.stephanofer.networkplayersettings.settings.style.StylePatternType;
import dev.dejvokep.boostedyaml.YamlDocument;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NetworkPlayerSettingsPlugin extends JavaPlugin {

    private Database database;
    private PluginYamlLoader yamlLoader;
    private PluginConfig config;
    private DefaultNetworkAssetService networkAssetService;
    private CountryFlagService countryFlagService;
    private GeoIpCountryResolver countryResolver;
    private DefaultPlayerSettingsService settingsService;
    private DefaultPlayerStyleService styleService;
    private PlayerSettingsPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        try {
            loadConfiguration();
            initializeAssets();
            initializeDatabase();
            initializeSettingsService();
            initializeStyleService();
            registerServices();
            registerPlaceholderExpansion();
            registerListeners();
            registerCommands();
        } catch (final Exception exception) {
            getLogger().severe("Failed to enable NetworkPlayerSettings: " + rootCauseMessage(exception));
            getLogger().log(java.util.logging.Level.SEVERE, "Startup failure details", exception);
            unregisterPlaceholderExpansion();
            shutdownResources();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        unregisterPlaceholderExpansion();
        if (this.settingsService != null) {
            this.settingsService.clearCachedState();
        }
        if (getServer() != null) {
            getServer().getServicesManager().unregisterAll(this);
        }
        shutdownResources();
    }

    private void loadConfiguration() {
        this.yamlLoader = new PluginYamlLoader(this);
        final YamlDocument configDocument = this.yamlLoader.load("config.yml");
        this.config = PluginConfig.fromDocument(configDocument);
    }

    private void initializeAssets() {
        final YamlDocument countryDocument = this.yamlLoader.load("assets/countries.yml");
        this.networkAssetService = (DefaultNetworkAssetService) new NetworkAssetBootstrap(new CountryAssetLoader())
            .initialize(countryDocument, getServer().getServicesManager(), this);
    }

    private void initializeDatabase() {
        this.database = Databases.mysql(this.config.database().toDatabaseConfig(getClass().getClassLoader()));
        this.database.migrate().join();
    }

    private void initializeSettingsService() {
        this.countryResolver = openCountryResolver();
        final PlayerSettingsRepository repository = new SqlPlayerSettingsRepository(this.database);
        this.settingsService = new DefaultPlayerSettingsService(
            repository,
            new LanguageResolver(this.config.settings().defaultLanguage()),
            this.config,
            this.countryResolver,
            getLogger(),
            this
        );
        this.countryFlagService = new DefaultCountryFlagService(this.settingsService, this.networkAssetService);
    }

    private void initializeStyleService() {
        final StylePatternCatalog nickCatalog = new StylePatternCatalogLoader(StylePatternType.NICK)
            .load(this.yamlLoader.load("styles/nick-patterns.yml"));
        final StylePatternCatalog chatCatalog = new StylePatternCatalogLoader(StylePatternType.CHAT)
            .load(this.yamlLoader.load("styles/chat-patterns.yml"));
        this.styleService = new DefaultPlayerStyleService(
            this.settingsService,
            nickCatalog,
            chatCatalog,
            new StylePatternRenderer()
        );
    }

    private void registerPlaceholderExpansion() {
        if (!this.config.placeholderapi().enabled()) {
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            getLogger().warning("PlaceholderAPI integration is enabled in config, but PlaceholderAPI is not installed.");
            return;
        }

        this.placeholderExpansion = new PlayerSettingsPlaceholderExpansion(
            this.settingsService,
            this.styleService,
            this.countryFlagService,
            this.config.settings().defaultLanguage(),
            Duration.ofMillis(Math.max(0L, this.config.placeholderapi().cacheTtlMillis())),
            this.config.placeholderapi().cacheMaximumSize(),
            getPluginMeta().getVersion()
        );
        this.placeholderExpansion.register();
        getServer().getPluginManager().registerEvents(this.placeholderExpansion, this);
    }

    private void unregisterPlaceholderExpansion() {
        if (this.placeholderExpansion == null) {
            return;
        }

        this.placeholderExpansion.unregister();
        this.placeholderExpansion = null;
    }

    private void registerServices() {
        getServer().getServicesManager().register(PlayerSettingsService.class, this.settingsService, this, ServicePriority.Normal);
        getServer().getServicesManager().register(PlayerStyleService.class, this.styleService, this, ServicePriority.Normal);
        getServer().getServicesManager().register(CountryFlagService.class, this.countryFlagService, this, ServicePriority.Normal);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this.settingsService, this.config), this);
        getServer().getPluginManager().registerEvents(new PlayerChatStyleListener(this.styleService), this);
    }

    private void registerCommands() {
        final NetworkPlayerSettingsCommand command = new NetworkPlayerSettingsCommand(this);
        final org.bukkit.command.PluginCommand pluginCommand = Objects.requireNonNull(
            getCommand("networkplayersettings"),
            "networkplayersettings command is missing from plugin.yml"
        );
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    public void reloadRuntimeResources() {
        final var countryCatalog = new CountryAssetLoader().load(this.yamlLoader.load("assets/countries.yml"));
        final StylePatternCatalog nickCatalog = new StylePatternCatalogLoader(StylePatternType.NICK)
            .load(this.yamlLoader.load("styles/nick-patterns.yml"));
        final StylePatternCatalog chatCatalog = new StylePatternCatalogLoader(StylePatternType.CHAT)
            .load(this.yamlLoader.load("styles/chat-patterns.yml"));

        this.networkAssetService.replaceCatalog(countryCatalog);
        this.styleService.replaceCatalogs(nickCatalog, chatCatalog);
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.clearCache();
        }
    }

    private GeoIpCountryResolver openCountryResolver() {
        if (!this.config.geoip().enabled()) {
            getLogger().info("GeoIP country detection disabled by config.");
            return GeoIpCountryResolver.disabled(getLogger());
        }

        final Path configuredPath = Path.of(this.config.geoip().databasePath());
        final Path databasePath = configuredPath.isAbsolute()
            ? configuredPath
            : getDataFolder().toPath().resolve(configuredPath);
        return GeoIpCountryResolver.open(databasePath, getLogger());
    }

    private void shutdownResources() {
        if (this.countryResolver != null) {
            this.countryResolver.close();
            this.countryResolver = null;
        }
        if (this.database != null) {
            this.database.close();
            this.database = null;
        }
    }

    private static String rootCauseMessage(final Throwable throwable) {
        Throwable cursor = Objects.requireNonNull(throwable, "throwable");
        while (cursor instanceof CompletionException && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    public DefaultPlayerSettingsService settingsService() {
        return this.settingsService;
    }

    public PlayerStyleService styleService() {
        return this.styleService;
    }

    public PluginConfig config() {
        return this.config;
    }
}
