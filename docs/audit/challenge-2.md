# Contre-expertise — moitié 2 (architecture, produit)

> Écrit par l'orchestrateur à partir de la réponse finale du contre-expert, sans modification du fond. Le harnais refuse aux sous-agents l'écriture de fichiers de rapport.

Bilan : 29 confirmés, 2 réfutés, 0 incertain, 6 reclassés (5 changements de sévérité : C-AR-04, C-AR-05, C-AR-06, C-AR-07, C-PR-04 ; 1 changement de porte : C-PR-05).

Les chemins Java partent de `src/main/java/fr/julien/packagedautoterminals/`. Gradle n'a pas tourné. Les preuves viennent de `sed`, `grep` et `javap`.

### C-AR-01 — CONFIRMÉ
- Preuve : `ContainerPatTerminal.java:221-226` supprime `group.recipes.get(index)` sans vérifier l'identité de la recette. `deleteCurrent` le fait (`ContainerPatEditor.java:1129-1133`). L'ordre vient de `HashMap` et `HashSet` : `.tools/ae2-src/appeng/me/Grid.java:34`, `MachineSet.java:11-12`. `ACTION_EDIT` souffre du même défaut (`ContainerPatTerminal.java:532-534`).
- Porte double sens confirmée. `FMLModContainer.java:608` passe `null` quand `acceptableRemoteVersions` manque. `NetworkModHolder.acceptVersion` (l. 290-297) exige alors la version exacte, et `FMLHandshakeServerState.java:70` refuse le client.
- Correction : aucune. C-PR-02 touche le même chemin : traitez-les ensemble.

### C-AR-02 — CONFIRMÉ
- Preuve : `ContainerPatEditor.java:378-380` appelle `refreshTabs()` hors de la garde `ticks % refreshTicks`, donc à chaque tick. La chaîne est `currentRecipes` → `currentGroup` (`:1320-1326`) → `ProviderScanner.scan`. Chaque scan relit le NBT de chaque holder : `ItemRecipeHolder.getRecipeList` fait `new RecipeListHelper(nbt)` (`.tools/pa-src/.../ItemRecipeHolder.java:66-67`). Côté client, `GuiPatTerminal.java:320` appelle `summary()` à chaque image, puis `:1458-1459` regroupe.
- Correction : comptage exact côté serveur : 10 appels à `scan` et 9 à `group`, pas 11. Sévérité majeur maintenue. L'impact reste à mesurer.

### C-AR-03 — CONFIRMÉ
- Preuve : `groupTroubled` (`GuiPatTerminal.java:1097-1101`) teste `needsHolder`, et `hasProblem` (`:1647-1662`) ne le teste pas. Les règles d'état vivent dans l'écran, sans test.
- Correction : l'exemple est trop large. Dans une paire de 2 machines, une machine sans holder rend `isComplete` faux (`ProviderPairing.java:54-65`), et les deux prédicats concordent. La divergence apparaît seulement dans un groupe nommé de 3 machines ou plus, par exemple 2 Packagers dont un sans holder et 1 Unpackager.

### C-AR-04 — CONFIRMÉ
- Preuve : `no_permission`, `machine_gone`, `no_blank_holder` et `holders_partial` sont émises (`ContainerPatTerminal.java:258,266,354,414,437` ; `ContainerPatEditor.java:611,933`). Elles manquent dans `Feedback.java:45-66`. `repairGroup` (`:490-501`) ne traite pas `supplied`.
- Correction : majeur → **mineur**. Le texte de chaque message dit déjà le refus ; seule la couleur est fausse. « Six copies » est exagéré : 4 méthodes traduisent un `RecipeWriter.Result`. `removeHolder` et `insertHolders` utilisent leurs propres compteurs.

