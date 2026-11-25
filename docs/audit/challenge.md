# Contre-expertise — moitié 1 (correction-minecraft, vérifiabilité)

> Écrit par l'orchestrateur à partir de la réponse finale du contre-expert, sans modification du fond. Le harnais refuse aux sous-agents l'écriture de fichiers de rapport.

Bilan : 31 confirmés, 0 réfuté, 0 incertain, 7 reclassés (C-MC-01, C-MC-04, C-VE-02, C-VE-04, C-VE-05, C-VE-06, C-VE-07). C-VE-03 reçoit une correction de statut et fusionne avec C-MC-01.

### C-MC-01 — CONFIRMÉ
- Preuve : `PacketBuffer.java:344` fixe le plafond à `NBTSizeTracker(2097152L)`. Avec netty 4.1.9, `EmbeddedChannel.flushInbound` appelle `checkException(voidPromise)`, qui relance l'exception (`javap -c -p`). `FMLProxyPacket.java:129-133` l'attrape et appelle `rejectHandshake`. `NetworkDispatcher.java:326-345` appelle alors `kickWithMessage`, puis `closeChannel` côté client. La déconnexion est donc prouvée. Le décodage `fromBytes` passe aussi par ce chemin en solo.
- Correction : sévérité **majeur** au lieu de bloquant. Le joueur est déconnecté, mais rien n'est perdu. Le seuil n'est pas mesuré. Par estimation, le réseau connu (20 machines, 110 recettes, `DECISIONS.md:391`) se trouve à environ 10 % du plafond. Une pile compte environ 460 octets dans `NBTSizeTracker`, pas 300 à 400 : le seuil arrive un peu plus tôt que l'estimation du constat. C-VE-03 est un doublon : fusionnez-les.

### C-MC-02 — CONFIRMÉ
- Preuve : avec un objet en main, `GuiContainer.mouseReleased` mène à `handleMouseClick(slot, …, PICKUP)`. `AEBaseGui.java:432-441` envoie un `PacketInventoryAction`, que `AEBaseContainer.doAction:413-452` traite. `AppEngSlot.putStack:107-114` appelle `onSlotChange`, qui est vide (`AEBaseContainer:805`). `ContainerPatEditor` ne redéfinit ni `doAction` ni `onSlotChange`. `EditorInventory.setInventorySlotContents:212-220` ne teste pas `isEditable`. Le fork AE2 du joueur (`ae2-uel-v0.56.7-11`) a la même structure (`javap`).
- Correction : aucune.

### C-MC-03 — CONFIRMÉ
- Preuve : `GuiPatTerminal.send:1296-1299` envoie seulement l'index, sans garde ni retrait côté client. `ContainerPatTerminal.removeRecipe:212-226` regroupe de nouveau et supprime `group.recipes.get(index)`. `refreshNow:544-547` remet `ticks` à 0, donc l'envoi a lieu au tick suivant. Deux Maj + clic droit rapides effacent donc la recette suivante.
- Correction : aucune.

### C-MC-04 — CONFIRMÉ
- Preuve : le seul écrivain est `ContainerPatEditor.java:877-878`, et `grep BreakEvent|BlockEvent` ne trouve rien. `ProviderScanner.java:99` relit le nom par position. `DECISIONS.md` ne contient aucune décision sur la durée de vie d'un nom.
- Correction : sévérité **mineur**. Le cas exige un groupe nommé et une machine d'une autre chaîne posée à la même position exacte. Quand la machine vient de la même chaîne, le nom hérité est utile. Le mélange n'est pas silencieux : chaque recette présente d'un seul côté s'affiche en rouge (`unevenCount`, `isComplete`).

### C-MC-05 — CONFIRMÉ
- Preuve : `ContainerPatEditor.java:379-380` appelle `refreshTabs()` à chaque tick, hors de tout test de `ticks`. `refreshTabs:981` mène à `currentRecipes`, puis `currentGroup:1319-1326`, qui lance `scan` et `group`. `ItemRecipeHolder.getRecipeList:66-67` relit le NBT à chaque appel. Quatre appels explicites (lignes 428, 450, 562, 577) ajoutent un scan chacun.
- Correction : aucune. Le coût reste à mesurer.

