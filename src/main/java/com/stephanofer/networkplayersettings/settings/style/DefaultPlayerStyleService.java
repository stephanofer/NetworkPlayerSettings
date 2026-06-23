package com.stephanofer.networkplayersettings.settings.style;

import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.api.SettingKey;
import com.stephanofer.networkplayersettings.settings.api.StylePatternInfo;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public final class DefaultPlayerStyleService implements PlayerStyleService {

    private final PlayerSettingsService settingsService;
    private final StylePatternCatalog nickCatalog;
    private final StylePatternCatalog chatCatalog;
    private final StylePatternRenderer renderer;

    public DefaultPlayerStyleService(
        final PlayerSettingsService settingsService,
        final StylePatternCatalog nickCatalog,
        final StylePatternCatalog chatCatalog,
        final StylePatternRenderer renderer
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.nickCatalog = Objects.requireNonNull(nickCatalog, "nickCatalog");
        this.chatCatalog = Objects.requireNonNull(chatCatalog, "chatCatalog");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public List<StylePatternInfo> nickPatterns() {
        return this.nickCatalog.infos();
    }

    @Override
    public List<StylePatternInfo> chatPatterns() {
        return this.chatCatalog.infos();
    }

    @Override
    public Optional<StylePatternInfo> nickPattern(final String patternId) {
        return this.nickCatalog.find(patternId).map(StylePattern::toInfo);
    }

    @Override
    public Optional<StylePatternInfo> chatPattern(final String patternId) {
        return this.chatCatalog.find(patternId).map(StylePattern::toInfo);
    }

    @Override
    public Optional<String> nickStyleId(final UUID playerId) {
        return this.settingsService.getSetting(playerId, SettingKey.NICK_STYLE);
    }

    @Override
    public Optional<String> chatStyleId(final UUID playerId) {
        return this.settingsService.getSetting(playerId, SettingKey.CHAT_STYLE);
    }

    @Override
    public boolean canUseNickStyle(final Player player, final String patternId) {
        return canUse(player, this.nickCatalog.find(patternId));
    }

    @Override
    public boolean canUseChatStyle(final Player player, final String patternId) {
        return canUse(player, this.chatCatalog.find(patternId));
    }

    @Override
    public boolean hasActiveNickStyle(final Player player) {
        return activeNickPattern(player).isPresent();
    }

    @Override
    public boolean hasActiveChatStyle(final Player player) {
        return activeChatPattern(player).isPresent();
    }

    @Override
    public Component formattedNick(final Player player) {
        return activeNickPattern(player)
            .map(pattern -> this.renderer.renderNick(pattern, player.getName()))
            .orElseGet(() -> Component.text(player.getName()));
    }

    @Override
    public String formattedNickMiniMessage(final Player player) {
        return activeNickPattern(player)
            .map(pattern -> this.renderer.renderNickMiniMessage(pattern, player.getName()))
            .orElseGet(player::getName);
    }

    @Override
    public String nickPreviewMiniMessage(final Player player, final String patternId) {
        return this.nickCatalog.find(patternId)
            .map(pattern -> this.renderer.renderNickMiniMessage(pattern, player.getName()))
            .orElseGet(player::getName);
    }

    @Override
    public Optional<Component> formatChatMessage(final Player player, final Component message) {
        return activeChatPattern(player).map(pattern -> this.renderer.renderChat(pattern, message));
    }

    @Override
    public String chatPreviewMiniMessage(final Player player) {
        return activeChatPattern(player)
            .map(this.renderer::renderChatPreviewMiniMessage)
            .orElse("");
    }

    @Override
    public String chatPreviewMiniMessage(final String patternId) {
        return this.chatCatalog.find(patternId)
            .map(this.renderer::renderChatPreviewMiniMessage)
            .orElse("");
    }

    @Override
    public CompletableFuture<Void> setNickStyle(final UUID playerId, final String patternId) {
        final String normalized = normalizePatternId(patternId);
        if (this.nickCatalog.find(normalized).isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("unknown nick style: " + patternId));
        }
        return this.settingsService.setSetting(playerId, SettingKey.NICK_STYLE, normalized);
    }

    @Override
    public CompletableFuture<Void> clearNickStyle(final UUID playerId) {
        return this.settingsService.setSetting(playerId, SettingKey.NICK_STYLE, "");
    }

    @Override
    public CompletableFuture<Void> setChatStyle(final UUID playerId, final String patternId) {
        final String normalized = normalizePatternId(patternId);
        if (this.chatCatalog.find(normalized).isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("unknown chat style: " + patternId));
        }
        return this.settingsService.setSetting(playerId, SettingKey.CHAT_STYLE, normalized);
    }

    @Override
    public CompletableFuture<Void> clearChatStyle(final UUID playerId) {
        return this.settingsService.setSetting(playerId, SettingKey.CHAT_STYLE, "");
    }

    private Optional<StylePattern> activeNickPattern(final Player player) {
        return activePattern(player, SettingKey.NICK_STYLE, this.nickCatalog);
    }

    private Optional<StylePattern> activeChatPattern(final Player player) {
        return activePattern(player, SettingKey.CHAT_STYLE, this.chatCatalog);
    }

    private Optional<StylePattern> activePattern(final Player player, final SettingKey key, final StylePatternCatalog catalog) {
        Objects.requireNonNull(player, "player");
        final Optional<StylePattern> pattern = this.settingsService.getSetting(player.getUniqueId(), key).flatMap(catalog::find);
        return pattern.filter(value -> canUse(player, Optional.of(value)));
    }

    private static boolean canUse(final Player player, final Optional<StylePattern> pattern) {
        if (player == null || pattern.isEmpty()) {
            return false;
        }
        final String permission = pattern.get().permission();
        return permission.isBlank() || player.hasPermission(permission);
    }

    private static String normalizePatternId(final String patternId) {
        return patternId == null ? "" : patternId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
