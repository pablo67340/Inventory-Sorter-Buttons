package com.invsortbuttons.network;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Client noticed a hotbar stack ran out (or a tool broke) and asks the server
 * to refill that slot with a matching item from the main inventory.
 */
public final class RefillPacket implements IMessage {
    private int hotbarSlot;
    private String itemId;
    private int meta;

    public RefillPacket() {
    }

    public RefillPacket(int hotbarSlot, String itemId, int meta) {
        this.hotbarSlot = hotbarSlot;
        this.itemId = itemId;
        this.meta = meta;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.hotbarSlot);
        ByteBufUtils.writeUTF8String(buf, this.itemId);
        buf.writeShort(this.meta);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.hotbarSlot = buf.readByte();
        this.itemId = ByteBufUtils.readUTF8String(buf);
        this.meta = buf.readShort();
    }

    public static final class Handler implements IMessageHandler<RefillPacket, IMessage> {
        @Override
        public IMessage onMessage(RefillPacket pkt, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (pkt.hotbarSlot < 0 || pkt.hotbarSlot > 8) {
                return null;
            }
            InventoryPlayer inv = player.inventory;
            if (inv.getStackInSlot(pkt.hotbarSlot) != null) {
                return null;
            }
            Item wanted = (Item) Item.itemRegistry.getObject(pkt.itemId);
            if (wanted == null) {
                return null;
            }
            // Search the main inventory (not the hotbar) for the same item
            for (int i = 9; i <= 35; i++) {
                ItemStack candidate = inv.getStackInSlot(i);
                if (candidate == null || candidate.getItem() != wanted) {
                    continue;
                }
                // Metadata is meaningful pre-flattening (wool colors etc.),
                // but ignore it for damageable tools
                if (wanted.getHasSubtypes() && candidate.getItemDamage() != pkt.meta) {
                    continue;
                }
                inv.setInventorySlotContents(pkt.hotbarSlot, candidate);
                inv.setInventorySlotContents(i, null);
                player.inventoryContainer.detectAndSendChanges();
                return null;
            }
            return null;
        }
    }
}
