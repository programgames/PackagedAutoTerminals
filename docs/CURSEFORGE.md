# CurseForge page

The text below goes into the description editor of the project page. The editor accepts
Markdown: choose the Markdown mode before you paste, otherwise it escapes every character.

Replace `<IMAGE: ...>` with an upload from the Images tab of the project. CurseForge refuses a
picture served from another site inside the description.

---

## Description to paste

PackagedAuto Terminals adds two Applied Energistics 2 terminals. They list and edit your
PackagedAuto recipes from anywhere on the ME network.

Without this mod, you encode a recipe on foot. You fill a Package Recipe Encoder. You take the
Package Recipe Holder out. You walk to the Packager. Then you walk to the Unpackager. Every
change repeats the walk.

This mod removes the walk. The model is the AE2 Interface Terminal, which lists the ME
Interfaces of a network and edits their patterns from one screen.

<IMAGE: the terminal, with two machine groups and their recipes>

### It lists every PackagedAuto machine of the network

The list holds the Packagers, the Unpackagers and the Packaging Providers. The terminal joins
the machines that work together into a group, and it shows the recipes of each group. A group is
normally one Packager with one Unpackager.

### It edits a recipe from a distance

The editor follows the layout of the Package Recipe Encoder, so you learn one screen only. One
save writes the recipe into every machine of the group at the same time. The two sides of a
group cannot differ any more. That difference is the most common silent fault in PackagedAuto.

<IMAGE: the editor, with a processing recipe and the package preview>

### It reports the recipes that no machine can run

The Machines tab names every encoded recipe that no crafter on the network runs. It lists the
Package Crafters of the network below that report. This fault is frequent in game, and no other
mod reports it.

<IMAGE: the Machines tab, with an orphan recipe reported>

### It works with JEI in both directions

The `+` button of a JEI recipe fills the whole grid. A single ingredient dragged out of JEI
fills one slot. A fluid enters as an AE2FC Fluid Packet, which is the item PackagedFluidCrafting
itself writes. You edit its amount in millibuckets.

### It carries every recipe type, and the types of the addons too

The mod reads the PackagedAuto recipe type registry and hard codes no type. An addon released
tomorrow appears in the editor with no change here.

### It goes wireless

The Wireless PackagedAuto Terminal opens the same screen from anywhere in range. It answers the
AE2 range, energy and security rules. It works from either hand. With AE2WUT installed, it joins
the Wireless Universal Terminal.

---

## Getting started

1. Craft the PackagedAuto Terminal from one ME Terminal and one Package Recipe Holder.
2. Place the terminal on a face of an ME cable, like any other AE2 part.
3. Right click the terminal. The screen lists the machine groups of the network.
4. Left click a group row. The editor opens on a new recipe.
5. Fill the grid, then press Save. The recipe reaches the Packager and the Unpackager together.

To edit a recipe that already exists, right click its row instead.

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
| PackagedAuto 1.0.24.73 or newer | required |
| JEI or HadEnoughItems | optional: recipe transfer, drag and drop, description pages |
| PackagedExCrafting, PackagedAvaritia | optional: their recipe types appear on their own |
| AE2 Fluid Crafting with PackagedFluidCrafting | optional: fluids need both |
| PackagingProvider | optional |
| AE2WUT | optional: the terminal joins the Wireless Universal Terminal |

The mod detects every integration by modid at load time. Without the addon, the feature
disappears and the game does not crash.

---

## Limits to know

- A recipe holder carries twenty recipes at most. This limit belongs to the Package Recipe
  Encoder itself. A pack that lowers the `pattern_slots` entry of the PackagedAuto configuration
  lowers the limit, and the terminal follows it.
- A craft recipe takes one item per cell. PackagedAuto forces this count, so the amount panel
  does not open on those recipes.
- The mod ships sixteen languages in game.
- Gases do not pass yet.

---

## The rest of the page

- **Source and issues**: https://github.com/programgames/PackagedAutoTerminals
- **Wiki**: https://github.com/programgames/PackagedAutoTerminals/wiki
- **License**: MIT. This project copies no file from AE2 or from PackagedAuto. It compiles
  against them only.
- **In a modpack**: yes, no permission needed. Credit is welcome, not required.
