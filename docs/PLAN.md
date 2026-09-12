# Plan d'action

> Validé le 2026-09-12. Aucun lot ne démarre avant que le précédent soit validé.
> Chaque lot se termine par un critère observable en jeu.

## Lot 0 — Lever les inconnues ✅ terminé

| Question | Réponse |
|---|---|
| Un mod fait-il déjà ce terminal ? | **Non.** Rien sur 1.12.2. `Extended Terminal` existe mais cible 1.20/1.21 et ne touche pas à PackagedAuto |
| Licence amont de PackagedAuto | **MIT** — dépendance de compilation et réutilisation de l'API sans obstacle, avec attribution |
| Comment obtenir AE2UEL pour compiler | **CurseMaven** en premier choix. Repli : jar local dans `libs/`, déjà présent sur le poste (`ae2-uel-v0.56.5.jar.bak`). Aucun dépôt Maven officiel trouvé |
| Cleanroom pose-t-il un risque ? | **Non.** Confirmé par le joueur : tous les mods du pack tournent aussi sous Forge. MeatballCraft fonctionne sous Forge ou Cleanroom, sans impact |

Restent ouvertes : **Q1** (URL du fork AE2UEL du joueur) et **Q3** (dépôt public ou privé).
Restent à vérifier dans le code, au début du lot 1 :
parcours générique des nœuds de grille, effet exact de `setPatternStack()`, comportement du
`Packager Extension`.

## Lot 1 — Squelette et environnement de dev ✅ terminé le 2026-09-12

Dépôt Git, `build.gradle`, dépendances épinglées, `mcmod.info`, classe principale vide,
`run/mods` peuplé.

**Résultat** : `gradlew setupDecompWorkspace`, `gradlew build` et `gradlew runClient`
passent. Le client de dev charge **17 mods**, dont `PackagedAuto Terminals 1.12.2-0.1.0`.
Le journal affiche bien `pre-init` puis `init`.

**Trois points de code vérifiés au passage**, consignés dans `docs/PACKAGEDAUTO-MODEL.md`
section 7 :

1. Le Recipe Holder occupe l'**emplacement 10** du Packager.
2. Écrire cet emplacement appelle `updatePatternList()`, qui appelle `postPatternChange()`.
   **Le point le plus fragile du projet est donc résolu avant d'avoir écrit une ligne de
   GUI.** Règle qui en découle : toujours réécrire le stack, jamais modifier son NBT en place.
3. La découverte se fait par `IGrid.getMachinesClasses()` puis `getMachines(cls)`, sans
   connaître aucune classe d'addon à la compilation.

**Contrainte D20 prouvée** : une sonde a compilé contre les classes internes d'AE2
(`AbstractPartTerminal`, `AEBasePoweredItem`). Le montage `flatDir` + `deobfProvided` tient.

**Reste à faire par toi** : ouvrir le menu « Mods » du client déjà lancé, et vérifier la
ligne `PackagedAuto Terminals`. Puis le test de chargement dans l'instance Cleanroom réelle,
avec le jar de `build/libs/`.

## Lot 2 — Lecture seule, terminal câblé ✅ terminé le 2026-09-12

Découverte des `IPackageProvidingMachine` sur la grille. Instantané côté serveur. Paquet
vers le client. GUI qui liste les machines et leurs recettes. Aucune écriture.

**Résultat** : le terminal se pose sur un câble ME, s'ouvre, et affiche le Packager, son
état, puis sa recette avec le type `Processing`. Infobulle au survol, ascenseur en place.

**Mesure R2** : **344 octets** pour une machine et une recette. Le découpage en chunks reste
donc inutile. La mesure sur un réseau chargé est à refaire avant de clore la révision R2.

### Ce que ce lot a coûté, et pourquoi

Sept défauts, tous dus à des suppositions non vérifiées sur l'API d'un autre mod :

