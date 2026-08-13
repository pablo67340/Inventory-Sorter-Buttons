# Modder API

Everything in `com.invsortbuttons.api` is stable public API and will not break
within a major version. Compile against the mod jar (`compileOnly fg.deobf(...)`
from a flatDir/maven repo) — none of the interfaces require the mod at runtime,
so a soft dependency is enough.

## Zero-effort integration

You may already be done: any `AbstractContainerMenu` with **9 or more
freely-usable non-player slots** automatically gets the sorting buttons and
server-side sorting. "Freely-usable" means the slots accept arbitrary items
(no result/crafting slots). Player inventory slots are detected whether you use
vanilla `Slot`s or Forge's `SlotItemHandler` wrappers.

## Put the buttons inside your GUI — `ISortButtonHost`

Implement on your **screen** to anchor the 10×10 button strip wherever your
art wants it:

```java
public class MyChestScreen extends AbstractContainerScreen<MyChestMenu>
        implements ISortButtonHost {

    @Override
    public SortButtonPlacement getSortButtonPlacement() {
        // A horizontal [z][||][=][...] row, 12px spacing, anchored at the z button
        return SortButtonPlacement.row(this.leftPos + this.imageWidth - 54, this.topPos + 5);

        // Or a vertical [...][=][||][z] column, 13px spacing, anchored at the ... button
        // return SortButtonPlacement.column(this.leftPos - 13, this.topPos + 8);

        // Or keep this screen clean
        // return SortButtonPlacement.hidden();
    }
}
```

Coordinates are absolute screen coordinates — add your own `leftPos`/`topPos`.
Returning `null` (or `SortButtonPlacement.automatic()`) keeps the default
behavior. The buttons respect the player's "Chest buttons" setting; the `...`
settings button is always shown unless you return `hidden()`.

## Control what gets sorted — `ISortableMenu`

Implement on your **menu** (the shared client/server class) when automatic
detection gets your container wrong — ghost slots, filter slots, mixed
machine + storage inventories:

```java
public class MyMachineMenu extends AbstractContainerMenu implements ISortableMenu {

    @Override
    public List<Slot> getSortableSlots() {
        // Only the storage section; filter/ghost slots stay put
        return this.slots.subList(STORAGE_START, STORAGE_END);

        // Or opt out entirely: no buttons, no sorting
        // return List.of();

        // Or return null for automatic detection
    }
}
```

The sorter reads the returned slots' `x`/`y` to reconstruct the visual grid,
so row/column sorting works on any rectangular layout.

## Trigger sorts yourself — `InvSortButtonsApi`

Client side only:

```java
InvSortButtonsApi.sortOpenContainer(InvSortButtonsApi.SORT_DEFAULT); // z
InvSortButtonsApi.sortOpenContainer(InvSortButtonsApi.SORT_COLUMNS); // ||
InvSortButtonsApi.sortOpenContainer(InvSortButtonsApi.SORT_ROWS);    // =
InvSortButtonsApi.sortPlayerInventory();                             // R key
```

These send the same packet the buttons do; the server sorts the menu the
player currently has open (respecting `ISortableMenu`).

## Notes

- Sorting mutates slots directly on the server via `Slot#set` and calls
  `broadcastChanges()`; your menu's `slotsChanged`/`setChanged` hooks fire
  normally.
- The player's hotbar is never touched by inventory sorting.
- Sort order = the player's item tree (category order), so your items can be
  categorized by pack makers via item tags without any code.
