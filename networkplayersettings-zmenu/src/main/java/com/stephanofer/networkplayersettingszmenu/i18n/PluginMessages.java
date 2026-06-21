package com.stephanofer.networkplayersettingszmenu.i18n;

import com.stephanofer.networkplayersettings.settings.language.Language;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public final class PluginMessages {

    private final Map<Language, ResourceBundle> bundles;
    private final Logger logger;

    public PluginMessages(final Logger logger) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.bundles = Map.of(
            Language.SPANISH, ResourceBundle.getBundle("messages.messages", Locale.forLanguageTag("es")),
            Language.ENGLISH, ResourceBundle.getBundle("messages.messages", Locale.ENGLISH)
        );
    }

    public String get(final Language language, final String key, final Object... args) {
        final ResourceBundle bundle = this.bundles.getOrDefault(Objects.requireNonNull(language, "language"), this.bundles.get(Language.ENGLISH));
        final String pattern = messagePattern(bundle, key);
        return MessageFormat.format(pattern, args);
    }

    private String messagePattern(final ResourceBundle bundle, final String key) {
        try {
            return bundle.getString(key);
        } catch (final MissingResourceException exception) {
            this.logger.warning("Missing NetworkPlayerSettingsZMenu message key: " + key);
            return "<red>Missing message: " + key;
        }
    }
}
