package com.stephanofer.networkplayersettings.settings.style;

import com.stephanofer.networkplayersettings.settings.api.NickStyleRenderRequest;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsService;
import com.stephanofer.networkplayersettings.settings.api.PlayerSettingsSnapshot;
import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import com.stephanofer.networkplayersettings.settings.api.SettingKey;
import com.stephanofer.networkplayersettings.settings.api.StylePermissionChecker;
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
    private volatile Catalogs catalogs;
    private final StylePatternRenderer renderer;

    public DefaultPlayerStyleService(
        final PlayerSettingsService settingsService,
        final StylePatternCatalog nickCatalog,
        final StylePatternCatalog chatCatalog,
        final StylePatternRenderer renderer
    ) {
        this.settingsService = Objects.requireNonNull(settingsService, "settingsService");
        this.catalogs = validatedCatalogs(nickCatalog, chatCatalog);
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public List<StylePatternInfo> nickPatterns() {
        return this.catalogs.nick().infos();
    }

    @Override
    public List<StylePatternInfo> chatPatterns() {
        return this.catalogs.chat().infos();
    }

    public void replaceCatalogs(final StylePatternCatalog nickCatalog, final StylePatternCatalog chatCatalog) {
        this.catalogs = validatedCatalogs(nickCatalog, chatCatalog);
    }

    private static Catalogs validatedCatalogs(final StylePatternCatalog nickCatalog, final StylePatternCatalog chatCatalog) {
        if (Objects.requireNonNull(nickCatalog, "nickCatalog").type() != StylePatternType.NICK) {
            throw new IllegalArgumentException("nickCatalog must contain nick patterns");
        }
        if (Objects.requireNonNull(chatCatalog, "chatCatalog").type() != StylePatternType.CHAT) {
            throw new IllegalArgumentException("chatCatalog must contain chat patterns");
        }
        return new Catalogs(nickCatalog, chatCatalog);
    }

    @Override
    public Optional<StylePatternInfo> nickPattern(final String patternId) {
        return this.catalogs.nick().find(patternId).map(StylePattern::toInfo);
    }

    @Override
    public Optional<StylePatternInfo> chatPattern(final String patternId) {
        return this.catalogs.chat().find(patternId).map(StylePattern::toInfo);
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
        return canUse(player, this.catalogs.nick().find(patternId));
    }

    @Override
    public boolean canUseChatStyle(final Player player, final String patternId) {
        return canUse(player, this.catalogs.chat().find(patternId));
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
    public CompletableFuture<Component> formattedNick(final NickStyleRenderRequest request) {
        Objects.requireNonNull(request, "request");
        return this.settingsService.load(request.playerId())
            .thenApply(snapshot -> activeNickPattern(snapshot, request.permissionChecker())
                .map(pattern -> this.renderer.renderNick(pattern, request.username()))
                .orElseGet(() -> Component.text(request.username()))
            );
    }

    @Override
    public String formattedNickMiniMessage(final Player player) {
        return activeNickPattern(player)
            .map(pattern -> this.renderer.renderNickMiniMessage(pattern, player.getName()))
            .orElseGet(player::getName);
    }

    @Override
    public String nickPreviewMiniMessage(final Player player, final String patternId) {
        return this.catalogs.nick().find(patternId)
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
        return this.catalogs.chat().find(patternId)
            .map(this.renderer::renderChatPreviewMiniMessage)
            .orElse("");
    }

    @Override
    public CompletableFuture<Void> setNickStyle(final UUID playerId, final String patternId) {
        final String normalized = normalizePatternId(patternId);
        if (this.catalogs.nick().find(normalized).isEmpty()) {
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
        if (this.catalogs.chat().find(normalized).isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("unknown chat style: " + patternId));
        }
        return this.settingsService.setSetting(playerId, SettingKey.CHAT_STYLE, normalized);
    }

    @Override
    public CompletableFuture<Void> clearChatStyle(final UUID playerId) {
        return this.settingsService.setSetting(playerId, SettingKey.CHAT_STYLE, "");
    }

    private Optional<StylePattern> activeNickPattern(final Player player) {
        Objects.requireNonNull(player, "player");
        return activePattern(
            this.settingsService.getCachedOrDefault(player.getUniqueId()),
            SettingKey.NICK_STYLE,
            this.catalogs.nick(),
            player::hasPermission
        );
    }

    private Optional<StylePattern> activeChatPattern(final Player player) {
        Objects.requireNonNull(player, "player");
        return activePattern(
            this.settingsService.getCachedOrDefault(player.getUniqueId()),
            SettingKey.CHAT_STYLE,
            this.catalogs.chat(),
            player::hasPermission
        );
    }

    private Optional<StylePattern> activeNickPattern(
        final PlayerSettingsSnapshot snapshot,
        final StylePermissionChecker permissionChecker
    ) {
        return activePattern(snapshot, SettingKey.NICK_STYLE, this.catalogs.nick(), permissionChecker);
    }

    private static Optional<StylePattern> activePattern(
        final PlayerSettingsSnapshot snapshot,
        final SettingKey key,
        final StylePatternCatalog catalog,
        final StylePermissionChecker permissionChecker
    ) {
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshot.setting(key)
            .flatMap(catalog::find)
            .filter(pattern -> canUse(permissionChecker, pattern));
    }

    private static boolean canUse(final Player player, final Optional<StylePattern> pattern) {
        return player != null && pattern.filter(value -> canUse(player::hasPermission, value)).isPresent();
    }

    private static boolean canUse(final StylePermissionChecker permissionChecker, final StylePattern pattern) {
        Objects.requireNonNull(permissionChecker, "permissionChecker");
        Objects.requireNonNull(pattern, "pattern");
        return pattern.permission().isBlank() || permissionChecker.hasPermission(pattern.permission());
    }

    private static String normalizePatternId(final String patternId) {
        return patternId == null ? "" : patternId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private record Catalogs(StylePatternCatalog nick, StylePatternCatalog chat) {
    }
}
