package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import com.invsortbuttons.InvSortButtons;
import com.invsortbuttons.client.widget.MoveAllIconButton;
import com.invsortbuttons.client.widget.SettingsIconButton;
import com.invsortbuttons.client.widget.SortingIconButton;
import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.RefillPacket;
import com.invsortbuttons.network.SortPacket;
import com.invsortbuttons.sort.SortEngine;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.util.List;

@Mod.EventBusSubscriber(modid = InvSortButtons.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private static final ItemStack[] HOTBAR_SNAPSHOT = new ItemStack[9];
    private static int lastInventoryTotal = -1;
    private static int pickupSortCooldown = 0;

    private ClientEvents() {
    }

    private static void sendPlayerSort() {
        // Reload edited rules/tree files first, like the original's sort key did
        ConfigFiles.syncToServer(false);
        NetworkHandler.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, true,
                ISBConfig.AUTO_EQUIP_ARMOR.get()));
        playSortSound();
    }

    @SubscribeEvent
    public static void onLoggingIn(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        ConfigFiles.syncToServer(true);
    }

    private static void playSortSound() {
        if (ISBConfig.SOUNDS.get()) {
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                            net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    // ------------------------------------------------------------------
    // Button injection — the heart of the tribute
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int xSize = screen.getXSize();

        // Survival inventory: just the "..." settings button, like the original
        if (screen instanceof InventoryScreen) {
            event.addListener(new SettingsIconButton(left + xSize - 15, top + 5,
                    Component.translatable("invsortbuttons.tooltip.settings")));
            return;
        }

        List<Slot> chest = SortEngine.chestSlots(screen.getMenu(), mc.player.getInventory());
        if (chest.isEmpty()) {
            return;
        }

        // API hook: the screen can place (or hide) the buttons itself
        com.invsortbuttons.api.SortButtonPlacement placement =
                screen instanceof com.invsortbuttons.api.ISortButtonHost host
                        ? host.getSortButtonPlacement() : null;
        if (placement != null && placement.type() == com.invsortbuttons.api.SortButtonPlacement.Type.HIDDEN) {
            return;
        }

        Component tipSettings = Component.translatable("invsortbuttons.tooltip.settings");
        Component tipH = Component.translatable("invsortbuttons.tooltip.sort_horizontal");
        Component tipV = Component.translatable("invsortbuttons.tooltip.sort_vertical");
        Component tipS = Component.translatable("invsortbuttons.tooltip.sort_default");
        Component tipPut = Component.translatable("invsortbuttons.tooltip.put_all");
        Component tipTake = Component.translatable("invsortbuttons.tooltip.take_all");

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
            if (ISBConfig.CHEST_BUTTONS.get()) {
                event.addListener(new MoveAllIconButton(x - 12, y, false, tipTake));
                event.addListener(new SortingIconButton(x, y, 's', SortPacket.MODE_DEFAULT, tipS));
                event.addListener(new SortingIconButton(x + 12, y, 'v', SortPacket.MODE_VERTICAL, tipV));
                event.addListener(new SortingIconButton(x + 24, y, 'h', SortPacket.MODE_HORIZONTAL, tipH));
            }
            event.addListener(new SettingsIconButton(x + 36, y, tipSettings));
        } else {
            // Top to bottom: ..., =, ||, z, down
            event.addListener(new SettingsIconButton(x, y, tipSettings));
            if (ISBConfig.CHEST_BUTTONS.get()) {
                event.addListener(new SortingIconButton(x, y + 13, 'h', SortPacket.MODE_HORIZONTAL, tipH));
                event.addListener(new SortingIconButton(x, y + 26, 'v', SortPacket.MODE_VERTICAL, tipV));
                event.addListener(new SortingIconButton(x, y + 39, 's', SortPacket.MODE_DEFAULT, tipS));
                event.addListener(new MoveAllIconButton(x, y + 52, false, tipTake));
                event.addListener(new MoveAllIconButton(x, y + 65, true, tipPut));
            }
        }

        // In row (vanilla) layouts, Put-all lives at the top-right of the player
        // inventory section, since that's the section it acts on. Modded GUIs can
        // pack slots right up to that spot, so there it stays in the column.
        if (row && ISBConfig.CHEST_BUTTONS.get()) {
            int invMaxX = Integer.MIN_VALUE;
            int invMinY = Integer.MAX_VALUE;
            for (Slot s : screen.getMenu().slots) {
                if (SortEngine.isPlayerSlot(s, mc.player.getInventory())) {
                    invMaxX = Math.max(invMaxX, s.x);
                    invMinY = Math.min(invMinY, s.y);
                }
            }
            if (invMaxX != Integer.MIN_VALUE) {
                event.addListener(new MoveAllIconButton(left + invMaxX + 6, top + invMinY - 13, true, tipPut));
            }
        }
    }

    // ------------------------------------------------------------------
    // Middle click sort + click shortcuts
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static boolean onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || screen instanceof CreativeModeInventoryScreen) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return false;
        }

        if (event.getInfo().button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && ISBConfig.MIDDLE_CLICK.get()) {
            List<Slot> chest = SortEngine.chestSlots(screen.getMenu(), mc.player.getInventory());
            Slot hovered = screen.getSlotUnderMouse();
            boolean overPlayer = hovered != null && SortEngine.isPlayerSlot(hovered, mc.player.getInventory());
            if (overPlayer) {
                sendPlayerSort();
                return true;
            } else if (!chest.isEmpty()) {
                NetworkHandler.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, false));
                playSortSound();
                return true;
            }
            return false;
        }

        // Click shortcuts, matching the original mappings:
        //   Alt+Click        drop the stack
        //   Space+Click      move everything in that section across
        //   Ctrl+Shift+Click move all stacks of the same item across
        //   Ctrl+Click       move a single item across
        if (event.getInfo().button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && ISBConfig.SHORTCUTS.get()) {
            Slot hovered = screen.getSlotUnderMouse();
            if (hovered == null || !screen.getMenu().getCarried().isEmpty()) {
                return false;
            }
            int containerId = screen.getMenu().containerId;
            boolean ctrl = event.getInfo().hasControlDown();
            boolean shift = event.getInfo().hasShiftDown();
            boolean fromPlayer = SortEngine.isPlayerSlot(hovered, mc.player.getInventory());

            if (event.getInfo().hasAltDown() && hovered.hasItem()) {
                mc.gameMode.handleContainerInput(containerId,
                        hovered.index, 1, ContainerInput.THROW, mc.player);
                return true;
            } else if (InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_SPACE)) {
                for (Slot s : screen.getMenu().slots) {
                    if (s.hasItem() && SortEngine.isPlayerSlot(s, mc.player.getInventory()) == fromPlayer) {
                        mc.gameMode.handleContainerInput(containerId,
                                s.index, 0, ContainerInput.QUICK_MOVE, mc.player);
                    }
                }
                return true;
            } else if (ctrl && shift && hovered.hasItem()) {
                ItemStack reference = hovered.getItem().copy();
                for (Slot s : screen.getMenu().slots) {
                    if (s.hasItem() && SortEngine.isPlayerSlot(s, mc.player.getInventory()) == fromPlayer
                            && ItemStack.isSameItemSameComponents(s.getItem(), reference)) {
                        mc.gameMode.handleContainerInput(containerId,
                                s.index, 0, ContainerInput.QUICK_MOVE, mc.player);
                    }
                }
                return true;
            } else if (ctrl && hovered.hasItem()) {
                // Move one item: pick up the stack, right-click one into the
                // other section, put the rest back
                Slot target = null;
                for (Slot s : screen.getMenu().slots) {
                    if (SortEngine.isPlayerSlot(s, mc.player.getInventory()) == fromPlayer) {
                        continue;
                    }
                    boolean sameWithRoom = s.hasItem()
                            && ItemStack.isSameItemSameComponents(s.getItem(), hovered.getItem())
                            && s.getItem().getCount() < s.getItem().getMaxStackSize();
                    if (sameWithRoom || (!s.hasItem() && s.mayPlace(hovered.getItem()))) {
                        target = s;
                        break;
                    }
                }
                if (target != null) {
                    mc.gameMode.handleContainerInput(containerId, hovered.index, 0, ContainerInput.PICKUP, mc.player);
                    mc.gameMode.handleContainerInput(containerId, target.index, 1, ContainerInput.PICKUP, mc.player);
                    mc.gameMode.handleContainerInput(containerId, hovered.index, 0, ContainerInput.PICKUP, mc.player);
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Sort key (R by default)
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static boolean onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)
                || event.getScreen() instanceof CreativeModeInventoryScreen) {
            return false;
        }
        if (event.getScreen().getFocused() instanceof EditBox) {
            return false;
        }
        if (ClientModEvents.SORT_KEY.getKey().getValue() == event.getInfo().key()) {
            sendPlayerSort();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Auto-refill: watch the hotbar for stacks that ran out during gameplay
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            java.util.Arrays.fill(HOTBAR_SNAPSHOT, null);
            lastInventoryTotal = -1;
            return;
        }
        // Gameplay sort key
        while (ClientModEvents.SORT_KEY.consumeClick()) {
            sendPlayerSort();
        }

        boolean watch = mc.screen == null && ISBConfig.AUTO_REFILL.get() && !mc.options.keyDrop.isDown();
        for (int i = 0; i < 9; i++) {
            ItemStack now = mc.player.getInventory().getItem(i);
            ItemStack before = HOTBAR_SNAPSHOT[i];
            if (watch && before != null && !before.isEmpty() && now.isEmpty()) {
                var id = ForgeRegistries.ITEMS.getKey(before.getItem());
                if (id != null) {
                    NetworkHandler.sendToServer(new RefillPacket(i, id.toString()));
                }
            }
            HOTBAR_SNAPSHOT[i] = now.copy();
        }

        // Sort on pickup: the inventory's total item count only grows on pickup
        int total = 0;
        for (int i = 0; i <= 35; i++) {
            total += mc.player.getInventory().getItem(i).getCount();
        }
        if (pickupSortCooldown > 0) {
            pickupSortCooldown--;
        }
        if (ISBConfig.SORT_ON_PICKUP.get() && mc.screen == null
                && lastInventoryTotal >= 0 && total > lastInventoryTotal && pickupSortCooldown == 0) {
            pickupSortCooldown = 10;
            NetworkHandler.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, true,
                    ISBConfig.AUTO_EQUIP_ARMOR.get()));
        }
        lastInventoryTotal = total;
    }
}
