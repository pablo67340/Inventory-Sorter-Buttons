package com.invsortbuttons.network;

import com.invsortbuttons.InvSortButtons;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class NetworkHandler {
    public static final SimpleNetworkWrapper CHANNEL =
            NetworkRegistry.INSTANCE.newSimpleChannel(InvSortButtons.MOD_ID);

    private NetworkHandler() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(SortPacket.Handler.class, SortPacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(RefillPacket.Handler.class, RefillPacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(SyncConfigPacket.Handler.class, SyncConfigPacket.class, id++, Side.SERVER);
    }
}
