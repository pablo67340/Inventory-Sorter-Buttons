package com.invsortbuttons.network;

import com.invsortbuttons.InvSortButtons;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

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
