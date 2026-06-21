package com.stephanofer.networkplayersettingszmenu.settings.language;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettings.settings.language.LanguagePreference;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
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

public final class LanguageButton extends Button {

    private static final Cache<UUID, Cooldown> COOLDOWNS = Caffeine.newBuilder()
        .maximumSize(10_000L)
        .expireAfter(Expiry.creating((UUID playerId, Cooldown cooldown) -> Duration.ofMillis(cooldown.durationMillis())))
        .build();

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PluginMessages messages;
    private final ZMenuPluginConfig.SettingsSection settingsConfig;
    private final LanguagePreference preference;

    public LanguageButton(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PluginMessages messages,
        final ZMenuPluginConfig.SettingsSection settingsConfig,
        final LanguagePreference preference
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.settingsConfig = Objects.requireNonNull(settingsConfig, "settingsConfig");
        this.preference = Objects.requireNonNull(preference, "preference");
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

        final PlayerSettingsSnapshot snapshot = this.settingsService.getCachedOrDefault(player.getUniqueId());
        if (snapshot.languagePreference() == this.preference) {
            final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
            player.sendRichMessage(this.messages.get(
                viewerLanguage,
                "settings.language.already-selected",
                currentSelectionDisplayName(viewerLanguage)
            ));
            return;
        }

        final long cooldownMillis = Math.max(0L, this.settingsConfig.languageChangeCooldownMillis());
        final Cooldown cooldown = COOLDOWNS.getIfPresent(player.getUniqueId());
        if (cooldown != null) {
            final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
            final long seconds = Math.max(1L, (cooldown.expiresAtMillis() - System.currentTimeMillis() + 999L) / 1000L);
            player.sendRichMessage(this.messages.get(viewerLanguage, "settings.language.cooldown", seconds));
            return;
        }

        if (cooldownMillis > 0L) {
            COOLDOWNS.put(player.getUniqueId(), new Cooldown(System.currentTimeMillis() + cooldownMillis, cooldownMillis));
        }

        this.settingsService.setLanguage(player.getUniqueId(), this.preference)
            .whenComplete((unused, throwable) -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (throwable != null) {
                    COOLDOWNS.invalidate(player.getUniqueId());
                    player.sendRichMessage(this.messages.get(
                        this.settingsService.resolvedLanguage(player),
                        "settings.language.update-failed"
                    ));
                    return;
                }

                final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
                player.sendRichMessage(this.messages.get(
                    viewerLanguage,
                    "settings.language.updated",
                    selectedLanguage(player).displayName(viewerLanguage)
                ));
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
        if (!isSelected(player)) {
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
        final Placeholders placeholders = new Placeholders();
        final Language viewerLanguage = this.settingsService.resolvedLanguage(player);
        final boolean selected = snapshot.languagePreference() == this.preference;

        placeholders.register("language_option", this.preference.storageValue());
        placeholders.register("language_selected", String.valueOf(selected));
        placeholders.register("selected_marker", selected ? "✔ " : "");
        placeholders.register("selected_state", selected
            ? this.messages.get(viewerLanguage, "menu.language.selected-state")
            : this.messages.get(viewerLanguage, "menu.language.available-state"));
        placeholders.register("effective_language", this.settingsService.resolvedLanguage(player).code());
        placeholders.register("current_preference", snapshot.languagePreference().storageValue());
        return placeholders;
    }

    private boolean isSelected(final Player player) {
        return this.settingsService.getCachedOrDefault(player.getUniqueId()).languagePreference() == this.preference;
    }

    private String currentSelectionDisplayName(final Language viewerLanguage) {
        return switch (this.preference) {
            case AUTO -> this.messages.get(viewerLanguage, "settings.language.auto-name");
            case SPANISH -> Language.SPANISH.displayName(viewerLanguage);
            case ENGLISH -> Language.ENGLISH.displayName(viewerLanguage);
        };
    }

    private Language selectedLanguage(final Player player) {
        return switch (this.preference) {
            case AUTO -> this.settingsService.resolvedLanguage(player);
            case SPANISH -> Language.SPANISH;
            case ENGLISH -> Language.ENGLISH;
        };
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
