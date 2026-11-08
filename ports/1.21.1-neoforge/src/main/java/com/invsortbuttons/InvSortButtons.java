package com.invsortbuttons;

import com.invsortbuttons.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Inventory Sorter Buttons — a tribute to Inventory Tweaks by Jimeo Wan /
 * Marwane Kalam-Alami (MIT licensed). Recreates the classic s/v/h chest sorting
 * buttons and settings screen for NeoForge 1.21.1, working on any mod's containers.
 */
@Mod(InvSortButtons.MOD_ID)
public class InvSortButtons {
    public static final String MOD_ID = "invsortbuttons";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InvSortButtons(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ISBConfig.SPEC);
        modBus.addListener(NetworkHandler::register);
    }
}
