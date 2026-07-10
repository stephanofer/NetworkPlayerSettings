package com.stephanofer.networkplayersettings.settings.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface PlayerStyleService {

    List<StylePatternInfo> nickPatterns();

    List<StylePatternInfo> chatPatterns();

    Optional<StylePatternInfo> nickPattern(String patternId);

    Optional<StylePatternInfo> chatPattern(String patternId);

    Optional<String> nickStyleId(UUID playerId);

    Optional<String> chatStyleId(UUID playerId);

    boolean canUseNickStyle(Player player, String patternId);

    boolean canUseChatStyle(Player player, String patternId);

    boolean hasActiveNickStyle(Player player);

    boolean hasActiveChatStyle(Player player);

    Component formattedNick(Player player);

    CompletableFuture<Component> formattedNick(NickStyleRenderRequest request);

    String formattedNickMiniMessage(Player player);

    String nickPreviewMiniMessage(Player player, String patternId);

    Optional<Component> formatChatMessage(Player player, Component message);

    String chatPreviewMiniMessage(Player player);

    String chatPreviewMiniMessage(String patternId);

    CompletableFuture<Void> setNickStyle(UUID playerId, String patternId);

    CompletableFuture<Void> clearNickStyle(UUID playerId);

    CompletableFuture<Void> setChatStyle(UUID playerId, String patternId);

    CompletableFuture<Void> clearChatStyle(UUID playerId);
}
