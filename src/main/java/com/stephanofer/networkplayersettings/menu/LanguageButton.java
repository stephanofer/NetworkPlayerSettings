package com.stephanofer.networkplayersettings.menu;

import com.stephanofer.networkplatform.menus.MenuKey;
import com.stephanofer.networkplatform.menus.OpenOptions;
import com.stephanofer.networkplayersettings.NetworkPlayerSettingsPlugin;
import com.stephanofer.networkplayersettings.api.Language;
import com.stephanofer.networkplayersettings.api.LanguagePreference;
import com.stephanofer.networkplayersettings.api.PlayerSettingsSnapshot;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import java.util.Objects;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class LanguageButton extends Button {

    private static final MenuKey LANGUAGE_MENU = MenuKey.of("language");

    private final NetworkPlayerSettingsPlugin plugin;
    private final LanguagePreference preference;

    public LanguageButton(final NetworkPlayerSettingsPlugin plugin, final LanguagePreference preference) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
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
        this.plugin.settingsService().setLanguage(player.getUniqueId(), this.preference);
        final Language viewerLanguage = this.plugin.settingsService().resolvedLanguage(player);
        final Language selectedLanguage = switch (this.preference) {
            case AUTO -> this.plugin.settingsService().resolvedLanguage(player);
            case SPANISH -> Language.SPANISH;
            case ENGLISH -> Language.ENGLISH;
        };
        player.sendRichMessage(this.plugin.messages().get(
            viewerLanguage,
            "settings.language.updated",
            selectedLanguage.displayName(viewerLanguage)
        ));
        this.plugin.menuService().open(player, LANGUAGE_MENU, OpenOptions.page(inventory.getPage()).withoutHistory());
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
        final PlayerSettingsSnapshot snapshot = this.plugin.settingsService().getCachedOrDefault(player.getUniqueId());
        final Placeholders placeholders = new Placeholders();
        final Language viewerLanguage = this.plugin.settingsService().resolvedLanguage(player);
        final boolean selected = snapshot.languagePreference() == this.preference;

        placeholders.register("language_option", this.preference.storageValue());
        placeholders.register("language_selected", String.valueOf(selected));
        placeholders.register("selected_marker", selected ? "✔ " : "");
        placeholders.register("selected_state", selected
            ? this.plugin.messages().get(viewerLanguage, "menu.language.selected-state")
            : this.plugin.messages().get(viewerLanguage, "menu.language.available-state"));
        placeholders.register("effective_language", this.plugin.settingsService().resolvedLanguage(player).code());
        placeholders.register("current_preference", snapshot.languagePreference().storageValue());
        return placeholders;
    }

    private boolean isSelected(final Player player) {
        return this.plugin.settingsService().getCachedOrDefault(player.getUniqueId()).languagePreference() == this.preference;
    }
}
