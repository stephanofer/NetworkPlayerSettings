package com.stephanofer.networkplayersettingszmenu.i18n;

import com.stephanofer.networkplayersettings.settings.language.Language;
import dev.dejvokep.boostedyaml.YamlDocument;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public final class PluginMessages {

    private volatile Map<Language, YamlDocument> documents;
    private final Logger logger;

    public PluginMessages(final Logger logger, final YamlDocument spanish, final YamlDocument english) {
        this.logger = Objects.requireNonNull(logger, "logger");
        replaceDocuments(spanish, english);
    }

    public void replaceDocuments(final YamlDocument spanish, final YamlDocument english) {
        this.documents = Map.of(
            Language.SPANISH, Objects.requireNonNull(spanish, "spanish"),
            Language.ENGLISH, Objects.requireNonNull(english, "english")
        );
    }

    public String get(final Language language, final String key, final Object... args) {
        final Map<Language, YamlDocument> documents = this.documents;
        final YamlDocument document = documents.getOrDefault(
            Objects.requireNonNull(language, "language"),
            documents.get(Language.ENGLISH)
        );
        final String pattern = messagePattern(documents, document, key);
        return MessageFormat.format(pattern, args);
    }

    private String messagePattern(final Map<Language, YamlDocument> documents, final YamlDocument document, final String key) {
        final String direct = document.getString(key, null);
        if (direct != null) {
            return direct;
        }

        final YamlDocument english = documents.get(Language.ENGLISH);
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
