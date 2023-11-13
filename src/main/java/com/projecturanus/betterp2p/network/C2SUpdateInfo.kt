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
    buf.writeByte(info.type)
}

fun readMemoryInfo(buf: ByteBuf): MemoryInfo {
    val selectedEntry = buf.readLong()
    val frequency = buf.readShort()
    val mode = try {
        BetterMemoryCardModes.values()[buf.readInt()]
    } catch (e: Exception) {
        BetterMemoryCardModes.OUTPUT
    }
    val gui = try {
        GuiScale.values()[buf.readByte().toInt()]
    } catch (e: ArrayIndexOutOfBoundsException) {
        GuiScale.DYNAMIC
    }
    val type = buf.readByte().toInt()
    return MemoryInfo(selectedEntry, frequency, mode, gui, type)
}

class C2SUpdateInfo(var info: MemoryInfo = MemoryInfo()) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        info = readMemoryInfo(buf)
    }

    override fun toBytes(buf: ByteBuf) {
        writeMemoryInfo(buf, info)
    }
}
