# Inventory Sorter Buttons 1.1.1

## Fixed

- **Put All button overlapping slots in modded GUIs.** In GUIs whose slots
  extend right up to the player inventory (AE2-style terminals, big storage
  mods, etc.) the Put All button could render on top of a slot. In modded
  (sidebar) layouts the Put All button now sits in the button column below
  Take All; in vanilla GUIs it stays at the top-right of your inventory
  section.

Available for Minecraft 1.7.10, 1.12.2, 1.16.5, 1.19.2, 1.20.6, 1.21.11,
26.1.2 and 26.2 (Forge, client + server).

---

# Inventory Sorter Buttons 1.1.0

## New: Move All buttons (Inventory Profiles Next style)

- **Take All (down arrow)**, next to the sort buttons: empties the container
  into your main inventory. It never fills your hotbar - if your main
  inventory runs out of room, the remainder stays in the container.
- **Put All (up arrow)**, at the top-right of your inventory section: moves
  your main inventory into the container. Your hotbar is left alone by
  default; hold Shift while clicking to include it.
- Both buttons work entirely client-side (like the Space+Click shortcut), so
  they work on servers running the older mod version too.
- Buttons respect the existing "Chest buttons" toggle, and the "?" help
  screen documents them.

Available for Minecraft 1.7.10, 1.12.2, 1.16.5, 1.19.2, 1.20.6, 1.21.11,
26.1.2 and 26.2 (Forge, client + server).
