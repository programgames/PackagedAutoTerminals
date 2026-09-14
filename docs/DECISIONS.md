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

## D32 — Le transfert JEI choisit le type le plus étroit

**Preuve.** `javap -c` sur `thelm/packagedauto/recipe/RecipeTypeProcessing.class` montre que
`getJEICategories()` passe par `MiscUtil.conditionalSupplier(() -> Loader.isModLoaded("jei"), …)`
et rend **toutes** les catégories de JEI quand JEI est présent. `RecipeTypeProcessingOrdered`
et `RecipeTypeProcessingPositioned` héritent de cette méthode sans la redéfinir.

**Conséquence observée.** Le bouton « + » de JEI sur une recette de l'Ultimate Table plaçait
bien les objets, mais basculait le type sur « Positioned ». Notre `findType` rendait le
premier type déclarant la catégorie, donc un fourre-tout, selon l'ordre du registre.

**Décision.** `findType` retient le type dont la liste de catégories est la **plus courte**.
Un type qui nomme deux catégories connaît son domaine ; un type qui les nomme toutes se
contente d'accepter. La règle ne cite aucun mod, donc un addon inconnu en bénéficie aussi.

## D33 — AE2WUT n'a aucune interface d'extension : trois greffes, et rien de plus

**Preuve.** Décompilation de `ae2wut-1.0.5.jar`, tirée du modpack Cleanroom du joueur.

| Méthode | Forme observée |
|---|---|
| `ItemWirelessUniversalTerminal.getAllMode()` | une `ArrayList` remplie de 0 à 3, puis 4 si `ae2fc`, 5 si `mekeng`, 6 à 9 si `ae2exttable` |
| `ItemWirelessUniversalTerminal.getWirelessName(int)` | un `tableswitch` de 1 à 9, avec les clés de traduction écrites en dur |
| `ItemWirelessUniversalTerminal.canHandle(ItemStack)` | compare l'objet à sa propre instance |
| `AllWUTRecipe.getIngredient()` | une `HashMap` remplie à la main, par les mêmes tests de présence |

Le champ `registry` existe, mais il ne sert qu'à ouvrir la fenêtre d'AE2. Il n'alimente
jamais la liste des modes. Aucun registre, aucun point d'extension, aucune annotation.

**Ce qui est déjà générique, et qu'il ne faut donc pas toucher.**

1. `WirelessUniversalTerminalHandler.onMouseEvent` calcule sa borne par
   `max(modes) + 1`, puis avance jusqu'à trouver un mode présent. Un mode 41 y entre sans
   rien changer.
2. `DynamicUniversalRecipe.registerRecipes()` crée une recette par entrée de
   `AllWUTRecipe.itemList`.
3. `AllWUTRecipe.reciperRegister()` bâtit la recette « tout en un » à partir de la même
   table.

**Décision.** Trois greffes, et seulement trois.

| Greffe | Effet |
|---|---|
| `getAllMode`, au retour | notre mode rejoint `allMode`, donc la recette « tout en un » et la molette |
| `getWirelessName`, à l'entrée | le nom de notre terminal s'affiche sur l'objet et dans son infobulle |
| `AllWUTRecipe.getIngredient`, au retour | notre terminal sans fil devient un ingrédient, donc une recette d'assemblage naît toute seule |

**Ce qui ne passe pas par un mixin, et pourquoi.**

L'ouverture de la fenêtre vit dans `Item.onItemRightClick`, une méthode **de Minecraft**.
Son nom diffère entre le poste de développement et le jeu publié. Y greffer quelque chose
exigerait une table de remappage, donc le processeur d'annotations de Mixin, donc toute une
chaîne de compilation de plus. Un écouteur de `PlayerInteractEvent.RightClickItem` fait le
même travail, et il passe avant la méthode de l'objet.

**Pourquoi les classes visées sont désignées par leur nom, et non par `X.class`.**

