package com.stephanofer.networkplayersettingszmenu.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.dejvokep.boostedyaml.YamlDocument;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ZMenuPluginConfigTest {

    private static final Logger LOGGER = Logger.getLogger(ZMenuPluginConfigTest.class.getName());

    @TempDir
    Path tempDir;

    @Test
    void defaultsMissingOpenTargetKeyToSettingsMain() {
        final ZMenuPluginConfig config = ZMenuPluginConfig.fromDocument(document("""
            command:
              name: settings
              aliases: [prefs]
              open:
                type: menu
            settings:
              mutation-cooldown-millis: 750
            """), LOGGER);

        assertEquals("settings-main", config.command().openTargetKey());
    }

    @Test
    void fallsBackInvalidOpenTargetKeyToSettingsMain() {
        final ZMenuPluginConfig config = ZMenuPluginConfig.fromDocument(document("""
            command:
              name: settings
              aliases: [prefs]
              open:
                type: menu
                key: "bad key with spaces"
            settings:
              mutation-cooldown-millis: 750
            """), LOGGER);

        assertEquals("settings-main", config.command().openTargetKey());
    }

    private YamlDocument document(final String yaml) {
        try {
            final Path file = this.tempDir.resolve("config.yml");
            Files.createDirectories(file.getParent());
            return YamlDocument.create(file.toFile(), new ByteArrayInputStream(yaml.getBytes(UTF_8)));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to create test config", exception);
        }
    }
}
