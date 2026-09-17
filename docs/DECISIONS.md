# Approved decisions

Every line here was approved with the player. Date: 2026-09-12.

## Base decisions

| # | Decision | Reason |
|---|---|---|
| D01 | Name `PackagedAuto Terminals`, modid `packagedautoterminals`, package `fr.julien.packagedautoterminals` | the mod also provides a wired terminal, not only a wireless one |
| D02 | The terminal lists machines by the `instanceof IPackageProvidingMachine` criterion | that is the only block type carrying editable recipes; an exact, stable and extensible criterion |
| D03 | **Option B**: writable "Patterns" tab, plus a read-only "Machines" tab with a diagnostic of orphan recipes | the grid walk is already done; the diagnostic exists in no mod. **See revision R1** |
| D04 | The `Packager Extension` is shown as a line attached to its Packager | it shares the patterns of the neighbouring Packager |
| D05 | The server is the authority; the client sends intents | security, anti-cheat, consistency |
| D06 | No mixin as long as a public API is enough | a mixin breaks on every update of another mod |
| D07 | Every integration is optional, detected by modid | the mod must run without the addons |
| D08 | Delta synchronisation, sent in chunks | **Revised, see R2** |
| D09 | v1 = items. v2 = fluids and gases (`PackagedFluidCrafting`) | that addon extends the core through mixins; it needs a dedicated module |
| D10 | The terminal pulls a blank Recipe Holder from the ME network. Otherwise it refuses and explains | avoids forcing the player to carry one |
| D11 | "Encode from JEI" button in the terminal | the most useful day-to-day function |
| D12 | Strict respect of the AE2 security permissions | consistency with the native terminals |
| D13 | Delivery order: wired terminal, then wireless item, then AE2WUT integration | each step is testable on its own |
| D14 | Baubles support and infinite range card if the cost is low | comfort, not blocking |
| D15 | Compile against **official** AE2UEL, never against the player's local fork | the fork only adds fixes |
| D16 | `core` + `forge-1.12` structure | **Revised, see R3** |
| D17 | Languages `en_us` and `fr_fr` | |
| D18 | Minimal dev environment: AE2UEL + PackagedAuto + 3 addons + JEI | 40 s startup instead of 6 min |
| D19 | Mandatory step 0: decompile and document before coding | `docs/PACKAGEDAUTO-MODEL.md` |

## Revisions, after bytecode verification

| # | Revision | Evidence |
|---|---|---|
| **R1** | The "Machines" tab diagnostic **cannot be generic**. It needs a `recipe type → crafter class` table, written in each integration module. An unknown type shows "unrecognised" and stays silent, instead of lying | 1. `IPackageCraftingMachine.acceptPackage(…, boolean)` is **not** a simulation mode: the default method delegates to the 3-argument version, which actually runs the craft. 2. `IRecipeType.getRepresentation()` returns the **source station** of the craft (`Blocks.CRAFTING_TABLE`, `ModBlocks.blockBasicTable`), not the Package Crafter |
| **R2** | Replace chunk splitting with **simple incremental updates**, following the AE2 `ContainerInterfaceTerminal`. Measure at batch 2. Only add splitting if the measurement proves it necessary | our payload is far smaller than the one of `cell-terminal`, which handles thousands of items per cell |
| **R3** | **A single module to start with**, no `core`. The `core` module will appear at the 1.16 port, once we know what is truly shared | here, almost everything depends on Minecraft: `ItemStack`, `TileEntity`, the AE2 grid. An empty module costs friction on every build |
| **R4** | Load test under Cleanroom from batch 1, with the empty jar | a failure discovered at batch 7 would cost days. The player notes that the risk is low: every mod of the pack also runs on Forge, and MeatballCraft accepts both |

## Findings that become constraints

