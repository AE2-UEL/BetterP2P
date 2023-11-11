package com.projecturanus.betterp2p.capability

import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

data class P2PTunnelInfo(
    var pos: BlockPos = BlockPos(0, 0, 0),
    var world: Int = 0,
    var facing: EnumFacing = EnumFacing.UP,
    var name: String? = null
)
