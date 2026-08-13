package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.Util;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Recreation of InvTweaksGuiSettings (MIT): the "PlayerInventory and chests settings"
 * screen with the classic two-column toggle layout.
 */
public class SettingsScreen extends Screen {
    private static final String HELP_URL = "https://github.com/pablo67340";

    private final Screen parent;

    public SettingsScreen(Screen parent) {
        super(new TranslationTextComponent("invsortbuttons.settings.title"));
        this.parent = parent;
        ConfigFiles.ensureDefaults();
    }

    @Override
    public void removed() {
        // Pick up any edits made while the files were open
        ConfigFiles.syncToServer(false);
    }

    @Override
    protected void init() {
        int leftX = this.width / 2 - 155;
        int rightX = this.width / 2 + 5;
        int rowY = this.height / 6;

        // Row 0: Shortcuts + "?" | Middle click
        this.addButton(toggleButton(leftX, rowY, 130, ISBConfig.SHORTCUTS, "invsortbuttons.settings.shortcuts"));
        this.addButton(new Button(leftX + 132, rowY, 20, 20, new StringTextComponent("?"),
                b -> this.minecraft.setScreen(new ShortcutsHelpScreen(this))));
        this.addButton(toggleButton(rightX, rowY, 150, ISBConfig.MIDDLE_CLICK, "invsortbuttons.settings.middleclick"));

        // Row 1: Chest buttons | Auto-refill
        this.addButton(toggleButton(leftX, rowY + 24, 150, ISBConfig.CHEST_BUTTONS, "invsortbuttons.settings.chestbuttons"));
        this.addButton(toggleButton(rightX, rowY + 24, 150, ISBConfig.AUTO_REFILL, "invsortbuttons.settings.autorefill"));

        // Row 2: More options... (auto-equip armor, sort on pickup, sounds)
        this.addButton(new Button(leftX, rowY + 48, 150, 20,
                new TranslationTextComponent("invsortbuttons.settings.moreoptions"),
                b -> this.minecraft.setScreen(new AdvancedSettingsScreen(this))));

        // Bottom stack, like the original: rules file / tree file / help / Done
        int bottomX = this.width / 2 - 100;
        this.addButton(new Button(bottomX, rowY + 96, 200, 20,
                new TranslationTextComponent("invsortbuttons.settings.rulesfile"),
                b -> ConfigFiles.openRules()));

        this.addButton(new Button(bottomX, rowY + 120, 200, 20,
                new TranslationTextComponent("invsortbuttons.settings.treefile"),
                b -> ConfigFiles.openTree()));

        this.addButton(new Button(bottomX, rowY + 144, 200, 20,
                new TranslationTextComponent("invsortbuttons.settings.onlinehelp"),
                b -> Util.getPlatform().openUri(HELP_URL)));

        this.addButton(new Button(bottomX, rowY + 168, 200, 20,
                new TranslationTextComponent("gui.done"), b -> this.onClose()));
    }

    private Button toggleButton(int x, int y, int w, ForgeConfigSpec.BooleanValue value, String labelKey) {
        return new Button(x, y, w, 20, toggleLabel(labelKey, value.get()), b -> {
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
        super.render(pose, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
