package com.invsortbuttons.network;

import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * Client noticed a hotbar stack ran out (or a tool broke) and asks the server
 * to refill that slot with a matching item from the main inventory.
 */
public final class RefillPacket {
    private final int hotbarSlot;
    private final String itemId;

    public RefillPacket(int hotbarSlot, String itemId) {
        this.hotbarSlot = hotbarSlot;
        this.itemId = itemId;
    }

    public static void encode(RefillPacket pkt, PacketBuffer buf) {
        buf.writeVarInt(pkt.hotbarSlot);
        buf.writeUtf(pkt.itemId);
    }

    public static RefillPacket decode(PacketBuffer buf) {
        return new RefillPacket(buf.readVarInt(), buf.readUtf());
    }

    public static void handle(final RefillPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        final NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayerEntity player = context.getSender();
            if (player == null || pkt.hotbarSlot < 0 || pkt.hotbarSlot > 8) {
                return;
            }
            PlayerInventory inv = player.inventory;
            if (!inv.getItem(pkt.hotbarSlot).isEmpty()) {
                return;
            }
            Item wanted = ForgeRegistries.ITEMS.getValue(net.minecraft.util.ResourceLocation.tryParse(pkt.itemId));
            if (wanted == null) {
                return;
            }
            // Search the main inventory (not the hotbar) for the same item
            for (int i = 9; i <= 35; i++) {
                ItemStack candidate = inv.getItem(i);
                if (!candidate.isEmpty() && candidate.getItem() == wanted) {
                    inv.setItem(pkt.hotbarSlot, candidate);
                    inv.setItem(i, ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                    return;
                }
            }
        });
        context.setPacketHandled(true);
    }
}