`ItemWirelessUniversalTerminal` porte dans ses signatures des types de MekEng, d'AE2FC et
d'ae2exttable. La citer par sa classe obligerait à mettre ces mods sur le chemin de
compilation, contre la règle 7. Par son nom, aucune dépendance n'est nécessaire : ni AE2WUT,
ni ses quatre satellites.

**Conséquence sur le build.** Le processeur d'annotations de Mixin, que javac découvre tout
seul dans le jar de MixinBooter, exige que chaque classe visée soit résoluble. Il s'arrête
sinon sur « Mixin target ... could not be found ». Le build passe donc `-proc:none`. Les
deux greffes s'en passent, car aucune méthode visée n'a de type de Minecraft dans sa
signature : `()[I`, `(I)Ljava/lang/String;` et `()Ljava/util/Map;`.

**Échec bruyant, à la demande du joueur.** Les deux greffes portent `require = 1`. Si AE2WUT
change de forme, le jeu s'arrête avec un message clair, plutôt que de perdre le mode en
silence.

**PIÈGE évité.** L'écouteur n'annule **jamais** le clic côté client.
`PlayerControllerMP.processRightClick` sort dès que l'événement est annulé, et n'envoie plus
le paquet au serveur. La fenêtre ne s'ouvrirait jamais.

**PIÈGE évité, second.** Les greffes tournent pendant les événements de registre, qui
précèdent le `preInit` de notre mod. Or Forge ne remplit `PatConfig` qu'au `preInit`. Lire
`wutModeId` sans précaution donnerait la valeur par défaut à l'enregistrement, puis la
valeur réglée ensuite : deux identifiants pour un seul terminal. `WutSupport.mode()` force
donc la lecture du fichier, puis retient le résultat pour toute la session.

## D34 — Cell Terminal, modèle vérifié pour la touche et pour AE2WUT

**Source.** `cell-terminal-1.6.7.jar`, décompilé depuis le modpack Cleanroom du joueur. Le
jar est désormais dans `run/mods`, comme **modèle** et non comme dépendance.

### Comment il déclare une touche

Trois classes, et aucun mixin.

| Classe | Rôle |
|---|---|
| `client.KeyBindings` | une énumération de `KeyBinding`, plus `registerAll()` qui appelle `ClientRegistry.registerKeyBinding` |
| `client.KeyInputHandler` | écoute `InputEvent.KeyInputEvent`, teste `isPressed()`, puis envoie un paquet |
| `network.PacketOpenWirelessTerminal` | le serveur cherche l'objet et ouvre la fenêtre |

La touche d'ouverture est déclarée en `KeyConflictContext.UNIVERSAL`, avec `KeyModifier.SHIFT`
et le code 25, donc Maj + P.

L'ordre de recherche côté serveur mérite d'être copié :

1. l'inventaire principal, pour son propre terminal sans fil ;
2. la main gauche, emplacement 40 ;
3. le terminal universel d'AE2WUT, inventaire puis main gauche ;
4. les Baubles, si le mod est présent.

Chaque candidat est vérifié dans le même ordre : clé de liaison, station de sécurité
localisable, puis énergie. Chaque appel vers AE2WUT ou Baubles passe par
`@Optional.Method(modid=...)`.

### Ce qu'il révèle sur AE2WUT

Cell Terminal n'utilise **aucun mixin** pour AE2WUT. Il appelle une API :

```java
AE2UELWirelessUniversalTerminal.instance.registryContainer(mode, factory);
AE2UELWirelessUniversalTerminal.instance.registryGui(mode, factory);
AllWUTRecipe.itemList.put(mode, ingredient);
ItemWUTBakedModel.regIcon(mode, iconStack);
```

**Ces méthodes n'existent pas dans `ae2wut-1.0.5.jar`**, vérifié au `javap`. AE2WUT en est à
la version 1.2.10 sur CurseForge. L'API est donc arrivée après la 1.0.5.

**Preuve en jeu.** Avec `ae2wut-1.0.5` et `cell-terminal-1.6.7`, le client de dev s'arrête :

