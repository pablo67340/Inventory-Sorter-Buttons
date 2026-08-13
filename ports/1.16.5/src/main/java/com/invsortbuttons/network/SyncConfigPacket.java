package com.invsortbuttons.network;

import com.invsortbuttons.sort.PlayerSortConfig;
import com.invsortbuttons.sort.SortEngine;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The client's rules and tree files, sent raw on login and whenever they are
 * edited. The server parses them per player, so everyone can have their own
 * sorting config — just like the original client-side mod.
 */
public final class SyncConfigPacket {
    private static final int MAX_LENGTH = 262144;

    private final String rulesText;
    private final String treeText;

    public SyncConfigPacket(String rulesText, String treeText) {
        this.rulesText = rulesText;
        this.treeText = treeText;
    }

    public static void encode(SyncConfigPacket pkt, PacketBuffer buf) {
        buf.writeUtf(pkt.rulesText, MAX_LENGTH);
        buf.writeUtf(pkt.treeText, MAX_LENGTH);
    }

    public static SyncConfigPacket decode(PacketBuffer buf) {
        return new SyncConfigPacket(buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH));
    }

    public static void handle(final SyncConfigPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player != null) {
                SortEngine.setPlayerConfig(player.getUUID(),
                        PlayerSortConfig.parse(pkt.rulesText, pkt.treeText));
            }
        });
        context.setPacketHandled(true);
    }
}