### C-MC-06 — CONFIRMÉ
- Preuve : `grep lockPlayerInventorySlot src/main` ne trouve rien. `TerminalContext.refusal:84-96` ne relit pas la case. `ContainerMEPortableTerminal.java:54-56` verrouille la case, et ses lignes 95-107 relisent l'objet. `WirelessTerminalGuiObject.extractAEPower:214-222` utilise `effectiveItem`, capturé à la ligne 79.
- Correction : aucune.

### C-MC-07 — CONFIRMÉ
- Preuve : `TileEncoder.disabledRecipeTypes` n'est lu qu'à un seul endroit, `InventoryEncoderPattern.java:142` (`grep` sur `pa-src`). `cycleRecipeType:1345-1353` et `fillFromRecipe:403-409` ne le lisent pas.
- Correction : aucune. Nuance : PackagedAuto filtre seulement le bouton de cycle. Son transfert JEI ne change jamais le type.

### C-MC-08 — CONFIRMÉ
- Preuve : `GuiBridge.java:181` exige `BUILD`, et `securityCheck:418-431` le teste. `PartPatTerminal.onPartActivate:34-43` n'a aucun test. `AbstractPartReporting.onPartActivate:144-173` ne teste que la clé.
- Correction : aucune.

### C-MC-09 — CONFIRMÉ
- Preuve : `GuiPatTerminal.java:321` appelle `summary()` à chaque image. `summary()` appelle `networkSummary:1458-1459`, qui appelle `ProviderPairing.group`. Chaque ligne visible appelle aussi `groupState:1050-1072`, puis `unevenCount`, `isComplete` et `carriers`. Le commentaire des lignes 196-206 décrit seulement le cache de `buildLines`.
- Correction : aucune.

### C-MC-10 — CONFIRMÉ
- Preuve : `PatConfig` est un `@Config` sans synchronisation. `machinesTab` est lu côté serveur (`ContainerPatTerminal.java:189`) et côté client (`GuiPatTerminal.java:475`). `buildMachineLines:1520-1533` signale chaque type quand `present` est vide.
- Correction : aucune. Le défaut existe seulement sur un serveur dédié : en solo, les deux côtés partagent les mêmes champs statiques.

### C-MC-11 — CONFIRMÉ
- Preuve : le jar contient `VERSION = "1.12.2-0.1.0-beta.10"` (`javap -constants`). `ForgeVersion.java:267` lit `mod.getVersion()`, puis les lignes 295-304 comparent avec `"0.1.0-beta.9"`. Le premier élément vaut 1 contre 0, et l'état devient BETA.
- Correction : aucune. Deux ajouts :
  - `tools/release.sh:56` impose la clé sans préfixe (`"${version}"`). La solution la plus courte retire donc le préfixe de `Reference.VERSION` (`build.gradle:57`).
  - `update.json` n'existe pas sur `origin/main` (`git cat-file -e` échoue). L'avis de mise à jour reçoit donc une erreur 404.

### C-MC-12 — CONFIRMÉ
- Preuve : une case `SlotTab` passe par `AEBaseGui.handleMouseClick:561` (`super`), puis `PlayerControllerMP.windowClick:610`, qui appelle `slotClick` sur le client. `recipeCount` est synchronisé (`@GuiSync(3)`), donc `load` puis `currentGroup` s'exécutent sur le client. `LocatableRegistry:21,40-42` est une seule instance pour les deux côtés. `rangeCheck:256` parcourt `getMachines` de la grille du serveur depuis le fil client.
- Correction : aucune.

### C-MC-13 — CONFIRMÉ
- Preuve : `Container.setAll:561-566` appelle `AppEngSlot.putStack`, puis `EditorInventory.setInventorySlotContents:217-219`, puis `updateRecipeInfo` pour chaque case. `RecipeInfoCrafting.java:141` appelle `findMatchingRecipe`. Au premier envoi, le type est `null` et l'appel sort tout de suite (lignes 112-113). Le coût touche donc seulement les changements d'onglet suivants.
- Correction : aucune.

### C-MC-14 — CONFIRMÉ
- Preuve : `RecipeWriter.java:44` est la seule référence symbolique à `thelm.packagedauto.tile` (`grep`). Les autres noms de PackagedAuto sont des chaînes. La plage de dépendance est `[1.0.24.73,)` (`PackagedAutoTerminals.java:39`).
- Correction : aucune.