| # | Constraint | Evidence |
|---|---|---|
| **D20** | We compile against the **full deobfuscated jar** of AE2UEL, not against an API-only artifact | third-party terminals extend internal classes: `PartCellTerminal extends appeng.parts.reporting.AbstractPartDisplay`, `ItemWirelessCellTerminal extends appeng.items.tools.powered.powersink.AEBasePoweredItem` |
| **D21** | The AE2WUT integration is done **without a mixin**, through a public call, and the mode id must be **configurable** | `AE2UELWirelessUniversalTerminal.registryContainer(byte, GetGui)`, `registryGui(byte, GetGui)`, `ItemWUTBakedModel.regIcon(byte, ItemStack)`. `cell-terminal` makes its id configurable to avoid collisions between third-party mods |
| **D22** | The recipe editor must be a real `Container`, with ghost slots **indexed like the Encoder ones** | without that, `IRecipeType.getRecipeTransferMap(IRecipeLayout, String)` and the PackagedAuto JEI handles become unusable |
| **D23** | PackagedAuto is under the **MIT** licence | compile dependency and API reuse with no obstacle, with attribution |
| **D24** | No mod already covers this need on 1.12.2 | batch 0 research. `Extended Terminal` targets 1.20 and 1.21, and does not touch PackagedAuto |

## Rejected options

| Option | Reason for rejection |
|---|---|
| Also list the crafters in the patterns tab | they carry no recipe; dozens of empty lines on a large network |
| Embed the ME item panel in the terminal screen, in v1 | doubles the GUI work without serving the main need |
| Hard code the Basic to Ultimate and Extreme tiers in the mod core | `RecipeTypeRegistry` already provides them. Only the R1 diagnostic table is wired, and it lives in the integration modules |
| Probe a crafter with a dry `acceptPackage` call | the method actually runs the craft; no simulation mode exists |

## Decisions taken during the night of 12 to 13 September 2026

| # | Decision | Reason |
|---|---|---|
| D25 | **MIT** licence, plus a `NOTICE` file | same licence as PackagedAuto. No AE2 or PackagedAuto file is copied: we only compile against them |
| D26 | The crafting recipe is registered **in code**, not in JSON | AE2 items are told apart by their metadata; a JSON file would hard code it and break on the first renumbering |
| D27 | **Universal** JEI transfer handler, rather than one per category | it catches the addons that register their types after JEI is loaded |
| D28 | Moving a recipe holder goes **through the network**: remove, then take back | nothing can be lost. If the network refuses the item, the machine keeps it |
| D29 | Amounts go up to **4096**, not 64 | PackagedAuto writes large amounts through `MiscUtil.saveItemWithLargeCount`, and processing recipes need them |
| D30 | Snapshot comparison covers the **whole message** | comparing the providers alone hid every machine state change |
| D31 | A `checkLang` Gradle task breaks the build when the languages diverge | Minecraft falls back to English without a word: the divergence would be invisible |

## Questions still open

| # | Question |
|---|---|
| Q1 | URL of the GitHub repository of the player's AE2UEL fork, to check that the API has not moved |
| Q3 | Public or private repository? Future release on CurseForge? |

## D32 — The JEI transfer picks the narrowest type

**Evidence.** `javap -c` on `thelm/packagedauto/recipe/RecipeTypeProcessing.class` shows that
`getJEICategories()` goes through `MiscUtil.conditionalSupplier(() -> Loader.isModLoaded("jei"), …)`
and returns **every** JEI category when JEI is present. `RecipeTypeProcessingOrdered` and
`RecipeTypeProcessingPositioned` inherit that method without overriding it.

**Observed consequence.** The JEI "+" button on an Ultimate Table recipe did place the items,
but switched the type to "Positioned". Our `findType` returned the first type declaring the
category, hence a catch all, depending on the registry order.

**Decision.** `findType` keeps the type whose category list is the **shortest**. A type that
names two categories knows its domain; a type that names them all merely accepts. The rule
names no mod, so an unknown addon benefits from it too.

## D33 — AE2WUT has no extension interface: three injections, and nothing more

**Evidence.** Decompilation of `ae2wut-1.0.5.jar`, taken from the player's Cleanroom modpack.

| Method | Observed shape |
|---|---|
| `ItemWirelessUniversalTerminal.getAllMode()` | an `ArrayList` filled from 0 to 3, then 4 if `ae2fc`, 5 if `mekeng`, 6 to 9 if `ae2exttable` |
| `ItemWirelessUniversalTerminal.getWirelessName(int)` | a `tableswitch` from 1 to 9, with the translation keys hard coded |
| `ItemWirelessUniversalTerminal.canHandle(ItemStack)` | compares the item to its own instance |
| `AllWUTRecipe.getIngredient()` | a `HashMap` filled by hand, through the same presence tests |

