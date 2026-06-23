package com.stephanofer.networkplayersettings.settings.style;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StylePatternTest {

    @Test
    void rejectsNickPatternWithoutNamePlaceholder() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new StylePattern(
            StylePatternType.NICK,
            "ruby",
            "Ruby",
            "premium",
            "networkplayersettings.nick.ruby",
            "<gradient:#ff4d6d:#c9184a>Vendimia</gradient>",
            "Vendimia"
        ));

        assertEquals("ruby: mini-message must contain <name>", error.getMessage());
    }

    @Test
    void rejectsChatPatternWithoutMessagePlaceholder() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new StylePattern(
            StylePatternType.CHAT,
            "aurora",
            "Aurora",
            "premium",
            "networkplayersettings.chat.aurora",
            "<gradient:#80ffdb:#5390d9>hello</gradient>",
            "hello"
        ));

        assertEquals("aurora: mini-message must contain <message>", error.getMessage());
    }

    @Test
    void rejectsInvalidPatternIds() {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new StylePattern(
            StylePatternType.NICK,
            "Ruby Gradient",
            "Ruby",
            "premium",
            "networkplayersettings.nick.ruby",
            "<red><name></red>",
            "Vendimia"
        ));

        assertEquals("invalid pattern id: Ruby Gradient", error.getMessage());
    }
}
