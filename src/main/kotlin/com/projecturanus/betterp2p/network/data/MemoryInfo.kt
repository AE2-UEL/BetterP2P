package com.projecturanus.betterp2p.network.data

import com.projecturanus.betterp2p.client.gui.widget.GuiScale
import com.projecturanus.betterp2p.item.BetterMemoryCardModes
import io.netty.buffer.ByteBuf

const val TUNNEL_ANY: Int = -1

data class MemoryInfo(
    var selectedEntry: P2PLocation? = null,
    var frequency: Short = 0,
    var mode: BetterMemoryCardModes = BetterMemoryCardModes.OUTPUT,
    var guiScale: GuiScale = GuiScale.DYNAMIC,
    var type: Int = TUNNEL_ANY
)

fun writeMemoryInfo(buf: ByteBuf, info: MemoryInfo) {
  val hasSelected = info.selectedEntry != null

  buf.writeBoolean(hasSelected)
  if (hasSelected) {
    writeP2PLocation(buf, info.selectedEntry!!)
  }
  buf.writeShort(info.frequency.toInt())
  buf.writeInt(info.mode.ordinal)
  buf.writeByte(info.guiScale.ordinal)
  buf.writeByte(info.type)
}

fun readMemoryInfo(buf: ByteBuf): MemoryInfo {
  var selectedEntry: P2PLocation? = null
  if (buf.readBoolean()) {
    selectedEntry = readP2PLocation(buf)
  }
  val frequency = buf.readShort()
  val mode =
      try {
        BetterMemoryCardModes.values()[buf.readInt()]
      } catch (e: Exception) {
        BetterMemoryCardModes.OUTPUT
      }
  val gui =
      try {
        GuiScale.values()[buf.readByte().toInt()]
      } catch (e: ArrayIndexOutOfBoundsException) {
        GuiScale.DYNAMIC
      }
  val type = buf.readByte().toInt()
  return MemoryInfo(selectedEntry, frequency, mode, gui, type)
}
