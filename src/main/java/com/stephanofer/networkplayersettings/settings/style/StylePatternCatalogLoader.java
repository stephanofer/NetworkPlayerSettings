package com.stephanofer.networkplayersettings.settings.style;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import java.util.List;
import java.util.Objects;

public final class StylePatternCatalogLoader {

    private final StylePatternType type;

    public StylePatternCatalogLoader(final StylePatternType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public StylePatternCatalog load(final YamlDocument document) {
        Objects.requireNonNull(document, "document");
        final Section patternsSection = document.getSection("patterns");
        if (patternsSection == null || patternsSection.getKeys().isEmpty()) {
            throw invalidCatalog(document, "missing patterns section");
        }

        final List<StylePattern> patterns = patternsSection.getKeys().stream()
            .map(patternKey -> mapPattern(document, patternsSection, String.valueOf(patternKey)))
            .toList();

        try {
            return new StylePatternCatalog(this.type, patterns);
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(document, exception.getMessage(), exception);
        }
    }

    private StylePattern mapPattern(final YamlDocument document, final Section patternsSection, final String patternKey) {
        final Section patternSection = patternsSection.getSection(patternKey);
        if (patternSection == null) {
            throw invalidCatalog(document, "pattern entry is malformed: " + patternKey);
        }
        try {
            return new StylePattern(
                this.type,
                patternKey,
                patternSection.getString("display-name", ""),
                patternSection.getString("category", "basic"),
                patternSection.getString("permission", ""),
                patternSection.getString("mini-message", ""),
                patternSection.getString("preview", this.type == StylePatternType.NICK ? "Vendimia" : "This is my message")
            );
        } catch (final IllegalArgumentException exception) {
            throw invalidCatalog(document, patternKey + ": " + exception.getMessage(), exception);
        }
    }

    private static IllegalStateException invalidCatalog(final YamlDocument document, final String rule) {
        return new IllegalStateException("Invalid style catalog at " + document.getFile().toPath() + ": " + rule);
    }

    private static IllegalStateException invalidCatalog(final YamlDocument document, final String rule, final Exception cause) {
        return new IllegalStateException("Invalid style catalog at " + document.getFile().toPath() + ": " + rule, cause);
    }
}
