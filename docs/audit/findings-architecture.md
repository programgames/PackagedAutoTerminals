# Constats — lentille architecture

Audit du 2026-11-25, branche `audit/2026-11-25` (partie de `85dadf6`). Périmètre : les 61 fichiers de `src/main/java`, les tests, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/PORTING.md`. Je n'ai lancé aucune commande Gradle (consigne). Les preuves sur l'API viennent de `.tools/forge-src`, `.tools/ae2-src` et `.tools/pa-src`, plus `javap` sur `libs/maven/pat/ae2-uel/0.56.5/ae2-uel-0.56.5.jar`.

Bilan : 0 bloquant, 7 majeurs, 10 mineurs, 1 cosmétique.

> Note de l'orchestrateur : le harnais a refusé l'écriture de ce fichier au sous-agent. L'orchestrateur l'a recopié tel quel depuis son rapport final.

---

### C-AR-01 — La suppression depuis le terminal vise une position, pas une recette
- Sévérité : majeur
- Statut : PROBABLE
- Où : `container/ContainerPatTerminal.java:212-237`, `client/gui/GuiPatTerminal.java:1209-1211`, `1223-1226`, `1617-1620`
- Constat : le terminal envoie `ACTION_REMOVE` avec `recipeIndex`, c'est-à-dire l'index de la recette dans `group.recipes` côté client. Le serveur refait un scan, regroupe les machines, puis supprime `group.recipes.get(index)` dans toutes les machines du groupe. Il ne demande aucune confirmation. L'éditeur a corrigé exactement ce défaut avec `openedRecipe`, mais le terminal ne l'a pas fait. La migration « nommer la recette, jamais sa position » est donc faite à moitié.
- Preuve :
  - `ContainerPatTerminal.java:221-226` :
    ```java
    if (group == null || index < 0 || index >= group.recipes.size()) {
        return;
    }
    RecipeWriter.Result result = RecipeWriter.apply(grid, getActionSource(),
            group.machines, group.recipes.get(index), null);
    ```
  - `ContainerPatEditor.java:100-103` décrit le même piège. L'éditeur refuse alors l'écriture (`:1129-1133`, `// Same rule as save: the recipe is named, never its position.`).
  - `PacketRecipeAction.java:16-18` désigne la machine par sa position parce que « the order of a scan can change between two refreshes ». La même raison vaut pour l'ordre des recettes.
  - L'ordre vient de collections non ordonnées d'AE2 : `.tools/ae2-src/appeng/me/Grid.java:34` (`machines = new HashMap<...>`) et `appeng/me/MachineSet.java:11-12` (`extends HashSet<IGridNode>`). L'union des recettes d'un groupe suit l'ordre du premier holder scanné (`ProviderPairing.java:138-141`).
- Pourquoi c'est un problème : entre l'instantané (une fois par seconde) et le clic, la liste peut se décaler. Un autre joueur, un hopper ou un double clic suffit. Le serveur supprime alors une autre recette, dans les deux machines de la paire. Le joueur perd une recette encodée sans aucun message.
- Correctif proposé : envoyer l'identité de la recette avec l'index, par exemple `MiscUtil.recipeHashCode` (`.tools/pa-src/thelm/packagedauto/api/MiscUtil.java:361`). Le serveur compare les deux et refuse avec `group_changed` s'ils diffèrent, comme l'éditeur. Pour vérifier : un test unitaire sur la résolution, et un test manuel avec deux joueurs (une suppression pendant un ajout).
- Porte : double sens. FML exige la même version des deux côtés : `forge-src/net/minecraftforge/fml/common/Mod.java:173` dit « Defaults to nothing, which is interpreted as "only this version" ». `PackagedAutoTerminals.java:27-41` ne déclare pas `acceptableRemoteVersions`.
- Coût : S

### C-AR-02 — Pas de modèle partagé du réseau : le scan et le regroupement se refont partout, jusqu'à chaque tick et chaque image
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `container/ContainerPatEditor.java:356-383`, `980-985`, `1311-1327` ; `client/gui/GuiPatTerminal.java:320`, `1453-1461`
- Constat : le code appelle `ProviderScanner.scan` puis `ProviderPairing.group` à 11 endroits côté serveur et à 3 côté client. Dans l'éditeur en ligne, `refreshTabs()` est hors de la garde `refreshTicks`. Chaque tick refait donc le scan complet de la grille, lit tous les holders et regroupe les machines. Côté client, le cache `cachedLines` a sorti le regroupement de `drawFG`, mais `summary()` le refait à chaque image.
- Preuve :
  - `ContainerPatEditor.java:367` ne garde que la vérification de portée. Les lignes `:378-380` sont hors de cette garde :
    ```java
    // The name is read again on every cycle: another screen may have changed it.
    groupName = GroupNames.get(world).get(dimension, pos);
    refreshTabs();
    ```
    La chaîne d'appels est `refreshTabs` → `currentRecipes` → `currentGroup()` (`:1325` : `ProviderPairing.group(ProviderScanner.scan(grid))`).
  - Cadence : `.tools/forge-src/net/minecraft/entity/player/EntityPlayerMP.java:365` appelle `this.openContainer.detectAndSendChanges();` dans `onUpdate()`, donc à chaque tick.
  - Client : `GuiPatTerminal.java:320` appelle `summary()` dans `drawFG`, et `:1458-1459` refait `ProviderPairing.group(terminalContainer.providers)`. Pourtant le commentaire `:196-207` affirme que ce coût a quitté le chemin de rendu.
  - `ProviderPairing.group` est en O(n²), avec des `equals` linéaires (`ProviderPairing.java:122-149`, `335-348`).
