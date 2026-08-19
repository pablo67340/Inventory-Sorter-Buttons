package com.invsortbuttons.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The "?" help screen listing the shortcuts this recreation supports. */
public class ShortcutsHelpScreen extends Screen {
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

    private final Screen parent;

    public ShortcutsHelpScreen(Screen parent) {
        super(Component.translatable("invsortbuttons.help.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 - 100, this.height - 40, 200, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        int y = this.height / 6;
        for (String key : LINES) {
            graphics.centeredText(this.font, Component.translatable(key), this.width / 2, y, 0xE0E0E0);
            y += 14;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }
}
