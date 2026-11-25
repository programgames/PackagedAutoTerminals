# Rapport — audit du 2026-11-25

Branche `audit/2026-11-25`, poussée sur GitHub (question 6, option A). Elle part du commit `85dadf6` (ton travail en cours, mode holder et mise à l'échelle). Deux passages de la phase 5 :
- passage 1 : T01 à T20, commits `09d6dfb` à `64a36d1`, rapport `bd2c67a` ;
- passage 2 (`/audit-mod suite`) : T21 à T37, commits `40aa0ca` à `5e5046c`.

`main` n'a pas bougé. Tu fusionnes toi-même.

## 1. Bilan chiffré

| Mesure | Avant (`85dadf6`) | Après le passage 1 | Après le passage 2 |
|---|---|---|---|
| Tickets | — | 20 faits | 34 faits, 3 bloqués, 1 à faire (T38, créé par la rétrospective) |
| Tests JUnit | 44 (5 classes) | 101 (15 classes) | 121 (18 classes) |
| Avertissements javac (`-Xlint:all`) | 1, non bloquant | 0, bloquant (`-Werror`) | 0, bloquant |
| Clés de traduction (`checkLang`) | 141 | 148 | 153 |
| Serveur dédié démarré | jamais | local et CI | local et CI |
| CI GitHub | aucune exécution depuis le 2026-09-18 | run `45659544057` vert, 101 tests | run `45699513457` vert : build, 121 tests, serveur dédié sous Ubuntu |

## 2. Ce qui est fait

Chaque ticket a eu un implémenteur et un relecteur indépendant. Le relecteur a relancé lui-même la vérification. Les résultats ci-dessous sont ceux du relecteur.

### Passage 1

| Ticket | Titre | Résultat du relecteur |
|---|---|---|
| T01 | Le compilateur refuse tout avertissement | rc=0, 0 avertissement |
| T02 | Tests de garde : réseau, noms de classe, bornes | 53 tests ; 6 régressions provoquées font échouer les tests |
| T03 | Les refus s'affichent en rouge | 54 tests ; chaque mutation fait échouer `FeedbackKeysTest` |
| T04 | Mesurer la taille de l'instantané | limite 27 paires ; ton réseau : 20 % |
| T05 | Logique de liste du porte-recettes pure et testée | 71 tests ; comportement identique, comparé cas par cas |
| T06 | Un serveur dédié démarre | exit 0 en local et sur un clone propre |
| T07 | Clics sur les cases fantômes | 74 tests ; branche morte prouvée morte |
| T08 | Une recette est désignée par son identité | 78 tests |
| T09 | Supprimer demande un clic gauche et une confirmation | 83 tests |
| T10 | Bouton de ligne : insérer ou réparer seulement | 87 tests |
| T11 | Plus de regroupement à chaque tick ni à chaque image | chaque écriture décide sur un scan frais |
| T12 | L'instantané reste sous le plafond NBT | limite 52 paires ; message au lieu d'une déconnexion |
| T13 | Un seul calcul dit si un groupe a un problème | 96 tests |
| T14 | Permission BUILD et types désactivés | les 5 chemins d'ouverture contrôlés |
| T15 | Le client suit la configuration du serveur | 99 tests |
| T16 | L'avis de mise à jour compare les bonnes versions | `VERSION = "0.1.0-beta.10"` |
| T17 | Le terminal sans fil suit l'objet tenu | accepté au second essai |
| T18 | 10 lignes, libellés traduits | 331 px, échelle 3 en 1080p |
| T19 | L'éditeur client ne lit plus la grille | corrige aussi un défaut ancien (onglet crafting → processing) |
| T20 | Retirer `tooltipStacks` et le code mort | un ancien fichier de configuration se charge |

### Passage 2

| Ticket | Titre | Commande de vérification | Résultat du relecteur |
|---|---|---|---|
| T21 | Un nom de groupe s'efface quand la machine casse | `./gradlew cleanTest test` + `server-smoke.sh` | 103 tests ; serveur dédié OK |
| T22 | Limite de 20 recettes lue par réflexion | `./gradlew cleanTest build` | 105 tests ; la limite suit un changement de config en jeu |
| T23 | Refus du mode holder visible, orphelines en orange | `./gradlew build` | 105 tests ; 16 langues |
| T24 | Les gestes cachés ont une infobulle | `./gradlew clean build` | **accepté au second essai** (ordre de dessin) |
| T25 | Flèche du type, ressources fermées | `./gradlew cleanTest build` + 2 mutations | 106 tests |
| T26 | La documentation du code dit ce que fait le code | diff filtré + `./gradlew clean build` | 0 ligne de code exécutable changée |
| T27 | Le wiki et le protocole de test disent la vérité | `./gradlew build` | 11 fichiers `.md` ; marqueurs 🆕 vérifiés dans l'historique |
| T28 | Glossaire du domaine | `test -f GLOSSARY.md` | chaque nom cité existe |
| T29 | Une seule vérification d'accès sans fil | `./gradlew cleanTest build` + `server-smoke.sh` | 110 tests ; corrige aussi le test L8 (AE2WUT) |
| T30 | Un seul choix de message après une écriture | `./gradlew cleanTest test` + mutation | 119 tests ; 38 clés vues avant et après |
| T31 | L'écran lit la géométrie des cases | bytecode comparé + `server-smoke.sh` | aucun pixel ne bouge |
| T32 | Le terminal renvoie le NBT reçu du serveur | `./gradlew cleanTest test` | 121 tests |
| T33 | Aucun champ `@GuiSync` ne laisse le client écrire l'état du serveur | vérification refaite par le relecteur | **un plantage de serveur fermé** (voir section 7) |
| T37 | Double chargement de MixinBooter | `server-smoke.sh` puis `grep -c 'Multiple Mixin containers'` | 0 ; PackagingProvider charge |

## 3. Ce qui est bloqué

| Ticket | Pourquoi | Ce qui est prêt |
|---|---|---|
| T34 — CHANGELOG et `CODING_STANDARDS.md` | Deux revues refusées. Toutes les lignes du CHANGELOG ont été vérifiées exactes, sauf trois phrases : la règle 3 de `CODING_STANDARDS.md` est fausse pour le bouton Delete de l'éditeur (un clic, voulu par T09) ; la règle 1 ne dit pas que ~49 commentaires anciens ne la suivent pas encore ; la ligne T24 du CHANGELOG oublie que l'onglet Machines n'affiche que le texte libre. | `docs/audit/T34-essai.patch` (`git apply`), puis trois phrases à corriger. |
| T35 — La CI refuse une branche sans CHANGELOG | Attend T34. | Rien. |
| T36 — Test des champs `@GuiSync` | Deux revues refusées. Le second essai détecte 25 mutations sur 27 ; un `onUpdate` à retour anticipé passe encore. | `docs/audit/T36-essai.patch`, puis remplacer `[^{]*` par `[^{;]*` dans `GUARD` (correction vérifiée par le relecteur). Voir aussi la rétrospective : une version par réflexion serait plus solide. |

Le CHANGELOG `[Unreleased]` ne décrit donc pas encore les changements de l'audit. **Ne publie pas de version avant T34.**

## 4. Portes à sens unique

- `tooltipStacks` retiré (T20) : fait, avec ton accord (question 5).
- Push de la branche d'audit : fait, avec ton accord (question 6).
- Aucune porte n'est en attente. Le format de sauvegarde de `GroupNames` n'a pas changé (T21, DA-07). Les changements de format réseau (T08, T12, T15, T20) sont à double sens : FML exige la même version des deux côtés (DA-02).
- `run/mods/mixinbooter-10.7.jar` a été retiré de ton dossier local (T37). Le même jar reste dans `libs/`, et une copie existe dans `C:\Users\Julien\AppData\Local\Temp\claude\156f49fc3d88051d\72558024-eef1-46e3-949c-08b5aee1befe\scratchpad\mixinbooter-10.7.jar`. Ne le remets pas dans `run/mods` : `checkDevMods` le refuserait.

## 5. QA manuelle en jeu

Aucun test automatique ne prouve ces points. Chaque point prend une à cinq minutes.

1. **Cases fantômes (T07).** Ouvre l'éditeur, pose un objet au clic dans une case, puis clique un autre onglet : le message « non enregistré » apparaît. Main vide, clic droit sur une case remplie : la case se vide. En type crafting, clique hors du carré 3×3 : rien ne change.
2. **Suppression (T09).** Clic droit sur la croix d'une recette : rien. Deux clics gauches en moins de 3 s : la recette disparaît. Dans l'éditeur, Delete garde la grille ; Save la restaure.
3. **Bouton de ligne (T10).** Paire saine : aucun bouton. Deux fois Maj + clic gauche sur la ligne : les porte-recettes reviennent au réseau. Packager {A, B} et Unpackager {A} sans nom : pas de « réparer », l'infobulle demande le même nom.
4. **Serveur dédié et client (T08, T32).** Lance `runServer` et `runClient`. Supprime une recette de crafting, puis une de processing : chacune disparaît des deux machines. Si tu as une recette ajoutée par un script côté serveur seulement, ouvre-la dans l'éditeur : pas de `group_changed`.
5. **Permission et accès sans fil (T14, T29).** Joueur sans `BUILD` : aucun écran ne s'ouvre. Terminal sans fil hors de portée ou avec la station cassée : message dans la barre d'action, aucun écran. Terminal universel AE2WUT réglé sur un autre mode : la touche ouvre notre écran au premier appui (L8).
6. **Terminal sans fil (T17).** Ouvre-le depuis la case 3. Survole une case vide et presse 3 : l'écran se ferme. Infobulle : « Lié » ou « Non lié ».
7. **Hauteur, libellés et infobulles (T18, T24).** En 1920×1080 plein écran, échelle Auto : échelle 3, 10 lignes. Survole le champ de recherche (3 lignes sur Patterns, 1 sur Machines) et le bouton `×N`.
8. **Onglets et flèche (T19, T25).** En mode holder, avec une recette crafting et une recette processing, passe de l'une à l'autre : sorties visibles, Save actif. La flèche droite avance dans le même ordre que l'Encoder.
9. **Mode holder et mise à l'échelle (D43, D44).** Joue O1, O3, O6, P1 et P3 de `docs/TESTING.md`. Ces fonctions déplacent de vrais objets et n'ont jamais été jouées.
10. **Écriture pendant un craft (C-VE-04, test N9).** Craft AE2 de 64 objets sur une paire lente, ×2 pendant le craft, puis annulation : compte les objets rendus.

## 6. Décisions prises seul

Voir `docs/audit/DECISIONS.md`, DA-01 à DA-13. Aucune nouvelle décision au passage 2 : chaque ticket suivait ses critères, et les implémenteurs ont noté leurs choix dans leur rapport (repris dans la note de chaque ticket de `tickets.json`).

## 7. Faits nouveaux trouvés pendant les tickets

- **Un client modifié pouvait faire planter le serveur (T33).** AE2 laisse tout client écrire un champ `@GuiSync` int, long, boolean ou enum de son conteneur, côté serveur. Une valeur négative de `tabOffset` atteignait `recipes.get(-n)` pendant le tick du joueur : plantage « Ticking player ». T33 borne la valeur. Les autres champs touchent seulement l'écran du tricheur.
- **AE2WUT et la touche (T29).** Avec un terminal universel réglé sur un autre mode, le premier appui sur la touche n'ouvrait rien et n'affichait rien. Corrigé.
- **MixinBooter (T37).** ForgeGradle 2.3.10 met tout `libs/` sur le chemin d'exécution, pas seulement `libs/maven`. `CLAUDE.md` §4.1 le dit encore mal (ticket T38).
- **Recettes connues du seul serveur (T32).** Le client les affichait, mais le serveur refusait de les modifier. Corrigé.
- **Les routeurs (Distributor, Proxy) affichent « no recipe » en rouge**, car aucun type de recette ne les vise. Défaut d'interface probable, non corrigé (backlog).

## 8. Backlog issu des revues

Chaque point est détaillé dans la note de son ticket (`tickets.json`) :
- T38 (nouveau, dette) : rendre vrais `CLAUDE.md` §4.1, le commentaire de `build.yml`, les imprécisions d'`ARCHITECTURE.md` et de `DECISIONS.md` R1/R2 relevées par T26, et le marqueur 🆕 de B2 (T27).
- Interface : routeurs en rouge (T27) ; survol des titres « Crafters » et « Routers » qui affiche `orphan_help` (T23) ; infobulle de recherche qui cache le texte tapé (T24) ; police Unicode qui ramène l'échelle 3 à 2 (T18).
- Accès sans fil : station détruite pendant que l'éditeur sans fil est ouvert, le Retour affiche `not_linked` et l'éditeur reste ouvert (T29).
- Robustesse : pas de borne contre la limite de 32 767 octets d'un paquet client → serveur avec une recette à gros NBT (T08, T32) ; `PLACE_JEI_GHOST_ITEM` d'AE2 contourne `doAction` (T07).
- Couverture : `securityBreak` d'AE2 et les explosions ne purgent pas un nom de groupe (T21, accepté par DA-07).
- Dette : second lien dur vers `TileEncoder.disabledRecipeTypes` (T14) ; constantes inutilisées dans `ContainerPatEditor` (T31) ; renommages du glossaire (T28).

## 9. Rétrospective

### Passage 1 (rappel)

Les quatre lignes de jugement (commentaire = règle + preuve ; identité, pas index ; geste destructeur confirmé ; écriture sur scan frais) attendent T34. Le contrôle CI du CHANGELOG attend T35. Le cliquet `@GuiSync` attend T36.

### Passage 2

| Problème qui revient | Où | Nature | Proposition |
|---|---|---|---|
| Un texte (CHANGELOG, wiki, règle) promet plus que le code | T34 (deux refus), T26, T27, T34 | jugement | Ligne pour `CODING_STANDARDS.md` : « Une ligne de CHANGELOG ou de wiki donne la condition exacte du comportement, vérifiée dans le code, jamais dans le message de commit. » À ajouter dans T34. |
| Un test qui lit les sources par expression régulière laisse passer des formes valides | T36 (deux refus), `FeedbackKeysTest` | mécanique | Réécrire T36 **par réflexion** : charger les conteneurs avec `Class.forName(nom, false, loader)` et lire leurs champs `@GuiSync` (rétention RUNTIME, car AE2 les lit par réflexion dans `AEBaseContainer`). Aucune forme d'annotation ne peut alors échapper. Seule la règle sur `onUpdate` garde une lecture de source. Noté dans T36. |
| Le hook `rtk` casse une commande `git` avec des chemins | orchestrateur, T34 | mécanique | Réglé : `CONTEXTE.md` dit d'utiliser `rtk proxy git …` pour `diff`, `restore` et `rm` avec des chemins. |
| Le second essai corrige le point bloquant mais pas les notes voisines | T24, T34, T36 | jugement | Le relecteur classe déjà ses remarques. Aucune règle nouvelle. |

## 10. Pour continuer

1. Lis ce rapport et joue la QA de la section 5, au moins les points 1 à 5.
2. Ne publie pas avant T34 : le CHANGELOG ne décrit pas encore l'audit.
3. Fusionne `audit/2026-11-25` dans `main` toi-même, si la CI est verte.
4. Lance `/audit-mod suite` pour T34, T35, T36 et T38. Les patchs de T34 et T36 rendent ces tickets courts.
