package com.stephanofer.networkplayersettingszmenu.settings.country;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class CountryFlagButton extends Button {

    private static final Cache<UUID, Cooldown> COOLDOWNS = Caffeine.newBuilder()
        .maximumSize(10_000L)
        .expireAfter(Expiry.creating((UUID playerId, Cooldown cooldown) -> Duration.ofMillis(cooldown.durationMillis())))
        .build();

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;

    public CountryFlagButton(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.settingsConfig = Objects.requireNonNull(settingsConfig, "settingsConfig");
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

        final long cooldownMillis = Math.max(0L, this.settingsConfig.countryFlagToggleCooldownMillis());
        final Cooldown cooldown = COOLDOWNS.getIfPresent(player.getUniqueId());
        if (cooldown != null) {
            final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
            final long seconds = Math.max(1L, (cooldown.expiresAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
            player.sendRichMessage(this.messages.get(viewerLanguage, "settings.country-flag.cooldown", seconds));
            return;
        }

        final boolean nextState = !this.settingsService.showCountryFlag(player.getUniqueId());
        if (cooldownMillis > 0L) {
            COOLDOWNS.put(player.getUniqueId(), new Cooldown(System.currentTimeMillis() + cooldownMillis, cooldownMillis));
        }

        this.settingsService.setShowCountryFlag(player.getUniqueId(), nextState)
            .whenComplete((unused, throwable) -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (throwable != null) {
                    COOLDOWNS.invalidate(player.getUniqueId());
                    player.sendRichMessage(this.messages.get(
                        this.settingsService.resolvedLanguage(player),
                        "settings.country-flag.update-failed"
                    ));
                    return;
                }

                final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
                player.sendRichMessage(this.messages.get(
                    viewerLanguage,
                    nextState ? "settings.country-flag.enabled" : "settings.country-flag.disabled"
                ));
                onRender(player, inventory);
            }));
    }

    @Override
    public void onRender(final Player player, final InventoryEngine inventory) {
        if (inventory.getPage() != this.getPage() && !this.isPermanent()) {
            return;
        }

        final Placeholders placeholders = buildPlaceholders(player);
        inventory.displayFinalButton(this, placeholders, this.getRealSlot(inventory.getInventory().getSize(), inventory.getPage()));
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    @Override
    public ItemStack getCustomItemStack(final Player player, final boolean useCache, final Placeholders placeholders) {
        final ItemStack itemStack = super.getCustomItemStack(player, useCache, placeholders).clone();
        if (!this.settingsService.showCountryFlag(player.getUniqueId())) {
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

    private Placeholders buildPlaceholders(final Player player) {
        final PlayerSettingsSnapshot snapshot = this.settingsService.getCachedOrDefault(player.getUniqueId());
        final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
        final boolean enabled = snapshot.showCountryFlag();
        final Placeholders placeholders = new Placeholders();

        placeholders.register("country_flag_enabled", Boolean.toString(enabled));
        placeholders.register("country_flag_marker", enabled ? "✔ " : "");
        placeholders.register("country_flag_state", enabled
            ? this.messages.get(viewerLanguage, "menu.country-flag.enabled-state")
            : this.messages.get(viewerLanguage, "menu.country-flag.disabled-state"));
        placeholders.register("country_flag_action", enabled
            ? this.messages.get(viewerLanguage, "menu.country-flag.disable-action")
            : this.messages.get(viewerLanguage, "menu.country-flag.enable-action"));
        placeholders.register("country_code", snapshot.countryCode());
        return placeholders;
    }

    public static void clearCooldown(final UUID playerId) {
        COOLDOWNS.invalidate(playerId);
    }

    public static void clearCooldowns() {
        COOLDOWNS.invalidateAll();
    }

    private record Cooldown(long expiresAtMillis, long durationMillis) {
    }
}
