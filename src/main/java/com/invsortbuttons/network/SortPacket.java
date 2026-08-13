package com.invsortbuttons.network;

import com.invsortbuttons.sort.SortEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client asks the server to sort the open container or the player inventory. */
public record SortPacket(int mode, boolean playerSection, boolean equipArmor) {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_VERTICAL = 1;
    public static final int MODE_HORIZONTAL = 2;

    public SortPacket(int mode, boolean playerSection) {
        this(mode, playerSection, false);
    }

    public static void encode(SortPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.mode);
        buf.writeBoolean(pkt.playerSection);
        buf.writeBoolean(pkt.equipArmor);
    }

    public static SortPacket decode(FriendlyByteBuf buf) {
        return new SortPacket(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(SortPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (pkt.playerSection) {
                SortEngine.sortPlayerInventory(player, pkt.equipArmor);
            } else {
                SortEngine.sortOpenContainer(player, pkt.mode);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
