package com.projecturanus.betterp2p.client.gui

import appeng.util.Platform
import com.projecturanus.betterp2p.network.P2PInfo
import com.projecturanus.betterp2p.network.hashP2P
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.resources.I18n
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import java.util.*

class InfoWrapper(info: P2PInfo) {
    // Basic information
    val code: Long by lazy {
        hashP2P(pos, facing.ordinal, dim)
    }
    val frequency: Short = info.frequency
    val hasChannel = info.hasChannel
    val pos: BlockPos = info.pos
    val dim: Int = info.world
    val facing: EnumFacing = info.facing
    val description: String
    val output: Boolean = info.output
    var name: String = info.name
    var error: Boolean = false

    // Widgets
    val selectButton = GuiButton(0, 0, 0, 34, 20, I18n.format("gui.advanced_memory_card.select"))
    val bindButton = GuiButton(0, 0, 0, 34, 20, I18n.format("gui.advanced_memory_card.bind"))
    val renameButton = GuiButton(0, 0, 0, 0, 0, "")

    init {
        description = buildString {
            append("P2P ")
            if (output)
                append(I18n.format("gui.advanced_memory_card.desc.mode.output"))
            else
                append(I18n.format("gui.advanced_memory_card.desc.mode.input"))
            append(" - ")
            if (info.frequency.toInt() == 0)
                append(I18n.format("gui.advanced_memory_card.desc.not_set"))
            else
                append(Platform.p2p().toHexString(info.frequency))
        }
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is InfoWrapper) {
            this.pos == other.pos &&
            this.dim == other.dim &&
            this.facing == other.facing
        } else {
            false
        }
    }
}

fun Short.toHexString(): String {
    var tmp: Short = this
    var hex = String()
    while (tmp != 0.toShort()) {
        hex += Integer.toHexString((tmp % 16).toInt())
        tmp = (tmp / 16).toShort()
    }
    return hex.toUpperCase(Locale.getDefault()).reversed()
}

fun String.format4(): String {
    val format = StringBuilder()
    for (index in this.indices) {
        if (index % 4 == 0 && index != 0) {
            format.append(" ")
        }
        format.append(this[index])
    }
    return format.toString()
}
