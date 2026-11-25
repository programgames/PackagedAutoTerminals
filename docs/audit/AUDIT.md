# Audit — PackagedAuto Terminals 0.1.0-beta.10

Audit du 2026-11-25, branche `audit/2026-11-25`, partie du commit `85dadf6`. Quatre auditeurs ont écrit 62 constats, et deux contre-experts les ont revérifiés. Après la contre-expertise, 2 constats sont réfutés et 8 sont des doublons fusionnés. Il reste **52 constats** : 0 bloquant, 10 majeurs, 37 mineurs et 5 cosmétiques.

Sources : `findings-*.md` (constats bruts), `challenge.md` et `challenge-2.md` (verdicts), `CONTEXTE.md` (versions et commandes).

## 1. Verdict

1. Le mod tient sa promesse principale. Le serveur reste l'autorité : chaque écriture refait le scan, teste `BUILD` et nomme la recette dans l'éditeur.
2. Les gestes qui détruisent sont les moins protégés. Un clic, avec n'importe quel bouton, renvoie au réseau les porte-recettes de tout un groupe, et l'infobulle de ce bouton ment.
3. Un clic droit sur la croix supprime une recette dans toutes les machines, sans confirmation. Le terminal désigne la recette par sa position, pas par son identité.
4. L'éditeur perd le travail non enregistré sans avertir. AE2 traite les clics sur les cases fantômes dans `doAction`, que l'éditeur ne redéfinit pas. Les sources AE2UEL 0.56.5 et le fork du joueur le prouvent.
5. L'instantané du réseau n'a pas de plafond. Au-delà de 2 Mo NBT, le client est déconnecté. La mesure R2, promise depuis le lot 2, n'existe toujours pas.
6. La preuve est mince. 7,5 % du code a des tests. La CI n'a jamais lancé un test, car 11 commits ne sont pas poussés. Aucun serveur dédié n'a démarré.
7. Les deux fonctions les plus récentes (D43, D44) déplacent de vrais objets et réécrivent des groupes entiers. Elles n'ont ni test automatique ni test en jeu.
8. `GuiPatTerminal` (1 856 lignes) porte des règles du domaine qui divergent déjà. L'éditeur rescanne toute la grille à chaque tick.
9. La documentation dérive du code : `ARCHITECTURE.md`, le wiki et `TESTING.md` affirment des choses fausses.
10. Priorité : protéger les gestes destructeurs et corriger `doAction` avant toute nouvelle fonction. Mesurez ensuite R2. Sortez enfin les règles de l'écran.

## 2. Notes par lentille

| Lentille | Note | Justification |
|---|---|---|
| correction-minecraft | C | Le serveur valide bien, mais un chemin AE2 vérifié contourne `slotClick`, la suppression vise un index, et l'instantané n'a pas de plafond. |
| architecture | C | Les écritures passent par un seul chemin et les décisions sont documentées, mais un écran de 1 856 lignes porte des règles du domaine sans test. |
| vérifiabilité | D | 7,5 % du code sous test, une CI qui n'a jamais lancé un test, aucun serveur dédié, et les fonctions les plus récentes jamais jouées. |
| produit | C | La promesse est tenue en 16 langues, mais trois gestes détruisent du travail en un clic, avec des infobulles fausses. |

## 3. Les 5 constats les plus importants

1. **C-MC-02** — Les clics sur les cases fantômes passent par `AEBaseContainer.doAction`, jamais par `ContainerPatEditor.slotClick`. Le travail non enregistré disparaît sans avertissement, le clic droit ne vide pas la case, et le serveur accepte un objet dans une case désactivée. VÉRIFIÉ, coût S.
2. **C-MC-03 + C-PR-02** — La suppression depuis le terminal vise un index calculé sur un instantané ancien. Elle accepte le clic droit, que le README enseigne pour « éditer », et elle ne demande aucune confirmation. Elle peut effacer la mauvaise recette dans toutes les machines du groupe. Coût S.
3. **C-PR-01** — Un clic simple sur le bouton de ligne renvoie au réseau le porte-recettes de chaque machine du groupe. L'infobulle dit « Shift + left click ». Le terminal ne sait pas remettre un porte-recettes qui porte des recettes. Coût S, avec une décision de produit (question 1).
4. **C-MC-01** — L'instantané du terminal part dans un seul tag NBT sans plafond. Au-delà de 2 097 152 octets comptés, FML coupe la connexion. Le seuil réel n'est pas mesuré. Coût M.
5. **C-VE-01 + C-VE-08** — Le mode holder et la mise à l'échelle n'ont aucune preuve. Le protocole manuel n'a pas de parcours court, et `release.sh` ne demande aucun test en jeu. Coût M.

