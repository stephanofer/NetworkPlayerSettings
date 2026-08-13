package com.stephanofer.networkplayersettingszmenu.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import com.stephanofer.networkplayersettingszmenu.settings.SettingFeedback;
import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class StyleFilterButton extends Button {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PluginMessages messages;
    private final StyleButtonKind kind;

    public StyleFilterButton(
        final JavaPlugin plugin,
        final PlayerSettingsService settingsService,
        final PluginMessages messages,
        final StyleButtonKind kind
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.messages = Objects.requireNonNull(messages, "messages");
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
        StylePatternFilterState.toggle(player.getUniqueId(), this.kind);
        SettingFeedback.filter(player);
        final Inventory menuInventory = inventory.getMenuInventory();
        if (menuInventory == null) {
            inventory.getPlugin().getInventoryManager().updateInventory(player);
            return;
        }
        inventory.getPlugin().getInventoryManager().openInventory(player, menuInventory, 1, inventory.getOldInventories());
    }

    @Override
    public void onRender(final Player player, final InventoryEngine inventory) {
        if (inventory.getPage() != this.getPage() && !this.isPermanent()) {
            return;
        }
        final Language language = this.settingsService.resolvedLanguage(player);
        final StylePatternFilter filter = StylePatternFilterState.get(player.getUniqueId(), this.kind);
        final Placeholders placeholders = new Placeholders();
        placeholders.register("style_filter", this.messages.get(language, filter == StylePatternFilter.ALL ? "menu.style.filter-all" : "menu.style.filter-available"));
        placeholders.register("style_filter_action", this.messages.get(language, filter == StylePatternFilter.ALL ? "menu.style.filter-action-available" : "menu.style.filter-action-all"));
        placeholders.register("filter_all_marker", filter == StylePatternFilter.ALL ? "#92ffff> " : "#555555  ");
        placeholders.register("filter_available_marker", filter == StylePatternFilter.AVAILABLE ? "#92ffff> " : "#555555  ");
        inventory.displayFinalButton(this, placeholders, this.getRealSlot(inventory.getInventory().getSize(), inventory.getPage()));
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }
}
