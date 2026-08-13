package com.invsortbuttons.network;

import com.invsortbuttons.InvSortButtons;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(InvSortButtons.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private NetworkHandler() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, SortPacket.class,
                SortPacket::encode, SortPacket::decode, SortPacket::handle);
        CHANNEL.registerMessage(id++, RefillPacket.class,
                RefillPacket::encode, RefillPacket::decode, RefillPacket::handle);
        CHANNEL.registerMessage(id++, SyncConfigPacket.class,
                SyncConfigPacket::encode, SyncConfigPacket::decode, SyncConfigPacket::handle);
    }
}
