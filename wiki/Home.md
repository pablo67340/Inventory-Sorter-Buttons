# Inventory Sorter Buttons Wiki

A faithful revival of **Inventory Tweaks 1.41b** for Forge 1.19.2: the classic
chest sorting buttons (`z` `||` `=` `...`), settings screens, rules file, and
item tree — working on containers from **any mod**, with server-authoritative
sorting.

## Pages

- **[Configuration](Configuration)** — the settings screens, the rules file,
  and the item tree file, with the full modern keyword syntax.
- **[Modder API](Modder-API)** — put the buttons inside your own GUI, define
  your menu's sortable slots, or trigger sorts from your own code.

## Quick facts

- Minecraft 1.19.2, Forge 43+, install on both client and server.
- Any menu with 9+ freely-usable non-player slots gets buttons automatically.
- Vanilla GUIs: button row in the top-right. Modded GUIs: button column off
  the left edge.
- The hotbar is never touched by sorting.
- Config files live in `config/`: `InvSortButtonsRules.txt`,
  `InvSortButtonsTree.txt`, `InvSortButtonsShortcuts.txt` — delete one and the
  default regenerates. Edits reload on the next sort.

## Credits

A tribute to **Inventory Tweaks** by Jimeo Wan (Marwane Kalam-Alami), released
under the MIT license — the same license this recreation uses.
