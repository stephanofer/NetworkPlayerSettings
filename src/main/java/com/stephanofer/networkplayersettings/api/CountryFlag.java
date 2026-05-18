package com.stephanofer.networkplayersettings.api;

import java.util.Locale;

public final class CountryFlag {

    public static final String UNKNOWN_CODE = "XX";
    public static final String UNKNOWN_FLAG = "🏳";

    private CountryFlag() {
    }

    public static String normalizeCode(final String raw) {
        if (raw == null) {
            return UNKNOWN_CODE;
        }
        final String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!isIsoAlpha2(code)) {
            return UNKNOWN_CODE;
        }
        return code;
    }

    public static boolean isIsoAlpha2(final String code) {
        return code != null
            && code.length() == 2
            && code.charAt(0) >= 'A'
            && code.charAt(0) <= 'Z'
            && code.charAt(1) >= 'A'
            && code.charAt(1) <= 'Z';
    }

    public static String emoji(final String rawCode) {
        final String code = normalizeCode(rawCode);
        if (UNKNOWN_CODE.equals(code)) {
            return UNKNOWN_FLAG;
        }

        final int first = Character.codePointAt(code, 0) - 'A' + 0x1F1E6;
        final int second = Character.codePointAt(code, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }
}
