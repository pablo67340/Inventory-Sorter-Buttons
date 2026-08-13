# Inventory Sorter Buttons

The classic **Inventory Tweaks** chest sorting buttons, revived for
**Minecraft 1.19.2 / Forge 43+** — and working on containers from **any mod**.

A tribute to Inventory Tweaks 1.41b (the Tekkit Classic era): same 10x10 buttons,
same glyphs, same settings screen, rebuilt on modern Forge with server-authoritative
sorting.

## Features

- **The buttons** — `z` (sort), `||` (sort in columns), `=` (sort in rows), and
  `...` (settings) on every chest-like GUI:
  - Vanilla GUIs get the classic horizontal row in the top-right corner
  - Modded GUIs (Iron Chests, ProjectE alchemy bags, anything) get a vertical
    column off the left edge
- **Middle-click** a container to sort it; hover your own inventory to sort that instead
- **R** sorts your main inventory (rebindable), in and out of GUIs
- **Auto-refill** — when a hotbar stack runs out or a tool breaks, it refills
  from your inventory
- **Shortcuts** — Alt+Click drops a whole stack, Space+Click moves a stack to the
  other inventory section
- **Settings screen** — the original "Inventory and chests settings" layout, with
  working toggles for all of the above, plus the "More options..." page
  (auto-equip armor, sort on pickup, sounds)
- **Rules & item tree files** — the original config files, modernized:
  `config/InvSortButtonsRules.txt` (grid rules like `A food`, now with registry
  names and `#tags`) and `config/InvSortButtonsTree.txt` (the XML item tree,
  with `id`/`tag`/`class` matchers so modded items categorize automatically).
  Edits reload on the next sort; on servers each player's rules sync from
  their own client.
- **Modder API** — `com.invsortbuttons.api`: place the buttons inside your own
  GUI (`ISortButtonHost`), define your menu's sortable slots (`ISortableMenu`),
  or trigger sorts (`InvSortButtonsApi`). See `wiki/Modder-API.md`.

## How it works

Button clicks send a small packet; the server reorders the container directly
through the menu's own slots, so it is compatible with any mod's containers and
safe on dedicated servers. GUIs are detected generically: any menu with 9+
freely-usable non-player slots gets buttons.

## Requirements

- Minecraft 1.19.2, Forge 43+
- Install on both client and server

## Build

Requires JDK 17.

```bat
gradlew.bat runClient
```

## Credits

- **Jimeo Wan (Marwane Kalam-Alami)** — creator of **Inventory Tweaks** (MIT
  license). The buttons, glyphs, layout offsets, sorting modes, and settings
  screen are recreated from Inventory Tweaks 1.41b. Thank you for open-sourcing
  it — this recreation exists because the MIT license made it possible.
- **CFR** decompiler — used to study the original 1.41b behavior for accuracy.

## License

MIT, same as the original Inventory Tweaks.