### C-MC-15 — CONFIRMÉ
- Preuve : `RecipeWriter.java:280` jette la valeur de retour de `insert`. Avec `ItemRecipeHolder`, `editHolder:157-190` réussit toujours sur un porte-recettes vierge. Le défaut est donc latent.
- Correction : aucune.

### C-MC-16 — CONFIRMÉ
- Preuve : dans `GuiEncoder.java:74-75`, le bouton 0 est à x=189 et le bouton 1 à x=225. La ligne 150 passe `reverse = (id == 0)`, donc la flèche droite avance de +1. Chez nous, `PacketRecipeAction.java:117` passe `index == 1` au paramètre `reverse` (`RecipeTypeRegistry:51`), donc notre flèche « > » recule de -1.
- Correction : aucune.

### C-MC-17 — CONFIRMÉ
- Preuve : `javap -v` sur le jar donne 0 occurrence de `SideOnly` dans `ItemWirelessPatTerminal`. Dans AE2, la méthode parente est `@SideOnly(CLIENT)` (`AEBasePoweredItem.java:55`).
- Correction : aucune.

### C-MC-18 — CONFIRMÉ
- Preuve : `FallbackResourceManager.getResource:62,65` ouvre le flux dès la construction de `SimpleResource`. `ClientDiagnostics.java:67` ne le ferme jamais.
- Correction : aucune.

### C-VE-01 — CONFIRMÉ
- Preuve : `grep` sur `src/test` donne 0 résultat. `DECISIONS.md:699` et `TESTING.md:755,823` disent « not tested in game ». `RecipeScaleTest` ne teste pas la borne exacte `<=`.
- Correction : aucune.

### C-VE-02 — CONFIRMÉ
- Preuve : `git ls-remote origin` montre seulement `main` (`38ff493`) et `lot6-sans-fil`. `git ls-tree` trouve 0 fichier `src/test/` sur ces deux branches. `gh run list` : dernière exécution le 2026-09-18. Les étiquettes distantes s'arrêtent à `v0.1.0-beta.7`.
- Correction : sévérité **mineur**. Les tests tournent à chaque `gradlew build` local, et `tools/release.sh:75` lance `./gradlew build` avant chaque étiquette. Aucune version publique n'a contourné la CI, car beta.8 et beta.9 n'ont jamais été poussées. Le défaut est un travail non poussé, pas un code faux.

### C-VE-03 — CONFIRMÉ
- Preuve : `grep lastPayloadBytes` trouve seulement une écriture (`ClientHandler:44`) et une déclaration (`ContainerPatTerminal:123`). `PLAN.md:147-149` confirme qu'aucune mesure n'existe.
- Correction : fusionnez avec C-MC-01. La suite de l'exception n'est plus une hypothèse : c'est une **déconnexion** (preuve de C-MC-01). La phrase « au mieux, le terminal reste figé » est donc fausse. La sévérité majeur reste valable.

### C-VE-04 — CONFIRMÉ
- Preuve : la question N9 (`TESTING.md:736-751`) n'a pas de réponse. `CraftingGridCache.getMediums:470-475` rend une liste vide pour un pattern inconnu.
- Correction : sévérité **mineur**. Le seul effet démontré est un job bloqué, comportement normal d'AE2 quand un pattern disparaît. `CraftingCPUCluster.cancel:449-471` appelle `storeItems` et rend les objets. Un paquet déjà en route porte sa propre recette (`TileUnpackager.java:575`), donc la perte d'objets devient peu probable. Gardez la QA proposée.

### C-VE-05 — CONFIRMÉ
- Preuve : `grep tell(` relève `machine_gone` (8 appels), `no_permission` (2), `no_blank_holder` (1) et `holders_partial` (1). Aucune de ces clés n'est dans `Feedback.REFUSALS:44-64`. La couleur dépend de `isRefusal` (`GuiPatEditor:1046`, `GuiPatTerminal:1014`).
- Correction : sévérité **mineur**. Seule la couleur est fausse : le texte dit déjà le refus. Le test N4 échouerait quand même.

