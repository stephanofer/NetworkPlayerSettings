package com.stephanofer.networkplayersettings.settings.style;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class StylePatternRenderer {

    private final MiniMessage miniMessage;

    public StylePatternRenderer() {
        this.miniMessage = MiniMessage.miniMessage();
    }

    public Component renderNick(final StylePattern pattern, final String playerName) {
        Objects.requireNonNull(pattern, "pattern");
        return this.miniMessage.deserialize(
            pattern.miniMessage(),
            Placeholder.component("name", Component.text(playerName == null ? "" : playerName))
        );
    }

    public Component renderChat(final StylePattern pattern, final Component message) {
        Objects.requireNonNull(pattern, "pattern");
        return this.miniMessage.deserialize(
            pattern.miniMessage(),
            Placeholder.component("message", message == null ? Component.empty() : message)
        );
    }

    public String renderNickMiniMessage(final StylePattern pattern, final String playerName) {
        return pattern.miniMessage().replace("<name>", this.miniMessage.escapeTags(playerName == null ? "" : playerName));
    }

    public String renderChatPreviewMiniMessage(final StylePattern pattern) {
        return pattern.miniMessage().replace("<message>", this.miniMessage.escapeTags(pattern.previewText()));
    }
}
