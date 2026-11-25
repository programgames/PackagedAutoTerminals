# Constats — lentille produit

Audit du 2026-11-25, branche `audit/2026-11-25` (commit `85dadf6`). Je n'ai lancé ni Gradle ni le jeu. Les preuves viennent du code du projet, de ses documents et des sources décompilées de `.tools/` (AE2UEL 0.56.5, PackagedAuto 1.0.24.73, Forge 14.23.5.2847). Les chemins Java sont relatifs à `src/main/java/fr/julien/packagedautoterminals/`.

Point de vue : un joueur qui installe le mod pour la première fois, puis un administrateur de serveur.

Bilan : 4 majeurs, 8 mineurs, 1 cosmétique. Aucun bloquant sous cette lentille.

> Note de l'orchestrateur : le harnais a refusé l'écriture de ce fichier au sous-agent (« Subagents should return findings as text »). L'orchestrateur l'a recopié tel quel depuis son rapport final.

---

### C-PR-01 — Un clic sur le bouton d'une ligne de groupe retire les porte-recettes de toutes les machines, et le terminal ne sait pas les remettre
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `client/gui/GuiPatTerminal.java:821-836`, `:839-847`, `:1199-1203`, `:1217-1220` ; `container/ContainerPatTerminal.java:318-366`, `:381-424`
- Constat : sur un groupe sain, le bouton qui apparaît au survol est le bouton « éjecter ». Un clic simple, avec n'importe quel bouton de souris, renvoie au réseau ME le porte-recettes de **chaque** machine du groupe. L'infobulle de ce bouton affiche `Shift + left click: send the holder to the network`, alors que le clic ne demande pas la touche Maj. Après l'éjection, le même bouton devient « insérer », et il ne prend que des porte-recettes **vierges**. Les porte-recettes éjectés portent leurs recettes, donc ils ne correspondent jamais.
- Preuve :
  - L'infobulle réutilise la clé du geste Maj + clic (`GuiPatTerminal.java:841-847`) :
    ```
    if (action == BUTTON_EJECT) {
        int glyph = groupButton(line.group);
        lines.add(I18n.format(glyph == REPAIR_U
                ? "gui.packagedautoterminals.repair_hint"
                : (glyph == INSERT_U
                        ? "gui.packagedautoterminals.insert_holder_hint"
                        : "gui.packagedautoterminals.remove_holder_hint")));
    ```
    `en_us.lang` contient : `gui.packagedautoterminals.remove_holder_hint=Shift + left click: send the holder to the network`.
  - Le clic ne teste ni la touche Maj ni le bouton de souris (`GuiPatTerminal.java:1199-1203`) : `if (action == BUTTON_EJECT) { send(clicked.anchor(), -1, groupButtonAction(clicked.group)); return; }`.
  - L'éjection vide la machine (`ContainerPatTerminal.java:343-348`) : `machine.setPatternStack(ItemStack.EMPTY);`.
  - L'insertion demande un prototype sans NBT (`ContainerPatTerminal.java:402-403`) : `NetworkItems.extractOne(grid, new ItemStack(blank), getActionSource());`.
  - AE2 compare le NBT. `.tools/ae2-src/appeng/util/item/AESharedItemStack.java:54` appelle `ItemStack.areItemStacksEqual(...)`, et `.tools/forge-src/net/minecraft/item/ItemStack.java:522-528` refuse deux `stackTagCompound` différents.
  - Le test manuel E6 confirme le comportement : `docs/TESTING.md:345` dit « Both recipe holders come back into the ME terminal, with their recipes. »