```
LoaderExceptionModCrash: Caught exception from Cell Terminal (cellterminal)
Caused by: NoClassDefFoundError: com/circulation/ae2wut/AE2UELWirelessUniversalTerminal$GetGui
    at com.cellterminal.integration.AE2WUTIntegration.registerContainerInternal
```

L'instance réelle du joueur porte déjà `B:enableAE2WUT=false` : il a rencontré ce plantage
avant nous.

### Conséquence sur la décision D33

D33 reste exacte **pour AE2WUT 1.0.5**, la version du joueur. Elle devient fausse pour les
versions récentes. Si le joueur monte AE2WUT à 1.2.x, nos deux mixins doivent céder la place
à quatre appels d'API, et `-proc:none` peut disparaître du build.

Notre mode vaut 41 ; celui de Cell Terminal vaut 11. Aucun conflit.

## D35 — La touche d'ouverture, calquée sur ae2exttable et Cell Terminal

**Sources.** `ae2exttable-v1.0.8.jar` et `cell-terminal-1.6.7.jar`, décompilés depuis le
modpack du joueur.

**Ce que font les deux mods.** Une énumération de `KeyBinding`, un écouteur de
`InputEvent.KeyInputEvent`, un paquet vers le serveur. Aucun mixin, aucune API d'AE2.

| Mod | Contexte | Touche par défaut |
|---|---|---|
| ae2exttable | `UNIVERSAL` | **aucune**, code 0 |
| Cell Terminal | `UNIVERSAL` | Maj + P, code 25 |

**Décision.** Touche **non liée** par défaut, comme ae2exttable. Une touche liée d'office
entrerait en conflit avec les quatre d'AE2, avec le Maj + P de Cell Terminal, ou avec un
autre mod du pack.

**Contexte `IN_GAME`, et non `UNIVERSAL`.** `InputEvent.KeyInputEvent` ne se déclenche que
sans fenêtre ouverte. Annoncer un contexte plus large ferait signaler des conflits qui ne
peuvent pas se produire.

**Ordre de recherche côté serveur**, repris de Cell Terminal : inventaire principal, puis
main gauche, pour notre terminal ; puis les mêmes endroits pour le terminal universel.

**Nuance qui compte.** `check` rend **vrai** dès que l'objet est le bon candidat, même quand
un contrôle échoue. Sans cela, la recherche continuerait après un terminal non lié, et le
joueur n'apprendrait jamais pourquoi rien ne s'ouvre.

**Baubles n'est pas géré.** Cell Terminal le fait, mais ni notre terminal ni celui d'AE2WUT
n'implémentent `IBauble` : aucun des deux ne peut occuper un emplacement de Baubles sans un
troisième mod. Ajouter ce chemin demanderait Baubles sur le chemin de compilation, contre la
règle 7.

**La molette et la touche ne posent pas la même question.** `isOurMode` demande « ce
terminal universel est-il réglé sur notre mode ? », et sert au clic droit. `hasOurMode`
demande « a-t-il absorbé notre terminal ? », et sert à la touche : le joueur ne doit pas
avoir à tourner la molette d'abord.

**PIÈGE.** Écrire `mode` dans le NBT ne suffit pas à basculer un terminal universel. AE2WUT
range la grille de craft du mode quitté par `nbtChangeB`, puis restaure celle du mode
demandé par `nbtChange`. Les deux sont appelées par réflexion, faute de dépendance de
compilation, avec repli sur l'écriture simple.

## D36 — L'image du terminal universel, par enveloppe de modèle cuit

**Symptôme.** Réglé sur notre mode, le Wireless Universal Terminal affichait le damier
violet et noir du modèle manquant, avec le nom
`ae2exttable:item/wireless_ultimate_crafting_terminal` en surimpression.

**Cause, vérifiée dans le jar.** `assets/ae2wut/models/item/wireless_universal_terminal.json`
est un `item/generated` muni de dix surcharges :

