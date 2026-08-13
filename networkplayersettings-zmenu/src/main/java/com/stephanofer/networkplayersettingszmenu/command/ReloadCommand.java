package com.stephanofer.networkplayersettingszmenu.command;

import static net.kyori.adventure.text.Component.text;

import com.stephanofer.networkplayersettingszmenu.NetworkPlayerSettingsZMenuPlugin;
import java.util.Objects;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.Source;

public final class ReloadCommand {

    private final NetworkPlayerSettingsZMenuPlugin plugin;

    public ReloadCommand(final NetworkPlayerSettingsZMenuPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void register(final PaperCommandManager<Source> commandManager) {
        commandManager.command(
            commandManager.commandBuilder(
                    "networkplayersettingszmenu",
                    RichDescription.of(text("Administrative commands for NetworkPlayerSettingsZMenu")),
                    "npszmenu"
                )
                .literal("reload")
                .permission("networkplayersettingszmenu.command.reload")
                .handler(context -> {
                    try {
                        this.plugin.reloadRuntimeResources();
                        context.sender().source().sendRichMessage(
                            "<green>Reloaded messages, menus, and dialogs. Command and cooldown configuration was not changed."
                        );
                    } catch (final RuntimeException exception) {
                        this.plugin.getLogger().log(java.util.logging.Level.SEVERE, "Failed to reload addon runtime resources", exception);
                        context.sender().source().sendRichMessage("<red>Reload failed. Check the console before trying again.");
                    }
                })
        );
    }
}
