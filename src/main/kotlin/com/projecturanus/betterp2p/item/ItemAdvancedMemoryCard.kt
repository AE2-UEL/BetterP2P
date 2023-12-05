package com.projecturanus.betterp2p.item

import appeng.api.config.SecurityPermissions
import appeng.api.networking.IGridHost
import appeng.api.networking.security.ISecurityGrid
import appeng.api.util.AEPartLocation
import appeng.core.CreativeTab
import appeng.parts.p2p.PartP2PTunnel
import com.projecturanus.betterp2p.client.ClientCache
import com.projecturanus.betterp2p.client.gui.widget.GuiScale
import com.projecturanus.betterp2p.network.ModNetwork
import com.projecturanus.betterp2p.network.data.*
import com.projecturanus.betterp2p.util.getPart
import com.projecturanus.betterp2p.util.p2p.getTypeIndex
import net.minecraft.client.resources.I18n
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
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

  override fun onUpdate(
      stack: ItemStack,
      worldIn: World,
      entityIn: Entity,
      itemSlot: Int,
      isSelected: Boolean
  ) {
    super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected)
  }

  @SideOnly(Side.CLIENT)
  override fun addInformation(
      stack: ItemStack,
      worldIn: World?,
      tooltip: MutableList<String>,
      flagIn: ITooltipFlag
  ) {
    val info = getInfo(stack)
    tooltip += I18n.format("gui.advanced_memory_card.mode.${info.mode.name.toLowerCase()}")
  }

  @SideOnly(Side.CLIENT)
  private fun clearClientCache() {
    ClientCache.clear()
  }

  override fun onItemRightClick(
      worldIn: World,
      playerIn: EntityPlayer,
      handIn: EnumHand
  ): ActionResult<ItemStack> {
    if (playerIn.isSneaking && worldIn.isRemote) {
      clearClientCache()
    }
    return super.onItemRightClick(worldIn, playerIn, handIn)
  }

  override fun onItemUse(
      player: EntityPlayer,
      w: World,
      pos: BlockPos,
      hand: EnumHand,
      side: EnumFacing,
      hx: Float,
      hy: Float,
      hz: Float
  ): EnumActionResult {
    if (!w.isRemote) {
      val te = w.getTileEntity(pos)
      if (te is IGridHost && te.getGridNode(AEPartLocation.fromFacing(side)) != null) {
        val part = getPart(w, pos, hx, hy, hz)
        val grid = part?.gridNode?.grid ?: return EnumActionResult.FAIL

        if (grid is ISecurityGrid && !grid.hasPermission(player, SecurityPermissions.BUILD)) {
          return EnumActionResult.FAIL
        }

        val stack = player.getHeldItem(hand)
        val info = getInfo(stack)
        val type: Int
        if (part is PartP2PTunnel<*>) {
          type = part.getTypeIndex()
          info.selectedEntry = part.toLoc()
        } else {
          type = TUNNEL_ANY
          info.selectedEntry = null
        }
        info.type = type
        writeInfo(stack, info)
        ModNetwork.initConnection(player, grid, info)
        return EnumActionResult.SUCCESS
      }
    }
    return EnumActionResult.PASS
  }

  override fun doesSneakBypassUse(
      itemstack: ItemStack,
      world: IBlockAccess?,
      pos: BlockPos?,
      player: EntityPlayer?
  ): Boolean {
    return true
  }

  fun getInfo(stack: ItemStack): MemoryInfo {
    if (stack.item != this)
        throw ClassCastException("Cannot cast ${stack.item.javaClass.name} to ${javaClass.name}")

    // Initialize NBT if it isn't already a thing
    if (stack.tagCompound == null) {
      stack.tagCompound = NBTTagCompound()
    }
    val compound = stack.tagCompound!!
    if (!compound.hasKey("gui")) {
      compound.setByte("gui", GuiScale.DYNAMIC.ordinal.toByte())
    }
    if (!compound.hasKey("selectedIndex", Constants.NBT.TAG_COMPOUND)) {
      compound.setTag("selectedIndex", NBTTagCompound())
    }

    return MemoryInfo(
        selectedEntry = readP2PLocation(compound.getCompoundTag("selectedIndex")),
        frequency = compound.getShort("frequency"),
        mode = BetterMemoryCardModes.values()[compound.getInteger("mode")],
        guiScale = GuiScale.values()[compound.getByte("gui").toInt()])
  }

  fun writeInfo(stack: ItemStack, info: MemoryInfo) {
    if (stack.item != this)
        throw ClassCastException("Cannot cast ${stack.item.javaClass.name} to ${javaClass.name}")

    if (stack.tagCompound == null) stack.tagCompound = NBTTagCompound()
    val compound = stack.tagCompound!!
    compound.setTag("selectedIndex", writeP2PLocation(info.selectedEntry))
    compound.setShort("frequency", info.frequency)
    compound.setInteger("mode", info.mode.ordinal)
    compound.setByte("gui", info.guiScale.ordinal.toByte())
  }
}
