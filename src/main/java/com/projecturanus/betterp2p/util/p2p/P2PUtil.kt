package com.projecturanus.betterp2p.util.p2p

import appeng.api.config.SecurityPermissions
import appeng.api.networking.IGrid
import appeng.api.networking.security.ISecurityGrid
import appeng.api.parts.IPart
import appeng.api.parts.PartItemStack
import appeng.api.util.AEColor
import appeng.api.util.AEPartLocation
import appeng.me.GridAccessException
import appeng.me.GridNode
import appeng.parts.p2p.PartP2PTunnel
import appeng.util.Platform
import appeng.me.cache.P2PCache
import com.projecturanus.betterp2p.BetterP2P
import com.projecturanus.betterp2p.capability.TUNNEL_ANY
import com.projecturanus.betterp2p.network.P2PInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumHand

val PartP2PTunnel<*>.colorCode: Array<AEColor> get() = Platform.p2p().toColors(this.frequency)

fun linkP2P(player: EntityPlayer, inputIndex: Long, outputIndex: Long, status: P2PStatus) : Pair<PartP2PTunnel<*>, PartP2PTunnel<*>>? {
    val input = status.listP2P[inputIndex] ?: return null
    var output = status.listP2P[outputIndex] ?: return null

    val grid: IGrid? = input.gridNode?.grid
    if (grid is ISecurityGrid) {
        if (!grid.hasPermission(player, SecurityPermissions.BUILD) || !grid.hasPermission(player, SecurityPermissions.SECURITY)) {
            return null
        }
    }

    // TODO Change to exception
    if (input.javaClass != output.javaClass) {
        // change output to input
        output = changeP2P(output, BetterP2P.proxy.getP2PFromClass(input.javaClass)!!, player) ?: return null
    }
    if (input == output) {
        // Network loop
        return null
    }
    var frequency = input.frequency
    val cache = input.proxy.p2P
    // TODO reduce changes
    if (input.frequency == 0.toShort() || input.isOutput) {
        frequency = cache.newFrequency()
    }
    if (cache.getInputs(frequency, input.javaClass) != null) {
        val originalInputs = cache.getInputs(frequency, input.javaClass)
        for (originalInput in originalInputs) {
            if (originalInput != input)
                updateP2P(originalInput, frequency, player, true, input.customInventoryName)
        }
    }

    return updateP2P(input, frequency, player, false, input.customInventoryName) to updateP2P(output, frequency, player, true, input.customInventoryName)
}

/**
 * Due to Applied Energistics' limit
 */
fun updateP2P(tunnel: PartP2PTunnel<*>, frequency: Short, player: EntityPlayer, output: Boolean, name: String): PartP2PTunnel<*> {
    val side = tunnel.side
    val data = NBTTagCompound()
    tunnel.host.removePart(side, true)

    val p2pItem: ItemStack = tunnel.getItemStack(PartItemStack.WRENCH)
    tunnel.outputProperty = output
    tunnel.setCustomName(name)

    p2pItem.writeToNBT(data)
    data.setShort("freq", frequency)

    val colors = Platform.p2p().toColors(frequency)
    val colorCode = intArrayOf(
        colors[0].ordinal, colors[0].ordinal, colors[1].ordinal, colors[1].ordinal,
        colors[2].ordinal, colors[2].ordinal, colors[3].ordinal, colors[3].ordinal
    )

    data.setIntArray("colorCode", colorCode)

    val newType = ItemStack(data)
    val dir: AEPartLocation = tunnel.host?.addPart(newType, side, player, EnumHand.MAIN_HAND) ?: throw RuntimeException("Cannot bind")
    val newBus: IPart = tunnel.host.getPart(dir)

    if (newBus is PartP2PTunnel<*>) {
        newBus.outputProperty = output
        try {
            val p2p = newBus.proxy.p2P
            p2p.updateFreq(newBus, frequency)
        } catch (e: GridAccessException) {
            // :P
        }
        newBus.onTunnelNetworkChange()
        return newBus
    } else {
        throw RuntimeException("Cannot bind")
    }
}

