package com.projecturanus.betterp2p.client.gui.widget;

import appeng.client.gui.widgets.MEGuiTextField;
import com.projecturanus.betterp2p.client.gui.InfoWrapper;
import net.minecraft.client.gui.FontRenderer;

public class IGuiTextField extends MEGuiTextField {
    public InfoWrapper info = null;

    public IGuiTextField(final FontRenderer fontRenderer, final int width, final int height) {
        super(fontRenderer,0, 0, width, height );
        this.setVisible(false);
    }
}
