package com.stephanofer.networkplayersettings.menu;

import com.stephanofer.networkplayersettings.NetworkPlayerSettingsPlugin;
import com.stephanofer.networkplayersettings.yaml.PluginYamlLoader;
import fr.maxlego08.menu.api.BedrockManager;
import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.DialogManager;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.exceptions.DialogException;
import fr.maxlego08.menu.api.exceptions.InventoryException;
import fr.maxlego08.menu.api.pattern.PatternManager;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.Comparator;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class ZMenuBatchLoader {

    private static final String INVENTORIES_DIRECTORY = "inventories";
    private static final String DIALOGS_DIRECTORY = "dialogs";
    private static final String PATTERNS_DIRECTORY = "patterns";
    private static final String ACTION_PATTERNS_DIRECTORY = "actions_patterns";
    private static final String BEDROCK_DIRECTORY = "bedrock";

    private final NetworkPlayerSettingsPlugin plugin;
    private final PluginYamlLoader yamlLoader;
    private final ButtonManager buttonManager;
    private final InventoryManager inventoryManager;
    private final DialogManager dialogManager;
    private final PatternManager patternManager;
    private final BedrockManager bedrockManager;
    private final Logger logger;

    public ZMenuBatchLoader(
        final NetworkPlayerSettingsPlugin plugin,
        final PluginYamlLoader yamlLoader,
        final ButtonManager buttonManager,
        final InventoryManager inventoryManager,
        final DialogManager dialogManager,
        final PatternManager patternManager,
        final BedrockManager bedrockManager,
        final Logger logger
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.yamlLoader = Objects.requireNonNull(yamlLoader, "yamlLoader");
        this.buttonManager = Objects.requireNonNull(buttonManager, "buttonManager");
        this.inventoryManager = Objects.requireNonNull(inventoryManager, "inventoryManager");
        this.dialogManager = Objects.requireNonNull(dialogManager, "dialogManager");
        this.patternManager = Objects.requireNonNull(patternManager, "patternManager");
        this.bedrockManager = bedrockManager;
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void initialize() throws InventoryException, DialogException {
        ensureDataDirectories();
        this.buttonManager.register(new LanguageButtonLoader(this.plugin));
        syncBundledDirectory(INVENTORIES_DIRECTORY);
        syncBundledDirectory(DIALOGS_DIRECTORY);
        syncBundledDirectory(PATTERNS_DIRECTORY);
        syncBundledDirectory(ACTION_PATTERNS_DIRECTORY);
        if (this.bedrockManager != null) {
            syncBundledDirectory(BEDROCK_DIRECTORY);
        }
        loadPatternFiles();
        loadActionPatternFiles();
        loadInventoryFiles();
        loadDialogFiles();
        if (this.bedrockManager != null) {
            loadBedrockFiles();
        }
    }

    private void ensureDataDirectories() {
        try {
            Files.createDirectories(this.plugin.getDataFolder().toPath().resolve(INVENTORIES_DIRECTORY));
            Files.createDirectories(this.plugin.getDataFolder().toPath().resolve(DIALOGS_DIRECTORY));
            Files.createDirectories(this.plugin.getDataFolder().toPath().resolve(PATTERNS_DIRECTORY));
            Files.createDirectories(this.plugin.getDataFolder().toPath().resolve(ACTION_PATTERNS_DIRECTORY));
            if (this.bedrockManager != null) {
                Files.createDirectories(this.plugin.getDataFolder().toPath().resolve(BEDROCK_DIRECTORY));
            }
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to create zMenu data directories.", exception);
        }
    }

    private void loadPatternFiles() throws InventoryException {
        for (final File file : listYamlFiles(PATTERNS_DIRECTORY)) {
            this.patternManager.loadPattern(file);
        }
    }

    private void loadActionPatternFiles() throws InventoryException {
        for (final File file : listYamlFiles(ACTION_PATTERNS_DIRECTORY)) {
            this.patternManager.loadActionPattern(file);
        }
    }

    private void loadInventoryFiles() throws InventoryException {
        for (final File file : listYamlFiles(INVENTORIES_DIRECTORY)) {
            this.inventoryManager.loadInventory(this.plugin, file);
        }
    }

    private void loadDialogFiles() throws DialogException, InventoryException {
        for (final File file : listYamlFiles(DIALOGS_DIRECTORY)) {
            this.dialogManager.loadInventory(this.plugin, file);
        }
    }

    private void loadBedrockFiles() throws DialogException, InventoryException {
        for (final File file : listYamlFiles(BEDROCK_DIRECTORY)) {
            this.bedrockManager.loadInventory(this.plugin, file);
        }
    }

    private void syncBundledDirectory(final String directory) {
        for (final String relativePath : listBundledYamlResources(directory)) {
            this.yamlLoader.load(relativePath);
        }
    }

    private File[] listYamlFiles(final String directory) {
        final Path root = this.plugin.getDataFolder().toPath().resolve(directory);
        if (!Files.isDirectory(root)) {
            return new File[0];
        }

        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".yml"))
                .sorted(Comparator.naturalOrder())
                .map(Path::toFile)
                .toArray(File[]::new);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to scan zMenu directory: " + directory, exception);
        }
    }

    private String[] listBundledYamlResources(final String directory) {
        final CodeSource codeSource = this.plugin.getClass().getProtectionDomain().getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            this.logger.warning("No se pudo inspeccionar el jar del plugin para descubrir resources de zMenu en " + directory + '.');
            return new String[0];
        }

        try {
            final File source = new File(codeSource.getLocation().toURI());
            if (!source.isFile()) {
                return new String[0];
            }

            try (JarFile jarFile = new JarFile(source)) {
                return jarFile.stream()
                    .map(JarEntry::getName)
                    .filter(name -> name.startsWith(directory + "/"))
                    .filter(name -> name.endsWith(".yml"))
                    .sorted()
                    .toArray(String[]::new);
            }
        } catch (final IOException | URISyntaxException exception) {
            this.logger.log(Level.WARNING, "No se pudieron descubrir resources bundled para zMenu en " + directory + '.', exception);
            return new String[0];
        }
    }
}