- Pourquoi c'est un problème : un clic de travers arrête toute l'automatisation du groupe. Le joueur doit retrouver chaque porte-recettes dans le stockage, deviner quelle machine reçoit lequel, puis marcher jusqu'à chaque machine. C'est la marche que le README promet de supprimer (`README.md:36`, « It never sends you back to the machine »). Si le réseau contient des porte-recettes vierges, le bouton « insérer » les pose à la place. Les machines reçoivent alors des listes vides, et le joueur croit avoir réparé. Une paire sans nom peut aussi se séparer en deux lignes, car `ProviderPairing.java:206` ne fusionne deux machines vides que si le choix est unique.
- Correctif proposé : retirez l'éjection du bouton de ligne. Le bouton garde « insérer » et « réparer », les deux gestes qui ajoutent. Gardez l'éjection sur Maj + clic gauche de la ligne, avec une confirmation en deux clics. Le motif existe déjà pour le travail non enregistré (`ContainerPatEditor.java:965-967`). Limitez les boutons de ligne au clic gauche.
- Porte : double sens
- Coût : S

### C-PR-02 — Une recette se supprime en un clic, sans confirmation ni retour possible, y compris par le clic droit documenté pour « éditer »
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `client/gui/GuiPatTerminal.java:754-762`, `:775-797`, `:1209-1212`, `:1223-1226` ; `container/ContainerPatTerminal.java:212-238` ; `client/gui/GuiPatEditor.java:320-321` ; `container/ContainerPatEditor.java:1100-1148`
- Constat : au survol d'une ligne de recette, une croix apparaît au bord droit de la ligne. `actionUnder` ne teste pas le bouton de souris. Un clic droit sur ce bord supprime donc la recette de toutes les machines du groupe. Le README enseigne justement « Right click a recipe row: Edit that recipe » (`README.md:78`). Le bouton Delete de l'éditeur supprime aussi en un clic. Ensuite l'éditeur charge une recette vide (`load(-1)`, `ContainerPatEditor.java:1147`), donc le contenu supprimé disparaît aussi de l'écran.
- Preuve : extrait de `GuiPatTerminal.java:1209-1212`, copié par `sed -n` :
  ```
  if (action == BUTTON_REMOVE) {
      send(clicked.anchor(), clicked.recipeIndex, PacketRecipeAction.ACTION_REMOVE);
      return;
  }
  ```
  Le code entre `:1199` et `:1213` ne teste pas `mouseButton`. Côté serveur, `PacketRecipeAction.java:91-92` appelle `removeRecipe` sans autre condition. `removeRecipe` écrit tout de suite (`ContainerPatTerminal.java:225-226`).
- Pourquoi c'est un problème : la saisie d'une recette de traitement PackagedAuto prend souvent plusieurs minutes (quantités, fluides en mB). Un clic droit trop à droite de quelques pixels efface ce travail. Le mod protège déjà le travail non enregistré (« Unsaved recipe. Click again to leave it. »), mais il ne protège pas le travail enregistré, qui vaut plus.
- Correctif proposé :
  1. Acceptez seulement le clic gauche sur la croix.
  2. Ajoutez une confirmation en deux clics, avec le même motif que `unsaved`.
  3. Après Delete dans l'éditeur, gardez la grille remplie sur l'onglet « nouvelle recette ». Un Save annule alors la suppression, sans code d'annulation dédié.
- Porte : double sens
- Coût : S

