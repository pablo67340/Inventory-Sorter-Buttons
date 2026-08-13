package com.invsortbuttons;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

/** Client-side preferences, mirroring the original InvTweaks settings screen. */
public final class ISBConfig {
    public static boolean enableShortcuts = true;
    public static boolean enableMiddleClick = true;
    public static boolean showChestButtons = true;
    public static boolean enableAutoRefill = true;
    public static boolean enableAutoEquipArmor = false;
    public static boolean enableSortingOnPickup = false;
    public static boolean enableSounds = true;

    private static Configuration config;

    private ISBConfig() {
    }

    public static void load(File file) {
        config = new Configuration(file);
        read();
        config.save();
    }

    private static void read() {
        enableShortcuts = config.getBoolean("enableShortcuts", "settings", true,
                "Enable click shortcuts (Ctrl one item, Ctrl+Shift all of type, Space everything, Alt drop)");
        enableMiddleClick = config.getBoolean("enableMiddleClick", "settings", true,
                "Middle-click a container GUI to sort it");
        showChestButtons = config.getBoolean("showChestButtons", "settings", true,
                "Show the s/v/h sorting buttons on container GUIs");
        enableAutoRefill = config.getBoolean("enableAutoRefill", "settings", true,
                "Refill your hotbar slot when a stack runs out or a tool breaks");
        enableAutoEquipArmor = config.getBoolean("enableAutoEquipArmor", "settings", false,
                "Sorting your inventory also equips better armor");
        enableSortingOnPickup = config.getBoolean("enableSortingOnPickup", "settings", false,
                "Sort your inventory automatically when picking up items");
        enableSounds = config.getBoolean("enableSounds", "settings", true,
                "Play a click sound when sorting");
    }

    public static void save() {
        if (config == null) {
            return;
        }
        config.get("settings", "enableShortcuts", true).set(enableShortcuts);
        config.get("settings", "enableMiddleClick", true).set(enableMiddleClick);
        config.get("settings", "showChestButtons", true).set(showChestButtons);
        config.get("settings", "enableAutoRefill", true).set(enableAutoRefill);
        config.get("settings", "enableAutoEquipArmor", false).set(enableAutoEquipArmor);
        config.get("settings", "enableSortingOnPickup", false).set(enableSortingOnPickup);
        config.get("settings", "enableSounds", true).set(enableSounds);
        config.save();
    }
}
