package com.invsortbuttons;

import com.invsortbuttons.network.NetworkHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Inventory Sorter Buttons — a tribute to Inventory Tweaks by Jimeo Wan /
 * Marwane Kalam-Alami (MIT licensed). Recreates the classic s/v/h chest sorting
 * buttons and settings screen for Forge 1.7.10, working on any mod's containers.
 */
@Mod(modid = InvSortButtons.MOD_ID, name = InvSortButtons.MOD_NAME, version = InvSortButtons.VERSION,
        acceptedMinecraftVersions = "[1.7.10,1.8)")
public class InvSortButtons {
    public static final String MOD_ID = "invsortbuttons";
    public static final String MOD_NAME = "Inventory Sorter Buttons";
    public static final String VERSION = "1.0.0";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @SidedProxy(clientSide = "com.invsortbuttons.ClientProxy",
            serverSide = "com.invsortbuttons.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ISBConfig.load(event.getSuggestedConfigurationFile());
        NetworkHandler.init();
        FMLCommonHandler.instance().bus().register(new CommonEvents());
        proxy.preInit();
    }
}
