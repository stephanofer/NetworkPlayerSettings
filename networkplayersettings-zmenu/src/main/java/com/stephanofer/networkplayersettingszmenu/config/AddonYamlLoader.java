package com.stephanofer.networkplayersettingszmenu.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class AddonYamlLoader {

    private static final String VERSION_ROUTE = "file-version";

    private final JavaPlugin plugin;

    public AddonYamlLoader(final JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public YamlDocument load(final String relativePath) {
        Objects.requireNonNull(relativePath, "relativePath");

        try (InputStream defaults = openDefaults(relativePath)) {
            return YamlDocument.create(
                this.plugin.getDataFolder().toPath().resolve(relativePath).toFile(),
                defaults,
                GeneralSettings.DEFAULT,
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setVersioning(new BasicVersioning(VERSION_ROUTE)).build()
            );
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to load YAML document: " + relativePath, exception);
        }
    }

    private InputStream openDefaults(final String relativePath) {
        final InputStream defaults = this.plugin.getResource(relativePath);
        if (defaults == null) {
            throw new IllegalStateException("Missing bundled resource: " + relativePath);
        }
        return defaults;
    }
}
