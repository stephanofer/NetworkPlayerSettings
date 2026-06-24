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
import org.junit.jupiter.api.Test;

class StyleResourcesTest {

    @Test
    void bundledNickCatalogLoads() {
        final StylePatternCatalog catalog = new StylePatternCatalogLoader(StylePatternType.NICK)
            .load(document(Path.of("src/main/resources/styles/nick-patterns.yml")));

        assertFalse(catalog.infos().isEmpty());
        assertTrue(catalog.find("ruby-gradient").isPresent());
        assertTrue(catalog.find("trans-pride").isPresent());
    }

    @Test
    void bundledChatCatalogLoads() {
        final StylePatternCatalog catalog = new StylePatternCatalogLoader(StylePatternType.CHAT)
            .load(document(Path.of("src/main/resources/styles/chat-patterns.yml")));

        assertFalse(catalog.infos().isEmpty());
        assertTrue(catalog.find("soft-emerald").isPresent());
        assertTrue(catalog.find("progress-pride-message").isPresent());
    }

    @Test
    void nickInventoryMatchesBundledCatalogExactly() {
        final Set<String> catalogIds = patternIds(document(Path.of("src/main/resources/styles/nick-patterns.yml")));
        final Set<String> inventoryIds = inventoryPatternIds(document(Path.of("networkplayersettings-zmenu/src/main/resources/inventories/nick-styles.yml")), "nps-nick-style-button");

        assertEquals(catalogIds, inventoryIds);
    }

    @Test
    void chatInventoryMatchesBundledCatalogExactly() {
        final Set<String> catalogIds = patternIds(document(Path.of("src/main/resources/styles/chat-patterns.yml")));
        final Set<String> inventoryIds = inventoryPatternIds(document(Path.of("networkplayersettings-zmenu/src/main/resources/inventories/chat-styles.yml")), "nps-chat-style-button");

        assertEquals(catalogIds, inventoryIds);
    }

    private static Set<String> patternIds(final YamlDocument document) {
        final Section patterns = document.getSection("patterns");
        return patterns.getKeys().stream()
            .map(String::valueOf)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> inventoryPatternIds(final YamlDocument document, final String expectedPatternFile) {
        final Section items = document.getSection("items");
        return items.getKeys().stream()
            .map(String::valueOf)
            .map(key -> items.getSection(key))
            .map(section -> section == null ? null : section.getSection("pattern"))
            .filter(pattern -> pattern != null && expectedPatternFile.equals(pattern.getString("file-name", "")))
            .map(pattern -> pattern.getString("style-id", ""))
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
