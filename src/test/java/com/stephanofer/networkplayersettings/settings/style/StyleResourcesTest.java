package com.stephanofer.networkplayersettings.settings.style;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Test;

class StyleResourcesTest {

    @Test
    void bundledNickCatalogLoads() {
        final StylePatternCatalog catalog = new StylePatternCatalogLoader(StylePatternType.NICK)
            .load(document(Path.of("src/main/resources/styles/nick-patterns.yml")));

        assertTrue(catalog.infos().size() >= 70);
        assertTrue(catalog.find("aurora").isPresent());
        assertTrue(catalog.find("ranked-red").isPresent());
        assertTrue(catalog.find("pride-trans").isPresent());
    }

    @Test
    void bundledChatCatalogLoads() {
        final StylePatternCatalog catalog = new StylePatternCatalogLoader(StylePatternType.CHAT)
            .load(document(Path.of("src/main/resources/styles/chat-patterns.yml")));

        assertTrue(catalog.infos().size() >= 60);
        assertTrue(catalog.find("aurora-message").isPresent());
        assertTrue(catalog.find("ranked-crimson").isPresent());
        assertTrue(catalog.find("pride-progress-message").isPresent());
    }

    @Test
    void nickInventoryUsesPaginatedCatalogButton() {
        final YamlDocument inventory = document(Path.of("networkplayersettings-zmenu/src/main/resources/inventories/nick-styles.yml"));

        assertPaginatedStyleButton(inventory, "NPS_NICK_STYLE");
        assertEquals("NPS_NICK_STYLE_FILTER", inventory.getString("items.filter.type", ""));
    }

    @Test
    void chatInventoryUsesPaginatedCatalogButton() {
        final YamlDocument inventory = document(Path.of("networkplayersettings-zmenu/src/main/resources/inventories/chat-styles.yml"));

        assertPaginatedStyleButton(inventory, "NPS_CHAT_STYLE");
        assertEquals("NPS_CHAT_STYLE_FILTER", inventory.getString("items.filter.type", ""));
    }

    @Test
    void bundledCatalogVisualTagsParseWithMiniMessage() {
        final MiniMessage miniMessage = MiniMessage.miniMessage();
        final StylePatternRenderer renderer = new StylePatternRenderer();
        final StylePatternCatalog nickCatalog = new StylePatternCatalogLoader(StylePatternType.NICK)
            .load(document(Path.of("src/main/resources/styles/nick-patterns.yml")));
        final StylePatternCatalog chatCatalog = new StylePatternCatalogLoader(StylePatternType.CHAT)
            .load(document(Path.of("src/main/resources/styles/chat-patterns.yml")));

        for (final String id : Set.of("aurora", "rainbow-clean", "pride-trans", "transition-ocean")) {
            final String rendered = renderer.renderNickMiniMessage(nickCatalog.find(id).orElseThrow(), "Vendimia");
            assertFalse(miniMessage.deserialize(rendered).equals(Component.empty()));
        }
        for (final String id : Set.of("aurora-message", "rainbow-message", "pride-progress-message", "transition-tide")) {
            final String rendered = renderer.renderChatPreviewMiniMessage(chatCatalog.find(id).orElseThrow());
            assertFalse(miniMessage.deserialize(rendered).equals(Component.empty()));
        }
    }

    private static void assertPaginatedStyleButton(final YamlDocument document, final String expectedType) {
        final Section items = document.getSection("items");
        final Section styles = items.getSection("styles");
        assertEquals(expectedType, styles.getString("type", ""));
        assertFalse(styles.getStringList("slots").isEmpty());
        assertTrue(styles.getString("style-id", "").isBlank());
    }

    @SuppressWarnings("unused")
    private static Set<String> patternIds(final YamlDocument document) {
        final Section patterns = document.getSection("patterns");
        return patterns.getKeys().stream()
            .map(String::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static YamlDocument document(final Path path) {
        try {
            return YamlDocument.create(path.toFile(), new ByteArrayInputStream(Files.readString(path, UTF_8).getBytes(UTF_8)));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to load test document: " + path, exception);
        }
    }
}
