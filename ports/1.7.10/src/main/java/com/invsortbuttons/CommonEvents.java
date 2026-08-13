package com.invsortbuttons;

import com.invsortbuttons.sort.SortEngine;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

/** Registered on the FML bus in preInit. */
public final class CommonEvents {
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SortEngine.clearPlayerConfig(event.player.getUniqueID());
    }
}
