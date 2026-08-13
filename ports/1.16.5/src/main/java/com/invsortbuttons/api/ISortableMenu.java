package com.invsortbuttons.api;

import net.minecraft.inventory.container.Slot;
import javax.annotation.Nullable;

import java.util.List;

/**
 * Implement this on your {@code Container} to control which slots
 * the sorter treats as the "chest section". Useful when auto-detection gets it
 * wrong: menus with ghost/filter slots, mixed storage + machine slots, etc.
 *
 * <p>Checked on both sides — the client uses it for button placement, the
 * server for the actual sort — so implement it on the shared menu class.
 */
public interface ISortableMenu {
    /**
     * The slots to sort, in visual order preference (the sorter re-reads their
     * x/y for grid layout). Return:
     * <ul>
     *   <li>{@code null} — use automatic detection (default behavior)</li>
     *   <li>an empty list — this menu is not sortable; no buttons, no sorting</li>
     *   <li>a list of slots — exactly these get sorted</li>
     * </ul>
     */
    @Nullable
    List<Slot> getSortableSlots();
}