The `registry` field exists, but it only serves to open the AE2 screen. It never feeds the
mode list. No registry, no extension point, no annotation.

**What is already generic, and must therefore not be touched.**

1. `WirelessUniversalTerminalHandler.onMouseEvent` computes its bound as `max(modes) + 1`,
   then walks forward until it finds a present mode. A mode 41 fits in with no change.
2. `DynamicUniversalRecipe.registerRecipes()` creates one recipe per entry of
   `AllWUTRecipe.itemList`.
3. `AllWUTRecipe.reciperRegister()` builds the "all in one" recipe from the same table.

**Decision.** Three injections, and only three.

| Injection | Effect |
|---|---|
| `getAllMode`, at return | our mode joins `allMode`, hence the "all in one" recipe and the wheel |
| `getWirelessName`, at head | the name of our terminal shows on the item and in its tooltip |
| `AllWUTRecipe.getIngredient`, at return | our wireless terminal becomes an ingredient, so an assembly recipe appears on its own |

**What does not go through a mixin, and why.**

Opening the screen lives in `Item.onItemRightClick`, a **Minecraft** method. Its name differs
between the development workspace and the shipped game. Injecting there would need a
remapping table, hence the Mixin annotation processor, hence a whole extra build chain. A
`PlayerInteractEvent.RightClickItem` listener does the same work, and it runs before the item
method.

**Why the target classes are named by their name, not by `X.class`.**

`ItemWirelessUniversalTerminal` carries MekEng, AE2FC and ae2exttable types in its signatures.
Naming it by class would force those mods onto the compile path, against rule 7. By name, no
dependency is needed: neither AE2WUT, nor its four satellites.

**Consequence on the build.** The Mixin annotation processor, which javac discovers on its own
in the MixinBooter jar, requires every target class to be resolvable. Otherwise it stops with
"Mixin target ... could not be found". The build therefore passes `-proc:none`. Both
injections do without it, because no target method carries a Minecraft type in its signature:
`()[I`, `(I)Ljava/lang/String;` and `()Ljava/util/Map;`.

**Loud failure, at the player's request.** Both injections carry `require = 1`. If AE2WUT
changes shape, the game stops with a clear message, rather than losing the mode silently.

**PITFALL avoided.** The listener **never** cancels the click on the client.
`PlayerControllerMP.processRightClick` returns as soon as the event is cancelled, and no
longer sends the packet to the server. The screen would never open.

**PITFALL avoided, second one.** The injections run during the registry events, which come
before the `preInit` of our mod. Forge only fills `PatConfig` at `preInit`. Reading
`wutModeId` without care would give the default value at registration, then the configured
value afterwards: two ids for a single terminal. `WutSupport.mode()` therefore forces the file
to be read, then keeps the result for the whole session.

## D34 — Cell Terminal, a verified model for the key and for AE2WUT

**Source.** `cell-terminal-1.6.7.jar`, decompiled from the player's Cleanroom modpack. The jar
now sits in `run/mods`, as a **model** and not as a dependency.

### How it declares a key

Three classes, and no mixin.

| Class | Role |
|---|---|
| `client.KeyBindings` | an enum of `KeyBinding`, plus `registerAll()` which calls `ClientRegistry.registerKeyBinding` |
| `client.KeyInputHandler` | listens to `InputEvent.KeyInputEvent`, tests `isPressed()`, then sends a packet |
| `network.PacketOpenWirelessTerminal` | the server looks for the item and opens the screen |

The opening key is declared as `KeyConflictContext.UNIVERSAL`, with `KeyModifier.SHIFT` and
code 25, hence Shift + P.

The server-side search order is worth copying:

1. the main inventory, for its own wireless terminal;
2. the offhand, slot 40;
3. the AE2WUT universal terminal, inventory then offhand;
4. Baubles, when the mod is present.

Each candidate is checked in the same order: link key, locatable security station, then
energy. Every call into AE2WUT or Baubles goes through `@Optional.Method(modid=...)`.

### What it reveals about AE2WUT

Cell Terminal uses **no mixin** for AE2WUT. It calls an API:

```java
AE2UELWirelessUniversalTerminal.instance.registryContainer(mode, factory);
AE2UELWirelessUniversalTerminal.instance.registryGui(mode, factory);
AllWUTRecipe.itemList.put(mode, ingredient);
ItemWUTBakedModel.regIcon(mode, iconStack);
```

**These methods do not exist in `ae2wut-1.0.5.jar`**, verified with `javap`. AE2WUT is at
version 1.2.10 on CurseForge. The API therefore arrived after 1.0.5.

**Evidence in game.** With `ae2wut-1.0.5` and `cell-terminal-1.6.7`, the dev client stops:

```
LoaderExceptionModCrash: Caught exception from Cell Terminal (cellterminal)
Caused by: NoClassDefFoundError: com/circulation/ae2wut/AE2UELWirelessUniversalTerminal$GetGui
    at com.cellterminal.integration.AE2WUTIntegration.registerContainerInternal
```

The player's real instance already carries `B:enableAE2WUT=false`: they hit this crash before
we did.

### Consequence on decision D33

D33 stays correct **for AE2WUT 1.0.5**, the player's version. It becomes wrong for recent
versions. If the player moves AE2WUT to 1.2.x, our two mixins must give way to four API calls,
and `-proc:none` can disappear from the build.

Our mode is 41; the Cell Terminal one is 11. No conflict.

## D35 — The opening key, modelled on ae2exttable and Cell Terminal

**Sources.** `ae2exttable-v1.0.8.jar` and `cell-terminal-1.6.7.jar`, decompiled from the
player's modpack.

**What both mods do.** An enum of `KeyBinding`, an `InputEvent.KeyInputEvent` listener, a
packet to the server. No mixin, no AE2 API.

| Mod | Context | Default key |
|---|---|---|
| ae2exttable | `UNIVERSAL` | **none**, code 0 |
| Cell Terminal | `UNIVERSAL` | Shift + P, code 25 |

**Decision.** Key **unbound** by default, like ae2exttable. A key bound by default would clash
with the four AE2 keys, with the Shift + P of Cell Terminal, or with another mod of the pack.

**Context `IN_GAME`, not `UNIVERSAL`.** `InputEvent.KeyInputEvent` only fires when no screen is
open. Declaring a wider context would report conflicts that cannot happen.

**Server-side search order**, taken from Cell Terminal: main inventory, then offhand, for our
terminal; then the same places for the universal terminal.

**A nuance that matters.** `check` returns **true** as soon as the item is the right candidate,
even when a check fails. Without that, the search would continue past an unlinked terminal, and
the player would never learn why nothing opens.

**Baubles is not supported.** Cell Terminal does it, but neither our terminal nor the AE2WUT one
implements `IBauble`: neither can occupy a Baubles slot without a third mod. Adding that path
would require Baubles on the compile path, against rule 7.

**The wheel and the key do not ask the same question.** `isOurMode` asks "is this universal
terminal set to our mode?", and serves the right click. `hasOurMode` asks "has it absorbed our
terminal?", and serves the key: the player must not have to turn the wheel first.

**PITFALL.** Writing `mode` into the NBT is not enough to switch a universal terminal. AE2WUT
stores the crafting grid of the mode being left through `nbtChangeB`, then restores the one of
the requested mode through `nbtChange`. Both are called through reflection, for lack of a
compile dependency, with a fallback to the plain write.

## D36 — The universal terminal icon, through a baked model wrapper

**Symptom.** Set to our mode, the Wireless Universal Terminal showed the purple and black
checkerboard of a missing model, with the name
`ae2exttable:item/wireless_ultimate_crafting_terminal` written over it.

**Cause, verified in the jar.** `assets/ae2wut/models/item/wireless_universal_terminal.json`
is an `item/generated` with ten overrides:

| Threshold | Model |
|---|---|
| 1 | `appliedenergistics2:item/wireless_crafting_terminal` |
| 2 | `appliedenergistics2:item/wireless_fluid_terminal` |
| 3 | `appliedenergistics2:item/wireless_pattern_terminal` |
| 4 | `ae2fc:item/wireless_fluid_pattern_terminal` |
| 5 | `mekeng:item/wireless_gas_terminal` |
| 6 to 9 | the four of `ae2exttable` |
| 114514 | `ae2wut:item/wireless_nova_terminal` |

