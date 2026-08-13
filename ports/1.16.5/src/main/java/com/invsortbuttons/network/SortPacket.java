package com.invsortbuttons.network;

import com.invsortbuttons.sort.SortEngine;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Client asks the server to sort the open container or the player inventory. */
public final class SortPacket {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_VERTICAL = 1;
    public static final int MODE_HORIZONTAL = 2;

    private final int mode;
    private final boolean playerSection;
    private final boolean equipArmor;

    public SortPacket(int mode, boolean playerSection, boolean equipArmor) {
        this.mode = mode;
        this.playerSection = playerSection;
        this.equipArmor = equipArmor;
    }

    public SortPacket(int mode, boolean playerSection) {
        this(mode, playerSection, false);
    }

    public static void encode(SortPacket pkt, PacketBuffer buf) {
        buf.writeVarInt(pkt.mode);
        buf.writeBoolean(pkt.playerSection);
        buf.writeBoolean(pkt.equipArmor);
    }

    public static SortPacket decode(PacketBuffer buf) {
        return new SortPacket(buf.readVarInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(final SortPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null) {
                return;
            }
            if (pkt.playerSection) {
                SortEngine.sortPlayerInventory(player, pkt.equipArmor);
            } else {
                SortEngine.sortOpenContainer(player, pkt.mode);
            }
        });
        context.setPacketHandled(true);
    }
}
