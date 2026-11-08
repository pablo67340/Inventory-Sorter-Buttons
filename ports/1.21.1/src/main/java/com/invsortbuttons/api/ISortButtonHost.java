package com.invsortbuttons.api;

/**
 * Implement this on your {@code AbstractContainerScreen} to control where
 * Inventory Sorter Buttons places its button strip — put the buttons right
 * inside your GUI art, or hide them on specific screens.
 *
 * <p>No hard dependency needed at runtime: if this mod isn't installed, an
 * unused interface on your screen does nothing.
 */
public interface ISortButtonHost {
    /**
     * Called when your screen initializes. Return a placement from
     * {@link SortButtonPlacement}'s factories; {@code null} behaves like
     * {@link SortButtonPlacement#automatic()}.
     */
    SortButtonPlacement getSortButtonPlacement();
}
