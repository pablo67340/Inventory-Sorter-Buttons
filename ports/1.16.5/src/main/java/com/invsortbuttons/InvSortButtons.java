package com.invsortbuttons;

import com.invsortbuttons.network.NetworkHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PlayerInventory Sorter Buttons — a tribute to PlayerInventory Tweaks by Jimeo Wan /
 * Marwane Kalam-Alami (MIT licensed). Recreates the classic s/v/h chest sorting
 * buttons and settings screen for Forge 1.16.5, working on any mod's containers.
 */
@Mod(InvSortButtons.MOD_ID)
public class InvSortButtons {
    public static final String MOD_ID = "invsortbuttons";
    public static final Logger LOGGER = LogManager.getLogger();

    public InvSortButtons() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ISBConfig.SPEC);
        NetworkHandler.init();
    }
}
