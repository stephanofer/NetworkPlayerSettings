package com.stephanofer.networkplayersettings.asset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryAssetLoaderTest {

    private static final String VALID_BASE64 = "eyJ0ZXh0dXJlcyI6e319";

    @TempDir
    Path tempDir;

    @Test
    void copiesBundledCatalogWhenRuntimeFileIsMissing() throws Exception {
        final CountryAssetLoader loader = new CountryAssetLoader(resource(validCatalogYaml()));

        final CountryAssetCatalog catalog = loader.load(this.tempDir);

        assertNotNull(catalog);
        assertTrue(Files.exists(this.tempDir.resolve("assets/countries.yml")));
        assertEquals("Argentina", catalog.find("AR").displayName());
    }

    @Test
    void rejectsCatalogWithoutFallbackEntry() {
        final CountryAssetLoader loader = new CountryAssetLoader(resource("""
            countries:
              AR:
                name: Argentina
                head-texture-base64: \"%s\"
                aliases: [argentina]
            """.formatted(VALID_BASE64)));

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> loader.load(this.tempDir));

        assertTrue(error.getMessage().contains("XX"));
    }

    @Test
    void rejectsAliasCollisionsAndCodeCollisionsAfterNormalization() {
        final CountryAssetLoader aliasCollisionLoader = new CountryAssetLoader(resource("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: \"%s\"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: \"%s\"
                aliases: [shared]
              AM:
                name: Armenia
                head-texture-base64: \"%s\"
                aliases: [shared]
            """.formatted(VALID_BASE64, VALID_BASE64, VALID_BASE64)));

        final IllegalStateException aliasCollision = assertThrows(IllegalStateException.class, () -> aliasCollisionLoader.load(this.tempDir));
        assertTrue(aliasCollision.getMessage().contains("shared"));

        final CountryAssetLoader codeCollisionLoader = new CountryAssetLoader(resource("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: \"%s\"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: \"%s\"
                aliases: [argentina]
              ar:
                name: Duplicate Argentina
                head-texture-base64: \"%s\"
                aliases: [duplicate]
            """.formatted(VALID_BASE64, VALID_BASE64, VALID_BASE64)));

        final IllegalStateException codeCollision = assertThrows(IllegalStateException.class, () -> codeCollisionLoader.load(this.tempDir.resolve("second")));
        assertTrue(codeCollision.getMessage().contains("AR"));
    }

    @Test
    void rejectsBlankNamesAndInvalidBase64() {
        final CountryAssetLoader blankNameLoader = new CountryAssetLoader(resource("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: \"%s\"
                aliases: [unknown]
              AR:
                name: \"   \"
                head-texture-base64: \"%s\"
                aliases: [argentina]
            """.formatted(VALID_BASE64, VALID_BASE64)));

        final IllegalStateException blankName = assertThrows(IllegalStateException.class, () -> blankNameLoader.load(this.tempDir));
        assertTrue(blankName.getMessage().contains("name"));

        final CountryAssetLoader invalidBase64Loader = new CountryAssetLoader(resource("""
            countries:
              XX:
                name: Unknown
                head-texture-base64: \"%s\"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: \"not-base64\"
                aliases: [argentina]
            """.formatted(VALID_BASE64)));

        final IllegalStateException invalidBase64 = assertThrows(IllegalStateException.class, () -> invalidBase64Loader.load(this.tempDir.resolve("third")));
        assertTrue(invalidBase64.getMessage().contains("base64"));
    }

    private static Supplier<InputStream> resource(final String yaml) {
        return () -> new ByteArrayInputStream(yaml.getBytes(UTF_8));
    }

    private static String validCatalogYaml() {
        return """
            countries:
              XX:
                name: Unknown
                head-texture-base64: \"%s\"
                aliases: [unknown]
              AR:
                name: Argentina
                head-texture-base64: \"%s\"
                aliases: [argentina, south-america]
            """.formatted(VALID_BASE64, VALID_BASE64);
    }
}
