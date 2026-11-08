package com.invsortbuttons.network;

import com.invsortbuttons.InvSortButtons;
import com.invsortbuttons.sort.SortEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks the server to sort the open container or the player inventory. */
public record SortPacket(int mode, boolean playerSection, boolean equipArmor) implements CustomPacketPayload {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_VERTICAL = 1;
    public static final int MODE_HORIZONTAL = 2;

    public static final Type<SortPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InvSortButtons.MOD_ID, "sort"));
    public static final StreamCodec<FriendlyByteBuf, SortPacket> STREAM_CODEC =
            StreamCodec.ofMember(SortPacket::encode, SortPacket::decode);

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

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SortPacket pkt, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player)) {
            return;
        }
        if (pkt.playerSection) {
            SortEngine.sortPlayerInventory(player, pkt.equipArmor);
        } else {
            SortEngine.sortOpenContainer(player, pkt.mode);
        }
    }
}
