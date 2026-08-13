package com.invsortbuttons;

import com.invsortbuttons.client.ClientEvents;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

/** Client proxy: key binding + GUI/tick event handlers. */
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        ClientRegistry.registerKeyBinding(ClientEvents.SORT_KEY);
        ClientEvents events = new ClientEvents();
        MinecraftForge.EVENT_BUS.register(events);     // GuiScreenEvent...
        FMLCommonHandler.instance().bus().register(events); // TickEvent...
    }
}
