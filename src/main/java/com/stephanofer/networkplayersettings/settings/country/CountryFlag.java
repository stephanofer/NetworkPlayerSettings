package com.stephanofer.networkplayersettings.settings.country;

import java.util.Locale;

public final class CountryFlag {

    public static final String UNKNOWN_CODE = "XX";

    private CountryFlag() {}

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
        return (
            code != null &&
            code.length() == 2 &&
            code.charAt(0) >= 'A' &&
            code.charAt(0) <= 'Z' &&
            code.charAt(1) >= 'A' &&
            code.charAt(1) <= 'Z'
        );
    }
}
