package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import com.invsortbuttons.InvSortButtons;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

/**
 * Recreation of InvTweaksGuiSettings (MIT): the "Inventory and chests settings"
 * screen with the classic two-column toggle layout.
 */
public class SettingsScreen extends GuiScreen {
    private static final String HELP_URL = "https://github.com/pablo67340";

    private static final int ID_SHORTCUTS = 0;
    private static final int ID_HELP = 1;
    private static final int ID_MIDDLE_CLICK = 2;
    private static final int ID_CHEST_BUTTONS = 3;
    private static final int ID_AUTO_REFILL = 4;
    private static final int ID_MORE_OPTIONS = 5;
    private static final int ID_RULES_FILE = 6;
    private static final int ID_TREE_FILE = 7;
    private static final int ID_ONLINE_HELP = 8;
    private static final int ID_DONE = 9;

    private final GuiScreen parent;

    public SettingsScreen(GuiScreen parent) {
        this.parent = parent;
        ConfigFiles.ensureDefaults();
    }

    @Override
    public void initGui() {
        int leftX = this.width / 2 - 155;
        int rightX = this.width / 2 + 5;
        int rowY = this.height / 6;

        // Row 0: Shortcuts + "?" | Middle click
        this.buttonList.add(new GuiButton(ID_SHORTCUTS, leftX, rowY, 130, 20,
                toggleLabel("invsortbuttons.settings.shortcuts", ISBConfig.enableShortcuts)));
        this.buttonList.add(new GuiButton(ID_HELP, leftX + 132, rowY, 20, 20, "?"));
        this.buttonList.add(new GuiButton(ID_MIDDLE_CLICK, rightX, rowY, 150, 20,
                toggleLabel("invsortbuttons.settings.middleclick", ISBConfig.enableMiddleClick)));

        // Row 1: Chest buttons | Auto-refill
        this.buttonList.add(new GuiButton(ID_CHEST_BUTTONS, leftX, rowY + 24, 150, 20,
                toggleLabel("invsortbuttons.settings.chestbuttons", ISBConfig.showChestButtons)));
        this.buttonList.add(new GuiButton(ID_AUTO_REFILL, rightX, rowY + 24, 150, 20,
                toggleLabel("invsortbuttons.settings.autorefill", ISBConfig.enableAutoRefill)));

        // Row 2: More options... (auto-equip armor, sort on pickup, sounds)
        this.buttonList.add(new GuiButton(ID_MORE_OPTIONS, leftX, rowY + 48, 150, 20,
                I18n.format("invsortbuttons.settings.moreoptions")));

        // Bottom stack, like the original: rules file / tree file / help / Done
        int bottomX = this.width / 2 - 100;
        this.buttonList.add(new GuiButton(ID_RULES_FILE, bottomX, rowY + 96, 200, 20,
                I18n.format("invsortbuttons.settings.rulesfile")));
        this.buttonList.add(new GuiButton(ID_TREE_FILE, bottomX, rowY + 120, 200, 20,
                I18n.format("invsortbuttons.settings.treefile")));
        this.buttonList.add(new GuiButton(ID_ONLINE_HELP, bottomX, rowY + 144, 200, 20,
                I18n.format("invsortbuttons.settings.onlinehelp")));
        this.buttonList.add(new GuiButton(ID_DONE, bottomX, rowY + 168, 200, 20,
                I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case ID_SHORTCUTS:
                ISBConfig.enableShortcuts = !ISBConfig.enableShortcuts;
                ISBConfig.save();
                button.displayString = toggleLabel("invsortbuttons.settings.shortcuts", ISBConfig.enableShortcuts);
                break;
            case ID_HELP:
                this.mc.displayGuiScreen(new ShortcutsHelpScreen(this));
                break;
            case ID_MIDDLE_CLICK:
                ISBConfig.enableMiddleClick = !ISBConfig.enableMiddleClick;
                ISBConfig.save();
                button.displayString = toggleLabel("invsortbuttons.settings.middleclick", ISBConfig.enableMiddleClick);
                break;
            case ID_CHEST_BUTTONS:
                ISBConfig.showChestButtons = !ISBConfig.showChestButtons;
                ISBConfig.save();
                button.displayString = toggleLabel("invsortbuttons.settings.chestbuttons", ISBConfig.showChestButtons);
                break;
            case ID_AUTO_REFILL:
                ISBConfig.enableAutoRefill = !ISBConfig.enableAutoRefill;
                ISBConfig.save();
                button.displayString = toggleLabel("invsortbuttons.settings.autorefill", ISBConfig.enableAutoRefill);
                break;
            case ID_MORE_OPTIONS:
                this.mc.displayGuiScreen(new AdvancedSettingsScreen(this));
                break;
            case ID_RULES_FILE:
                ConfigFiles.openRules();
                break;
            case ID_TREE_FILE:
                ConfigFiles.openTree();
                break;
            case ID_ONLINE_HELP:
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(HELP_URL));
                } catch (Exception e) {
                    InvSortButtons.LOGGER.warn("Could not open browser: {}", e.toString());
                }
                break;
            case ID_DONE:
                this.mc.displayGuiScreen(this.parent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onGuiClosed() {
        // Pick up any edits made while the files were open
        ConfigFiles.syncToServer(false);
    }

    private static String toggleLabel(String key, boolean on) {
        return I18n.format(key) + ": " + I18n.format(on ? "options.on" : "options.off");
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj,
                I18n.format("invsortbuttons.settings.title"), this.width / 2, 20, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
