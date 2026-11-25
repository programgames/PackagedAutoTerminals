# Constats — lentille vérifiabilité (audit du 2026-11-25)

Branche `audit/2026-11-25`, commit `85dadf6`. Question de la lentille : peut-on prouver que le mod marche ?

Méthode. J'ai lu les 5 classes de test, `build.gradle`, les deux workflows, `tools/release.sh`, `docs/TESTING.md`, `docs/PLAN.md`, les décisions D37 à D44, et toutes les classes de `common`, `container` et `network`. J'ai lu aussi la logique de liste de `GuiPatTerminal`. Je n'ai pas lancé Gradle (consigne). Les résultats de build, de test et de `-Xlint:all` viennent de `docs/audit/CONTEXTE.md`. Les preuves API citent `.tools/forge-src`, `.tools/ae2-src` et `.tools/pa-src`.

> Note de l'orchestrateur : le harnais a refusé l'écriture de ce fichier au sous-agent. L'orchestrateur l'a recopié tel quel depuis son rapport final.

| Sévérité | Nombre |
|---|---|
| bloquant | 0 |
| majeur | 8 |
| mineur | 5 |
| cosmétique | 0 |

Mesure de départ : 782 lignes sur 10 374 dans `src/main` (7,5 %) sont dans une classe qui a un test (`Feedback`, `GroupNames`, `ProviderPairing`, `RecipeScale`, `SearchQuery`). Les paquets `container` (1 964 lignes), `client/gui` (3 343) et `network` (672) n'ont aucun test.

---

### C-VE-01 — Mode holder (D43) et mise à l'échelle (D44) : aucune preuve, ni automatique ni en jeu
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `src/main/java/fr/julien/packagedautoterminals/container/ContainerPatEditor.java:704-833` (`saveToHolders`, `writeHolders`, `syncHolders`, `holderRecipes`, `sameRecipes`), `:1180-1302` (`scaleAll`, `scaled`) ; `common/RecipeWriter.java:157-232` (`editHolder`, `mapHolder`, `mapMachines`) ; `src/test/.../RecipeScaleTest.java:13-43`.
- Constat : le commit `85dadf6` ajoute deux fonctions. La première déplace de vrais objets du joueur : deux recipe holders sortis de l'inventaire. La seconde réécrit toutes les recettes d'un groupe. Toute leur logique de décision est dans `ContainerPatEditor`, qui hérite de `AEBaseContainer` et ne s'instancie pas hors du jeu. Les six tests ajoutés couvrent une multiplication et une recherche de doublon. Personne n'a joué les groupes O et P de `docs/TESTING.md`.
- Preuve :
  - `grep -rn "editHolder\|mapHolder\|scaleAll\|syncHolders\|saveToHolders" src/test | wc -l` → `0`.
  - `docs/DECISIONS.md:699` : « Asked by a tester on 2026-10-25, settled and coded the same day. Not tested in game yet. »
  - `docs/TESTING.md:755` : « ## O — Holder mode, with no network 🆕 coded, not tested in game ». `:823` porte la même mention pour P.
  - `CHANGELOG.md:8-24` : les deux fonctions sont sous `[Unreleased]` → `Added`, sans réserve. Rien ne démontre la promesse de `:22`, « Nothing is written unless every recipe can be scaled ».
  - `RecipeScale.java:30` : `return result <= max ? result : -1;`. `RecipeScaleTest.java:19-21` teste seulement `3000 × 2` contre 4096. Aucun test n'essaie `2048 × 2`, donc une erreur `<` au lieu de `<=` passerait.
  - Depuis D43, la sauvegarde en ligne passe aussi par `editHolder` (`docs/DECISIONS.md:748-749`). Une régression dans cette méthode touche donc les deux chemins.
  - `tools/release.sh:28-80` contrôle le build, le CHANGELOG et `update.json`. Il ne contrôle pas l'état des tests en jeu : rien n'empêche de publier ce code tel quel.
- Pourquoi c'est un problème : une erreur ici corrompt la liste de recettes de deux holders ou d'un groupe entier. Aucun retour arrière n'est possible. Le joueur découvre le défaut sur ses machines, pas dans un rapport de test.
- Correctif proposé :
  1. Sortir le cœur « liste » en fonctions statiques pures sur `List<IRecipeInfo>`. Elles doivent couvrir ces opérations :
     - éditer une liste (retrait de toutes les copies, remplacement, ajout borné) ;
     - mapper une liste ;
     - réunir deux holders ;
     - décider du refus `group_full` en mode holder ;
     - planifier une mise à l'échelle (recettes craft sautées, première recette en échec, collision).
     `FakeRecipe` suffit pour les tester.
  2. Ajouter les tests de bord : `2048 × 2` accepté, `2049 × 2` refusé, jumeau vide pour la synchro, jumeau à 20 recettes pour l'ajout.
  3. Jouer O1, O3, O6, P1 et P3 avant toute publication. Ce parcours prend environ dix minutes.
