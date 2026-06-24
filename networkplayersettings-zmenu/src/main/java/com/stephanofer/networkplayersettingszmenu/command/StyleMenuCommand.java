package com.stephanofer.networkplayersettingszmenu.command;

import static net.kyori.adventure.text.Component.text;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.settings.view.SettingsViewOpener;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;

public final class StyleMenuCommand {

    private final PlayerSettingsService settingsService;
    private final SettingsViewOpener settingsViewOpener;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.CommandSection commandConfig;
    private final String description;

    public StyleMenuCommand(
        final PlayerSettingsService settingsService,
        final SettingsViewOpener settingsViewOpener,
        final PluginMessages messages,
        final ZMenuPluginConfig.CommandSection commandConfig,
        final String description
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.settingsViewOpener = Objects.requireNonNull(settingsViewOpener, "settingsViewOpener");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.commandConfig = Objects.requireNonNull(commandConfig, "commandConfig");
        this.description = Objects.requireNonNull(description, "description");
    }

    public void register(final PaperCommandManager<Source> commandManager) {
        Objects.requireNonNull(commandManager, "commandManager");

        commandManager.command(
            commandManager.commandBuilder(
                    this.commandConfig.name(),
                    RichDescription.of(text(this.description)),
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
    }

    private String[] aliases() {
        final List<String> aliases = this.commandConfig.aliases();
        return aliases.toArray(String[]::new);
    }
}
