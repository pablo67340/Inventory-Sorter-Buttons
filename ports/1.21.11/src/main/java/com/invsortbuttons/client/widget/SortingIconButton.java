package com.invsortbuttons.client.widget;

import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.SortPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Port of InvTweaksGuiSortingButton (MIT). The glyphs are the original's exact
 * pixel patterns: "s" (the Z shape), "v" (two columns), "h" (two rows).
 */
public class SortingIconButton extends MiniIconButton {
    private final int mode;
    private final char glyph;

    public SortingIconButton(int x, int y, char glyph, int mode, Component tooltip) {
        super(x, y, tooltip);
        this.glyph = glyph;
        this.mode = mode;
    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        com.invsortbuttons.client.ConfigFiles.syncToServer(false);
        NetworkHandler.sendToServer(new SortPacket(this.mode, false));
    }

    @Override
    protected void drawIcon(GuiGraphics graphics, int color) {
        int x = this.getX();
        int y = this.getY();
        if (this.glyph == 'h') {
            graphics.fill(x + 3, y + 3, x + this.width - 3, y + 4, color);
            graphics.fill(x + 3, y + 6, x + this.width - 3, y + 7, color);
        } else if (this.glyph == 'v') {
            graphics.fill(x + 3, y + 3, x + 4, y + this.height - 3, color);
            graphics.fill(x + 6, y + 3, x + 7, y + this.height - 3, color);
        } else {
            graphics.fill(x + 3, y + 3, x + this.width - 3, y + 4, color);
            graphics.fill(x + 5, y + 4, x + 6, y + 5, color);
            graphics.fill(x + 4, y + 5, x + 5, y + 6, color);
            graphics.fill(x + 3, y + 6, x + this.width - 3, y + 7, color);
        }
    }
}
