package com.stephanofer.networkplayersettings.command;

import com.stephanofer.networkplatform.menus.DialogKey;
import com.stephanofer.networkplatform.menus.DialogOpenResult;
import com.stephanofer.networkplatform.menus.MenuKey;
import com.stephanofer.networkplatform.menus.MenuOpenResult;
import com.stephanofer.networkplatform.menus.MenuService;
import com.stephanofer.networkplatform.paper.command.CommandExecutionContext;
import com.stephanofer.networkplatform.paper.command.CommandSpec;
import com.stephanofer.networkplatform.paper.command.SenderScope;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.config.PluginConfig;
import com.stephanofer.networkplayersettings.i18n.PluginMessages;
import org.bukkit.entity.Player;

public final class GlobalSettingsCommand {

    private final PlayerSettingsService settingsService;
    private final MenuService menuService;
    private final PluginMessages messages;
    private final PluginConfig.CommandSection commandConfig;

    public GlobalSettingsCommand(
        final PlayerSettingsService settingsService,
        final MenuService menuService,
        final PluginMessages messages,
        final PluginConfig.CommandSection commandConfig
    ) {
        this.settingsService = settingsService;
        this.menuService = menuService;
        this.messages = messages;
        this.commandConfig = commandConfig;
    }

    public CommandSpec spec() {
        return CommandSpec.builder(this.commandConfig.name())
            .aliases(this.commandConfig.aliases())
            .description("Open your global player settings")
            .senderScope(SenderScope.PLAYER)
            .handler(this::handle)
            .build();
    }

    private int handle(final CommandExecutionContext context) {
        final Player player = context.requirePlayer();
        if (!this.settingsService.isReady(player.getUniqueId())) {
            context.replyRich(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.loading"));
            return 1;
        }

        if (this.commandConfig.openTargetType() == PluginConfig.CommandTargetType.DIALOG) {
            final DialogOpenResult result = this.menuService.dialogs().open(player, DialogKey.of(this.commandConfig.openTargetKey()));
            if (!(result instanceof DialogOpenResult.Opened)) {
                context.replyRich(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.menu-open-failed"));
            }
            return 1;
        }

        final MenuOpenResult result = this.menuService.open(player, MenuKey.of(this.commandConfig.openTargetKey()));
        if (!(result instanceof MenuOpenResult.Opened)) {
            context.replyRich(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.menu-open-failed"));
        }
        return 1;
    }
}
