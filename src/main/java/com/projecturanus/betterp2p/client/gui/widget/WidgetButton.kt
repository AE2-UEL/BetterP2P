package com.projecturanus.betterp2p.client.gui.widget

import com.projecturanus.betterp2p.MODID
import com.projecturanus.betterp2p.client.gui.GUI_TEX_HEIGHT
import com.projecturanus.betterp2p.client.gui.GUI_WIDTH
import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard
import com.projecturanus.betterp2p.client.gui.drawTexturedQuad
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11
import kotlin.math.exp

/**
 * Widget button for stuff.
 * @param x
 * @param y
 * @param width
 * @param height
 * @param hoverText - unlocalized text to display when mouse hovering
 */
abstract class WidgetButton(val gui: GuiAdvancedMemoryCard, x: Int, y: Int, width: Int, height: Int):
    GuiButton(0, x, y, width, height, "") {

    private var texX = 0.0
    private var texY = 0.0
    var hoverText: List<String> = listOf()

    abstract fun mousePressed(mouseX: Int, mouseY: Int, button: Int): Boolean

    open fun draw(mc: Minecraft, mouseX: Int, mouseY: Int, partial: Float) {
        val tessellator = Tessellator.getInstance()

        drawBG(tessellator, mouseX, mouseY, partial)



        // Draw Button Icon
        drawTexturedQuad(tessellator,
            x0 = x + 1.0,
            y0 = y + 1.0,
            x1 = x + width - 1.0,
            y1 = y + height - 1.0,
            u0 = texX / GUI_WIDTH,
            v0 = texY / GUI_TEX_HEIGHT,
            u1 = (texX + width) / GUI_WIDTH,
            v1 = (texY + height) / GUI_TEX_HEIGHT
        )
    }

    open fun drawBG(tessellator: Tessellator, mouseX: Int, mouseY: Int, partial: Float) {
        gui.bindTexture(gui.BACKGROUND)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height
        val k = getHoverState(hovered)
        GL11.glEnable(GL11.GL_BLEND)
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        // background
        drawTexturedQuad(tessellator, x.toDouble(), y.toDouble(),
            (x + width).toDouble(), (y + height).toDouble(),
            u0 = (32.0 * k) / GUI_WIDTH, v0 = (232.0) / GUI_TEX_HEIGHT,
            u1 = (32.0 * (k + 1)) / GUI_WIDTH, v1 = (232.0 + height) / GUI_TEX_HEIGHT)

    }

    fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    fun setTexCoords(x: Double, y: Double) {
        this.texX = x
        this.texY = y
    }

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun isHovering(mouseX: Int, mouseY: Int): Boolean {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height
    }
}
