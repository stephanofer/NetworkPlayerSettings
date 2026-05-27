package com.stephanofer.networkplayersettings;

import com.stephanofer.networkplatform.database.DatabaseModule;
import com.stephanofer.networkplatform.database.DatabaseService;
import com.stephanofer.networkplatform.hooks.placeholderapi.PlaceholderModule;
import com.stephanofer.networkplatform.hooks.placeholderapi.PlaceholderService;
import com.stephanofer.networkplatform.menus.MenuModule;
import com.stephanofer.networkplatform.menus.MenuModuleConfig;
import com.stephanofer.networkplatform.menus.MenuService;
import com.stephanofer.networkplatform.paper.NetworkPlatform;
import com.stephanofer.networkplatform.paper.config.ConfigFileSpec;
import com.stephanofer.networkplatform.paper.libs.boostedyaml.YamlDocument;
import com.stephanofer.networkplayersettings.api.NetworkAssetService;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.asset.CountryAssetLoader;
import com.stephanofer.networkplayersettings.asset.NetworkAssetBootstrap;
import com.stephanofer.networkplayersettings.command.GlobalSettingsCommand;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.country.GeoIpCountryResolver;
import com.stephanofer.networkplayersettings.i18n.PluginMessages;
import com.stephanofer.networkplayersettings.language.LanguageResolver;
import com.stephanofer.networkplayersettings.listener.PlayerConnectionListener;
import com.stephanofer.networkplayersettings.menu.LanguageButtonLoader;
import com.stephanofer.networkplayersettings.placeholder.PlayerSettingsPlaceholderRegistrar;
import com.stephanofer.networkplayersettings.repository.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.repository.SqlPlayerSettingsRepository;
import com.stephanofer.networkplayersettings.service.DefaultPlayerSettingsService;
import java.nio.file.Path;
import java.time.Duration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class NetworkPlayerSettingsPlugin extends JavaPlugin {

    private NetworkPlatform platform;
    private DatabaseService databaseService;
    private MenuService menuService;
    private PlaceholderService placeholderService;
    private PluginConfig config;
    private PluginMessages messages;
    private NetworkAssetService networkAssetService;
    private GeoIpCountryResolver countryResolver;
    private DefaultPlayerSettingsService settingsService;

    @Override
    public void onEnable() {
        this.platform = NetworkPlatform.create(this);
        final YamlDocument configDocument = this.platform.configs().load(
            ConfigFileSpec.builder("config.yml")
                .autoUpdate(true)
                .versionRoute("file-version")
                .build()
        );
        this.platform.configs().load("inventories/language.yml");
        this.config = PluginConfig.fromDocument(configDocument);
        this.messages = new PluginMessages();
        this.networkAssetService = new NetworkAssetBootstrap(new CountryAssetLoader())
            .initialize(this.platform.configs(), getServer().getServicesManager(), this);

        this.databaseService = DatabaseModule.install(this.platform, this.config.database().toDatabaseConfig());
        this.menuService = MenuModule.install(this.platform, new MenuModuleConfig(false, "patterns", "inventories", "dialogs"));
        this.placeholderService = PlaceholderModule.install(this.platform);

        this.countryResolver = openCountryResolver();

        final PlayerSettingsRepository repository = new SqlPlayerSettingsRepository(this.databaseService);
        this.settingsService = new DefaultPlayerSettingsService(
            repository,
            new LanguageResolver(this.config.settings().defaultLanguage()),
            this.config,
            this.countryResolver,
            getLogger()
        );

        this.menuService.loader().registerButtons(buttonManager -> buttonManager.register(new LanguageButtonLoader(this)));
        this.menuService.loader().loadPatterns("patterns");
        this.menuService.loader().loadInventories("inventories");
        this.menuService.loader().loadDialogs("dialogs");

        if (this.config.placeholderapi().enabled()) {
            new PlayerSettingsPlaceholderRegistrar(
                this.placeholderService,
                this.settingsService,
                Duration.ofMillis(Math.max(0L, this.config.placeholderapi().cacheTtlMillis())),
                getPluginMeta().getVersion()
            ).register();
        }

        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this.settingsService, this.config), this);
        this.platform.commands().register(new GlobalSettingsCommand(this.settingsService, this.menuService, this.messages, this.config.command()).spec());
        getServer().getServicesManager().register(PlayerSettingsService.class, this.settingsService, this, ServicePriority.Normal);
    }

    @Override
    public void onDisable() {
        if (getServer() != null) {
            getServer().getServicesManager().unregisterAll(this);
        }
        if (this.countryResolver != null) {
            this.countryResolver.close();
        }
        if (this.platform != null) {
            this.platform.shutdown();
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

    public DefaultPlayerSettingsService settingsService() {
        return this.settingsService;
    }

    public MenuService menuService() {
        return this.menuService;
    }

    public PluginMessages messages() {
        return this.messages;
    }

    public PluginConfig config() {
        return this.config;
    }
}
