package com.stephanofer.networkplayersettings.settings.style;

public enum StylePatternType {
    NICK("name"),
    CHAT("message");

    private final String requiredPlaceholder;

    StylePatternType(final String requiredPlaceholder) {
        this.requiredPlaceholder = requiredPlaceholder;
    }

    public String requiredPlaceholder() {
        return this.requiredPlaceholder;
    }
}
