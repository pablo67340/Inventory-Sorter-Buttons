package com.invsortbuttons.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** The "?" help screen listing the shortcuts this recreation supports. */
public class ShortcutsHelpScreen extends GuiScreen {
    private static final String[] LINES = {
            "invsortbuttons.help.buttons",
            "invsortbuttons.help.moveall",
            "invsortbuttons.help.middleclick",
            "invsortbuttons.help.sortkey",
            "invsortbuttons.help.drop",
            "invsortbuttons.help.oneitem",
            "invsortbuttons.help.alloftype",
            "invsortbuttons.help.everything",
            "invsortbuttons.help.refill",
    };

    private final GuiScreen parent;

    public ShortcutsHelpScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height - 40, 200, 20,
                I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer,
                I18n.format("invsortbuttons.help.title"), this.width / 2, 20, 0xFFFFFF);
        int y = this.height / 6;
        for (String key : LINES) {
            this.drawCenteredString(this.fontRenderer, I18n.format(key), this.width / 2, y, 0xE0E0E0);
            y += 14;
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
