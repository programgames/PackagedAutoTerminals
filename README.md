# PackagedAuto Terminals

AE2 terminals that browse and edit **PackagedAuto** recipes from anywhere on the ME network.

Without this mod, encoding a recipe means walking: you fill a **Package Recipe Encoder**, take
the **Package Recipe Holder** out, then go to the Packager, then to the Unpackager, then back
again for every change. This mod removes that walk. The reference model is the AE2 **Interface
Terminal**, which lists the ME Interfaces of a network and edits their patterns remotely.

![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-brightgreen)
![Licence MIT](https://img.shields.io/badge/licence-MIT-blue)

---

## What it does

**Lists every PackagedAuto machine of the network.** Packagers, Unpackagers and Packaging
Providers, grouped by pair, each with the recipes it carries.

**Edits a recipe remotely.** The editor follows the layout of the Package Recipe Encoder, so
nothing has to be learned twice. Saving writes the recipe into **every machine of the pair at
once**: the two sides can no longer drift apart, which is the most common silent breakage in
PackagedAuto.

**Diagnoses the network.** The Machines tab lists the Package Crafters and reports every encoded
recipe that **no crafter on the network can run**. That is the most frequent mistake in game, and
no other mod detects it.

**Follows JEI both ways.** The `+` button fills the whole grid from a JEI recipe. Dragging one
ingredient fills one slot. A fluid enters as an AE2FC **Fluid Packet**, the same item
PackagedFluidCrafting writes, and its amount is edited in millibuckets.

**Every recipe type, including the addons.** The mod reads `RecipeTypeRegistry` and hard codes no
type. An addon released tomorrow appears with no change here.

---

## Getting started

1. Craft the **PackagedAuto Terminal**: one ME Terminal and one Package Recipe Holder.
2. Place it on an ME cable, like any AE2 part, and right click it.
3. Right click a group row to open the editor. Right click the empty tab to create a recipe.
4. Press **Save**. The recipe reaches the Packager and the Unpackager together.

The **Wireless PackagedAuto Terminal** opens the same screen from anywhere in range. Link it in
an ME Security Terminal first. It works from either hand.

Press the JEI usage key on either item for a description page in game.

### Gestures worth knowing

| Gesture | Effect |
|---|---|
| Right click a group | Open the editor |
| Shift + right click a recipe | Remove that recipe from the pair |
| Shift + left click a group | Send its recipe holders back to the network |
| Left click a filled slot | Open the amount panel |
| Wheel over a slot | Adjust the amount; Shift and Ctrl take bigger steps |
| Right click a filled slot | Empty it |

---

## Requirements

| Mod | Role |
|---|---|
| **AE2 Unofficial Extended Life** | required |
| **PackagedAuto** | required |
| JEI or HadEnoughItems | optional: recipe transfer, drag and drop, description pages |
| PackagedExCrafting, PackagedAvaritia | optional: their recipe types appear on their own |
| **AE2 Fluid Crafting** + **PackagedFluidCrafting** | optional, both needed for fluids |
| PackagingProvider | optional |
| AE2WUT | optional: the terminal joins the Wireless Universal Terminal |

Every integration is detected by modid at load time. Without the addon, the feature disappears
and nothing crashes.

---

## Limits to know

- A recipe holder carries **twenty recipes** at most. That is the limit of the Package Recipe
  Encoder itself, set by the `pattern_slots` entry of the PackagedAuto config, which Forge bounds
  to 20. The terminal refuses the twenty first rather than writing a recipe your Encoder could
  never open again.
- A craft recipe takes **one item per cell**. PackagedAuto forces it, so the amount panel does not
  open on those recipes.
- **Gases** are not carried yet. The format exists in AE2 Fluid Crafting, but it cannot be
  verified without Mekanism, and this project does not write from memory.
- Fourteen of the sixteen languages carry the **English text** for the new in-game help.
  Translations are welcome.

---

## Building

JDK 8 is required: ForgeGradle 2.3 and Gradle 4.x fail on anything newer.

```
gradlew build
```

The jar lands in `build/libs/`. `CLAUDE.md` holds the full environment, the pinned versions and
the traps this project has already hit.

---

## Documentation

| File | Content |
|---|---|
| `docs/ARCHITECTURE.md` | layers, data flow, entry points |
| `docs/PACKAGEDAUTO-MODEL.md` | the verified data model of PackagedAuto, **with the evidence** |
| `docs/DECISIONS.md` | approved decisions, revisions, and the player reports behind them |
| `docs/TESTING.md` | the manual in-game tests, step by step |
| `CHANGELOG.md` | SemVer |

Every statement about another mod in those files names the class and the method it was read from.
Nothing is written from memory: that is working rule 1 of the project.

---

## Licence

MIT, like PackagedAuto. No AE2 or PackagedAuto file is copied here; the mod only compiles against
them. See `NOTICE`.
