package com.invsortbuttons.network;

import com.invsortbuttons.sort.PlayerSortConfig;
import com.invsortbuttons.sort.SortEngine;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * The client's rules and tree files, sent raw on login and whenever they are
 * edited. The server parses them per player, so everyone can have their own
 * sorting config — just like the original client-side mod.
 */
public final class SyncConfigPacket implements IMessage {
    private String rulesText;
    private String treeText;

    public SyncConfigPacket() {
    }

    public SyncConfigPacket(String rulesText, String treeText) {
        this.rulesText = rulesText;
        this.treeText = treeText;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.rulesText);
        ByteBufUtils.writeUTF8String(buf, this.treeText);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.rulesText = ByteBufUtils.readUTF8String(buf);
        this.treeText = ByteBufUtils.readUTF8String(buf);
    }

    public static final class Handler implements IMessageHandler<SyncConfigPacket, IMessage> {
        @Override
        public IMessage onMessage(final SyncConfigPacket pkt, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() ->
                    SortEngine.setPlayerConfig(player.getUniqueID(),
                            PlayerSortConfig.parse(pkt.rulesText, pkt.treeText)));
            return null;
        }
    }
}
