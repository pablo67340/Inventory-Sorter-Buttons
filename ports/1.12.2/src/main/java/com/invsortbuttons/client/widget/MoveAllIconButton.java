package com.invsortbuttons.client.widget;

import com.invsortbuttons.sort.SortEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * The IPN-style "move all" arrow buttons: an up arrow that puts your whole
 * inventory into the container, and a down arrow that takes everything out of
 * it. Works purely client-side by quick-moving each slot, exactly like the
 * Space+Click shortcut, so it needs no server support.
 */
public class MoveAllIconButton extends MiniIconButton {
    private final boolean intoContainer;

    public MoveAllIconButton(int id, int x, int y, boolean intoContainer, String tooltip) {
        super(id, x, y, tooltip);
        this.intoContainer = intoContainer;
    }

    @Override
    public void onPress() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.playerController == null
                || !(mc.currentScreen instanceof GuiContainer)) {
            return;
        }
        GuiContainer screen = (GuiContainer) mc.currentScreen;
        int containerId = screen.inventorySlots.windowId;
        if (this.intoContainer) {
            // Put all: every player slot except the hotbar (hold Shift to include it)
            boolean includeHotbar = GuiScreen.isShiftKeyDown();
            for (Slot s : screen.inventorySlots.inventorySlots) {
                if (!s.getHasStack() || !SortEngine.isPlayerSlot(s, mc.player.inventory)) {
                    continue;
                }
                boolean hotbar = s.inventory == mc.player.inventory && s.getSlotIndex() < 9;
                if (hotbar && !includeHotbar) {
                    continue;
                }
                mc.playerController.windowClick(containerId, s.slotNumber, 0, ClickType.QUICK_MOVE, mc.player);
            }
        } else {
            // Take all: empty the container into the main inventory only — never
            // the hotbar, mirroring how Put All leaves the hotbar alone. Quick-move
            // would spill into the hotbar, so pick up each stack and place it into
            // main-inventory slots by hand (merge into matching stacks first).
            List<Slot> chest = SortEngine.chestSlots(screen.inventorySlots, mc.player.inventory);
            List<Slot> targets = new java.util.ArrayList<Slot>();
            for (Slot s : screen.inventorySlots.inventorySlots) {
                if (s.inventory == mc.player.inventory
                        && s.getSlotIndex() >= 9 && s.getSlotIndex() < 36) {
                    targets.add(s);
                }
            }
            for (Slot cs : chest) {
                if (!cs.getHasStack()) {
                    continue;
                }
                mc.playerController.windowClick(containerId, cs.slotNumber, 0, ClickType.PICKUP, mc.player);
                for (int pass = 0; pass < 2 && !mc.player.inventory.getItemStack().isEmpty(); pass++) {
                    for (Slot t : targets) {
                        ItemStack carried = mc.player.inventory.getItemStack();
                        if (carried.isEmpty()) {
                            break;
                        }
                        boolean fits = pass == 0
                                ? t.getHasStack() && sameStack(t.getStack(), carried)
                                        && t.getStack().getCount() < t.getStack().getMaxStackSize()
                                : !t.getHasStack();
                        if (fits) {
                            mc.playerController.windowClick(containerId, t.slotNumber, 0, ClickType.PICKUP, mc.player);
                        }
                    }
                }
                // Inventory is full: put the remainder back where it came from
                if (!mc.player.inventory.getItemStack().isEmpty()) {
                    mc.playerController.windowClick(containerId, cs.slotNumber, 0, ClickType.PICKUP, mc.player);
                }
            }
        }
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getMetadata() == b.getMetadata()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    @Override
    protected void drawIcon(Minecraft mc, int color) {
        int x = this.x;
        int y = this.y;
        if (this.intoContainer) {
            // Up arrow: tip, widening head, then the shaft
            drawRect(x + 4, y + 2, x + 6, y + 3, color);
            drawRect(x + 3, y + 3, x + 7, y + 4, color);
            drawRect(x + 2, y + 4, x + 8, y + 5, color);
            drawRect(x + 4, y + 5, x + 6, y + 8, color);
        } else {
            // Down arrow: shaft, widening head, then the tip
            drawRect(x + 4, y + 2, x + 6, y + 5, color);
            drawRect(x + 2, y + 5, x + 8, y + 6, color);
            drawRect(x + 3, y + 6, x + 7, y + 7, color);
            drawRect(x + 4, y + 7, x + 6, y + 8, color);
        }
    }
}
