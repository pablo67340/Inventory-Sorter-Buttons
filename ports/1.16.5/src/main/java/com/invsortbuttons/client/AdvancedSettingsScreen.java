package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

/** Recreation of InvTweaksGuiSettingsAdvanced (MIT): the "More options..." page. */
public class AdvancedSettingsScreen extends Screen {
    private final Screen parent;

    public AdvancedSettingsScreen(Screen parent) {
        super(new TranslationTextComponent("invsortbuttons.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int leftX = this.width / 2 - 155;
        int rightX = this.width / 2 + 5;
        int rowY = this.height / 6 + 48;

        this.addButton(toggleButton(leftX, rowY, ISBConfig.AUTO_EQUIP_ARMOR, "invsortbuttons.settings.autoequip"));
        this.addButton(toggleButton(rightX, rowY, ISBConfig.SORT_ON_PICKUP, "invsortbuttons.settings.sortonpickup"));
        this.addButton(toggleButton(leftX, rowY + 24, ISBConfig.SOUNDS, "invsortbuttons.settings.sounds"));

        int bottomX = this.width / 2 - 100;
        this.addButton(new Button(bottomX, this.height / 6 + 168, 200, 20,
                new TranslationTextComponent("invsortbuttons.settings.shortcutsfile"),
                b -> ConfigFiles.openShortcuts()));

        this.addButton(new Button(bottomX, this.height / 6 + 192, 200, 20,
                new TranslationTextComponent("gui.done"), b -> this.onClose()));
    }

    private Button toggleButton(int x, int y, ForgeConfigSpec.BooleanValue value, String labelKey) {
        return new Button(x, y, 150, 20, toggleLabel(labelKey, value.get()), b -> {
            boolean next = !value.get();
            value.set(next);
            ISBConfig.SPEC.save();
            b.setMessage(toggleLabel(labelKey, next));
        });
    }

    private static ITextComponent toggleLabel(String key, boolean on) {
        return new TranslationTextComponent(key).append(": ").append(
                new TranslationTextComponent(on ? "options.on" : "options.off"));
    }

    @Override
    public void render(MatrixStack pose, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(pose);
        drawCenteredString(pose, this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        // The original's PvP warning note, kept verbatim in spirit
        drawCenteredString(pose, this.font, new TranslationTextComponent("invsortbuttons.settings.pvpnote1"),
                this.width / 2, this.height / 6 + 16, 0xE0E0E0);
        drawCenteredString(pose, this.font, new TranslationTextComponent("invsortbuttons.settings.pvpnote2"),
                this.width / 2, this.height / 6 + 28, 0xE0E0E0);
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
