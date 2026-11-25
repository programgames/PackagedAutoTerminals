# Constats — lentille correction-minecraft

Audit du 2026-11-25, branche `audit/2026-11-25` (`85dadf6`). Sources de référence : `.tools/forge-src` (Forge 14.23.5.2847, noms MCP), `.tools/ae2-src` (AE2UEL 0.56.5), `.tools/pa-src` (PackagedAuto 1.0.24.73). Gradle n'a pas été lancé. Le jar `build/libs/packagedautoterminals-1.12.2-0.1.0-beta.10.jar` a été lu avec `javap`.

Bilan : 1 bloquant, 4 majeurs, 10 mineurs, 3 cosmétiques.

> Note de l'orchestrateur : le harnais a refusé l'écriture de ce fichier au sous-agent (« Subagents should return findings as text »). L'orchestrateur l'a recopié tel quel depuis son rapport final.

---

### C-MC-01 — L'instantané du terminal n'a pas de plafond, et le client se déconnecte au-delà de 2 Mo NBT
- Sévérité : bloquant (quand le seuil est atteint)
- Statut : PROBABLE (le seuil réel n'est pas mesuré)
- Où : `container/ContainerPatTerminal.java:163-184`, `common/ProviderSnapshot.java:53-65`, `network/PacketProviderList.java:23-27`
- Constat : à chaque changement, le serveur envoie un seul `NBTTagCompound`. Ce tag contient toutes les recettes de toutes les machines du réseau, et chaque machine d'une paire envoie sa propre copie de la même liste. Le client lit ce tag avec `ByteBufUtils.readTag`, qui impose un plafond de 2 097 152 octets comptés. Au-delà, l'exception remonte jusqu'à `FMLProxyPacket`, qui coupe la connexion.
- Preuve :
  - `ContainerPatTerminal.java:166-174` : `wrapper.setTag("Providers", buildPayload()); wrapper.setTag("Machines", buildMachinePayload()); ... PatNetwork.CHANNEL.sendTo(new PacketProviderList(wrapper), ...)`. `ProviderSnapshot.java:64` : `tag.setTag("Recipes", MiscUtil.writeRecipeListToNBT(new NBTTagList(), recipes));` (une liste par machine, pas par groupe).
  - `PacketProviderList.java:26` : `payload = ByteBufUtils.readTag(buf);`
  - `.tools/forge-src/net/minecraftforge/fml/common/network/ByteBufUtils.java:222-227` → `pb.readCompoundTag()` ; `net/minecraft/network/PacketBuffer.java:344` : `CompressedStreamTools.read(new ByteBufInputStream(this), new NBTSizeTracker(2097152L));`
  - `net/minecraft/nbt/NBTSizeTracker.java:25-33` : `throw new RuntimeException("Tried to read NBT tag that was too big; ...")`.
  - `net/minecraftforge/fml/common/network/internal/FMLProxyPacket.java:129-133` : `catch (Throwable t) { ... dispatcher.rejectHandshake("A fatal error has occurred, this connection is terminated"); }`
  - Le transport découpe jusqu'à 255 parts (`FMLProxyPacket.java:154-171`). Le plafond vient donc de la lecture NBT, pas du paquet.
  - HYPOTHÈSE de seuil : une pile sérialisée par `MiscUtil.saveAllItems` (`Index`, `id`, `Count`, `Damage`) pèse de l'ordre de 300 à 400 octets comptés (`NBTTagCompound.java:47,61,66`). Une recette de traitement à 5 piles pèse environ 2 Ko, et une paire pleine (2 × 20 recettes) environ 80 Ko. Le plafond tombe donc vers 15 à 25 paires pleines sur un seul réseau.
  - `ContainerPatTerminal.java:115-123` dit que `lastPayloadBytes` existe pour la mesure que demande la révision R2. Le même commentaire dit qu'aucune mesure n'a été prise.
- Pourquoi c'est un problème : le joueur qui a le plus besoin du mod (un gros réseau PackagedAuto) perd la connexion à chaque ouverture du terminal, en solo comme en multijoueur. Le constat ne conteste pas R2 (renvoi complet plutôt que deltas). Il conteste l'absence de plafond et de mesure.
- Correctif proposé : mesurer d'abord. Créez un monde de test avec N paires pleines, puis lisez `payloadBytes`, que `PacketProviderList.fromBytes` stocke déjà. Ensuite, envoyez les recettes une seule fois par groupe ou par table dédupliquée. Découpez l'instantané en plusieurs messages sous un plafond fixe (par exemple 1 Mo compté).
- Porte : double sens (format de paquet ; FML exige la même version de mod des deux côtés)
- Coût : M

### C-MC-02 — Les clics sur les cases fantômes ne passent pas par `ContainerPatEditor.slotClick`
- Sévérité : majeur
- Statut : VÉRIFIÉ (chemin de code, sources AE2UEL 0.56.5) ; non reproduit en jeu
- Où : `container/ContainerPatEditor.java:297-340`, `container/ContainerPatEditor.java:949-972`, `container/ContainerPatEditor.java:1184-1189`, `client/gui/GuiPatEditor.java:481-510`
- Constat : pour une `SlotFake`, l'écran AE2 n'envoie pas de clic vanilla. Il envoie un `PacketInventoryAction`, que le serveur traite dans `AEBaseContainer.doAction`. `ContainerPatEditor` redéfinit `slotClick`, mais pas `doAction`. La branche `SlotFake` de `slotClick` (contrôle `isEditable`, `dirty = true`, clic droit qui vide) ne s'exécute donc jamais pour un clic de souris, un glisser ou une touche numérique.
- Preuve :
  - `.tools/ae2-src/appeng/client/gui/AEBaseGui.java:432-441` : `} else if (slot instanceof SlotFake) { ... PacketInventoryAction p3 = new PacketInventoryAction(action2, slotIdx, 0L); NetworkHandler.instance().sendToServer(p3); return; }`. Glisser : `AEBaseGui.java:375-378`.
  - `appeng/core/sync/packets/PacketInventoryAction.java:134-135` → `doAction`. `appeng/container/AEBaseContainer.java:413-421` : `if (s instanceof SlotFake) { ... case PICKUP_OR_SET_DOWN: ... s.putStack(hand.copy());`. `AEBaseContainer.java:448-452` : `case SPLIT_OR_PLACE_SINGLE: ... is.setCount(Math.max(1, is.getCount() - 1));`
  - `AppEngSlot.java:107-114` appelle `getContainer().onSlotChange(this)`, et `AEBaseContainer.java:805-806` laisse `onSlotChange` vide. `javap -p` sur le jar : `ContainerPatEditor` n'a aucune méthode `doAction`. `grep` : `dirty = true` n'apparaît qu'aux lignes 335, 427, 449, 561, 576, 1165 et 1352.
  - `GuiPatEditor.java:492` n'intercepte que les boutons 0 et 2. Le clic droit part donc vers `AEBaseGui.handleMouseClick`.
- Pourquoi c'est un problème :
  - Un objet posé au clic ne lève pas `dirty`. Un clic sur un autre onglet (`selectTab`, ligne 965) ou sur « Mettre à l'échelle » (`scaleAll`, ligne 1184) recharge alors l'onglet sans avertir. Le travail non enregistré disparaît, et le point rouge n'apparaît pas.
  - Le clic droit à main vide décrémente la quantité jusqu'à 1, mais ne vide jamais la case. Cela contredit le commentaire `ContainerPatEditor.java:327-328` et le test `docs/TESTING.md:208-210` (D17).
  - Le serveur accepte un objet dans une case que le type désactive.
  - Les tests D6 et D17 sont notés comme joués. Deux explications sont possibles : les testeurs ont utilisé la molette, ou le fork AE2UEL du joueur diffère de 0.56.5. Pour trancher en une minute, posez un objet au clic, puis cliquez un autre onglet.
- Correctif proposé : redéfinissez `doAction` dans `ContainerPatEditor` pour les `SlotFake` (contrôle `isEditable`, `dirty = true`, sémantique du clic droit). Une autre option est de redéfinir `onSlotChange` pour lever `dirty`. Supprimez ensuite la branche morte de `slotClick`.
- Porte : double sens
- Coût : S

### C-MC-03 — La suppression depuis le terminal vise un index, et peut effacer une autre recette
- Sévérité : majeur
- Statut : PROBABLE
- Où : `client/gui/GuiPatTerminal.java:1209-1211`, `client/gui/GuiPatTerminal.java:1223-1227`, `client/gui/GuiPatTerminal.java:1617-1621`, `container/ContainerPatTerminal.java:212-226`
- Constat : le client envoie la position de la recette dans `group.recipes`. Il calcule cette position sur un instantané qui peut avoir jusqu'à `refreshTicks` d'âge. Le serveur refait le groupement, puis supprime `group.recipes.get(index)` dans toutes les machines du groupe. L'éditeur a reçu le correctif `openedRecipe` pour ce même problème (`ContainerPatEditor.java:97-108`), mais pas le terminal.
- Preuve :
  - `GuiPatTerminal.java:1620` : `recipes.add(Line.recipe(group, recipe, index));`, puis ligne 1210 : `send(clicked.anchor(), clicked.recipeIndex, PacketRecipeAction.ACTION_REMOVE);`
  - `ContainerPatTerminal.java:221-226` : `if (group == null || index < 0 || index >= group.recipes.size()) return; RecipeWriter.apply(grid, getActionSource(), group.machines, group.recipes.get(index), null);`
  - L'ordre de l'union dépend de l'ordre du scan : `ProviderPairing.java:163-181` ajoute les recettes du second groupe nommé à la suite du premier. Le scan parcourt `.tools/ae2-src/appeng/me/Grid.java:34` (`HashMap`) et `appeng/me/MachineSet.java:11-12` (`extends HashSet<IGridNode>`). Leur ordre change quand le réseau change.
  - `ProviderSnapshot.java:22-23` déclare un `id` « stable pour la session » que personne n'utilise.
- Pourquoi c'est un problème : deux cas réels existent. (1) Un double Maj + clic droit rapide sur un serveur à 150 ms : le second paquet arrive avant le nouvel instantané (`refreshNow`, lignes 544-547, n'envoie qu'au tick suivant). Il efface alors la recette qui a glissé à cet index. (2) Un groupe fusionné par nom, après un changement du réseau. Dans les deux cas, la recette effacée était enregistrée dans toutes les machines du groupe, et aucun message d'erreur n'apparaît.
- Correctif proposé : envoyez l'identité de la recette (son NBT, ou un hachage plus l'index). Refusez avec le message existant `group_changed` quand elle ne correspond plus, comme `ContainerPatEditor.save` le fait déjà.
- Porte : double sens (format de paquet ; FML impose la même version des deux côtés)
- Coût : S

