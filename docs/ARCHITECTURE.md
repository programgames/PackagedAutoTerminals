# Architecture

## 1. Functional scope

### Tab 1 — "Patterns" (read and write)

The terminal walks the ME grid. It keeps every machine that answers
`instanceof IPackageProvidingMachine`. It reads its `getPatternStack()`. When the stack
implements `IRecipeListItem`, it pulls the `IRecipeList` out of it, hence the list of
`IRecipeInfo`.

Display: one header per machine group, then its recipes, like the AE2 Interface Terminal. A
group gathers the machines that carry exactly the same recipes, or that share a name given by
the player (D38). The `Packager Extension` does not implement `IPackageProvidingMachine`: the
terminal does not list it.

Actions: add a recipe, remove a recipe, edit a recipe, change its type, repair a group, give
blank Recipe Holders from the ME network to the machines of a group that lack one, send the
Recipe Holders of a group back to the network.

Filters: by machine name, by recipe type, by input or output item.

### Tab 2 — "Machines" (read only)

It lists the `IPackageCraftingMachine` machines of the network, grouped by the recipe type
they accept. It reports the `isBusy()` state.

Its main value is the **diagnostic**: it reports every encoded recipe that has no crafter
able to run it on the network. That is the most frequent mistake in game, and no mod detects
it today.

### Out of scope for v1

- Gases (`PackagedFluidCrafting` + Mekanism) → v2. Fluids arrived in v1: see **R5**
- ME network item panel in the same screen → no, we stay on a pattern list, like the
  Interface Terminal
- `ISettingsCloneable` → later

---

## 2. Modules

A single Gradle module (revision **R3**). Split by package:

```
fr.julien.packagedautoterminals/
 ├── PackagedAutoTerminals, Reference   mod entry point, identity constants
 ├── common/            grid scan (ProviderScanner), snapshots (ProviderSnapshot,
 │                      MachineSnapshot), pairing (ProviderPairing), recipe writes
 │                      (RecipeWriter), editor slots (EditorInventory), crafter table
 │                      (CrafterTypes), group names, config, ME storage access
 ├── container/         ContainerPatTerminal, ContainerPatEditor
 ├── network/           PatNetwork, channel "pat_terminals" (D37), and its 7 packets
 ├── part/              PartPatTerminal, the wired terminal
 ├── item/              ItemPatTerminal (places the part), ItemWirelessPatTerminal
 ├── proxy/             PatGuiHandler: every screen opens through FML, from its id
 ├── client/            opening key, snapshot reception, block highlight, AE2WUT icon,
 │                      startup diagnostics
 ├── client/gui/        GuiPatTerminal, GuiPatEditor, GuiScaleFit, PatButton
 ├── integration/jei/   recipe transfer, ghost drag, screen areas
 ├── integration/wut/   AE2WUT bridge, right click listener, mixin loader
 └── mixin/ae2wut/      the three AE2WUT injections (D33)
```

The addons (PackagedExCrafting, PackagedAvaritia, PackagedFluidCrafting, PackagingProvider)
have no package of their own. `CrafterTypes` and `ProviderRole` name the machine classes of
the addons by string. `FluidPackets` checks the PackagedFluidCrafting modid, and names the AE2FC
item `ae2fc:fluid_packet` by its registry id (R5).

`src/test/java` holds the JUnit tests of the logic that runs without a world: search, pairing,
scaling, list operations of `RecipeWriter`, messages, network protocol, snapshot size.

The recipe editor is a real `Container`, with ghost slots indexed like the Encoder ones. That
is constraint **D22**, without which JEI becomes unusable.

## 3. Data flow

### Terminal

```
SERVER, ContainerPatTerminal                        CLIENT
  every refreshTicks, and right after a write:
  refusal: range, link, BUILD, energy
    → the screen closes, with a message
  ProviderScanner.scan walks the ME grid
  ProviderSnapshot.writeAll: each distinct
    recipe list once, machines point to it
  + crafters, + the machinesTab setting
  same message as last time → nothing sent
  PacketProviderList.capped: past 2 000 000
    counted bytes, only the size ("TooBig")    →   ClientHandler fills the container
                                                    GuiPatTerminal groups the machines
                                                    and rebuilds its rows on a new snapshot

  PacketRecipeAction                           ←   intent: action, machine position,
                                                    index in the group, and the recipe NBT
                                                    for a removal or an edit
  BUILD check, fresh scan
  RecipeWriter.resolve finds the recipe by identity:
    gone → nothing written, "group changed"
  RecipeWriter.apply writes the server copy into
    every machine of the group
  setPatternStack → PackagedAuto republishes
    its AE2 patterns
```

### Editor

`ContainerPatEditor` opens on one recipe of a group, or on two recipe holders in holder mode
(D43). The vanilla container synchronises its slots. The client rebuilds the recipe from them,
as the Encoder does, to draw the preview, enable Save and show the "Invalid recipe" line; it
never writes it. The server rebuilds the recipe on every change, and writes it into the group
at Save. JEI and the amount panel go through `PacketEditorFill`, `PacketEditorGhost` and
`PacketEditorSlot`, which the server checks slot by slot.

### Opening

The wired terminal opens from its part. The wireless terminal opens from a right click, from
the key (`PacketOpenTerminal`), or from AE2WUT (`WutEventHandler`). Every path ends in
`PatGuiHandler.getServerGuiElement`, which asks `TerminalContext.openingRefusal`: the BUILD
permission, and for the wireless terminal the link, the range and the energy.
Holder mode needs no network: a sneak right click with the wireless terminal, or a button of
the terminal, opens it.

Four structuring decisions:

1. **The server is the authority** (D05). The client sends an intent. The recipe NBT that a
   removal or an edit carries only names the recipe: the server looks it up in its own lists
   and writes its own copy.
2. **No mixin as long as an API is enough** (D06). AE2WUT has no API: see D33.
3. **Every integration is optional**, detected by modid (D07).
4. **The whole snapshot, no delta** (R2). The server resends the full message when it changes.
   `SnapshotSizeTest` measures the limit; `docs/PLAN.md` carries the figure.

---

## 4. AE2UEL hook points

| Need | Reference AE2UEL class |
|---|---|
| Wired terminal | `AbstractPartTerminal`, `PartInterfaceTerminal` |
| Remote container | `ContainerInterfaceTerminal` |
| Wireless terminal | `IWirelessTermHandler`, `IWirelessTermRegistry`, `WirelessTerminalGuiObject`, `ToolWirelessInterfaceTerminal` |
| Terminal host | `ITerminalHost` |
| Security | `ISecurityGrid`, `SecurityPermissions` |
| Change notification | `MENetworkCraftingPatternChange` |

## 5. Precedents to study in the instance

| Mod | What it demonstrates |
|---|---|
| `cell-terminal-1.6.7` | a complete third-party terminal: part + wireless item, NBT in chunks, deltas, AE2WUT integration (`AE2WUTIntegration`, `WUTModeSwitcher`), scanner registries per mod |
| `ae2wut-1.0.5` | absorbing a third-party terminal into the universal terminal, through mixins |
| `apiarist-terminal-0.3.0` | a small third-party terminal, a good minimal example |