A Minecraft override applies as soon as the value is **greater than or equal to** the
threshold, and `ItemOverrideList` walks its list **backwards**: the last one that matches
wins. Every mode above 9 therefore lands on the mode 9 override.

**This is not our fault, and it is not specific to our mode.** Cell Terminal, with its mode 11,
suffers the same fate on AE2WUT 1.0.5. Recent versions have replaced those overrides with a
baked model and an `ItemWUTBakedModel.regIcon` method, absent from 1.0.5: see decision D34.

**Decision.** Wrap the baked AE2WUT model at `ModelBakeEvent`. The wrapper delegates
everything, except its override list: on our mode it returns the model of our wireless
terminal; on any other mode it falls back to the original list, untouched.

**Rejected options.**

| Option | Why not |
|---|---|
| Pick a mode below 9 | they are all taken, and mode 0 is the AE2 one |
| Ship our own `assets/ae2wut/models/...` | we would overwrite the AE2WUT model, and its ten overrides would have to be rewritten |
| A third mixin | the Forge event is enough, and depends on nothing |

**PITFALL.** `handleItemState` must receive the **original** model, not the wrapper. Handing it
the wrapper would make the override lookup loop on itself.

**Nuance.** Rendering queries `showsOurMode`, which only looks at the current mode. A creative
item, set to our mode without having absorbed us, therefore carries our icon rather than a
missing model. The right click keeps `isOurMode`, which is stricter.

---

## D37 — The network channel name fits in 20 characters

**Symptom.** The player opens the terminal on a dedicated server. The client is kicked with
`io.netty.handler.codec.DecoderException: The received string length is longer than maximum
allowed (21 > 20)`. In single player, nothing happens.

**Verified cause.** On 1.12.2, `CPacketCustomPayload.readPacketData` reads the channel name
with `buf.readString(20)`. The client-to-server channel name therefore cannot exceed 20
characters. `Reference.MOD_ID` is `packagedautoterminals`, which is **21** characters long. The
first upstream packet of the mod cut the connection.

Single player escapes the defect: Forge links both sides through an `EmbeddedChannel`, which
serialises no string. The defect only shows on a dedicated server, or in LAN.

**Decision.** Add `Reference.CHANNEL = "pat_terminals"`, 13 characters long, and stop using
`MOD_ID` as the channel name. `MOD_ID` keeps its role everywhere else: registries, resources,
`mcmod.info`.

**PITFALL.** The channel name is part of the protocol. A client and a server carrying different
versions of the mod no longer talk to each other. Every update must go to both sides at once.

**Rule.** Every new network channel must fit in 20 characters. The other direction, server to
client, also allows 20 characters in `SPacketCustomPayload`.

---

## D38 — Machines group only when they carry exactly the same recipes

**Symptom.** On the player's real server, the terminal showed "Group of 6 machines" and
"Group of 4 machines", for 20 machines and 110 recipes. Pairs that were physically separate
appeared on one line.

**Verified cause.** The old criterion merged two machines as soon as they **shared one**
recipe, and the merge cascaded. One package common to three pairs was enough to solder the
six machines together:

| Machine | Recipes | Effect |
|---|---|---|
| Packager A | iron, gold | group 1 |
| Unpackager A | iron, gold | joins group 1 |
| Packager B | copper, **gold** | joins group 1, through "gold" |
| Unpackager B | copper, gold | joins group 1 |
| Packager C | tin, **copper** | joins group 1, through "copper" |
| Unpackager C | tin, copper | joins group 1 |

The test bench never showed the defect: two pairs there never share a recipe.

**Decision.** A machine joins a group only when it carries **exactly** the recipes of that
group, no more and no fewer. No cascade is then possible: two groups never hold the same set,
because the second machine would already have joined the first one.

An empty machine never matches by recipe. Otherwise every empty machine of the network would
land in the same group. Empty machines keep going through `mergeLonePartners` and
`mergeEmptyPair`, which only merge when the choice is certain.

**Consequence, accepted on purpose.** A pair whose two sides no longer carry the same recipes
splits into two rows. Two cases must be told apart:

1. One side becomes **empty**. `mergeLonePartners` joins it again, so the pair stays on one
   line and the recipe is still flagged in red. This is the common case, and test E1 is
   unchanged.
