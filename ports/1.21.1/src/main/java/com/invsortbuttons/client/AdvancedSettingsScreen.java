package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/** Recreation of InvTweaksGuiSettingsAdvanced (MIT): the "More options..." page. */
public class AdvancedSettingsScreen extends Screen {
    private final Screen parent;

    public AdvancedSettingsScreen(Screen parent) {
        super(Component.translatable("invsortbuttons.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int leftX = this.width / 2 - 155;
        int rightX = this.width / 2 + 5;
        int rowY = this.height / 6 + 48;

        this.addRenderableWidget(toggleButton(leftX, rowY, ISBConfig.AUTO_EQUIP_ARMOR, "invsortbuttons.settings.autoequip"));
        this.addRenderableWidget(toggleButton(rightX, rowY, ISBConfig.SORT_ON_PICKUP, "invsortbuttons.settings.sortonpickup"));
        this.addRenderableWidget(toggleButton(leftX, rowY + 24, ISBConfig.SOUNDS, "invsortbuttons.settings.sounds"));

        int bottomX = this.width / 2 - 100;
        this.addRenderableWidget(Button.builder(
                Component.translatable("invsortbuttons.settings.shortcutsfile"),
                b -> ConfigFiles.openShortcuts())
                .bounds(bottomX, this.height / 6 + 168, 200, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"), b -> this.onClose())
                .bounds(bottomX, this.height / 6 + 192, 200, 20).build());
    }

    private Button toggleButton(int x, int y, ForgeConfigSpec.BooleanValue value, String labelKey) {
        return Button.builder(toggleLabel(labelKey, value.get()), b -> {
            boolean next = !value.get();
            value.set(next);
            ISBConfig.SPEC.save();
            b.setMessage(toggleLabel(labelKey, next));
        }).bounds(x, y, 150, 20).build();
    }

    private static Component toggleLabel(String key, boolean on) {
        return Component.translatable(key).append(": ").append(
                Component.translatable(on ? "options.on" : "options.off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        // The original's PvP warning note, kept verbatim in spirit
        graphics.drawCenteredString(this.font, Component.translatable("invsortbuttons.settings.pvpnote1"),
                this.width / 2, this.height / 6 + 16, 0xE0E0E0);
        graphics.drawCenteredString(this.font, Component.translatable("invsortbuttons.settings.pvpnote2"),
                this.width / 2, this.height / 6 + 28, 0xE0E0E0);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
