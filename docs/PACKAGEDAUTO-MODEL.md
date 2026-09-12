# Modèle de données de PackagedAuto — vérifié

> **Source des preuves** : `javap -p` sur les jars de l'instance
> `H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`, le 2026-09-12.
> Versions : PackagedAuto `1.0.24.73`, PackagedExCrafting `1.0.3.33`,
> PackagedAvaritia `1.0.3.25`, PackagedFluidCrafting `1.0.0.3`, PackagingProvider `1.0.0.2`.
> Auteur amont : TheLMiffy1111, package racine `thelm.packagedauto`.

---

## 1. Le flux de jeu

```
  Package Recipe Encoder  (bloc, hors réseau ME)
     │  encode jusqu'à N recettes d'un même type
     ▼
  Package Recipe Holder  (ITEM)
     │  inséré à la main dans une machine « fournisseur »
     ▼
  Packager / Unpackager / Packaging Provider
     │  publient les patterns sur la grille ME (ICraftingProvider)
     ▼
  AE2 planifie ──► Packager fabrique des Recipe Packages
     │
     ▼
  Unpackager distribue ──► crafters : Package Crafter, Basic → Ultimate,
                            Combination, Ender, Extreme (Avaritia)
```

Point clé : **la recette vit dans un item**, le Recipe Holder. Elle ne vit pas dans le bloc.
Le bloc fournisseur ne fait que porter cet item et republier les patterns.

---

## 2. L'API publique : `thelm.packagedauto.api`

| Type | Signature utile | Rôle |
|---|---|---|
| `IRecipeListItem` | `getRecipeList(ItemStack)` / `setRecipeList(ItemStack, IRecipeList)` | implémenté par `ItemRecipeHolder` |
| `IRecipeList` | `getRecipeList(): List<IRecipeInfo>` / `setRecipeList(List)` / NBT | le contenu du holder |
| `IRecipeInfo` | `getRecipeType()`, `getInputs()`, `getOutputs()`, `getPatterns()`, `getEncoderStacks()`, `generateFromStacks(...)`, `isValid()` | **une recette** |
| `IRecipeType` | `getName(): ResourceLocation`, `getEnabledSlots(): IntSet`, `canSetOutput()`, `getJEICategories()`, `getSlotColor(int)`, `getNewRecipeInfo()` | **le type**, décrit la grille d'édition |
| `RecipeTypeRegistry` | `getRegistry(): NavigableMap<ResourceLocation, IRecipeType>`, `getId`, `getRecipeType`, `getNextRecipeType` | registre global extensible |
| `IPackagePattern` | `getRecipeInfo()`, `getIndex()`, `getInputs()`, `getOutput()` | un pattern AE2 dérivé d'une recette |
| `IPackageProvidingMachine` | `getPatternStack()` / `setPatternStack(ItemStack)` | **machine qui porte un holder** |
| `IPackageCraftingMachine` | `acceptPackage(...)`, `isBusy()` | **machine qui exécute** |
| `ISettingsCloneable` | copie de configuration entre machines | hors périmètre v1 |
| `MiscUtil` | `writeRecipeToNBT`, `readRecipeFromNBT`, `writeRecipeListToNBT`, `readRecipeListFromNBT`, `getPatternHelper`, `condenseStacks`, `recipeEquals`, `recipeHashCode`, `arePatternsDisjoint` | **boîte à outils de sérialisation prête à l'emploi** |

`MiscUtil` est décisif : la sérialisation des recettes est déjà écrite et publique. Le
terminal n'a pas à réinventer le format NBT.

---

## 3. Qui porte des recettes, qui les exécute

| Bloc | `IPackageProvidingMachine` | `ICraftingProvider` | `IPackageCraftingMachine` | Champ interne |
|---|:---:|:---:|:---:|---|
| Packager | ✅ | ✅ | ❌ | `List<IPackagePattern> patternList` |
| Unpackager | ✅ | ✅ | ❌ | `List<IRecipeInfo> recipeList` |
| Packaging Provider | ✅ | ✅ | ❌ | `List<IRecipeInfo> recipeList` |
| Package Crafter | ❌ | ❌ | ✅ | `currentRecipe`, volatil |
| Basic → Ultimate Crafter | ❌ | ❌ | ✅ | `currentRecipe`, volatil |
| Combination / Ender Crafter | ❌ | ❌ | ✅ | volatil |
| Extreme Crafter (Avaritia) | ❌ | ❌ | ✅ | volatil |
| Positioned Package Distributor | ❌ | ❌ | ✅ | positions + marqueurs |
| Package Crafting Machine Proxy | ❌ | ❌ | ✅ | une cible |
| Package Recipe Encoder | ❌ | ❌ | ❌ | **hors grille ME** |

**Conséquence majeure** : le niveau du craft (Basic, Advanced, Elite, Ultimate, Extreme…)
n'est **pas** une propriété de la machine. C'est une propriété du **type de recette**,
porté par `IRecipeInfo.getRecipeType()`. Le crafter correspondant se contente d'accepter le
package.

---

## 4. Les types de recettes présents dans l'instance

| Mod | Types enregistrés |
|---|---|
| PackagedAuto | `Crafting`, `Processing`, `ProcessingOrdered`, `ProcessingPositioned` |
| PackagedExCrafting | `Basic`, `Advanced`, `Elite`, `Ultimate`, `Combination`, `Ender` |
| PackagedAvaritia | `Extreme` |
| PackagedFluidCrafting | étend le core **par mixins** : fluides et gaz |

Le terminal ne code aucun de ces types en dur. Il lit `RecipeTypeRegistry.getRegistry()`.
Tout addon futur apparaît donc sans modification du code.

`IRecipeType.getEnabledSlots()` donne la forme de la grille d'édition. `getSlotColor(int)`
donne la couleur de chaque emplacement. `canSetOutput()` dit si la sortie est éditable.
Ces trois méthodes suffisent à construire un éditeur générique.

---

## 5. Le bloc Encoder, à répliquer dans le terminal

`TileEncoder` expose : `patternInventories`, `patternIndex`, `setPatternIndex(int)`,
`saveRecipeList(boolean)`, `loadRecipeList(boolean, boolean)`, et le champ statique
`disabledRecipeTypes`.

Ses paquets réseau montrent les actions à reproduire : `PacketSetRecipe`,
`PacketSaveRecipeList`, `PacketLoadRecipeList`, `PacketCycleRecipeType`,
`PacketSetPatternIndex`, `PacketSetItemStack`.

Le nombre de recettes par holder vient de la configuration : `TileEncoder.patternSlots`.

---

## 6. Points encore à vérifier avant de coder

1. Comment parcourir tous les nœuds d'une grille AE2UEL de façon générique :
   `IGrid.getMachines(Class)` par classe connue, ou parcours des nœuds via `IGridVisitor`.
   Le parcours générique est préférable : il capte les addons inconnus.
2. Ce que `setPatternStack()` déclenche exactement côté AE2 : republication des patterns,
   ou simple stockage. Vérifier `HostHelperTilePackager` et l'appel à
   `postChange` / `MENetworkCraftingPatternChange`.
3. La licence amont de PackagedAuto, pour la dépendance de compilation.
4. Le comportement du `Packager Extension` : il partage les patterns du Packager voisin
   (`InventoryPackager.updatePatternList()` scanne les `TilePackagerExtension`).
