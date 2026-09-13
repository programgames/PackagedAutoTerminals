# PackagedAuto Terminals — mod Minecraft Forge 1.12.2

## Ce qu'est ce projet

Un mod qui ajoute des **terminaux AE2** pour consulter et modifier les recettes de
**PackagedAuto** à distance, depuis le réseau ME. Aujourd'hui, le joueur doit encoder ses
recettes dans un **Package Recipe Encoder**, puis marcher jusqu'à chaque machine pour y
insérer le **Package Recipe Holder**. Ce mod supprime ce déplacement.

Modèle de référence : l'**Interface Terminal** d'AE2, qui liste les ME Interfaces du réseau
et édite leurs patterns à distance.

---

## 1. Environnement technique

| Élément | Valeur | Note |
|---|---|---|
| Minecraft | **1.12.2** | |
| Loader d'exécution | Cleanroom 0.5.17-alpha | l'instance de test du joueur |
| Forge (compilation) | **14.23.5.2847** | même contrainte que le projet `actuallyplayed` : les artefacts `userdev` 2848→2860 ne sont pas publiés |
| Mappings MCP | **snapshot_20171003** | |
| ForgeGradle | **2.3.10** épinglé | |
| Gradle | **4.10.3** (wrapper) | ForgeGradle 2.3 ne va pas au-delà |
| JDK de build | **JDK 8 obligatoire** | `C:\Program Files\Eclipse Adoptium\jdk-8.0.472.8-hotspot`, déjà dans `JAVA_HOME` |
| `sourceCompatibility` | 1.8 | |

> ⚠️ **Ne jamais compiler avec le JDK 17 installé sur ce poste.** ForgeGradle 2.3 et
> Gradle 4.x échouent sur tout JDK plus récent que 8. En cas d'erreur
> `Unsupported class file major version`, vérifie `JAVA_HOME` en premier.

### Identité du mod

- **modid** : `packagedautoterminals`
- **Nom** : `PackagedAuto Terminals`
- **Package racine** : `fr.julien.packagedautoterminals`
- **Version** : SemVer. Jar nommé `packagedautoterminals-1.12.2-<version>.jar`
- **Langues** : `en_us`, `fr_fr`

### Dépendances

| Mod | Version de référence | Type |
|---|---|---|
| AE2 Unofficial Extended Life (AE2UEL) | `v0.56.5` officiel pour la compilation | **requise** |
| PackagedAuto | `1.12.2-1.0.24.73` | **requise** |
| PackagedExCrafting | `1.12.2-1.0.3.33` | optionnelle |
| PackagedAvaritia | `1.12.2-1.0.3.25` | optionnelle |
| PackagedFluidCrafting | `1.12.2-1.0.0.3` | optionnelle, **v2** |
| PackagingProvider | `1.12.2-1.0.0.2` | optionnelle |
| AE2WUT | `1.0.5` | optionnelle |
| JEI / HEI | HadEnoughItems 4.31.2 | optionnelle |

> Le joueur exécute un **fork local** d'AE2UEL (`ae2-uel-v0.56.7-10-gac98c09.dirty.jar`).
> Ce fork n'ajoute que des correctifs. **On compile contre AE2UEL officiel**, jamais contre
> le fork. Le jar produit doit fonctionner avec les deux.

### Environnement d'exécution de dev

`libs/` porte les dépendances de compilation. `run/mods` porte **uniquement** les mods
absents de `libs/`, sans quoi FML refuse de démarrer (voir section 4.1) :

```
Avaritia, Baubles, CodeChickenLib, Cucumber, ExtendedCrafting-Nomifactory-Edition,
HadEnoughItems, PackagedAvaritia, PackagedExCrafting, PackagingProvider, mixinbooter
```

> `PackagingProvider` exige `mixinbooter`. Sans lui, FML s'arrête sur
> `MissingModsException`.

### Instance de test

`H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`

---

## 2. Règles de travail

1. **Vérifier avant d'écrire.** Toute affirmation sur le comportement d'un autre mod doit
   venir d'une lecture du code, pas d'un souvenir. Consigner la preuve dans
   `docs/PACKAGEDAUTO-MODEL.md`.
2. **Le serveur est l'autorité.** Le client n'écrit jamais un NBT de recette. Il envoie une
   intention. Le serveur valide les droits AE2, l'énergie et la distance.
3. **Pas de mixin tant qu'une API suffit.** Un mixin casse à chaque mise à jour d'un autre
   mod. Les mixins sont réservés à `PackagedFluidCrafting` et à AE2WUT, et seulement si
   aucune autre voie n'existe.
4. **Chaque intégration est optionnelle.** Détection par modid au chargement. Sans l'addon,
   la fonction disparaît et rien ne plante.
5. **Incréments testables.** Chaque étape se termine par un lancement du client de dev.
6. **`core` testable hors Minecraft.** Tri, filtre, diff et validation vont dans `core`,
   avec des tests JUnit purs.
7. **Environnement de dev minimal.** AE2UEL + PackagedAuto + les trois addons + JEI.
   Jamais le pack complet.
