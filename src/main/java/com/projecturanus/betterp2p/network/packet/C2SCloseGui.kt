package com.projecturanus.betterp2p.network.packet

import com.projecturanus.betterp2p.network.ModNetwork
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class C2SCloseGui : IMessage {
    override fun fromBytes(buf: ByteBuf) {

    }

    override fun toBytes(buf: ByteBuf) {
    }
}

class ServerCloseGuiHandler : IMessageHandler<C2SCloseGui, IMessage?> {
    override fun onMessage(message: C2SCloseGui, ctx: MessageContext): IMessage? {
        ModNetwork.playerState.remove(ctx.serverHandler.player.uniqueID)

        return null
    }
}