### C-AR-05 — CONFIRMÉ
- Preuve : 11 branches `holders`/`terminal` dans `ContainerPatEditor.java` (193, 360, 597, 751, 846, 1105, 1229, 1312, 1332, 1386, 1407). L'écran en a 9 (`holderMode`).
- Correction : majeur → **mineur**. Aucune NPE n'est atteignable aujourd'hui : chaque appelant de `grid()` (l. 1120, 1238, 1321) passe d'abord une garde `holders != null`. C'est de la dette, pas du code qui casse.

### C-AR-06 — CONFIRMÉ
- Preuve : le grep des imports montre les cycles `common→proxy→client.gui→common`, `common↔item`, `common↔part`, `container↔network`, `network↔client` et `network↔item`. `ProviderPairing.java:10` importe `thelm…IRecipeInfo` et utilise `net.minecraft…BlockPos` à `:260`. `PORTING.md:69-70` compte pourtant les imports `thelm` : son classement T0 se contredit. Le code fait 10 374 lignes.
- Correction : majeur → **mineur**. « Les cycles empêchent de tester » est faux : `ProviderPairingTest` teste déjà `ProviderPairing` en JUnit. Le vrai défaut est un budget de portage faux dans la documentation.

### C-AR-07 — CONFIRMÉ
- Preuve : `EditorInventory.setInventorySlotContents` (`:213-220`) appelle `updateRecipeInfo`, qui réécrit les slots 81 à 98 côté client. `GuiPatEditor.java:1013` active Save selon le `recipeInfo` du client. La Javadoc `ContainerPatEditor.java:48-49` dit le contraire.
- Correction : majeur → **mineur**. « D05 n'est pas tenue » est faux. D05 (`DECISIONS.md:13`) parle d'autorité et d'intentions : le client calcule un aperçu, il n'écrit rien. Le serveur recalcule à l'enregistrement. L'Encoder de PackagedAuto fait le même calcul des deux côtés (`InventoryEncoderPattern.updateRecipeInfo`). La perte de Save reste une HYPOTHÈSE : testez-la avec une recette ajoutée par un script côté serveur seulement.

### C-AR-08 — CONFIRMÉ
- Preuve : `javap -c` sur `ae2-uel-0.56.5.jar` donne `81 112 115 Class java/lang/NumberFormatException` autour de `parseLong`. Le PITFALL de `PatGuiHandler.java:163-165` est donc faux. Seul `PacketOpenTerminal.java:133-142` vérifie la station.
- Correction : l'écran ouvert par clic droit se ferme au premier tick, pas après une seconde : `ticks` part de 0, et `ContainerPatTerminal.java:152` vérifie dès `ticks == 0`. Sévérité mineur maintenue.

### C-AR-09 — CONFIRMÉ
- Preuve : `GuiPatTerminal.java:475` et `ContainerPatTerminal.java:189` lisent chacun leur `PatConfig.machinesTab` local. `ConfigManager.java:146-172` lit seulement le fichier local et ne contient aucun code réseau.
- Correction : doublon de C-PR-05. Fusionnez les deux constats.

### C-AR-10 — CONFIRMÉ
- Preuve : la recherche par `equals` existe en plusieurs copies : `RecipeWriter.java:284-295`, `ContainerPatEditor.java:821-833` et `1150-1158`, `ProviderPairing.java:350-383`. `ProviderScanner.find` sert encore dans `ContainerPatTerminal.java:397-398`, alors que `RecipeWriter.machineOf` existe.
- Correction : un exemple est faible. Le calcul de `ContainerPatEditor.java:529-545` est une proportion `amount/previous`. `RecipeScale.scale` multiplie par un facteur entier : ce n'est pas le même calcul. Sévérité mineur maintenue.

### C-AR-11 — CONFIRMÉ
- Preuve : `PacketRecipeAction.java:29`, `:47` et `:51` donnent trois sens à `index`. Les deux chaînes de `if` (`:89-127`) ignorent sans log une action inconnue.
- Correction : aucune.