### C-PR-03 — La panne que le mod dit la plus fréquente (paire désynchronisée) s'affiche en deux lignes rouges, et le bouton « réparer » répond « Nothing was written »
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `common/ProviderPairing.java:122-149`, `:198-222`, `:335-347` ; `client/gui/GuiPatTerminal.java:821-826`, `:1359-1365` ; `container/ContainerPatTerminal.java:453-503` ; `common/RecipeWriter.java:182-183`
- Constat : une paire sans nom dont un côté porte une recette de plus se sépare en deux lignes. La décision D38 l'accepte (`docs/DECISIONS.md:417-425`) et le test E8 le décrit (`docs/TESTING.md:352`). Chaque ligne contient une seule machine, donc toutes ses recettes sont « d'un seul côté ». Le bouton de ligne prend alors la forme « réparer ». L'infobulle dit « Click: copy the missing recipes to the other side ». Or `repairGroup` copie seulement vers `group.machines`, c'est-à-dire vers la machine qui porte déjà ces recettes. `RecipeWriter.editHolder` rend `UNCHANGED` (`RecipeWriter.java:182-183`), `copied` vaut 0, et le joueur lit « Nothing was written » en rouge.
- Preuve : `ContainerPatTerminal.java:466-471` choisit les recettes dans ce seul groupe, puis `:483-484` écrit `RecipeWriter.apply(grid, getActionSource(), group.machines, null, recipe)`. Au contraire, `save()` ajoute la machine partenaire manquante (`ContainerPatEditor.java:632-639`, `findLonePartner`). Le test N1 (`docs/TESTING.md:665`) couvre seulement le cas où le partenaire est **vide** : `mergeLonePartners` le rattache alors au groupe (`ProviderPairing.java:206`, condition `group.recipes.isEmpty()`).
- Pourquoi c'est un problème : le README présente la synchronisation des deux côtés comme la raison d'être du mod (`README.md:28`). Sur un vrai réseau à plusieurs paires, la dérive produit deux lignes « no unpackager » et « no packager » côte à côte. Leur bouton promet la réparation et échoue. Le seul remède est dans le wiki : taper deux fois le même nom de groupe (`docs/wiki/Troubleshooting.md`, section « Two machines that belong in one group »).
- Test qui le fixerait : dans `ProviderPairingTest`, un Packager {A, B} et un Unpackager {A} donnent deux groupes, et dans chaque groupe `isComplete(A) == false`. En jeu, jouez le test E8, puis cliquez sur le bouton de chaque ligne.
- Correctif proposé : à court terme, masquez le bouton « réparer » sur un groupe qui ne contient qu'un rôle. Remplacez sa ligne d'infobulle par la consigne de nommage. À moyen terme, voyez la feature F3 (lier deux lignes en un geste).
- Porte : double sens
- Coût : S (masquer) / M (geste de liaison)

### C-PR-04 — Le terminal fait 367 px de haut : en 1920×1080, il passe à l'échelle d'interface 2, même si le joueur a choisi une échelle plus grande
- Sévérité : majeur
- Statut : VÉRIFIÉ (calcul sur le code, plein écran 1920×1080)
- Où : `container/ContainerPatTerminal.java:47`, `:59-80` ; `client/gui/GuiScaleFit.java:93-129`
- Constat : `HEIGHT = LIST_TOP + ROWS * ROW_HEIGHT + FOOTER` = 47 + 12 × 18 + 104 = **367**. `GuiScaleFit` baisse l'échelle jusqu'à ce que l'écran tienne dans la fenêtre. Avec 1080 px de haut, l'échelle 4 (« auto ») donne 270 px logiques, et l'échelle 3 donne 360 px. Les deux échelles sont trop petites de quelques pixels. L'écran passe donc à l'échelle 2 (540 px), et son texte s'affiche à la moitié de la taille « auto ».
- Preuve : la règle vanilla, `.tools/forge-src/net/minecraft/client/gui/ScaledResolution.java:30` : `while (this.scaleFactor < i && this.scaledWidth / (this.scaleFactor + 1) >= 320 && this.scaledHeight / (this.scaleFactor + 1) >= 240)`. En 1920×1080, cette règle donne 4. `GuiScaleFit.fits` (`:126-128`) exige `displayHeight / factor >= ySize`, et 1080 / 3 = 360 < 367. Le CHANGELOG annonce 362 px (« grows from 338 to 362 pixels tall »), ce qui ne correspond plus au code.
- Pourquoi c'est un problème : 1920×1080 est la définition d'écran la plus courante. À chaque ouverture de l'écran principal, l'interface rétrécit fortement, puis reprend sa taille à la fermeture, et le joueur lit un texte minuscule. Sept pixels de trop provoquent ce saut. En fenêtre agrandie (barre de titre et barre des tâches), l'éditeur (338 px) peut subir le même saut. Ce dernier point est une HYPOTHÈSE : vérifiez `mc.displayHeight` en jeu.
- Correctif proposé : calculez `ROWS` à partir de la hauteur disponible, entre 6 et 16 lignes, comme les terminaux AE2. La feuille de texture dessine déjà 16 lignes (`ContainerPatTerminal.java:46-49`). Correctif minimal : `ROWS = 10` (331 px), qui tient à l'échelle 3.
- Porte : double sens
- Coût : S (constante) / M (lignes dynamiques)