### C-VE-06 — CONFIRMÉ
- Preuve : les 42 journaux de `run/logs` montrent 31 démarrages de serveur intégré et 0 démarrage de serveur dédié. `run/eula.txt` est absent. Les cibles de mixin AE2WUT ne portent pas `SideOnly` (`javap -v`).
- Correction : sévérité **mineur**. Aucune fuite de classe client n'est identifiée, et les motifs utilisés sont les motifs Forge habituels. D37 montre que le joueur a déjà fait tourner une version plus ancienne sur un serveur dédié. « Rien ne prouve que le jar démarre » vaut seulement pour les versions suivantes.

### C-VE-07 — CONFIRMÉ
- Preuve : `hasProblem:1647-1662` ne contient pas `needsHolder`, alors que `groupTroubled:1097-1101` le contient. Le filtre de la ligne 1609 peut donc cacher une ligne rouge.
- Correction : sévérité **mineur**. L'effet reste visuel, sans aucune donnée touchée. Le cas exige trois machines qui partagent un nom, avec des recettes toutes complètes.

### C-VE-08 — CONFIRMÉ
- Preuve : `wc -l` donne 890 lignes, `grep -c '^### '` donne 149 titres. Le tableau `TESTING.md:24-29` marque I à N « never tested ». O et P manquent au tableau. `release.sh` n'a aucune porte de test en jeu.
- Correction : sévérité majeur gardée. Nuances : O et P répètent C-VE-01 ; `PLAN.md:131` dit que K et L sont « written and tested », donc le compte réel des groupes jamais joués est entre 6 et 8.

### C-VE-09 — CONFIRMÉ
- Preuve : `CPacketCustomPayload.java:37` lit le nom avec `readString(20)`. `src/test` ne contient aucune classe de paquet. `PacketRecipeAction` et `PacketEditorSlot` lisent seulement des entiers, un `BlockPos` et des booléens : les tests proposés sont faisables.
- Correction : aucune.

### C-VE-10 — CONFIRMÉ
- Preuve : logique dupliquée entre `ContainerPatEditor.java:536,541` et `RecipeScale.java:27,30`. Le message n'est pas choisi de la même façon dans `save:655-672` et `repairGroup:485-501`. La correction Unicode de `ScaledResolution.java:35-38` manque dans `GuiScaleFit.factorOf`.
- Correction : aucune. Nuance : `setSlotCount` applique un rapport nouveau/ancien, et `RecipeScale` un facteur entier : même forme, pas exactement une seule règle.

### C-VE-11 — CONFIRMÉ
- Preuve : `build.gradle:41` est la seule ligne `compilerArgs`. `WutSupport.java:13` importe `I18n`, et le `@SuppressWarnings` de la ligne 89 ne couvre pas l'import. Dans ForgeGradle, `TaskRecompileMc` hérite de `CachedTask`, pas de `JavaCompile` (`javap`) : `-Werror` ne touche donc pas la recompilation de Minecraft.
- Correction : aucune.

### C-VE-12 — CONFIRMÉ
- Preuve : `TESTING.md:8` dit que tout ce qui porte 🆕 n'a jamais tourné en jeu. Les en-têtes D, E, F et G portent 🆕 alors que le tableau les marque « passed ». `en_us.lang:143` (`scale_unsaved`) et `:78` (`unsaved`) diffèrent, et P6 (`:864`) attend le mauvais. Les lignes 43, 264, 313, 654 et 827 sont conformes au constat.
- Correction : aucune.

### C-VE-13 — CONFIRMÉ
- Preuve : `grep` compte 13 noms de classe `*.tile.*` dans `CrafterTypes` et `ProviderRole`. `unzip -l` trouve les 8 noms des addons dans `run/mods`. `ProviderPairingTest` n'a aucun cas `UNKNOWN`. `ProviderPairing.java:58` répond « complète » quand le rôle est `UNKNOWN`.
- Correction : aucune.

## Hors des 31 constats

Le serveur peut refuser un remplissage JEI trop gros (non vérifié). `CPacketCustomPayload.java:26-29` refuse tout paquet client → serveur de plus de 32 767 octets. `PacketEditorFill` borne le nombre de piles, pas leur taille en octets. Un remplissage JEI de piles à gros NBT peut donc lever une exception côté client. Pour trancher : un test qui appelle `toBytes` avec 90 piles à gros NBT, puis lit `writerIndex`.
