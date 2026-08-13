# Configuration

Everything can be configured in game: click the `...` button on any chest (or
your inventory screen) to open **Inventory and chests settings**. Toggles are
saved to `config/invsortbuttons-client.toml`.

| Setting | Default | What it does |
|---|---|---|
| Shortcuts | ON | Click shortcuts (see below) |
| Middle click | ON | Middle-click a container GUI to sort it |
| Chest buttons | ON | Show the `z` `\|\|` `=` buttons on containers |
| Auto-refill | ON | Refill a hotbar slot when its stack runs out or a tool breaks |
| Auto-equip armor | OFF | Sorting your inventory also equips better armor |
| Sort on pickup | OFF | Picking up items sorts your inventory |
| Enable sound | ON | Click sound when sorting |

The sort key (default **R**) is rebindable in Options → Controls → Inventory
Sorter Buttons.

## Shortcuts

| Input | Action |
|---|---|
| Middle click | Sort the chest (or your inventory when hovering it) |
| R | Sort your main inventory |
| Alt + Click | Drop the whole stack |
| Ctrl + Click | Move one item to the other section |
| Ctrl + Shift + Click | Move all stacks of that item across |
| Space + Click | Move everything in that section across |

## The rules file — `config/InvSortButtonsRules.txt`

Rules shape how **your inventory** is sorted (chests use the plain algorithms).
One rule per line: a *constraint* and a *keyword*.

```
     1   2   3   4   5   6   7   8   9
 A [A1][A2][A3][A4][A5][A6][A7][A8][A9]   <- top row of your inventory
 B [B1][B2][B3][B4][B5][B6][B7][B8][B9]
 C [C1][C2][C3][C4][C5][C6][C7][C8][C9]
 D [D1][D2][D3][D4][D5][D6][D7][D8][D9]   <- hotbar (never moved)
```

Examples:

```
A food                  fill the top row with food
1 pickaxe               fill column 1 with pickaxes (bottom-to-top)
D1 sword                a sword in the first hotbar slot (ignored: hotbar is never moved)
A1-C4 block             fill that rectangle with blocks (add v to fill vertically, r to reverse)
B FROZEN                leave row B exactly as it is
A1 minecraft:cobblestone    exact item by registry name
B #forge:ores           any item in an item tag (# prefix)
```

More specific rules win: a single slot beats a column, a column beats a row, a
row beats a rectangle; deeper tree keywords beat shallower ones. Edit the file,
press **R**, and the changes apply — on servers, your rules follow you (each
player's client syncs its own files).

## The item tree file — `config/InvSortButtonsTree.txt`

The tree defines every keyword and the global sort order (top to bottom =
first to last). Same XML format as the original, with modern matchers:

```xml
<sword class="sword" />                 <!-- item kind, any mod -->
<oreBlock tag="forge:ores" />           <!-- item tag, any mod -->
<cobblestone id="minecraft:cobblestone" />  <!-- exact item -->
```

Available classes: `sword`, `bow`, `crossbow`, `trident`, `pickaxe`, `shovel`,
`axe`, `hoe`, `tool`, `shears`, `fishingrod`, `flintandsteel`, `helmet`,
`chestplate`, `leggings`, `boots`, `armor`, `shield`, `food`, `potion`,
`bucket`, `minecart`, `boat`, `record`, `block`.

Every element name (category or item) becomes a keyword usable in rules, e.g.
`A material` fills row A with ingots, gems, dusts, etc. Items matching nothing
in the tree sort last, grouped by item.

If a file is ever broken, it falls back to defaults; delete it to regenerate.
