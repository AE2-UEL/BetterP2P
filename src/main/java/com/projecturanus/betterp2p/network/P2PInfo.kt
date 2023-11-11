package com.projecturanus.betterp2p.network

import com.projecturanus.betterp2p.client.gui.InfoWrapper
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class P2PInfo(val frequency: Short, val pos: BlockPos, val world: Int, val facing: EnumFacing, val name: String, val output: Boolean, val hasChannel: Boolean) {
    override fun hashCode(): Int {
        return hashP2P(pos, facing.ordinal, world).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is InfoWrapper) {
            this.pos == other.pos &&
            this.facing == other.facing
        } else {
            false
        }
    }
}
