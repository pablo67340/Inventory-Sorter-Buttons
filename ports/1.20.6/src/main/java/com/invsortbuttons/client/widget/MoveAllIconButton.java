package com.invsortbuttons.client.widget;

import com.invsortbuttons.sort.SortEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The IPN-style "move all" arrow buttons: an up arrow that puts your whole
 * inventory into the container, and a down arrow that takes everything out of
 * it. Works purely client-side by quick-moving each slot, exactly like the
 * Space+Click shortcut, so it needs no server support.
 */
public class MoveAllIconButton extends MiniIconButton {
    private final boolean intoContainer;

    public MoveAllIconButton(int x, int y, boolean intoContainer, Component tooltip) {
        super(x, y, tooltip);
        this.intoContainer = intoContainer;
    }

    @Override
    public void onPress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null
                || !(mc.screen instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        int containerId = screen.getMenu().containerId;
        if (this.intoContainer) {
            // Put all: every player slot except the hotbar (hold Shift to include it)
            boolean includeHotbar = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            for (Slot s : screen.getMenu().slots) {
                if (!s.hasItem() || !SortEngine.isPlayerSlot(s, mc.player.getInventory())) {
                    continue;
                }
                boolean hotbar = s.container == mc.player.getInventory() && s.getContainerSlot() < 9;
                if (hotbar && !includeHotbar) {
                    continue;
                }
                mc.gameMode.handleInventoryMouseClick(containerId, s.index, 0, ClickType.QUICK_MOVE, mc.player);
            }
        } else {
            // Take all: empty the container into the main inventory only — never
            // the hotbar, mirroring how Put All leaves the hotbar alone. Quick-move
            // would spill into the hotbar, so pick up each stack and place it into
            // main-inventory slots by hand (merge into matching stacks first).
            List<Slot> chest = SortEngine.chestSlots(screen.getMenu(), mc.player.getInventory());
            List<Slot> targets = new java.util.ArrayList<>();
            for (Slot s : screen.getMenu().slots) {
                if (s.container == mc.player.getInventory()
                        && s.getContainerSlot() >= 9 && s.getContainerSlot() < 36) {
                    targets.add(s);
                }
            }
            for (Slot cs : chest) {
                if (!cs.hasItem()) {
                    continue;
                }
                mc.gameMode.handleInventoryMouseClick(containerId, cs.index, 0, ClickType.PICKUP, mc.player);
                for (int pass = 0; pass < 2 && !screen.getMenu().getCarried().isEmpty(); pass++) {
                    for (Slot t : targets) {
                        ItemStack carried = screen.getMenu().getCarried();
                        if (carried.isEmpty()) {
                            break;
                        }
                        boolean fits = pass == 0
                                ? t.hasItem() && ItemStack.isSameItemSameComponents(t.getItem(), carried)
                                        && t.getItem().getCount() < t.getItem().getMaxStackSize()
                                : !t.hasItem();
                        if (fits) {
                            mc.gameMode.handleInventoryMouseClick(containerId, t.index, 0, ClickType.PICKUP, mc.player);
                        }
                    }
                }
                // Inventory is full: put the remainder back where it came from
                if (!screen.getMenu().getCarried().isEmpty()) {
                    mc.gameMode.handleInventoryMouseClick(containerId, cs.index, 0, ClickType.PICKUP, mc.player);
                }
            }
        }
    }

    @Override
    protected void drawIcon(GuiGraphics graphics, int color) {
        int x = this.getX();
        int y = this.getY();
        if (this.intoContainer) {
            // Up arrow: tip, widening head, then the shaft
            graphics.fill(x + 4, y + 2, x + 6, y + 3, color);
            graphics.fill(x + 3, y + 3, x + 7, y + 4, color);
            graphics.fill(x + 2, y + 4, x + 8, y + 5, color);
            graphics.fill(x + 4, y + 5, x + 6, y + 8, color);
        } else {
            // Down arrow: shaft, widening head, then the tip
            graphics.fill(x + 4, y + 2, x + 6, y + 5, color);
            graphics.fill(x + 2, y + 5, x + 8, y + 6, color);
            graphics.fill(x + 3, y + 6, x + 7, y + 7, color);
            graphics.fill(x + 4, y + 7, x + 6, y + 8, color);
        }
    }
}