| Défaut | Cause |
|---|---|
| l'item ne se posait pas | `IPartItem` seul ne suffit pas ; il faut rediriger `onItemUse` vers `PartPlacement` |
| la fenêtre ne s'ouvrait pas | `AEBaseContainer` refuse un `Slot` vanilla ; il exige un `AppEngSlot` |
| la fenêtre se refermait | le constructeur à `TileEntity` appelé avec `null` |
| traductions absentes | pas de `pack.mcmeta` ; Forge appliquait les règles d'avant la 1.11 |
| planche de fond cassée | bande d'AE2 réutilisée sans la regarder ; c'était un champ de recherche |
| texte chevauché | deux lignes dans une rangée de 18 pixels |
| code périmé au lancement | FML charge le mod depuis le jar, pas depuis les classes |

**Règle qui en découle, et qui vaut pour les lots suivants** : ne jamais supposer le
comportement d'une classe d'AE2. La lire avec `javap`, ou lire la source amont, avant de
l'utiliser. Les six premiers défauts auraient été évités par dix minutes de lecture.

## Lot 3 — Écriture ✅ terminé le 2026-09-13

Éditeur piloté par `IRecipeType.getEnabledSlots()`, `getSlotColor()`, `canSetOutput()`.
Encodage par `IRecipeInfo.generateFromStacks()`, côté serveur. Validation par `isValid()`.
Écriture dans le holder, puis republication des patterns AE2.

**Fait quand** : j'ajoute, je modifie et je supprime une recette depuis le terminal, et AE2
voit le changement sans que je touche au bloc.

## Lot 4 — JEI et confort ✅ écrit le 2026-09-13, non testé en jeu

`IRecipeType.getRecipeTransferMap(IRecipeLayout, String)` existe déjà dans l'API. Le
transfert JEI est donc peu coûteux, **à condition** que l'éditeur soit un vrai `Container`
avec des slots fantômes indexés comme ceux de l'Encoder. Recherche, filtres, tri.

**Fait quand** : le bouton « + » de JEI remplit l'éditeur du terminal.

## Lot 5 — Onglet « Machines » et diagnostic ✅ écrit le 2026-09-13, non testé en jeu

Table de correspondance `type de recette → classe de crafter`, une par module
d'intégration. Liste des crafters, état `isBusy()`, alerte sur les recettes orphelines.

**Fait quand** : j'encode une recette Ultimate sans poser d'Ultimate Crafter, et le terminal
me le signale.

## Lot 6 — Terminal sans fil — 1 à 2 sessions

Item alimenté, `IWirelessTermHandler`, enregistrement au registre, portée, énergie.

**Fait quand** : le terminal fonctionne à distance et se coupe hors de portée.

## Lot 7 — Intégration, finition, publication 🔶 partiellement écrit

AE2WUT avec identifiant de mode **configurable**, Baubles, `fr_fr`, workflow GitHub,
`CHANGELOG.md`, test final dans l'instance réelle.

**Total indicatif : 10 à 16 sessions.**

---

## Le point le plus fragile du projet

Ce n'est ni la GUI, ni le réseau. C'est **la republication des patterns après écriture**.
Si `setPatternStack()` ne notifie pas correctement la grille, AE2 gardera une vue périmée.
Le joueur verra sa modification à l'écran, sans effet sur ses crafts.
Ce point se vérifie au début du lot 1, jamais au lot 3.


---

## État au 2026-09-13

| Lot | État | Reste |
|---|---|---|
| 0, 1, 2 | ✅ testés en jeu | |
| 3 | ✅ écrit, suppression et édition testées | création, quantités et déplacement **non testés** |
| 4 | ✅ écrit | transfert JEI et recherche **non testés** |
| 5 | ✅ écrit | onglet Machines et diagnostic **non testés** |
| 6 | ⬜ pas commencé | terminal sans fil, sur une branche séparée |
| 7 | 🔶 licence, notice, journal, CI, configuration, recette de fabrication | AE2WUT, Baubles |

> Tout ce qui porte « non testé » attend la session de tests décrite dans
> `docs/TESTING.md`. Le code compile, mais n'a jamais tourné.

### Ce qui reste, par ordre de valeur

1. **Dérouler `docs/TESTING.md`**, de T1 à T15. C'est le seul moyen de valider quinze
   fonctions écrites sans jeu.
2. **Lot 6**, le terminal sans fil. Il demande de détacher le conteneur de la part câblée,
   ce qui touche du code qui marche. À faire sur une branche.
3. **AE2WUT et Baubles**, une fois le sans-fil validé.
4. **Fluides et gaz**, prévus en version 2 (décision D09).
