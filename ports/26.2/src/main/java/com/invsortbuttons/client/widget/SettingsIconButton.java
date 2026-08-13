package com.invsortbuttons.client.widget;

import com.invsortbuttons.client.SettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Port of InvTweaksGuiSettingsButton (MIT): the "..." button that opens settings. */
public class SettingsIconButton extends MiniIconButton {
    public SettingsIconButton(int x, int y, Component tooltip) {
        super(x, y, tooltip);
    }

    @Override
    public void onPress(net.minecraft.client.input.InputWithModifiers input) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new SettingsScreen(mc.gui.screen()));
    }

    @Override
    protected void drawIcon(GuiGraphicsExtractor graphics, int color) {
        // Original drew "..." shifted up a pixel so the dots sit centered
        Minecraft mc = Minecraft.getInstance();
        graphics.text(mc.font, "...", this.getX() + 2, this.getY() - 1, color, false);
    }
}
