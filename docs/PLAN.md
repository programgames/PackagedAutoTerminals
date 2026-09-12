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

## Lot 1 — Squelette et environnement de dev — 1 session

Dépôt Git, `build.gradle`, dépendances épinglées, `mcmod.info`, classe principale vide,
`run/mods` peuplé des 6 jars nécessaires.

**Fait quand** : `gradlew runClient` démarre et le mod apparaît dans la liste.
**Plus** : un chargement de contrôle dans l'instance Cleanroom réelle, avec le jar vide.

## Lot 2 — Lecture seule, terminal câblé — 2 à 3 sessions

Découverte des `IPackageProvidingMachine` sur la grille. Instantané côté serveur. Paquet
vers le client. GUI qui liste les machines et leurs recettes. Aucune écriture.

**Fait quand** : je pose un Packager avec un holder encodé, j'ouvre le terminal, je vois la
recette.
**Mesure obligatoire** : taille du paquet sur un réseau à forte charge. Elle décide de
l'ajout, ou non, du découpage en chunks.

> C'est le lot qui prouve la faisabilité du projet.

## Lot 3 — Écriture — 3 à 4 sessions

Éditeur piloté par `IRecipeType.getEnabledSlots()`, `getSlotColor()`, `canSetOutput()`.
Encodage par `IRecipeInfo.generateFromStacks()`, côté serveur. Validation par `isValid()`.
Écriture dans le holder, puis republication des patterns AE2.

**Fait quand** : j'ajoute, je modifie et je supprime une recette depuis le terminal, et AE2
voit le changement sans que je touche au bloc.

## Lot 4 — JEI et confort — 1 à 2 sessions

`IRecipeType.getRecipeTransferMap(IRecipeLayout, String)` existe déjà dans l'API. Le
transfert JEI est donc peu coûteux, **à condition** que l'éditeur soit un vrai `Container`
avec des slots fantômes indexés comme ceux de l'Encoder. Recherche, filtres, tri.

**Fait quand** : le bouton « + » de JEI remplit l'éditeur du terminal.

## Lot 5 — Onglet « Machines » et diagnostic — 1 à 2 sessions

Table de correspondance `type de recette → classe de crafter`, une par module
d'intégration. Liste des crafters, état `isBusy()`, alerte sur les recettes orphelines.

**Fait quand** : j'encode une recette Ultimate sans poser d'Ultimate Crafter, et le terminal
me le signale.

## Lot 6 — Terminal sans fil — 1 à 2 sessions

Item alimenté, `IWirelessTermHandler`, enregistrement au registre, portée, énergie.

**Fait quand** : le terminal fonctionne à distance et se coupe hors de portée.

## Lot 7 — Intégration, finition, publication — 1 à 2 sessions

AE2WUT avec identifiant de mode **configurable**, Baubles, `fr_fr`, workflow GitHub,
`CHANGELOG.md`, test final dans l'instance réelle.

**Total indicatif : 10 à 16 sessions.**

---

## Le point le plus fragile du projet

Ce n'est ni la GUI, ni le réseau. C'est **la republication des patterns après écriture**.
Si `setPatternStack()` ne notifie pas correctement la grille, AE2 gardera une vue périmée.
Le joueur verra sa modification à l'écran, sans effet sur ses crafts.
Ce point se vérifie au début du lot 1, jamais au lot 3.
