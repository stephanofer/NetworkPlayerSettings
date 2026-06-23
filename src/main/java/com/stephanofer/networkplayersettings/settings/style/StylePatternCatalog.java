package com.stephanofer.networkplayersettings.settings.style;

import com.stephanofer.networkplayersettings.settings.api.StylePatternInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class StylePatternCatalog {

    private final StylePatternType type;
    private final Map<String, StylePattern> patterns;
    private final List<StylePatternInfo> infos;

    public StylePatternCatalog(final StylePatternType type, final List<StylePattern> patterns) {
        this.type = java.util.Objects.requireNonNull(type, "type");
        if (patterns == null || patterns.isEmpty()) {
            throw new IllegalArgumentException(type + " catalog must contain at least one pattern");
        }
        final Map<String, StylePattern> byId = new LinkedHashMap<>();
        for (final StylePattern pattern : patterns) {
            if (pattern.type() != type) {
                throw new IllegalArgumentException(pattern.id() + ": pattern type does not match catalog type");
            }
            if (byId.putIfAbsent(pattern.id(), pattern) != null) {
                throw new IllegalArgumentException("duplicate pattern id: " + pattern.id());
            }
        }
        this.patterns = Collections.unmodifiableMap(byId);
        this.infos = List.copyOf(byId.values().stream().map(StylePattern::toInfo).collect(Collectors.toCollection(ArrayList::new)));
    }

    public StylePatternType type() {
        return this.type;
    }

    public Optional<StylePattern> find(final String patternId) {
        if (patternId == null || patternId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.patterns.get(patternId.trim().toLowerCase(java.util.Locale.ROOT)));
    }

    public List<StylePatternInfo> infos() {
        return this.infos;
    }
}
