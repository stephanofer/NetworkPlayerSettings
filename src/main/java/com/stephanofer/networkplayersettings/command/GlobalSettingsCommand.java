package com.stephanofer.networkplayersettings.command;

import com.stephanofer.networkplatform.menus.MenuKey;
import com.stephanofer.networkplatform.menus.MenuOpenResult;
import com.stephanofer.networkplatform.menus.MenuService;
import com.stephanofer.networkplatform.paper.command.CommandExecutionContext;
import com.stephanofer.networkplatform.paper.command.CommandSpec;
import com.stephanofer.networkplatform.paper.command.SenderScope;
import com.stephanofer.networkplayersettings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.i18n.PluginMessages;
import org.bukkit.entity.Player;

public final class GlobalSettingsCommand {

    private static final MenuKey LANGUAGE_MENU = MenuKey.of("language");

    private final PlayerSettingsService settingsService;
    private final MenuService menuService;
    private final PluginMessages messages;

    public GlobalSettingsCommand(
        final PlayerSettingsService settingsService,
        final MenuService menuService,
        final PluginMessages messages
    ) {
        this.settingsService = settingsService;
        this.menuService = menuService;
        this.messages = messages;
    }

    public CommandSpec spec() {
        return CommandSpec.builder("globalsettings")
            .aliases("settings", "prefs")
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

        final MenuOpenResult result = this.menuService.open(player, LANGUAGE_MENU);
        if (!(result instanceof MenuOpenResult.Opened)) {
            context.replyRich(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.menu-open-failed"));
        }
        return 1;
    }
}