- Porte : double sens
- Coût : M

### C-VE-02 — La CI n'a jamais exécuté un seul test unitaire
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `.github/workflows/build.yml:3-5`, `:27` ; dépôt distant `origin/main`.
- Constat : `build.yml` lance bien `./gradlew build`, donc les tests et `checkLang`. Mais la CI a tourné pour la dernière fois le 2026-09-18, sur `38ff493` (beta.7). Personne n'a poussé les 11 commits suivants, et ce sont eux qui créent `src/test`. Le dépôt distant ne contient aucun fichier de test.
- Preuve :
  - `gh run list --limit 8` → première ligne : `completed success Release 0.1.0-beta.7 build main push 35358656552 1m23s 2026-09-18T14:49:44Z`.
  - `git log --oneline origin/main..HEAD | wc -l` → `11`.
  - `git log --diff-filter=A --format='%h %ad %s' --date=short -- src/test` → `85dadf6 2026-11-25 …` et `5fe03f3 2026-10-01 …`.
  - `git ls-tree -r --name-only origin/main | grep -c "^src/test/"` → `0`.
  - `gh release list` s'arrête à `0.1.0-beta.7`. Le `update.json` local annonce pourtant `"1.12.2-latest": "0.1.0-beta.9"`, et `CHANGELOG.md:41` date beta.9 du 2026-10-01.
- Pourquoi c'est un problème : les 44 tests n'ont tourné que sur une seule machine, avec son cache Gradle. Depuis deux mois et demi, personne n'a prouvé que le build passe sur une machine propre. `CHANGELOG.md` dit que « `gradlew build` runs them », mais la CI ne les a jamais lancés.
- Effet de bord hors lentille, à transmettre à la lentille `produit` : `git show origin/main:update.json` répond « exists on disk, but not in 'origin/main' ». Or `Reference.java:23-24` lit ce fichier sur `main`.
- Correctif proposé : pousser la branche de travail, pas forcément `main`. `build.yml` se déclenche sur toutes les branches (`branches: ["**"]`, `:5`). Ensuite, pousser au moins à chaque fin de lot.
- Porte : sens unique, car c'est un push vers un dépôt distant. Il faut l'accord de Julien.
- Coût : S

### C-VE-03 — Le seul plafond dur du protocole n'est ni mesuré ni testé (révision R2)
- Sévérité : majeur
- Statut : PROBABLE
- Où : `network/PacketProviderList.java:24-27` ; `container/ContainerPatTerminal.java:166-174` ; `client/ClientHandler.java:44` ; `docs/PLAN.md:53-55`, `:147-149`.
- Constat : le terminal renvoie tout l'instantané du réseau dans un seul tag NBT. Côté client, la lecture refuse un tag dont l'allocation estimée dépasse 2 097 152 octets. La seule mesure date du lot 2 : 344 octets, pour une machine et une recette. Personne n'a fait la mesure « réseau chargé » qu'exige R2. Le compteur `lastPayloadBytes` n'apparaît plus nulle part à l'écran.
- Preuve :
  - `PacketProviderList.java:26` : `payload = ByteBufUtils.readTag(buf);`
  - `.tools/forge-src/net/minecraftforge/fml/common/network/ByteBufUtils.java:222-227` : `readTag` appelle `pb.readCompoundTag()`.
  - `.tools/forge-src/net/minecraft/network/PacketBuffer.java:344` : `return CompressedStreamTools.read(new ByteBufInputStream(this), new NBTSizeTracker(2097152L));`
  - `.tools/forge-src/net/minecraft/nbt/NBTSizeTracker.java:27-31` : `this.read += bits / 8L;` puis `throw new RuntimeException("Tried to read NBT tag that was too big; …`.
  - `.tools/forge-src/net/minecraftforge/fml/common/network/FMLIndexedMessageToMessageCodec.java:126-129` : l'exception remonte au codec, qui l'écrit dans le journal.
  - `docs/PLAN.md:147-149` : « The R2 measurement: the size of the snapshot packet on a loaded network. … no figure has been taken. »
  - `grep -rn lastPayloadBytes src/main/java` → `ClientHandler.java:44` (écriture) et `ContainerPatTerminal.java:123` (déclaration). Aucun code ne lit ce champ. `docs/TESTING.md:91` attend pourtant « Hovering gives the packet size ».
  - `docs/DECISIONS.md:391` : le serveur du joueur a déjà « 20 machines and 110 recipes ».