- Pourquoi c'est un problème : le coût augmente avec la taille du réseau et avec le nombre de joueurs qui ont un éditeur ouvert. Le projet chiffre lui-même ce coût à « tens of thousands of recipe comparisons per second » sur 50 machines (`GuiPatTerminal.java:200-203`). Aucune mesure n'existe, alors que R2 en demande une (`ContainerPatTerminal.java:115-123`). Chaque nouvelle action recopie la même chaîne scan → group → groupOf.
- Correctif proposé : une seule vue du réseau par conteneur. Côté serveur, l'éditeur recalcule son groupe à la cadence `refreshTicks`, comme le terminal, et garde le résultat entre deux ticks. Chaque action refait un seul scan frais (le serveur reste l'autorité), puis met cette vue à jour. Côté client, calculer `ProviderPairing.group(providers)` une fois par `revision`, puis partager ce résultat entre `lines()`, `summary()` et l'onglet Machines. Mesurer d'abord avec un profileur sur un réseau de 50 machines.
- Porte : double sens
- Coût : M

### C-AR-03 — Les règles du domaine vivent dans l'écran et dans les conteneurs, hors de portée des tests
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `client/gui/GuiPatTerminal.java:805-836`, `884-915`, `1050-1101`, `1647-1662`, `1711-1785` ; `container/ContainerPatTerminal.java:212-503` ; `container/ContainerPatEditor.java:518-563`, `593-677`, `1180-1268`
- Constat : `GuiPatTerminal` (1 856 lignes) décide de l'état d'un groupe et dit s'il est en défaut. Il choisit aussi l'action que le bouton du groupe envoie, la machine que le groupe cible et le filtrage de la recherche. Les conteneurs portent les cas d'usage : supprimer, réparer, insérer des holders, sauver, mettre à l'échelle. Seules cinq petites classes ont des tests. Les deux prédicats « ce groupe a un problème » ont déjà divergé.
- Preuve :
  - `GuiPatTerminal.java:1097-1101` :
    ```java
    private boolean groupTroubled(ProviderPairing.Group group) {
        return ProviderPairing.missingRoleOf(group) != null
                || hasInactive(group)
                || (!group.recipes.isEmpty() && (needsHolder(group) || unevenCount(group) > 0));
    }
    ```
    `hasProblem` (`:1647-1662`) ignore `needsHolder`. Prenons un groupe nommé dont une machine n'a pas de holder. L'écran l'affiche en rouge, mais le filtre « problèmes seulement » le cache.
  - `SearchQuery.java:14-15` dit : « The matching itself stays in the screen ».
  - La règle 6 de `CLAUDE.md` dit : « Sorting, filtering, diffing and validation go into `core`, with pure JUnit tests ». `docs/PORTING.md:81` dit : « T3 is 44 % of the code, and it is the GUI ». Or chaque portage réécrit cette partie.
- Pourquoi c'est un problème : chaque règle placée dans l'écran sera réécrite trois fois (familles B, C et D du portage), et aucune n'a de test. Les divergences entre les écrans et les conteneurs (voir aussi C-AR-04) viennent de là.
- Correctif proposé : déplacer les règles vers `common`, une à la fois : `Group.state()`, `Group.isTroubled()` (un seul prédicat), `Group.nextAction()`, `Group.crafterType()`. Mettre `matches` dans `SearchQuery`, avec une fonction `ItemStack → nom` passée en paramètre. Ajouter un test JUnit pour chaque règle déplacée, avec `FakeRecipe`.
- Porte : double sens
- Coût : M

