package com.projecturanus.betterp2p.client.gui

import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.opengl.GL11

/**
 * Draws a textured quad.
 *
 * x0, y0 - top left corner
 * x1, y1 - bottom right corner
 * u0, v0 - top left texture corner
 * u1, v1 - bottom right texture corner
 */
fun drawTexturedQuad(tessellator: Tessellator, x0: Double, y0: Double, x1: Double, y1: Double, u0: Double, v0: Double, u1: Double, v1: Double) {
    val bufferBuilder = tessellator.buffer
    bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
    bufferBuilder.pos(x0, y1, 0.0).tex(u0, v1).endVertex()
    bufferBuilder.pos(x1, y1, 0.0).tex(u1, v1).endVertex()
    bufferBuilder.pos(x1, y0, 0.0).tex(u1, v0).endVertex()
    bufferBuilder.pos(x0, y0, 0.0).tex(u0, v0).endVertex()
    tessellator.draw()
}
