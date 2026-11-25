# Contexte vérifié — audit du 2026-11-25

Branche `audit/2026-11-25`, partie de `85dadf6` (mode holder D43 et mise à l'échelle D44, **jamais testés en jeu**).

## Versions (lues dans les fichiers du projet)
- Minecraft 1.12.2, Forge 14.23.5.2847 (`gradle.properties`), MCP `snapshot_20171003`. Joueur : Cleanroom 0.5.17-alpha.
- ForgeGradle 2.3.10 (`build.gradle`), Gradle 4.10.3 (wrapper), JDK 8 obligatoire (`JAVA_HOME` = Temurin 8.0.472).
- Mod `packagedautoterminals` 0.1.0-beta.10. Requis : AE2UEL 0.56.5, PackagedAuto 1.0.24.73. Optionnels : JEI/HEI 4.31.2, AE2WUT 1.0.5 (mixins par nom), AE2FC, PackagedExCrafting, PackagedAvaritia, PackagedFluidCrafting, PackagingProvider.

## Commandes vérifiées
| Commande | Résultat |
|---|---|
| `./gradlew clean build` | vert, 9 s (décompilation Forge en cache), jar `build/libs/packagedautoterminals-1.12.2-0.1.0-beta.10.jar` |
| `./gradlew test` (dans `build`) | 44 tests JUnit 4.12, 0 échec : `FeedbackTest`, `GroupNamesTest`, `ProviderPairingTest`, `RecipeScaleTest`, `SearchQueryTest` |
| javac par défaut | 0 avertissement, 1 note de dépréciation |
| javac `-Xlint:all` (init script, `build.gradle` intact) | 1 avertissement : `[deprecation] I18n` dans `integration/wut/WutSupport.java:13` |
| `checkLang` (dans `check`) | 141 clés, 16 langues en accord |
| `checkDevMods` | dépendance de `runClient`/`runServer`, refuse un doublon `libs/maven` ↔ `run/mods` |
| `runClient`, `runServer` | existent ; jamais lancés pendant cet audit |
| GameTest, datagen | n'existent pas en 1.12.2 / absents du projet |

**Sortie brute de Gradle** : le hook `rtk` filtre la sortie de `./gradlew`. Pour lire ou compter des lignes (`grep warning:`, tests `PASSED`), lance `rtk proxy ./gradlew ...`. Pour relancer vraiment les tests, utilise `cleanTest test` : sinon Gradle marque `:test` UP-TO-DATE et ne relance rien.

**Ne lance pas Gradle pendant la phase d'audit** : quatre auditeurs en parallèle se bloqueraient sur le cache. Les résultats ci-dessus suffisent.

## Sources de référence (preuves API)
Ces dossiers sont dans `.tools/` (ignoré par git) :
- `.tools/forge-src/` — Minecraft + Forge 1.12.2-14.23.5.2847, noms MCP, extrait du `forgeSrc-…-sources.jar` du cache Gradle.
- `.tools/ae2-src/` — AE2UEL 0.56.5, jar désobfusqué par ForgeGradle puis décompilé par CFR 0.152.
- `.tools/pa-src/` — PackagedAuto 1.0.24.73, même méthode.
- `.tools/ct-src/` (Cell Terminal 1.6.7), `.tools/ext-src/` (ae2exttable) — modèles, décompilés par CFR.
- Autres addons : jars dans `run/mods/`. Décompile à la demande avec `"$JAVA_HOME/bin/java" -jar .tools/cfr.jar <jar> --outputdir <dossier du scratchpad>`. Ces jars sont en noms SRG.
- CFR rate parfois une méthode (`summary.txt`) : vérifie le bytecode avec `javap -c` en cas de doute.

## Structure (10 374 lignes Java dans `src/main`, 560 dans `src/test`)
`fr.julien.packagedautoterminals` : `client` (6) + `client/gui` (4), `common` (19), `container` (2), `network` (8), `item` (2), `part` (1), `proxy` (1), `integration/jei` (5), `integration/wut` (3), `mixin/ae2wut` (2). Stub `src/api/java/yalter/mousetweaks`.
Plus grosses classes : `GuiPatTerminal` 1 856 l., `ContainerPatEditor` 1 411, `GuiPatEditor` 1 271, `ContainerPatTerminal` 553, `ProviderPairing` 384, `RecipeWriter` 296, `EditorInventory` 296.
Ressources : 2 modèles d'item, 2 modèles de part, 7 textures, `mixins.packagedautoterminals.ae2wut.json`, 16 fichiers `.lang`. Recettes : codées en Java (`common/PatRecipes.java`), pas en JSON.

## Décisions et conventions existantes (à citer si un constat les conteste)
- `CLAUDE.md` : règles de travail 1 à 8 (vérifier avant d'écrire, serveur autorité, pas de mixin si une API suffit, intégrations optionnelles par modid, `core` testable hors Minecraft) et pièges 4.1 à 4.7 (slots `AppEngSlot`, canal réseau ≤ 20 caractères, etc.).
- `docs/DECISIONS.md` : D01–D44 et révisions R1–R5. Elles tiennent lieu d'ADR. R2 : resynchronisation complète au lieu de deltas, tant qu'aucune mesure ne l'interdit. R3 : un seul module.
- `docs/PACKAGEDAUTO-MODEL.md` : modèle de données PackagedAuto vérifié dans le bytecode.
- `docs/TESTING.md` : tests manuels en jeu, groupes A à P (O et P non joués).
- `docs/ARCHITECTURE.md`, `docs/PLAN.md`, `docs/PORTING.md`, `CHANGELOG.md`, wiki dans `docs/wiki/`.
- Absents : `GLOSSARY.md`, `CODING_STANDARDS.md`, `docs/adr/`.
- CI : `.github/workflows/build.yml` lance `./gradlew build` sur JDK 8 (aucun démarrage de serveur dédié) ; `release.yml` publie sur tag `v*`.

## Bruit à ignorer
Fichiers non suivis : `.agents/`, `LISEZMOI.md`, `logs/`, `skills-lock.json` (outils locaux, hors du mod).