### C-AR-04 — La gravité d'un message dépend d'une liste tenue à la main, et quatre refus s'affichent en vert
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `common/Feedback.java:28-66` ; `container/ContainerPatTerminal.java:258`, `266`, `354`, `437` ; `container/ContainerPatEditor.java:611`, `933` ; `client/gui/GuiPatTerminal.java:318-319` ; `client/gui/GuiPatEditor.java:1050-1051`
- Constat : `Feedback.REFUSALS` liste les clés à dessiner en rouge. L'endroit qui émet le message ne donne pas sa gravité. Le code émet `no_permission`, `machine_gone`, `no_blank_holder` et `holders_partial`, mais ces quatre clés manquent dans la liste. Elles s'affichent donc dans la couleur du succès. De plus, la traduction d'un `RecipeWriter.Result` en message existe en six copies, avec six ordres de priorité différents.
- Preuve :
  - `Feedback.java:39-40` dit : « **A new refusal key must be added here**, or it will be drawn as a success. »
  - J'ai comparé les clés émises (Grep `"gui\.packagedautoterminals\.[a-z_]+"` sur `container/`) avec `Feedback.java:47-66`. Les quatre clés citées manquent. Exemple dans `en_us.lang` : `no_permission=You are not allowed to build on this network`.
  - Couleur : `GuiPatTerminal.java:319` contient `messageRefused ? COLOR_WARNING : COLOR_OK`.
  - Les six copies : `ContainerPatTerminal.java:229-236`, `351-364`, `413-422`, `490-501` ; `ContainerPatEditor.java:643-673`, `1139-1146`. `repairGroup` (`:483-487`) ignore `result.supplied`, alors que `save` (`:663-665`) l'annonce.
- Pourquoi c'est un problème : un joueur sans droit lit « You are not allowed… » en vert. La règle « ajoute la clé ici » a déjà échoué quatre fois, et elle échouera encore. Une réparation qui prend des holders dans le stockage ne le dit pas au joueur.
- Correctif proposé : envoyer la gravité avec le message, au moyen de deux méthodes `tell` et `refuse`, ou d'un octet de gravité dans la chaîne emballée. Supprimer ensuite `REFUSALS`. Une seule fonction pure `Feedback.ofResult(Result, context)` choisit le message d'une écriture. Ajouter un test pour cette fonction.
- Porte : double sens
- Coût : S

### C-AR-05 — Le mode holder est un drapeau `null` dans une classe de 1 411 lignes
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `container/ContainerPatEditor.java:138-153`, `360`, `597`, `751`, `846`, `1105`, `1229`, `1312`, `1332`, `1386`, `1407` ; `client/gui/GuiPatEditor.java:292`, `347`, `460`, `506`, `858`, `964`, `992`, `1055`, `1059`
- Constat : le conteneur change de comportement selon que `holders` vaut `null` ou non. En mode holder, `terminal` vaut `null`. Le mode crée onze branches côté serveur et neuf dans l'écran. Chaque nouvelle fonction doit tester le mode, sinon elle appelle `terminal.grid()` sur une référence nulle. D44 (`scaleAll`, `:1229-1248`) a déjà dû se dédoubler.
- Preuve : la liste ci-dessus (Grep `holders [!=]= null|terminal == null|holderMode`). `DECISIONS.md:744-748` rejette une sous-classe, parce que JEI cherche le handler par la classe exacte du conteneur. `docs/audit/CONTEXTE.md:3` dit que D43 et D44 n'ont « jamais [été] testés en jeu ».
- Pourquoi c'est un problème : la contrainte JEI interdit une sous-classe, mais elle n'interdit pas la composition. Avec le drapeau, chaque évolution se fait deux fois, et un oubli lève une `NullPointerException` sur le fil serveur. Le mode le plus récent est aussi le moins vérifié.
- Correctif proposé : garder une seule classe `ContainerPatEditor`, ce qui respecte la contrainte D43. Lui donner une stratégie `EditTarget` avec deux implémentations : `GroupTarget` (réseau) et `HolderTarget` (deux slots). Méthodes de la stratégie : `recipes()`, `write(old, new)`, `map(from, to)`, `checkAccess()`, `close()`. L'écran lit un seul champ `kind`.
- Porte : double sens
- Coût : M

