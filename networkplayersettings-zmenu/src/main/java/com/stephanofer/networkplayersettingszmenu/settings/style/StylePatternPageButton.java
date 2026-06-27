package com.stephanofer.networkplayersettingszmenu.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.api.StylePatternInfo;
import com.stephanofer.networkplayersettings.settings.language.Language;
import com.stephanofer.networkplayersettingszmenu.config.ZMenuPluginConfig;
import com.stephanofer.networkplayersettingszmenu.i18n.PluginMessages;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.engine.ItemButton;
import fr.maxlego08.menu.api.utils.Placeholders;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class StylePatternPageButton extends PaginateButton {

    private final JavaPlugin plugin;
    private final PlayerSettingsService settingsService;
    private final PlayerStyleService styleService;
    private final PluginMessages messages;
    private final StyleButtonKind kind;
    private final StylePatternSelectionHandler selectionHandler;

    public StylePatternPageButton(
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
        this.kind = Objects.requireNonNull(kind, "kind");
        this.selectionHandler = new StylePatternSelectionHandler(plugin, settingsService, styleService, messages, settingsConfig);
    }

    @Override
    public int getPaginationSize(final Player player) {
        return patterns(player).size();
    }

    @Override
    public void onRender(final Player player, final InventoryEngine inventory) {
        paginate(patterns(player), inventory, (slot, pattern) -> {
            final ItemButton itemButton = inventory.addItem(slot, itemStack(player, pattern));
            if (itemButton != null) {
                itemButton.setClick(event -> this.selectionHandler.select(player, inventory, this.kind, pattern));
            }
        });
    }

    private ItemStack itemStack(final Player player, final StylePatternInfo pattern) {
        final StyleItemState state = state(player, pattern.id());
        final ItemStack itemStack = getCustomItemStack(player, isUseCache(), placeholders(player, pattern)).clone();
        itemStack.setType(resolveMaterial(pattern.category(), state));
        if (state != StyleItemState.SELECTED) {
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

    private Placeholders placeholders(final Player player, final StylePatternInfo pattern) {
        final Language language = this.settingsService.resolvedLanguage(player);
        final boolean selected = isSelected(player, pattern.id());
        final boolean unlocked = canUse(player, pattern.id());
        final boolean active = selected && unlocked;
        final Placeholders placeholders = new Placeholders();
        placeholders.register("style_id", pattern.id());
        placeholders.register("style_name", pattern.displayName());
        placeholders.register("style_category", pattern.category());
        placeholders.register("style_permission", pattern.permission());
        placeholders.register("style_selected_marker", active ? "ACTIVE " : "");
        placeholders.register("style_lock_marker", unlocked ? "" : "LOCKED ");
        placeholders.register("style_state", !unlocked
            ? this.messages.get(language, "menu.style.locked-state")
            : active ? this.messages.get(language, "menu.style.selected-state") : this.messages.get(language, "menu.style.available-state"));
        placeholders.register("style_action", !unlocked
            ? this.messages.get(language, "menu.style.locked-action")
            : active ? this.messages.get(language, "menu.style.selected-action") : this.messages.get(language, "menu.style.select-action"));
        placeholders.register("style_preview", preview(player, pattern.id()));
        return placeholders;
    }

    private List<StylePatternInfo> patterns(final Player player) {
        final List<StylePatternInfo> patterns = this.kind == StyleButtonKind.NICK ? this.styleService.nickPatterns() : this.styleService.chatPatterns();
        final StylePatternFilter filter = StylePatternFilterState.get(player.getUniqueId(), this.kind);
        if (filter == StylePatternFilter.ALL) {
            return patterns;
        }
        return patterns.stream()
            .filter(pattern -> canUse(player, pattern.id()))
            .toList();
    }

    private String preview(final Player player, final String patternId) {
        if (this.kind == StyleButtonKind.NICK) {
            return this.styleService.nickPreviewMiniMessage(player, patternId);
        }
        return this.styleService.chatPreviewMiniMessage(patternId);
    }

    private boolean canUse(final Player player, final String patternId) {
        return this.kind == StyleButtonKind.NICK
            ? this.styleService.canUseNickStyle(player, patternId)
            : this.styleService.canUseChatStyle(player, patternId);
    }

    private boolean isSelected(final Player player, final String patternId) {
        final Optional<String> current = this.kind == StyleButtonKind.NICK
            ? this.styleService.nickStyleId(player.getUniqueId())
            : this.styleService.chatStyleId(player.getUniqueId());
        return current.map(patternId::equalsIgnoreCase).orElse(false);
    }

    private StyleItemState state(final Player player, final String patternId) {
        final boolean unlocked = canUse(player, patternId);
        if (!unlocked) {
            return StyleItemState.LOCKED;
        }
        return isSelected(player, patternId) ? StyleItemState.SELECTED : StyleItemState.AVAILABLE;
    }

    private Material resolveMaterial(final String category, final StyleItemState state) {
        if (state == StyleItemState.LOCKED) {
            return Material.GRAY_DYE;
        }
        return switch (category) {
            case "basic" -> Material.WHITE_DYE;
            case "clean" -> Material.LIGHT_BLUE_DYE;
            case "professional" -> Material.NETHERITE_INGOT;
            case "competitive" -> Material.DIAMOND_SWORD;
            case "dark" -> Material.BLACK_DYE;
            case "soft" -> Material.PINK_DYE;
            case "luxury" -> Material.GOLD_INGOT;
            case "premium" -> Material.NETHER_STAR;
            case "special" -> Material.AMETHYST_SHARD;
            case "pride" -> Material.FIREWORK_STAR;
            default -> Material.NAME_TAG;
        };
    }

    private enum StyleItemState {
        AVAILABLE,
        SELECTED,
        LOCKED
    }
}
