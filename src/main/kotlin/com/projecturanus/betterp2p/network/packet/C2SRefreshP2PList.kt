package com.projecturanus.betterp2p.network.packet

import com.projecturanus.betterp2p.network.ModNetwork
import com.projecturanus.betterp2p.network.data.TUNNEL_ANY
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/** Send a request to the server to refresh the p2p list with the given type. */
class C2SRefreshP2PList(var type: Int = TUNNEL_ANY) : IMessage {
  override fun fromBytes(buf: ByteBuf) {
    type = buf.readByte().toInt()
  }

  override fun toBytes(buf: ByteBuf) {
    buf.writeByte(type)
  }
}

/** Client -> C2SRefreshP2P -> Server Handler on server side */
class ServerRefreshP2PListHandler : IMessageHandler<C2SRefreshP2PList, IMessage?> {
  override fun onMessage(message: C2SRefreshP2PList, ctx: MessageContext): S2COpenGui? {
    ModNetwork.requestP2PList(ctx.serverHandler.player, message.type)

    return null
  }
}
