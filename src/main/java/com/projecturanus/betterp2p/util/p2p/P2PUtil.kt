package com.projecturanus.betterp2p.util.p2p

import appeng.api.config.SecurityPermissions
import appeng.api.networking.IGrid
import appeng.api.networking.security.ISecurityGrid
import appeng.api.parts.IPart
import appeng.api.parts.PartItemStack
import appeng.api.util.AEColor
import appeng.api.util.AEPartLocation
import appeng.me.GridAccessException
import appeng.parts.p2p.PartP2PTunnel
import appeng.util.Platform
import com.projecturanus.betterp2p.network.P2PInfo
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumHand
import net.minecraftforge.common.DimensionManager

val PartP2PTunnel<*>.colorCode: Array<AEColor> get() = Platform.p2p().toColors(this.frequency)

fun linkP2P(player: EntityPlayer, inputIndex: Long, outputIndex: Long, status: P2PStatus) : Pair<PartP2PTunnel<*>, PartP2PTunnel<*>>? {
    val input = status.listP2P[inputIndex]!!
    val output = status.listP2P[outputIndex]!!

    val grid: IGrid? = input.gridNode?.grid
    if (grid is ISecurityGrid) {
        if (!grid.hasPermission(player, SecurityPermissions.BUILD) || !grid.hasPermission(player, SecurityPermissions.SECURITY)) {
            return null
        }
    }

    // TODO Change to exception
    if (input.javaClass != output.javaClass) {
        // Cannot pair two different type of P2P
        return null
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
    = P2PInfo(frequency, location.pos, location.world.provider.dimension ,side.facing, customInventoryName, isOutput, hasChannel)
