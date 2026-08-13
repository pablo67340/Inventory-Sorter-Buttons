package com.invsortbuttons.network;

import com.invsortbuttons.InvSortButtons;
import net.minecraft.resources.Identifier;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class NetworkHandler {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(InvSortButtons.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    private NetworkHandler() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.messageBuilder(SortPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SortPacket::encode)
                .decoder(SortPacket::decode)
                .consumerMainThread(SortPacket::handle)
                .add();
        CHANNEL.messageBuilder(RefillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RefillPacket::encode)
                .decoder(RefillPacket::decode)
                .consumerMainThread(RefillPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncConfigPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncConfigPacket::encode)
                .decoder(SyncConfigPacket::decode)
                .consumerMainThread(SyncConfigPacket::handle)
                .add();
    }

    public static void sendToServer(Object packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }
}