### C-AR-12 — CONFIRMÉ
- Preuve :
  - `grep tooltipStacks` trouve seulement `PatConfig.java:67-68`.
  - `ProviderSnapshot.id` est écrit et sérialisé (`:55`, `:70`), puis lu nulle part.
  - `wutMode()` n'a aucun appelant.
  - `PACKAGED_EX_CRAFTING`, `PACKAGED_AVARITIA` et `PACKAGING_PROVIDER` ont 0 usage.
  - `lastPayloadBytes` est écrit et jamais lu.
- Correction : doublon de C-PR-08 (`tooltipStacks`) et de C-PR-12 (`wutMode`). `ClientDiagnostics` journalise une seule fois par lancement : ce n'est pas du code mort.

### C-AR-13 — CONFIRMÉ
- Preuve :
  - `ARCHITECTURE.md:44-52` et `:62-67` décrivent `api/`, `PartPackagedAutoTerminal` et un « delta packet », qui n'existent pas.
  - R4 parle de Cleanroom (`DECISIONS.md`, tableau des révisions), mais `WutSupport.java:22-23` la cite pour AE2WUT.
  - `ContainerPatTerminal.java:243-245` dit « pulls nothing ».
  - `PacketRecipeAction.java:27` contredit `:111-112`.
  - Des Javadoc sont orphelines ou en double : `GuiPatTerminal.java:112-116`, `1479-1486`, `1787-1799`, `ContainerPatEditor.java:197-205`, `453-458`, `1166-1170`, `ItemWirelessPatTerminal.java:164-169`.
- Correction : aucune.

### C-AR-14 — CONFIRMÉ
- Preuve : `ContainerPatTerminal.java:106` déclare `List<MachineSnapshot> machines`, qui sont des crafters. `ProviderPairing.java:34` déclare `List<ProviderSnapshot> machines`, qui sont des providers.
- Correction : aucune. L'onglet « Patterns » reprend le vocabulaire de l'Interface Terminal d'AE2, qui sert de modèle. Ce point est un choix de produit défendable, pas une erreur.

### C-AR-15 — CONFIRMÉ
- Preuve : `GuiPatEditor.java:1129-1130` et `1258-1265` recalculent `TAB_LEFT + (slot % TAB_COLUMNS) * 18` et `GRID_LEFT + (slot % 9) * 18`. Le grep compte 25 constantes `int` dans `ContainerPatTerminal.java:40-100`. Le conteneur en utilise seulement 2 : `PLAYER_INVENTORY_OFFSET_X` et `PLAYER_INVENTORY_TOP`.
- Correction : 25 constantes, pas 24. Sévérité mineur maintenue.

### C-AR-16 — RÉFUTÉ
- Preuve : il n'existe qu'un seul appelant, `ContainerPatTerminal.java:538-540`. Il appelle toujours `setPendingEdit` puis `setPendingRecipe`. La table est indexée par UUID, et chaque ouverture écrase l'ancienne entrée. Aucun autre chemin n'ouvre `EDITOR` ou `WIRELESS_EDITOR`. Une entrée restée en mémoire reste donc bornée à une par joueur, et elle ne sert jamais.
- Correction : aucune conséquence n'est atteignable. Au plus cosmétique.

### C-AR-17 — RÉFUTÉ
- Preuve : une machine inconnue échoue sans danger, par conception.
  - `ProviderRole.of` rend `UNKNOWN` (`ProviderRole.java:45-50`), et `isComplete` rend alors `true` (`ProviderPairing.java:58-59`).
  - Un type de recette inconnu ne déclenche aucun diagnostic (`CrafterTypes.isDiagnosable`), comme R1 le demande.
  - L'édition des recettes reste générique (`ProviderScanner` par interface).
- Correction : le besoin est spéculatif. Le constat le dit lui-même pour l'énergie (« si un besoin réel apparaît »). Le correctif ouvrirait une porte à sens unique (nouvelles clés de configuration publiques) sans besoin démontré.

### C-AR-18 — CONFIRMÉ
- Preuve : `grep` compte 33 `PITFALL`, 21 `FIXED`, 2 745 lignes de commentaire sur 10 374, 455 dans `GuiPatTerminal` et 372 dans `ContainerPatEditor`.
- Correction : aucune. La sévérité cosmétique est juste.

