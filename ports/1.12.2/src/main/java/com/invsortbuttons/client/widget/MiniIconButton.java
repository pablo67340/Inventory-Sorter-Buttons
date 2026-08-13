package com.invsortbuttons.client.widget;

import com.invsortbuttons.ISBConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Port of InvTweaksGuiIconButton (MIT): a 10x10 mini button assembled from the
 * four corners of the vanilla widget texture, so it scales the classic button
 * look down without distortion.
 */
public abstract class MiniIconButton extends GuiButton {
    private final String tooltip;

    protected MiniIconButton(int id, int x, int y, String tooltip) {
        super(id, x, y, 10, 10, "");
        this.tooltip = tooltip;
    }

    public String getTooltipText() {
        return this.tooltip;
    }

    /** Called by the mod's ActionPerformedEvent handler when this button is clicked. */
    public abstract void onPress();

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }
        mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
        GlStateManager.color(1f, 1f, 1f, 1f);
        this.hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
        int k = this.getHoverState(this.hovered);
        int halfW = this.width / 2;
        int halfH = this.height / 2;
        // The original's 4-quad trick: stitch the button from its corners
        this.drawTexturedModalRect(this.x, this.y, 1, 46 + k * 20 + 1, halfW, halfH);
        this.drawTexturedModalRect(this.x, this.y + halfH, 1, 46 + k * 20 + 20 - halfH - 1, halfW, halfH);
        this.drawTexturedModalRect(this.x + halfW, this.y, 200 - halfW - 1, 46 + k * 20 + 1, halfW, halfH);
        this.drawTexturedModalRect(this.x + halfW, this.y + halfH, 200 - halfW - 1, 46 + k * 20 + 19 - halfH, halfW, halfH);
        this.drawIcon(mc, this.getIconColor());
    }

    protected abstract void drawIcon(Minecraft mc, int color);

    protected int getIconColor() {
        if (!this.enabled) {
            return 0xFFA0A0A0;
        }
        return this.hovered ? 0xFFFFFFA0 : 0xFFE0E0E0;
    }

    @Override
    public void playPressSound(SoundHandler soundHandler) {
        if (ISBConfig.enableSounds) {
            super.playPressSound(soundHandler);
        }
    }
}