## 4. Constats retenus

Statut final après contre-expertise. « + » indique un doublon fusionné.

| Id | Titre | Sévérité | Statut | Porte | Coût |
|---|---|---|---|---|---|
| C-MC-01 + C-VE-03 | Instantané sans plafond, déconnexion au-delà de 2 Mo NBT, R2 jamais mesuré | majeur | VÉRIFIÉ (déconnexion) ; PROBABLE (seuil) | double sens | M |
| C-MC-02 | Clics sur les cases fantômes hors de `slotClick` : travail perdu, clic droit sans effet, case désactivée acceptée | majeur | VÉRIFIÉ | double sens | S |
| C-MC-03 + C-AR-01 | Suppression et édition par index depuis le terminal | majeur | PROBABLE | double sens | S |
| C-MC-05 + C-AR-02 | L'éditeur rescanne et regroupe toute la grille à chaque tick | majeur | PROBABLE (coût non mesuré) | double sens | S |
| C-VE-01 | Mode holder et mise à l'échelle sans test ni partie jouée | majeur | VÉRIFIÉ | double sens | M |
| C-VE-08 | Protocole manuel sans parcours court ; 6 à 8 groupes jamais joués | majeur | VÉRIFIÉ | double sens | S |
| C-AR-03 + C-VE-07 | Règles du domaine dans l'écran, sans test ; `groupTroubled` et `hasProblem` divergent | majeur | VÉRIFIÉ | double sens | M |
| C-PR-01 | Un clic éjecte les porte-recettes de tout le groupe ; infobulle fausse | majeur | VÉRIFIÉ | double sens | S |
| C-PR-02 | Suppression en un clic, clic droit compris, sans confirmation | majeur | VÉRIFIÉ | double sens | S |
| C-PR-03 | Paire séparée : le bouton « réparer » répond « Nothing was written » | majeur | VÉRIFIÉ | double sens | S |
| C-MC-04 | Un nom de groupe survit à la machine cassée | mineur | PROBABLE | double sens (purge) | M |
| C-MC-06 | Terminal sans fil : case non verrouillée, objet non relu | mineur | PROBABLE | double sens | S |
| C-MC-07 | L'éditeur ignore `disabled_recipe_types` et fait confiance au type envoyé par le client | mineur | VÉRIFIÉ | double sens | S |
| C-MC-08 | Le terminal s'ouvre sans `BUILD`, contre D12 et contre AE2 | mineur | VÉRIFIÉ | double sens | S |
| C-MC-09 | L'écran regroupe les machines à chaque image | mineur | VÉRIFIÉ | double sens | S |
| C-MC-10 + C-AR-09 + C-PR-05 | `machinesTab` non synchronisé : fausses alertes rouges | mineur | VÉRIFIÉ | double sens | S |
| C-MC-11 | `update.json` ne peut jamais annoncer de mise à jour | mineur | VÉRIFIÉ | double sens | S |
| C-MC-12 | En solo, le fil client lit la grille du serveur | mineur | PROBABLE | double sens | S |
| C-MC-13 | 99 recherches de recette côté client à chaque changement d'onglet | mineur | PROBABLE | double sens | S |
| C-MC-14 | Champ interne `TileEncoder.patternSlots` lu sans borne de version | mineur | VÉRIFIÉ | double sens | S |
| C-MC-15 | `giveBack` jette le reste d'une insertion refusée | mineur | HYPOTHÈSE (latent) | double sens | S |
| C-VE-02 | La CI n'a jamais lancé un test : 11 commits non poussés | mineur | VÉRIFIÉ | **sens unique** (push) | S |
| C-VE-04 | Écrire pendant un craft : question N9 sans réponse | mineur | PROBABLE | double sens | S (QA) |
| C-VE-05 + C-AR-04 | Quatre refus s'affichent en vert ; liste `REFUSALS` tenue à la main | mineur | VÉRIFIÉ | double sens | S |
| C-VE-06 | Aucun démarrage de serveur dédié | mineur | VÉRIFIÉ | double sens | S |
| C-VE-09 | Protocole réseau sans test (canal ≤ 20, lectures bornées) | mineur | VÉRIFIÉ | double sens | S |
| C-VE-10 | Règles pures enfermées dans des classes Minecraft | mineur | VÉRIFIÉ | double sens | S |
| C-VE-11 | Ni `-Xlint` ni `-Werror` | mineur | VÉRIFIÉ | double sens | S |
| C-VE-12 | Les documents de test se contredisent | mineur | VÉRIFIÉ | double sens | S |
| C-VE-13 | Aucun test ne garde les tables de noms de classe | mineur | VÉRIFIÉ | double sens | S |
| C-AR-05 | Mode holder = drapeau `null` dans une classe de 1 411 lignes | mineur | VÉRIFIÉ | double sens | M |
| C-AR-06 | Cycles de paquets ; budget de portage faux dans `PORTING.md` | mineur | VÉRIFIÉ | double sens | M |
| C-AR-07 | Le client recalcule la recette ; Javadoc fausse | mineur | PROBABLE | double sens | S |
| C-AR-08 | Quatre portes d'ouverture sans fil ; PITFALL `parseLong` faux | mineur | VÉRIFIÉ | double sens | S |
| C-AR-10 | Aides recopiées (recherche linéaire, parcours de grille, feedback) | mineur | VÉRIFIÉ | double sens | M |
| C-AR-11 | Un paquet pour quinze actions, `index` à trois sens | mineur | VÉRIFIÉ | double sens | M |
| C-AR-12 + C-PR-08 | Code mort ; `tooltipStacks` sans effet | mineur | VÉRIFIÉ | double sens (code) ; **sens unique** (clé de config) | S |
| C-AR-13 | Documentation et Javadoc contredisent le code | mineur | VÉRIFIÉ | double sens | S |
| C-AR-14 | « machine » et « pattern » nomment chacun deux concepts | mineur | VÉRIFIÉ | double sens | S |
| C-AR-15 | Géométrie des slots recalculée à la main ; mise en page dans le conteneur | mineur | VÉRIFIÉ | double sens | S |
| C-PR-04 | Terminal de 367 px : échelle 2 forcée en 1080p | mineur | VÉRIFIÉ | double sens | S |
| C-PR-06 | Aide en jeu muette sur les gestes récents | mineur | VÉRIFIÉ | double sens | S |
| C-PR-07 | Le wiki contredit le comportement | mineur | VÉRIFIÉ | double sens | S |
| C-PR-09 | Diagnostic « aucun crafter » faux pour un crafter hors réseau ME | mineur | PROBABLE | double sens | S |
| C-PR-10 | Refus « mode holder désactivé » invisible sous l'écran | mineur | PROBABLE | double sens | S |
| C-PR-11 | Infobulle du terminal sans fil : jamais « lié » ou « non lié » | mineur | VÉRIFIÉ | double sens | S |
| C-PR-12 | `wutModeId` modifiable en jeu, mais lu une seule fois | mineur | VÉRIFIÉ | double sens | S |
| C-MC-16 | La flèche « suivant » du type recule | cosmétique | VÉRIFIÉ | double sens | S |
| C-MC-17 | `addCheckedInformation` sans `@SideOnly(CLIENT)` | cosmétique | PROBABLE | double sens | S |
| C-MC-18 | `ClientDiagnostics` ne ferme pas sept ressources | cosmétique | VÉRIFIÉ | double sens | S |
| C-AR-18 | Les commentaires racontent l'histoire des bugs (33 PITFALL, 21 FIXED) | cosmétique | VÉRIFIÉ | double sens | M |
| C-PR-13 | Libellés non traduits, pluriel faux, CHANGELOG faux (362 px) | cosmétique | VÉRIFIÉ | double sens | S |

