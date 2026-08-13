package com.invsortbuttons.client;

import com.invsortbuttons.ISBConfig;
import com.invsortbuttons.InvSortButtons;
import com.invsortbuttons.client.widget.MiniIconButton;
import com.invsortbuttons.client.widget.SettingsIconButton;
import com.invsortbuttons.client.widget.SortingIconButton;
import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.RefillPacket;
import com.invsortbuttons.network.SortPacket;
import com.invsortbuttons.sort.SortEngine;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
        NetworkHandler.CHANNEL.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, true,
                ISBConfig.AUTO_EQUIP_ARMOR.get()));
        playSortSound();
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
        ConfigFiles.syncToServer(true);
    }

    private static void playSortSound() {
        if (ISBConfig.SOUNDS.get()) {
            Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.audio.SimpleSound.forUI(
                            net.minecraft.util.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    /** 1.16: item + NBT equality (isSameItemSameTags arrived in 1.17). */
    private static boolean sameItemSameTags(ItemStack a, ItemStack b) {
        return ItemStack.isSame(a, b) && ItemStack.tagMatches(a, b);
    }

    // ------------------------------------------------------------------
    // Button injection — the heart of the tribute
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof ContainerScreen)
                || event.getGui() instanceof CreativeScreen) {
            return;
        }
        ContainerScreen<?> screen = (ContainerScreen<?>) event.getGui();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        int xSize = screen.getXSize();

        // Survival inventory: just the "..." settings button, like the original
        if (screen instanceof InventoryScreen) {
            event.addWidget(new SettingsIconButton(left + xSize - 15, top + 5,
                    new TranslationTextComponent("invsortbuttons.tooltip.settings")));
            return;
        }

        List<Slot> chest = SortEngine.chestSlots(screen.getMenu(), mc.player.inventory);
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

        ITextComponent tipSettings = new TranslationTextComponent("invsortbuttons.tooltip.settings");
        ITextComponent tipH = new TranslationTextComponent("invsortbuttons.tooltip.sort_horizontal");
        ITextComponent tipV = new TranslationTextComponent("invsortbuttons.tooltip.sort_vertical");
        ITextComponent tipS = new TranslationTextComponent("invsortbuttons.tooltip.sort_default");

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
            // Left to right: z, ||, =, ...
            if (ISBConfig.CHEST_BUTTONS.get()) {
                event.addWidget(new SortingIconButton(x, y, 's', SortPacket.MODE_DEFAULT, tipS));
                event.addWidget(new SortingIconButton(x + 12, y, 'v', SortPacket.MODE_VERTICAL, tipV));
                event.addWidget(new SortingIconButton(x + 24, y, 'h', SortPacket.MODE_HORIZONTAL, tipH));
            }
            event.addWidget(new SettingsIconButton(x + 36, y, tipSettings));
        } else {
            // Top to bottom: ..., =, ||, z
            event.addWidget(new SettingsIconButton(x, y, tipSettings));
            if (ISBConfig.CHEST_BUTTONS.get()) {
                event.addWidget(new SortingIconButton(x, y + 13, 'h', SortPacket.MODE_HORIZONTAL, tipH));
                event.addWidget(new SortingIconButton(x, y + 26, 'v', SortPacket.MODE_VERTICAL, tipV));
                event.addWidget(new SortingIconButton(x, y + 39, 's', SortPacket.MODE_DEFAULT, tipS));
            }
        }
    }

    @SubscribeEvent
    public static void onScreenRender(GuiScreenEvent.DrawScreenEvent.Post event) {
        for (IGuiEventListener child : event.getGui().children()) {
            if (child instanceof MiniIconButton && ((MiniIconButton) child).isHovered()) {
                event.getGui().renderTooltip(event.getMatrixStack(),
                        ((MiniIconButton) child).getTooltipText(),
                        event.getMouseX(), event.getMouseY());
            }
        }
    }

    // ------------------------------------------------------------------
    // Middle click sort + click shortcuts
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onMouseClick(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (!(event.getGui() instanceof ContainerScreen)
                || event.getGui() instanceof CreativeScreen) {
            return;
        }
        ContainerScreen<?> screen = (ContainerScreen<?>) event.getGui();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) {
            return;
        }

        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && ISBConfig.MIDDLE_CLICK.get()) {
            List<Slot> chest = SortEngine.chestSlots(screen.getMenu(), mc.player.inventory);
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
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT && ISBConfig.SHORTCUTS.get()) {
            Slot hovered = screen.getSlotUnderMouse();
            if (hovered == null || !mc.player.inventory.getCarried().isEmpty()) {
                return;
            }
            int containerId = screen.getMenu().containerId;
            long window = mc.getWindow().getWindow();
            boolean ctrl = net.minecraft.client.gui.screen.Screen.hasControlDown();
            boolean shift = net.minecraft.client.gui.screen.Screen.hasShiftDown();
            boolean fromPlayer = SortEngine.isPlayerSlot(hovered, mc.player.inventory);

            if (net.minecraft.client.gui.screen.Screen.hasAltDown() && hovered.hasItem()) {
                mc.gameMode.handleInventoryMouseClick(containerId,
                        hovered.index, 1, ClickType.THROW, mc.player);
                event.setCanceled(true);
            } else if (InputMappings.isKeyDown(window, GLFW.GLFW_KEY_SPACE)) {
                for (Slot s : screen.getMenu().slots) {
                    if (s.hasItem() && SortEngine.isPlayerSlot(s, mc.player.inventory) == fromPlayer) {
                        mc.gameMode.handleInventoryMouseClick(containerId,
                                s.index, 0, ClickType.QUICK_MOVE, mc.player);
                    }
                }
                event.setCanceled(true);
            } else if (ctrl && shift && hovered.hasItem()) {
                ItemStack reference = hovered.getItem().copy();
                for (Slot s : screen.getMenu().slots) {
                    if (s.hasItem() && SortEngine.isPlayerSlot(s, mc.player.inventory) == fromPlayer
                            && sameItemSameTags(s.getItem(), reference)) {
                        mc.gameMode.handleInventoryMouseClick(containerId,
                                s.index, 0, ClickType.QUICK_MOVE, mc.player);
                    }
                }
                event.setCanceled(true);
            } else if (ctrl && hovered.hasItem()) {
                // Move one item: pick up the stack, right-click one into the
                // other section, put the rest back
                Slot target = null;
                for (Slot s : screen.getMenu().slots) {
                    if (SortEngine.isPlayerSlot(s, mc.player.inventory) == fromPlayer) {
                        continue;
                    }
                    boolean sameWithRoom = s.hasItem()
                            && sameItemSameTags(s.getItem(), hovered.getItem())
                            && s.getItem().getCount() < s.getItem().getMaxStackSize();
                    if (sameWithRoom || (!s.hasItem() && s.mayPlace(hovered.getItem()))) {
                        target = s;
                        break;
                    }
                }
                if (target != null) {
                    mc.gameMode.handleInventoryMouseClick(containerId, hovered.index, 0, ClickType.PICKUP, mc.player);
                    mc.gameMode.handleInventoryMouseClick(containerId, target.index, 1, ClickType.PICKUP, mc.player);
                    mc.gameMode.handleInventoryMouseClick(containerId, hovered.index, 0, ClickType.PICKUP, mc.player);
                    event.setCanceled(true);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // Sort key (R by default)
    // ------------------------------------------------------------------

    @SubscribeEvent
    public static void onScreenKey(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        if (!(event.getGui() instanceof ContainerScreen)
                || event.getGui() instanceof CreativeScreen) {
            return;
        }
        if (event.getGui().getFocused() instanceof TextFieldWidget) {
            return;
        }
        if (ClientModEvents.SORT_KEY.getKey().getValue() == event.getKeyCode()) {
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
            ItemStack now = mc.player.inventory.getItem(i);
            ItemStack before = HOTBAR_SNAPSHOT[i];
            if (watch && before != null && !before.isEmpty() && now.isEmpty()) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(before.getItem());
                if (id != null) {
                    NetworkHandler.CHANNEL.sendToServer(new RefillPacket(i, id.toString()));
                }
            }
            HOTBAR_SNAPSHOT[i] = now.copy();
        }

        // Sort on pickup: the inventory's total item count only grows on pickup
        int total = 0;
        for (int i = 0; i <= 35; i++) {
            total += mc.player.inventory.getItem(i).getCount();
        }
        if (pickupSortCooldown > 0) {
            pickupSortCooldown--;
        }
        if (ISBConfig.SORT_ON_PICKUP.get() && mc.screen == null
                && lastInventoryTotal >= 0 && total > lastInventoryTotal && pickupSortCooldown == 0) {
            pickupSortCooldown = 10;
            NetworkHandler.CHANNEL.sendToServer(new SortPacket(SortPacket.MODE_DEFAULT, true,
                    ISBConfig.AUTO_EQUIP_ARMOR.get()));
        }
        lastInventoryTotal = total;
    }
}
