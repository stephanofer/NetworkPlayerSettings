package com.stephanofer.networkplayersettings.settings.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

class StylePatternRendererTest {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final StylePatternRenderer renderer = new StylePatternRenderer();

    @Test
    void escapesPlayerSuppliedTagsInNickMiniMessage() {
        final StylePattern pattern = new StylePattern(
            StylePatternType.NICK,
            "safe",
            "Safe",
            "basic",
            "",
            "<blue><name></blue>",
            "Vendimia"
        );

        final String rendered = this.renderer.renderNickMiniMessage(pattern, "<red>boom</red>");
        final Component component = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(rendered);

        assertEquals("<red>boom</red>", PLAIN_TEXT.serialize(component));
    }
    @Test
    void formattedTextPresetSupportsVisualTagsUsedByCatalog() {
        final StylePattern pattern = new StylePattern(
            StylePatternType.NICK,
            "aurora-pride",
            "Aurora Pride",
            "special",
            "",
            "<shadow:#00ffff80><pride><name></pride>",
            "Vendimia"
        );

        final Component rendered = this.renderer.renderNick(pattern, "Vendimia");

        final String plainText = PLAIN_TEXT.serialize(rendered);
        assertFalse(plainText.contains("<shadow:"));
        assertFalse(plainText.contains("<pride"));
    }
}
