package com.stephanofer.networkplayersettings.settings.style;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class StylePatternRenderer {

    private final MiniMessage miniMessage;

    public StylePatternRenderer() {
            /*
     * Antes se usaba:
     *
     * this.miniMessage = MiniMessage.miniMessage(MiniMessage.Preset.FORMATTED_TEXT);
     *
     * Problema:
     * MiniMessage.Preset.FORMATTED_TEXT solo existe en versiones nuevas de Adventure/MiniMessage.
     * En algunos servidores Paper, especialmente si usan Adventure 4.x, esa clase no está disponible
     * en runtime y el plugin falla al iniciar con:
     *
     * NoClassDefFoundError: net/kyori/adventure/text/minimessage/MiniMessage$Preset
     *
     * Solución actual:
     * Usamos MiniMessage.miniMessage(), que es compatible con Adventure 4.x y funciona con Paper.
     *
     * Más adelante cuanjdo subamos de version el servidor queremos volver a usar MiniMessage.Preset.FORMATTED_TEXT,
     * debemos agregar Adventure/MiniMessage 5.x correctamente al plugin usando ShadowJar
     * y revisar que no haya conflicto con las clases Adventure que ya trae Paper.
     */
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
