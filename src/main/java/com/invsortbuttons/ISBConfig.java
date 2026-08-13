package com.invsortbuttons;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-side preferences, mirroring the original InvTweaks settings screen. */
public final class ISBConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SHORTCUTS;
    public static final ForgeConfigSpec.BooleanValue MIDDLE_CLICK;
    public static final ForgeConfigSpec.BooleanValue CHEST_BUTTONS;
    public static final ForgeConfigSpec.BooleanValue AUTO_REFILL;
    public static final ForgeConfigSpec.BooleanValue AUTO_EQUIP_ARMOR;
    public static final ForgeConfigSpec.BooleanValue SORT_ON_PICKUP;
    public static final ForgeConfigSpec.BooleanValue SOUNDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("settings");
        SHORTCUTS = b.comment("Enable click shortcuts (Ctrl one item, Ctrl+Shift all of type, Space everything, Alt drop)")
                .define("enableShortcuts", true);
        MIDDLE_CLICK = b.comment("Middle-click a container GUI to sort it")
                .define("enableMiddleClick", true);
        CHEST_BUTTONS = b.comment("Show the s/v/h sorting buttons on container GUIs")
                .define("showChestButtons", true);
        AUTO_REFILL = b.comment("Refill your hotbar slot when a stack runs out or a tool breaks")
                .define("enableAutoRefill", true);
        AUTO_EQUIP_ARMOR = b.comment("Sorting your inventory also equips better armor")
                .define("enableAutoEquipArmor", false);
        SORT_ON_PICKUP = b.comment("Sort your inventory automatically when picking up items")
                .define("enableSortingOnPickup", false);
        SOUNDS = b.comment("Play a click sound when sorting")
                .define("enableSounds", true);
        b.pop();
        SPEC = b.build();
    }

    private ISBConfig() {
    }
}
