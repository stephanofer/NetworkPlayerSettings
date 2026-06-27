package com.stephanofer.networkplayersettingszmenu.settings.style;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StylePatternFilterState {

    private static final Map<UUID, EnumMap<StyleButtonKind, StylePatternFilter>> FILTERS = new ConcurrentHashMap<>();

    private StylePatternFilterState() {
    }

    public static StylePatternFilter get(final UUID playerId, final StyleButtonKind kind) {
        final EnumMap<StyleButtonKind, StylePatternFilter> filters = FILTERS.get(playerId);
        if (filters == null) {
            return StylePatternFilter.ALL;
        }
        return filters.getOrDefault(kind, StylePatternFilter.ALL);
    }

    public static StylePatternFilter toggle(final UUID playerId, final StyleButtonKind kind) {
        final EnumMap<StyleButtonKind, StylePatternFilter> filters = FILTERS.computeIfAbsent(playerId, unused -> new EnumMap<>(StyleButtonKind.class));
        final StylePatternFilter next = get(playerId, kind).next();
        filters.put(kind, next);
        return next;
    }

    public static void clear(final UUID playerId) {
        FILTERS.remove(playerId);
    }

    public static void clearAll() {
        FILTERS.clear();
    }
}
