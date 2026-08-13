package com.invsortbuttons;

import com.invsortbuttons.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Inventory Sorter Buttons — a tribute to Inventory Tweaks by Jimeo Wan /
 * Marwane Kalam-Alami (MIT licensed). Recreates the classic s/v/h chest sorting
 * buttons and settings screen for Forge 1.19.2, working on any mod's containers.
 */
@Mod(InvSortButtons.MOD_ID)
public class InvSortButtons {
    public static final String MOD_ID = "invsortbuttons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InvSortButtons() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ISBConfig.SPEC);
        NetworkHandler.init();
    }
}
