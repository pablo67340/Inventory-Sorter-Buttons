package com.invsortbuttons.network;

import com.invsortbuttons.InvSortButtons;
import com.invsortbuttons.sort.PlayerSortConfig;
import com.invsortbuttons.sort.SortEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The client's rules and tree files, sent raw on login and whenever they are
 * edited. The server parses them per player, so everyone can have their own
 * sorting config — just like the original client-side mod.
 */
public record SyncConfigPacket(String rulesText, String treeText) implements CustomPacketPayload {
    private static final int MAX_LENGTH = 262144;

    public static final Type<SyncConfigPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InvSortButtons.MOD_ID, "sync_config"));
    public static final StreamCodec<FriendlyByteBuf, SyncConfigPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncConfigPacket::encode, SyncConfigPacket::decode);

    public static void encode(SyncConfigPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.rulesText, MAX_LENGTH);
        buf.writeUtf(pkt.treeText, MAX_LENGTH);
    }

    public static SyncConfigPacket decode(FriendlyByteBuf buf) {
        return new SyncConfigPacket(buf.readUtf(MAX_LENGTH), buf.readUtf(MAX_LENGTH));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncConfigPacket pkt, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer player) {
            SortEngine.setPlayerConfig(player.getUUID(),
                    PlayerSortConfig.parse(pkt.rulesText, pkt.treeText));
        }
    }
}
