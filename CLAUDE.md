# PackagedAuto Terminals — Minecraft Forge 1.12.2 mod

## What this project is

A mod that adds **AE2 terminals** to browse and edit **PackagedAuto** recipes remotely, from
the ME network. Today the player has to encode their recipes into a **Package Recipe
Encoder**, then walk to every machine to insert the **Package Recipe Holder**. This mod
removes that walk.

Reference model: the AE2 **Interface Terminal**, which lists the ME Interfaces of the network
and edits their patterns remotely.

---

## 1. Technical environment

| Element | Value | Note |
|---|---|---|
| Minecraft | **1.12.2** | |
| Runtime loader | Cleanroom 0.5.17-alpha | the player's test instance |
| Forge (compilation) | **14.23.5.2847** | same constraint as the `actuallyplayed` project: the `userdev` artifacts 2848→2860 are not published |
| MCP mappings | **snapshot_20171003** | |
| ForgeGradle | **2.3.10** pinned | |
| Gradle | **4.10.3** (wrapper) | ForgeGradle 2.3 does not go beyond |
| Build JDK | **JDK 8 required** | `C:\Program Files\Eclipse Adoptium\jdk-8.0.472.8-hotspot`, already in `JAVA_HOME` |
| `sourceCompatibility` | 1.8 | |

> ⚠️ **Never build with the JDK 17 installed on this machine.** ForgeGradle 2.3 and Gradle 4.x
> fail on any JDK newer than 8. On an `Unsupported class file major version` error, check
> `JAVA_HOME` first.

### Mod identity

- **modid**: `packagedautoterminals`
- **Name**: `PackagedAuto Terminals`
- **Root package**: `fr.julien.packagedautoterminals`
- **Version**: SemVer. Jar named `packagedautoterminals-1.12.2-<version>.jar`
- **Languages**: `en_us`, `fr_fr`

### Dependencies

| Mod | Reference version | Type |
|---|---|---|
| AE2 Unofficial Extended Life (AE2UEL) | official `v0.56.5` for compilation | **required** |
| PackagedAuto | `1.12.2-1.0.24.73` | **required** |
| PackagedExCrafting | `1.12.2-1.0.3.33` | optional |
| PackagedAvaritia | `1.12.2-1.0.3.25` | optional |
| PackagedFluidCrafting | `1.12.2-1.0.0.3` | optional, **v2** |
| PackagingProvider | `1.12.2-1.0.0.2` | optional |
| AE2WUT | `1.0.5` | optional |
| JEI / HEI | HadEnoughItems 4.31.2 | optional |

> The player runs a **local fork** of AE2UEL (`ae2-uel-v0.56.7-10-gac98c09.dirty.jar`). That
> fork only adds fixes. **We compile against official AE2UEL**, never against the fork. The
> produced jar must work with both.

### Dev runtime environment

`libs/` carries the compile dependencies. `run/mods` carries **only** the mods that are absent
from `libs/`, otherwise FML refuses to start (see section 4.1):

```
Avaritia, Baubles, CodeChickenLib, Cucumber, ExtendedCrafting-Nomifactory-Edition,
HadEnoughItems, PackagedAvaritia, PackagedExCrafting, PackagingProvider, mixinbooter,
ae2wut, cell-terminal
```

> ⚠️ **`cell-terminal` crashes with `ae2wut-1.0.5`.** Cell Terminal 1.6.7 calls
> `AE2UELWirelessUniversalTerminal.registryContainer`, an API absent from version 1.0.5. The
> game stops with `NoClassDefFoundError: com/circulation/ae2wut/AE2UELWirelessUniversalTerminal$GetGui`.
> Fix: `B:enableAE2WUT=false` in `run/config/cellterminal_server.cfg`. The player's real
> instance already carries that setting.

> `cell-terminal` serves as a **model**, not as a dependency. It shows how an AE2UEL addon
> declares a key: see decision **D34**.

> `ae2wut-1.0.5.jar` serves the Wireless Universal Terminal integration. It is **not** a
> compile dependency: our mixins target its classes by name. See decision **D33**.

> `libs/mixinbooter-10.7.jar` carries the Mixin library, as `compileOnly`. It lives outside
> `libs/maven`, on purpose: ForgeGradle does not put `compileOnly` entries on the runtime
> path, so there is no duplicate with `run/mods`, and `checkDevMods` only walks `libs/maven`.

> `PackagingProvider` requires `mixinbooter`. Without it, FML stops with a
> `MissingModsException`.

### Test instance

`H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`

---

## 2. Working rules

1. **Verify before writing.** Every statement about the behaviour of another mod must come
   from reading the code, not from memory. Record the evidence in
   `docs/PACKAGEDAUTO-MODEL.md`.
2. **The server is the authority.** The client never writes a recipe NBT. It sends an intent.
   The server validates the AE2 permissions, the energy and the range.
3. **No mixin as long as an API is enough.** A mixin breaks on every update of another mod.
   Mixins are reserved for `PackagedFluidCrafting` and AE2WUT, and only when no other path
   exists.
4. **Every integration is optional.** Detected by modid at load time. Without the addon, the
   feature disappears and nothing crashes.
