package com.invsortbuttons;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;

/** Client-side preferences, mirroring the original InvTweaks settings screen. */
@Config(modid = InvSortButtons.MOD_ID, category = "settings")
public final class ISBConfig {
    @Config.Comment("Enable click shortcuts (Ctrl one item, Ctrl+Shift all of type, Space everything, Alt drop)")
    public static boolean enableShortcuts = true;

    @Config.Comment("Middle-click a container GUI to sort it")
    public static boolean enableMiddleClick = true;

    @Config.Comment("Show the s/v/h sorting buttons on container GUIs")
    public static boolean showChestButtons = true;

    @Config.Comment("Refill your hotbar slot when a stack runs out or a tool breaks")
    public static boolean enableAutoRefill = true;

    @Config.Comment("Sorting your inventory also equips better armor")
    public static boolean enableAutoEquipArmor = false;

    @Config.Comment("Sort your inventory automatically when picking up items")
    public static boolean enableSortingOnPickup = false;

    @Config.Comment("Play a click sound when sorting")
    public static boolean enableSounds = true;

    public static void save() {
        ConfigManager.sync(InvSortButtons.MOD_ID, Config.Type.INSTANCE);
    }
}
