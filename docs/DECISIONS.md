# Décisions validées

Toutes ces lignes ont été validées avec le joueur. Date : 2026-09-12.

## Décisions de base

| # | Décision | Motif |
|---|---|---|
| D01 | Nom `PackagedAuto Terminals`, modid `packagedautoterminals`, package `fr.julien.packagedautoterminals` | le mod fournit aussi un terminal câblé, pas seulement sans fil |
| D02 | Le terminal liste les machines par le critère `instanceof IPackageProvidingMachine` | c'est le seul type de bloc qui porte des recettes modifiables ; critère exact, stable et extensible |
| D03 | **Option B** : onglet « Patterns » en écriture, plus onglet « Machines » en lecture seule avec diagnostic des recettes orphelines | le parcours de grille est déjà fait ; le diagnostic n'existe dans aucun mod. **Voir la révision R1** |
| D04 | Le `Packager Extension` s'affiche en ligne rattachée à son Packager | il partage les patterns du Packager voisin |
| D05 | Le serveur est l'autorité ; le client envoie des intentions | sécurité, anti-triche, cohérence |
| D06 | Pas de mixin tant qu'une API publique suffit | un mixin casse à chaque mise à jour d'un autre mod |
| D07 | Chaque intégration est optionnelle, détectée par modid | le mod doit tourner sans les addons |
| D08 | Synchronisation par delta, envoi découpé en chunks | **Révisé, voir R2** |
| D09 | v1 = items. v2 = fluides et gaz (`PackagedFluidCrafting`) | cet addon étend le core par mixins ; il demande un module dédié |
| D10 | Le terminal tire un Recipe Holder vierge du réseau ME. Sinon il refuse et l'explique | évite d'obliger le joueur à en porter un |
| D11 | Bouton « encoder depuis JEI » dans le terminal | fonction la plus utile au quotidien |
| D12 | Respect strict des permissions de sécurité AE2 | cohérence avec les terminaux natifs |
| D13 | Ordre de livraison : terminal câblé, puis item sans fil, puis intégration AE2WUT | chaque étape est testable seule |
| D14 | Support Baubles et carte de portée infinie si le coût est faible | confort, non bloquant |
| D15 | Compilation contre AE2UEL **officiel**, jamais contre le fork local du joueur | le fork n'ajoute que des correctifs |
| D16 | Structure `core` + `forge-1.12` | **Révisé, voir R3** |
| D17 | Langues `en_us` et `fr_fr` | |
| D18 | Environnement de dev minimal : AE2UEL + PackagedAuto + 3 addons + JEI | démarrage en 40 s au lieu de 6 min |
| D19 | Étape 0 obligatoire : décompiler et documenter avant de coder | `docs/PACKAGEDAUTO-MODEL.md` |

## Révisions, après vérification du bytecode

| # | Révision | Preuve |
|---|---|---|
| **R1** | Le diagnostic de l'onglet « Machines » **ne peut pas être générique**. Il lui faut une table `type de recette → classe de crafter`, écrite dans chaque module d'intégration. Un type inconnu affiche « non reconnu » et se tait, au lieu de mentir | 1. `IPackageCraftingMachine.acceptPackage(…, boolean)` **n'est pas** un mode simulation : la méthode par défaut délègue à la version à 3 arguments, qui exécute réellement. 2. `IRecipeType.getRepresentation()` renvoie la **station d'origine** du craft (`Blocks.CRAFTING_TABLE`, `ModBlocks.blockBasicTable`), pas le Package Crafter |
| **R2** | Remplacer le découpage en chunks par des **mises à jour incrémentales simples**, sur le modèle de `ContainerInterfaceTerminal` d'AE2. Mesurer au lot 2. N'ajouter le découpage que si la mesure le prouve | notre charge utile est bien plus faible que celle de `cell-terminal`, qui gère des milliers d'items par cellule |
| **R3** | **Un seul module au départ**, pas de `core`. Le module `core` naîtra au portage 1.16, quand on saura ce qui est réellement commun | ici, presque tout dépend de Minecraft : `ItemStack`, `TileEntity`, grille AE2. Un module vide coûte de la friction à chaque build |
| **R4** | Test de chargement sous Cleanroom dès le lot 1, avec le jar vide | un échec découvert au lot 7 coûterait des jours. Le joueur précise que le risque est faible : tous les mods du pack tournent aussi sous Forge, et MeatballCraft accepte les deux |

