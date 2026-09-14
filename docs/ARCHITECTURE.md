# Target architecture

## 1. Functional scope

### Tab 1 — "Patterns" (read and write)

The terminal walks the ME grid. It keeps every machine that answers
`instanceof IPackageProvidingMachine`. It reads its `getPatternStack()`. When the stack
implements `IRecipeListItem`, it pulls the `IRecipeList` out of it, hence the list of
`IRecipeInfo`.

Display: one line per machine, then its recipes, like the AE2 Interface Terminal. The
`Packager Extension` is shown as a line attached to its Packager, never as a separate
machine.

Actions: add a recipe, remove a recipe, edit a recipe, change its type, move a Recipe Holder
from one machine to another.

Filters: by machine name, by recipe type, by input or output item.

### Tab 2 — "Machines" (read only)

It lists the `IPackageCraftingMachine` machines of the network, grouped by the recipe type
they accept. It reports the `isBusy()` state.

Its main value is the **diagnostic**: it reports every encoded recipe that has no crafter
able to run it on the network. That is the most frequent mistake in game, and no mod detects
it today.

### Out of scope for v1

- Fluids and gases (`PackagedFluidCrafting`) → v2
- ME network item panel in the same screen → no, we stay on a pattern list, like the
  Interface Terminal
- `ISettingsCloneable` → later

---

## 2. Modules

A single Gradle module to start with (revision **R3**). Split by package:

```
fr.julien.packagedautoterminals/
 ├── api/                  stable interfaces for the recipe type adapters
 ├── common/               registry, bridge to the PackagedAuto API, data model
 ├── network/              packets and incremental updates
 ├── part/                 PartPackagedAutoTerminal (wired terminal)
 ├── item/                 ItemWirelessPackagedAutoTerminal (IWirelessTermHandler)
 ├── container/            ContainerPackagedAutoTerminal + wireless variant + editor
 ├── client/gui/           GUI, widgets, search, recipe editor
 └── integration/          packagedexcrafting, packagedavaritia, packagedfluidcrafting,
                           packagingprovider, ae2wut, jei
```

The recipe editor is a real `Container`, with ghost slots indexed like the Encoder ones. That
is constraint **D22**, without which JEI becomes unusable.

## 3. Data flow

```
SERVER                                             CLIENT
  walk the ME grid
  → snapshot of the providing machines
  → diff against the previous snapshot
  → delta packet, split into chunks            →   apply the delta
                                                   render the list
  validation (AE2 permissions, energy, range)  ←   user intent
  write into the Recipe Holder
  republish the AE2 patterns
```

Four structuring decisions:

1. **The server is the authority.** The client sends an intent, never a recipe NBT.
2. **No mixin as long as an API is enough.**
3. **Every integration is optional**, detected by modid.
4. **Simple incremental updates**, following the AE2 `ContainerInterfaceTerminal`. Chunk
   splitting only arrives if the batch 2 measurement proves it necessary (revision **R2**).

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
