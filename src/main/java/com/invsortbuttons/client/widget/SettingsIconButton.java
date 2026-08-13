package com.invsortbuttons.client.widget;

import com.invsortbuttons.client.SettingsScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Port of InvTweaksGuiSettingsButton (MIT): the "..." button that opens settings. */
public class SettingsIconButton extends MiniIconButton {
    public SettingsIconButton(int x, int y, Component tooltip) {
        super(x, y, tooltip);
    }

    @Override
    public void onPress() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new SettingsScreen(mc.screen));
    }

    @Override
    protected void drawIcon(PoseStack pose, int color) {
        // Original drew "..." shifted up a pixel so the dots sit centered
        Minecraft mc = Minecraft.getInstance();
        mc.font.draw(pose, "...", this.x + 2.0f, this.y - 1.0f, color);
    }
}
