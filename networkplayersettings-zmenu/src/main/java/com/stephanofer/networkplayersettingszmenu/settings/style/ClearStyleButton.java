package com.stephanofer.networkplayersettingszmenu.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.settings.SettingFeedback;
import com.stephanofer.networkplayersettingszmenu.settings.SettingMutationCooldowns;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClearStyleButton extends Button {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PlayerStyleService styleService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;
    private final StyleButtonKind kind;

    public ClearStyleButton(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PlayerStyleService styleService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig,
        final StyleButtonKind kind
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.styleService = Objects.requireNonNull(styleService, "styleService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.settingsConfig = Objects.requireNonNull(settingsConfig, "settingsConfig");
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    @Override
    public void onClick(
        final Player player,
        final InventoryClickEvent event,
        final InventoryEngine inventory,
        final int slot,
        final Placeholders placeholders
    ) {
        super.onClick(player, event, inventory, slot, placeholders);
        if (!this.settingsService.isReady(player.getUniqueId())) {
            player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.loading"));
            SettingFeedback.error(player);
            return;
        }
        if (!hasSelected(player)) {
            player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.style.none-active"));
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
        clear(player.getUniqueId()).whenComplete((unused, throwable) -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
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
                this.kind == StyleButtonKind.NICK ? "settings.nick.cleared" : "settings.chat.cleared"
            ));
            SettingFeedback.success(player);
            inventory.getPlugin().getInventoryManager().updateInventory(player);
        }));
    }

    @Override
    public void onRender(final Player player, final InventoryEngine inventory) {
        if (inventory.getPage() != this.getPage() && !this.isPermanent()) {
            return;
        }
        final Placeholders placeholders = new Placeholders();
        placeholders.register("clear_state", hasSelected(player)
            ? this.messages.get(this.settingsService.resolvedLanguage(player), "menu.style.clear-available")
            : this.messages.get(this.settingsService.resolvedLanguage(player), "menu.style.clear-empty"));
        inventory.displayFinalButton(this, placeholders, this.getRealSlot(inventory.getInventory().getSize(), inventory.getPage()));
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    private CompletableFuture<Void> clear(final UUID playerId) {
        return this.kind == StyleButtonKind.NICK
            ? this.styleService.clearNickStyle(playerId)
            : this.styleService.clearChatStyle(playerId);
    }

    private boolean hasSelected(final Player player) {
        return this.kind == StyleButtonKind.NICK
            ? this.styleService.nickStyleId(player.getUniqueId()).isPresent()
            : this.styleService.chatStyleId(player.getUniqueId()).isPresent();
    }
}