### C-PR-05 — `machinesTab=false` affiche en rouge « which is not on the network » sur chaque recette de craft, et cette option n'est pas synchronisée entre serveur et client
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `common/PatConfig.java:60` ; `container/ContainerPatTerminal.java:187-191` ; `client/gui/GuiPatTerminal.java:474-476`, `:904-915`, `:1420-1432`, `:1516-1535`
- Constat : avec `machinesTab=false`, le serveur envoie une liste de crafters vide. Le client utilise pourtant cette liste pour l'infobulle de **l'onglet Patterns**. `onNetwork` rend toujours faux, et chaque recette liée à un crafter affiche en rouge « Crafted by X, which is not on the network. ». Le défaut existe aussi en solo. En multijoueur, un client resté à `true` montre en plus un onglet Machines qui déclare orphelines toutes les recettes de craft.
- Preuve : `ContainerPatTerminal.java:189-191` contient `if (!PatConfig.machinesTab) { return list; }`. `GuiPatTerminal.java:909-913` parcourt `terminalContainer.machines`. Forge ne transmet pas une config `@Config` au client. `ConfigManager.sync` lit le fichier du dossier local : `.tools/forge-src/net/minecraftforge/common/config/ConfigManager.java:150` (`Loader.instance().getConfigDir()`) et `:172` (`new File(configDir, name + ".cfg")`). Une recherche dans `.tools/forge-src` ne trouve aucune classe réseau qui appelle `ConfigManager`.
- Pourquoi c'est un problème : le mod se présente comme l'outil qui signale les pannes silencieuses. Ici, une option documentée produit au contraire de fausses pannes en rouge.
- Correctif proposé : envoyez la valeur `machinesTab` du serveur dans le paquet `PacketProviderList`. Masquez l'indice « machine absente » quand le serveur n'a pas fourni de liste.
- Porte : sens unique si le format du paquet change (client et serveur de versions différentes). Double sens si le client masque seulement l'indice quand la liste est vide.
- Coût : S

### C-PR-06 — L'aide en jeu ne décrit aucun des gestes ajoutés depuis beta.9
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `integration/jei/PatJeiPlugin.java:51-57` ; `lang/en_us.lang` (clés `jei.packagedautoterminals.*`) ; `client/PatKeyBindings.java:27-29` ; `common/SearchQuery.java:10-12` ; `README.md:72-81`
- Constat : selon le commentaire de `PatJeiPlugin.java`, les pages JEI sont « la seule aide en jeu ». Elles décrivent le clic gauche, le clic droit et Save. Elles ne décrivent pas :
  - le bouton de ligne (insérer, réparer, éjecter) ;
  - la croix ;
  - l'épingle ;
  - le mode porte-recettes ;
  - le bouton `×N` ;
  - les préfixes de recherche `@mod` et `#type` ;
  - la touche d'ouverture du terminal sans fil.

  Le mod livre cette touche sans assignation (`KeyBinding(..., 0, CATEGORY)`). Une recherche de « key » dans `README.md`, `docs/wiki/*.md` et `docs/curseforge-description.md` trouve seulement « the JEI usage key ». La syntaxe `@`/`#` n'apparaît dans aucun de ces fichiers.
