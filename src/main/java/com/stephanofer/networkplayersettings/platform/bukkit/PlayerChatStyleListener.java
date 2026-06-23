package com.stephanofer.networkplayersettings.platform.bukkit;

import com.stephanofer.networkplayersettings.settings.api.PlayerStyleService;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PlayerChatStyleListener implements Listener {

    private final PlayerStyleService styleService;
    private final PlainTextComponentSerializer plainText;

    public PlayerChatStyleListener(final PlayerStyleService styleService) {
        this.styleService = Objects.requireNonNull(styleService, "styleService");
        this.plainText = PlainTextComponentSerializer.plainText();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onAsyncChat(final AsyncChatEvent event) {
        if (!this.styleService.hasActiveChatStyle(event.getPlayer())) {
            return;
        }

        final String safeText = this.plainText.serialize(event.message());
        this.styleService.formatChatMessage(event.getPlayer(), Component.text(safeText))
            .ifPresent(event::message);
    }
}