5. **Testable increments.** Every step ends with a dev client launch.
6. **`core` testable outside Minecraft.** Sorting, filtering, diffing and validation go into
   `core`, with pure JUnit tests.
7. **Minimal dev environment.** AE2UEL + PackagedAuto + the three addons + JEI. Never the full
   pack.
8. **Pinned dependencies.** No floating version in `build.gradle`.

---

## 3. Repository structure

```
PackagedAutoTerminals/
├── CLAUDE.md                     this file
├── README.md
├── CHANGELOG.md                  SemVer
├── settings.gradle               a single module to start with (see revision R3)
├── src/main/java/fr/julien/packagedautoterminals/
├── src/main/resources/
├── libs/                         local AE2UEL jar, when CurseMaven fails
├── docs/
│   ├── PLAN.md                   the 8 batches and their acceptance criteria
│   ├── ARCHITECTURE.md           layers, data flow, entry points
│   ├── PACKAGEDAUTO-MODEL.md     verified data model, with evidence
│   ├── DECISIONS.md              approved decisions, revisions and constraints
│   └── TESTING.md                manual in-game tests, step by step
└── .github/workflows/build.yml
```

The `core` module and the `forge-1.12` folder will appear at the 1.16 port, once we know what
is truly shared. See revision **R3** in `docs/DECISIONS.md`.

---

## 4. Pitfalls hit, and their cause

### 4.1 `DuplicateModsFoundException` when starting the dev client

ForgeGradle puts the `deobfProvided` dependencies on the `runClient` runtime path. FML
therefore loads them as mods. Putting the **same** jars into `run/mods` produces:

```
Found a duplicate mod appliedenergistics2 at [.\modse2-uel-v0.56.5.jar, ...\libs\...]
```

**Rule**: any jar present in `libs/maven` must **never** be copied into `run/mods`.

This trap was hit twice: with AE2UEL, then with JEI, the day it became a compile dependency.
The `checkDevMods` task now detects it, and `runClient` depends on it: the game can no longer
start with a duplicate.

### 4.2 Non-deobfuscated dependencies: two traps in a row

Symptom: `GuiPatTerminal is not abstract and does not override abstract method
drawGuiContainerBackgroundLayer`. Yet the code extends an AE2 class that implements it.

Cause: the compile path carried the **SRG** names (`func_146976_a`), not the MCP ones. Two
distinct causes followed each other.

1. **`flatDir` does not trigger deobfuscation.** The log shows
   `deobfProvidedDeobfDepTask0 SKIPPED`. Fix: a real local Maven repository, in `libs/maven`,
   with one `.pom` per artifact, and the `@jar` notation on the dependency. Without `@jar`,
   Gradle puts the `.pom` itself on the compile path.
2. **ForgeGradle leaves the raw jar on the path, before the deobfuscated one.** `javac` takes
   the first match, hence the raw one. Fix: the `afterEvaluate` block of `build.gradle`
   filters `sourceSets.main.compileClasspath`.

The `gradlew printCp` task prints the compile path. It served to find both causes; keep it.

### 4.2 bis Every slot of an AE2 container must extend AppEngSlot

`AEBaseContainer.addSlotToContainer` throws
`Invalid Slot [...] for AE Container instead of AppEngSlot`, and the screen never opens. This
trap was hit **twice**: with a vanilla `Slot` for the player inventory, then with the
PackagedAuto `SlotFalseCopy` in the editor.

**Rule**: in a container that extends `AEBaseContainer`, use only the AE2 slots.

| Need | AE2 class |
|---|---|
| player inventory | `bindPlayerInventory(inventory, x, y)` |
| ghost slot | `SlotFake`, which also implements `IJEITargetSlot` |
| read-only preview | `AppEngSlot` with `isItemValid` and `canTakeStack` returning false |

### 4.3 MCP names of `snapshot_20171003`

The 2017 mappings do not know the recent names:

| Recent name | Name expected here |
|---|---|
| `CreativeTabs.createIcon()` | `getTabIconItem()` |
| `Item.setTranslationKey()` | `setUnlocalizedName()` |

### 4.4 `cannot access IMTModGuiContainer2`

`appeng.client.gui.AEBaseGui` implements the Mouse Tweaks API. Without that interface on the
compile path, every class extending it fails. The project therefore bundles the stub
`src/api/java/yalter/mousetweaks/api/IMTModGuiContainer2.java`.

### 4.5 Backslash in a Groovy string

`"C:\Program Files\..."` in `build.gradle` fails with `unexpected char: ''`. Use forward
slashes.

### 4.6 The shell breaks on very long commands

Write the Java files with the direct write tool, not with a multi-line `cat`.

### 4.7 The network channel name is limited to 20 characters

On 1.12.2, `CPacketCustomPayload` reads the channel name with `buf.readString(20)`. A longer
name disconnects the player on a dedicated server, with
`DecoderException: The received string length is longer than maximum allowed`. Single player
escapes it: Forge links both sides through an `EmbeddedChannel`, which serialises no string.

`Reference.MOD_ID` is 21 characters long. The channel therefore uses `Reference.CHANNEL`,
which is `pat_terminals`. See decision **D37**.