Note sur les portes : FML exige la même version du mod des deux côtés, car `@Mod` ne déclare pas `acceptableRemoteVersions` (`FMLModContainer.java:608`, `NetworkModHolder.acceptVersion`). Un changement du format des paquets est donc une porte à double sens. Le format NBT de `GroupNames` (sauvegarde) reste une porte à sens unique.

## 5. À supprimer ou à réécrire

À supprimer :
- `PatConfig.tooltipStacks` et ses 16 traductions : aucun code ne lit cette option (question 5).
- `ItemWirelessPatTerminal.wutMode()`, `ProviderSnapshot.id`, les trois modids inutilisés de `Reference`, `CrafterTypes.all()` : code mort.
- La branche `SlotFake` de `ContainerPatEditor.slotClick`, une fois `doAction` redéfini : elle ne s'exécute jamais.
- Le PITFALL `parseLong` de `PatGuiHandler.java:163-165` : AE2UEL 0.56.5 attrape l'exception.
- Les Javadoc orphelines ou en double listées dans C-AR-13.

À réécrire :
- `docs/ARCHITECTURE.md`, d'après les paquets réels.
- La couleur des messages : les conteneurs doivent appeler `tell` ou `refuse`, et la liste `Feedback.REFUSALS` doit disparaître.
- L'état d'un groupe : une seule fonction pure dans `common`, utilisée par la puce, la couleur et le filtre.
- Le récit « FIXED » dans les commentaires : déplacez-le vers le CHANGELOG, fichier par fichier, à chaque modification.