fun unlinkP2P(player: EntityPlayer, p2pIndex: Long, status: P2PStatus): PartP2PTunnel<*>? {
    if (status.grid is ISecurityGrid) {
        if (!status.grid.hasPermission(player, SecurityPermissions.BUILD) ||
            !status.grid.hasPermission(player, SecurityPermissions.SECURITY)) {
            return null
        }
    }
    val tunnel = status.listP2P[p2pIndex] ?: return null
    val oldFreq = tunnel.frequency
    if (oldFreq == 0.toShort()) {
        return tunnel
    }

    updateP2P(tunnel, 0.toShort(), player, false, tunnel.customInventoryName)
    return tunnel
}

/**
 * Converts one P2P into the type
 */
fun changeP2P(tunnel: PartP2PTunnel<*>, newType: TunnelInfo, player: EntityPlayer): PartP2PTunnel<*>? {
    val grid = tunnel.gridNode.grid
    if (grid is ISecurityGrid) {
        if (!grid.hasPermission(player, SecurityPermissions.BUILD) ||
            !grid.hasPermission(player, SecurityPermissions.SECURITY)) {
            return null
        }
    }
    if (BetterP2P.proxy.getP2PFromClass(tunnel.javaClass) == newType) {
        return null
    }
//    if (tunnel is PartP2PInterface) {
//         For output, drop items.
//        val dropItems = mutableListOf<ItemStack>()
//        val patternsOut = tunnel.interfaceDuality.patterns as AppEngInternalInventory
//        dropItems.addAll(patternsOut)
//        Platform.spawnDrops(player.worldObj, tunnel.location.x, tunnel.location.y, tunnel.location.z, dropItems)
//    }
    val host = tunnel.host
    host.removePart(tunnel.side, false)
    val dir = host.addPart(newType.stack, tunnel.side, player, EnumHand.MAIN_HAND)
    val newPart = host.getPart(dir)
    if (newPart is PartP2PTunnel<*>) {
        newPart.outputProperty = tunnel.isOutput
        try {
            val p2p: P2PCache = newPart.proxy.p2P
            p2p.updateFreq(newPart, tunnel.frequency)
            return newPart
        } catch (e: GridAccessException) {
            // :P
        }
    }
    return null
}

/**
 * Converts all connected P2Ps to a new type
 */
fun changeAllP2Ps(tunnel: PartP2PTunnel<*>, newType: TunnelInfo, player: EntityPlayer): Boolean {
    val grid = tunnel.gridNode.grid
    if (grid is ISecurityGrid) {
        if (!grid.hasPermission(player, SecurityPermissions.BUILD) ||
            !grid.hasPermission(player, SecurityPermissions.SECURITY)) {
            return false
        }
    }
    try {
        if (tunnel.isOutput && !tunnel.inputs.isEmpty) {
            return changeAllP2Ps(tunnel.inputs.first(), newType, player)
        } else {
            val outputs = tunnel.outputs.toMutableList()
            changeP2P(tunnel, newType, player)
            for (o in outputs) {
                changeP2P(o, newType, player)
            }
            return true
        }
    } catch (e: GridAccessException) {
        // :P
    }
    return false
}

var PartP2PTunnel<*>.outputProperty
    get() = isOutput
    set(value) {
        val field = PartP2PTunnel::class.java.getDeclaredField("output")
        field.isAccessible = true
        field.setBoolean(this, value)
    }

val PartP2PTunnel<*>.hasChannel
    get() = isPowered && isActive

fun PartP2PTunnel<*>.toInfo()
    = P2PInfo(
    frequency,
    location.pos,
    location.world.provider.dimension,
    side.facing,
    customInventoryName,
    isOutput,
    hasChannel,
    (externalFacingNode as? GridNode)?.usedChannels() ?: -1,
    getTypeIndex()
    )

/**
 * Get the type index or use TUNNEL_ANY
 */
fun PartP2PTunnel<*>.getTypeIndex()
    = BetterP2P.proxy.getP2PFromClass(this.javaClass)?.index ?: TUNNEL_ANY


