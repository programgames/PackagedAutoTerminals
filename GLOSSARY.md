# Glossary

The words of the domain, and the name that each word has in the code. Use these words in the
code, the documents and the screens. Some code names are older than this file: read the
Conflicts section before you name a new thing.

## Terms

| Term | Meaning | Name in the code |
|---|---|---|
| Terminal | The PackagedAuto Terminal part on an ME cable, or the Wireless PackagedAuto Terminal | `PartPatTerminal`, `ItemWirelessPatTerminal`, `ContainerPatTerminal`, `TerminalContext` |
| Editor | The screen that edits one recipe. It follows the layout of the Package Recipe Encoder | `ContainerPatEditor`, `GuiPatEditor`, `EditorInventory` |
| Provider | A PackagedAuto machine that carries a recipe holder and publishes patterns to AE2: Packager, Unpackager, Packaging Provider | `ProviderSnapshot`, `IPackageProvidingMachine` |
| Crafter | A machine that runs packages: Package Crafter, and the crafters of the addons. A crafter carries no recipe | `MachineSnapshot`, `IPackageCraftingMachine` |
| Machine | A provider or a crafter, when the difference does not matter | see Conflicts |
| Role | The job of a provider: `PACKAGER`, `UNPACKAGER`, `COMPLETE` (both jobs, the Packaging Provider), `UNKNOWN` | `ProviderRole` |
| Group | Providers that carry exactly the same recipes, or that carry the same group name | `ProviderPairing.Group` |
| Pair | The normal group: one Packager and one Unpackager | `Group.isPair()` |
| Group name | A name that the player gives to a group. Each provider of the group stores it | `GroupNames`, `ProviderSnapshot.customName` |
| Recipe holder | The Package Recipe Holder item. It carries the recipes of one provider, 20 at most | `ItemRecipeHolder`, `IRecipeListItem`, `RecipeWriter.maxRecipes()` |
| Blank holder | A recipe holder with no NBT. The terminal takes only this kind from the ME network | `NetworkItems`, `pullHolderFromNetwork` |
| Recipe | One entry of a recipe holder | `IRecipeInfo` |
| Recipe type | Processing, Crafting, and the types that the addons register | `IRecipeType` |
| Pattern | What a recipe publishes to AE2. Its output is a package | `IPackagePattern` |
| Package | The item that a Packager builds, and that a crafter or an Unpackager opens | `ItemPackage` (PackagedAuto) |
| Uneven recipe | A recipe that only one side of a pair carries. AE2 cannot run it | `Group.isComplete()`, `Group.unevenCount()` |
| Group state | The state that the chip of a group row shows. The first state that applies wins: `MISSING_ROLE`, `INACTIVE`, `EMPTY`, `NO_HOLDER`, `UNEVEN`, `HEALTHY` | `ProviderPairing.State`, `Group.state()` |
| Fix | What the button of a group row offers: `REPAIR` copies the recipes that one side lacks, `INSERT_HOLDERS` takes blank holders | `ProviderPairing.Fix`, `Group.fix()` |
| Patterns tab | The first tab of the terminal. It lists the groups and their recipes | `GuiPatTerminal.buildPatternLines` |
| Machines tab | The second tab. It lists the orphan recipes, then the crafters of the network | `GuiPatTerminal.buildMachineLines`, `PatConfig.machinesTab` |
| Orphan recipe | A recipe whose type no crafter on the ME network runs. It is a doubt: a crafter off the network can still run it | `CrafterTypes` |
| Holder mode | The editor works on two recipe holders in its slots, with no ME network (D43) | `ContainerPatEditor.isHolderMode()`, `PatConfig.portableEncoder` |
| Twin | The second recipe holder of holder mode. Sync copies the missing recipes both ways | `ContainerPatEditor.holders`, `syncHolders()` |
| Scaling | Multiply or divide every processing recipe of a group (D44) | `ContainerPatEditor.scaleAll()`, `RecipeScale` |
| Snapshot | The providers, the crafters and the Machines tab setting, sent from the server to the client | `PacketProviderList`, `ProviderSnapshot.writeAll()` |
| Second click | A gesture that acts only on its second click on the same target, within a short delay | `SecondClick` |
| Refusal | A message that says that an action wrote nothing, or only a part. The screens draw it in the warning colour | `Feedback.REFUSALS` |

## Conflicts

- **Machine.** The code uses it for providers (`Group.machines`, `ProviderSnapshot.machine`,
  `RecipeWriter.Result.changed`) and for crafters (`MachineSnapshot`,
  `ContainerPatTerminal.machines`, the Machines tab). Write "provider" or "crafter" in new code.
  Keep "machine" for the generic sense. The renames of `MachineSnapshot` and `Group.machines`
  wait in the audit backlog (C-AR-14).
- **Pattern.** The Patterns tab shows recipes. It copies the vocabulary of the AE2 Interface
  Terminal, the model of this mod: this is a product choice, and the tab keeps its name. In the
  code and the documents, "pattern" means `IPackagePattern` only. Say "recipe" for an entry of a
  recipe holder.
- **Holder.** Write "recipe holder" in full. The short "holder" is for code names and for the
  slot label of holder mode.
- **Encoder.** "Encoder" means the Package Recipe Encoder block of PackagedAuto. Holder mode uses
  that block as its icon, and its config key says `portableEncoder`. Say "holder mode" for the
  feature. The config key stays: a public config key does not change.
- **Snapshot.** `ProviderSnapshot` and `MachineSnapshot` describe one machine. "The snapshot"
  is the whole list that `PacketProviderList` carries.
- **Complete.** `ProviderRole.COMPLETE` is a provider that fills both roles.
  `Group.isComplete()` says that AE2 can run a recipe. Say "self-sufficient" for the role.