### C-MC-04 — Un nom de groupe survit à la machine et fusionne un groupe étranger
- Sévérité : majeur
- Statut : PROBABLE
- Où : `common/GroupNames.java:93-106`, `common/ProviderScanner.java:94-101`, `common/ProviderPairing.java:163-181`, `container/ContainerPatEditor.java:874-879`
- Constat : le mod stocke un nom par position et par dimension. Seul `renameGroup` écrit ou efface ce nom, et aucun code ne retire l'entrée quand la machine est cassée. Une machine posée plus tard à la même position hérite donc du nom. `mergeNamed` la fusionne alors avec le groupe qui porte ce nom.
- Preuve :
  - `grep` sur `GroupNames|names.set|BreakEvent|BlockEvent` : le seul appel d'écriture est `ContainerPatEditor.java:878` (`names.set(machine.dimension, machine.pos, name);`). Le mod n'écoute aucun événement de destruction.
  - `ProviderScanner.java:99-100` : `snapshot.customName = GroupNames.get(tile.getWorld()).get(snapshot.dimension, snapshot.pos);`
  - `ProviderPairing.java:175-179` : `first.machines.addAll(group.machines); ... groups.remove(group);`
  - Toutes les écritures visent `group.machines` : `ContainerPatEditor.java:632-642` (enregistrement), `ContainerPatTerminal.java:225-226` (suppression), `RecipeWriter.mapMachines` (mise à l'échelle).
- Pourquoi c'est un problème : exemple. Le joueur remplace un Packager de la paire « Fer » par un Packager d'une autre chaîne, au même endroit. Le terminal montre alors une seule ligne « Fer ». Chaque enregistrement, suppression ou mise à l'échelle écrit dans les deux chaînes, et les recettes de deux automatisations se mélangent sans avertissement. La table grossit aussi sans limite.
- Correctif proposé : effacez l'entrée sur `BlockEvent.BreakEvent` aux positions concernées. Ignorez aussi une entrée dont la classe de machine ne correspond plus. Si vous enregistrez la classe au moment du nommage, le format de `GroupNames` change : prévoyez la lecture sans ce champ (valeur par défaut « inconnue »).
- Porte : double sens pour la purge ; sens unique si le format de `GroupNames` gagne un champ (accord de Julien requis)
- Coût : M

### C-MC-05 — L'éditeur rescanne et regroupe tout le réseau à chaque tick
- Sévérité : majeur
- Statut : PROBABLE (coût estimé, pas mesuré)
- Où : `container/ContainerPatEditor.java:355-383`, `container/ContainerPatEditor.java:1310-1327`, appels explicites aux lignes 428, 450, 562 et 577
- Constat : `detectAndSendChanges` appelle `refreshTabs()` à chaque tick. `refreshTabs` appelle `currentGroup()`, qui exécute `ProviderScanner.scan` puis `ProviderPairing.group`. Le terminal limite ce travail à `refreshTicks`, mais l'éditeur ne le limite pas. Chaque paquet de molette, de case fantôme ou de remplissage JEI rappelle aussi `detectAndSendChanges()`, ce qui ajoute un scan.
- Preuve :
  - `ContainerPatEditor.java:379-380` : `groupName = GroupNames.get(world).get(dimension, pos); refreshTabs();`, hors de tout test de `ticks`.
  - `.tools/forge-src/net/minecraft/entity/player/EntityPlayerMP.java:365` : `this.openContainer.detectAndSendChanges();` dans `onUpdate`, donc 20 fois par seconde.
  - Le scan relit le NBT de chaque porte-recettes : `.tools/pa-src/thelm/packagedauto/util/RecipeListHelper.java:25-27,45-50`. Le cache `MiscUtil.java:67` est limité à 500 entrées et indexé par `NBTTagCompound`. `NBTTagCompound.java:570-573` calcule un hachage profond.
  - `ProviderPairing.java:335-341` recalcule `distinctCount(provider.recipes)` (O(R²)) pour chaque couple machine × groupe, soit environ M × G × R² comparaisons de recettes.
- Pourquoi c'est un problème : avec 80 machines à 20 recettes, l'ordre de grandeur est de quelques centaines de milliers d'appels `equals` par tick et par éditeur ouvert. Au-delà de 500 recettes distinctes, le cache PackagedAuto déborde, et chaque tick reconstruit les recettes (avec `CraftingManager.getRecipe`). Ce coût porte sur le fil serveur, donc sur tous les joueurs.
- Correctif proposé : gardez le groupe calculé dans l'éditeur. Recalculez-le seulement au rythme de `refreshTicks` ou après une écriture. Retirez le scan des appels explicites à `detectAndSendChanges()`. Mesurez avant et après avec un profileur (spark) sur un réseau de test.
- Porte : double sens
- Coût : S

### C-MC-06 — Le terminal sans fil ne verrouille pas sa case et ne vérifie pas que l'objet est toujours là
- Sévérité : mineur
- Statut : PROBABLE
- Où : `container/ContainerPatTerminal.java:132-144`, `container/ContainerPatEditor.java:111-128`, `common/TerminalContext.java:117-124`
- Constat : AE2 verrouille la case du terminal sans fil et ferme l'écran si l'objet quitte cette case. Nos deux conteneurs ne font ni l'un ni l'autre. Le mod tire l'énergie de la pile capturée à l'ouverture, même après le départ de l'objet.
- Preuve :
  - `.tools/ae2-src/appeng/container/implementations/ContainerMEPortableTerminal.java:54-56` : `this.lockPlayerInventorySlot(slotIndex);`. Lignes 95-107 : comparaison `currentItem != this.wirelessTerminalGUIObject.getItemStack()`, puis `setValidContainer(false)`.
  - `appeng/helpers/WirelessTerminalGuiObject.java:79` (`this.effectiveItem = is;`) et lignes 214-222 (`this.wth.usePower(this.myPlayer, amt, this.effectiveItem)`).
  - Nos constructeurs appellent `bindPlayerInventory(...)` sans `lockPlayerInventorySlot`, et aucun `detectAndSendChanges` ne relit la case.
- Pourquoi c'est un problème : si le joueur jette le terminal (Q sur la case dans l'écran), l'écran reste ouvert et fonctionne. `splitStack` laisse une pile fantôme de quantité 0 qui garde son NBT d'énergie. Le mod tire donc l'énergie de cette pile fantôme, pas de l'objet réel. Après un déplacement de l'objet, « Retour » depuis l'éditeur ne rouvre rien, car `PatGuiHandler.wireless` lit l'ancienne case.
- Correctif proposé : en mode sans fil, appelez `lockPlayerInventorySlot(slot)` (cases 0 à 35). Fermez le conteneur quand `getStackInSlot(slot) != host.getItemStack()`, comme `ContainerMEPortableTerminal`.
- Porte : double sens
- Coût : S

