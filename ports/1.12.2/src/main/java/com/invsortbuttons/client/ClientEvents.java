package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import com.invsortbuttons.InvSortButtons;
import com.invsortbuttons.client.widget.MiniIconButton;
import com.invsortbuttons.client.widget.MoveAllIconButton;
import com.invsortbuttons.client.widget.SettingsIconButton;
import com.invsortbuttons.client.widget.SortingIconButton;
import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.RefillPacket;
import com.invsortbuttons.network.SortPacket;
import com.invsortbuttons.sort.SortEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.List;

@Mod.EventBusSubscriber(modid = InvSortButtons.MOD_ID, value = Side.CLIENT)
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
    private static boolean keyRegistered = false;
    private static boolean sessionSynced = false;

    private ClientEvents() {
    }

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
                    PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    /** Item + metadata + NBT equality, the pre-flattening "same stack" check. */
    private static boolean sameItemSameTags(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem() && a.getItemDamage() == b.getItemDamage()
                && ItemStack.areItemStackTagsEqual(a, b);
    }

    // ------------------------------------------------------------------
    // Button injection — the heart of the tribute
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenInit(GuiScreenEvent.InitGuiEvent.Post event) {
        INJECTED.clear();
        if (!(event.getGui() instanceof GuiContainer)
                || event.getGui() instanceof GuiContainerCreative) {
            return;
        }
        GuiContainer screen = (GuiContainer) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int xSize = screen.getXSize();

        // Survival inventory: just the "..." settings button, like the original
        if (screen instanceof GuiInventory) {
            addButton(event.getButtonList(), new SettingsIconButton(ID_BASE, left + xSize - 15, top + 5,
                    I18n.format("invsortbuttons.tooltip.settings")));
            return;
        }

        List<Slot> chest = SortEngine.chestSlots(screen.inventorySlots, mc.player.inventory);
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

        List<GuiButton> buttons = event.getButtonList();
        if (row) {
            // Left to right: down, z, ||, =, ...
            if (ISBConfig.showChestButtons) {
                addButton(buttons, new MoveAllIconButton(ID_BASE + 5, x - 12, y, false, tipTake));
                addButton(buttons, new SortingIconButton(ID_BASE + 1, x, y, 's', SortPacket.MODE_DEFAULT, tipS));
                addButton(buttons, new SortingIconButton(ID_BASE + 2, x + 12, y, 'v', SortPacket.MODE_VERTICAL, tipV));
                addButton(buttons, new SortingIconButton(ID_BASE + 3, x + 24, y, 'h', SortPacket.MODE_HORIZONTAL, tipH));
            }
            addButton(buttons, new SettingsIconButton(ID_BASE, x + 36, y, tipSettings));
        } else {
            // Top to bottom: ..., =, ||, z, down
            addButton(buttons, new SettingsIconButton(ID_BASE, x, y, tipSettings));
            if (ISBConfig.showChestButtons) {
                addButton(buttons, new SortingIconButton(ID_BASE + 3, x, y + 13, 'h', SortPacket.MODE_HORIZONTAL, tipH));
                addButton(buttons, new SortingIconButton(ID_BASE + 2, x, y + 26, 'v', SortPacket.MODE_VERTICAL, tipV));
                addButton(buttons, new SortingIconButton(ID_BASE + 1, x, y + 39, 's', SortPacket.MODE_DEFAULT, tipS));
                addButton(buttons, new MoveAllIconButton(ID_BASE + 5, x, y + 52, false, tipTake));
            }
        }

        // Put-all lives at the top-right of the player inventory section, since
        // that's the section it acts on
        if (ISBConfig.showChestButtons) {
            int invMaxX = Integer.MIN_VALUE;
            int invMinY = Integer.MAX_VALUE;
            for (Slot s : screen.inventorySlots.inventorySlots) {
                if (SortEngine.isPlayerSlot(s, mc.player.inventory)) {
                    invMaxX = Math.max(invMaxX, s.xPos);
                    invMinY = Math.min(invMinY, s.yPos);
                }
            }
            if (invMaxX != Integer.MIN_VALUE) {
                addButton(buttons, new MoveAllIconButton(ID_BASE + 4, left + invMaxX + 6, top + invMinY - 13, true, tipPut));
            }
        }
    }

    private static void addButton(List<GuiButton> buttons, MiniIconButton button) {
        buttons.add(button);
        INJECTED.add(button);
    }

    @SubscribeEvent
    public static void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton() instanceof MiniIconButton) {
            ((MiniIconButton) event.getButton()).onPress();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (INJECTED.isEmpty() || !(event.getGui() instanceof GuiContainer)) {
            return;
        }
        GuiContainer screen = (GuiContainer) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        for (MiniIconButton button : INJECTED) {
            if (button.isMouseOver()) {
                GuiUtils.drawHoveringText(
                        java.util.Collections.singletonList(button.getTooltipText()),
                        event.getMouseX(), event.getMouseY(),
                        screen.width, screen.height, -1, mc.fontRenderer);
            }
        }
    }

    // ------------------------------------------------------------------
    // Middle click sort + click shortcuts
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer)
                || event.getGui() instanceof GuiContainerCreative) {
            return;
        }
        if (!Mouse.getEventButtonState()) {
            return;
        }
        GuiContainer screen = (GuiContainer) event.getGui();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.playerController == null) {
            return;
        }
        int button = Mouse.getEventButton();

        if (button == 2 && ISBConfig.enableMiddleClick) {
            List<Slot> chest = SortEngine.chestSlots(screen.inventorySlots, mc.player.inventory);
            Slot hovered = screen.getSlotUnderMouse();
            boolean overPlayer = hovered != null && SortEngine.isPlayerSlot(hovered, mc.player.inventory);
            if (overPlayer) {
                sendPlayerSort();
                event.setCanceled(true);
            } else if (!chest.isEmpty()) {
                NetworkHandler.CHANNEL.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, false));
                playSortSound();
                event.setCanceled(true);
            }
            return;
        }

        // Click shortcuts, matching the original mappings:
        //   Alt+Click        drop the stack
        //   Space+Click      move everything in that section across
        //   Ctrl+Shift+Click move all stacks of the same item across
        //   Ctrl+Click       move a single item across
        if (button == 0 && ISBConfig.enableShortcuts) {
            Slot hovered = screen.getSlotUnderMouse();
            if (hovered == null || !mc.player.inventory.getItemStack().isEmpty()) {
                return;
            }
            int containerId = screen.inventorySlots.windowId;
            boolean ctrl = GuiScreen.isCtrlKeyDown();
            boolean shift = GuiScreen.isShiftKeyDown();
            boolean fromPlayer = SortEngine.isPlayerSlot(hovered, mc.player.inventory);

            if (GuiScreen.isAltKeyDown() && hovered.getHasStack()) {
                mc.playerController.windowClick(containerId,
                        hovered.slotNumber, 1, ClickType.THROW, mc.player);
                event.setCanceled(true);
            } else if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                for (Slot s : screen.inventorySlots.inventorySlots) {
                    if (s.getHasStack() && SortEngine.isPlayerSlot(s, mc.player.inventory) == fromPlayer) {
                        mc.playerController.windowClick(containerId,
                                s.slotNumber, 0, ClickType.QUICK_MOVE, mc.player);
                    }
                }
                event.setCanceled(true);
            } else if (ctrl && shift && hovered.getHasStack()) {
                ItemStack reference = hovered.getStack().copy();
                for (Slot s : screen.inventorySlots.inventorySlots) {
                    if (s.getHasStack() && SortEngine.isPlayerSlot(s, mc.player.inventory) == fromPlayer
                            && sameItemSameTags(s.getStack(), reference)) {
                        mc.playerController.windowClick(containerId,
                                s.slotNumber, 0, ClickType.QUICK_MOVE, mc.player);
                    }
                }
                event.setCanceled(true);
            } else if (ctrl && hovered.getHasStack()) {
                // Move one item: pick up the stack, right-click one into the
                // other section, put the rest back
                Slot target = null;
                for (Slot s : screen.inventorySlots.inventorySlots) {
                    if (SortEngine.isPlayerSlot(s, mc.player.inventory) == fromPlayer) {
                        continue;
                    }
                    boolean sameWithRoom = s.getHasStack()
                            && sameItemSameTags(s.getStack(), hovered.getStack())
                            && s.getStack().getCount() < s.getStack().getMaxStackSize();
                    if (sameWithRoom || (!s.getHasStack() && s.isItemValid(hovered.getStack()))) {
                        target = s;
                        break;
                    }
                }
                if (target != null) {
                    mc.playerController.windowClick(containerId, hovered.slotNumber, 0, ClickType.PICKUP, mc.player);
                    mc.playerController.windowClick(containerId, target.slotNumber, 1, ClickType.PICKUP, mc.player);
                    mc.playerController.windowClick(containerId, hovered.slotNumber, 0, ClickType.PICKUP, mc.player);
                    event.setCanceled(true);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Sort key (R by default)
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!(event.getGui() instanceof GuiContainer)
                || event.getGui() instanceof GuiContainerCreative) {
            return;
        }
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        if (SORT_KEY.getKeyCode() != 0 && Keyboard.getEventKey() == SORT_KEY.getKeyCode()) {
            sendPlayerSort();
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------
    // Auto-refill: watch the hotbar for stacks that ran out during gameplay
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!keyRegistered) {
            keyRegistered = true;
            ClientRegistry.registerKeyBinding(SORT_KEY);
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
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
        // Gameplay sort key
        while (SORT_KEY.isPressed()) {
            sendPlayerSort();
        }

        boolean watch = mc.currentScreen == null && ISBConfig.enableAutoRefill
                && !mc.gameSettings.keyBindDrop.isKeyDown();
        for (int i = 0; i < 9; i++) {
            ItemStack now = mc.player.inventory.getStackInSlot(i);
            ItemStack before = HOTBAR_SNAPSHOT[i];
            if (watch && before != null && !before.isEmpty() && now.isEmpty()) {
                ResourceLocation id = before.getItem().getRegistryName();
                if (id != null) {
                    NetworkHandler.CHANNEL.sendToServer(
                            new RefillPacket(i, id.toString(), before.getItemDamage()));
                }
            }
            HOTBAR_SNAPSHOT[i] = now.copy();
        }

        // Sort on pickup: the inventory's total item count only grows on pickup
        int total = 0;
        for (int i = 0; i <= 35; i++) {
            total += mc.player.inventory.getStackInSlot(i).getCount();
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
