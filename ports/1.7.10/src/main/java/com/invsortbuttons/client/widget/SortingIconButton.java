package com.invsortbuttons.client.widget;

import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.SortPacket;
import net.minecraft.client.Minecraft;

/**
 * Port of InvTweaksGuiSortingButton (MIT). The glyphs are the original's exact
 * pixel patterns: "s" (the Z shape), "v" (two columns), "h" (two rows).
 */
public class SortingIconButton extends MiniIconButton {
    private final int mode;
    private final char glyph;

    public SortingIconButton(int id, int x, int y, char glyph, int mode, String tooltip) {
        super(id, x, y, tooltip);
        this.glyph = glyph;
        this.mode = mode;
    }

    @Override
    public void onPress() {
        com.invsortbuttons.client.ConfigFiles.syncToServer(false);
        NetworkHandler.CHANNEL.sendToServer(new SortPacket(this.mode, false));
    }

    @Override
    protected void drawIcon(Minecraft mc, int color) {
        int x = this.xPosition;
        int y = this.yPosition;
        if (this.glyph == 'h') {
            drawRect(x + 3, y + 3, x + this.width - 3, y + 4, color);
            drawRect(x + 3, y + 6, x + this.width - 3, y + 7, color);
        } else if (this.glyph == 'v') {
            drawRect(x + 3, y + 3, x + 4, y + this.height - 3, color);
            drawRect(x + 6, y + 3, x + 7, y + this.height - 3, color);
        } else {
            drawRect(x + 3, y + 3, x + this.width - 3, y + 4, color);
            drawRect(x + 5, y + 4, x + 6, y + 5, color);
            drawRect(x + 4, y + 5, x + 5, y + 6, color);
            drawRect(x + 3, y + 6, x + this.width - 3, y + 7, color);
        }
    }
}