### C-MC-07 — L'éditeur ignore `disabled_recipe_types` de PackagedAuto
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `container/ContainerPatEditor.java:1345-1353`, `container/ContainerPatEditor.java:1366-1378`, `container/ContainerPatEditor.java:403-409`
- Constat : l'encodeur de PackagedAuto saute les types que la configuration du pack désactive. Notre éditeur parcourt tous les types et propose `crafting` par défaut sans test. Il accepte aussi n'importe quel identifiant de type que le client envoie dans `PacketEditorFill`.
- Preuve :
  - `.tools/pa-src/thelm/packagedauto/inventory/InventoryEncoderPattern.java:139-142` : `do { this.recipeType = RecipeTypeRegistry.getNextRecipeType(...); } while (... TileEncoder.disabledRecipeTypes.contains(this.recipeType.getName().toString()));`
  - `config/PackagedAutoConfig.java:43-44` : `"disabled_recipe_types" ... "The list of recipe types to disable in the Package Recipe Encoder."`
  - `ContainerPatEditor.java:404-409` : `IRecipeType type = RecipeTypeRegistry.getRecipeType(typeId); if (type == null) return; ... editor.recipeType = type;`
- Pourquoi c'est un problème : un pack qui masque un type dans l'encodeur voit ce type revenir par le terminal. Le serveur fait confiance à une valeur du client, contre la règle 2 du projet.
- Correctif proposé : filtrez les trois chemins côté serveur avec `TileEncoder.disabledRecipeTypes`, en gardant `processing` comme le fait l'encodeur. Quand le client demande un type refusé, répondez par un message.
- Porte : double sens
- Coût : S

