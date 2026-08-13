package com.invsortbuttons.sort;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Port of InvTweaks' sorting behavior (InvTweaksHandlerSorting, MIT).
 *
 * Modes match the original buttons:
 *   0 "s" default    — merge + sort, fill the grid row by row
 *   1 "v" vertical   — each distinct item gets its own column band
 *   2 "h" horizontal — each distinct item gets its own row band
 */
public final class SortEngine {
    /** Per-player configs, synced from each client's rules/tree files. */
    private static final Map<java.util.UUID, PlayerSortConfig> PLAYER_CONFIGS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private SortEngine() {
    }

    public static void setPlayerConfig(java.util.UUID id, PlayerSortConfig config) {
        PLAYER_CONFIGS.put(id, config);
    }

    public static void clearPlayerConfig(java.util.UUID id) {
        PLAYER_CONFIGS.remove(id);
    }

    private static PlayerSortConfig configFor(ServerPlayer player) {
        return PLAYER_CONFIGS.getOrDefault(player.getUUID(), PlayerSortConfig.getDefault());
    }

    /**
     * The "chest section" of a menu: every slot backed by something other than
     * the player inventory that freely accepts items. Empty list = this menu
     * has no sortable container (furnaces, crafting tables, villagers...).
     */
    public static List<Slot> chestSlots(AbstractContainerMenu menu, Inventory playerInv) {
        // API hook: the menu can define (or veto) its own sortable section
        if (menu instanceof com.invsortbuttons.api.ISortableMenu sortable) {
            List<Slot> custom = sortable.getSortableSlots();
            if (custom != null) {
                return custom.isEmpty() ? List.of() : new ArrayList<>(custom);
            }
        }
        List<Slot> out = new ArrayList<>();
        for (Slot s : menu.slots) {
            if (isPlayerSlot(s, playerInv)) {
                continue;
            }
            if (s instanceof ResultSlot || s.container instanceof ResultContainer
                    || s.container instanceof CraftingContainer) {
                return List.of();
            }
            out.add(s);
        }
        if (out.size() < 9) {
            return List.of();
        }
        ItemStack probe = new ItemStack(Items.STONE);
        for (Slot s : out) {
            if (!s.mayPlace(probe)) {
                return List.of();
            }
        }
        return out;
    }

    /** True if this slot shows part of the player's inventory (vanilla or item-handler wrapped). */
    public static boolean isPlayerSlot(Slot s, Inventory playerInv) {
        if (s.container == playerInv) {
            return true;
        }
        if (s instanceof net.minecraftforge.items.SlotItemHandler sih) {
            net.minecraftforge.items.IItemHandler h = sih.getItemHandler();
            if (h instanceof net.minecraftforge.items.wrapper.PlayerInvWrapper
                    || h instanceof net.minecraftforge.items.wrapper.PlayerMainInvWrapper
                    || h instanceof net.minecraftforge.items.wrapper.PlayerArmorInvWrapper) {
                return true;
            }
            if (h instanceof net.minecraftforge.items.wrapper.InvWrapper w && w.getInv() == playerInv) {
                return true;
            }
        }
        return false;
    }

