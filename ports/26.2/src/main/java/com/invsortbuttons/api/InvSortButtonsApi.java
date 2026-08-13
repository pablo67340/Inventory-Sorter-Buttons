package com.invsortbuttons.api;

import com.invsortbuttons.network.NetworkHandler;
import com.invsortbuttons.network.SortPacket;

/**
 * Public entry point for mods integrating with Inventory Sorter Buttons.
 *
 * <p>Stability: everything in {@code com.invsortbuttons.api} will not break
 * within a major version.
 *
 * <p>The three integration levels:
 * <ol>
 *   <li><b>Do nothing.</b> Any menu with 9+ freely-usable non-player slots
 *       gets buttons and sorting automatically.</li>
 *   <li><b>Implement {@link ISortButtonHost} on your screen</b> to place the
 *       buttons exactly where you want them (or hide them).</li>
 *   <li><b>Implement {@link ISortableMenu} on your menu</b> to define exactly
 *       which slots are sortable, or opt out entirely.</li>
 * </ol>
 */
public final class InvSortButtonsApi {
    public static final String MOD_ID = "invsortbuttons";

    /** Sort modes, matching the z / || / = buttons. */
    public static final int SORT_DEFAULT = SortPacket.MODE_DEFAULT;
    public static final int SORT_COLUMNS = SortPacket.MODE_VERTICAL;
    public static final int SORT_ROWS = SortPacket.MODE_HORIZONTAL;

    private InvSortButtonsApi() {
    }

    /**
     * Ask the server to sort the container the player currently has open,
     * exactly as if a sort button was clicked. Client side only.
     *
     * @param mode one of {@link #SORT_DEFAULT}, {@link #SORT_COLUMNS}, {@link #SORT_ROWS}
     */
    public static void sortOpenContainer(int mode) {
        NetworkHandler.sendToServer(new SortPacket(mode, false));
    }

    /** Sort the player's main inventory (hotbar untouched). Client side only. */
    public static void sortPlayerInventory() {
        NetworkHandler.sendToServer(new SortPacket(SORT_DEFAULT, true, false));
    }
}