### C-MC-08 — Le terminal s'ouvre sans la permission que AE2 exige pour l'Interface Terminal
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `part/PartPatTerminal.java:34-43`, `proxy/PatGuiHandler.java:93-107`, `container/ContainerPatTerminal.java:146-175`
- Constat : n'importe quel joueur qui clique la pièce ouvre le terminal. Il reçoit alors la liste complète des recettes, avec la dimension et la position de chaque machine. Seules les écritures testent `BUILD`, alors que AE2 refuse d'ouvrir son Interface Terminal sans `BUILD`.
- Preuve :
  - `.tools/ae2-src/appeng/core/sync/GuiBridge.java:181` : `GUI_INTERFACE_TERMINAL(ContainerInterfaceTerminal.class, PartInterfaceTerminal.class, GuiHostType.WORLD, SecurityPermissions.BUILD)`. Contrôle aux lignes 394-431 (`securityCheck`).
  - `PartPatTerminal.java:41` : `TerminalContext.ofPart(this).openTerminal(player);`, sans test de sécurité. `ProviderSnapshot.java:58-59` écrit `Dim` et `Pos`.
- Pourquoi c'est un problème : sur un serveur à factions, un joueur qui n'appartient pas au réseau voit les recettes. Le bouton « localiser » lui montre aussi l'emplacement des machines.
- Correctif proposé : testez `hasAccess(SecurityPermissions.BUILD, false)` à l'ouverture et dans `refusal()`, comme `GuiBridge`.
- Porte : double sens
- Coût : S

