# PackagedAuto data model — verified

> **Source of the evidence**: `javap -p` on the jars of the instance
> `H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`, on 2026-09-12.
> Versions: PackagedAuto `1.0.24.73`, PackagedExCrafting `1.0.3.33`,
> PackagedAvaritia `1.0.3.25`, PackagedFluidCrafting `1.0.0.3`, PackagingProvider `1.0.0.2`.
> Upstream author: TheLMiffy1111, root package `thelm.packagedauto`.

---

## 1. The gameplay flow

```
  Package Recipe Encoder  (block, outside the ME network)
     │  encodes up to N recipes of a single type
     ▼
  Package Recipe Holder  (ITEM)
     │  inserted by hand into a "provider" machine
     ▼
  Packager / Unpackager / Packaging Provider
     │  publish the patterns onto the ME grid (ICraftingProvider)
     ▼
  AE2 schedules ──► Packager builds Recipe Packages
     │
     ▼
  Unpackager distributes ──► crafters: Package Crafter, Basic to Ultimate,
                              Combination, Ender, Extreme (Avaritia)
```

Key point: **the recipe lives in an item**, the Recipe Holder. It does not live in the block.
The provider block only carries that item and republishes the patterns.

---

## 2. The public API: `thelm.packagedauto.api`

| Type | Useful signature | Role |
|---|---|---|
| `IRecipeListItem` | `getRecipeList(ItemStack)` / `setRecipeList(ItemStack, IRecipeList)` | implemented by `ItemRecipeHolder` |
| `IRecipeList` | `getRecipeList(): List<IRecipeInfo>` / `setRecipeList(List)` / NBT | the contents of the holder |
| `IRecipeInfo` | `getRecipeType()`, `getInputs()`, `getOutputs()`, `getPatterns()`, `getEncoderStacks()`, `generateFromStacks(...)`, `isValid()` | **one recipe** |
| `IRecipeType` | `getName(): ResourceLocation`, `getEnabledSlots(): IntSet`, `canSetOutput()`, `getJEICategories()`, `getSlotColor(int)`, `getNewRecipeInfo()` | **the type**, describes the editing grid |
| `RecipeTypeRegistry` | `getRegistry(): NavigableMap<ResourceLocation, IRecipeType>`, `getId`, `getRecipeType`, `getNextRecipeType` | extensible global registry |
| `IPackagePattern` | `getRecipeInfo()`, `getIndex()`, `getInputs()`, `getOutput()` | an AE2 pattern derived from a recipe |
| `IPackageProvidingMachine` | `getPatternStack()` / `setPatternStack(ItemStack)` | **machine that carries a holder** |
| `IPackageCraftingMachine` | `acceptPackage(...)`, `isBusy()` | **machine that runs it** |
| `ISettingsCloneable` | configuration copy between machines | out of scope for v1 |
| `MiscUtil` | `writeRecipeToNBT`, `readRecipeFromNBT`, `writeRecipeListToNBT`, `readRecipeListFromNBT`, `getPatternHelper`, `condenseStacks`, `recipeEquals`, `recipeHashCode`, `arePatternsDisjoint` | **ready-made serialisation toolbox** |

`MiscUtil` is decisive: recipe serialisation is already written and public. The terminal does
not have to reinvent the NBT format.

---

## 3. Who carries recipes, who runs them

| Block | `IPackageProvidingMachine` | `ICraftingProvider` | `IPackageCraftingMachine` | Internal field |
|---|:---:|:---:|:---:|---|
| Packager | ✅ | ✅ | ❌ | `List<IPackagePattern> patternList` |
| Unpackager | ✅ | ✅ | ❌ | `List<IRecipeInfo> recipeList` |
| Packaging Provider | ✅ | ✅ | ❌ | `List<IRecipeInfo> recipeList` |
| Package Crafter | ❌ | ❌ | ✅ | `currentRecipe`, volatile |
| Basic to Ultimate Crafter | ❌ | ❌ | ✅ | `currentRecipe`, volatile |
| Combination / Ender Crafter | ❌ | ❌ | ✅ | volatile |
| Extreme Crafter (Avaritia) | ❌ | ❌ | ✅ | volatile |
| Positioned Package Distributor | ❌ | ❌ | ✅ | positions + markers |
| Package Crafting Machine Proxy | ❌ | ❌ | ✅ | a single target |
| Package Recipe Encoder | ❌ | ❌ | ❌ | **off the ME grid** |

**Major consequence**: the craft tier (Basic, Advanced, Elite, Ultimate, Extreme and so on)
is **not** a property of the machine. It is a property of the **recipe type**, carried by
`IRecipeInfo.getRecipeType()`. The matching crafter only accepts the package.

---

## 4. The recipe types present in the instance

| Mod | Registered types |
|---|---|
| PackagedAuto | `Crafting`, `Processing`, `ProcessingOrdered`, `ProcessingPositioned` |
| PackagedExCrafting | `Basic`, `Advanced`, `Elite`, `Ultimate`, `Combination`, `Ender` |
| PackagedAvaritia | `Extreme` |
| PackagedFluidCrafting | extends the core **through mixins**: fluids and gases |

