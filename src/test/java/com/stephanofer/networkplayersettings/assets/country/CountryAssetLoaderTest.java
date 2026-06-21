package com.stephanofer.networkplayersettings.assets.country;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.dejvokep.boostedyaml.YamlDocument;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryAssetLoaderTest {

    private static final String VALID_BASE64 = "eyJ0ZXh0dXJlcyI6e319";

    @TempDir
    Path tempDir;

    @Test
    void copiesBundledCatalogWhenRuntimeFileIsMissing() throws Exception {
        final CountryAssetLoader loader = new CountryAssetLoader();

        final CountryAssetCatalog catalog = loader.load(document(validCatalogYaml(), this.tempDir));

        assertNotNull(catalog);
        assertTrue(Files.exists(this.tempDir.resolve("assets/countries.yml")));
        assertEquals("Argentina", catalog.find("AR").displayName());
    }

    @Test
    void rejectsCatalogWithoutFallbackEntry() {
        final CountryAssetLoader loader = new CountryAssetLoader();

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> loader.load(document("""
            countries:
              AR:
                name: Argentina
                head-texture-base64: "%s"
                aliases: [argentina]
            """.formatted(VALID_BASE64), this.tempDir)));

        assertTrue(error.getMessage().contains("XX"));
    }

    @Test
    void rejectsAliasCollisionsAndCodeCollisionsAfterNormalization() {
        final CountryAssetLoader loader = new CountryAssetLoader();

        final IllegalStateException aliasCollision = assertThrows(IllegalStateException.class, () -> loader.load(document("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: "%s"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: "%s"
                aliases: [shared]
              AM:
                name: Armenia
                head-texture-base64: "%s"
                aliases: [shared]
            """.formatted(VALID_BASE64, VALID_BASE64, VALID_BASE64), this.tempDir)));
        assertTrue(aliasCollision.getMessage().contains("shared"));

        final IllegalStateException codeCollision = assertThrows(IllegalStateException.class, () -> loader.load(document("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: "%s"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: "%s"
                aliases: [argentina]
              ar:
                name: Duplicate Argentina
                head-texture-base64: "%s"
                aliases: [duplicate]
            """.formatted(VALID_BASE64, VALID_BASE64, VALID_BASE64), this.tempDir.resolve("second"))));
        assertTrue(codeCollision.getMessage().contains("AR"));
    }

    @Test
    void rejectsBlankNamesAndInvalidBase64() {
        final CountryAssetLoader loader = new CountryAssetLoader();

        final IllegalStateException blankName = assertThrows(IllegalStateException.class, () -> loader.load(document("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: "%s"
                aliases: [unknown]
              AR:
                name: "   "
                head-texture-base64: "%s"
                aliases: [argentina]
            """.formatted(VALID_BASE64, VALID_BASE64), this.tempDir)));
        assertTrue(blankName.getMessage().contains("name"));

        final IllegalStateException invalidBase64 = assertThrows(IllegalStateException.class, () -> loader.load(document("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: "%s"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: "not-base64"
                aliases: [argentina]
            """.formatted(VALID_BASE64), this.tempDir.resolve("third"))));
        assertTrue(invalidBase64.getMessage().contains("base64"));
    }

    private static String validCatalogYaml() {
        return """
            countries:
              XX:
                name: Unknown
                head-texture-base64: "%s"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: "%s"
                aliases: [argentina, south-america]
            """.formatted(VALID_BASE64, VALID_BASE64);
    }

    private static YamlDocument document(final String yaml, final Path dataFolder) {
        try {
            final Path file = dataFolder.resolve("assets/countries.yml");
            Files.createDirectories(file.getParent());
            return YamlDocument.create(file.toFile(), new ByteArrayInputStream(yaml.getBytes(UTF_8)));
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to create test config", exception);
        }
    }
}
