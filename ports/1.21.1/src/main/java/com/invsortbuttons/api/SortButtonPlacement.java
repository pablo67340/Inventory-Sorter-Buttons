package com.invsortbuttons.api;

/**
 * Where the sorting button strip goes on a screen. Create one with the static
 * factories and return it from {@link ISortButtonHost#getSortButtonPlacement()}.
 *
 * <p>Coordinates are absolute screen coordinates (add your own leftPos/topPos).
 * Buttons are 10x10. A ROW lays them out left-to-right as [z][||][=][...]
 * with 12px spacing, anchored at the z button — the classic vanilla-chest look.
 * A COLUMN stacks them top-to-bottom as [...][=][||][z] with 13px spacing,
 * anchored at the ... button — the classic "off the left edge" look.
 */
public record SortButtonPlacement(Type type, int x, int y) {
    public enum Type {
        /** Let the mod decide (vanilla-style row, or a column off the left edge). */
        AUTOMATIC,
        /** Horizontal strip anchored at (x, y). */
        ROW,
        /** Vertical strip anchored at (x, y). */
        COLUMN,
        /** No buttons on this screen at all. */
        HIDDEN
    }

    public static SortButtonPlacement automatic() {
        return new SortButtonPlacement(Type.AUTOMATIC, 0, 0);
    }

    public static SortButtonPlacement row(int x, int y) {
        return new SortButtonPlacement(Type.ROW, x, y);
    }

    public static SortButtonPlacement column(int x, int y) {
        return new SortButtonPlacement(Type.COLUMN, x, y);
    }

    public static SortButtonPlacement hidden() {
        return new SortButtonPlacement(Type.HIDDEN, 0, 0);
    }
}
