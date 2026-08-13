package com.invsortbuttons.client.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

/**
 * Port of InvTweaksGuiIconButton (MIT): a 10x10 mini button assembled from the
 * four corners of the vanilla widget texture, so it scales the classic button
 * look down without distortion.
 */
public abstract class MiniIconButton extends AbstractButton {
    private final Component tooltip;

    protected MiniIconButton(int x, int y, Component tooltip) {
        super(x, y, 10, 10, Component.empty());
        this.tooltip = tooltip;
    }

    public Component getTooltipText() {
        return this.tooltip;
    }

    @Override
    public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int k = this.getYImage(this.isHoveredOrFocused());
        int halfW = this.width / 2;
        int halfH = this.height / 2;
        // The original's 4-quad trick: stitch the button from its corners
        this.blit(pose, this.x, this.y, 1, 46 + k * 20 + 1, halfW, halfH);
        this.blit(pose, this.x, this.y + halfH, 1, 46 + k * 20 + 20 - halfH - 1, halfW, halfH);
        this.blit(pose, this.x + halfW, this.y, 200 - halfW - 1, 46 + k * 20 + 1, halfW, halfH);
        this.blit(pose, this.x + halfW, this.y + halfH, 200 - halfW - 1, 46 + k * 20 + 19 - halfH, halfW, halfH);
        this.drawIcon(pose, this.getIconColor(mouseX, mouseY));
    }

    protected abstract void drawIcon(PoseStack pose, int color);

    protected int getIconColor(int mouseX, int mouseY) {
        if (!this.active) {
            return 0xFFA0A0A0;
        }
        return this.isHoveredOrFocused() ? 0xFFFFFFA0 : 0xFFE0E0E0;
    }

    @Override
    public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
        if (com.invsortbuttons.ISBConfig.SOUNDS.get()) {
            super.playDownSound(manager);
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