    public static void sortOpenContainer(ServerPlayer player, int mode) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu == player.inventoryMenu) {
            return;
        }
        List<Slot> section = chestSlots(menu, player.getInventory());
        if (section.isEmpty()) {
            return;
        }
        // Visual order: top-to-bottom, left-to-right
        section.sort(Comparator.comparingInt((Slot s) -> s.y).thenComparingInt(s -> s.x));

        List<ItemStack> merged = mergeAndSort(section.stream().map(Slot::getItem).toList(),
                configFor(player).tree);

        int[] gridDims = gridDimensions(section);
        int columns = gridDims[0];
        int rows = gridDims[1];
        boolean rectangular = columns > 0 && columns * rows == section.size();

        List<ItemStack> layout = placeStacks(merged, mode, rectangular, columns, rows, section.size());

        for (int i = 0; i < section.size(); i++) {
            section.get(i).set(i < layout.size() ? layout.get(i) : ItemStack.EMPTY);
        }
        menu.broadcastChanges();
    }

    /**
     * Sorts the 27 main inventory slots (hotbar untouched), applying the
     * player's rules file: FROZEN/LOCKED slots stay put, then rules place
     * matching items into their preferred slots by priority, then everything
     * else fills the gaps in tree order — the original's inventory algorithm.
     *
     * Grid mapping: rules rows a-c are inventory slots 9-35 (grid index =
     * slot - 9); row d is the hotbar, which this recreation never moves.
     */
    public static void sortPlayerInventory(ServerPlayer player, boolean equipArmor) {
        Inventory inv = player.getInventory();
        PlayerSortConfig config = configFor(player);
        if (equipArmor) {
            autoEquipArmor(player);
        }

        // Collect from non-frozen main-inventory slots
        List<ItemStack> pool = new ArrayList<>();
        for (int g = 0; g < 27; g++) {
            if (!config.rules.isFrozen(g)) {
                pool.add(inv.getItem(g + 9));
            }
        }
        List<ItemStack> merged = mergeAndSort(pool, config.tree);

        ItemStack[] result = new ItemStack[27];
        boolean[] taken = new boolean[27];
        for (int g = 0; g < 27; g++) {
            if (config.rules.isFrozen(g)) {
                taken[g] = true;
            }
        }
        boolean[] placed = new boolean[merged.size()];

        // Rules are pre-sorted by priority, most specific first
        for (RuleSet.Rule rule : config.rules.getRules()) {
            for (int m = 0; m < merged.size(); m++) {
                if (placed[m] || !config.tree.matches(merged.get(m), rule.keyword())) {
                    continue;
                }
                for (int slot : rule.preferredSlots()) {
                    if (slot >= 27) {
                        continue; // row d = hotbar, never touched
                    }
                    if (!taken[slot]) {
                        result[slot] = merged.get(m);
                        taken[slot] = true;
                        placed[m] = true;
                        break;
                    }
                }
            }
        }

        // Everything unmatched fills the remaining slots in order
        int g = 0;
        for (int m = 0; m < merged.size(); m++) {
            if (placed[m]) {
                continue;
            }
            while (g < 27 && taken[g]) {
                g++;
            }
            if (g >= 27) {
                break;
            }
            result[g] = merged.get(m);
            taken[g] = true;
        }

        for (int i = 0; i < 27; i++) {
            if (!config.rules.isFrozen(i)) {
                inv.setItem(i + 9, result[i] == null ? ItemStack.EMPTY : result[i]);
            }
        }
        player.inventoryMenu.broadcastChanges();
        if (player.containerMenu != null) {
            player.containerMenu.broadcastChanges();
        }
    }

    /** InvTweaks' "auto-equip armor": swap in higher-defense (or less worn) pieces while sorting. */
    private static void autoEquipArmor(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i <= 35; i++) {
            ItemStack candidate = inv.getItem(i);
            if (candidate.getCount() != 1
                    || !(candidate.getItem() instanceof net.minecraft.world.item.ArmorItem armor)) {
                continue;
            }
            int armorSlot = 36 + armor.getSlot().getIndex();
            ItemStack worn = inv.getItem(armorSlot);
            boolean better;
            if (worn.isEmpty()) {
                better = true;
            } else if (worn.getItem() instanceof net.minecraft.world.item.ArmorItem wornArmor) {
                better = wornArmor.getDefense() < armor.getDefense()
                        || (wornArmor.getDefense() == armor.getDefense()
                        && worn.getDamageValue() > candidate.getDamageValue());
            } else {
                better = false;
            }
            if (better && !net.minecraft.world.item.enchantment.EnchantmentHelper.hasBindingCurse(worn)) {
                inv.setItem(armorSlot, candidate);
                inv.setItem(i, worn);
            }
        }
    }

    // ------------------------------------------------------------------
    // Core: merge stacks, order them, lay them out per algorithm
    // ------------------------------------------------------------------

    /** Merge partial stacks of the same item and sort the result in tree order. */
    public static List<ItemStack> mergeAndSort(List<ItemStack> stacks, ItemTree tree) {
        Map<String, List<ItemStack>> groups = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            String key = groupKey(stack);
            List<ItemStack> group = groups.computeIfAbsent(key, k -> new ArrayList<>());
            ItemStack remaining = stack.copy();
            // Top off existing partial stacks first
            for (ItemStack existing : group) {
                if (existing.getCount() >= existing.getMaxStackSize()) {
                    continue;
                }
                int take = Math.min(existing.getMaxStackSize() - existing.getCount(), remaining.getCount());
                existing.grow(take);
                remaining.shrink(take);
                if (remaining.isEmpty()) {
                    break;
                }
            }
            if (!remaining.isEmpty()) {
                group.add(remaining);
            }
        }

        List<List<ItemStack>> ordered = new ArrayList<>(groups.values());
        ordered.sort((a, b) -> {
            int orderA = tree.getItemOrder(a.get(0));
            int orderB = tree.getItemOrder(b.get(0));
            if (orderA != orderB) {
                return Integer.compare(orderA, orderB);
            }
            return compareGroups(a, b);
        });
        List<ItemStack> out = new ArrayList<>();
        for (List<ItemStack> group : ordered) {
            group.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
            out.addAll(group);
        }
        return out;
    }

    private static String groupKey(ItemStack stack) {
        return stack.getItem() + "|" + (stack.hasTag() ? String.valueOf(stack.getTag()) : "");
    }

    /**
     * Ordering, like the original's item-ID fallback: numeric item id groups
     * related vanilla items together (the old InvTweaks tree used ID ranges too),
     * then damage, then NBT.
     */
    private static int compareGroups(List<ItemStack> a, List<ItemStack> b) {
        ItemStack sa = a.get(0);
        ItemStack sb = b.get(0);
        int ia = Item.getId(sa.getItem());
        int ib = Item.getId(sb.getItem());
        if (ia != ib) {
            return Integer.compare(ia, ib);
        }
        if (sa.getDamageValue() != sb.getDamageValue()) {
            return Integer.compare(sa.getDamageValue(), sb.getDamageValue());
        }
        return String.valueOf(sa.getTag()).compareTo(String.valueOf(sb.getTag()));
    }

    /** [columns, rows]; columns = 0 if the section is not a clean rectangle. */
    private static int[] gridDimensions(List<Slot> visualOrder) {
        TreeMap<Integer, Integer> rows = new TreeMap<>();
        for (Slot s : visualOrder) {
            rows.merge(s.y, 1, Integer::sum);
        }
        int columns = -1;
        for (int len : rows.values()) {
            if (columns == -1) {
                columns = len;
            } else if (columns != len) {
                return new int[]{0, 0};
            }
        }
        return new int[]{columns, rows.size()};
    }

    /**
     * Lay merged stacks into slot indices (visual row-major order).
     *
     * Vertical/horizontal modes port the original's computeLineSortingRules:
     * every distinct item gets a rectangular band. Horizontal bands are 1 row
     * tall and {@code columns / ceil(distinct / rows)} wide (so a 9x3 chest
     * with 6 items gets 4-wide bands, two item types per row); vertical is the
     * mirror image. Bands grow to fit larger groups, and whatever doesn't fit
     * spills into free slots from the bottom, like the original's catch-all rule.
     */
    private static List<ItemStack> placeStacks(List<ItemStack> merged, int mode,
                                               boolean rectangular, int columns, int rows, int slotCount) {
        ItemStack[] out = new ItemStack[slotCount];
        java.util.Arrays.fill(out, ItemStack.EMPTY);

        if (!rectangular || mode == 0) {
            for (int i = 0; i < merged.size() && i < slotCount; i++) {
                out[i] = merged.get(i);
            }
            return List.of(out);
        }

        // Group boundaries in the merged list (consecutive stacks of one item)
        List<List<ItemStack>> groups = new ArrayList<>();
        List<ItemStack> current = null;
        String lastKey = null;
        for (ItemStack stack : merged) {
            String key = groupKey(stack);
            if (!key.equals(lastKey)) {
                current = new ArrayList<>();
                groups.add(current);
                lastKey = key;
            }
            current.add(stack);
        }

        boolean horizontal = mode == 2;

        // Original placed groups too big for one lane first
        int laneCapacity = horizontal ? columns : rows;
        List<List<ItemStack>> ordered = new ArrayList<>();
        for (List<ItemStack> g : groups) {
            if (g.size() > laneCapacity) {
                ordered.add(g);
            }
        }
        for (List<ItemStack> g : groups) {
            if (g.size() <= laneCapacity) {
                ordered.add(g);
            }
        }

        int distinct = ordered.size();
        int bandW;
        int bandH;
        if (horizontal) {
            bandH = 1;
            bandW = Math.max(1, columns / ((distinct + rows - 1) / rows));
        } else {
            bandW = 1;
            bandH = Math.max(1, rows / ((distinct + columns - 1) / columns));
        }

        ItemStack[][] grid = new ItemStack[rows][columns];
        List<ItemStack> overflow = new ArrayList<>();
        int row = 0;
        int col = 0;
        boolean full = false;

        for (List<ItemStack> group : ordered) {
            if (full) {
                overflow.addAll(group);
                continue;
            }
            int w = bandW;
            int h = bandH;
            // Grow the band until the group fits (or the container edge stops us)
            while (group.size() > w * h) {
                if (horizontal) {
                    if (col + w < columns) {
                        w = columns - col;
                    } else if (row + h < rows) {
                        h++;
                    } else {
                        break;
                    }
                } else {
                    if (row + h < rows) {
                        h = rows - row;
                    } else if (col + w < columns) {
                        w++;
                    } else {
                        break;
                    }
                }
            }
            // Original: absorb a would-be single-slot leftover lane
            if (horizontal && col + w == columns - 1) {
                w++;
            } else if (!horizontal && row + h == rows - 1) {
                h++;
            }
            w = Math.min(w, columns - col);
            h = Math.min(h, rows - row);

            int idx = 0;
            for (ItemStack stack : group) {
                boolean placed = false;
                while (idx < w * h) {
                    int r = horizontal ? idx / w : idx % h;
                    int c = horizontal ? idx % w : idx / h;
                    idx++;
                    if (grid[row + r][col + c] == null) {
                        grid[row + r][col + c] = stack;
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    overflow.add(stack);
                }
            }

            if (horizontal) {
                if (col + w + bandW <= columns) {
                    col += w;
                } else {
                    col = 0;
                    row += h;
                }
            } else {
                if (row + h + bandH <= rows) {
                    row += h;
                } else {
                    row = 0;
                    col += w;
                }
            }
            if (row >= rows || col >= columns) {
                full = true;
            }
        }

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                if (grid[r][c] != null) {
                    out[r * columns + c] = grid[r][c];
                }
            }
        }
        // Catch-all region: fill leftovers from the bottom row upward
        int oi = 0;
        for (int r = rows - 1; r >= 0 && oi < overflow.size(); r--) {
            for (int c = 0; c < columns && oi < overflow.size(); c++) {
                int i = r * columns + c;
                if (out[i].isEmpty()) {
                    out[i] = overflow.get(oi++);
                }
            }
        }
        return List.of(out);
    }
}
