package com.stephanofer.networkplayersettingszmenu.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.api.StylePatternInfo;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.settings.SettingFeedback;
import com.stephanofer.networkplayersettingszmenu.settings.SettingMutationCooldowns;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class StylePatternSelectionHandler {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PlayerStyleService styleService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;

    public StylePatternSelectionHandler(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PlayerStyleService styleService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.styleService = Objects.requireNonNull(styleService, "styleService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.settingsConfig = Objects.requireNonNull(settingsConfig, "settingsConfig");
    }

    public void select(final Player player, final InventoryEngine inventory, final StyleButtonKind kind, final StylePatternInfo pattern) {
        if (!this.settingsService.isReady(player.getUniqueId())) {
            player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.loading"));
            SettingFeedback.error(player);
            return;
        }
        if (!canUse(player, kind, pattern.id())) {
            player.sendRichMessage(this.messages.get(
                this.settingsService.resolvedLanguage(player),
                "settings.style.locked",
                pattern.permission()
            ));
            SettingFeedback.error(player);
            return;
        }
        if (isSelected(player, kind, pattern.id())) {
            player.sendRichMessage(this.messages.get(
                this.settingsService.resolvedLanguage(player),
                kind == StyleButtonKind.NICK ? "settings.nick.already-selected" : "settings.chat.already-selected"
            ));
            SettingFeedback.error(player);
            return;
        }

        final SettingMutationCooldowns.Cooldown cooldown = SettingMutationCooldowns.get(player.getUniqueId());
        if (cooldown != null) {
            final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
            final long seconds = Math.max(1L, (cooldown.expiresAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
            player.sendRichMessage(this.messages.get(viewerLanguage, "settings.style.cooldown", seconds));
            SettingFeedback.error(player);
            return;
        }

        SettingMutationCooldowns.put(player.getUniqueId(), Math.max(0L, this.settingsConfig.mutationCooldownMillis()));
        mutate(player.getUniqueId(), kind, pattern.id()).whenComplete((unused, throwable) -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (throwable != null) {
                SettingMutationCooldowns.clear(player.getUniqueId());
                player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.style.update-failed"));
                SettingFeedback.error(player);
                return;
            }
            player.sendRichMessage(this.messages.get(
                this.settingsService.resolvedLanguage(player),
                kind == StyleButtonKind.NICK ? "settings.nick.updated" : "settings.chat.updated"
            ));
            SettingFeedback.success(player);
            inventory.getPlugin().getInventoryManager().updateInventory(player);
        }));
    }

    private CompletableFuture<Void> mutate(final UUID playerId, final StyleButtonKind kind, final String patternId) {
        return kind == StyleButtonKind.NICK
            ? this.styleService.setNickStyle(playerId, patternId)
            : this.styleService.setChatStyle(playerId, patternId);
    }

    private boolean canUse(final Player player, final StyleButtonKind kind, final String patternId) {
        return kind == StyleButtonKind.NICK
            ? this.styleService.canUseNickStyle(player, patternId)
            : this.styleService.canUseChatStyle(player, patternId);
    }

    private boolean isSelected(final Player player, final StyleButtonKind kind, final String patternId) {
        final Optional<String> current = kind == StyleButtonKind.NICK
            ? this.styleService.nickStyleId(player.getUniqueId())
            : this.styleService.chatStyleId(player.getUniqueId());
        return current.map(patternId::equalsIgnoreCase).orElse(false);
    }
}
