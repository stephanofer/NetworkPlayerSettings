package com.stephanofer.networkplayersettingszmenu.i18n;

import com.stephanofer.networkplayersettings.api.Language;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public final class PluginMessages {

    private final Map<Language, ResourceBundle> bundles;

    public PluginMessages() {
        this.bundles = Map.of(
            Language.SPANISH, ResourceBundle.getBundle("messages.messages", Locale.forLanguageTag("es")),
            Language.ENGLISH, ResourceBundle.getBundle("messages.messages", Locale.ENGLISH)
        );
    }

    public String get(final Language language, final String key, final Object... args) {
        final ResourceBundle bundle = this.bundles.getOrDefault(Objects.requireNonNull(language, "language"), this.bundles.get(Language.ENGLISH));
        final String pattern = bundle.getString(key);
        return MessageFormat.format(pattern, args);
    }
}
