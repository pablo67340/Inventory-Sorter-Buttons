package com.invsortbuttons.network;

import com.invsortbuttons.InvSortButtons;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client noticed a hotbar stack ran out (or a tool broke) and asks the server
 * to refill that slot with a matching item from the main inventory.
 */
public record RefillPacket(int hotbarSlot, String itemId) implements CustomPacketPayload {
    public static final Type<RefillPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(InvSortButtons.MOD_ID, "refill"));
    public static final StreamCodec<FriendlyByteBuf, RefillPacket> STREAM_CODEC =
            StreamCodec.ofMember(RefillPacket::encode, RefillPacket::decode);

    public static void encode(RefillPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.hotbarSlot);
        buf.writeUtf(pkt.itemId);
    }

    public static RefillPacket decode(FriendlyByteBuf buf) {
        return new RefillPacket(buf.readVarInt(), buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RefillPacket pkt, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer player) || pkt.hotbarSlot < 0 || pkt.hotbarSlot > 8) {
            return;
        }
        Inventory inv = player.getInventory();
        if (!inv.getItem(pkt.hotbarSlot).isEmpty()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(pkt.itemId);
        Item wanted = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (wanted == null) {
            return;
        }
        // Search the main inventory (not the hotbar) for the same item
        for (int i = 9; i <= 35; i++) {
            ItemStack candidate = inv.getItem(i);
            if (!candidate.isEmpty() && candidate.is(wanted)) {
                inv.setItem(pkt.hotbarSlot, candidate);
                inv.setItem(i, ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
                return;
            }
        }
    }
}
