# Porting plan, 1.12.2 to 1.21.1

This document is the work sheet for the port of PackagedAuto Terminals to the other Minecraft
versions. It says what to do, in which order, how to validate each step, and how to read the
progress at a glance.

Rule 1 of `CLAUDE.md` applies here too: **verify before writing**. Every version number and
every claim about another mod in this file that carries the mark `?` is a hypothesis. Batch P0
of each target replaces it with a verified value and records the evidence.

---

## 1. The targets

| Target | Loader | AE2 artifact | PackagedAuto branch | Mappings | Order | State |
|---|---|---|---|---|---|---|
| 1.12.2 | Forge 14.23.5.2847 | AE2UEL 0.56.5 | `1.12` (1.0.24.73) | MCP snapshot_20171003 | source | ✅ released |
| 1.20.1 | Forge ? | AE2 15.x ? | `1.20` (3.4.15.46) | Mojang (official) | 1 | ⬜ not started |
| 1.21.1 | NeoForge ? | AE2 19.x ? | `1.21` (4.0.8.21) | Mojang (official) | 2 | ⬜ not started |
| 1.19.2 | Forge ? | AE2 12.x ? | `1.19` ? | Mojang (official) | 3 | ⬜ not started |
| 1.18.2 | Forge ? | AE2 11.x ? | `1.18.2` (3.1.13.52) | Mojang (official) | 4 | ⬜ not started |
| 1.16.5 | Forge ? | AE2 8.x ? | `1.16` ? | Mojang (official) ? | 5 | ⬜ optional |

**Why this order.** 1.20.1 carries the largest player base and the modern AE2 API. 1.21.1 shares
that API and differs mainly by the loader, so it costs a fraction of the first port. 1.19.2 and
1.18.2 are then near-copies of 1.20.1. 1.16.5 has the worst ratio: an AE2 API close to none of
the others, and few players. Do it last, or drop it.

**Do not start two targets at once.** The first port pays for the whole architecture. The
following ones reuse it.

### 1.1 The four API families

The cost of a port is driven by the AE2 family, not by the Minecraft version.

| Family | Versions | AE2 shape | Consequence for us |
|---|---|---|---|
| **A** | 1.12.2 | AE2UEL. `ME Interface`, `Interface Terminal`. `IAEItemStack`, `IGrid`, `AEBaseContainer`, `AEBaseGui`. | the current code |
| **B** | 1.16.5 | AE2 rv8. Same vocabulary as A, rewritten classes, new registration. | a full rewrite of the AE2 layer |
| **C** | 1.18.2, 1.19.2, 1.20.1 | `Pattern Provider`, `Pattern Access Terminal`. New menu system, `AEKey` in place of `IAEItemStack`. | a second full rewrite, then reused three times |
| **D** | 1.21.1 | Family C plus NeoForge. | the C layer, with the loader adapted |

The rename `ME Interface` → `ME Pattern Provider`, and `Interface Terminal` → `Pattern Access
Terminal`, happened between 1.18 and 1.19.2. Our reference model changes name at that point,
not nature: it is still the terminal that lists the network machines and edits their patterns.

---

## 2. Strategy: one branch per family

**Decision P1.** `main` stays 1.12.2. Each target gets a long-lived branch named `mc/<version>`,
for example `mc/1.20.1`. No multi-loader mono-repo, and no Gradle subproject shared between
branches: ForgeGradle 2.3 and the modern toolchains cannot live in the same build.

**Decision P2.** The pure logic lives in `src/core/java/fr/julien/packagedautoterminals/core/`
on **every** branch, with an identical file list and identical contents. `main` is the source of
truth. A branch synchronizes by `git cherry-pick` of the core commits, never by hand.

**Decision P3.** A CI job `core-sync` compares the SHA-256 of every file under `src/core/java`
across the branches, and fails when two branches disagree. Without that gate, decision P2 decays
in two months.

> Extracting `core` is the first task of the whole plan, on `main`, **before** any port. It is
> revision R3 of `docs/DECISIONS.md`, now due.

---

## 3. The portability map

Measured on the 53 files and 8207 lines of `src/main/java`, by counting the imports of
`net.minecraft`, `appeng` and `thelm`.

