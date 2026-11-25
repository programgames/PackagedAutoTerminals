# Rapport — audit du 2026-11-25

Branche `audit/2026-11-25`, poussée sur GitHub (question 6, option A). Elle part du commit `85dadf6` (ton travail en cours, mode holder et mise à l'échelle) et porte 21 commits : `1c53491` (documents d'audit) puis `09d6dfb` à `64a36d1` (un commit par ticket).

## 1. Bilan chiffré

| Mesure | Avant (`85dadf6`) | Après (`64a36d1`) |
|---|---|---|
| Tickets | — | 20 faits, 0 bloqué, 13 à faire |
| Tests JUnit | 44 (5 classes) | 101 (15 classes) |
| Avertissements javac (`-Xlint:all`) | 1, non bloquant | 0, et `-Werror` les rend bloquants |
| Clés de traduction (`checkLang`) | 141 | 148 |
| Serveur dédié démarré | jamais | `tools/server-smoke.sh`, en local et en CI |
| Limite de l'instantané | inconnue (27 paires, mesuré) | 52 paires, plafond serveur sans déconnexion |
| CI GitHub | aucune exécution depuis le 2026-09-18 ; 0 test jamais lancé | run `45659544057` vert : build, 101 tests, serveur dédié démarré sous Ubuntu |

## 2. Ce qui est fait

Chaque ticket a eu un implémenteur et un relecteur indépendant. Le relecteur a relancé lui-même la vérification. Les résultats ci-dessous sont ceux du relecteur.

| Ticket | Titre | Commande de vérification | Résultat du relecteur |
|---|---|---|---|
| T01 | Le compilateur refuse tout avertissement | `rtk proxy ./gradlew clean build`, puis `grep -c warning:` | rc=0, 0 avertissement, 44 tests |
| T02 | Tests de garde : réseau, noms de classe, bornes | `./gradlew cleanTest test` | 53 tests ; 6 régressions provoquées font échouer les tests |
| T03 | Les refus s'affichent en rouge, avec un test de classification | `./gradlew cleanTest test` + 2 mutations | 54 tests ; chaque mutation fait échouer `FeedbackKeysTest` |
| T04 | Mesurer la taille de l'instantané | `./gradlew cleanTest test -i \| grep -E 'snapshot\|limit'` | limite 27 paires (2 095 268 octets comptés) ; ton réseau : 20 % |
| T05 | Logique de liste du porte-recettes pure et testée | `./gradlew cleanTest test` | 71 tests ; comportement comparé cas par cas, identique |
| T06 | Un serveur dédié démarre | `bash tools/server-smoke.sh` | exit 0 en local et sur un clone propre ; une fuite de classe client injectée donne exit 1 |
| T07 | Les clics sur les cases fantômes passent par l'éditeur | `./gradlew cleanTest build` | 74 tests ; branche morte de `slotClick` prouvée morte dans les sources AE2 |
| T08 | Une recette est désignée par son identité | `./gradlew cleanTest test` | 78 tests (index juste, décalé, recette absente) |
| T09 | Supprimer demande un clic gauche et une confirmation | `./gradlew cleanTest build` | 83 tests ; 3 clés dans les 16 langues |
| T10 | Le bouton de ligne : insérer ou réparer seulement | `./gradlew cleanTest build` | 87 tests ; éjection gardée sur Maj + clic gauche, confirmée |
| T11 | Plus de regroupement à chaque tick ni à chaque image | `./gradlew cleanTest build` + lecture | 87 tests ; chaque écriture décide sur un scan frais |
| T12 | L'instantané reste sous le plafond NBT | `./gradlew cleanTest test` | 90 tests ; limite 52 paires ; au-delà, un message rouge, pas de déconnexion |
| T13 | Un seul calcul dit si un groupe a un problème | `./gradlew cleanTest test` | 96 tests ; seul changement : le filtre ne cache plus une ligne rouge |
| T14 | Permission BUILD et types désactivés | `./gradlew cleanTest build` | 99 tests ; les 5 chemins d'ouverture passent par le contrôle |
| T15 | Le client suit la configuration du serveur | `./gradlew cleanTest build` | 99 tests |
| T16 | L'avis de mise à jour compare les bonnes versions | `javap -constants … Reference` | `VERSION = "0.1.0-beta.10"` ; beta.9 < beta.10 < beta.11 |
| T17 | Le terminal sans fil suit l'objet tenu | `./gradlew clean build` | **accepté au second essai** ; 99 tests |
| T18 | 10 lignes, libellés traduits | `./gradlew cleanTest build` | 100 tests ; 331 px, échelle 3 en 1080p (`TerminalSizeTest`) |
| T19 | L'éditeur client ne lit plus la grille | `./gradlew clean build` + `server-smoke.sh` | 100 tests ; corrige aussi un défaut ancien (onglet crafting → processing) |
| T20 | Retirer `tooltipStacks` et le code mort | `./gradlew build` puis `grep -rn tooltipStacks src/` | `grep_rc=1` ; 101 tests ; un ancien fichier de configuration se charge |

