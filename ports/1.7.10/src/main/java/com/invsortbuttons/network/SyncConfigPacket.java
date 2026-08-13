package com.invsortbuttons.network;

import com.invsortbuttons.sort.PlayerSortConfig;
import com.invsortbuttons.sort.SortEngine;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

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
        public IMessage onMessage(SyncConfigPacket pkt, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            SortEngine.setPlayerConfig(player.getUniqueID(),
                    PlayerSortConfig.parse(pkt.rulesText, pkt.treeText));
            return null;
        }
    }
}