| Tier | What it means | Packages | Files | Lines |
|---|---|---|---|---|
| **T0** | moves unchanged, no Minecraft import | `Feedback`, `ProviderPairing`, `ProviderRole`, `Reference`, `jei/PatGuiAreas` | 5 | 641 |
| **T1** | light adaptation, Minecraft types only | `GroupNames`, `EditorInventory`, `ProviderSnapshot`, `CrafterTypes`, `PatDiagnostics`, `RecipeWriter`, `gui/GuiScaleFit`, the payload of `network/` | 12 | ~1100 |
| **T2** | heavy adaptation, AE2 and PackagedAuto API | `TerminalContext`, `NetworkItems`, `ProviderScanner`, `MachineSnapshot`, `container/`, `proxy/PatGuiHandler`, `item/`, `part/` | 10 | ~2200 |
| **T3** | rewrite, the framework itself changed | `client/gui/GuiPatTerminal`, `client/gui/GuiPatEditor`, `PatButton`, `PatItems`, `PatRecipes`, `PatConfig`, `client/PatKey*`, `BlockHighlighter`, `network/PatNetwork` and the packet registration | 16 | ~3600 |
| **T4** | dropped or replaced on the target | `integration/wut/`, `mixin/ae2wut/`, `client/WutModelPatch`, `common/FluidPackets` | 10 | ~670 |

Read this table as the real budget. **T3 is 44 % of the code, and it is the GUI.** The two GUI
files alone are 2202 lines. Any port that does not plan for them fails.

### 3.1 What disappears on the target

| Piece | Reason | Replacement |
|---|---|---|
| `integration/wut/`, `mixin/ae2wut/`, `WutModelPatch` | AE2WUT exists only on 1.12.2 | none, or the target's own wireless terminal system |
| `common/FluidPackets` | PackagedAuto handles fluids natively from 1.18.2 `?` | the native PackagedAuto fluid API |
| `packagedfluidcrafting` integration | same reason `?` | none |
| the `-proc:none` compiler flag and MixinBooter | no mixin left once AE2WUT is gone | none |
| `checkDevMods` | ForgeGradle 2.3 pitfall only | none |

Each line marked `?` is a hypothesis to confirm in P0.

---

## 4. The batches, per target

Every target runs the same eight batches. Copy `docs/porting/TEMPLATE.md` to
`docs/porting/<version>.md` and follow it. A batch is done when its acceptance criterion passes,
not when the code compiles.

### P0 — Reconnaissance

**Goal.** Kill the unknowns before writing a line.

1. Clone the PackagedAuto branch of the target and read `TilePackager`, the recipe holder item,
   and the recipe type registry.
2. Clone AE2 at the target version and read the Pattern Access Terminal, or the Interface
   Terminal on family B: the part, the menu, the packet that edits a pattern.
3. Fill the version table of section 1 with the exact artifacts and their Maven coordinates.
4. Answer the eight questions of the P0 questionnaire, below.
5. Write `docs/PACKAGEDAUTO-MODEL-<version>.md`, on the model of the 1.12.2 one, with the
   evidence: file path and line for every claim.

**P0 questionnaire.**

1. Does PackagedAuto still use a **Package Recipe Holder** item inserted into the machine, or
   does the machine hold the recipes itself?
2. Where does the recipe list live: in the item NBT, in the block entity, or in a capability?
3. Which interface exposes a recipe type, and how do the addons register theirs?
4. Does AE2 still expose a **part** API that allows a third-party terminal on a cable?
5. Does AE2 still expose a grid scan that returns the machines by type?
6. Which AE2 class does the Pattern Access Terminal use for its menu, and is it extensible?
7. Which JEI or REI version does the target use, and does its transfer handler API still fit?
8. Is there a wireless terminal API, and does it accept a third-party terminal?

**Acceptance.** `docs/PACKAGEDAUTO-MODEL-<version>.md` exists, every `?` of section 1 is
resolved, and the eight questions have an answer with its evidence.

**Stop condition.** If question 4 or question 5 answers no, stop and report. The whole design
rests on them.

### P1 — Build skeleton and dev environment

**Goal.** An empty mod that loads, next to AE2 and PackagedAuto.

1. Create the branch `mc/<version>` from an empty tree, not from `main`. The build files share
   nothing.
2. Write `build.gradle` for the target toolchain: ForgeGradle 5/6, NeoGradle, or the
   loader's own plugin.
3. Declare the two required dependencies, pinned, from an official Maven when one exists.
4. Port `Reference.java` and the `@Mod` class, with `required-after` on `appliedenergistics2`
   and `packagedauto`.
5. Set up the dev run directory with the optional addons.

**Acceptance.** `gradlew build` succeeds, `gradlew runClient` reaches the main menu, and the mod
list shows PackagedAuto Terminals with its two dependencies.

### P2 — Registration and presence in game

**Goal.** The terminal exists as an object.

1. Port the item, the part and the creative tab to the target registration system.
2. Port the recipe, the models, the textures, the two language files.

**Acceptance.** The item is craftable, it places on a cable as a part, and both textures render.
Test A1, A2 and A3 of `docs/TESTING.md` pass.

### P3 — Read-only terminal

**Goal.** The screen opens and lists the machines of the network.

1. Port `TerminalContext`, `NetworkItems`, `ProviderScanner`, `ProviderSnapshot`,
   `MachineSnapshot` to the target AE2 API.
2. Port `ContainerPatTerminal` and `GuiPatTerminal`.
3. Port the list packet.

