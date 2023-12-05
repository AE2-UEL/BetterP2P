package com.projecturanus.betterp2p.network.packet

import com.projecturanus.betterp2p.BetterP2P
import com.projecturanus.betterp2p.network.ModNetwork
import com.projecturanus.betterp2p.network.data.P2PLocation
import com.projecturanus.betterp2p.network.data.TUNNEL_ANY
import com.projecturanus.betterp2p.network.data.readP2PLocation
import com.projecturanus.betterp2p.network.data.writeP2PLocation
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class C2STypeChange(var newType: Int = TUNNEL_ANY, var p2p: P2PLocation? = null) : IMessage {
  override fun fromBytes(buf: ByteBuf) {
    newType = buf.readByte().toInt()
    p2p = readP2PLocation(buf)
  }

  override fun toBytes(buf: ByteBuf) {
    buf.writeByte(newType)
    writeP2PLocation(buf, p2p!!)
  }
}

class ServerTypeChangeHandler : IMessageHandler<C2STypeChange, S2COpenGui?> {
  override fun onMessage(message: C2STypeChange, ctx: MessageContext): S2COpenGui? {
    // validate
    if (message.p2p == null) {
      return null
    }

    val state = ModNetwork.playerState[ctx.serverHandler.player.uniqueID] ?: return null
    val type = BetterP2P.proxy.getP2PFromIndex(message.newType) ?: return null

    if (state.gridCache.changeAllP2Ps(message.p2p!!, type)) {
      ModNetwork.requestP2PList(ctx.serverHandler.player, type.index)
    }

    return null
  }
}
