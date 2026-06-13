package com.stephanofer.networkplayersettings;

import static net.kyori.adventure.text.Component.text;

import com.hera.craftkit.database.Database;
import com.hera.craftkit.database.Databases;
import com.hera.craftkit.zmenu.ZMenuIntegration;
import com.hera.craftkit.zmenu.ZMenus;
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
import com.stephanofer.networkplayersettings.menu.SettingsViewOpener;
import com.stephanofer.networkplayersettings.menu.SettingsMenuBootstrap;
import com.stephanofer.networkplayersettings.placeholder.PlayerSettingsPlaceholderExpansion;
import com.stephanofer.networkplayersettings.repository.PlayerSettingsRepository;
import com.stephanofer.networkplayersettings.repository.SqlPlayerSettingsRepository;
import com.stephanofer.networkplayersettings.service.DefaultPlayerSettingsService;
import com.stephanofer.networkplayersettings.yaml.PluginYamlLoader;
import dev.dejvokep.boostedyaml.YamlDocument;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;

public final class NetworkPlayerSettingsPlugin extends JavaPlugin {

    private Database database;
    private ZMenuIntegration zmenu;
    private PluginYamlLoader yamlLoader;
    private PluginConfig config;
    private PluginMessages messages;
    private NetworkAssetService networkAssetService;
    private GeoIpCountryResolver countryResolver;
    private DefaultPlayerSettingsService settingsService;
    private PlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        try {
            loadConfiguration();
            initializeAssets();
            initializeDatabase();
            final SettingsViewOpener settingsViewOpener = initializeMenus();
            initializeSettingsService();
            registerPlaceholderExpansion();
            registerCommandsAndListeners(settingsViewOpener);
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
        if (getServer() != null) {
            getServer().getServicesManager().unregisterAll(this);
        }
        shutdownResources();
    }

    private void loadConfiguration() {
        this.yamlLoader = new PluginYamlLoader(this);
        final YamlDocument configDocument = this.yamlLoader.load("config.yml");
        this.config = PluginConfig.fromDocument(configDocument);
        this.messages = new PluginMessages();
    }

    private void initializeAssets() {
        final YamlDocument countryDocument = this.yamlLoader.load("assets/countries.yml");
        this.networkAssetService = new NetworkAssetBootstrap(new CountryAssetLoader())
            .initialize(countryDocument, getServer().getServicesManager(), this);
    }

    private void initializeDatabase() {
        this.database = Databases.mysql(this.config.database().toDatabaseConfig(getClass().getClassLoader()));
        this.database.migrate().join();
    }

    private SettingsViewOpener initializeMenus() {
        this.zmenu = ZMenus.require(this);
        new SettingsMenuBootstrap(this, this.zmenu).load();
        return new SettingsViewOpener(this, this.zmenu, getLogger());
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
            Duration.ofMillis(Math.max(0L, this.config.placeholderapi().cacheTtlMillis())),
            getPluginMeta().getVersion()
        );
        this.placeholderExpansion.register();
    }

    private void unregisterPlaceholderExpansion() {
        if (this.placeholderExpansion == null) {
            return;
        }

        this.placeholderExpansion.unregister();
        this.placeholderExpansion = null;
    }

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommands(final SettingsViewOpener settingsViewOpener) {
        final PaperCommandManager<Source> commandManager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
            .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
            .buildOnEnable(this);

        MinecraftExceptionHandler.create(Source::source)
            .defaultInvalidSyntaxHandler()
            .defaultInvalidSenderHandler()
            .defaultNoPermissionHandler()
            .defaultArgumentParsingHandler()
            .defaultCommandExecutionHandler()
            .decorator(component -> text().append(text("[", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY))
                .append(text("Settings", net.kyori.adventure.text.format.NamedTextColor.AQUA))
                .append(text("] ", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY))
                .append(component)
                .build())
            .registerTo(commandManager);

        final MinecraftHelp<Source> minecraftHelp = MinecraftHelp.<Source>builder()
            .commandManager(commandManager)
            .audienceProvider(Source::source)
            .commandPrefix('/' + this.config.command().name() + " help")
            .messageProvider(MinecraftHelp.captionMessageProvider(
                commandManager.captionRegistry(),
                ComponentCaptionFormatter.miniMessage()
            ))
            .build();
        commandManager.captionRegistry().registerProvider(MinecraftHelp.defaultCaptionsProvider());

        new GlobalSettingsCommand(this.settingsService, settingsViewOpener, this.messages, this.config.command())
            .register(commandManager, minecraftHelp);
    }

    private void registerCommandsAndListeners(final SettingsViewOpener settingsViewOpener) {
        registerCommands(settingsViewOpener);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this.settingsService, this.config), this);
        getServer().getServicesManager().register(PlayerSettingsService.class, this.settingsService, this, ServicePriority.Normal);
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

    public PluginMessages messages() {
        return this.messages;
    }

    public PluginConfig config() {
        return this.config;
    }
}