2. One side keeps **other** recipes. The two rows separate. The player joins them again by
   naming them, which is the explicit link of `mergeNamed`.

**Rejected option.** Merging when one set contains the other. It keeps case 2 together, but it
brings back a cascade as soon as a machine carries the union of two others.

---

## D39 — The search reads the outputs, and the machine sits on the group row

Two changes asked for after the first run on the real server.

### The search reads the produced items only

**Symptom.** Typing `elite` returned "ME Interface", "Molecular Assembler" and "Ultimate
Crafting Table". None of them is an Elite item.

**Cause.** `hasText` and `hasMod` walked `allStacks`, which held the outputs **and** the
inputs. Every recipe that merely consumes an Elite part matched.

**Decision.** Both read `outputStacks`, hence the produced items only. The player looks for
the recipe that makes an item, not for the list of its consumers.

**Rejected option.** A fourth prefix for the ingredients, such as `>elite`. Three prefixes are
already enough to learn.

### The crafting machine moves to the group row

**Reason given by the player.** On the network the crafting machine always sits next to the
Unpackager of the group. Its place is therefore the group row, not each recipe row.

**Decision.** The group row shows the machine icon left of the pin, and only when every recipe
that **needs** a machine names the same one. Two different machines in one group show nothing:
the row would otherwise state something false.

The recipes whose type needs no machine, such as `processing`, are left out of the
comparison. They target no machine, so they cannot disagree with one.

**Consequence.** The recipe rows get the type name back on their right, which is what they
carried before the icon arrived. The tooltip of the group row names the machine, and turns red
when it is absent from the network.

### The Patterns button was cut

It started at 2 pixels from the top of the screen. The panel bevel takes the first three. The
button now starts at 4.

### Drag and drop from JEI: one slot at a time

**Need.** The **+** button of JEI fills the whole grid. It says nothing about a single
ingredient the player wants to place by hand, and it cannot place a fluid.

**Decision.** A second JEI entry point, `IGhostIngredientHandler`, registered on
`GuiPatEditor` only. It offers one target per **enabled** slot, and the drop sends
`PacketEditorGhost`. The server checks the slot, like every other write (D05).

**Why the exact class.** AE2UEL already registers a ghost handler on `AEBaseGui`, from which
our screen inherits. JEI resolves a handler by **exact class first**, and only then walks the
registered classes with `isInstance`. Registering `GuiPatEditor.class` therefore wins, whatever
the load order of the two mods. Read in `GuiScreenHelper.getGhostIngredientHandler`.

**Fluids.** The editor stores `ItemStack` only. A dragged fluid becomes the bucket that holds
it, through `FluidUtil.getFilledBucket`. A fluid with no bucket offers **no** target, so the
player never drops into nothing. Real fluid slots stay with `PackagedFluidCrafting`, hence
with version 2 (D09).

### The left click opens the amount panel

**Need given by the player.** In the editor, the left click emptied the slot. The Package
Recipe Encoder opens a small screen instead, where the amount is typed.

**Read in the jar**, `packagedauto-1.0.24.73`:

- `GuiContainerTileBase.handleMouseClick` opens `GuiItemAmountSpecifying` under five
  conditions: `mouseButton == 0`, click type other than `QUICK_MOVE`, empty hand, slot of
  class `SlotFalseCopy` and enabled, slot not empty.
- `GuiItemAmountSpecifying.getIncrements` returns `{1, 10, 64}`, and `getMultipliers`
  returns `{2, 3, 5}`. The six buttons add or subtract a step, and become a multiplication or
  a division while Shift is held, in `GuiAmountSpecifying.onAmountButtonClicked`.
- `onOkButtonPressed` clamps to `[0, maxAmount]`, then sends the stack with that count.
  **Zero therefore empties the slot.**
- `GuiEncoder.getItemAmountSpecificationLimit` caps the inputs at the maximum stack size of
  the item, and the outputs at one billion.

**Decision.** The same gesture, and the same six buttons. Three differences, on purpose:

1. The panel is drawn **inside** the editor screen, and is not a screen of its own. A screen
   of its own would need a container and a second GUI handler, for no gain.
2. The upper bound is `MAX_SLOT_COUNT`, hence 4096, for every slot. The project already chose
   that bound for the wheel, and processing recipes need it on the **inputs**, which the
   Encoder caps at 64.
