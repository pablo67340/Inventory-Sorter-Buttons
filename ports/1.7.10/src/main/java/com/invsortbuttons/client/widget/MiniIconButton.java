package com.invsortbuttons.client.widget;

import com.invsortbuttons.ISBConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Port of InvTweaksGuiIconButton (MIT): a 10x10 mini button assembled from the
 * four corners of the vanilla widget texture, so it scales the classic button
 * look down without distortion.
 */
public abstract class MiniIconButton extends GuiButton {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");

    private final String tooltip;
    protected boolean hoveredNow;

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
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }
        mc.getTextureManager().bindTexture(WIDGETS);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        this.hoveredNow = mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
        int k = this.getHoverState(this.hoveredNow);
        int halfW = this.width / 2;
        int halfH = this.height / 2;
        // The original's 4-quad trick: stitch the button from its corners
        this.drawTexturedModalRect(this.xPosition, this.yPosition,
                1, 46 + k * 20 + 1, halfW, halfH);
        this.drawTexturedModalRect(this.xPosition, this.yPosition + halfH,
                1, 46 + k * 20 + 20 - halfH - 1, halfW, halfH);
        this.drawTexturedModalRect(this.xPosition + halfW, this.yPosition,
                200 - halfW - 1, 46 + k * 20 + 1, halfW, halfH);
        this.drawTexturedModalRect(this.xPosition + halfW, this.yPosition + halfH,
                200 - halfW - 1, 46 + k * 20 + 19 - halfH, halfW, halfH);
        this.drawIcon(mc, this.getIconColor());
    }

    protected abstract void drawIcon(Minecraft mc, int color);

    protected int getIconColor() {
        if (!this.enabled) {
            return 0xFFA0A0A0;
        }
        return this.hoveredNow ? 0xFFFFFFA0 : 0xFFE0E0E0;
    }

    public boolean isHoveredNow() {
        return this.hoveredNow;
    }

    /** func_146113_a = playPressSound; never renamed by the 1.7.10 stable mappings. */
    @Override
    public void func_146113_a(SoundHandler soundHandler) {
        if (ISBConfig.enableSounds) {
            super.func_146113_a(soundHandler);
        }
    }
}
