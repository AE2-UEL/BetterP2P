package com.projecturanus.betterp2p.network

import io.netty.buffer.ByteBuf
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

fun readInfo(buf: ByteBuf): P2PInfo {
    val freq = buf.readShort()
    val pos = BlockPos.fromLong(buf.readLong())
    val world = buf.readInt()
    val facing = EnumFacing.values()[buf.readInt()]
    val nameLength = buf.readShort() - 1
    var name = ""
    for (i in 0..nameLength) {
        name += buf.readChar()
    }
    val output = buf.readBoolean()
    val hasChannel = buf.readBoolean()
    val channels = buf.readByte().toInt()
    val type = buf.readByte().toInt()
    return P2PInfo(freq, pos, world, facing, name, output, hasChannel, channels, type)
}

fun writeInfo(buf: ByteBuf, info: P2PInfo) {
    buf.writeShort(info.frequency.toInt())
    buf.writeLong(info.pos.toLong())
    buf.writeInt(info.world)
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

class S2CRefreshInfo(var infos: List<P2PInfo> = emptyList()) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        val length = buf.readInt()
        val list = ArrayList<P2PInfo>(length)
        for (i in 0 until length) {
            list += readInfo(buf)
        }
        infos = list
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(infos.size)
        for (info in infos) {
            writeInfo(buf, info)
        }
    }
}