### C-PR-01 — CONFIRMÉ
- Preuve :
  - Le bouton envoie l'action sans tester `mouseButton` (`GuiPatTerminal.java:1199-1203`, `actionUnder` `:775-797`).
  - Son infobulle réutilise `remove_holder_hint`, « Shift + left click… » (`:841-847`).
  - `insertHolders` extrait `new ItemStack(blank)` sans NBT (`ContainerPatTerminal.java:402-403`), et AE2 compare le NBT (`AESharedItemStack.java:54`). Le terminal ne peut donc pas remettre un holder qui porte des recettes.
- Correction : l'éjection par le bouton est un choix récent et voulu (commit 8158530, CHANGELOG « Unreleased » : « One button puts the recipe holders in, and takes them out »). Le retirer demande l'accord de Julien. Le défaut d'infobulle est un bug net. Sévérité majeur maintenue.

### C-PR-02 — CONFIRMÉ
- Preuve : `actionUnder` ne teste pas le bouton de souris. Un clic droit sur la croix envoie `ACTION_REMOVE` (`GuiPatTerminal.java:1209-1212`), alors que `README.md:78` enseigne « Right click a recipe row: Edit ». L'infobulle de la croix dit en plus « Shift + right click: delete » (`:851`). `deleteCurrent` puis `load(-1)` (`ContainerPatEditor.java:1147`, `:1069-1089`) vident la grille.
- Correction : aucune. Corrigez-le avec C-AR-01, sur le même chemin.

### C-PR-03 — CONFIRMÉ
- Preuve : prenez P{A,B} et U{A}. `sameRecipes` échoue (`distinctCount` 1 ≠ 2), et `mergeLonePartners` exige `recipes.isEmpty()`. Le code forme donc deux groupes. Dans le groupe de P, `unevenCount` vaut 2, et le bouton prend la forme REPAIR (`:822`). `repairGroup` écrit seulement dans `[P]`. `editHolder` rend `UNCHANGED` (`RecipeWriter.java:182-183`), puis le joueur lit `write_failed` (« Nothing was written »).
- Correction : le titre est inexact. D38 (`DECISIONS.md:419-421`) nomme le cas 1 (un côté vide) « the common case », pas ce cas 2. Sévérité majeur maintenue : le bouton promet une réparation et échoue.

### C-PR-04 — CONFIRMÉ
- Preuve : `HEIGHT` = 47 + 12 × 18 + 104 = 367 (`ContainerPatTerminal.java:47-80`). En 1920×1080, `factorOf` donne 4. 1080/4 = 270 et 1080/3 = 360, tous deux sous 367, donc `scaleFor` rend 2 (`GuiScaleFit.java:93-129`).
- Correction : majeur → **mineur**. L'écran reste utilisable. L'échelle 2 est l'échelle « Normal » de vanilla. Le joueur ne perd aucune fonction, et le correctif est une constante.

### C-PR-05 — CONFIRMÉ
- Preuve : avec `machinesTab=false`, `buildMachinePayload` rend une liste vide (`ContainerPatTerminal.java:189-191`). `onNetwork` (`GuiPatTerminal.java:904-915`) rend alors faux, et `machineHint` (`:1420-1432`) affiche `machine_absent` en rouge dans l'infobulle de l'onglet Patterns.
- Correction : porte sens unique → **double sens**. FML refuse un client dont la version diffère (voir la preuve de C-AR-01). Le format du paquet peut donc changer sans dommage. Doublon de C-AR-09.

