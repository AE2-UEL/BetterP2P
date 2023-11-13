package com.projecturanus.betterp2p.network

import com.projecturanus.betterp2p.util.p2p.P2PCache
import com.projecturanus.betterp2p.util.p2p.linkP2P
import com.projecturanus.betterp2p.util.p2p.toInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ServerLinkP2PHandler : IMessageHandler<C2SLinkP2P, IMessage?> {
    override fun onMessage(message: C2SLinkP2P, ctx: MessageContext): IMessage? {
        val status = P2PCache.statusMap[ctx.serverHandler.player.uniqueID] ?: return null
        val result = linkP2P(ctx.serverHandler.player, message.input, message.output, status)

        if (result != null) {
            status.listP2P[message.input] = result.first
            status.listP2P[message.output] = result.second
            ModNetwork.queueP2PListUpdate(status, ctx.serverHandler.player)
        }
        return null
    }
}
