package com.invsortbuttons.client.widget;

import com.invsortbuttons.client.SettingsScreen;
import net.minecraft.client.Minecraft;

/** Port of InvTweaksGuiSettingsButton (MIT): the "..." button that opens settings. */
public class SettingsIconButton extends MiniIconButton {
    public SettingsIconButton(int id, int x, int y, String tooltip) {
        super(id, x, y, tooltip);
    }

    @Override
    public void onPress() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new SettingsScreen(mc.currentScreen));
    }

    @Override
    protected void drawIcon(Minecraft mc, int color) {
        // Original drew "..." shifted up a pixel so the dots sit centered
        mc.fontRenderer.drawString("...", this.x + 2, this.y - 1, color);
    }
}