- Pourquoi c'est un problème : sans le wiki, le joueur découvre ces gestes par hasard, souvent par un clic malheureux (voir C-PR-01 et C-PR-02). Le bouton `×N` n'a ni infobulle ni libellé traduit.
- Correctif proposé :
  - Ajoutez une quatrième page JEI « gestes ».
  - Ajoutez une infobulle sur le champ de recherche (`@mod #type texte`) et une infobulle sur `×N`.
  - Ajoutez une ligne sur la touche d'ouverture dans l'infobulle du terminal sans fil.
  - Mettez à jour le tableau « Gestures worth knowing » du README au moment de la publication.
- Porte : double sens
- Coût : S (plus 16 langues à traduire)

### C-PR-07 — Le wiki affirme le contraire du comportement par défaut
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `docs/wiki/Troubleshooting.md:7`, `docs/wiki/Depannage.md:7` ; `docs/wiki/Diagnostics.md` (section « The Machines tab »)
- Constat : la page de dépannage dit « The terminal never removes one from storage on its own. That choice stays yours. » Depuis beta.9, `pullHolderFromNetwork` vaut `true` par défaut (`PatConfig.java:48`). Le terminal prend donc un porte-recettes vierge dans le stockage à l'enregistrement (`RecipeWriter.java:265-274`). La page Diagnostics annonce les états « inactive, busy or idle ». Le code affiche « inactive », « busy », « no recipe » ou un nombre de recettes (`GuiPatTerminal.java:941-954`).
- Pourquoi c'est un problème : sur un serveur, un joueur prudent lit qu'aucun objet ne quitte son stockage sans son accord, puis il constate l'inverse.
- Correctif proposé : corrigez le wiki dans les trois langues (en, fr, zh). Ajoutez une vérification du wiki à la liste de publication.
- Porte : double sens
- Coût : S

### C-PR-08 — L'option `tooltipStacks` ne fait rien
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `common/PatConfig.java:63-68`
- Constat : l'option « Maximum number of items listed in a recipe tooltip » apparaît dans le fichier de config et dans l'écran de config en jeu, traduite en 16 langues. Aucune ligne de code ne la lit : dans `src/`, la recherche de `tooltipStacks` trouve seulement sa déclaration. L'infobulle de recette ne liste plus d'objets (`GuiPatTerminal.java:1397-1398`).
- Pourquoi c'est un problème : un administrateur règle une valeur qui n'a aucun effet, et le mod perd en crédibilité.
- Correctif proposé : supprimez l'option et ses 16 traductions. Forge ignore une clé inconnue dans un ancien fichier de config.
- Porte : sens unique (clé de configuration publique). Impact faible, car le mod ne lit pas cette valeur.
- Coût : S

### C-PR-09 — Le diagnostic « aucun crafter sur ce réseau » se trompe quand le crafter fonctionne hors du réseau ME
- Sévérité : mineur
- Statut : PROBABLE
- Où : `common/MachineSnapshot.java:59-76` ; `client/gui/GuiPatTerminal.java:1516-1535`
- Constat : le diagnostic connaît seulement les crafters inscrits sur la grille AE2. Or PackagedAuto livre un paquet à tout `IPackageCraftingMachine` adjacent à l'Unpackager (`.tools/pa-src/thelm/packagedauto/tile/TileUnpackager.java:227-232`). Un `TileCrafter` hors de la grille fonctionne avec sa propre énergie (`.tools/pa-src/thelm/packagedauto/tile/TileCrafter.java:116-124`, branche `else this.ejectItems()`). Le projet note lui-même qu'un Package Crafting Machine Proxy relaie vers un crafter « often too far away to reach the Unpackager » (`common/CrafterTypes.java`, commentaire de `ROUTERS`).
- Pourquoi c'est un problème : un crafter distant derrière un Proxy, alimenté en RF et sans câble ME, déclenche une fausse alerte rouge. Le joueur cherche alors une panne qui n'existe pas.
- Test qui trancherait : en jeu, construisez une paire, un Proxy et un Package Crafter alimenté en RF sans câble ME. Ouvrez l'onglet Machines.
- Correctif proposé : écrivez l'alerte comme un doute (« aucun crafter de ce type sur le réseau ME ; un crafter hors réseau reste possible »). Affichez-la en orange au lieu du rouge.
- Porte : double sens
- Coût : S

