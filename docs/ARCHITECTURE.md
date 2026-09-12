# Architecture cible

## 1. Périmètre fonctionnel

### Onglet 1 — « Patterns » (lecture et écriture)

Le terminal parcourt la grille ME. Il retient chaque machine qui répond à
`instanceof IPackageProvidingMachine`. Il lit son `getPatternStack()`. Si le stack
implémente `IRecipeListItem`, il en tire la `IRecipeList`, donc la liste des `IRecipeInfo`.

Affichage : une ligne par machine, puis ses recettes, comme l'Interface Terminal d'AE2.
Le `Packager Extension` s'affiche en ligne rattachée à son Packager, jamais en machine
séparée.

Actions : ajouter une recette, supprimer une recette, modifier une recette, changer son
type, déplacer un Recipe Holder d'une machine à l'autre.

Filtres : par nom de machine, par type de recette, par item d'entrée ou de sortie.

### Onglet 2 — « Machines » (lecture seule)

Il liste les machines `IPackageCraftingMachine` du réseau, groupées par type de recette
accepté. Il indique l'état `isBusy()`.

Son intérêt principal est le **diagnostic** : il signale toute recette encodée qui n'a
aucun crafter capable de l'exécuter sur le réseau. C'est l'erreur la plus fréquente en jeu,
et aucun mod ne la détecte aujourd'hui.

### Hors périmètre v1

- Fluides et gaz (`PackagedFluidCrafting`) → v2
- Panneau d'items du réseau ME dans la même fenêtre → non, on reste sur une liste de
  patterns, comme l'Interface Terminal
- `ISettingsCloneable` → plus tard

---

## 2. Modules

Un seul module Gradle au départ (révision **R3**). Découpage par paquets :

```
fr.julien.packagedautoterminals/
 ├── api/                  interfaces stables pour les adaptateurs de types de recettes
 ├── common/               registre, passerelle vers l'API PackagedAuto, modèle de données
 ├── network/              paquets et mises à jour incrémentales
 ├── part/                 PartPackagedAutoTerminal (terminal câblé)
 ├── item/                 ItemWirelessPackagedAutoTerminal (IWirelessTermHandler)
 ├── container/            ContainerPackagedAutoTerminal + variante sans fil + éditeur
 ├── client/gui/           GUI, widgets, recherche, éditeur de recette
 └── integration/          packagedexcrafting, packagedavaritia, packagedfluidcrafting,
                           packagingprovider, ae2wut, jei
```

L'éditeur de recette est un vrai `Container`, avec des slots fantômes indexés comme ceux de
l'Encoder. C'est la contrainte **D22**, sans laquelle JEI devient inutilisable.

## 3. Flux de données

```
SERVEUR                                            CLIENT
  parcours de la grille ME
  → instantané des machines fournisseuses
  → diff avec l'instantané précédent
  → paquet delta, découpé en chunks           →    application du delta
                                                   rendu de la liste
  validation (droits AE2, énergie, distance)  ←    intention utilisateur
  écriture dans le Recipe Holder
  republication des patterns AE2
```

Quatre décisions structurantes :

1. **Le serveur est l'autorité.** Le client envoie une intention, jamais un NBT de recette.
2. **Pas de mixin tant qu'une API suffit.**
3. **Chaque intégration est optionnelle**, détectée par modid.
4. **Mises à jour incrémentales simples**, sur le modèle de `ContainerInterfaceTerminal`
   d'AE2. Le découpage en chunks n'arrive que si la mesure du lot 2 le prouve (révision
   **R2**).

---

## 4. Points d'accroche AE2UEL

| Besoin | Classe AE2UEL de référence |
|---|---|
| Terminal câblé | `AbstractPartTerminal`, `PartInterfaceTerminal` |
| Conteneur distant | `ContainerInterfaceTerminal` |
| Terminal sans fil | `IWirelessTermHandler`, `IWirelessTermRegistry`, `WirelessTerminalGuiObject`, `ToolWirelessInterfaceTerminal` |
| Hôte de terminal | `ITerminalHost` |
| Sécurité | `ISecurityGrid`, `SecurityPermissions` |
| Notification de changement | `MENetworkCraftingPatternChange` |

## 5. Précédents à étudier dans l'instance

| Mod | Ce qu'il démontre |
|---|---|
| `cell-terminal-1.6.7` | terminal tiers complet : part + item sans fil, NBT en chunks, deltas, intégration AE2WUT (`AE2WUTIntegration`, `WUTModeSwitcher`), registres de scanners par mod |
| `ae2wut-1.0.5` | absorption d'un terminal tiers dans le terminal universel, par mixins |
| `apiarist-terminal-0.3.0` | petit terminal tiers, bon exemple minimal |