## 6. Features proposées (valeur / coût)

| Rang | Feature | Pourquoi | Coût |
|---|---|---|---|
| 1 | Retirer l'éjection du clic simple sur le bouton de ligne (C-PR-01) | Un clic de travers arrête une paire, et le joueur doit retourner à pied à chaque machine. | S |
| 2 | Confirmation en deux clics pour toute suppression, grille gardée après Delete (C-PR-02) | Un clic efface une recette qui a demandé plusieurs minutes de saisie. | S |
| 3 | Aide en jeu : page JEI « gestes », infobulles de recherche `@mod #type` et `×N`, statut « lié » du terminal sans fil (C-PR-06, C-PR-11) | Les fonctions existent, mais le joueur ne les découvre pas. | S |
| 4 | Hauteur du terminal adaptée à 1080p (C-PR-04) | À chaque ouverture, l'interface rétrécit et le texte devient petit. | S (10 lignes) / M (dynamique) |
| 5 | Geste « lier deux lignes » (C-PR-03, F3) | La seule réparation d'une paire séparée est aujourd'hui de taper deux fois le même nom, et seul le wiki le dit. | M |

## 7. Hypothèses restantes

| Hypothèse | Comment trancher |
|---|---|
| Seuil réel de C-MC-01 | Test JUnit : construire le tag `Providers` pour N paires pleines, l'écrire avec `CompressedStreamTools.write`, le relire avec `NBTSizeTracker(2097152L)`, chercher N limite. |
| C-MC-02 en jeu | Posez un objet au clic dans une case fantôme, puis cliquez un autre onglet. L'avertissement « non enregistré » ne doit pas apparaître si le constat est juste. |
| C-AR-07 : Save désactivé pour une recette ajoutée seulement côté serveur | Serveur dédié avec un script de recette côté serveur seulement, puis essayer de sauver. |
| C-VE-04 : écriture pendant un craft | QA de 5 minutes : craft de 64 objets, ×2 pendant le craft, observer le CPU, annuler, compter les objets. |
| C-PR-09 : crafter hors réseau ME | Paire + Proxy + Package Crafter alimenté en RF, sans câble ME. Ouvrir l'onglet Machines. |
| C-MC-15 : reste jeté par `giveBack` | Latent avec le porte-recettes de PackagedAuto. Rien à tester tant qu'aucun addon ne fournit un autre porte-recettes. |
| `PacketEditorFill` au-delà de 32 767 octets | Test : `toBytes` avec 90 piles à gros NBT, puis lire `writerIndex`. |
| `runServer` sans écran sous ForgeGradle 2.3 en CI | Lancer une fois `eula=true` puis `timeout 300 ./gradlew runServer`, et chercher `Done (` dans `run/logs/latest.log`. |
| C-MC-10 : `wutModeId` différent entre client et serveur | Serveur dédié avec deux valeurs différentes, puis ouvrir le terminal universel. |
| C-PR-04 : l'éditeur (338 px) en fenêtre agrandie | Lire `mc.displayHeight` en jeu dans une fenêtre agrandie. |

## 8. Proposition de glossaire

`GLOSSARY.md` n'existe pas. Proposition, tirée des noms du code :

