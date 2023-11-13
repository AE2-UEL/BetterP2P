package com.projecturanus.betterp2p.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.opengl.GL11

/**
 * Draws a textured quad.
 *
 * x0, y0 - top left corner
 * x1, y1 - bottom right corner
 * u0, v0 - top left texture corner
 * u1, v1 - bottom right texture corner
 */

@SideOnly(Side.CLIENT)
fun drawTexturedQuad(tessellator: Tessellator, x0: Double, y0: Double, x1: Double, y1: Double, u0: Double, v0: Double, u1: Double, v1: Double) {
    val bufferBuilder = tessellator.buffer
    bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
    bufferBuilder.pos(x0, y1, 0.0).tex(u0, v1).endVertex()
    bufferBuilder.pos(x1, y1, 0.0).tex(u1, v1).endVertex()
    bufferBuilder.pos(x1, y0, 0.0).tex(u1, v0).endVertex()
    bufferBuilder.pos(x0, y0, 0.0).tex(u0, v0).endVertex()
    tessellator.draw()
}

fun drawBlockIcon(mc: Minecraft, icon: ResourceLocation, overlay: ResourceLocation = ResourceLocation("appliedenergistics2", "textures/items/part/p2p_tunnel_front.png"), x: Int, y: Int, width: Double = 16.0, height: Double = 16.0) {
    val tessellator = Tessellator.getInstance()
    GL11.glPushAttrib(GL11.GL_BLEND or GL11.GL_TEXTURE_2D or GL11.GL_COLOR)
    GL11.glEnable(GL11.GL_BLEND)
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glColor3f(255f, 255f, 255f)
    OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
    mc.textureManager.bindTexture(icon)
    drawTexturedQuad(tessellator,
        x0 = x.toDouble() + 2,
        y0 = y.toDouble() + 2,
        x1 = x + width - 2,
        y1 = y + height - 2,
        u0 = 0.0, v0 = 0.0,
        u1 = 1.0, v1 = 1.0)
    mc.textureManager.bindTexture(overlay)
    drawTexturedQuad(tessellator,
        x0 = x.toDouble(),
        y0 = y.toDouble(),
        x1 = x + width,
        y1 = y + height,
        u0 = 0.0, v0 = 0.0,
        u1 = 1.0, v1 = 1.0)
    GL11.glPopAttrib()
}
