package com.invsortbuttons.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * Port of InvTweaksGuiIconButton (MIT): a 10x10 mini button assembled from the
 * four corners of the vanilla widget texture, so it scales the classic button
 * look down without distortion.
 */
public abstract class MiniIconButton extends AbstractButton {
    private final ITextComponent tooltip;

    protected MiniIconButton(int x, int y, ITextComponent tooltip) {
        super(x, y, 10, 10, new StringTextComponent(""));
        this.tooltip = tooltip;
    }

    public ITextComponent getTooltipText() {
        return this.tooltip;
    }

    @Override
    public void renderButton(MatrixStack pose, int mouseX, int mouseY, float partialTick) {
        Minecraft.getInstance().getTextureManager().bind(WIDGETS_LOCATION);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        int k = this.getYImage(this.isHovered());
        int halfW = this.width / 2;
        int halfH = this.height / 2;
        // The original's 4-quad trick: stitch the button from its corners
        this.blit(pose, this.x, this.y, 1, 46 + k * 20 + 1, halfW, halfH);
        this.blit(pose, this.x, this.y + halfH, 1, 46 + k * 20 + 20 - halfH - 1, halfW, halfH);
        this.blit(pose, this.x + halfW, this.y, 200 - halfW - 1, 46 + k * 20 + 1, halfW, halfH);
        this.blit(pose, this.x + halfW, this.y + halfH, 200 - halfW - 1, 46 + k * 20 + 19 - halfH, halfW, halfH);
        this.drawIcon(pose, this.getIconColor(mouseX, mouseY));
    }

    protected abstract void drawIcon(MatrixStack pose, int color);

    protected int getIconColor(int mouseX, int mouseY) {
        if (!this.active) {
            return 0xFFA0A0A0;
        }
        return this.isHovered() ? 0xFFFFFFA0 : 0xFFE0E0E0;
    }

    @Override
    public void playDownSound(net.minecraft.client.audio.SoundHandler manager) {
        if (com.invsortbuttons.ISBConfig.SOUNDS.get()) {
            super.playDownSound(manager);
        }
    }
}
