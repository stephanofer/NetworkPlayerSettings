package com.stephanofer.networkplayersettings.settings.style;

import com.stephanofer.networkplayersettings.settings.api.StylePatternInfo;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class StylePattern {

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]{2,64}");

    private final StylePatternType type;
    private final String id;
    private final String displayName;
    private final String category;
    private final String permission;
    private final String miniMessage;
    private final String previewText;

    public StylePattern(
        final StylePatternType type,
        final String id,
        final String displayName,
        final String category,
        final String permission,
        final String miniMessage,
        final String previewText
    ) {
        this.type = Objects.requireNonNull(type, "type");
        this.id = normalizeId(id);
        this.displayName = requireNotBlank(displayName, "displayName");
        this.category = requireNotBlank(category, "category").toLowerCase(Locale.ROOT);
        this.permission = permission == null ? "" : permission.trim();
        this.miniMessage = requireNotBlank(miniMessage, "miniMessage");
        this.previewText = requireNotBlank(previewText, "previewText");
        if (!this.miniMessage.contains("<" + type.requiredPlaceholder() + ">")) {
            throw new IllegalArgumentException(this.id + ": mini-message must contain <" + type.requiredPlaceholder() + ">");
        }
    }

    public StylePatternType type() {
        return this.type;
    }

    public String id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public String category() {
        return this.category;
    }

    public String permission() {
        return this.permission;
    }

    public String miniMessage() {
        return this.miniMessage;
    }

    public String previewText() {
        return this.previewText;
    }

    public StylePatternInfo toInfo() {
        return new StylePatternInfo(this.id, this.displayName, this.category, this.permission, this.previewText);
    }

    private static String normalizeId(final String raw) {
        final String normalized = requireNotBlank(raw, "id").toLowerCase(Locale.ROOT);
        if (!ID_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("invalid pattern id: " + raw);
        }
        return normalized;
    }

    private static String requireNotBlank(final String value, final String field) {
        final String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