## Découvertes qui deviennent des contraintes

| # | Contrainte | Preuve |
|---|---|---|
| **D20** | On compile contre le **jar complet déobfusqué** d'AE2UEL, pas contre un artefact d'API seul | les terminaux tiers étendent des classes internes : `PartCellTerminal extends appeng.parts.reporting.AbstractPartDisplay`, `ItemWirelessCellTerminal extends appeng.items.tools.powered.powersink.AEBasePoweredItem` |
| **D21** | L'intégration AE2WUT se fait **sans mixin**, par appel public, et l'identifiant de mode doit être **configurable** | `AE2UELWirelessUniversalTerminal.registryContainer(byte, GetGui)`, `registryGui(byte, GetGui)`, `ItemWUTBakedModel.regIcon(byte, ItemStack)`. `cell-terminal` rend son identifiant configurable pour éviter les collisions entre mods tiers |
| **D22** | L'éditeur de recette doit être un vrai `Container`, avec des slots fantômes **indexés comme ceux de l'Encoder** | sans cela, `IRecipeType.getRecipeTransferMap(IRecipeLayout, String)` et les poignées JEI de PackagedAuto deviennent inutilisables |
| **D23** | PackagedAuto est sous licence **MIT** | dépendance de compilation et réutilisation de l'API sans obstacle, avec attribution |
| **D24** | Aucun mod ne couvre déjà ce besoin sur 1.12.2 | recherche du lot 0. `Extended Terminal` cible 1.20 et 1.21, et ne touche pas à PackagedAuto |

## Options écartées

| Option | Motif du rejet |
|---|---|
| Lister aussi les crafters dans l'onglet des patterns | ils ne portent aucune recette ; des dizaines de lignes vides sur un grand réseau |
| Intégrer le panneau d'items ME dans la fenêtre du terminal, en v1 | double le travail de GUI sans servir le besoin principal |
| Coder en dur les niveaux Basic → Ultimate et Extreme dans le cœur du mod | `RecipeTypeRegistry` les fournit déjà. Seule la table de diagnostic R1 est câblée, et elle vit dans les modules d'intégration |
| Tester un crafter par appel à blanc de `acceptPackage` | la méthode exécute réellement le craft ; aucun mode simulation n'existe |

## Décisions prises pendant la nuit du 12 au 13 septembre 2026

| # | Décision | Motif |
|---|---|---|
| D25 | Licence **MIT**, plus un fichier `NOTICE` | même licence que PackagedAuto. Aucun fichier d'AE2 ni de PackagedAuto n'est copié : nous compilons seulement contre eux |
| D26 | La recette de fabrication est enregistrée **par le code**, pas en JSON | les objets d'AE2 se distinguent par leur métadonnée ; un JSON l'écrirait en dur et casserait à la moindre renumérotation |
| D27 | Gestionnaire de transfert JEI **universel**, plutôt qu'un par catégorie | il capte les addons qui enregistrent leurs types après le chargement de JEI |
| D28 | Déplacer un porte-recettes se fait **par le réseau** : retrait, puis reprise | rien ne peut se perdre. Si le réseau refuse l'objet, la machine le garde |
| D29 | Les quantités vont jusqu'à **4096**, pas 64 | PackagedAuto écrit de grandes quantités par `MiscUtil.saveItemWithLargeCount`, et les recettes de traitement en ont besoin |
| D30 | La comparaison d'instantané porte sur le **message entier** | comparer les seuls fournisseurs masquait tout changement d'état des machines |
| D31 | Une tâche Gradle `checkLang` casse le build si les langues divergent | Minecraft retombe en anglais sans rien signaler : la divergence serait invisible |

## Questions encore ouvertes

| # | Question |
|---|---|
| Q1 | URL du dépôt GitHub du fork AE2UEL du joueur, pour vérifier que l'API n'a pas bougé |
| Q3 | Dépôt public ou privé ? Publication future sur CurseForge ? |
