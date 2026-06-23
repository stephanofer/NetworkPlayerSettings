package com.stephanofer.networkplayersettings.settings.api;

import java.util.Objects;

public record StylePatternInfo(
    String id,
    String displayName,
    String category,
    String permission,
    String previewText
) {

    public StylePatternInfo {
        id = requireNotBlank(id, "id");
        displayName = requireNotBlank(displayName, "displayName");
        category = requireNotBlank(category, "category");
        permission = permission == null ? "" : permission.trim();
        previewText = requireNotBlank(previewText, "previewText");
    }

    private static String requireNotBlank(final String value, final String field) {
        final String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
