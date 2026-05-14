package com.stephanofer.networkplayersettings.api;

public enum Language {
    SPANISH("es", "Español", "Spanish"),
    ENGLISH("en", "Inglés", "English");

    private final String code;
    private final String spanishName;
    private final String englishName;

    Language(final String code, final String spanishName, final String englishName) {
        this.code = code;
        this.spanishName = spanishName;
        this.englishName = englishName;
    }

    public String code() {
        return this.code;
    }

    public String displayName(final Language viewerLanguage) {
        return viewerLanguage == SPANISH ? this.spanishName : this.englishName;
    }

    public static Language fromCode(final String rawCode) {
        if (rawCode != null && rawCode.equalsIgnoreCase(SPANISH.code)) {
            return SPANISH;
        }
        return ENGLISH;
    }
}
