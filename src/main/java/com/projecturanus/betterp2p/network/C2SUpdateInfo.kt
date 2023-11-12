package com.projecturanus.betterp2p.network

import com.projecturanus.betterp2p.capability.MemoryInfo
import com.projecturanus.betterp2p.client.gui.widget.GuiScale
import com.projecturanus.betterp2p.item.BetterMemoryCardModes
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage

fun writeMemoryInfo(buf: ByteBuf, info: MemoryInfo) {
    buf.writeLong(info.selectedEntry)
    buf.writeShort(info.frequency.toInt())
    buf.writeInt(info.mode.ordinal)
    buf.writeByte(info.gui.ordinal)
}

fun readMemoryInfo(buf: ByteBuf): MemoryInfo {
    return MemoryInfo(
        buf.readLong(), // selectedIndex
        buf.readShort(), // frequency
        BetterMemoryCardModes.values()[buf.readInt()], // mode
        GuiScale.values()[buf.readByte().toInt()]
    )
}

class C2SUpdateInfo(var info: MemoryInfo = MemoryInfo()) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        info = readMemoryInfo(buf)
    }

    override fun toBytes(buf: ByteBuf) {
        writeMemoryInfo(buf, info)
    }
}