| Seuil | Modèle |
|---|---|
| 1 | `appliedenergistics2:item/wireless_crafting_terminal` |
| 2 | `appliedenergistics2:item/wireless_fluid_terminal` |
| 3 | `appliedenergistics2:item/wireless_pattern_terminal` |
| 4 | `ae2fc:item/wireless_fluid_pattern_terminal` |
| 5 | `mekeng:item/wireless_gas_terminal` |
| 6 à 9 | les quatre d'`ae2exttable` |
| 114514 | `ae2wut:item/wireless_nova_terminal` |

Une surcharge de Minecraft s'applique dès que la valeur est **supérieure ou égale** au
seuil, et `ItemOverrideList` parcourt sa liste **à l'envers** : la dernière qui correspond
l'emporte. Tout mode au-delà de 9 tombe donc sur la surcharge du mode 9.

**Ce n'est pas notre faute, et ce n'est pas propre à notre mode.** Cell Terminal, avec son
mode 11, subit le même sort sur AE2WUT 1.0.5. Les versions récentes ont remplacé ces
surcharges par un modèle cuit et une méthode `ItemWUTBakedModel.regIcon`, absente de la
1.0.5 : voir la décision D34.

**Décision.** Envelopper le modèle cuit d'AE2WUT au `ModelBakeEvent`. L'enveloppe délègue
tout, sauf sa liste de surcharges : sur notre mode, elle rend le modèle de notre terminal
sans fil ; sur tout autre mode, elle retourne à la liste d'origine, intacte.

**Options écartées.**

| Option | Pourquoi non |
|---|---|
| Choisir un mode inférieur à 9 | tous sont pris, et le mode 0 est celui d'AE2 |
| Livrer notre propre `assets/ae2wut/models/...` | nous écraserions le modèle d'AE2WUT, et il faudrait réécrire ses dix surcharges |
| Un troisième mixin | l'événement de Forge suffit, et ne dépend de rien |

**PIÈGE.** `handleItemState` doit recevoir le modèle **d'origine**, et non l'enveloppe. Lui
passer l'enveloppe ferait boucler la recherche de surcharge sur elle-même.

**Nuance.** Le rendu interroge `showsOurMode`, qui ne regarde que le mode courant. Un objet
de création, réglé sur notre mode sans nous avoir absorbés, porte ainsi notre image plutôt
qu'un modèle manquant. Le clic droit, lui, garde `isOurMode`, plus strict.

---

## D37 — Le nom du canal réseau tient en 20 caractères

**Symptôme.** Le joueur ouvre le terminal sur un serveur dédié. Le client est expulsé sur
`io.netty.handler.codec.DecoderException: The received string length is longer than maximum
allowed (21 > 20)`. En solo, rien ne se passe.

**Cause vérifiée.** En 1.12.2, `CPacketCustomPayload.readPacketData` lit le nom du canal
avec `buf.readString(20)`. Le nom du canal client vers serveur ne peut donc pas dépasser
20 caractères. `Reference.MOD_ID` vaut `packagedautoterminals`, soit **21** caractères. Le
premier paquet montant du mod coupait la connexion.

Le solo échappe au défaut : Forge relie les deux côtés par un `EmbeddedChannel`, qui ne
sérialise aucune chaîne. Le défaut n'apparaît que sur un serveur dédié, ou en LAN.

**Décision.** Ajouter `Reference.CHANNEL = "pat_terminals"`, soit 13 caractères, et ne plus
employer `MOD_ID` comme nom de canal. `MOD_ID` garde son rôle partout ailleurs : registres,
ressources, `mcmod.info`.

**PIÈGE.** Le nom du canal fait partie du protocole. Un client et un serveur qui portent des
versions différentes du mod ne se parlent plus. Toute mise à jour doit aller sur les deux
côtés à la fois.

**Règle.** Tout nouveau canal réseau doit tenir en 20 caractères. Le sens inverse, serveur
vers client, tolère 20 caractères aussi dans `SPacketCustomPayload`.
