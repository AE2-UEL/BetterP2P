package com.projecturanus.betterp2p.client.gui.widget

import appeng.client.gui.widgets.MEGuiTextField
import com.projecturanus.betterp2p.client.gui.InfoWrapper
import net.minecraft.client.gui.FontRenderer

class IGuiTextField(fontRenderer: FontRenderer, width: Int, height: Int) :
    MEGuiTextField(fontRenderer, 0, 0, width, height) {
  var info: InfoWrapper? = null

  init {
    this.visible = false
  }
}
