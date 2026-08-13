package com.invsortbuttons.client.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Port of InvTweaksGuiIconButton (MIT): a 10x10 mini button. On modern
 * versions the vanilla button background is nine-sliced and drawn by
 * AbstractButton itself, so only the icon needs custom rendering.
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
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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