| Terme | Sens | Nom dans le code |
|---|---|---|
| Provider | Machine PackagedAuto qui porte un porte-recettes : Packager, Unpackager, Packaging Provider | `ProviderSnapshot`, `IPackageProvidingMachine` |
| Crafter | Machine qui exécute un paquet : Package Crafter, crafters des addons | `MachineSnapshot`, `IPackageCraftingMachine` |
| Rôle | Fonction d'un provider : PACKAGER, UNPACKAGER, COMPLETE, UNKNOWN | `ProviderRole` |
| Groupe | Ensemble de providers qui portent les mêmes recettes, ou qui partagent un nom | `ProviderPairing.Group` |
| Paire | Groupe normal : un Packager et un Unpackager | (pas de type) |
| Porte-recettes (holder) | Objet Package Recipe Holder, qui contient au plus 20 recettes | `ItemRecipeHolder` |
| Porte-recettes vierge | Porte-recettes sans NBT, que le terminal peut prendre au réseau | `blank` |
| Recette | Une entrée du porte-recettes | `IRecipeInfo` |
| Pattern (paquet) | Paquet tiré d'une recette, publié dans AE2 | `IPackagePattern` |
| Type de recette | Processing, Crafting, types des addons | `IRecipeType` |
| Recette orpheline | Recette qu'aucun crafter du réseau ne peut exécuter | diagnostic de l'onglet Machines |
| Mode holder | Édition de deux porte-recettes tenus en main, sans réseau ME (D43) | `holders != null` |
| Jumeau | Second porte-recettes du mode holder | `holder_sync`, slot jumeau |
| Mise à l'échelle | Multiplication ou division de toutes les recettes d'un groupe (D44) | `scaleAll`, `RecipeScale` |
| Instantané | Liste des providers et crafters envoyée au client | `PacketProviderList` |
| Refus | Message qui dit qu'une action n'a rien écrit | `Feedback.REFUSALS` |

Conflits de vocabulaire repérés :
- « machine » désigne les providers (`Group.machines`, `ProviderSnapshot.machine`) **et** les crafters (`MachineSnapshot`, `ContainerPatTerminal.machines`). Proposition : réserver « machine » au mot générique et dire « provider » ou « crafter » dans le code.
- « Pattern » nomme l'onglet de la liste des recettes, comme l'Interface Terminal d'AE2. Dans l'API, `IPackagePattern` nomme le paquet. Choix de produit défendable (C-AR-14) : documentez-le, ne renommez pas l'onglet.
- « Holder » reste en anglais dans 10 langues, alors que `fr_fr` dit « porte-recettes » partout ailleurs (C-PR-13).
- « Encoder » nomme l'icône du mode holder et aussi le bloc Package Recipe Encoder de PackagedAuto.

## Backlog (hors tickets)

- C-AR-05 : remplacer le drapeau `holders != null` par une stratégie `EditTarget` (M).
- C-AR-06 : rendre `common` sans cycle et refaire la carte T0–T4 de `PORTING.md` (M).
- C-AR-11 : séparer `PacketRecipeAction` par famille d'actions (M).
- C-AR-18 : déplacer le récit « FIXED » vers le CHANGELOG, à chaque modification de fichier.
- Feature 5 : geste « lier deux lignes » (M), selon la réponse à la question 3.
- Découpage de l'instantané en plusieurs messages, seulement si la mesure de C-MC-01 l'exige.
- Page JEI « gestes » (C-PR-06) : nouvelle feature, pas validée par Julien.
- Feature 4 version dynamique : nombre de lignes du terminal selon la fenêtre (Julien a choisi 10 lignes fixes).
- C-MC-15 : `giveBack` jette le reste. Hypothèse latente, sans moyen de vérifier tant qu'aucun addon ne fournit un autre porte-recettes.
- C-VE-04 : écriture pendant un craft. QA manuelle seulement, listée dans `RAPPORT.md`.
- C-AR-10 hors du choix de message : recherche linéaire en quatre copies, parcours de grille en trois copies, base commune des deux conteneurs.
- C-AR-14 : renommer `MachineSnapshot` en `CrafterSnapshot` et `Group.machines` en `providers`, après le glossaire (T28).
- C-VE-08 : porte de publication en jeu. Julien a choisi de ne pas en mettre (question 7).

## Annexe — constats réfutés

| Id | Titre | Raison du rejet |
|---|---|---|
| C-AR-16 | Table statique qui couple le terminal et le handler d'écran | Un seul appelant, toujours dans le bon ordre. La table est indexée par UUID, et chaque ouverture écrase l'ancienne entrée. Aucune conséquence atteignable. |
| C-AR-17 | Tables d'addons et constantes d'énergie codées en dur | Une machine inconnue échoue sans danger, par conception (R1). Le besoin est spéculatif, et le correctif ouvrirait une porte à sens unique (nouvelles clés de configuration). |
