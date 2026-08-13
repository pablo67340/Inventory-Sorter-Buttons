package com.invsortbuttons.client.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Port of InvTweaksGuiIconButton (MIT): a 10x10 mini button. On 1.20.2+ the
 * vanilla button sprites are nine-sliced, so scaling the classic button look
 * down is a single sprite blit instead of the original's four-corner trick.
 */
public abstract class MiniIconButton extends AbstractButton {
    private static final ResourceLocation SPRITE = new ResourceLocation("widget/button");
    private static final ResourceLocation SPRITE_HIGHLIGHTED = new ResourceLocation("widget/button_highlighted");
    private static final ResourceLocation SPRITE_DISABLED = new ResourceLocation("widget/button_disabled");

    private final Component tooltip;

    protected MiniIconButton(int x, int y, Component tooltip) {
        super(x, y, 10, 10, Component.empty());
        this.tooltip = tooltip;
        this.setTooltip(Tooltip.create(tooltip));
    }

    public Component getTooltipText() {
        return this.tooltip;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation sprite = !this.active ? SPRITE_DISABLED
                : this.isHoveredOrFocused() ? SPRITE_HIGHLIGHTED : SPRITE;
        graphics.blitSprite(sprite, this.getX(), this.getY(), this.width, this.height);
        this.drawIcon(graphics, this.getIconColor(mouseX, mouseY));
    }

    protected abstract void drawIcon(GuiGraphics graphics, int color);

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
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
