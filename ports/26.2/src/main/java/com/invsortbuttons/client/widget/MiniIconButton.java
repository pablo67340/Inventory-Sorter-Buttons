package com.invsortbuttons.client.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Port of InvTweaksGuiIconButton (MIT): a 10x10 mini button. In 26.x,
 * AbstractButton no longer draws its background sprite automatically —
 * extractContents must call extractDefaultSprite itself (like vanilla
 * Button does), then draw the icon on top.
 */
public abstract class MiniIconButton extends AbstractButton {
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
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.extractDefaultSprite(graphics);
        this.drawIcon(graphics, this.getIconColor(mouseX, mouseY));
    }

    protected abstract void drawIcon(GuiGraphicsExtractor graphics, int color);

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