8. **Dépendances épinglées.** Aucune version flottante dans `build.gradle`.

---

## 3. Structure du dépôt

```
PackagedAutoTerminals/
├── CLAUDE.md                     ce fichier
├── README.md
├── CHANGELOG.md                  SemVer
├── settings.gradle               un seul module au depart (voir revision R3)
├── src/main/java/fr/julien/packagedautoterminals/
├── src/main/resources/
├── libs/                         jar AE2UEL local, si CurseMaven echoue
├── docs/
│   ├── PLAN.md                   les 8 lots et leurs criteres de validation
│   ├── ARCHITECTURE.md           couches, flux de donnees, points d'entree
│   ├── PACKAGEDAUTO-MODEL.md     modele de donnees verifie, avec preuves
│   ├── DECISIONS.md              decisions validees, revisions et contraintes
│   └── TESTING.md                tests manuels en jeu, etape par etape
└── .github/workflows/build.yml
```

Le module `core` et le dossier `forge-1.12` naîtront au portage 1.16, quand on saura ce qui
est réellement commun. Voir la révision **R3** dans `docs/DECISIONS.md`.

---

## 4. Pieges rencontres, et leur cause

### 4.1 `DuplicateModsFoundException` au lancement du client de dev

ForgeGradle place les dependances `deobfProvided` sur le chemin d'execution de `runClient`.
FML les charge donc comme des mods. Mettre les **memes** jars dans `run/mods` produit :

```
Found a duplicate mod appliedenergistics2 at [.\modse2-uel-v0.56.5.jar, ...\libs\...]
```

**Regle** : tout jar present dans `libs/maven` ne doit **jamais** etre copie dans `run/mods`.

Ce piege est tombe deux fois : avec AE2UEL, puis avec JEI, le jour ou il est devenu une
dependance de compilation. La tache `checkDevMods` le detecte desormais, et `runClient` en
depend : le jeu ne peut plus demarrer avec un doublon.

### 4.2 Dependances non deobfusquees : deux pieges en serie

Symptome : `GuiPatTerminal is not abstract and does not override abstract method
drawGuiContainerBackgroundLayer`. Le code herite pourtant d'une classe AE2 qui l'implemente.

Cause : le chemin de compilation portait les noms **SRG** (`func_146976_a`), pas les noms
MCP. Deux causes distinctes se sont succede.

1. **`flatDir` ne declenche pas la deobfuscation.** Le journal affiche
   `deobfProvidedDeobfDepTask0 SKIPPED`. Correctif : un vrai depot Maven local, dans
   `libs/maven`, avec un `.pom` par artefact, et la notation `@jar` sur la dependance.
   Sans `@jar`, Gradle place le `.pom` lui-meme sur le chemin de compilation.
2. **ForgeGradle laisse le jar brut sur le chemin, avant le jar deobfusque.** `javac` prend
   la premiere correspondance, donc le brut. Correctif : le `afterEvaluate` de
   `build.gradle` filtre `sourceSets.main.compileClasspath`.

La tache `gradlew printCp` affiche le chemin de compilation. Elle a servi a trouver les deux
causes ; garde-la.

### 4.2 bis Tout emplacement d'un conteneur AE2 doit heriter d'AppEngSlot

`AEBaseContainer.addSlotToContainer` leve
`Invalid Slot [...] for AE Container instead of AppEngSlot`, et la fenetre ne s'ouvre
jamais. Ce piege est tombe **deux fois** : avec un `Slot` vanilla pour l'inventaire du
joueur, puis avec `SlotFalseCopy` de PackagedAuto dans l'editeur.

**Regle** : dans un conteneur qui herite d'`AEBaseContainer`, n'utiliser que les
emplacements d'AE2.

| Besoin | Classe AE2 |
|---|---|
| inventaire du joueur | `bindPlayerInventory(inventory, x, y)` |
| emplacement fantome | `SlotFake`, qui implemente aussi `IJEITargetSlot` |
| aperçu en lecture seule | `AppEngSlot` avec `isItemValid` et `canTakeStack` a faux |

### 4.3 Noms MCP de `snapshot_20171003`

Les mappings de 2017 ne connaissent pas les noms recents :

| Nom recent | Nom attendu ici |
|---|---|
| `CreativeTabs.createIcon()` | `getTabIconItem()` |
| `Item.setTranslationKey()` | `setUnlocalizedName()` |

### 4.4 `cannot access IMTModGuiContainer2`

`appeng.client.gui.AEBaseGui` implemente l'API de Mouse Tweaks. Sans cette interface sur le
chemin de compilation, toute classe qui en herite echoue. Le projet embarque donc le stub
`src/api/java/yalter/mousetweaks/api/IMTModGuiContainer2.java`.

### 4.5 Antislash dans une chaine Groovy

`"C:\Program Files\..."` dans `build.gradle` echoue sur `unexpected char: ''`.
Utiliser des barres obliques.

### 4.6 Le shell casse sur les commandes tres longues

Ecrire les fichiers Java par l'outil d'ecriture directe, pas par un `cat` multiligne.