## 3. Ce qui est bloqué

Aucun ticket. T17 a été refusé une fois : le premier essai ne voyait pas une pile vidée sur place par un client modifié. Le second essai l'a corrigé.

## 4. Portes à sens unique

- `tooltipStacks` (T20) : faite, avec ton accord (question 5).
- Push de la branche d'audit : fait, avec ton accord (question 6). `main` n'a pas bougé.
- Aucune autre porte n'est en attente. Le format de sauvegarde de `GroupNames` reste intact : T21 prévoit une purge sans nouveau champ (DA-07).
- Les changements de format réseau (T08, T12, T15, T20) sont des portes à double sens : FML exige la même version du mod des deux côtés (DA-02).

## 5. QA manuelle en jeu

Aucun test automatique ne prouve ces points. Chaque point prend une à cinq minutes.

1. **Cases fantômes (T07).** Ouvre l'éditeur, pose un objet au clic dans une case, puis clique un autre onglet : le message « non enregistré » apparaît. Main vide, fais un clic droit sur une case remplie : la case se vide. En type crafting, clique hors du carré 3×3 : rien ne change.
2. **Suppression (T09).** Dans le terminal, fais un clic droit sur la croix d'une recette : rien n'est supprimé. Fais deux clics gauches sur la croix en moins de 3 s : la recette disparaît. Dans l'éditeur, clique Delete : la grille reste, avec le point rouge ; clique Save : la recette revient.
3. **Bouton de ligne (T10).** Survole une paire saine : aucun bouton. Fais deux fois Maj + clic gauche sur la ligne : les porte-recettes reviennent au réseau. Prépare un Packager {A, B} et un Unpackager {A} sans nom : aucun bouton « réparer », et l'infobulle demande le même nom.
4. **Identité sur serveur dédié (T08).** Lance `runServer` et `runClient`. Fais Maj + clic droit deux fois sur une recette de crafting, puis sur une recette de processing : chacune disparaît des deux machines. Fais deux suppressions très rapides sur la même ligne : la seconde affiche `group_changed`.
5. **Permission (T14).** Avec un Security Terminal, prends un joueur sans `BUILD`. Clique le terminal câblé, puis le terminal sans fil : un message, aucun écran. Mets `disabled_recipe_types=["packagedauto:crafting"]` dans la config de PackagedAuto : les flèches sautent Crafting.
6. **Terminal sans fil (T17).** Ouvre le terminal sans fil depuis la case 3 de la barre rapide. Survole une case vide de l'inventaire et presse 3 : l'écran se ferme. Survole le terminal hors écran : « Lié » ou « Non lié ».
7. **Hauteur (T18).** En 1920×1080 plein écran, échelle Auto, ouvre le terminal : l'échelle reste 3, la liste montre 10 lignes. En `fr_fr`, ouvre le mode holder : « Porte-r. », « Jumeau » et « Unir » tiennent dans leurs boutons.
8. **Onglets de types différents (T19).** En mode holder, avec un porte-recettes qui contient une recette crafting et une recette processing, clique l'une puis l'autre : les sorties restent visibles et Save reste actif.
9. **Mode holder et mise à l'échelle (D43, D44).** Joue les tests O1, O3, O6, P1 et P3 de `docs/TESTING.md`. Ces deux fonctions déplacent de vrais objets et n'ont jamais été jouées.
10. **Écriture pendant un craft (C-VE-04, test N9).** Lance un craft AE2 de 64 objets sur une paire lente. Pendant le craft, applique ×2 au groupe. Regarde le CPU : le job finit-il ? Annule-le, puis compte les objets rendus.

## 6. Décisions prises seul

Voir `docs/audit/DECISIONS.md`, DA-01 à DA-13. Les plus importantes :
- DA-01 : les sous-agents n'ont pas pu écrire leurs rapports ; l'orchestrateur les a recopiés sans modification.
- DA-02 : un changement de format réseau est une porte à double sens, car FML exige la même version des deux côtés.
- DA-05 et DA-06 : la permission `BUILD` à l'ouverture suit D12 ; les flèches de type suivent l'Encoder.
- DA-13 : la limite de T08 (recettes de crafting connues du seul serveur) devient le ticket T32.

## 7. Faits nouveaux trouvés pendant les tickets