### C-AR-06 — `common` n'est pas une feuille : il y a des cycles de paquets, et le futur `core` est mal délimité
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `common/TerminalContext.java:10-11`, `common/PatItems.java:7-9`, `proxy/PatGuiHandler.java:14-23`, `network/PacketProviderList.java:3`, `container/ContainerPatTerminal.java:18-21`, `network/PacketOpenTerminal.java:8-9`, `item/ItemWirelessPatTerminal.java:10-11`, `integration/wut/WutEventHandler.java:6-8` ; `docs/PORTING.md:70-81`, `281`
- Constat : le code contient ces cycles : `common → proxy → client.gui → common`, `common ↔ item`, `common ↔ part`, `container ↔ network`, `network ↔ client`, `network ↔ item`, `proxy ↔ integration.wut`. `PORTING.md` classe `ProviderPairing` en T0 (« no Minecraft import ») et estime l'extraction de `core` à « 1 session, 5 files ». C'est faux. `ProviderPairing` dépend de `IRecipeInfo` (PackagedAuto) et de `BlockPos`, écrit en nom complet, ce qui échappe au comptage des imports. Il dépend aussi de `ProviderSnapshot` (`ItemStack`, NBT, `MiscUtil`).
- Preuve :
  - `common/TerminalContext.java:11` contient `import fr.julien.packagedautoterminals.proxy.PatGuiHandler;`, et `proxy/PatGuiHandler.java:14-15` importe `client.gui.GuiPatEditor` et `GuiPatTerminal`.
  - `common/ProviderPairing.java:10` contient `import thelm.packagedauto.api.IRecipeInfo;`, et `:260` contient `groupOf(List<Group> groups, int dimension, net.minecraft.util.math.BlockPos pos)`.
  - `common/ProviderSnapshot.java:6-12` importe Minecraft et PackagedAuto. La ligne `:51` déclare `IPackageProvidingMachine machine`.
  - `docs/PORTING.md:70` mesure « 53 files and 8207 lines ». Le code compte aujourd'hui 10 374 lignes (`CONTEXTE.md:33`).
- Pourquoi c'est un problème : la décision P2 fait de l'extraction de `core` la première tâche du portage (`PORTING.md:63-64`), et cette tâche part d'un budget faux. Les cycles empêchent de tester un paquet sans charger les autres. De plus, un fichier « commun » tire des classes client.
- Correctif proposé : faire de `common` une feuille. Déplacer `TerminalContext` vers `container`, `PatItems` et `PatRecipes` vers un paquet `registry`, et les identifiants d'écran vers une classe sans import client. Créer de petits types valeur (par exemple `MachineRef` = dimension + position) pour libérer `ProviderPairing` de `BlockPos` et de `ProviderSnapshot`. Refaire la carte T0–T4 de `PORTING.md` en comptant aussi les noms complets.
- Porte : double sens
- Coût : M

### C-AR-07 — Le client recalcule la recette et écrit dans des slots que le serveur synchronise aussi
- Sévérité : majeur
- Statut : PROBABLE
- Où : `common/EditorInventory.java:97-154`, `213-221` ; `container/ContainerPatEditor.java:48-49`, `385-395` ; `client/gui/GuiPatEditor.java:1013`
- Constat : côté client, chaque slot reçu appelle `updateRecipeInfo`. Cette méthode lance `generateFromStacks` et réécrit les slots 81 à 98. Le serveur envoie aussi ces mêmes slots : deux écrivains modifient donc un même état. Le bouton Save s'active selon le `recipeInfo` que le client calcule. La Javadoc du conteneur affirme le contraire.
- Preuve :
  - `ContainerPatEditor.java:48-49` dit : « The recipe is built **by the server** (decision D05), on every slot change. The client only displays the result. »
  - `EditorInventory.java:100-108` décrit le bug que ce double calcul a causé (« it cost a release »).
  - `ContainerPatEditor.java:392` appelle `editor.updateRecipeInfo();` dans `onUpdate`, côté client.
  - `GuiPatEditor.java:1013` contient `saveButton.enabled = editor.recipeInfo != null;`.
  - HYPOTHÈSE à trancher : la recherche d'une recette `crafting` utilise le registre de recettes local. Si un serveur ajoute des recettes que le client n'a pas (script côté serveur seulement), le client désactive Save alors que le serveur accepterait la recette. Test : un serveur dédié avec une recette ajoutée par un script serveur seul, puis essayer de sauver.
- Pourquoi c'est un problème : l'état visible dépend de l'ordre d'arrivée des paquets, et cela a déjà cassé une release. La règle D05 n'est pas tenue : chaque changement de l'éditeur doit donc raisonner sur deux calculs.
- Correctif proposé : côté client, `EditorInventory` stocke les slots sans recalculer. Le serveur synchronise la validité (`@GuiSync boolean valid`) et reste seul à écrire les slots 81 à 98.
- Porte : double sens
- Coût : S

### C-AR-08 — Quatre portes d'ouverture du terminal sans fil, quatre jeux de vérifications, et un PITFALL faux
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `item/ItemWirelessPatTerminal.java:63-81`, `network/PacketOpenTerminal.java:116-151`, `integration/wut/WutEventHandler.java:52-75`, `proxy/PatGuiHandler.java:160-188`
- Constat : le clic droit, la touche, AE2WUT et le handler ne vérifient pas les mêmes choses. Seul `PacketOpenTerminal` teste la station de sécurité. Les commentaires justifient ces gardes par un `Long.parseLong` qui lèverait une exception. Or, dans AE2UEL 0.56.5, cette exception est attrapée.
- Preuve :
  - `PatGuiHandler.java:163-165` dit : « its constructor reads the link key and calls `Long.parseLong`. On a terminal that was never linked, the key is empty and the call throws. »
  - `.tools/ae2-src/appeng/helpers/WirelessTerminalGuiObject.java:85-91` contient `try { long encKey = Long.parseLong(...) ... } catch (NumberFormatException encKey) { }`.
  - `javap -c` sur le jar AE2UEL 0.56.5 donne `Exception table: 81 112 115 Class java/lang/NumberFormatException`.
