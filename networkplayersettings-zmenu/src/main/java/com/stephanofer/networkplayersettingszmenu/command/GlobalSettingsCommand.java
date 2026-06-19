package com.stephanofer.networkplayersettingszmenu.command;

import static net.kyori.adventure.text.Component.text;
import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.menu.SettingsViewOpener;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;

public final class GlobalSettingsCommand {

    private final PlayerSettingsService settingsService;
    private final SettingsViewOpener settingsViewOpener;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.CommandSection commandConfig;

    public GlobalSettingsCommand(
        final PlayerSettingsService settingsService,
        final SettingsViewOpener settingsViewOpener,
        final PluginMessages messages,
        final ZMenuPluginConfig.CommandSection commandConfig
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.settingsViewOpener = Objects.requireNonNull(settingsViewOpener, "settingsViewOpener");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.commandConfig = Objects.requireNonNull(commandConfig, "commandConfig");
    }

    public void register(final PaperCommandManager<Source> commandManager, final MinecraftHelp<Source> minecraftHelp) {
        Objects.requireNonNull(commandManager, "commandManager");
        Objects.requireNonNull(minecraftHelp, "minecraftHelp");

        commandManager.command(
            commandManager.commandBuilder(
                    this.commandConfig.name(),
                    RichDescription.of(text("Open your global player settings")),
                    aliases()
                )
                .senderType(PlayerSource.class)
                .handler(context -> {
                    final Player player = context.sender().source();
                    if (!this.settingsService.isReady(player.getUniqueId())) {
                        player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.loading"));
                        return;
                    }

                    if (!this.settingsViewOpener.open(player, this.commandConfig)) {
                        player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.menu-open-failed"));
                    }
                })
        );

        commandManager.command(
            commandManager.commandBuilder(this.commandConfig.name(), Description.of("Command help"), aliases())
                .literal("help")
                .optional("query", greedyStringParser(), DefaultValue.constant(""))
                .handler(context -> minecraftHelp.queryCommands(context.get("query"), context.sender()))
        );
    }

    private String[] aliases() {
        final List<String> aliases = this.commandConfig.aliases();
        return aliases.toArray(String[]::new);
    }
}
