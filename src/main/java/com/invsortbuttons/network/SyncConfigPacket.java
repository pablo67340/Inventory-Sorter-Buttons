package com.invsortbuttons.network;

import com.invsortbuttons.sort.PlayerSortConfig;
import com.invsortbuttons.sort.SortEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The client's rules and tree files, sent raw on login and whenever they are
 * edited. The server parses them per player, so everyone can have their own
 * sorting config — just like the original client-side mod.
 */
public record SyncConfigPacket(String rulesText, String treeText) {
    private static final int MAX_LENGTH = 262144;

    public static void encode(SyncConfigPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.rulesText, MAX_LENGTH);
        buf.writeUtf(pkt.treeText, MAX_LENGTH);
    }

    public static SyncConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncConfigPacket(buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH));
    }

    public static void handle(SyncConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                SortEngine.setPlayerConfig(player.getUUID(),
                        PlayerSortConfig.parse(pkt.rulesText, pkt.treeText));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
