package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import com.invsortbuttons.client.widget.MiniIconButton;
import com.invsortbuttons.client.widget.MoveAllIconButton;
import com.invsortbuttons.client.widget.SettingsIconButton;
import com.invsortbuttons.client.widget.SortingIconButton;
import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.RefillPacket;
import com.invsortbuttons.network.SortPacket;
import com.invsortbuttons.sort.SortEngine;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.List;

/** Registered on both the Forge and FML buses by the client proxy. */
public final class ClientEvents {
    public static final KeyBinding SORT_KEY = new KeyBinding(
            "key.invsortbuttons.sort", Keyboard.KEY_R, "key.categories.invsortbuttons");

    // Injected button ids, high enough to never collide with a screen's own
    private static final int ID_BASE = 673400;

    /** Buttons we injected into the current screen, for tooltip rendering. */
    private static final List<MiniIconButton> INJECTED = new java.util.ArrayList<>();

    private static final ItemStack[] HOTBAR_SNAPSHOT = new ItemStack[9];
    private static int lastInventoryTotal = -1;
    private static int pickupSortCooldown = 0;
    private static boolean sessionSynced = false;

    // 1.7.10 has no cancellable per-screen input events, so we poll LWJGL state
    // from the tick handler and act right after vanilla processes the click —
    // the same trick the original InvTweaks used on this version.
    private static boolean leftWasDown = false;
    private static boolean middleWasDown = false;
    private static boolean sortKeyWasDown = false;
    /** Hovered slot's stack as of the last rendered frame (pre-click state). */
    private static ItemStack lastHoveredStack = null;
    /** Whether the cursor was empty as of the last rendered frame. */
    private static boolean cursorWasEmpty = true;