**Acceptance.** Tests B1 to B7 and C1 to C3 of `docs/TESTING.md` pass.

### P4 — Editing

**Goal.** The player changes a recipe from the terminal, and the server validates it.

1. Port `EditorInventory`, `RecipeWriter`, `ContainerPatEditor`, `GuiPatEditor`.
2. Port the action packets. **Rule 2 of `CLAUDE.md` holds: the client sends an intent, the
   server validates the permissions, the energy and the range.**

**Acceptance.** Tests D1 and the following pass. A recipe edited from the terminal produces the
same NBT as a recipe encoded by hand in a Package Recipe Encoder. Compare the two tags.

### P5 — Wireless

**Goal.** The wireless terminal works, if the target allows it.

**Acceptance.** The terminal opens at range, drains energy, and closes out of range.

### P6 — Recipe viewer integration

**Goal.** Drag from JEI or REI into the editor.

**Acceptance.** The transfer of a recipe fills the editor slots, and the ghost items render.

### P7 — Polish and release

1. Port the in-game help and the sixteen language files.
2. Update `README.md`, `CHANGELOG.md` and the wiki with the target version.
3. Add the target to `.github/workflows/build.yml`.

**Acceptance.** The CI produces `packagedautoterminals-<mcversion>-<modVersion>.jar`, and the jar
loads in a real instance, not only in the dev workspace.

---

## 5. The validation protocol

Four gates, in this order. A batch that fails a gate is not done.

| Gate | Command or action | Passes when |
|---|---|---|
| **G1 build** | `gradlew build` | no error, and no warning about a mapping |
| **G2 launch** | `gradlew runClient` | the main menu appears, and the log holds no exception from our modid |
| **G3 in game** | the tests of `docs/TESTING.md` for the batch | every listed test passes |
| **G4 real instance** | copy the jar into a real profile with the full addon set | the world loads, and the terminal works |

**Regression rule.** Every target keeps its own copy of `docs/TESTING.md`, with the results
dated. A test that passed on 1.12.2 and fails on the target is a port defect, not a new feature.

**Warning: never build a family A branch with a JDK newer than 8.** ForgeGradle 2.3 stops with
`Unsupported class file major version`. The family C and D branches need JDK 17 or newer. Set
`JAVA_HOME` before every build, or use a Gradle toolchain declaration on the modern branches.

---

## 6. Progress tracking

### 6.1 The matrix

Update this table at the end of every session. One cell, one state:
⬜ not started, 🔶 in progress, ✅ done, ⛔ blocked.

| Target | P0 | P1 | P2 | P3 | P4 | P5 | P6 | P7 |
|---|---|---|---|---|---|---|---|---|
| 1.12.2 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 🔶 |
| 1.20.1 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 1.21.1 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 1.19.2 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 1.18.2 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 1.16.5 | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

Prerequisite, on `main`, before target 1: extract `core` (decision P2) ⬜, and add the
`core-sync` job (decision P3) ⬜.

### 6.2 The per-target sheet

`docs/porting/<version>.md` holds, for that target only:

1. the resolved version table,
2. the eight P0 answers with their evidence,
3. one section per batch, with the date, the gate results, and the deviations from this plan,
4. the list of pieces dropped, with the reason.

### 6.3 The rule that keeps the tracking honest

A batch marked ✅ without a dated gate result in the target sheet does not count. On the next
session, treat it as 🔶.

---

## 7. Risks

| Risk | Sign | Answer |
|---|---|---|
| AE2 refuses a third-party part | P0 question 4 answers no | ship the wireless item only, or drop the target |
| The AE2 menu system is closed | P0 question 6 answers no | rebuild the screen on the plain loader menu, without the AE2 base classes |
| PackagedAuto changes its recipe model between two of its own releases | the NBT written no longer loads | pin the PackagedAuto version per target, as in `gradle.properties` |
| The GUI port drags on | batch P3 crosses three sessions | cut the feature set: list and read first, search and pin later |
| The branches diverge | `core-sync` fails | fix on `main`, cherry-pick, never patch the branch |

---

## 8. The honest estimate

The first modern target costs more than the 1.12.2 mod did, because P0 and P3 restart from
nothing against an unknown API. The following targets of family C cost far less, because only
the registration and the loader differ.

| Target | Expected effort | Driver |
|---|---|---|
| `core` extraction on `main` | 1 session | 5 files, plus the CI job |
| 1.20.1 | 8 to 12 sessions | P0 and the GUI rewrite |
| 1.21.1 | 2 to 4 sessions | the loader, not the API |
| 1.19.2 | 1 to 3 sessions | near-copy of 1.20.1 |
| 1.18.2 | 1 to 3 sessions | near-copy, plus the fluid question |
| 1.16.5 | 6 to 10 sessions | a family of its own, for few players |

These numbers use the session as the unit of `docs/PLAN.md`. They assume one target at a time.