- Pourquoi c'est un problème : si la station est détruite, le clic droit ouvre l'écran, puis l'écran se ferme une seconde plus tard. La touche, elle, refuse tout de suite. Chaque correction future doit se faire quatre fois. Le commentaire faux pousse à garder du code inutile.
- Correctif proposé : une seule fonction `WirelessAccess.check(player, stack, slot)`, qui rend une raison de refus ou `null`, et que les quatre portes appellent. Corriger les deux commentaires.
- Porte : double sens
- Coût : S

### C-AR-09 — `PatConfig` mélange réglages client et serveur, sans synchronisation
- Sévérité : mineur
- Statut : PROBABLE
- Où : `common/PatConfig.java:14-76`, `client/gui/GuiPatTerminal.java:474-476`, `container/ContainerPatTerminal.java:186-196`
- Constat : deux côtés lisent `machinesTab` : le serveur pour remplir la charge utile, le client pour afficher l'onglet. Aucun paquet ne transmet la valeur du serveur au client. Rien n'indique de quel côté chaque réglage compte.
- Preuve : `GuiPatTerminal.java:475` contient `return PatConfig.machinesTab ? 2 : 1;`, et `ContainerPatTerminal.java:189` contient `if (!PatConfig.machinesTab)`. `PatNetwork.java:15-23` ne déclare aucun paquet de configuration. Dans `.tools/forge-src/net/minecraftforge/common/config/ConfigManager.java`, Grep `Packet|network|sendTo` ne trouve rien ; seul `sync(String, Config.Type)` (`:146`) lit le fichier local.
- Pourquoi c'est un problème : si un serveur dédié coupe l'onglet, le client affiche un onglet Machines vide. Dans le cas inverse, le serveur envoie des données que l'écran cache.
- Correctif proposé : le serveur dit au client si l'onglet existe, par un booléen dans `PacketProviderList` ou par un `@GuiSync`. Indiquer dans le commentaire de chaque réglage s'il compte côté « serveur » ou côté « client ».
- Porte : double sens pour le booléen ; sens unique si on scinde le fichier de configuration
- Coût : S

### C-AR-10 — Les mêmes aides sont recopiées, et deux façons font la même chose
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : voir la preuve
- Constat : la recherche linéaire d'une recette par `equals` existe en quatre copies, et le parcours de la grille en trois. `allowed()`, `tell()` et les champs `feedback` existent dans les deux conteneurs. L'état d'affichage des messages existe dans les deux écrans, et le calcul de proportion en deux copies. Enfin, `removeHolder` et `insertHolders` refont un parcours de grille par machine, alors que `ProviderSnapshot.machine` existe justement pour l'éviter.
- Preuve :
  - Recherche linéaire : `RecipeWriter.java:285-295`, `ContainerPatEditor.java:1150-1158` et `821-833`, `ProviderPairing.java:350-383`. Or `IRecipeInfo` a un `hashCode` : `.tools/pa-src/thelm/packagedauto/recipe/RecipeInfoProcessing.java:120-123`, `api/MiscUtil.java:361`.
  - Parcours de grille : `ProviderScanner.java:37-48`, `63-78`, `MachineSnapshot.java:64-75`.
  - Conteneurs : `ContainerPatTerminal.java:126-130`, `433-439`, `505-509`, comparés à `ContainerPatEditor.java:905-909`, `929-941`.
  - Écrans : `GuiPatTerminal.java:211-214`, `1005-1020`, comparés à `GuiPatEditor.java:203-208`, `1039-1047`.
  - Proportion : `ContainerPatEditor.java:529-545`, comparé à `RecipeScale.java:25-31`.
  - Machine : `ContainerPatTerminal.java:334-335`, `397-398` (`ProviderScanner.find`), comparés à `ProviderSnapshot.java:40-51` et `RecipeWriter.java:108-114`.
- Pourquoi c'est un problème : chaque copie dérive. `Feedback.java:32-37` raconte déjà une dérive entre les deux écrans. Les recherches linéaires gardent le regroupement en O(n²) sans raison.
- Correctif proposé :
  - un `RecipeSet` qui garde l'ordre (`LinkedHashSet` sur `IRecipeInfo`) ;
  - un `GridWalk.forEach(grid, type, action)` ;
  - une base abstraite `PatContainer extends AEBaseContainer` pour l'accès et le feedback ;
  - une petite classe `FeedbackLine` pour les deux écrans ;
  - `RecipeScale.scale` pour la proportion ;
  - `RecipeWriter.machineOf` partout, au lieu de `ProviderScanner.find`.
