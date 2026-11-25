# Rapport — audit du 2026-11-25

Branche `audit/2026-11-25`, poussée sur GitHub (question 6, option A). Elle part du commit `85dadf6` (ton travail en cours, mode holder et mise à l'échelle). Trois passages de la phase 5 :
- passage 1 : T01 à T20, commits `09d6dfb` à `64a36d1` ;
- passage 2 : T21 à T37, commits `40aa0ca` à `5e5046c` (T34 et T36 bloqués) ;
- passage 3 : T36, T34, T35, T39, T38, commits `df12430` à `feea8cc`.

**Les 39 tickets sont faits.** `main` n'a pas bougé : tu fusionnes toi-même.

## 1. Bilan chiffré

| Mesure | Avant (`85dadf6`) | Après le passage 1 | Après le passage 3 |
|---|---|---|---|
| Tickets | — | 20 faits | 39 faits, 0 bloqué |
| Tests JUnit | 44 (5 classes) | 101 (15 classes) | 124 (19 classes) |
| Avertissements javac (`-Xlint:all`) | 1, non bloquant | 0, bloquant (`-Werror`) | 0, bloquant |
| Clés de traduction (`checkLang`) | 141 | 148 | 153 |
| Serveur dédié démarré | jamais | local et CI | local et CI |
| Contrôle du CHANGELOG | aucun | aucun | CI (`tools/check-changelog.sh`) |
| CI GitHub | aucune exécution depuis le 2026-09-18 | run `45659544057` vert, 101 tests | run `45714933142` vert : CHANGELOG, build, 124 tests, serveur dédié sous Ubuntu |

## 2. Ce qui est fait

Chaque ticket a eu un implémenteur et un relecteur indépendant. Le relecteur a relancé lui-même la vérification. Les résultats ci-dessous sont ceux du relecteur.

### Passage 1

| Ticket | Titre | Résultat du relecteur |
|---|---|---|
| T01 | Le compilateur refuse tout avertissement | rc=0, 0 avertissement |
| T02 | Tests de garde : réseau, noms de classe, bornes | 6 régressions provoquées font échouer les tests |
| T03 | Les refus s'affichent en rouge | chaque mutation fait échouer `FeedbackKeysTest` |
| T04 | Mesurer la taille de l'instantané | limite 27 paires ; ton réseau : 20 % |
| T05 | Logique de liste du porte-recettes pure et testée | comportement identique, comparé cas par cas |
| T06 | Un serveur dédié démarre | exit 0 en local et sur un clone propre |
| T07 | Clics sur les cases fantômes | branche morte prouvée morte |
| T08 | Une recette est désignée par son identité | index juste, décalé, absent |
| T09 | Supprimer demande un clic gauche et une confirmation | 3 clés dans 16 langues |
| T10 | Bouton de ligne : insérer ou réparer seulement | éjection gardée sur Maj + clic gauche |
| T11 | Plus de regroupement à chaque tick ni à chaque image | chaque écriture décide sur un scan frais |
| T12 | L'instantané reste sous le plafond NBT | limite 52 paires ; message au lieu d'une déconnexion |
| T13 | Un seul calcul dit si un groupe a un problème | le filtre ne cache plus une ligne rouge |
| T14 | Permission BUILD et types désactivés | les 5 chemins d'ouverture contrôlés |
| T15 | Le client suit la configuration du serveur | `machinesTab` vient du serveur |
| T16 | L'avis de mise à jour compare les bonnes versions | `VERSION = "0.1.0-beta.10"` |
| T17 | Le terminal sans fil suit l'objet tenu | accepté au second essai |
| T18 | 10 lignes, libellés traduits | 331 px, échelle 3 en 1080p |
| T19 | L'éditeur client ne lit plus la grille | corrige aussi l'onglet crafting → processing |
| T20 | Retirer `tooltipStacks` et le code mort | un ancien fichier de configuration se charge |

### Passage 2

| Ticket | Titre | Résultat du relecteur |
|---|---|---|
| T21 | Un nom de groupe s'efface quand la machine casse | serveur dédié OK |
| T22 | Limite de 20 recettes lue par réflexion | suit un changement de config en jeu |
| T23 | Refus du mode holder visible, orphelines en orange | 16 langues |
| T24 | Les gestes cachés ont une infobulle | accepté au second essai (ordre de dessin) |
| T25 | Flèche du type, ressources fermées | 2 mutations |
| T26 | La documentation du code dit ce que fait le code | 0 ligne de code exécutable changée |
| T27 | Le wiki et le protocole de test disent la vérité | marqueurs 🆕 vérifiés dans l'historique |
| T28 | Glossaire du domaine | chaque nom cité existe |
| T29 | Une seule vérification d'accès sans fil | corrige aussi le test L8 (AE2WUT) |
| T30 | Un seul choix de message après une écriture | 38 clés vues avant et après |
| T31 | L'écran lit la géométrie des cases | aucun pixel ne bouge (bytecode comparé) |
| T32 | Le terminal renvoie le NBT reçu du serveur | recette connue du seul serveur |
| T33 | Aucun champ `@GuiSync` ne laisse le client écrire l'état du serveur | un plantage de serveur fermé |
| T37 | Double chargement de MixinBooter | 0 « Multiple Mixin containers » |

### Passage 3

| Ticket | Titre | Commande de vérification | Résultat du relecteur |
|---|---|---|---|
| T36 | Un test interdit qu'un `onUpdate` change l'état du serveur | `rtk proxy ./gradlew cleanTest test` + 57 mutations | **accepté au troisième essai** ; les 52 formes courantes détectées |
| T34 | Le CHANGELOG et les règles de code suivent l'audit | `./gradlew build` + `test -f CODING_STANDARDS.md` | **accepté au troisième essai** ; chaque ligne vérifiée contre le code |
| T35 | La CI refuse une branche qui change le code sans CHANGELOG | `bash tools/check-changelog.sh` sur une branche jetable | 6 cas vérifiés |
| T39 | Le bouton propose « insérer » quand une machine n'a pas de porte-recettes | `rtk proxy ./gradlew cleanTest test` | le nouveau test échoue sur l'ancien ordre |
| T38 | Les documents de référence disent vrai après l'audit | `./gradlew build` + `check-changelog.sh` | 7 fichiers de documentation, aucun code |

## 3. Ce qui est bloqué

Rien. T34 et T36, bloqués au passage 2, sont faits au passage 3 à partir de leur patch (DA-14).

## 4. Portes à sens unique

- `tooltipStacks` retiré (T20) : fait, avec ton accord (question 5).
- Push de la branche d'audit : fait, avec ton accord (question 6).
- Aucune porte n'est en attente. Le format de sauvegarde de `GroupNames` n'a pas changé (T21, DA-07). Les changements de format réseau (T08, T12, T15, T20) sont à double sens : FML exige la même version des deux côtés (DA-02).
- `run/mods/mixinbooter-10.7.jar` a été retiré de ton dossier local (T37). Le même jar reste dans `libs/`. Ne le remets pas dans `run/mods` : `checkDevMods` le refuserait.

## 5. QA manuelle en jeu

Aucun test automatique ne prouve ces points. Chaque point prend une à cinq minutes.

1. **Cases fantômes (T07).** Ouvre l'éditeur, pose un objet au clic dans une case, puis clique un autre onglet : le message « non enregistré » apparaît. Main vide, clic droit sur une case remplie : la case se vide. En type crafting, clique hors du carré 3×3 : rien ne change.
2. **Suppression (T09).** Clic droit sur la croix d'une recette : rien. Deux clics gauches en moins de 3 s : la recette disparaît. Dans l'éditeur, Delete garde la grille ; Save la restaure.
3. **Bouton de ligne (T10, T39).** Paire saine : aucun bouton. Deux fois Maj + clic gauche sur la ligne : les porte-recettes reviennent au réseau. Packager avec recettes et Unpackager sans porte-recettes : le bouton insère d'abord, puis répare au clic suivant (test N1 de `docs/TESTING.md`), avec `pullHolderFromNetwork` à `true` puis à `false`.
4. **Serveur dédié et client (T08, T32).** Lance `runServer` et `runClient`. Supprime une recette de crafting, puis une de processing : chacune disparaît des deux machines. Si tu as une recette ajoutée par un script côté serveur seulement, ouvre-la dans l'éditeur : pas de `group_changed`.
5. **Permission et accès sans fil (T14, T29).** Joueur sans `BUILD` : aucun écran ne s'ouvre. Terminal sans fil hors de portée ou avec la station cassée : message dans la barre d'action, aucun écran. Terminal universel AE2WUT réglé sur un autre mode : la touche ouvre notre écran au premier appui (L8).
6. **Terminal sans fil (T17).** Ouvre-le depuis la case 3. Survole une case vide et presse 3 : l'écran se ferme. Infobulle : « Lié » ou « Non lié ».
7. **Hauteur, libellés et infobulles (T18, T24).** En 1920×1080 plein écran, échelle Auto : échelle 3, 10 lignes. Survole le champ de recherche (3 lignes sur Patterns, 1 sur Machines) et le bouton `×N`.
8. **Onglets et flèche (T19, T25).** En mode holder, avec une recette crafting et une recette processing, passe de l'une à l'autre : sorties visibles, Save actif. La flèche droite avance dans le même ordre que l'Encoder.
9. **Mode holder et mise à l'échelle (D43, D44).** Joue O1, O3, O6, P1 et P3 de `docs/TESTING.md`. Ces fonctions déplacent de vrais objets et n'ont jamais été jouées.
10. **Écriture pendant un craft (C-VE-04, test N9).** Craft AE2 de 64 objets sur une paire lente, ×2 pendant le craft, puis annulation : compte les objets rendus.

## 6. Décisions prises seul

Voir `docs/audit/DECISIONS.md`, DA-01 à DA-14. Au passage 3, seule DA-14 s'applique : T34 et T36 repartent de leur patch, et chaque correction passe par une revue.

T39 a un coût : avec le réglage par défaut (`pullHolderFromNetwork=true`), une paire dont une machine n'a pas de porte-recettes demande maintenant deux clics (insérer, puis réparer) au lieu d'un. En échange, le bouton marche aussi avec `false`, et le joueur lit « No blank Package Recipe Holder on the network » au lieu de « Nothing was written ». Le client ne connaît pas ce réglage du serveur, donc un ordre fixe est le plus simple.

## 7. Faits nouveaux trouvés pendant les tickets

- **Un client modifié pouvait faire planter le serveur (T33).** Une valeur négative de `tabOffset` atteignait `recipes.get(-n)` pendant le tick du joueur. T33 borne la valeur, et T36 empêche qu'un nouveau champ `@GuiSync` rouvre ce chemin sans raison écrite.
- **Le bouton de ligne pouvait rendre l'insertion inaccessible (T39).** Avec `pullHolderFromNetwork=false`, une paire dont l'Unpackager n'avait pas de porte-recettes proposait « réparer », qui n'écrivait rien. Trouvé par le relecteur de T34.
- **AE2WUT et la touche (T29).** Le premier appui n'ouvrait rien avec un terminal universel réglé sur un autre mode. Corrigé.
- **MixinBooter (T37, T38).** ForgeGradle 2.3.10 met tout `libs/` sur le chemin d'exécution. `CLAUDE.md` §4.1 le dit maintenant.
- **Recettes connues du seul serveur (T32).** Corrigé.
- **Les routeurs (Distributor, Proxy) affichent « no recipe » en rouge**, car aucun type de recette ne les vise. Défaut d'interface probable, non corrigé (backlog).

## 8. Backlog issu des revues

Chaque point est détaillé dans la note de son ticket (`tickets.json`) :
- Interface : routeurs en rouge (T27) ; survol des titres « Crafters » et « Routers » qui affiche `orphan_help` (T23) ; infobulle de recherche qui cache le texte tapé (T24) ; police Unicode qui ramène l'échelle 3 à 2 (T18).
- Bouton de ligne : un groupe nommé de 3 machines ou plus, sans porte-recettes vierge sur le réseau, reste sur « insérer » (T39) ; `repairGroup` ne relit pas `fix()` côté serveur (T39, sans dégât).
- Accès sans fil : station détruite pendant que l'éditeur sans fil est ouvert, le Retour affiche `not_linked` et l'éditeur reste ouvert (T29).
- Robustesse : pas de borne contre la limite de 32 767 octets d'un paquet client → serveur avec une recette à gros NBT (T08, T32) ; `PLACE_JEI_GHOST_ITEM` d'AE2 contourne `doAction` (T07) ; `check-changelog.sh` laisserait passer un CHANGELOG committé en CRLF sous Linux (T35) ; 5 formes exotiques échappent à `GuiSyncTest` (T36).
- Couverture : `securityBreak` d'AE2 et les explosions ne purgent pas un nom de groupe (T21, accepté par DA-07).
- Documents : `PLAN.md:94`, `PLAN.md:92/130`, `CLAUDE.md:64` (HadEnoughItems listé dans `run/mods`), `build.yml:33`, Javadoc de `ContainerPatEditor` (T38) ; `CODING_STANDARDS.md` règle 2 sans exception nommée pour le clic d'onglet (T34).
- Dette : second lien dur vers `TileEncoder.disabledRecipeTypes` (T14) ; constantes inutilisées dans `ContainerPatEditor` (T31) ; renommages du glossaire (T28).

## 9. Rétrospective

### Passages 1 et 2 (rappel)

Les règles de jugement sont maintenant dans `CODING_STANDARDS.md` (T34, cinq règles). Le contrôle CI du CHANGELOG existe (T35). Le cliquet `@GuiSync` existe (T36).

### Passage 3

| Problème qui revient | Où | Nature | Proposition |
|---|---|---|---|
| Un second essai qui échoue jette un travail presque juste | T34, T36 | processus | La règle « patch gardé, troisième essai au passage suivant » (DA-14) a marché deux fois sur deux. Proposition pour le skill : après deux refus, garder le diff en patch au lieu de le perdre. |
| Un relecteur trouve un défaut ancien hors de son ticket | T34 → T39, T14 → T33 | jugement | Déjà fait : l'orchestrateur crée un ticket pour chaque défaut prouvé par une revue, au lieu de l'envoyer au backlog. Aucune règle nouvelle. |
| Des phrases fausses restent dans les documents de référence | T26, T27, T38 | mécanique en partie | Le contrôle du CHANGELOG (T35) couvre une partie. Pour le reste, la règle 5 de `CODING_STANDARDS.md` suffit pour l'instant ; aucun ticket. |

## 10. Pour continuer

1. Lis ce rapport et joue la QA de la section 5, au moins les points 1 à 5.
2. Fusionne `audit/2026-11-25` dans `main` toi-même, si la CI est verte. Tu peux ensuite publier : le CHANGELOG `[Unreleased]` décrit l'audit.
3. Le backlog (section 8) ne contient aucun défaut qui perd des données. Lance un nouvel audit (`/audit-mod`) plus tard, sur `main`, pour un regard neuf.
