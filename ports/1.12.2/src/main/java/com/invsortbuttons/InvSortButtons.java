package com.invsortbuttons;

import com.invsortbuttons.network.NetworkHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Inventory Sorter Buttons — a tribute to Inventory Tweaks by Jimeo Wan /
 * Marwane Kalam-Alami (MIT licensed). Recreates the classic s/v/h chest sorting
 * buttons and settings screen for Forge 1.12.2, working on any mod's containers.
 */
@Mod(modid = InvSortButtons.MOD_ID, name = InvSortButtons.MOD_NAME, version = InvSortButtons.VERSION,
        acceptedMinecraftVersions = "[1.12,1.13)")
public class InvSortButtons {
    public static final String MOD_ID = "invsortbuttons";
    public static final String MOD_NAME = "Inventory Sorter Buttons";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        NetworkHandler.init();
    }
}
