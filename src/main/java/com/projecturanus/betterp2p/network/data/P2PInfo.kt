package com.projecturanus.betterp2p.network.data

import appeng.me.GridNode
import appeng.parts.p2p.PartP2PTunnel
import com.projecturanus.betterp2p.util.p2p.getTypeIndex
import com.projecturanus.betterp2p.util.p2p.hasChannel
import io.netty.buffer.ByteBuf
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class P2PInfo(
    val frequency: Short,
    val pos: BlockPos,
    val dim: Int,
    val facing: EnumFacing,
    val name: String,
    val output: Boolean,
    val hasChannel: Boolean,
    val channels: Int,
    val type: Int
) {
    override fun hashCode(): Int {
        return hashP2P(pos, facing.ordinal, dim).hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as P2PInfo
        if (this.pos != other.pos) return false
        if (facing != other.facing) return false
        return dim == other.dim
    }
}

fun readP2PInfo(buf: ByteBuf): P2PInfo? {
    try {
        val freq = buf.readShort()
        val pos = BlockPos.fromLong(buf.readLong())
        val world = buf.readInt()
        val facing = EnumFacing.values()[buf.readInt()]
        val nameLength = buf.readShort() - 1
        var name: StringBuilder = StringBuilder()
        for (i in 0..nameLength) {
            name.append(buf.readChar())
        }
        val output = buf.readBoolean()
        val hasChannel = buf.readBoolean()
        val channels = buf.readByte().toInt()
        val type = buf.readByte().toInt()
        return P2PInfo(freq, pos, world, facing, name.toString(), output, hasChannel, channels, type)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun writeP2PInfo(buf: ByteBuf, info: P2PInfo) {
    buf.writeShort(info.frequency.toInt())
    buf.writeLong(info.pos.toLong())
    buf.writeInt(info.dim)
    buf.writeInt(info.facing.index)
    buf.writeShort(info.name.length)
    for (c in info.name) {
        buf.writeChar(c.toInt())
    }
    buf.writeBoolean(info.output)
    buf.writeBoolean(info.hasChannel)
    buf.writeByte(info.channels)
    buf.writeByte(info.type)
}

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
