# Tests manuels en jeu

> Le mod touche à un réseau ME et à des recettes encodées. Aucun test automatique ne couvre
> cela. Chaque lot se valide donc à la main, dans l'ordre ci-dessous.

## Préparer l'environnement

1. Vérifie que `JAVA_HOME` pointe vers le JDK 8 Adoptium.
2. Lance `gradlew runClient`.
3. Crée un monde créatif superplat.

> ⚠️ Ne copie jamais dans `run/mods` un jar déjà présent dans `libs/`. FML refuse de
> démarrer avec `DuplicateModsFoundException`. Voir CLAUDE.md, section 4.1.

## T1 — Le mod se charge (lot 1)

1. Ouvre le menu « Mods ».
2. Vérifie que `PackagedAuto Terminals` apparaît dans la liste.
3. Vérifie que le journal affiche `PackagedAuto Terminals … : pre-init`.

**Attendu** : aucun avertissement, aucune erreur.

## T2 — Découverte des fournisseurs (lot 2)

1. Pose un contrôleur ME, un câble et une source d'énergie créative.
2. Pose un Packager, relie-le au réseau.
3. Encode une recette avec le Package Recipe Encoder.
4. Insère le Package Recipe Holder dans le Packager.
5. Ouvre le terminal.

**Attendu** : le Packager apparaît, avec sa recette et son type.

## T3 — Cas limites de découverte (lot 2)

| Cas | Attendu |
|---|---|
| Packager sans holder | la machine apparaît, marquée « vide » |
| Packager hors énergie | la machine apparaît, marquée inactive |
| Unpackager et Packaging Provider | ils apparaissent aussi |
| Packager Extension | ligne rattachée au Packager, jamais une machine séparée |
| Deux réseaux distincts | seul le réseau du terminal apparaît |

## T4 — Écriture (lot 3)

1. Ajoute une recette depuis le terminal.
2. Ferme le terminal. **Ne touche pas au bloc.**
3. Ouvre le terminal de craft AE2 et demande le craft de la sortie.

**Attendu** : AE2 propose le craft. C'est la preuve que `postPatternChange()` a bien eu lieu.

4. Supprime la recette depuis le terminal.
5. Redemande le craft.

**Attendu** : AE2 ne propose plus le craft.

## T5 — Types de recettes (lot 3)

Répète T4 pour chaque type : `Crafting`, `Processing`, `Basic`, `Ultimate`, `Extreme`.

**Attendu** : la grille d'édition change de forme selon le type, d'après
`IRecipeType.getEnabledSlots()`.

## T6 — Diagnostic (lot 5)

1. Encode une recette `Ultimate` sans poser d'Ultimate Crafter.
2. Ouvre l'onglet « Machines ».

**Attendu** : le terminal signale la recette orpheline.

## T7 — Sans fil (lot 6)

1. Pose un point d'accès sans fil.
2. Éloigne-toi jusqu'à sortir de portée.

**Attendu** : le terminal se ferme et explique pourquoi.

## T8 — Instance réelle (lot 7)

1. Copie le jar dans `H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`.
2. Lance l'instance.

**Attendu** : le mod se charge et le terminal fonctionne avec le fork AE2UEL du joueur.
