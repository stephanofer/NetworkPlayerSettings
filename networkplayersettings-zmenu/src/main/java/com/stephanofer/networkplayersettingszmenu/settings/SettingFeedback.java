package com.stephanofer.networkplayersettingszmenu.settings;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SettingFeedback {

    private SettingFeedback() {
    }

    public static void success(final Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7F, 1.25F);
    }

    public static void error(final Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.65F, 1.0F);
    }

    public static void filter(final Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 1.15F);
    }
}
