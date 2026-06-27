package com.stephanofer.networkplayersettingszmenu.settings.style;

public enum StylePatternFilter {
    ALL,
    AVAILABLE;

    public StylePatternFilter next() {
        return this == ALL ? AVAILABLE : ALL;
    }
}