- **AE2 laisse le client écrire certains champs du serveur.** `PacketProgressBar.serverPacketData` applique côté serveur une valeur `@GuiSync` choisie par le client. T14 a fermé ce chemin pour le type de recette. Le ticket T33 vérifie les autres champs.
- **Un défaut ancien corrigé par T19.** Depuis `7cee15e`, passer d'un onglet crafting à un onglet processing effaçait les sorties côté client et laissait Save désactivé. T11 n'en est pas la cause : le relecteur de T19 l'a vérifié.
- **MixinBooter se charge deux fois en développement** (`libs/` et `run/mods`), contre ce que dit `CLAUDE.md` (relecteur de T06).
- **Les jars beta.1 à beta.9 déjà publiés** gardent le préfixe de version : l'avis de mise à jour ne marchera qu'à partir de la prochaine version publiée (T16).

## 8. Backlog issu des revues

Ces points sont notés dans `tickets.json`, champ `note` de chaque ticket fait :
- CHANGELOG `[Unreleased]` : T09, T10, T12, T14, T17, T19 et T20 changent le comportement visible, sans ligne de CHANGELOG (ticket T34).
- `docs/CURSEFORGE.md` et `docs/curseforge-description.md` décrivent encore les anciens gestes.
- Police Unicode (ja, ko, zh, ru, uk) : Minecraft ramène l'échelle 3 à 2, et `GuiScaleFit` ne copie pas cette règle.
- `recipes_scaled_skipped` garde « 1 recipes » ; `tools/preview_terminal.py` garde 12 lignes.
- Forge garde la clé `tooltipStacks` dans les anciens fichiers, et l'écran de configuration par défaut la montre comme une entrée brute sans effet.
- `PLACE_JEI_GHOST_ITEM` d'AE2 écrit dans une case fantôme sans passer par `doAction` (client modifié seulement).
- Une recette de plus de 32 Ko de NBT dépasse la taille d'un paquet client → serveur (T08).

## 9. Rétrospective

Problèmes qui reviennent, et la réponse proposée. Chaque proposition devient un ticket `harnais` à double sens (T34 à T37).

| Problème qui revient | Où | Nature | Proposition |
|---|---|---|---|
| Le CHANGELOG ne suit pas les changements visibles | revues de T09, T10, T12, T20 | mécanique | **T35** : une étape CI échoue quand une branche modifie `src/main` sans modifier la section `[Unreleased]` de `CHANGELOG.md`. Cliquet : la règle vaut pour les branches nouvelles. |
| Le serveur croit une valeur du client | C-MC-07, T14, revue de T07 | mécanique | **T36** : un test JUnit lit nos conteneurs et échoue si une méthode `onUpdate` change l'état sans garde `world.isRemote`. On mesure l'existant, puis le nombre ne peut plus augmenter. Bloqué par T33. |
| Une vérification qui ment : sortie filtrée par `rtk`, tâche `:test` UP-TO-DATE | T01, T02 | mécanique | Réglé dans `CONTEXTE.md` (`rtk proxy`, `cleanTest test`). Aucun ticket : la consigne a suffi pour les 18 tickets suivants. |
| Une affirmation de `CLAUDE.md` contredite par l'exécution | revue de T06 (MixinBooter chargé deux fois) | vérification | **T37** : vérifier le double chargement, puis corriger `CLAUDE.md` ou le build. |
| Un commentaire raconte l'histoire au lieu de la règle, puis devient faux | C-AR-08, C-AR-13, C-AR-18, revues de T08 et T11 | jugement | Ligne pour `CODING_STANDARDS.md` : « Un commentaire donne la règle et sa preuve (`fichier:ligne`), jamais l'histoire du bug ; l'histoire va dans le CHANGELOG. » |
| Une action désigne une recette ou une machine par sa position | C-MC-03, C-AR-01, T08 | jugement | Ligne : « Un paquet nomme une recette par son identité et une machine par sa position dans le monde, jamais par un index de liste. » |
| Un geste détruit sans confirmation | C-PR-01, C-PR-02 | jugement | Ligne : « Un geste qui retire ou supprime demande le clic gauche et un second clic ; un geste qui ajoute n'en demande pas. » |
| Une écriture décide sur une vue en cache | T11 | jugement | Ligne : « Une écriture décide toujours sur un scan frais ; un cache sert seulement à l'affichage. » |

**T34** (dette, avant T35) : écrire les lignes de CHANGELOG manquantes pour T09 à T20, puis `CODING_STANDARDS.md` avec les quatre lignes de jugement ci-dessus.

## 10. Pour continuer

1. Lis ce rapport et joue la QA de la section 5, au moins les points 1 à 4.
2. Si la CI de la branche est verte, fusionne `audit/2026-11-25` dans `main` toi-même.
3. Lance `/audit-mod suite` pour traiter T21 à T37 (17 tickets).
