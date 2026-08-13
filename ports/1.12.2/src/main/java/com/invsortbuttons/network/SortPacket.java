package com.invsortbuttons.network;

import com.invsortbuttons.sort.SortEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client asks the server to sort the open container or the player inventory. */
public final class SortPacket implements IMessage {
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_VERTICAL = 1;
    public static final int MODE_HORIZONTAL = 2;

    private int mode;
    private boolean playerSection;
    private boolean equipArmor;

    public SortPacket() {
    }

    public SortPacket(int mode, boolean playerSection, boolean equipArmor) {
        this.mode = mode;
        this.playerSection = playerSection;
        this.equipArmor = equipArmor;
    }

    public SortPacket(int mode, boolean playerSection) {
        this(mode, playerSection, false);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(this.mode);
        buf.writeBoolean(this.playerSection);
        buf.writeBoolean(this.equipArmor);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.mode = buf.readByte();
        this.playerSection = buf.readBoolean();
        this.equipArmor = buf.readBoolean();
    }

    public static final class Handler implements IMessageHandler<SortPacket, IMessage> {
        @Override
        public IMessage onMessage(final SortPacket pkt, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (pkt.playerSection) {
                    SortEngine.sortPlayerInventory(player, pkt.equipArmor);
                } else {
                    SortEngine.sortOpenContainer(player, pkt.mode);
                }
            });
            return null;
        }
    }
}
