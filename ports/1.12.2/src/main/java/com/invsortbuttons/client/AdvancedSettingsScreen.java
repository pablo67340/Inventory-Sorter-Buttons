package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/** Recreation of InvTweaksGuiSettingsAdvanced (MIT): the "More options..." page. */
public class AdvancedSettingsScreen extends GuiScreen {
    private static final int ID_AUTO_EQUIP = 0;
    private static final int ID_SORT_ON_PICKUP = 1;
    private static final int ID_SOUNDS = 2;
    private static final int ID_SHORTCUTS_FILE = 3;
    private static final int ID_DONE = 4;

    private final GuiScreen parent;

    public AdvancedSettingsScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        int leftX = this.width / 2 - 155;
        int rightX = this.width / 2 + 5;
        int rowY = this.height / 6 + 48;

        this.buttonList.add(new GuiButton(ID_AUTO_EQUIP, leftX, rowY, 150, 20,
                toggleLabel("invsortbuttons.settings.autoequip", ISBConfig.enableAutoEquipArmor)));
        this.buttonList.add(new GuiButton(ID_SORT_ON_PICKUP, rightX, rowY, 150, 20,
                toggleLabel("invsortbuttons.settings.sortonpickup", ISBConfig.enableSortingOnPickup)));
        this.buttonList.add(new GuiButton(ID_SOUNDS, leftX, rowY + 24, 150, 20,
                toggleLabel("invsortbuttons.settings.sounds", ISBConfig.enableSounds)));

        int bottomX = this.width / 2 - 100;
        this.buttonList.add(new GuiButton(ID_SHORTCUTS_FILE, bottomX, this.height / 6 + 168, 200, 20,
                I18n.format("invsortbuttons.settings.shortcutsfile")));
        this.buttonList.add(new GuiButton(ID_DONE, bottomX, this.height / 6 + 192, 200, 20,
                I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case ID_AUTO_EQUIP:
                ISBConfig.enableAutoEquipArmor = !ISBConfig.enableAutoEquipArmor;
                ISBConfig.save();
                button.displayString = toggleLabel("invsortbuttons.settings.autoequip", ISBConfig.enableAutoEquipArmor);
                break;
            case ID_SORT_ON_PICKUP:
                ISBConfig.enableSortingOnPickup = !ISBConfig.enableSortingOnPickup;
                ISBConfig.save();
                button.displayString = toggleLabel("invsortbuttons.settings.sortonpickup", ISBConfig.enableSortingOnPickup);
                break;
            case ID_SOUNDS:
                ISBConfig.enableSounds = !ISBConfig.enableSounds;
                ISBConfig.save();
                button.displayString = toggleLabel("invsortbuttons.settings.sounds", ISBConfig.enableSounds);
                break;
            case ID_SHORTCUTS_FILE:
                ConfigFiles.openShortcuts();
                break;
            case ID_DONE:
                this.mc.displayGuiScreen(this.parent);
                break;
            default:
                break;
        }
    }

    private static String toggleLabel(String key, boolean on) {
        return I18n.format(key) + ": " + I18n.format(on ? "options.on" : "options.off");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRenderer,
                I18n.format("invsortbuttons.settings.title"), this.width / 2, 20, 0xFFFFFF);
        // The original's PvP warning note, kept verbatim in spirit
        this.drawCenteredString(this.fontRenderer, I18n.format("invsortbuttons.settings.pvpnote1"),
                this.width / 2, this.height / 6 + 16, 0xE0E0E0);
        this.drawCenteredString(this.fontRenderer, I18n.format("invsortbuttons.settings.pvpnote2"),
                this.width / 2, this.height / 6 + 28, 0xE0E0E0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
