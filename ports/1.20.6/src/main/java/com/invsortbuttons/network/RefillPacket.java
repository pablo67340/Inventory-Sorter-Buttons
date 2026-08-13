package com.invsortbuttons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Client noticed a hotbar stack ran out (or a tool broke) and asks the server
 * to refill that slot with a matching item from the main inventory.
 */
public record RefillPacket(int hotbarSlot, String itemId) {
    public static void encode(RefillPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.hotbarSlot);
        buf.writeUtf(pkt.itemId);
    }

    public static RefillPacket decode(FriendlyByteBuf buf) {
        return new RefillPacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(RefillPacket pkt, CustomPayloadEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null || pkt.hotbarSlot < 0 || pkt.hotbarSlot > 8) {
            return;
        }
        Inventory inv = player.getInventory();
        if (!inv.getItem(pkt.hotbarSlot).isEmpty()) {
            return;
        }
        Item wanted = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.tryParse(pkt.itemId));
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
