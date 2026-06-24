package com.stephanofer.networkplayersettingszmenu;

import static net.kyori.adventure.text.Component.text;

import com.hera.craftkit.zmenu.ZMenuIntegration;
import com.hera.craftkit.zmenu.ZMenus;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettingszmenu.command.GlobalSettingsCommand;
import com.stephanofer.networkplayersettingszmenu.command.StyleMenuCommand;
import com.stephanofer.networkplayersettingszmenu.config.AddonYamlLoader;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.settings.SettingMutationCooldowns;
import com.stephanofer.networkplayersettingszmenu.settings.view.SettingsMenuBootstrap;
import com.stephanofer.networkplayersettingszmenu.settings.view.SettingsViewOpener;
import dev.dejvokep.boostedyaml.YamlDocument;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.caption.ComponentCaptionFormatter;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;

public final class NetworkPlayerSettingsZMenuPlugin extends JavaPlugin {

    private ZMenuPluginConfig config;
    private PluginMessages messages;
    private PlayerSettingsService settingsService;
    private PlayerStyleService styleService;
    private ZMenuIntegration zmenu;

    @Override
    public void onEnable() {
        try {
            final AddonYamlLoader yamlLoader = new AddonYamlLoader(this);
            final YamlDocument configDocument = yamlLoader.load("config.yml");
            final YamlDocument spanishMessages = yamlLoader.load("messages/messages_es.yml");
            final YamlDocument englishMessages = yamlLoader.load("messages/messages_en.yml");

            this.config = ZMenuPluginConfig.fromDocument(configDocument, getLogger());
            this.messages = new PluginMessages(getLogger(), spanishMessages, englishMessages);
            this.settingsService = requireService(PlayerSettingsService.class);
            this.styleService = requireService(PlayerStyleService.class);
            this.zmenu = ZMenus.require(this);

            new SettingsMenuBootstrap(this, this.zmenu, this.settingsService, this.styleService, this.messages, this.config.settings()).load();
            registerCommands(new SettingsViewOpener(this, this.zmenu, getLogger()));
            getServer().getPluginManager().registerEvents(new PlayerQuitCooldownListener(), this);
        } catch (final Exception exception) {
            getLogger().severe("Failed to enable NetworkPlayerSettingsZMenu: " + rootCauseMessage(exception));
            getLogger().log(java.util.logging.Level.SEVERE, "Startup failure details", exception);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        SettingMutationCooldowns.clearAll();
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
        new StyleMenuCommand(this.settingsService, settingsViewOpener, this.messages, this.config.nickStyleCommand(), "Open your nick style menu")
            .register(commandManager);
        new StyleMenuCommand(this.settingsService, settingsViewOpener, this.messages, this.config.chatStyleCommand(), "Open your chat style menu")
            .register(commandManager);
    }

    private <T> T requireService(final Class<T> serviceClass) {
        final RegisteredServiceProvider<T> provider = getServer().getServicesManager().getRegistration(serviceClass);
        if (provider == null || provider.getProvider() == null) {
            throw new IllegalStateException("Missing required Bukkit service: " + serviceClass.getName());
        }
        return provider.getProvider();
    }

    private static String rootCauseMessage(final Throwable throwable) {
        Throwable cursor = Objects.requireNonNull(throwable, "throwable");
        while (cursor instanceof CompletionException && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }

    private static final class PlayerQuitCooldownListener implements org.bukkit.event.Listener {

        @org.bukkit.event.EventHandler
        public void onPlayerQuit(final org.bukkit.event.player.PlayerQuitEvent event) {
            SettingMutationCooldowns.clear(event.getPlayer().getUniqueId());
        }
    }
}