3. The middle click keeps opening the same panel. That gesture existed before this change.

**Consequence.** `setSlotCount` now empties the slot on zero. `changeSlotCount`, which the
wheel uses, keeps its floor of one: scrolling down must never delete an item by surprise.

**Why the buttons are drawn by hand.** `GuiContainer` draws `buttonList` **before** the items
of the slots. A panel built from `GuiButton` would sit under the grid.

### Keeping the ratio of a recipe

**Need given by the player.** Changing the output from 1 to 10 should multiply the inputs by
ten, and the other way round.

**Read in the jar.** PackagedAuto has nothing of the sort. **AE2UEL does**, in the Expanded
Processing Pattern Terminal: `ContainerPatternEncoder.multiply(int)` and `divide(int)`, behind
the buttons `x2`, `x3`, `/2` and `/3`. Its `divide` walks **every** filled input and output,
and returns without touching anything as soon as one count is not a multiple. The tooltip of
its `+1` button even warns: `DOES NOT MAINTAIN INGREDIENT RATIO`.

**Decision.** A tick box in the amount panel, and not four fixed buttons. The ratio then comes
from the amount the player types, so it is not limited to two and three.

**Refusal rule.** The one of AE2UEL, because it is the honest one: the server tests every slot
first, and writes nothing at all when a single result is not a whole number. The player reads
why, in red. A half scaled recipe would be worse than no change.

**Scope.** Every slot the type enables, inputs and outputs alike. The code is the same, and a
player who fixes an input needs the outputs to follow just as much.

**Reminder.** A `crafting` recipe computes its output, so `canSetOutput()` is false and the
output slots are not editable. The box then serves the inputs only.

**The state is static.** The choice survives the closing of the editor. Ticking the box again
for every slot of the same recipe would be busy work.

### The pin, and a mark that is seen

**Reason given by the player.** The eye said nothing about what the button does, and the mark
in the world was too short and too discreet.

**The icon.** A map pin replaces the eye. An eye means "look at this"; a pin is the mark every
map uses for "the thing you look for is here". It is drawn by `draw_pin` in
`tools/make_gui_texture.py`: a round head with a hole, a tail down to a point, and a one pixel
ground shadow. Without the shadow the drawing floated.

**The mark.** Three layers, from the closest reading to the most distant:

1. a tinted cube, `RenderGlobal.renderFilledBox`, which shows the machine even against a wall
   of the same colour;
2. the outline, four pixels wide instead of three, which gives the exact block;
3. a beam sixty-four blocks high, which is seen from across the base. Without it the player
   had to already look the right way to find the mark.

`disableCull` goes with the beam: without it the beam vanishes as soon as the player stands
inside it, because the game would only keep the faces turned away.

**The duration.** Fifteen seconds, and a config entry, `highlightSeconds`, from three to a
hundred and twenty. Five seconds ran out while the player was still turning around.

### The amount panel takes the look of the game

**Reason given by the player.** The panel did not look like Minecraft, and the tick box showed
nothing.

**The dead tick. PITFALL.** `drawRect` was given `COLOR_FIELD_TEXT`, which is `0xE0E0E0`. That
value carries **no alpha byte**, so the alpha is zero and the rectangle is fully transparent.
The box did toggle; it just painted nothing. Every colour passed to `drawRect` must start with
`FF`. The text of a `drawString` escapes the trap, because `FontRenderer` forces an opaque
alpha when the four high bits are zero.

**The look.** Three changes, all of them vanilla:

1. the plate is grey, `0xC6C6C6`, with a black outline, white on the top and left edges and
   dark grey on the bottom and right ones. That is the relief of every screen of the game;
2. the buttons are real `GuiButton` objects, twenty pixels tall, with their texture, their
   hover state and their click sound. They stay **out** of `buttonList`, because the screen
   draws that list before the items of the slots, and the panel would sit under the grid.
   They are drawn by hand, at the end of `drawScreen`;
3. the title line carries the item and its name, and the tick box is drawn like a slot: dark
   edge, grey fill, green tick.

**Consequence.** The text of the panel is dark, `0x404040`, because the plate is light. The
amount field keeps its dark recess: that is what the rest of the mod already does.