### C-MC-09 — L'écran du terminal regroupe encore les machines à chaque image
- Sévérité : mineur
- Statut : VÉRIFIÉ (chemin d'appel) ; coût non mesuré
- Où : `client/gui/GuiPatTerminal.java:321`, `client/gui/GuiPatTerminal.java:1442-1460`, `client/gui/GuiPatTerminal.java:690`
- Constat : le cache de `lines()` (commentaire des lignes 199-206) a supprimé le regroupement à chaque image dans la liste. Mais `drawFG` appelle `summary()` à chaque image, et `networkSummary()` appelle `ProviderPairing.group`. Chaque ligne visible appelle aussi `groupState`, donc `unevenCount` (O(R × M × R)).
- Preuve : `GuiPatTerminal.java:321` : `fontRenderer.drawString(summary(), statusLeft, summaryY, COLOR_TEXT);`. Ligne 1449 : `return networkSummary();`. Lignes 1458-1459 : `for (ProviderPairing.Group group : ProviderPairing.group(terminalContainer.providers))`.
- Pourquoi c'est un problème : le commentaire déclare ce défaut corrigé (des dizaines de milliers de comparaisons par seconde sur un réseau de 50 machines). Le défaut existe toujours sur le client, tant qu'aucun message n'occupe la barre d'état.
- Correctif proposé : calculez le résumé et l'état de chaque groupe dans `buildLines`, sous la même clé de cache.
- Porte : double sens
- Coût : S

### C-MC-10 — La configuration n'est pas synchronisée, et le client tire des conclusions fausses
- Sévérité : mineur
- Statut : PROBABLE
- Où : `client/gui/GuiPatTerminal.java:473-476`, `client/gui/GuiPatTerminal.java:1516-1535`, `container/ContainerPatTerminal.java:187-191`, `integration/wut/WutSupport.java:65-75`
- Constat : le serveur lit `machinesTab` pour envoyer les machines, et le client le lit pour afficher l'onglet. Si le serveur désactive l'onglet et pas le client, l'onglet s'affiche avec une liste de machines vide. Le diagnostic « orphelin » signale alors chaque type de recette. Chaque côté lit aussi sa propre valeur de `wutModeId`.
- Preuve : `.tools/forge-src/net/minecraftforge/common/config/ConfigManager.java:146-150` lit le fichier du dossier de configuration local (`Loader.instance().getConfigDir()`), sans envoi réseau. `GuiPatTerminal.java:1531` : `if (!present.contains(CrafterTypes.machineClassFor(type))) reported.add(...)`, avec `present` vide.
- Pourquoi c'est un problème : sur un serveur qui coupe l'onglet, tous les joueurs lisent de faux avertissements. Pour `wutModeId`, les mixins du client et du serveur ajoutent des modes différents. La conséquence sur AE2WUT est une HYPOTHÈSE : vérifiez-la sur un serveur dédié avec deux valeurs différentes.
- Correctif proposé : ajoutez un booléen « onglet machines actif » dans l'instantané, et laissez le client suivre la valeur du serveur. Pour `wutModeId`, documentez que la valeur doit être identique des deux côtés.
- Porte : double sens
- Coût : S

### C-MC-11 — `update.json` ne peut jamais annoncer de mise à jour
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `Reference.java:8`, `build.gradle:19`, `build.gradle:57`, `update.json`
- Constat : la version du mod est `1.12.2-0.1.0-beta.10`, mais `update.json` publie `0.1.0-beta.9`. Forge compare les deux avec `ComparableVersion`. Comme `1` > `0`, le mod se croit toujours en avance.
- Preuve : `javap -constants` sur le jar : `VERSION = "1.12.2-0.1.0-beta.10"`. `update.json` : `"1.12.2-latest": "0.1.0-beta.9"`. `.tools/forge-src/net/minecraftforge/common/ForgeVersion.java:295-304` : `if (current.compareTo(latest) < 0) { status = BETA_OUTDATED; ... } else status = BETA;`. Ligne 320 : la liste des changements ne garde que `ver.compareTo(current) > 0`.
- Pourquoi c'est un problème : l'avis de mise à jour, ajouté en beta.8, ne fonctionne pas. Les joueurs restent sur une ancienne version alors qu'une version corrigée existe.
- Correctif proposé : écrivez les clés d'`update.json` avec le préfixe (`1.12.2-0.1.0-beta.10`), ou retirez le préfixe Minecraft de `Reference.VERSION`.
- Porte : double sens
- Coût : S

### C-MC-12 — En solo, le fil client lit la grille du serveur
- Sévérité : mineur
- Statut : PROBABLE
- Où : `container/ContainerPatEditor.java:316-318`, `container/ContainerPatEditor.java:1068-1088`, `proxy/PatGuiHandler.java:186-187`
- Constat : le client exécute `slotClick` en prédiction, donc `selectTab`, puis `load`, puis `currentGroup()`. En mode sans fil, l'hôte côté client est un `WirelessTerminalGuiObject` construit sur le client. En solo, le registre AE2 des stations est partagé entre les deux côtés et ne contient que des objets serveur. `targetGrid` désigne donc la grille du serveur.
- Preuve :
  - `.tools/forge-src/net/minecraft/client/multiplayer/PlayerControllerMP.java:610` : `player.openContainer.slotClick(slotId, mouseButton, type, player);`
  - `.tools/ae2-src/appeng/core/features/registries/LocatableRegistry.java:21,40-42` : `HashMap`, et `getLocatableBy` sans test de côté. `WirelessTerminalGuiObject.java:86-91` en tire `targetGrid`.
  - `WirelessTerminalGuiObject.java:235-236` : `getActionableNode()` appelle `rangeCheck()`, qui parcourt `targetGrid.getMachines(...)` (lignes 256 et 267) depuis le fil client. Le test de monde (`testWap`) échoue sur le client. Mais la branche Quantum Bridge (lignes 267-273) rend un nœud serveur, et `ProviderScanner.scan` parcourt alors toute la grille serveur depuis le fil client.
- Pourquoi c'est un problème : le fil client lit des `HashSet` et du NBT que le fil serveur modifie en même temps. Le risque est une `ConcurrentModificationException` qui arrête le jeu. La fenêtre est étroite, et le cas complet exige un lien Quantum.
- Correctif proposé : dans `slotClick`, traitez `SlotTab` seulement côté serveur (si `player.world.isRemote`, retournez `ItemStack.EMPTY`).
- Porte : double sens
- Coût : S

### C-MC-13 — Chaque renvoi complet de l'éditeur coûte 99 recherches de recette sur le client
- Sévérité : mineur
- Statut : PROBABLE
- Où : `common/EditorInventory.java:212-221`, `container/ContainerPatEditor.java:1092-1096`
- Constat : un changement d'onglet envoie tout l'inventaire. Sur le client, chaque case reçue passe par `setInventorySlotContents`, qui reconstruit la recette hors groupe. Pour un type d'artisanat, chaque reconstruction parcourt tout le registre de recettes.
- Preuve : `.tools/forge-src/net/minecraft/inventory/Container.java:561-566` (`setAll` → `putStack` pour chaque case). `AppEngSlot.java:107-109` → `setStackInSlot`. `EditorInventory.java:217-219` : `if (!grouping) updateRecipeInfo();`. `.tools/pa-src/thelm/packagedauto/recipe/RecipeInfoCrafting.java:141` : `CraftingManager.findMatchingRecipe(...)`. `CraftingManager.java:214-219` parcourt `REGISTRY`.
- Pourquoi c'est un problème : dans un gros pack (des dizaines de milliers de recettes), l'écran saccade à chaque changement d'onglet. Le serveur a déjà le correctif `beginGroup`, mais pas le client.
- Correctif proposé : sur le client, reconstruisez la recette seulement à la réception de `recipeTypeId` ou au dessin, pas à chaque case.
- Porte : double sens
- Coût : S

### C-MC-14 — Champ interne de PackagedAuto lu sans borne haute de version
- Sévérité : mineur
- Statut : VÉRIFIÉ (code) ; rupture future = HYPOTHÈSE
- Où : `common/RecipeWriter.java:43-45`, `PackagedAutoTerminals.java:38-39`
- Constat : `maxRecipes()` lit `thelm.packagedauto.tile.TileEncoder.patternSlots`, un champ hors du paquet `api`. La dépendance accepte `[1.0.24.73,)`, sans borne haute.
- Preuve : `.tools/pa-src/thelm/packagedauto/tile/TileEncoder.java:44` (champ public statique hors API). `grep` : c'est le seul accès du mod à `thelm.packagedauto.tile`.
- Pourquoi c'est un problème : si une version future renomme ce champ, l'enregistrement échoue avec `NoSuchFieldError`. Le joueur n'a pas de refus propre au chargement.
- Correctif proposé : lisez le champ par réflexion avec un repli sur 20, ou bornez la version.
- Porte : double sens
- Coût : S

### C-MC-15 — `giveBack` ignore le reste de l'insertion
- Sévérité : mineur
- Statut : HYPOTHÈSE (chemin inaccessible avec le porte-recettes de PackagedAuto)
- Où : `common/RecipeWriter.java:277-283`
- Constat : quand l'écriture refuse un porte-recettes vierge pris au réseau, le mod le rend au réseau avec `NetworkItems.insert`. Le mod jette la valeur de retour de cet appel.
- Preuve : `RecipeWriter.java:280` : `NetworkItems.insert(grid, holder, source);`, sans lecture du reste. Avec `ItemRecipeHolder`, `editHolder` réussit toujours sur un porte vierge (`ItemRecipeHolder.java:66-68`). Pour atteindre ce chemin, il faut un porte-recettes d'addon qui refuse l'écriture.
- Pourquoi c'est un problème : si le réseau refuse le retour (stockage plein, bus en extraction seule), l'objet disparaît.
- Correctif proposé : si le reste n'est pas vide, rendez-le au joueur (`placeItemBackInInventory`) ou jetez-le à ses pieds.
- Porte : double sens
- Coût : S

### C-MC-16 — La flèche « suivant » du type de recette recule
- Sévérité : cosmétique
- Statut : VÉRIFIÉ
- Où : `client/gui/GuiPatEditor.java:303-308`, `container/ContainerPatEditor.java:1345-1350`
- Constat : le bouton « suivant » envoie `1`. L'éditeur passe alors `forward = true` au paramètre `reverse` de PackagedAuto, et PackagedAuto recule d'un identifiant.
- Preuve : `ContainerPatEditor.java:1349` : `RecipeTypeRegistry.getNextRecipeType(editor.recipeType, forward);`. `.tools/pa-src/thelm/packagedauto/api/RecipeTypeRegistry.java:51` : `getId(type) + (!reverse ? 1 : -1)`. L'encodeur passe `button.id == 0` comme `reverse` (`GuiEncoder.java:150`).
- Pourquoi c'est un problème : les flèches parcourent les types dans l'ordre inverse de l'encodeur.
- Correctif proposé : passez `!forward`.
- Porte : double sens
- Coût : S

### C-MC-17 — `addCheckedInformation` n'a pas `@SideOnly(CLIENT)` dans une classe commune
- Sévérité : cosmétique
- Statut : PROBABLE (aucun plantage attendu, car HotSpot lie les classes à la demande ; aucun démarrage de serveur dédié ne le vérifie)
- Où : `item/ItemWirelessPatTerminal.java:174-181`
- Constat : la méthode référence `ITooltipFlag` et `net.minecraft.client.resources.I18n`, deux classes du client. AE2 déclare la méthode parente `@SideOnly(Side.CLIENT)` : cette méthode n'existe donc pas sur le serveur. `ItemPatTerminal.addInformation` porte bien l'annotation.
- Preuve : `.tools/forge-src/net/minecraft/client/util/ITooltipFlag.java:6` : `@SideOnly(Side.CLIENT)`. `.tools/ae2-src/appeng/items/tools/powered/powersink/AEBasePoweredItem.java:55-56`. `javap -v` : `ItemWirelessPatTerminal.class` ne contient aucune annotation `SideOnly`.
- Pourquoi c'est un problème : si un autre mod appelle `getMethods()` sur cette classe côté serveur, il charge `ITooltipFlag`, et le serveur plante.
- Correctif proposé : ajoutez `@SideOnly(Side.CLIENT)`, comme sur la méthode parente.
- Porte : double sens
- Coût : S

### C-MC-18 — `ClientDiagnostics` ouvre sept ressources sans les fermer
- Sévérité : cosmétique
- Statut : VÉRIFIÉ
- Où : `client/ClientDiagnostics.java:64-70`
- Constat : `getResource(location)` rend un `IResource` qui garde un flux ouvert, et le code jette ce résultat sans le fermer.
- Preuve : `.tools/forge-src/net/minecraft/client/resources/IResource.java:12` : `public interface IResource extends Closeable`.
- Pourquoi c'est un problème : sept descripteurs de fichier restent ouverts dans l'archive du mod jusqu'au passage du ramasse-miettes.
- Correctif proposé : `try (IResource ignored = ...getResource(location)) { ... }`.
- Porte : double sens
- Coût : S

---

## Ce qui est bien fait
- Chaque paquet client → serveur repasse sur le fil serveur (`addScheduledTask`). Le serveur revalide alors le conteneur ouvert, l'appartenance de la machine à la grille et `BUILD` (`PacketRecipeAction.java:88`, `ContainerPatTerminal.java:212-226`).
- Les lectures non fiables sont bornées : nombre d'entrées de `PacketEditorFill` (ligne 53), octets et contenu de `PacketRenameGroup` (lignes 54-58). Le nom du canal tient en 20 caractères (`Reference.java:17`).
- Le mode porte-recettes rend les vrais objets par `clearContainer`, qui les jette au sol en cas de mort ou de déconnexion (`ContainerPatEditor.java:1404-1410`, `Container.java:526-541`).
- Toute écriture passe par `setPatternStack`, qui déclenche `markDirty` et `updatePatternList` (`InventoryPackager.java:45-48`, `InventoryTileBase.java:85-89`). L'éditeur nomme la recette ouverte au lieu de son index.
- Les classes client sont isolées : `@SideOnly` sur la classe, abonnés `value = Side.CLIENT`, et `registerKeys` gardé par le test de côté.
