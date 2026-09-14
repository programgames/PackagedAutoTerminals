# Action plan

> Approved on 2026-09-12. No batch starts before the previous one is validated.
> Each batch ends with a criterion observable in game.

## Batch 0 — Clear the unknowns ✅ done

| Question | Answer |
|---|---|
| Does a mod already provide this terminal? | **No.** Nothing on 1.12.2. `Extended Terminal` exists but targets 1.20/1.21 and does not touch PackagedAuto |
| Upstream licence of PackagedAuto | **MIT** — compile dependency and API reuse with no obstacle, with attribution |
| How to get AE2UEL to compile against | **CurseMaven** as first choice. Fallback: local jar in `libs/`, already present on the machine (`ae2-uel-v0.56.5.jar.bak`). No official Maven repository found |
| Does Cleanroom add risk? | **No.** Confirmed by the player: every mod of the pack also runs on Forge. MeatballCraft works on Forge or Cleanroom, with no impact |

Still open: **Q1** (URL of the player's AE2UEL fork) and **Q3** (public or private
repository). Still to verify in the code, at the start of batch 1: generic walk over the grid
nodes, exact effect of `setPatternStack()`, behaviour of the `Packager Extension`.

## Batch 1 — Skeleton and dev environment ✅ done on 2026-09-12

Git repository, `build.gradle`, pinned dependencies, `mcmod.info`, empty main class,
populated `run/mods`.

**Result**: `gradlew setupDecompWorkspace`, `gradlew build` and `gradlew runClient` pass. The
dev client loads **17 mods**, including `PackagedAuto Terminals 1.12.2-0.1.0`. The log does
show `pre-init` then `init`.

**Three code points verified along the way**, recorded in `docs/PACKAGEDAUTO-MODEL.md`
section 7:

1. The Recipe Holder sits in **slot 10** of the Packager.
2. Writing that slot calls `updatePatternList()`, which calls `postPatternChange()`.
   **The most fragile point of the project is therefore settled before a single line of GUI
   was written.** Rule that follows: always rewrite the stack, never edit its NBT in place.
3. Discovery goes through `IGrid.getMachinesClasses()` then `getMachines(cls)`, without
   knowing any addon class at compile time.

**Constraint D20 proven**: a probe compiled against the AE2 internal classes
(`AbstractPartTerminal`, `AEBasePoweredItem`). The `flatDir` + `deobfProvided` setup holds.

**Left for you**: open the "Mods" menu of the already running client, and check the
`PackagedAuto Terminals` line. Then the load test in the real Cleanroom instance, with the
jar from `build/libs/`.

## Batch 2 — Read only, wired terminal ✅ done on 2026-09-12

Discovery of the `IPackageProvidingMachine` instances on the grid. Server-side snapshot.
Packet to the client. GUI listing the machines and their recipes. No writing.

**Result**: the terminal is placed on an ME cable, opens, and shows the Packager, its state,
then its recipe with the `Processing` type. Tooltip on hover, scrollbar in place.

**R2 measurement**: **344 bytes** for one machine and one recipe. Chunk splitting therefore
remains unnecessary. The measurement on a loaded network must be redone before closing
revision R2.

### What this batch cost, and why

Seven defects, all caused by unverified assumptions about the API of another mod:

| Defect | Cause |
|---|---|
| the item would not place | `IPartItem` alone is not enough; `onItemUse` must be redirected to `PartPlacement` |
| the screen would not open | `AEBaseContainer` refuses a vanilla `Slot`; it requires an `AppEngSlot` |
| the screen closed itself | the `TileEntity` constructor called with `null` |
| translations missing | no `pack.mcmeta`; Forge applied the pre-1.11 rules |
| broken background sheet | an AE2 band reused without looking at it; it was a search field |
| overlapping text | two lines inside an 18 pixel row |
| stale code at launch | FML loads the mod from the jar, not from the classes |

**Rule that follows, and that holds for the next batches**: never assume the behaviour of an
AE2 class. Read it with `javap`, or read the upstream source, before using it. The first six
defects would have been avoided by ten minutes of reading.

## Batch 3 — Writing ✅ done on 2026-09-13

Editor driven by `IRecipeType.getEnabledSlots()`, `getSlotColor()`, `canSetOutput()`.
Encoding through `IRecipeInfo.generateFromStacks()`, on the server. Validation through
`isValid()`. Write into the holder, then republish the AE2 patterns.

**Done when**: I add, edit and remove a recipe from the terminal, and AE2 sees the change
without me touching the block.

## Batch 4 — JEI and comfort ✅ written on 2026-09-13, not tested in game

`IRecipeType.getRecipeTransferMap(IRecipeLayout, String)` already exists in the API. The JEI
transfer is therefore cheap, **provided** the editor is a real `Container` with ghost slots
indexed like the Encoder ones. Search, filters, sorting.

**Done when**: the JEI "+" button fills the terminal editor.

## Batch 5 — "Machines" tab and diagnostic ✅ written on 2026-09-13, not tested in game

Mapping table `recipe type → crafter class`, one per integration module. List of the
crafters, `isBusy()` state, warning about orphan recipes.

**Done when**: I encode an Ultimate recipe without placing an Ultimate Crafter, and the
terminal reports it.

## Batch 6 — Wireless terminal — 1 to 2 sessions

Powered item, `IWirelessTermHandler`, registration in the registry, range, energy.

**Done when**: the terminal works remotely and cuts out when out of range.

## Batch 7 — Integration, polish, release 🔶 partly written

AE2WUT with a **configurable** mode id, Baubles, `fr_fr`, GitHub workflow, `CHANGELOG.md`,
final test in the real instance.

**Indicative total: 10 to 16 sessions.**

---

## The most fragile point of the project

It is neither the GUI nor the network. It is **republishing the patterns after a write**. If
`setPatternStack()` does not notify the grid correctly, AE2 keeps a stale view. The player
sees their change on screen, with no effect on their crafts.
This point is verified at the start of batch 1, never at batch 3.


---

## State as of 2026-09-13

| Batch | State | Left |
|---|---|---|
| 0, 1, 2 | ✅ tested in game | |
| 3 | ✅ written, removal and editing tested | creation, amounts and moving **untested** |
| 4 | ✅ written | JEI transfer and search **untested** |
| 5 | ✅ written | Machines tab and diagnostic **untested** |
| 6 | ⬜ not started | wireless terminal, on a separate branch |
| 7 | 🔶 licence, notice, changelog, CI, config, crafting recipe | AE2WUT, Baubles |

> Everything marked "untested" waits for the test session described in `docs/TESTING.md`. The
> code compiles, but has never run.

### What is left, by value

1. **Run through `docs/TESTING.md`**, from T1 to T15. That is the only way to validate
   fifteen functions written without playing.
2. **Batch 6**, the wireless terminal. It requires detaching the container from the wired
   part, which touches working code. To be done on a branch.
3. **AE2WUT and Baubles**, once the wireless terminal is validated.
4. **Fluids and gases**, planned for version 2 (decision D09).
