package com.invsortbuttons.sort;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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
 *
 * 1.7.10 note: empty stacks are {@code null} here, not {@code ItemStack.EMPTY}.
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

    private static PlayerSortConfig configFor(EntityPlayerMP player) {
        PlayerSortConfig config = PLAYER_CONFIGS.get(player.getUniqueID());
        return config != null ? config : PlayerSortConfig.getDefault();
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.stackSize <= 0;
    }

    /** All slots of a container, typed (1.7.10 uses a raw List). */
    @SuppressWarnings("unchecked")
    public static List<Slot> allSlots(Container menu) {
        return (List<Slot>) menu.inventorySlots;
    }

    /**
     * The "chest section" of a container: every slot backed by something other
     * than the player inventory that freely accepts items. Empty list = this
     * container has no sortable section (furnaces, crafting tables, villagers...).
     */
    public static List<Slot> chestSlots(Container menu, InventoryPlayer playerInv) {
        // API hook: the container can define (or veto) its own sortable section
        if (menu instanceof com.invsortbuttons.api.ISortableMenu) {
            List<Slot> custom = ((com.invsortbuttons.api.ISortableMenu) menu).getSortableSlots();
            if (custom != null) {
                return custom.isEmpty() ? java.util.Collections.<Slot>emptyList() : new ArrayList<>(custom);
            }
        }
        List<Slot> out = new ArrayList<>();
        for (Slot s : allSlots(menu)) {
            if (isPlayerSlot(s, playerInv)) {
                continue;
            }
            if (s instanceof SlotCrafting || s.inventory instanceof InventoryCraftResult
                    || s.inventory instanceof InventoryCrafting) {
                return java.util.Collections.emptyList();
            }
            out.add(s);
        }
        if (out.size() < 9) {
            return java.util.Collections.emptyList();
        }
        ItemStack probe = new ItemStack(Blocks.stone);
        for (Slot s : out) {
            if (!s.isItemValid(probe)) {
                return java.util.Collections.emptyList();
            }
        }
        return out;
    }

    /** True if this slot shows part of the player's inventory. */
    public static boolean isPlayerSlot(Slot s, InventoryPlayer playerInv) {
        return s.inventory == playerInv;
    }

    public static void sortOpenContainer(EntityPlayerMP player, int mode) {
        Container menu = player.openContainer;
        if (menu == null || menu == player.inventoryContainer) {
            return;
        }
        List<Slot> section = chestSlots(menu, player.inventory);
        if (section.isEmpty()) {
            return;
        }
        // Visual order: top-to-bottom, left-to-right
        section.sort(Comparator.comparingInt((Slot s) -> s.yDisplayPosition)
                .thenComparingInt(s -> s.xDisplayPosition));

        List<ItemStack> pool = new ArrayList<>();
        for (Slot s : section) {
            pool.add(s.getStack());
        }
        List<ItemStack> merged = mergeAndSort(pool, configFor(player).tree);

        int[] gridDims = gridDimensions(section);
        int columns = gridDims[0];
        int rows = gridDims[1];
        boolean rectangular = columns > 0 && columns * rows == section.size();

        List<ItemStack> layout = placeStacks(merged, mode, rectangular, columns, rows, section.size());

        for (int i = 0; i < section.size(); i++) {
            section.get(i).putStack(i < layout.size() ? layout.get(i) : null);
        }
        menu.detectAndSendChanges();
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
    public static void sortPlayerInventory(EntityPlayerMP player, boolean equipArmor) {
        InventoryPlayer inv = player.inventory;
        PlayerSortConfig config = configFor(player);
        if (equipArmor) {
            autoEquipArmor(player);
        }

        // Collect from non-frozen main-inventory slots
        List<ItemStack> pool = new ArrayList<>();
        for (int g = 0; g < 27; g++) {
            if (!config.rules.isFrozen(g)) {
                pool.add(inv.getStackInSlot(g + 9));
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
                inv.setInventorySlotContents(i + 9, result[i]);
            }
        }
        player.inventoryContainer.detectAndSendChanges();
        if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges();
        }
    }

    /** InvTweaks' "auto-equip armor": swap in higher-defense (or less worn) pieces while sorting. */
    private static void autoEquipArmor(EntityPlayerMP player) {
        InventoryPlayer inv = player.inventory;
        for (int i = 0; i <= 35; i++) {
            ItemStack candidate = inv.getStackInSlot(i);
            if (candidate == null || candidate.stackSize != 1
                    || !(candidate.getItem() instanceof net.minecraft.item.ItemArmor)) {
                continue;
            }
            net.minecraft.item.ItemArmor armor = (net.minecraft.item.ItemArmor) candidate.getItem();
            // 1.7.10: armorType 0=helmet..3=boots; inventory slot 36=boots..39=helmet
            int armorSlot = 36 + (3 - armor.armorType);
            ItemStack worn = inv.getStackInSlot(armorSlot);
            boolean better;
            if (worn == null) {
                better = true;
            } else if (worn.getItem() instanceof net.minecraft.item.ItemArmor) {
                net.minecraft.item.ItemArmor wornArmor = (net.minecraft.item.ItemArmor) worn.getItem();
                better = wornArmor.damageReduceAmount < armor.damageReduceAmount
                        || (wornArmor.damageReduceAmount == armor.damageReduceAmount
                        && worn.getItemDamage() > candidate.getItemDamage());
            } else {
                better = false;
            }
            if (better) { // no binding curses in 1.7.10
                inv.setInventorySlotContents(armorSlot, candidate);
                inv.setInventorySlotContents(i, worn);
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
            if (isEmpty(stack)) {
                continue;
            }
            String key = groupKey(stack);
            List<ItemStack> group = groups.computeIfAbsent(key, k -> new ArrayList<>());
            ItemStack remaining = stack.copy();
            // Top off existing partial stacks first
            for (ItemStack existing : group) {
                if (existing.stackSize >= existing.getMaxStackSize()) {
                    continue;
                }
                int take = Math.min(existing.getMaxStackSize() - existing.stackSize, remaining.stackSize);
                existing.stackSize += take;
                remaining.stackSize -= take;
                if (remaining.stackSize <= 0) {
                    break;
                }
            }
            if (remaining.stackSize > 0) {
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
            group.sort((a, b) -> Integer.compare(b.stackSize, a.stackSize));
            out.addAll(group);
        }
        return out;
    }

    private static String groupKey(ItemStack stack) {
        return stack.getItem() + "|" + stack.getItemDamage() + "|"
                + (stack.hasTagCompound() ? String.valueOf(stack.getTagCompound()) : "");
    }

    /**
     * Ordering, like the original's item-ID fallback: numeric item id groups
     * related vanilla items together (the old InvTweaks tree used ID ranges too),
     * then damage, then NBT.
     */
    private static int compareGroups(List<ItemStack> a, List<ItemStack> b) {
        ItemStack sa = a.get(0);
        ItemStack sb = b.get(0);
        int ia = Item.getIdFromItem(sa.getItem());
        int ib = Item.getIdFromItem(sb.getItem());
        if (ia != ib) {
            return Integer.compare(ia, ib);
        }
        if (sa.getItemDamage() != sb.getItemDamage()) {
            return Integer.compare(sa.getItemDamage(), sb.getItemDamage());
        }
        return String.valueOf(sa.getTagCompound()).compareTo(String.valueOf(sb.getTagCompound()));
    }

    /** [columns, rows]; columns = 0 if the section is not a clean rectangle. */
    private static int[] gridDimensions(List<Slot> visualOrder) {
        TreeMap<Integer, Integer> rows = new TreeMap<>();
        for (Slot s : visualOrder) {
            rows.merge(s.yDisplayPosition, 1, Integer::sum);
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

        if (!rectangular || mode == 0) {
            for (int i = 0; i < merged.size() && i < slotCount; i++) {
                out[i] = merged.get(i);
            }
            return java.util.Arrays.asList(out);
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
                if (out[i] == null) {
                    out[i] = overflow.get(oi++);
                }
            }
        }
        return java.util.Arrays.asList(out);
    }
}