The terminal hard codes none of these types. It reads `RecipeTypeRegistry.getRegistry()`. Any
future addon therefore appears with no code change.

`IRecipeType.getEnabledSlots()` gives the shape of the editing grid. `getSlotColor(int)`
gives the colour of each slot. `canSetOutput()` says whether the output is editable. These
three methods are enough to build a generic editor.

---

## 5. The Encoder block, to be replicated in the terminal

`TileEncoder` exposes: `patternInventories`, `patternIndex`, `setPatternIndex(int)`,
`saveRecipeList(boolean)`, `loadRecipeList(boolean, boolean)`, and the static field
`disabledRecipeTypes`.

Its network packets show the actions to reproduce: `PacketSetRecipe`,
`PacketSaveRecipeList`, `PacketLoadRecipeList`, `PacketCycleRecipeType`,
`PacketSetPatternIndex`, `PacketSetItemStack`.

The number of recipes per holder comes from the config: `TileEncoder.patternSlots`.

---

## 6. Points still to verify before coding

1. How to walk every node of an AE2UEL grid generically: `IGrid.getMachines(Class)` by known
   class, or a node walk through `IGridVisitor`. The generic walk is preferable: it catches
   unknown addons.
2. What `setPatternStack()` triggers exactly on the AE2 side: pattern republication, or plain
   storage. Check `HostHelperTilePackager` and the call to
   `postChange` / `MENetworkCraftingPatternChange`.
3. The upstream licence of PackagedAuto, for the compile dependency.
4. The behaviour of the `Packager Extension`: it shares the patterns of the neighbouring
   Packager (`InventoryPackager.updatePatternList()` scans the `TilePackagerExtension`).

---

## 7. Answers to the open points — verified on 2026-09-12

### 7.1 Where the Recipe Holder lives inside the machine

Upstream source, `1.12` branch of `TheLMiffy1111/PackagedAuto`:

```java
@Override
public ItemStack getPatternStack() {
    return inventory.getStackInSlot(10);
}

@Override
public void setPatternStack(ItemStack stack) {
    inventory.setInventorySlotContents(10, stack);
}
```

The holder therefore sits in **slot 10** of the Packager inventory. The insertion filter
requires it:

```java
case 10: return stack.getItem() instanceof IRecipeListItem
             || stack.getItem() instanceof IPackageItem;
```

### 7.2 Pattern republication is automatic

`InventoryPackager.setInventorySlotContents(10, ...)` calls `updatePatternList()`. That method
rebuilds the list, notifies the neighbouring `Packager Extension` blocks, then calls
`tile.hostHelper.postPatternChange()` when the world exists and is not remote.

`provideCrafting()` then republishes every pattern onto the grid:

```java
@Optional.Method(modid="appliedenergistics2")
@Override
public void provideCrafting(ICraftingProviderHelper craftingTracker) {
    if(hostHelper.isActive()) {
        for(IPackagePattern pattern : patternList) {
            craftingTracker.addCraftingOption(this, new PackageCraftingPatternHelper(pattern));
        }
    }
}
```

> **Implementation rule that follows.** After any recipe change, the terminal must **rewrite
> the stack** through `setPatternStack()`. Editing the NBT content of the holder in place,
> without rewriting the slot, does not call `updatePatternList()`. AE2 would then keep a stale
> view of the recipes.

This point was listed as "the most fragile of the project". It is now settled and documented.

### 7.3 Generic walk over the AE2 grid

`appeng.api.networking.IGrid` exposes:

```java
IReadOnlyCollection<Class<? extends IGridHost>> getMachinesClasses();
IMachineSet getMachines(Class<? extends IGridHost>);
IReadOnlyCollection<IGridNode> getNodes();
```

Discovery therefore happens without knowing any class in advance:

1. walk `getMachinesClasses()`;
2. keep only those where `IPackageProvidingMachine.class.isAssignableFrom(cls)`;
3. for each one, walk `getMachines(cls)` and read `node.getMachine()`.

This is preferable to `getNodes()`, which also visits every cable. Any future addon is caught
with no code change.

`IGridBlock.getLocation()` provides the position, and `getMachineRepresentation()` provides
the machine icon. Both are used directly to render the terminal rows.

### 7.4 The Packager Extension

`InventoryPackager.updatePatternList()` notifies the neighbouring `TilePackagerExtension`
blocks. The extension therefore does share the patterns of its Packager. Decision **D04**
holds.

### 7.5 Compilation verified

A temporary probe compiled, on 2026-09-12, against: `IGrid`, `IGridNode`,
`IPackageProvidingMachine`, `IRecipeListItem`, `RecipeTypeRegistry`, and the **internal**
classes `appeng.parts.reporting.AbstractPartTerminal` and
`appeng.items.tools.powered.powersink.AEBasePoweredItem`. Constraint **D20** is therefore
satisfied by the `flatDir` + `deobfProvided` setup.