### C-PR-06 — CONFIRMÉ
- Preuve : il y a seulement 5 clés `jei.*` (`en_us.lang:116-120`), et aucune ne décrit les préfixes `@` et `#`, le bouton `×N` ou la touche. La touche est créée sans assignation (`PatKeyBindings.java:27-29`, code 0). Le bouton `×N` porte un libellé `"×N"` en dur (`GuiPatEditor.java:289-290`). Le grep ne trouve ni `@mod` ni `#type` dans `README.md`, `docs/wiki/*.md` et `docs/curseforge-description.md`.
- Correction : nuance. Le bouton de ligne, le crayon et la croix ont déjà une infobulle au survol (`actionTooltip`, `:839-854`). « Découvre par hasard » vaut donc surtout pour les préfixes, `×N` et la touche.

### C-PR-07 — CONFIRMÉ
- Preuve : `Diagnostics.md:9-10` annonce « inactive, busy or idle ». Le code affiche `inactive`, `busy`, `no recipe` ou un nombre de recettes (`GuiPatTerminal.java:941-954`).
- Correction : nuance sur `Troubleshooting.md:7`. La phrase décrit le message `insert_holder_first`, et le code émet ce message seulement quand `pullHolderFromNetwork=false` (`ContainerPatTerminal.java:265-267`). Dans ce cas, la phrase est vraie ; elle est seulement trop générale. Le correctif doit nommer le réglage. Sévérité mineur maintenue.

### C-PR-08 — CONFIRMÉ
- Preuve : `grep -rn tooltipStacks src` trouve seulement la déclaration (`PatConfig.java:67-68`). La clé existe dans 16 fichiers `.lang`.
- Correction : doublon de C-AR-12. Porte sens unique maintenue, avec un impact nul.

### C-PR-09 — CONFIRMÉ
- Preuve : `MachineSnapshot.scan` parcourt seulement la grille (`:59-76`). L'Unpackager livre à tout `IPackageCraftingMachine` adjacent (`.tools/pa-src/.../TileUnpackager.java:224-233`). `TileCrafter` éjecte sans AE2 quand `hostHelper` est inactif (`TileCrafter.java:108-123`). `orphan_help` affirme « cannot be executed ».
- Correction : aucune. Statut PROBABLE maintenu : le test en jeu proposé tranche.

### C-PR-10 — CONFIRMÉ
- Preuve : l'outil Encoder est toujours dessiné (`GuiPatTerminal.java:416-424`) et envoie `ACTION_OPEN_HOLDERS` (`:1143-1147`). Le refus passe par `sendStatusMessage(..., true)` (`PatGuiHandler.java:85-88`). Forge dessine le HUD avant l'écran (`EntityRenderer.java:1151` puis `:1177`), et `AEBaseGui.java:192` assombrit le fond.
- Correction : aucune. Statut PROBABLE maintenu.

### C-PR-11 — CONFIRMÉ
- Preuve : `ItemWirelessPatTerminal.java:175-180` ajoute toujours `wireless_link`. La classe étend `AEBasePoweredItem`, pas `ToolWirelessTerminal`. Le terminal d'AE2 affiche Linked ou Unlinked (`ToolWirelessTerminal.java:93-107`).
- Correction : aucune.

### C-PR-12 — CONFIRMÉ
- Preuve : `FMLClientHandler.java:368-372` crée un `DefaultGuiFactory` pour tout mod `@Config`. `wutModeId` ne porte pas `@Config.RequiresMcRestart` (grep vide). `WutSupport.mode()` garde la valeur après la première lecture (`:65-75`). `isOurMode` compare cette valeur avec le NBT `modes` (`:141-145`).
- Correction : `wutMode()` sans appelant recoupe C-AR-12.

### C-PR-13 — CONFIRMÉ
- Preuve : `grep holder_main=` et `grep holder_sync=` sur `lang/*.lang`. `recipes_scaled` reçoit `from.size()`, qui peut valoir 1 (`ContainerPatEditor.java:1256`).
- Correction : la liste des langues est inversée pour le coréen. `ko_kr` garde `holder_main=Holder` en anglais, et `holder_sync=동기화` est traduit. Bilan : « Holder » reste en anglais dans 10 langues (cs, de, es, fr, it, ko, nl, pl, pt, tr), et « Sync » dans 9.