### C-PR-10 — Le refus « mode porte-recettes désactivé » s'affiche dans la barre d'action, sous l'écran ouvert
- Sévérité : mineur
- Statut : PROBABLE
- Où : `proxy/PatGuiHandler.java:84-90` ; `client/gui/GuiPatTerminal.java:404-425`, `:1141-1148`
- Constat : le terminal dessine toujours l'outil « Encoder », avec l'infobulle « Edit recipe holders by hand, with no network », même quand le serveur a `portableEncoder=false`. Le clic envoie une demande au serveur. Le serveur refuse avec `sendStatusMessage(..., true)`, donc dans la barre d'action. Le projet écrit lui-même que cette barre est « drawn below the screen, hence out of sight » (`GuiPatTerminal.java:311-313`).
- Pourquoi c'est un problème : sur un serveur qui désactive la fonction, le bouton semble cassé.
- Correctif proposé : quand un terminal est ouvert, envoyez le refus par le canal `feedback` du conteneur, comme les autres refus.
- Porte : double sens
- Coût : S

### C-PR-11 — L'infobulle du terminal sans fil demande toujours de le lier, même quand il est déjà lié
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `item/ItemWirelessPatTerminal.java:175-181`
- Constat : l'infobulle ajoute toujours « Link it in an ME Security Terminal first ». Elle n'indique jamais si le terminal est lié. Le terminal sans fil d'AE2 affiche « Linked » ou « Unlinked » selon la clé (`.tools/ae2-src/appeng/items/tools/powered/ToolWirelessTerminal.java:93-107`).
- Pourquoi c'est un problème : le joueur doit essayer le terminal pour savoir s'il est prêt.
- Correctif proposé : reprenez la logique d'AE2. Affichez `GuiText.Linked` ou `GuiText.Unlinked` selon `getEncryptionKey(stack)`, et affichez la consigne de liaison seulement pour un terminal non lié.
- Porte : double sens
- Coût : S

### C-PR-12 — L'écran de config en jeu permet de modifier `wutModeId`, mais la modification ne prend effet qu'après un redémarrage
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `common/PatConfig.java:23-30` ; `integration/wut/WutSupport.java:65-75` ; `item/ItemWirelessPatTerminal.java:160-162`
- Constat : `WutSupport.mode()` lit l'option une seule fois, puis garde la valeur. Forge fournit un écran de config pour un mod `@Config` (`.tools/forge-src/net/minecraftforge/fml/client/FMLClientHandler.java:370-372`). Cet écran permet de modifier la valeur en jeu, car l'option ne porte pas `@Config.RequiresMcRestart`. `ItemWirelessPatTerminal.wutMode()` relit la valeur à chaque appel, mais aucun code n'appelle cette méthode.
- Pourquoi c'est un problème : un administrateur qui corrige un conflit d'identifiant croit l'avoir corrigé. Un nouvel identifiant rend aussi muets les terminaux universels déjà assemblés, car leur NBT garde l'ancien numéro (`WutSupport.java:141-145`). Le commentaire de l'option ne le dit pas.
- Correctif proposé : ajoutez `@Config.RequiresMcRestart` sur `wutModeId`. Ajoutez au commentaire une phrase sur la perte du mode. Supprimez `wutMode()`, que rien n'appelle.
- Porte : double sens
- Coût : S

