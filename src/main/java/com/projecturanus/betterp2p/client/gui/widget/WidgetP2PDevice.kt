package com.projecturanus.betterp2p.client.gui.widget

import appeng.me.GridNode
import appeng.parts.p2p.PartP2PTunnelME
import appeng.tile.networking.TileCableBus
import appeng.util.Platform
import com.projecturanus.betterp2p.client.gui.InfoWrapper
import com.projecturanus.betterp2p.item.BetterMemoryCardModes
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.resources.I18n
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.DimensionManager
import java.awt.Color
import kotlin.reflect.KMutableProperty0

class WidgetP2PDevice(private val selectedInfoProperty: KMutableProperty0<InfoWrapper?>, val modeSupplier: () -> BetterMemoryCardModes, val infoSupplier: () -> InfoWrapper?, var x: Int, var y: Int): Widget() {
    private val outputColor = 0x4566ccff
    private val selectedColor = 0x4545DA75
    private val errorColor = 0x45DA4527
    private val inactiveColor = 0x45FFEA05

    private val rowWidth = 254
    private val rowHeight = 41

    var renderNameTextfield = true

    private var selectedInfo: InfoWrapper?
        get() = selectedInfoProperty.get()
        set(value) {
            selectedInfoProperty.set(value)
        }

    fun render(gui: GuiScreen, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val info = infoSupplier()
        if (info != null) {
            val fontRenderer = gui.mc.fontRenderer
            if (selectedInfo?.index == info.index)
                GuiScreen.drawRect(x, y, x + rowWidth, y + rowHeight, selectedColor)
            else if (info.error) {
                // P2P output without an input
                GuiScreen.drawRect(x, y, x + rowWidth, y + rowHeight, errorColor)
            }
            else if (!info.hasChannel && info.frequency != 0.toShort()) {
                // No channel
                GuiScreen.drawRect(x, y, x + rowWidth, y + rowHeight, inactiveColor)
            }
            else if (selectedInfo?.frequency == info.frequency && info.frequency != 0.toShort()) {
                // Show same frequency
                GuiScreen.drawRect(x, y, x + rowWidth, y + rowHeight, outputColor)
            }

            fontRenderer.drawString(info.description, x + 24, y + 3, 0)
            fontRenderer.drawString(I18n.format("gui.advanced_memory_card.pos", info.pos.x, info.pos.y, info.pos.z), x + 24, y + 12, 0)
            if (renderNameTextfield) {
                fontRenderer.drawString(I18n.format("gui.advanced_memory_card.name", info.name), x + 24, y + 21, 0)
            } else {
                fontRenderer.drawString(I18n.format("gui.advanced_memory_card.name", ""), x + 24, y + 21, 0)
            }
            fontRenderer.drawString(getExtraInfo(info.world, info.pos, info.facing), x + 24, y + 30, 0)

            if (selectedInfo == null) {
                info.bindButton.enabled = false
                info.selectButton.enabled = true
            } else if (info.index != selectedInfo?.index) {
                info.bindButton.enabled = true
                info.selectButton.enabled = true

            } else {
                // TODO Unbind
                info.bindButton.enabled = false
                info.selectButton.enabled = false
            }

            val mode = modeSupplier()
            if (mode == BetterMemoryCardModes.COPY && !info.output && info.frequency != 0.toShort()) {
                info.bindButton.enabled = false
            }
            drawButtons(gui, info, mouseX, mouseY, partialTicks)
            drawP2PColors(gui, info.frequency, x, y)
        }
    }

    private fun drawP2PColors(gui: GuiScreen, frequency: Short, x: Int, y: Int) {
        val colors = Platform.p2p().toColors(frequency)
        drawRectBorder(x + 9, y + 9, 3, 3, Color.BLACK.rgb)
        for (row in 0..1) {
            for (col in 0..1) {
                for (colorIndex in 0..3) {
                    val offsetX: Int = colorIndex % 2
                    val offsetY: Int = colorIndex / 2
                    drawHorizontalLine(x + 7 + col * 6 + offsetX, x + 8 + col * 6 + offsetX, y + 7 + row * 6 + offsetY, Color(colors[colorIndex].dye.colorValue, false).rgb)
                }
                drawRectBorder(x + 6 + col * 6, y + 6 + row * 6, 3, 3, Color.BLACK.rgb)
            }
        }
    }

    private fun drawButtons(gui: GuiScreen, info: InfoWrapper, mouseX: Int, mouseY: Int, partialTicks: Float) {
        info.renameButton.x = x
        info.renameButton.width = rowWidth
        info.renameButton.y = y
        info.renameButton.height = rowHeight

        if (!info.bindButton.enabled && info.selectButton.enabled) {
            info.bindButton.enabled = false
            info.selectButton.enabled = true

            info.selectButton.x = x + 215
            info.selectButton.width = 32
            info.selectButton.y = y + 6
            info.selectButton.drawButton(gui.mc, mouseX, mouseY, partialTicks)
        } else if (info.bindButton.enabled && info.selectButton.enabled) {
            info.bindButton.enabled = true
            info.selectButton.enabled = true

            // Select button on the left
            info.selectButton.x = x + 178
            info.selectButton.width = 32
            info.selectButton.y = y + 6
            info.selectButton.drawButton(gui.mc, mouseX, mouseY, partialTicks)

            // Bind button on the right
            info.bindButton.x = x + 215
            info.bindButton.width = 32
            info.bindButton.y = y + 6
            info.bindButton.drawButton(gui.mc, mouseX, mouseY, partialTicks)
        } else if (!info.selectButton.enabled && !info.bindButton.enabled) {
            // TODO Unbind
            info.bindButton.enabled = false
            info.selectButton.enabled = false
        }
    }

}

fun getExtraInfo(world: Int, pos: BlockPos, face: EnumFacing): String {
    val tile = DimensionManager.getWorld(world)?.getTileEntity(pos)
    if (tile is TileCableBus) {
        val used = ((tile.getPart(face) as? PartP2PTunnelME)?.externalFacingNode as? GridNode)?.usedChannels()
        if (used != null) {
            return I18n.format("gui.advanced_memory_card.extra.channel", used)
        }
    }
    return I18n.format("gui.advanced_memory_card.extra.none")
}
