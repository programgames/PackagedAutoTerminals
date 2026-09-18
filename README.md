# PackagedAuto Terminals

This mod adds two AE2 terminals. They list and edit PackagedAuto recipes from anywhere on the ME
network. An ME network is the item network of Applied Energistics 2.

Without this mod, you encode a recipe on foot. You fill a Package Recipe Encoder. You remove the
Package Recipe Holder. You walk to the Packager. Then you walk to the Unpackager. Every change
repeats the walk. This mod removes it. The model is the AE2 Interface Terminal, which lists the ME
Interfaces of a network and edits their patterns from one screen.

![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-brightgreen)
![License MIT](https://img.shields.io/badge/license-MIT-blue)

---

## What it does

### It lists every PackagedAuto machine of the network

The list holds the Packagers, the Unpackagers and the Packaging Providers. The terminal joins the
machines that work together into a group, and it shows the recipes of each group. A group is
normally one Packager with one Unpackager.

### It edits a recipe from a distance

The editor follows the layout of the Package Recipe Encoder, so you learn one screen only. One
save writes the recipe into every machine of the group at the same time. The two sides of a group
cannot differ any more. That difference is the most common silent fault in PackagedAuto.

### It reports the recipes that no machine can run

The Machines tab names every encoded recipe that no crafter on the network runs. It lists the
Package Crafters of the network below that report. This fault is frequent in game, and no other
mod reports it.

### It works with JEI in both directions

The `+` button of a JEI recipe fills the whole grid. A single ingredient dragged out of JEI fills
one slot. A fluid enters as an AE2FC Fluid Packet. PackagedFluidCrafting writes that same item,
and you edit its amount in millibuckets.

### It carries every recipe type, and the types of the addons too

The mod reads `RecipeTypeRegistry` and hard codes no type. An addon released tomorrow appears in
the editor with no change here.

---

## Getting started

1. Craft the PackagedAuto Terminal from one ME Terminal and one Package Recipe Holder.
2. Place the terminal on a face of an ME cable, like any other AE2 part.
3. Right click the terminal. The screen lists the machine groups of the network.
4. Left click a group row. The editor opens on a new recipe.
5. Fill the grid, then press Save. The recipe reaches the Packager and the Unpackager together.

To edit a recipe that already exists, right click its row instead.

The Wireless PackagedAuto Terminal opens the same screen from anywhere in range. Craft it from one
PackagedAuto Terminal and one AE2 Wireless Terminal. Link it in an ME Security Terminal first. It
works from either hand.

For a description page in game, press the JEI usage key on either item.

### Gestures worth knowing

| Gesture | Effect |
|---|---|
| Left click a group row | Open the editor on a new recipe |
| Shift and left click a group row | Send the recipe holders of the group back to the network |
| Right click a recipe row | Edit that recipe |
| Shift and right click a recipe row | Remove that recipe from the group |
| Left click a filled slot | Open the amount panel |
| Turn the wheel over a slot | Change the amount. Shift and Ctrl take bigger steps |
| Right click a filled slot | Empty the slot |

---

## Requirements

| Mod | Role |
|---|---|
| AE2 Unofficial Extended Life | required |
| PackagedAuto | required |
| JEI or HadEnoughItems | optional: recipe transfer, drag and drop, description pages |
| PackagedExCrafting, PackagedAvaritia | optional: their recipe types appear on their own |
| AE2 Fluid Crafting with PackagedFluidCrafting | optional: fluids need both |
| PackagingProvider | optional |
| AE2WUT | optional: the terminal joins the Wireless Universal Terminal |

The mod detects every integration by modid at load time. Without the addon, the feature disappears
and the game does not crash.

---

## Limits to know

- A recipe holder carries twenty recipes at most. This limit belongs to the Package Recipe Encoder
  itself. It comes from the `pattern_slots` entry of the PackagedAuto configuration, which Forge
  bounds to 20. A pack that lowers that entry lowers the limit, and the terminal follows it. When
  the group is full, the terminal refuses a new recipe. The Encoder cannot open such a recipe
  again.
- A craft recipe takes one item per cell. PackagedAuto forces this count, so the amount panel does
  not open on those recipes.
- The mod ships sixteen languages in game. The wiki uses English, French and Simplified Chinese.
- Gases do not pass yet. The format exists in AE2 Fluid Crafting. Nobody verified it in game
  without Mekanism. This project writes nothing from memory.

---

## Building

Build with JDK 8. ForgeGradle 2.3 and Gradle 4.x fail on any newer JDK.

```
gradlew build
```

The jar lands in `build/libs/`. `CLAUDE.md` holds the full environment, the pinned versions and
the traps that this project already hit.

---

## Documentation

| File | Content |
|---|---|
| `docs/ARCHITECTURE.md` | layers, data flow, entry points |
| `docs/PACKAGEDAUTO-MODEL.md` | the verified data model of PackagedAuto, with the evidence |
| `docs/DECISIONS.md` | approved decisions, revisions, and the player reports behind them |
| `docs/TESTING.md` | the manual tests in game, step by step |
| `CHANGELOG.md` | SemVer |

Every statement about another mod in those files names the class and the method that we read. We
write nothing from memory. That is working rule 1 of the project.

---

## License

MIT, like PackagedAuto. This repository copies no AE2 file and no PackagedAuto file. The mod
compiles against them only. Read `NOTICE`.
