package com.invsortbuttons.client.widget;

import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.SortPacket;
import com.mojang.blaze3d.vertex.PoseStack;
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
    public void onPress() {
        com.invsortbuttons.client.ConfigFiles.syncToServer(false);
        NetworkHandler.CHANNEL.sendToServer(new SortPacket(this.mode, false));
    }

    @Override
    protected void drawIcon(PoseStack pose, int color) {
        int x = this.x;
        int y = this.y;
        if (this.glyph == 'h') {
            fill(pose, x + 3, y + 3, x + this.width - 3, y + 4, color);
            fill(pose, x + 3, y + 6, x + this.width - 3, y + 7, color);
        } else if (this.glyph == 'v') {
            fill(pose, x + 3, y + 3, x + 4, y + this.height - 3, color);
            fill(pose, x + 6, y + 3, x + 7, y + this.height - 3, color);
        } else {
            fill(pose, x + 3, y + 3, x + this.width - 3, y + 4, color);
            fill(pose, x + 5, y + 4, x + 6, y + 5, color);
            fill(pose, x + 4, y + 5, x + 5, y + 6, color);
            fill(pose, x + 3, y + 6, x + this.width - 3, y + 7, color);
        }
    }
}
