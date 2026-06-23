package com.stephanofer.networkplayersettingszmenu.i18n;

import com.stephanofer.networkplayersettings.settings.language.Language;
import dev.dejvokep.boostedyaml.YamlDocument;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class PluginMessages {

    private final Map<Language, YamlDocument> documents;
    private final Logger logger;

    public PluginMessages(final Logger logger, final YamlDocument spanish, final YamlDocument english) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.documents = Map.of(
            Language.SPANISH, Objects.requireNonNull(spanish, "spanish"),
            Language.ENGLISH, Objects.requireNonNull(english, "english")
        );
    }

    public String get(final Language language, final String key, final Object... args) {
        final YamlDocument document = this.documents.getOrDefault(
            Objects.requireNonNull(language, "language"),
            this.documents.get(Language.ENGLISH)
        );
        final String pattern = messagePattern(document, key);
        return MessageFormat.format(pattern, args);
    }

    private String messagePattern(final YamlDocument document, final String key) {
        final String direct = document.getString(key, null);
        if (direct != null) {
            return direct;
        }

        final YamlDocument english = this.documents.get(Language.ENGLISH);
        if (english != null && english != document) {
            final String fallback = english.getString(key, null);
            if (fallback != null) {
                return fallback;
            }
        }

        this.logger.warning("Missing NetworkPlayerSettingsZMenu message key: " + key);
        return "<red>Missing message: " + key;
    }
}
