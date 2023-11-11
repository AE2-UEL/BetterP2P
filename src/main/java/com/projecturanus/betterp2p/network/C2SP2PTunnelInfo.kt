package com.projecturanus.betterp2p.network

import com.projecturanus.betterp2p.capability.P2PTunnelInfo
import io.netty.buffer.ByteBuf
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

fun readP2PTunnelInfo(buf: ByteBuf): P2PTunnelInfo {
    return P2PTunnelInfo(
        BlockPos.fromLong(buf.readLong()), // pos
        buf.readInt(),
        EnumFacing.values()[buf.readInt()], // Facing
        buf.readBytes(buf.readInt()).toString(Charsets.UTF_8) // name
    )
}

fun writeP2PTunnelInfo(buf: ByteBuf, info: P2PTunnelInfo) {
    buf.writeLong(info.pos.toLong())
    buf.writeInt(info.world)
    buf.writeInt(info.facing.index)
    if (info.name == null) {
        buf.writeInt(0)
    } else {
        val arr = info.name?.toByteArray(Charsets.UTF_8)
        arr?.let { buf.writeInt(it.size) }
        buf.writeBytes(arr)
    }
}

class C2SP2PTunnelInfo(var info: P2PTunnelInfo = P2PTunnelInfo()): IMessage {

    override fun fromBytes(buf: ByteBuf) {
        info = readP2PTunnelInfo(buf)
    }

    override fun toBytes(buf: ByteBuf) {
        writeP2PTunnelInfo(buf, info)
    }
}