    private static void sendPlayerSort() {
        // Reload edited rules/tree files first, like the original's sort key did
        ConfigFiles.syncToServer(false);
        NetworkHandler.CHANNEL.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, true,
                ISBConfig.enableAutoEquipArmor));
        playSortSound();
    }

    private static void playSortSound() {
        if (ISBConfig.enableSounds) {
            Minecraft.getMinecraft().getSoundHandler().playSound(
                    PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
        }
    }

    /** Item + metadata + NBT equality, the pre-flattening "same stack" check. */
    private static boolean sameItemSameTags(ItemStack a, ItemStack b) {
        return a != null && b != null
                && a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    private static int guiLeft(GuiContainer screen) {
        return ReflectionHelper.getPrivateValue(GuiContainer.class, screen, "guiLeft", "field_147003_i");
    }

    private static int guiTop(GuiContainer screen) {
        return ReflectionHelper.getPrivateValue(GuiContainer.class, screen, "guiTop", "field_147009_r");
    }

    private static int xSize(GuiContainer screen) {
        return ReflectionHelper.getPrivateValue(GuiContainer.class, screen, "xSize", "field_146999_f");
    }

    private static Slot slotUnderMouse(GuiContainer screen) {
        return ReflectionHelper.getPrivateValue(GuiContainer.class, screen, "theSlot", "field_147006_u");
    }

    // ------------------------------------------------------------------
    // Button injection — the heart of the tribute
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onScreenInit(GuiScreenEvent.InitGuiEvent.Post event) {
        INJECTED.clear();
        if (!(event.gui instanceof GuiContainer) || event.gui instanceof GuiContainerCreative) {
            return;
        }
        GuiContainer screen = (GuiContainer) event.gui;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            return;
        }

        int left = guiLeft(screen);
        int top = guiTop(screen);
        int xSize = xSize(screen);

        // Survival inventory: just the "..." settings button, like the original
        if (screen instanceof GuiInventory) {
            addButton(event.buttonList, new SettingsIconButton(ID_BASE, left + xSize - 15, top + 5,
                    I18n.format("invsortbuttons.tooltip.settings")));
            return;
        }

        List<Slot> chest = SortEngine.chestSlots(screen.inventorySlots, mc.thePlayer.inventory);
        if (chest.isEmpty()) {
            return;
        }

        // API hook: the screen can place (or hide) the buttons itself
        com.invsortbuttons.api.SortButtonPlacement placement =
                screen instanceof com.invsortbuttons.api.ISortButtonHost
                        ? ((com.invsortbuttons.api.ISortButtonHost) screen).getSortButtonPlacement() : null;
        if (placement != null && placement.type() == com.invsortbuttons.api.SortButtonPlacement.Type.HIDDEN) {
            return;
        }

        String tipSettings = I18n.format("invsortbuttons.tooltip.settings");
        String tipH = I18n.format("invsortbuttons.tooltip.sort_horizontal");
        String tipV = I18n.format("invsortbuttons.tooltip.sort_vertical");
        String tipS = I18n.format("invsortbuttons.tooltip.sort_default");
        String tipPut = I18n.format("invsortbuttons.tooltip.put_all");
        String tipTake = I18n.format("invsortbuttons.tooltip.take_all");

        boolean row;
        int x;
        int y;
        if (placement != null && placement.type() == com.invsortbuttons.api.SortButtonPlacement.Type.ROW) {
            row = true;
            x = placement.x();
            y = placement.y();
        } else if (placement != null && placement.type() == com.invsortbuttons.api.SortButtonPlacement.Type.COLUMN) {
            row = false;
            x = placement.x();
            y = placement.y();
        } else if (screen.getClass().getName().startsWith("net.minecraft.")) {
            // Vanilla GUI: horizontal row in the top-right corner, anchored at z
            row = true;
            x = left + xSize - 17 - 37;
            y = top + 5;
        } else {
            // Modded GUI: vertical column off the left edge, anchored at ...
            row = false;
            x = Math.max(2, left - 13);
            y = top + 8;
        }

        if (row) {
            // Left to right: down, z, ||, =, ...
            if (ISBConfig.showChestButtons) {
                addButton(event.buttonList, new MoveAllIconButton(ID_BASE + 5, x - 12, y, false, tipTake));
                addButton(event.buttonList, new SortingIconButton(ID_BASE + 1, x, y, 's', SortPacket.MODE_DEFAULT, tipS));
                addButton(event.buttonList, new SortingIconButton(ID_BASE + 2, x + 12, y, 'v', SortPacket.MODE_VERTICAL, tipV));
                addButton(event.buttonList, new SortingIconButton(ID_BASE + 3, x + 24, y, 'h', SortPacket.MODE_HORIZONTAL, tipH));
            }
            addButton(event.buttonList, new SettingsIconButton(ID_BASE, x + 36, y, tipSettings));
        } else {
            // Top to bottom: ..., =, ||, z, down
            addButton(event.buttonList, new SettingsIconButton(ID_BASE, x, y, tipSettings));
            if (ISBConfig.showChestButtons) {
                addButton(event.buttonList, new SortingIconButton(ID_BASE + 3, x, y + 13, 'h', SortPacket.MODE_HORIZONTAL, tipH));
                addButton(event.buttonList, new SortingIconButton(ID_BASE + 2, x, y + 26, 'v', SortPacket.MODE_VERTICAL, tipV));
                addButton(event.buttonList, new SortingIconButton(ID_BASE + 1, x, y + 39, 's', SortPacket.MODE_DEFAULT, tipS));
                addButton(event.buttonList, new MoveAllIconButton(ID_BASE + 5, x, y + 52, false, tipTake));
                addButton(event.buttonList, new MoveAllIconButton(ID_BASE + 4, x, y + 65, true, tipPut));
            }
        }

        // In row (vanilla) layouts, Put-all lives at the top-right of the player
        // inventory section, since that's the section it acts on. Modded GUIs can
        // pack slots right up to that spot, so there it stays in the column.
        if (row && ISBConfig.showChestButtons) {
            int invMaxX = Integer.MIN_VALUE;
            int invMinY = Integer.MAX_VALUE;
            for (Object o : screen.inventorySlots.inventorySlots) {
                Slot s = (Slot) o;
                if (SortEngine.isPlayerSlot(s, mc.thePlayer.inventory)) {
                    invMaxX = Math.max(invMaxX, s.xDisplayPosition);
                    invMinY = Math.min(invMinY, s.yDisplayPosition);
                }
            }
            if (invMaxX != Integer.MIN_VALUE) {
                addButton(event.buttonList, new MoveAllIconButton(ID_BASE + 4, left + invMaxX + 6, top + invMinY - 13, true, tipPut));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void addButton(List buttons, MiniIconButton button) {
        buttons.add(button);
        INJECTED.add(button);
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button instanceof MiniIconButton) {
            ((MiniIconButton) event.button).onPress();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onScreenRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiContainer)) {
            return;
        }
        GuiContainer screen = (GuiContainer) event.gui;
        Minecraft mc = Minecraft.getMinecraft();

        // Snapshot the hovered slot + cursor each frame; when a click lands,
        // the tick handler reads these to learn the pre-click state.
        Slot hovered = slotUnderMouse(screen);
        lastHoveredStack = hovered != null && hovered.getHasStack() ? hovered.getStack().copy() : null;
        cursorWasEmpty = mc.thePlayer == null || mc.thePlayer.inventory.getItemStack() == null;

        for (MiniIconButton button : INJECTED) {
            if (button.isHoveredNow()) {
                drawTooltip(screen, mc, button.getTooltipText(), event.mouseX, event.mouseY);
            }
        }
    }

    /** 1.7.10's GuiScreen.drawHoveringText is protected, so we draw our own. */
    private static void drawTooltip(GuiContainer screen, Minecraft mc, String text, int mouseX, int mouseY) {
        int width = mc.fontRenderer.getStringWidth(text);
        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + width + 4 > screen.width) {
            x = screen.width - width - 4;
        }
        GL11.glDisable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        Gui.drawRect(x - 3, y - 4, x + width + 3, y + 12, 0xF0100010);
        Gui.drawRect(x - 2, y - 3, x + width + 2, y + 11, 0x505000FF);
        Gui.drawRect(x - 1, y - 2, x + width + 1, y + 10, 0xF0100010);
        mc.fontRenderer.drawStringWithShadow(text, x, y, 0xFFFFFFFF);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        RenderHelper.enableStandardItemLighting();
        GL11.glEnable(org.lwjgl.opengl.GL12.GL_RESCALE_NORMAL);
    }

    // ------------------------------------------------------------------
    // Middle click sort, click shortcuts, and the in-GUI sort key.
    //
    // Vanilla has already processed the click by the time the END-phase tick
    // handler runs, so a plain left click has put the hovered stack on the
    // cursor. Each shortcut compensates for that, using the pre-click
    // snapshots taken in the draw handler.
    // ------------------------------------------------------------------

    private void handleContainerInput(Minecraft mc, GuiContainer screen) {
        boolean left = Mouse.isButtonDown(0);
        boolean middle = Mouse.isButtonDown(2);
        boolean leftClicked = left && !leftWasDown;
        boolean middleClicked = middle && !middleWasDown;
        leftWasDown = left;
        middleWasDown = middle;

        int sortCode = SORT_KEY.getKeyCode();
        boolean sortDown = sortCode > 0 && Keyboard.isKeyDown(sortCode);
        boolean sortPressed = sortDown && !sortKeyWasDown;
        sortKeyWasDown = sortDown;

        if (mc.thePlayer == null || mc.playerController == null) {
            return;
        }

        if (sortPressed) {
            sendPlayerSort();
        }

        // Middle click is safe to piggyback: survival containers ignore it
        if (middleClicked && ISBConfig.enableMiddleClick) {
            List<Slot> chest = SortEngine.chestSlots(screen.inventorySlots, mc.thePlayer.inventory);
            Slot hovered = slotUnderMouse(screen);
            boolean overPlayer = hovered != null && SortEngine.isPlayerSlot(hovered, mc.thePlayer.inventory);
            if (overPlayer) {
                sendPlayerSort();
            } else if (!chest.isEmpty()) {
                NetworkHandler.CHANNEL.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, false));
                playSortSound();
            }
        }

        // Click shortcuts, matching the original mappings:
        //   Alt+Click        drop the stack
        //   Space+Click      move everything in that section across
        //   Ctrl+Shift+Click move all stacks of the same item across
        //   Ctrl+Click       move a single item across
        if (leftClicked && ISBConfig.enableShortcuts && cursorWasEmpty) {
            Slot hovered = slotUnderMouse(screen);
            if (hovered == null) {
                return;
            }
            int containerId = screen.inventorySlots.windowId;
            boolean ctrl = GuiScreen.isCtrlKeyDown();
            boolean shift = GuiScreen.isShiftKeyDown();
            boolean alt = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
            boolean space = Keyboard.isKeyDown(Keyboard.KEY_SPACE);
            boolean fromPlayer = SortEngine.isPlayerSlot(hovered, mc.thePlayer.inventory);
            ItemStack cursor = mc.thePlayer.inventory.getItemStack();

            if (alt) {
                // Vanilla picked the stack up; a click outside drops it all
                if (cursor != null) {
                    mc.playerController.windowClick(containerId, -999, 0, 0, mc.thePlayer);
                }
            } else if (space) {
                // Put the picked-up stack back, then quick-move the section
                if (cursor != null) {
                    mc.playerController.windowClick(containerId, hovered.slotNumber, 0, 0, mc.thePlayer);
                }
                for (Slot s : SortEngine.allSlots(screen.inventorySlots)) {
                    if (s.getHasStack() && SortEngine.isPlayerSlot(s, mc.thePlayer.inventory) == fromPlayer) {
                        // mode 1 = shift-click (quick move)
                        mc.playerController.windowClick(containerId, s.slotNumber, 0, 1, mc.thePlayer);
                    }
                }
            } else if (ctrl && shift) {
                // Shift+click already quick-moved the hovered stack itself;
                // the pre-click snapshot tells us what item to match
                ItemStack reference = lastHoveredStack;
                if (reference == null) {
                    return;
                }
                for (Slot s : SortEngine.allSlots(screen.inventorySlots)) {
                    if (s.getHasStack() && SortEngine.isPlayerSlot(s, mc.thePlayer.inventory) == fromPlayer
                            && sameItemSameTags(s.getStack(), reference)) {
                        mc.playerController.windowClick(containerId, s.slotNumber, 0, 1, mc.thePlayer);
                    }
                }
            } else if (ctrl) {
                // The stack is on the cursor: right-click one item into the
                // other section, then put the rest back
                if (cursor == null) {
                    return;
                }
                Slot target = null;
                for (Slot s : SortEngine.allSlots(screen.inventorySlots)) {
                    if (s == hovered || SortEngine.isPlayerSlot(s, mc.thePlayer.inventory) == fromPlayer) {
                        continue;
                    }
                    boolean sameWithRoom = s.getHasStack()
                            && sameItemSameTags(s.getStack(), cursor)
                            && s.getStack().stackSize < s.getStack().getMaxStackSize();
                    if (sameWithRoom || (!s.getHasStack() && s.isItemValid(cursor))) {
                        target = s;
                        break;
                    }
                }
                if (target != null) {
                    mc.playerController.windowClick(containerId, target.slotNumber, 1, 0, mc.thePlayer);
                }
                // Put whatever is left back where it came from
                if (mc.thePlayer.inventory.getItemStack() != null) {
                    mc.playerController.windowClick(containerId, hovered.slotNumber, 0, 0, mc.thePlayer);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Auto-refill: watch the hotbar for stacks that ran out during gameplay
    // ------------------------------------------------------------------

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            java.util.Arrays.fill(HOTBAR_SNAPSHOT, null);
            lastInventoryTotal = -1;
            sessionSynced = false;
            return;
        }
        // Send the rules/tree files once per session, like the original's login hook
        if (!sessionSynced) {
            sessionSynced = true;
            ConfigFiles.syncToServer(true);
        }
        // Gameplay sort key (KeyBinding only ticks while no screen is open)
        while (SORT_KEY.isPressed()) {
            sendPlayerSort();
        }

        // In-GUI input: middle click sort, click shortcuts, sort key
        GuiScreen current = mc.currentScreen;
        if (current instanceof GuiContainer && !(current instanceof GuiContainerCreative)) {
            handleContainerInput(mc, (GuiContainer) current);
        } else {
            leftWasDown = Mouse.isButtonDown(0);
            middleWasDown = Mouse.isButtonDown(2);
            sortKeyWasDown = false;
        }

        boolean watch = mc.currentScreen == null && ISBConfig.enableAutoRefill
                && !mc.gameSettings.keyBindDrop.getIsKeyPressed();
        for (int i = 0; i < 9; i++) {
            ItemStack now = mc.thePlayer.inventory.getStackInSlot(i);
            ItemStack before = HOTBAR_SNAPSHOT[i];
            if (watch && before != null && before.stackSize > 0 && now == null) {
                String id = Item.itemRegistry.getNameForObject(before.getItem());
                if (id != null) {
                    NetworkHandler.CHANNEL.sendToServer(
                            new RefillPacket(i, id, before.getItemDamage()));
                }
            }
            HOTBAR_SNAPSHOT[i] = now == null ? null : now.copy();
        }

        // Sort on pickup: the inventory's total item count only grows on pickup
        int total = 0;
        for (int i = 0; i <= 35; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null) {
                total += stack.stackSize;
            }
        }
        if (pickupSortCooldown > 0) {
            pickupSortCooldown--;
        }
        if (ISBConfig.enableSortingOnPickup && mc.currentScreen == null
                && lastInventoryTotal >= 0 && total > lastInventoryTotal && pickupSortCooldown == 0) {
            pickupSortCooldown = 10;
            NetworkHandler.CHANNEL.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, true,
                    ISBConfig.enableAutoEquipArmor));
        }
        lastInventoryTotal = total;
    }
}
