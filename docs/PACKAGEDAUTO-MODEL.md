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

---

## 7. Réponses aux points laissés ouverts — vérifiées le 2026-09-12

### 7.1 Où vit le Recipe Holder dans la machine

Source amont, branche `1.12` de `TheLMiffy1111/PackagedAuto` :

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

Le holder occupe donc l'**emplacement 10** de l'inventaire du Packager. Le filtre
d'insertion l'exige :

```java
case 10: return stack.getItem() instanceof IRecipeListItem
             || stack.getItem() instanceof IPackageItem;
```

### 7.2 La republication des patterns est automatique

`InventoryPackager.setInventorySlotContents(10, …)` appelle `updatePatternList()`.
Cette méthode reconstruit la liste, prévient les `Packager Extension` voisins, puis appelle
`tile.hostHelper.postPatternChange()` quand le monde existe et n'est pas distant.

`provideCrafting()` republie ensuite chaque pattern sur la grille :

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

> **Règle d'implémentation qui en découle.** Après toute modification d'une recette, le
> terminal doit **réécrire le stack** par `setPatternStack()`. Modifier le contenu NBT du
> holder en place, sans réécrire l'emplacement, n'appelle pas `updatePatternList()`. AE2
> garderait alors une vue périmée des recettes.

Ce point était classé « le plus fragile du projet ». Il est désormais résolu et documenté.

### 7.3 Parcours générique de la grille AE2

`appeng.api.networking.IGrid` expose :

```java
IReadOnlyCollection<Class<? extends IGridHost>> getMachinesClasses();
IMachineSet getMachines(Class<? extends IGridHost>);
IReadOnlyCollection<IGridNode> getNodes();
```

La découverte se fait donc sans connaître aucune classe à l'avance :

1. parcourir `getMachinesClasses()` ;
2. ne garder que celles où `IPackageProvidingMachine.class.isAssignableFrom(cls)` ;
3. pour chacune, parcourir `getMachines(cls)` et lire `node.getMachine()`.

C'est préférable à `getNodes()`, qui traverse aussi chaque câble. Tout addon futur est
capté sans modification du code.

`IGridBlock.getLocation()` fournit la position, et `getMachineRepresentation()` fournit
l'icône de la machine. Les deux servent directement à l'affichage des lignes du terminal.

### 7.4 Le Packager Extension

`InventoryPackager.updatePatternList()` prévient les `TilePackagerExtension` voisins.
L'extension partage donc bien les patterns de son Packager. La décision **D04** tient.

### 7.5 Compilation vérifiée

Une sonde temporaire a compilé, le 2026-09-12, contre : `IGrid`, `IGridNode`,
`IPackageProvidingMachine`, `IRecipeListItem`, `RecipeTypeRegistry`, et les classes
**internes** `appeng.parts.reporting.AbstractPartTerminal` et
`appeng.items.tools.powered.powersink.AEBasePoweredItem`. La contrainte **D20** est donc
satisfaite par le montage `flatDir` + `deobfProvided`.
