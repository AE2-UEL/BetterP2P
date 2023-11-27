package com.projecturanus.betterp2p.network.packet

import com.projecturanus.betterp2p.item.ItemAdvancedMemoryCard
import com.projecturanus.betterp2p.network.data.MemoryInfo
import com.projecturanus.betterp2p.network.data.readMemoryInfo
import com.projecturanus.betterp2p.network.data.writeMemoryInfo
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class C2SUpdateMemoryInfo(var info: MemoryInfo = MemoryInfo()) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        info = readMemoryInfo(buf)
    }

    override fun toBytes(buf: ByteBuf) {
        writeMemoryInfo(buf, info)
    }
}

class ServerUpdateInfoHandler : IMessageHandler<C2SUpdateMemoryInfo, IMessage?> {
    override fun onMessage(message: C2SUpdateMemoryInfo, ctx: MessageContext): IMessage? {
        val player = ctx.serverHandler.player
        val stack = player.getHeldItem(player.activeHand)

        if (stack.item is ItemAdvancedMemoryCard) {
            ItemAdvancedMemoryCard.writeInfo(stack, message.info)
        }
        return null
    }
}