- Porte : double sens
- Coût : M

### C-AR-11 — Un seul paquet pour quinze actions, avec un champ `index` à trois sens
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `network/PacketRecipeAction.java:23-56`, `84-130` ; `client/gui/GuiPatTerminal.java:1145-1146` ; `client/gui/GuiPatEditor.java:341-344`
- Constat : `index` contient soit un index de recette, soit une direction (1 ou 0), soit un facteur signé (`:51`). Le paquet envoie une position même quand elle ne sert pas (`BlockPos.ORIGIN` pour `ACTION_OPEN_HOLDERS`). Le serveur choisit l'action par une chaîne de `if` selon la classe du conteneur ouvert, et il ignore sans message une action inconnue.
- Preuve : `PacketRecipeAction.java:29` dit « `index` is 1 forward, 0 backward », `:51` dit « `index` is the factor, negative to divide », et `:127` contient `editor.scaleAll(Math.abs(message.index), message.index < 0);`.
- Pourquoi c'est un problème : chaque nouvelle action donne un sens de plus au même champ et allonge deux chaînes de `if`. Une erreur de sens n'apparaît ni à la compilation ni dans les logs.
- Correctif proposé : un paquet par famille (terminal, éditeur), ou un `enum Action` avec une méthode par action et des champs nommés. Journaliser toute action inattendue.
- Porte : double sens (la même version est obligatoire des deux côtés, voir C-AR-01)
- Coût : M

### C-AR-12 — Code mort et réglage public sans effet
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : voir la preuve
- Constat : le code écrit, sérialise ou traduit plusieurs membres, mais ne les lit jamais.
- Preuve :
  - `PatConfig.java:62-68` : `tooltipStacks` est traduit dans 16 fichiers `.lang`, mais aucun code ne le lit (Grep `tooltipStacks` ne trouve que la déclaration).
  - `ProviderSnapshot.java:22-23` : la Javadoc de `id` dit « Used by the client commands ». Le champ est écrit (`ProviderScanner.java:84`) et envoyé à chaque rafraîchissement, mais personne ne le lit. `PacketRecipeAction.java:16-17` dit le contraire.
  - `ItemWirelessPatTerminal.java:159-162` : personne n'appelle `wutMode()`. De plus, cette méthode lit `PatConfig.wutModeId` sans passer par `WutSupport.mode()`, ce qui recrée le piège décrit en `WutSupport.java:48-56`.
  - `Reference.java:31-34` : trois modids ne servent nulle part, et `CrafterTypes.java:50-67` écrit les mêmes chaînes en dur.
  - `CrafterTypes.java:145-148` : la Javadoc de `all()` dit « Useful for tests and for the startup log », mais personne ne l'appelle.
  - `ContainerPatTerminal.java:115-123`, `ClientHandler.java:44`, `PacketProviderList.java:15` : `lastPayloadBytes` est écrit, jamais lu.
  - `ClientDiagnostics.java:26-72` : un outil de mise au point reste dans le mod livré. Il journalise sept chemins de ressources à chaque lancement.
- Pourquoi c'est un problème : un administrateur règle `tooltipStacks`, et rien ne change. Le code mort trompe le lecteur (`id`, `wutMode`).
- Correctif proposé : brancher `tooltipStacks` sur l'infobulle, ou le retirer. Supprimer le reste. Garder `lastPayloadBytes` seulement pendant la mesure R2.
- Porte : double sens pour le code ; sens unique pour retirer `tooltipStacks` (clé de configuration publique)
- Coût : S