### C-PR-13 — Libellés et textes incohérents
- Sévérité : cosmétique
- Statut : VÉRIFIÉ
- Où : `src/main/resources/assets/packagedautoterminals/lang/*.lang:122,124` ; `lang/en_us.lang` (`recipes_scaled`) ; `client/gui/GuiPatEditor.java:289-290` ; `CHANGELOG.md` (section beta.9, « Changed »)
- Constat :
  - `holder_main=Holder` et `holder_sync=Sync` restent en anglais dans 10 langues : cs, de, es, fr, it, nl, pl, pt, tr, plus `Sync` en ko. En `fr_fr`, toutes les autres clés disent « porte-recettes » : un concept, deux mots.
  - `recipes_scaled=%d recipes scaled` n'a pas de forme au singulier, donc le jeu affiche « 1 recipes scaled » (`ContainerPatEditor.java:1256`).
  - Le bouton `×N` n'a ni libellé traduit ni infobulle.
  - Le CHANGELOG annonce un terminal de 362 px, mais le code donne 367 px (voir C-PR-04).
- Pourquoi c'est un problème : un joueur non anglophone voit un mot anglais isolé dans un écran traduit.
- Correctif proposé : traduisez les deux libellés courts (« Porte-r. », « Synchro »). Ajoutez une clé `recipe_scaled` pour le singulier. Corrigez le CHANGELOG.
- Porte : double sens
- Coût : S

---

## Features proposées

Retirer et simplifier passe avant ajouter.

| # | Feature | Problème du joueur | Coût | Maintenance |
|---|---|---|---|---|
| F1 | **Retirer** l'éjection du bouton de ligne (C-PR-01). Garder Maj + clic, avec une confirmation. | Un clic de travers arrête une paire, et le joueur doit retourner à pied à chaque machine. | S | Aucune : le mod a moins de code. |
| F2 | Confirmation en deux clics pour toute suppression, et grille conservée après Delete (C-PR-02). | Un clic efface une recette longue à saisir. | S | Réutilise le motif `unsaved` qui existe déjà. |
| F3 | Geste « lier à… » : clic sur le bouton d'une ligne séparée, puis clic sur la ligne partenaire. Le serveur écrit le même nom de groupe sur les deux lignes, puis lance la réparation. | Aujourd'hui, la panne la plus fréquente demande de taper deux fois le même nom, et seul le wiki décrit cette astuce (C-PR-03). | M | Aucun nouveau format : réutilise `GroupNames` et `repairGroup`. Le serveur doit vérifier que les deux lignes sont sur la même grille. |
| F4 | Nombre de lignes adapté à la fenêtre, au lieu d'une échelle plus basse (C-PR-04). | En 1080p, le texte devient minuscule à chaque ouverture. | M | Recalculer côté client la position des slots de l'inventaire ; tester en 720p, 1080p et 1440p. |
| F5 | Page JEI « gestes », infobulle de recherche `@mod #type`, infobulle `×N` (C-PR-06). | Les fonctions existent, mais le joueur ne les voit pas. | S | Chaque nouveau geste ajoute une clé à traduire en 16 langues ; `checkLang` le rappelle au build. |

---

## Ce qui est bien fait

- Le serveur revérifie tout à chaque écriture : permission BUILD, présence du groupe, recette ouverte (`group_changed`). Le mod n'écrit jamais un groupe à moitié, et `×N` applique toutes les recettes ou aucune.
- La recette (terminal ME + porte-recettes) passe par l'API AE2. Elle coûte autant que l'Interface Terminal d'AE2, donc elle ne déséquilibre pas le jeu.
- Chaque refus a un message nommé et coloré, affiché dans l'écran. Les corrections de beta.9 ont supprimé les anciens refus silencieux.
- Seize langues sont complètes, et `checkLang` les contrôle au build. La traduction `fr_fr` est soignée.
- Les intégrations restent optionnelles. Le mod livre la touche d'ouverture sans assignation, donc elle ne crée aucun conflit dans un gros pack.