- Pourquoi c'est un problème : au-delà d'une taille de réseau inconnue, le client ne reçoit plus la liste. Au mieux, le terminal reste figé. La suite exacte dans le pipeline Netty, déconnexion ou non, reste une HYPOTHÈSE. Personne ne sait à quelle distance du plafond se trouve le réseau du joueur. L'outil de mesure que prévoit R2 est du code mort.
- Correctif proposé :
  1. Écrire un test JUnit qui tourne sans le jeu :
     - construire le tag `Providers` avec N machines et des tags de recette réalistes (structure relevée dans un holder réel) ;
     - l'écrire avec `CompressedStreamTools.write` ;
     - le relire avec `new NBTSizeTracker(2097152L)` ;
     - trouver la valeur limite de N.
  2. Afficher de nouveau `lastPayloadBytes`, dans l'infobulle ou dans le journal. Les joueurs peuvent alors remonter le chiffre.
  3. Découper le paquet seulement si le chiffre l'exige, comme le prévoit R2.
- Porte : double sens pour le test et l'affichage. Le découpage changerait le format réseau : sens unique.
- Coût : S (test, affichage) ; M (découpage)

### C-VE-04 — Écrire ou mettre à l'échelle pendant un craft : le scénario le plus risqué n'a aucune vérification
- Sévérité : majeur
- Statut : PROBABLE (job AE2 bloqué) ; HYPOTHÈSE (perte d'objets)
- Où : `docs/TESTING.md:736-751` (N9) ; `docs/DECISIONS.md:772` ; `common/RecipeWriter.java:217-232`.
- Constat : le test N9 pose une question : une écriture pendant qu'une machine travaille perd-elle des objets ? Personne n'a encore répondu. D44 élargit le risque, car un seul clic remplace tous les patterns que publie un groupe. D'après le code AE2UEL, un job déjà lancé ne retrouve plus de machine pour un pattern disparu.
- Preuve :
  - `docs/TESTING.md:736` : « ### N9 — Writing while the machine works ⚠ unverified ». `:738` : « This one has no fix behind it. It is here to be answered, not to be confirmed. »
  - `docs/DECISIONS.md:772` : « | Running crafts | Nothing special: same write path as Save, see test N9 | no loss proven | »
  - `.tools/ae2-src/appeng/me/cache/CraftingGridCache.java:470-475` : `getMediums` renvoie `ImmutableList.of()` pour un pattern que la grille ne connaît plus.
  - `.tools/ae2-src/appeng/me/cluster/implementations/CraftingCPUCluster.java:519-522` : le CPU envoie une tâche seulement aux machines de `cc.getMediums(details)`.
  - `.tools/pa-src/thelm/packagedauto/tile/TilePackager.java:203-205` : `setPatternStack` écrit seulement le slot 10. L'état de travail de la machine ne change pas.
- Pourquoi c'est un problème : un job AE2 dont le pattern change, par exemple ×4, ne trouve plus de fournisseur. Ses ingrédients restent alors dans le CPU jusqu'à l'annulation du job. Un test unitaire ne peut pas répondre à cette question : seul le jeu le peut.
- Correctif proposé : faire la QA manuelle la plus courte, qui prend environ cinq minutes.
  1. Lance un craft AE2 de 64 objets sur une paire Packager/Unpackager lente.
  2. Pendant le craft, applique ×2 au groupe depuis l'éditeur.
  3. Regarde le CPU AE2 : le job finit-il, ou reste-t-il bloqué ?
  4. Annule le job, puis compte les objets rendus.
  Si le job bloque, ajoute un avertissement au message. Autre option : refuser l'écriture quand un CPU du réseau travaille. L'API le permet avec `ICraftingGrid.getCpus()` (`.tools/ae2-src/appeng/api/networking/crafting/ICraftingGrid.java:34`) et `ICraftingCPU.isBusy()` (`ICraftingCPU.java:16`).
- Porte : double sens
- Coût : S (QA) ; M (garde)

### C-VE-05 — Quatre refus s'affichent en vert : aucun test ne garde la liste manuelle `Feedback.REFUSALS`
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `common/Feedback.java:39-66` ; `container/ContainerPatEditor.java:933` ; `container/ContainerPatTerminal.java:354`, `:414`, `:437` ; `client/gui/GuiPatEditor.java:1046-1051` ; `client/gui/GuiPatTerminal.java:1014`, `:319`.
- Constat : une liste écrite à la main décide de la couleur d'un message. Son propre commentaire le dit : « **A new refusal key must be added here**, or it will be drawn as a success. » Quatre clés que le code envoie par `tell(...)` sont des refus et manquent à la liste :
  - `machine_gone` (8 appels) ;
  - `no_permission` (2) ;
  - `no_blank_holder` (1) ;
  - `holders_partial` (1).
  Les deux écrans affichent ces messages en vert.
- Preuve :
  - `grep -rhoE 'tell\("gui\.packagedautoterminals\.[a-z_]+"' src/main/java | sort | uniq -c` → `8 …machine_gone`, `2 …no_permission`, `1 …no_blank_holder`, `1 …holders_partial`. `Feedback.java:45-66` ne contient aucune de ces clés.
  - `en_us.lang:61` : `no_permission=You are not allowed to build on this network`. `:62` : `machine_gone=That machine is no longer on the network`. `:26` : `no_blank_holder=No blank Package Recipe Holder on the network`. `:63` : `holders_partial=Sent %d back, the network refused %d`.
  - `GuiPatEditor.java:1050-1051` : `messageRefused ? COLOR_WARNING : COLOR_OK`. `:61` : `COLOR_OK = 0x2E7D32`.
  - `docs/TESTING.md:699` (N4) attend « `You are not allowed to build on this network`, in red. » Ce test échouerait.
  - `CHANGELOG.md:280` (beta.5) : « The terminal drew three of its own refusals in the colour of a success. » Le même défaut revient.
- Pourquoi c'est un problème : le joueur lit un refus dans la couleur d'un succès. La règle repose sur la mémoire du développeur. Elle a déjà échoué une fois, et D43/D44 ajoutent des clés à chaque lot.
- Correctif proposé :
  1. Ajouter les quatre clés tout de suite.
  2. Rendre la classification exhaustive avec un test JUnit. Ce test lit `src/main/java` et relève chaque clé passée à `tell(`. Chaque clé doit être dans `REFUSALS` ou dans une liste explicite de succès. Chaque clé doit aussi exister dans `en_us.lang`.
  Une autre voie est plus simple à long terme : écrire deux méthodes, `tell` et `refuse`, dans les containers. L'appel porte alors lui-même la couleur.
- Porte : double sens
- Coût : S

### C-VE-06 — Aucun démarrage de serveur dédié, ni en CI ni dans l'atelier local
- Sévérité : majeur
- Statut : VÉRIFIÉ (absence de démarrage) ; HYPOTHÈSE (fuite de classe client)
- Où : `.github/workflows/build.yml:27` ; dossier `run/` ; `PackagedAutoTerminals.java:70-83` ; `common/PatItems.java:10`, `:23`, `:52-76`.
- Constat : la CI lance seulement `./gradlew build`. Dans l'atelier local, `runServer` n'a jamais tourné. La séparation client/serveur repose sur un raisonnement écrit dans des commentaires. Aucune exécution ne la prouve.
- Preuve :
  - `ls run/eula.txt run/server.properties` → « No such file or directory » pour les deux. Un serveur dédié crée ces fichiers au premier démarrage.
  - `zgrep -a -h "Starting minecraft server version\|Starting integrated minecraft server" run/logs/*` → 31 lignes `IntegratedServer`, 0 ligne de serveur dédié.
  - `PackagedAutoTerminals.java:73-75` : « The side check in {@code init} is enough to avoid that, because Java only resolves a class when the code that uses it actually runs. »
  - `PatItems.java:23` : `@Mod.EventBusSubscriber(modid = Reference.MOD_ID)`, sans côté. La classe importe `net.minecraft.client.renderer.block.model.ModelResourceLocation` (`:10`) et porte des méthodes `@SideOnly(Side.CLIENT)` (`:53`, `:72`). Le serveur démarre seulement si FML retire bien ces méthodes.
  - 20 fichiers de `src/main/java` touchent `net.minecraft.client`, `@SideOnly` ou JEI.
  - Précédent : `docs/DECISIONS.md:364-374` (D37). Un défaut propre au serveur dédié est déjà parti chez le joueur.
- Pourquoi c'est un problème : si le serveur atteint une classe client, il s'arrête au démarrage avec `NoClassDefFoundError: net/minecraft/client/...`. Cet arrêt touche chaque propriétaire de serveur. Aujourd'hui, rien ne prouve que le jar démarre sur un serveur dédié.
- Attention : ce test n'aurait pas attrapé D37, qui touche le décodage d'un paquet client (voir C-VE-09).
- Correctif proposé : ajouter une étape CI. Lance-la une première fois pour confirmer que `runServer` tourne sans écran sur ForgeGradle 2.3 (HYPOTHÈSE).
  1. Écris `echo eula=true > run/eula.txt`. Sans ce fichier, le serveur refuse de démarrer (`.tools/forge-src/net/minecraft/server/dedicated/DedicatedServer.java:134-136`).
  2. Lance `timeout 300 ./gradlew runServer --no-daemon || true`.
  3. Exige la ligne `Done (` dans `run/logs/latest.log` (`DedicatedServer.java:276`). Fais échouer l'étape sur toute trace d'exception qui cite `packagedautoterminals`.
  Bonus : la CI n'a que `libs/maven` (AE2UEL, PackagedAuto, HEI). Ce test prouve donc aussi que le mod démarre sans AE2WUT, sans MixinBooter et sans les addons (règle 4 de `CLAUDE.md`).
- Porte : double sens
- Coût : S

### C-VE-07 — Les règles d'affichage du terminal vivent dans l'écran : on ne peut pas les tester, et elles divergent déjà
- Sévérité : majeur
- Statut : VÉRIFIÉ (aucun test possible) ; PROBABLE (divergence visible)
- Où : `client/gui/GuiPatTerminal.java:805-812` (`needsHolder`), `:1050-1101` (`groupState`, `hasInactive`, `unevenCount`, `groupTroubled`), `:1516-1662` (`buildMachineLines`, `buildPatternLines`, `hasProblem`), `:1671-1700` (`sortGroups`), `:1711-1785` (`matches`, `hasMod`, `hasText`, `matchesText`).
- Constat : les règles qui décident de ce que voit le joueur sont dans une sous-classe d'écran de 1 856 lignes, mêlées à `I18n.format`. Ces règles couvrent :
  - la recherche sur les sorties (D39) ;
  - le pliage ;
  - le filtre « problèmes seulement » ;
  - le diagnostic des recettes orphelines ;
  - le tri.
  Aucun test ne les atteint. Trois fonctions répondent à la même question, « ce groupe a-t-il un problème ? », avec trois règles différentes.
- Preuve :
  - `hasProblem` (`:1647-1662`) teste trois cas : un rôle manquant, une machine inactive, une recette incomplète. `groupTroubled` (`:1097-1101`) ajoute `needsHolder(group)`. `groupState` (`:1062-1064`) affiche `no_holder` en rouge.
  - Cas concret :
    1. Le joueur nomme « ingots » une paire Packager + Unpackager qui porte la recette R.
    2. Il donne le même nom à un second Unpackager, posé sans holder.
    3. `mergeNamed` (`ProviderPairing.java:163-181`) réunit les trois machines.
    4. R est complète (`ProviderPairing.java:54-65`), et aucun rôle ne manque (`:306-323`).
    5. La puce affiche `no holder` en rouge, mais `hasProblem` répond faux. Le filtre « problèmes seulement » (`GuiPatTerminal.java:1609`) cache donc une ligne rouge.
  - Le diagnostic des orphelines est « la valeur principale » de l'onglet Machines (`docs/ARCHITECTURE.md:26-28`). Il vit dans `buildMachineLines`.
  - Les tests de recherche B4, B5 et B7 portent 🆕 (`docs/TESTING.md:97`, `:101`, `:106`).
- Pourquoi c'est un problème : une régression de la recherche ou du diagnostic ne se voit qu'en jeu. La divergence ci-dessus vient de trois copies d'une même règle, et d'autres copies divergeront de la même façon.
- Correctif proposé :
  1. Déplacer dans `common` une fonction pure qui rend l'état d'un groupe : rôle manquant, inactif, vide, sans holder, déséquilibré ou sain. La puce, la couleur et le filtre l'utilisent tous les trois.
  2. Y ajouter un filtre de recettes. Il prend un `SearchQuery` et trois fonctions : nom affiché, mod, type.
  3. Les tester comme `ProviderPairingTest`, avec `FakeRecipe`.
- Porte : double sens
- Coût : M

### C-VE-08 — Le protocole manuel n'a pas de parcours court : 8 groupes sur 16 jamais joués
- Sévérité : majeur
- Statut : VÉRIFIÉ
- Où : `docs/TESTING.md:14-29`, `:755`, `:823` ; `tools/release.sh:28-80`.
- Constat : `docs/TESTING.md` compte 890 lignes et 149 titres de test. Aucune publication n'exige d'en jouer un seul. Les groupes I, J, K, L, M et N sont « never tested ». O et P sont « not tested in game », et ils manquent au tableau. Certaines de ces fonctions sont publiques depuis beta.1 : AE2WUT figure dans `CHANGELOG.md:406`, et personne n'a joué le groupe K.
- Preuve : le tableau `docs/TESTING.md:24-29`, plus les en-têtes `:755` et `:823`. Seul le jeu prouve chaque comportement du tableau ci-dessous :

  | Comportement | Pourquoi seulement en jeu | QA la plus courte |
  |---|---|---|
  | AE2 republie les patterns après une écriture | état de la grille AE2 | B1 puis D3 : un terminal ME simple montre le pattern changé (1 min) |
  | Montants au-delà de 64 affichés côté client | ces montants passent par le coremod AE2UEL (`.tools/ae2-src/appeng/core/transformer/AE2ELTransformer.java:66-67`, `PacketBufferPatch.java:46-49`) ; D29 (`docs/DECISIONS.md:66`) cite seulement le NBT | D15 : régler 193, rouvrir, lire 193 ; refaire sur le fork AE2UEL du joueur (1 min) |
  | Mixins AE2WUT par nom | cibles absentes du chemin de compilation (`build.gradle:41`, `-proc:none`) | K2 puis K6 (3 min) |
  | Mode holder, avec de vrais objets | inventaire du joueur | O3 puis O6 (4 min) |
  | Mise à l'échelle | `generateFromStacks` et écriture machine | P1 puis P3 (3 min) |
  | Client réel connecté à un serveur dédié | sérialisation réseau (même classe de défaut que D37) | `runServer` + `runClient`, ouvrir le terminal, Save (3 min) |

- Pourquoi c'est un problème : le protocole complet est trop long pour être rejoué. La moitié n'a donc jamais tourné. Les publications continuent quand même, sans parcours minimal.
- Correctif proposé :
  1. Placer en tête de `docs/TESTING.md` une section « Smoke, 15 minutes » avec les six lignes ci-dessus.
  2. À la fin du parcours, écrire une ligne `Smoke passed: <version>`.
  3. Faire vérifier cette ligne par `tools/release.sh`, comme il vérifie déjà le CHANGELOG.
- Porte : double sens
- Coût : S

### C-VE-09 — Le protocole réseau n'a aucun test, alors que ses règles sont pures
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `Reference.java:17` ; `network/PacketRenameGroup.java:52-59` ; `network/PacketEditorFill.java:50-62` ; `network/PacketRecipeAction.java:68-82` ; `network/PacketEditorSlot.java:42-56`.
- Constat : la règle D37 limite le nom de canal à 20 caractères. Seuls un commentaire et `CLAUDE.md` §4.7 la gardent. Les lectures bornées de `PacketRenameGroup` et de `PacketEditorFill` protègent le serveur contre un paquet forgé, mais aucun test ne les exerce. Ces méthodes ont besoin seulement de `Unpooled.buffer()` et de classes déjà présentes sur le chemin de test.
- Preuve :
  - `.tools/forge-src/net/minecraft/network/play/client/CPacketCustomPayload.java:37` : `this.channel = buf.readString(20);`
  - `Reference.java:17` : `public static final String CHANNEL = "pat_terminals";`
  - `src/test` ne contient aucune classe du paquet `network`. `GroupNamesTest` utilise déjà `NBTTagCompound` et `BlockPos` : le chemin de test charge donc ces classes.
  - `docs/DECISIONS.md:373-374` : le solo ne voit pas ce défaut, seul le serveur dédié le montre.
- Pourquoi c'est un problème : un retour à `MOD_ID` (21 caractères) recasserait tous les serveurs dédiés, et aucun test ne rougirait. Une régression des bornes rouvrirait l'attaque par paquet forgé que le code ferme aujourd'hui.
- Correctif proposé : écrire quatre tests.
  1. `Reference.CHANNEL.length() <= 20`.
  2. Aller-retour de `PacketRecipeAction` et de `PacketEditorSlot`.
  3. `PacketRenameGroup` annonce un million d'octets mais n'en contient que dix. Le nom lu fait 32 caractères au plus, et l'index de lecture reste cohérent.
  4. `PacketEditorFill` annonce `Integer.MAX_VALUE` entrées sur un tampon vide. La boucle sort tout de suite, sans créer d'`ItemStack`.
- Porte : double sens
- Coût : S

### C-VE-10 — Des règles pures restent enfermées dans des classes Minecraft
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `container/ContainerPatEditor.java:518-562`, `:593-681` ; `container/ContainerPatTerminal.java:453-503` ; `client/gui/GuiScaleFit.java:93-128` ; `integration/jei/PatTransferHandler.java:77-92`.
- Constat :
  - La règle de proportion par case (`setSlotCount(…, true)`) refait en ligne la division exacte et la borne. Pour D44, `RecipeScale.scale` porte déjà les deux. Comparer `ContainerPatEditor.java:536` (`if (scaled % previous != 0) {`) et `:541` (`if (scaled / previous > max) {`) avec `RecipeScale.java:27` et `:30`. Une seule règle a donc deux implémentations.
  - Le choix du message après une écriture existe deux fois, avec des branches différentes. `save()` traite `result.supplied` et `applied_no_partner` (`ContainerPatEditor.java:663-667`). `repairGroup()` ne les traite pas (`ContainerPatTerminal.java:490-501`).
  - `GuiScaleFit.scaleFor` reçoit `Minecraft` seulement pour lire deux entiers. Son commentaire dit « Copied from {@code ScaledResolution} » (`:111`). Mais la copie omet la correction Unicode de `.tools/forge-src/net/minecraft/client/gui/ScaledResolution.java:35-38`. Cet écart n'a aucun effet aujourd'hui : la copie surestime le facteur, donc elle se trompe seulement vers une échelle plus petite. Mais aucun test n'attraperait un écart nuisible.
  - `PatTransferHandler.findType` (D32, « le type le plus étroit ») fait un choix pur sur une table.
- Pourquoi c'est un problème : seul le jeu vérifie chacune de ces règles. Les tests D19, D20, D21 et D48 portent 🆕 (`docs/TESTING.md:217`, `:225`, `:229`, `:298`).
- Correctif proposé : déplacer chaque règle dans `common`, avec un test pour chacune. Par exemple :
  - une méthode de proportion sur des tableaux d'entiers dans `RecipeScale` ;
  - une fonction qui choisit le message à partir d'un `RecipeWriter.Result` ;
  - `scaleFor` sur (largeur, hauteur, unicode, échelle du joueur, taille de l'écran) ;
  - `findType` sur une `Map`.
- Porte : double sens
- Coût : S

### C-VE-11 — Le compilateur ne montre rien, et aucun outil d'analyse ne tourne
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `build.gradle:26-42`.
- Constat : `compilerArgs` porte seulement `-proc:none`. Le build n'a ni `-Xlint` ni `-Werror`, ni Checkstyle, ni PMD, ni formateur. Par défaut, javac affiche une note et aucun avertissement. Avec `-Xlint:all`, il affiche un seul avertissement.
- Preuve :
  - `build.gradle:41` : `options.compilerArgs << '-proc:none'`, seule option.
  - `docs/audit/CONTEXTE.md:15-16` : « javac par défaut | 0 avertissement, 1 note de dépréciation ». Et : « javac `-Xlint:all` … | 1 avertissement : `[deprecation] I18n` dans `integration/wut/WutSupport.java:13` ».
  - `WutSupport.java:13` est l'`import` de `net.minecraft.util.text.translation.I18n`. Le `@SuppressWarnings("deprecation")` de `:89` ne couvre pas un import, et javac 8 signale les imports dépréciés.
  - Gradle 4.10.3 embarque les plugins `pmd` et `checkstyle` : `unzip -l gradle-code-quality-4.10.3.jar` liste `META-INF/gradle-plugins/org.gradle.pmd.properties` et `org.gradle.checkstyle.properties`.
- Pourquoi c'est un problème : le code est propre aujourd'hui, donc il coûte presque rien de verrouiller cet état. Sans `-Werror`, le prochain avertissement passe sans bruit. Par exemple : un cast non vérifié, ou un `switch` qui tombe dans le cas suivant.
- Correctif proposé :
  1. Écrire le nom qualifié complet de `I18n` dans `modeName()` et retirer l'import.
  2. Ajouter `-Xlint:all` et `-Werror` aux `compilerArgs`.
  3. Ajouter PMD ou Checkstyle plus tard, si le besoin se confirme.
- Porte : double sens pour `-Xlint`/`-Werror`. Sens unique pour PMD ou Checkstyle, car c'est une nouvelle dépendance de build (§5 de la référence).
- Coût : S

### C-VE-12 — Les documents de test se contredisent sur ce qui est prouvé
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `docs/TESTING.md:14-29`, `:40`, `:43`, `:91`, `:137`, `:264`, `:313`, `:319`, `:359`, `:381`, `:654`, `:827`, `:864` ; `docs/PLAN.md:84`, `:92`, `:129-131` ; `docs/DECISIONS.md:697`, `:755`, `:770`.
- Constat : les documents ne disent pas ce qui est prouvé, ni quand.
- Preuve :
  - `docs/PLAN.md:131` : « AE2WUT and the opening key written and tested ». Mais `docs/TESTING.md:26-27` marque K et L « 🆕 never tested ».
  - `docs/PLAN.md:84` et `:92` marquent les lots 4 et 5 « not tested in game ». Mais `docs/PLAN.md:129` dit « 0, 1, 2, 3, 4, 5 | tested in game ».
  - `docs/TESTING.md:19-22` marque D, E, F et G « passed ». Mais leurs en-têtes portent 🆕 (`:137`, `:319`, `:359`, `:381`).
  - Le tableau de progression (`:14-29`) ne cite ni O ni P.
  - Collisions d'identifiants :
    - le test « D43 — The tab arrows » (`:264`) porte le numéro de la décision D43, le mode holder (`docs/DECISIONS.md:697`) ;
    - le test « D44 — The panel and the JEI tooltip » (`:313`) porte le numéro de la décision D44, la mise à l'échelle (`:755`) ;
    - « P1 — Launch » (`:40`) et « P1 — Multiply a group » (`:827`) partagent un numéro.
  - Attentes périmées :
    - P6 attend « the `unsaved` message » (`:864`, et `docs/DECISIONS.md:770`). Le code envoie `scale_unsaved` (`ContainerPatEditor.java:1186`).
    - B2 attend la taille du paquet au survol (`:91`). Voir C-VE-03.
    - La commande de lancement cite `/c/Users/Julien/Desktop/PackagedAutoTerminals` (`:43`). Le dépôt est sous `H:\Dev`.
    - H2 cite `packagedautoterminals-1.12.2-0.1.0.jar` (`:654`).
- Pourquoi c'est un problème : un testeur qui suit P6 marque en échec un comportement correct. Un rapport de bogue qui cite « D43 » est ambigu.
- Correctif proposé :
  1. Garder une seule source de vérité : le tableau de `docs/TESTING.md`, avec une ligne par groupe, une date et une version.
  2. Faire pointer `docs/PLAN.md` vers ce tableau.
  3. Renuméroter les tests du groupe D au-delà de D39, et le P1 de préparation.
  4. Corriger les quatre attentes périmées.
- Porte : double sens
- Coût : S

### C-VE-13 — Aucun test ne garde les tables écrites à la main par nom de classe
- Sévérité : mineur
- Statut : VÉRIFIÉ
- Où : `common/CrafterTypes.java:49-68`, `:83-86` ; `common/ProviderRole.java:37-43` ; `common/ProviderPairing.java:58`, `:313` ; `mixin/ae2wut/MixinWutTerminal.java:26`, `:35`, `:54` ; `mixin/ae2wut/MixinWutRecipe.java:27`, `:30`.
- Constat : le code écrit 13 noms de classe en chaîne : 8 crafters, 2 routeurs, 3 rôles. S'y ajoutent 2 cibles de mixin et 3 méthodes. Une faute de frappe ne casse pas le build. Pour un rôle, la faute est silencieuse et dangereuse. Une machine au rôle `UNKNOWN` rend toute recette « complète », donc une paire déséquilibrée paraît saine.
- Preuve :
  - `ProviderPairing.java:58` : `if (machine.role == ProviderRole.COMPLETE || machine.role == ProviderRole.UNKNOWN) {` puis `return true;`. `ProviderPairingTest` n'a aucun cas `UNKNOWN`.
  - Pour cet audit, j'ai vérifié que tous les noms existent aujourd'hui :
    - `.tools/pa-src/thelm/packagedauto/tile/` contient `TilePackager`, `TileUnpackager`, `TileCrafter`, `TileDistributor` et `TileCraftingProxy` ;
    - `unzip -l` trouve les six `Tile*Crafter` dans `PackagedExCrafting-1.12.2-1.0.3.33.jar` ;
    - `unzip -l` trouve `TileExtremeCrafter` dans `PackagedAvaritia-1.12.2-1.0.3.25.jar` ;
    - `unzip -l` trouve `TilePackagingProvider` dans `PackagingProvider-1.12.2-1.0.0.2.jar`.
  - `javap -p` sur `run/mods/ae2wut-1.0.5.jar` → `private static int[] getAllMode();`, `public static java.lang.String getWirelessName(int);`, `private static java.util.Map<java.lang.Integer, net.minecraft.item.ItemStack> getIngredient();`.
- Pourquoi c'est un problème : une mise à jour d'addon, ou une faute dans la table, éteint le diagnostic ou l'appariement sans aucun message. Le défaut se voit seulement en jeu, et seulement sur le bon type de machine.
- Correctif proposé :
  1. Écrire un test JUnit qui charge les cinq noms de PackagedAuto avec `Class.forName(nom, false, loader)`. Ces classes sont déjà sur le chemin de test.
  2. Ajouter un cas `UNKNOWN` dans `ProviderPairingTest`.
  3. Les addons et AE2WUT sont absents de la CI. Pour eux, écrire une tâche Gradle locale qui lance `javap` sur `run/mods` quand le dossier existe.
- Porte : double sens
- Coût : S

---

## Ce qui est bien fait

- Chaque test porte le nom d'un comportement et raconte le défaut qu'il verrouille. Exemples : `FeedbackTest.java:13-15`, et `GroupNamesTest.java:102-118`, qui prouve la lecture de l'ancien format NBT. Garde ce modèle pour les nouveaux tests.
- `checkLang` et `checkDevMods` changent deux pièges déjà subis en échec de build (`build.gradle:190-247`, `:253-283`). `runClient` dépend de `reobfJar` et de `checkDevMods` (`:171-172`).
- La CI garde le rapport de test en cas d'échec (`build.yml:30-31`), et `testLogging` nomme chaque test (`build.gradle:115-118`).
- Le projet relit dans le bytecode les affirmations sur l'API et les note. Exemple : le constructeur à quatre arguments d'`AEBaseContainer` accepte bien trois `null` (`.tools/ae2-src/appeng/container/AEBaseContainer.java:105-112` contre `:139-146`), comme le dit `ContainerPatEditor.java:134-136`.
- Les mixins AE2WUT portent `require = 1`. Si une cible disparaît, le démarrage s'arrête au lieu de casser en silence (`MixinWutTerminal.java:23-24`, `:35`).
