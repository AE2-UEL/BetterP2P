package com.projecturanus.betterp2p.network

import appeng.api.networking.IGridHost
import appeng.api.parts.IPartHost
import appeng.api.util.AEPartLocation
import appeng.parts.p2p.PartP2PTunnel
import com.projecturanus.betterp2p.item.ItemAdvancedMemoryCard
import com.projecturanus.betterp2p.util.p2p.P2PStatus
import com.projecturanus.betterp2p.util.p2p.toInfo
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ServerRenameP2PTunnel : IMessageHandler<C2SP2PTunnelInfo, IMessage?> {
    override fun onMessage(message: C2SP2PTunnelInfo, ctx: MessageContext): IMessage? {
        val world: World = DimensionManager.getWorld(message.info.world)
        val te: TileEntity? = world.getTileEntity(message.info.pos)
        val player = ctx.serverHandler.player
        if (te is IGridHost && te.getGridNode(AEPartLocation.fromFacing(message.info.facing)) != null) {
            val p = (te as IPartHost).getPart(AEPartLocation.fromFacing(message.info.facing)) as PartP2PTunnel<*>
            p.setCustomName(message.info.name)
            ModNetwork.channel.sendTo(S2CListP2P(
                P2PStatus(player, p.gridNode.grid, p).listP2P.values.map { it.toInfo() },
                ItemAdvancedMemoryCard.getInfo(player.heldItemMainhand)
            ), player)

        }

        return null
    }
}