### C-AR-13 — La documentation et les commentaires contredisent le code
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `docs/ARCHITECTURE.md:45-52`, `62-67` ; `docs/DECISIONS.md:44`, `55` ; le code cité ci-dessous
- Constat : la carte d'architecture décrit des paquets et des classes qui n'existent pas, ainsi qu'un flux de données que R2 a abandonné. Plusieurs Javadoc décrivent un comportement ancien.
- Preuve :
  - `ARCHITECTURE.md:45` (`api/`), `:48` (`PartPackagedAutoTerminal`), `:50` (« wireless variant »), `:52` (`integration/packagedexcrafting…`), `:66` (« delta packet, split into chunks ») : rien de cela n'existe. R2 (`DECISIONS.md:34`) et `ContainerPatTerminal.java:166-174` renvoient tout le message.
  - `DECISIONS.md:55` dit que la table R1 « lives in the integration modules ». Elle vit en fait dans `common/CrafterTypes.java`. D21 (`DECISIONS.md:44`, « without a mixin ») ne porte pas la mention « remplacée par D33 ».
  - `WutSupport.java:22-23` cite « revision R4 ». Or R4 parle de Cleanroom ; la preuve AE2WUT est D33.
  - `ContainerPatTerminal.java:243-245` dit « The terminal pulls **nothing** out of the network on its own ». Cela contredit D40 et `:262-269`.
  - `PacketRecipeAction.java:27` dit « then returns to the terminal », mais `:111-112` dit « We stay in the editor ».
  - Javadoc orphelines ou en double : `GuiPatTerminal.java:112-116`, `1479-1486`, `1787-1799` (deux versions contradictoires de `Line`), `ContainerPatEditor.java:197-204`, `453-458`, `ItemWirelessPatTerminal.java:164-169`. `ItemWirelessPatTerminal.java:154` dit « read by the container », mais les appelants sont `PacketOpenTerminal:144` et `WutEventHandler:62`.
- Pourquoi c'est un problème : `CLAUDE.md` impose de lire la documentation avant d'écrire du code. Une carte fausse envoie le prochain développeur, ou le prochain agent, au mauvais endroit.
- Correctif proposé : réécrire `ARCHITECTURE.md` d'après les paquets réels. Ajouter à D21 la mention « remplacée par D33 ». Corriger les six Javadoc fausses et supprimer les Javadoc orphelines.
- Porte : double sens
- Coût : S

### C-AR-14 — « machine » nomme deux concepts, et « pattern » aussi
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `common/MachineSnapshot.java:15-23`, `container/ContainerPatTerminal.java:103-106`, `common/ProviderPairing.java:34`, `common/ProviderSnapshot.java:51`, `common/RecipeWriter.java:49-50`, `client/gui/GuiPatTerminal.java:1804` ; `en_us.lang` (`tab_patterns=Patterns`) ; `common/EditorInventory.java:144-151`
- Constat : `MachineSnapshot` et `ContainerPatTerminal.machines` désignent les crafters. `Group.machines`, `ProviderSnapshot.machine` et `Result.changed` (« Machines actually changed ») désignent les providers. L'écran dit encore `crafter`. L'onglet « Patterns » montre des recettes, alors que l'API appelle `IPackagePattern` le paquet tiré d'une recette, et c'est ce paquet que montre l'aperçu de l'éditeur.
- Preuve : les citations ci-dessus. `MachineSnapshot.java:16` dit « A machine that **runs** the packages », et `ProviderPairing.java:34` déclare `public final List<ProviderSnapshot> machines`.
- Pourquoi c'est un problème : dans le même écran, `group.machines` et `container.machines` portent le même nom mais ont des types différents. Aucun document n'écrit le vocabulaire du domaine (`GLOSSARY.md` est absent, `CONTEXTE.md:44`).
- Correctif proposé : écrire un glossaire court (provider, crafter, holder, recipe, package). Renommer `MachineSnapshot` en `CrafterSnapshot`, `machines` en `crafters` dans le conteneur, et `Group.machines` en `providers`.
- Porte : double sens (la clé NBT `Machines` reste interne au paquet, et la même version est obligatoire)
- Coût : S

