package com.stephanofer.networkplayersettingszmenu.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.api.StylePatternInfo;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.settings.SettingMutationCooldowns;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class StylePatternButton extends Button {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PlayerStyleService styleService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;
    private final StyleButtonKind kind;
    private final String patternId;

    public StylePatternButton(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PlayerStyleService styleService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig,
        final StyleButtonKind kind,
        final String patternId
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.styleService = Objects.requireNonNull(styleService, "styleService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.settingsConfig = Objects.requireNonNull(settingsConfig, "settingsConfig");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.patternId = Objects.requireNonNull(patternId, "patternId").trim().toLowerCase(java.util.Locale.ROOT);
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
            return;
        }

        final Optional<StylePatternInfo> pattern = pattern();
        if (pattern.isEmpty()) {
            player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.style.missing-pattern"));
            return;
        }

        if (!canUse(player)) {
            player.sendRichMessage(this.messages.get(
                this.settingsService.resolvedLanguage(player),
                "settings.style.locked",
                pattern.get().permission()
            ));
            return;
        }

        if (isSelected(player)) {
            player.sendRichMessage(this.messages.get(
                this.settingsService.resolvedLanguage(player),
                this.kind == StyleButtonKind.NICK ? "settings.nick.already-selected" : "settings.chat.already-selected"
            ));
            return;
        }

        final SettingMutationCooldowns.Cooldown cooldown = SettingMutationCooldowns.get(player.getUniqueId());
        if (cooldown != null) {
            final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
            final long seconds = Math.max(1L, (cooldown.expiresAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
            player.sendRichMessage(this.messages.get(viewerLanguage, "settings.style.cooldown", seconds));
            return;
        }

        SettingMutationCooldowns.put(player.getUniqueId(), Math.max(0L, this.settingsConfig.mutationCooldownMillis()));
        mutate(player.getUniqueId()).whenComplete((unused, throwable) -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (throwable != null) {
                SettingMutationCooldowns.clear(player.getUniqueId());
                player.sendRichMessage(this.messages.get(this.settingsService.resolvedLanguage(player), "settings.style.update-failed"));
                return;
            }

            player.sendRichMessage(this.messages.get(
                this.settingsService.resolvedLanguage(player),
                this.kind == StyleButtonKind.NICK ? "settings.nick.updated" : "settings.chat.updated"
            ));
            onRender(player, inventory);
        }));
    }

    @Override
    public void onRender(final Player player, final InventoryEngine inventory) {
        if (inventory.getPage() != this.getPage() && !this.isPermanent()) {
            return;
        }
        inventory.displayFinalButton(this, buildPlaceholders(player), this.getRealSlot(inventory.getInventory().getSize(), inventory.getPage()));
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    @Override
    public ItemStack getCustomItemStack(final Player player, final boolean useCache, final Placeholders placeholders) {
        final ItemStack itemStack = super.getCustomItemStack(player, useCache, placeholders).clone();
        if (!isSelected(player) || !canUse(player)) {
            return itemStack;
        }
        final ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    private CompletableFuture<Void> mutate(final UUID playerId) {
        return this.kind == StyleButtonKind.NICK
            ? this.styleService.setNickStyle(playerId, this.patternId)
            : this.styleService.setChatStyle(playerId, this.patternId);
    }

    private Placeholders buildPlaceholders(final Player player) {
        final Language language = this.settingsService.resolvedLanguage(player);
        final Optional<StylePatternInfo> pattern = pattern();
        final boolean selected = isSelected(player);
        final boolean unlocked = canUse(player);
        final boolean active = selected && unlocked;
        final Placeholders placeholders = new Placeholders();
        placeholders.register("style_id", this.patternId);
        placeholders.register("style_name", pattern.map(StylePatternInfo::displayName).orElse(this.patternId));
        placeholders.register("style_category", pattern.map(StylePatternInfo::category).orElse("unknown"));
        placeholders.register("style_permission", pattern.map(StylePatternInfo::permission).orElse(""));
        placeholders.register("style_selected_marker", active ? "ACTIVE " : "");
        placeholders.register("style_lock_marker", unlocked ? "" : "LOCKED ");
        placeholders.register("style_state", !unlocked
            ? this.messages.get(language, "menu.style.locked-state")
            : active ? this.messages.get(language, "menu.style.selected-state") : this.messages.get(language, "menu.style.available-state"));
        placeholders.register("style_action", !unlocked
            ? this.messages.get(language, "menu.style.locked-action")
            : active ? this.messages.get(language, "menu.style.selected-action") : this.messages.get(language, "menu.style.select-action"));
        placeholders.register("style_preview", preview(player));
        return placeholders;
    }

    private String preview(final Player player) {
        if (this.kind == StyleButtonKind.NICK) {
            return this.styleService.nickPreviewMiniMessage(player, this.patternId);
        }
        return this.styleService.chatPreviewMiniMessage(this.patternId);
    }

    private Optional<StylePatternInfo> pattern() {
        return this.kind == StyleButtonKind.NICK
            ? this.styleService.nickPattern(this.patternId)
            : this.styleService.chatPattern(this.patternId);
    }

    private boolean canUse(final Player player) {
        return this.kind == StyleButtonKind.NICK
            ? this.styleService.canUseNickStyle(player, this.patternId)
            : this.styleService.canUseChatStyle(player, this.patternId);
    }

    private boolean isSelected(final Player player) {
        final Optional<String> current = this.kind == StyleButtonKind.NICK
            ? this.styleService.nickStyleId(player.getUniqueId())
            : this.styleService.chatStyleId(player.getUniqueId());
        return current.map(this.patternId::equalsIgnoreCase).orElse(false);
    }
}
