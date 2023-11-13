package com.projecturanus.betterp2p.item

import appeng.api.networking.IGridHost
import appeng.api.util.AEPartLocation
import appeng.core.CreativeTab
import appeng.parts.p2p.PartP2PTunnel
import com.projecturanus.betterp2p.capability.MemoryInfo
import com.projecturanus.betterp2p.capability.TUNNEL_ANY
import com.projecturanus.betterp2p.client.ClientCache
import com.projecturanus.betterp2p.client.gui.widget.GuiScale
import com.projecturanus.betterp2p.network.ModNetwork
import com.projecturanus.betterp2p.network.NONE_SELECTED
import com.projecturanus.betterp2p.network.S2CListP2P
import com.projecturanus.betterp2p.network.hashP2P
import com.projecturanus.betterp2p.util.getPart
import com.projecturanus.betterp2p.util.p2p.P2PCache
import com.projecturanus.betterp2p.util.p2p.P2PStatus
import com.projecturanus.betterp2p.util.p2p.getTypeIndex
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

object ItemAdvancedMemoryCard : Item() {
    init {
        maxStackSize = 1
        translationKey = "advanced_memory_card"
        creativeTab = CreativeTab.instance
    }

    override fun onUpdate(stack: ItemStack, worldIn: World, entityIn: Entity, itemSlot: Int, isSelected: Boolean) {
        super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected)
    }

    private fun sendStatus(status: P2PStatus, info: MemoryInfo, player: EntityPlayerMP) {
        P2PCache.statusMap[player.uniqueID] = status
        ModNetwork.channel.sendTo(
            S2CListP2P(status.refresh(info.type), info),
            player
        )
    }

    @SideOnly(Side.CLIENT)
    override fun addInformation(stack: ItemStack, worldIn: World?, tooltip: MutableList<String>, flagIn: ITooltipFlag) {
        val info = getInfo(stack)
        tooltip += I18n.format("gui.advanced_memory_card.mode.${info.mode.name.toLowerCase()}")
    }

    @SideOnly(Side.CLIENT)
    private fun clearClientCache() {
        ClientCache.clear()
    }

    override fun onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult<ItemStack> {
        if (playerIn.isSneaking && worldIn.isRemote) {
            clearClientCache()
        }
        return super.onItemRightClick(worldIn, playerIn, handIn)
    }

    override fun onItemUse(player: EntityPlayer, w: World, pos: BlockPos, hand: EnumHand, side: EnumFacing, hx: Float, hy: Float, hz: Float): EnumActionResult {
        if (!w.isRemote) {
            val te = w.getTileEntity(pos)
            if (te is IGridHost && te.getGridNode(AEPartLocation.fromFacing(side)) != null) {
                val part = getPart(w, pos, hx, hy, hz)
                val stack = player.getHeldItem(hand)
                val info = getInfo(stack)
                val type: Int
                val status: P2PStatus
                if (part is PartP2PTunnel<*>) {
                    type = part.getTypeIndex()
                    status = P2PStatus(part.gridNode.grid, player, type)
                    info.selectedEntry = hashP2P(part)
                } else {
                    type = TUNNEL_ANY
                    status = if (part != null) {
                        // If we have a PartP2PTunnelME, it looks at the P2P grid node,
                        // so we should use the part instead.
                        P2PStatus(part.gridNode.grid, player, type)
                    } else {
                        // Fall back to the tile entity.
                        P2PStatus(te.getGridNode(AEPartLocation.fromFacing(side))!!.grid, player, type)
                    }
                    info.selectedEntry = NONE_SELECTED
                }
                info.type = type
                writeInfo(stack, info)
                sendStatus(status, info, player as EntityPlayerMP)
                return EnumActionResult.SUCCESS
            }
        }
        return EnumActionResult.PASS
    }

    override fun doesSneakBypassUse(itemstack: ItemStack, world: IBlockAccess?, pos: BlockPos?, player: EntityPlayer?): Boolean {
        return true
    }

    fun getInfo(stack: ItemStack): MemoryInfo {
        if (stack.item != this) throw ClassCastException("Cannot cast ${stack.item.javaClass.name} to ${javaClass.name}")

        if (stack.tagCompound == null) stack.tagCompound = NBTTagCompound()
        val compound = stack.tagCompound!!
        if (!compound.hasKey("gui")) {
            compound.setByte("gui", GuiScale.DYNAMIC.ordinal.toByte())
        }
        if (!compound.hasKey("selectedIndex", Constants.NBT.TAG_LONG)) {
            compound.setLong("selectedIndex", NONE_SELECTED)
        }

        return MemoryInfo(
            compound.getLong("selectedIndex"),
            compound.getShort("fruequency"),
            BetterMemoryCardModes.values()[compound.getInteger("mode")],
            GuiScale.values()[compound.getByte("gui").toInt()]
        )

    }

    fun writeInfo(stack: ItemStack, info: MemoryInfo) {
        if (stack.item != this) throw ClassCastException("Cannot cast ${stack.item.javaClass.name} to ${javaClass.name}")

        if (stack.tagCompound == null) stack.tagCompound = NBTTagCompound()
        val compound = stack.tagCompound!!
        compound.setLong("selectedIndex", info.selectedEntry)
        compound.setShort("frequency", info.frequency)
        compound.setInteger("mode", info.mode.ordinal)
        compound.setByte("gui", info.gui.ordinal.toByte())
    }
}