### C-AR-15 — La géométrie des slots est recalculée à la main, et la mise en page vit dans les conteneurs serveur
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `client/gui/GuiPatEditor.java:1129-1130`, `1258-1267` ; `container/ContainerPatEditor.java:218-243` ; `container/ContainerPatTerminal.java:42-97`
- Constat : `drawSelectedTab` et `drawDisabledSlots` refont le calcul de position de `bindEditorSlots`. Seule `editableSlotAreas` lit la position dans les slots, et son commentaire explique pourquoi. `ContainerPatTerminal` porte 24 constantes de mise en page, mais le conteneur n'en utilise que deux.
- Preuve : `GuiPatEditor.java:1117-1120` raconte le bug né de cette copie (le cadre vert sortait de l'écran). `GuiPatEditor.java:1189-1191` dit : « The geometry is read from the slots themselves … the two can therefore never drift apart. » `ContainerPatTerminal.java:143` n'utilise que `PLAYER_INVENTORY_OFFSET_X` et `PLAYER_INVENTORY_TOP`.
- Pourquoi c'est un problème : la même dérive peut revenir sur le voile des slots. De plus, une classe serveur change à chaque retouche visuelle.
- Correctif proposé : lire `slot.xPos` et `slot.yPos` partout dans l'écran. Déplacer les constantes de mise en page vers une classe de `client.gui`, et ne garder dans le conteneur que la position de l'inventaire.
- Porte : double sens
- Coût : S

### C-AR-16 — Une table statique couple en secret le terminal et le handler d'écran
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `proxy/PatGuiHandler.java:56-76`, `109-112` ; `container/ContainerPatTerminal.java:537-540`
- Constat : pour ouvrir l'éditeur, il faut deux appels dans un ordre précis, `setPendingEdit` puis `setPendingRecipe`, sur une `HashMap` statique. Si le premier appel manque, le second ne fait rien et ne le signale pas. Si l'ouverture échoue avant `remove`, une entrée reste en mémoire.
- Preuve : `PatGuiHandler.java:71-76` contient `if (target != null) { target.recipe = recipe; }`, et `:101-104` retourne `null` avant le `PENDING.remove` de `:109`.
- Pourquoi c'est un problème : rien n'impose l'ordre des appels. Une entrée oubliée peut servir à la prochaine ouverture du même joueur.
- Correctif proposé : une seule méthode `PatGuiHandler.openEditor(player, context, target)` qui écrit la cible, ouvre l'écran, puis nettoie la table dans un `finally`.
- Porte : double sens
- Coût : S

### C-AR-17 — Les tables d'addons et les constantes d'énergie sont codées en dur
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `common/CrafterTypes.java:49-68`, `83-86` ; `common/ProviderRole.java:37-43` ; `item/ItemWirelessPatTerminal.java:37-40`
- Constat : pour reconnaître un crafter ou un provider d'un nouvel addon, il faut une nouvelle version du mod. Le coût par tick du terminal sans fil est configurable, mais la capacité et le coût d'ouverture ne le sont pas.
- Preuve : `CrafterTypes.java:50-67` contient huit appels `map(...)` littéraux. Voir aussi `ProviderRole.java:38-42`. `ItemWirelessPatTerminal.java:38` déclare `POWER_CAPACITY = 1_600_000d` et `:40` déclare `POWER_PER_OPEN = 0.5d`, alors que `PatConfig.java:37-39` rend le coût par tick configurable.
- Pourquoi c'est un problème : un auteur de pack ne peut pas brancher un addon que le mod ne prévoit pas. Or R1 voulait cette table « dans chaque module d'intégration » (`DECISIONS.md:33`).
- Correctif proposé : garder la table intégrée comme valeur par défaut, et accepter en plus une liste `type=classe` dans la configuration. Faire de même pour les deux constantes d'énergie, si un besoin réel apparaît.
- Porte : sens unique (nouvelles clés de configuration publiques)
- Coût : S

### C-AR-18 — Les commentaires racontent l'histoire des bugs au lieu de décrire le code
- Sévérité : cosmétique
- Statut : VÉRIFIÉ
- Où : tout `src/main/java`
- Constat : le code contient 33 `PITFALL` et 21 `FIXED`, et 26 % de ses lignes sont des commentaires. Beaucoup de ces commentaires disent « used to… », ce qui a sa place dans `CHANGELOG.md` ou `DECISIONS.md`. Ces récits vieillissent : C-AR-08 et C-AR-13 en montrent plusieurs devenus faux.
- Preuve : `grep -r PITFALL src/main/java | wc -l` donne 33, et `grep -r FIXED … | wc -l` donne 21. On compte 2 745 lignes de commentaire sur 10 374. Les fichiers les plus chargés sont `GuiPatTerminal.java` (455 sur 1 856) et `ContainerPatEditor.java` (372 sur 1 411).
- Pourquoi c'est un problème : le lecteur doit trier l'historique pour trouver la règle actuelle. Un PITFALL faux pousse à garder du code inutile.
- Correctif proposé : garder dans le code la règle et sa preuve (« AE2 exige X, voir `fichier:ligne` »). Déplacer le récit « FIXED » vers le CHANGELOG. Faire ce tri à chaque modification d'un fichier, pas dans un seul gros commit.
- Porte : double sens
- Coût : M

---

## Ce qui est bien fait

- La logique déjà extraite est pure et testée : `SearchQuery`, `RecipeScale`, `Feedback`, `GroupNames.sanitize`, `ProviderPairing` (44 tests). Continuez dans ce sens.
- Toutes les écritures passent par un seul chemin : `RecipeWriter.editHolder` sert à la machine et au mode holder. Les écritures de groupe valident tout avant d'écrire (`scaleAll`, proportions).
- Le serveur reste l'autorité : les paquets portent des intentions, et le serveur refait le scan et vérifie `BUILD`. Les paquets entrants sont bornés (`PacketEditorFill`, `PacketRenameGroup`).
- Les intégrations restent optionnelles, sans dépendance de compilation : mixins AE2WUT par nom avec repli par réflexion, `FluidPackets` en NBT pur, rôles par nom de classe.
- `DECISIONS.md` garde la raison et la preuve de chaque choix. C'est ce qui a rendu cet audit possible.
