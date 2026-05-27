package com.stephanofer.networkplayersettings.asset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stephanofer.networkplatform.paper.config.ConfigFileSpec;
import com.stephanofer.networkplatform.paper.config.ConfigService;
import com.stephanofer.networkplatform.paper.config.LoadedConfig;
import com.stephanofer.networkplatform.paper.libs.boostedyaml.YamlDocument;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountryAssetLoaderTest {

    private static final String VALID_BASE64 = "eyJ0ZXh0dXJlcyI6e319";

    @TempDir
    Path tempDir;

    @Test
    void copiesBundledCatalogWhenRuntimeFileIsMissing() throws Exception {
        final CountryAssetLoader loader = new CountryAssetLoader();

        final CountryAssetCatalog catalog = loader.load(configService(validCatalogYaml(), this.tempDir));

        assertNotNull(catalog);
        assertTrue(Files.exists(this.tempDir.resolve("assets/countries.yml")));
        assertEquals("Argentina", catalog.find("AR").displayName());
    }

    @Test
    void rejectsCatalogWithoutFallbackEntry() {
        final CountryAssetLoader loader = new CountryAssetLoader();

        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> loader.load(configService("""
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

        final IllegalStateException aliasCollision = assertThrows(IllegalStateException.class, () -> loader.load(configService("""
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

        final IllegalStateException codeCollision = assertThrows(IllegalStateException.class, () -> loader.load(configService("""
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

        final IllegalStateException blankName = assertThrows(IllegalStateException.class, () -> loader.load(configService("""
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

        final IllegalStateException invalidBase64 = assertThrows(IllegalStateException.class, () -> loader.load(configService("""
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

    private static ConfigService configService(final String yaml, final Path dataFolder) {
        return new StubConfigService(yaml, dataFolder);
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

    private static final class StubConfigService implements ConfigService {

        private final String yaml;
        private final Path dataFolder;

        private StubConfigService(final String yaml, final Path dataFolder) {
            this.yaml = yaml;
            this.dataFolder = dataFolder;
        }

        @Override
        public YamlDocument load(final String path) {
            try {
                final Path file = this.dataFolder.resolve(path);
                Files.createDirectories(file.getParent());
                return YamlDocument.create(file.toFile(), new ByteArrayInputStream(this.yaml.getBytes(UTF_8)));
            } catch (final Exception exception) {
                throw new IllegalStateException("Failed to create test config", exception);
            }
        }

        @Override public YamlDocument load(final ConfigFileSpec spec) { return load(spec.path()); }
        @Override public CompletableFuture<YamlDocument> loadAsync(final String path) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<YamlDocument> loadAsync(final ConfigFileSpec spec) { throw new UnsupportedOperationException(); }
        @Override public Optional<YamlDocument> find(final String path) { throw new UnsupportedOperationException(); }
        @Override public YamlDocument get(final String path) { throw new UnsupportedOperationException(); }
        @Override public boolean isLoaded(final String path) { throw new UnsupportedOperationException(); }
        @Override public Collection<LoadedConfig> loaded() { throw new UnsupportedOperationException(); }
        @Override public void reload(final String path) { throw new UnsupportedOperationException(); }
        @Override public void reloadAll() { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> reloadAsync(final String path) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> reloadAllAsync() { throw new UnsupportedOperationException(); }
        @Override public void save(final String path) { throw new UnsupportedOperationException(); }
        @Override public void saveAll() { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> saveAsync(final String path) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> saveAllAsync() { throw new UnsupportedOperationException(); }
        @Override public boolean update(final String path) { throw new UnsupportedOperationException(); }
        @Override public void updateAll() { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Boolean> updateAsync(final String path) { throw new UnsupportedOperationException(); }
        @Override public CompletableFuture<Void> updateAllAsync() { throw new UnsupportedOperationException(); }
        @Override public void unload(final String path) { throw new UnsupportedOperationException(); }
        @Override public void clear() { throw new UnsupportedOperationException(); }
        @Override public void shutdown() { throw new UnsupportedOperationException(); }
    }
}
