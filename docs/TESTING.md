# Tests manuels en jeu

> Le mod touche à un réseau ME et à des recettes encodées. Aucun test automatique ne couvre
> cela. Tout le code écrit pendant la nuit du 12 au 13 septembre 2026 **n'a jamais tourné
> en jeu**. Cette liste existe pour le vérifier en une seule session.
>
> Coche au fur et à mesure. Au premier échec, note le message du journal
> (`run/logs/latest.log`) : il porte presque toujours la cause exacte.

## Préparer

1. Vérifie que le **Gradle JVM d'IntelliJ pointe sur le JDK 8**.
2. Lance la configuration **Gradle runClient**.
3. Monde créatif superplat, avec : contrôleur ME, Creative Energy Cell, câble, Packager,
   Unpackager, Package Recipe Encoder, quelques Package Recipe Holders, un Package Crafter,
   un Ultimate Crafter, et des objets de base.

> ⚠️ Ne copie jamais dans `run/mods` un jar déjà présent dans `libs/maven`. FML refuse de
> démarrer. Voir CLAUDE.md, section 4.1.

---

## T1 — Le mod se charge

- [ ] Le menu **Mods** liste `PackagedAuto Terminals`.
- [ ] Le journal affiche `Types de recettes enregistres : 10`.
- [ ] Le journal affiche `Traductions chargees`, et **pas** `NE sont PAS chargees`.

## T2 — Fabrication *(nouveau, lot 7d)*

- [ ] Dans JEI, la recette du **Terminal PackagedAuto** apparaît : un terminal ME plus un
      Package Recipe Holder.
- [ ] La fabrication fonctionne en table de craft.

> Si la recette est absente, cherche dans le journal `Recette du terminal non enregistree`.

## T3 — Pose et ouverture

- [ ] Le terminal se pose sur un câble ME.
- [ ] Le clic droit ouvre la fenêtre.
- [ ] La liste montre le Packager, son état, puis ses recettes.

## T4 — Recherche *(nouveau, lot 4c)*

- [ ] Le champ de recherche apparaît en haut à droite de la fenêtre.
- [ ] Taper le nom d'une machine filtre la liste.
- [ ] Taper le nom d'un objet **produit** garde la machine ET sa recette.
- [ ] Taper le nom d'un objet **consommé** fonctionne aussi.
- [ ] **Échap** ferme la fenêtre même quand le champ a le focus.
- [ ] La touche d'inventaire ferme aussi la fenêtre.

## T5 — Suppression

- [ ] **Maj + clic droit** sur une recette la supprime.
- [ ] Le terminal de craft d'AE2 ne propose plus l'objet correspondant.

## T6 — Édition

- [ ] **Clic droit** sur une recette ouvre l'éditeur, rempli.
- [ ] Les libellés ne se chevauchent pas.
- [ ] L'icône du type apparaît entre les flèches `<` et `>`.
- [ ] Les flèches changent le type, et les cases actives suivent.
- [ ] **Enregistrer** ramène au terminal, et AE2 voit la nouvelle recette.

## T7 — Quantités *(nouveau, lot 3d)*

- [ ] La molette au-dessus d'une case occupée change la quantité.
- [ ] **Maj + molette** avance de dix.
- [ ] **Ctrl + molette** avance de soixante-quatre.
- [ ] La quantité ne descend jamais sous un, et ne dépasse pas 4096.
- [ ] La molette sur une case grisée ne fait rien.

## T8 — Création *(nouveau, lot 3c)*

- [ ] **Clic gauche** sur une machine qui porte un porte-recettes ouvre un éditeur vide.
- [ ] Le type proposé est **Crafting**.
- [ ] Après enregistrement, la recette s'ajoute à la suite des autres.
- [ ] Sur une machine **sans** porte-recettes, le clic gauche en prend un sur le réseau ME,
      puis ouvre l'éditeur.
- [ ] Sans porte-recettes vierge sur le réseau, le message
      « Aucun porte-recettes vierge sur le réseau » s'affiche.

## T9 — Déplacer un porte-recettes *(nouveau, lot 3e)*

- [ ] **Maj + clic gauche** sur une machine renvoie son porte-recettes au réseau.
- [ ] Le porte-recettes apparaît dans le terminal ME, avec ses recettes.
- [ ] La machine n'en porte plus, et AE2 ne propose plus ses recettes.

## T10 — Transfert depuis JEI *(nouveau, lot 4a)*

- [ ] Dans l'éditeur, ouvre une recette dans JEI, puis clique le bouton **+**.
- [ ] La grille se remplit, et le type bascule sur celui qui convient.
- [ ] Sur une catégorie qu'aucun type n'accepte, JEI affiche
      « Aucun type PackagedAuto n'accepte cette catégorie ».

## T11 — Onglet Machines *(nouveau, lot 5)*

- [ ] Le bouton en haut à gauche bascule entre **Patterns** et **Machines**.
- [ ] L'onglet Machines liste les crafters, avec **prête** ou **occupée**.
- [ ] Encode une recette **Ultimate** sans poser d'Ultimate Crafter : l'onglet Machines
      affiche `Ultimate : aucun crafter sur ce réseau`, en rouge.
- [ ] Pose l'Ultimate Crafter : l'avertissement disparaît.
- [ ] Une recette de type **Processing** ne déclenche aucun avertissement : ce type n'exige
      aucune machine reconnue.

## T12 — Configuration *(nouveau, lot 7c)*

- [ ] Le fichier `run/config/packagedautoterminals.cfg` existe.
- [ ] `machinesTab = false` fait disparaître le bouton d'onglet, et le titre revient.
- [ ] `refreshTicks = 100` ralentit visiblement la mise à jour de la liste.

## T13 — Cas limites

| Cas | Attendu |
|---|---|
| Packager sans porte-recettes | ligne « aucun porte-recettes » |
| Packager sans énergie | ligne « inactive » |
| Unpackager et Packaging Provider | ils apparaissent aussi |
| Deux réseaux distincts | seul le réseau du terminal apparaît |
| Casser la machine pendant l'édition | l'enregistrement échoue sans planter |
| Plus de six rangées | l'ascenseur fonctionne |

## T14 — Mesure de la révision R2

- [ ] Sur un réseau chargé, note la **taille du paquet** affichée à droite du libellé
      « Inventaire ».
- [ ] Au-delà de 30 000 octets, il faudra découper les paquets. En dessous, la révision R2
      tient, et le découpage reste inutile.

## T15 — Instance réelle

- [ ] Copie `build/libs/packagedautoterminals-1.12.2-0.1.0.jar` dans
      `H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`.
- [ ] L'instance démarre, et le terminal fonctionne avec ton fork d'AE2UEL.


---

## Tests de la branche `lot6-sans-fil`

> Ces tests ne valent que sur la branche `lot6-sans-fil`. La branche `master` n'a pas le
> terminal sans fil.

### T16 — Liaison et ouverture

- [ ] Fabrique le **Terminal PackagedAuto sans fil** : le terminal câblé plus le terminal
      sans fil d'AE2.
- [ ] Lie-le à un réseau avec le **Wireless Access Point**, comme un terminal d'AE2.
- [ ] Sans liaison, le clic droit affiche « Ce terminal n'est lié à aucun réseau ».
- [ ] Sans énergie, il affiche « Ce terminal n'a plus d'énergie ».
- [ ] Une fois lié et chargé, le clic droit ouvre le terminal.

### T17 — Portée

- [ ] Éloigne-toi jusqu'à sortir de portée : la fenêtre se referme seule.
- [ ] Ouvre l'éditeur, puis éloigne-toi : il se referme aussi.

### T18 — Parité avec le terminal câblé

- [ ] La liste, la recherche, l'onglet Machines et l'édition se comportent à l'identique.
- [ ] La batterie descend lentement pendant que la fenêtre reste ouverte.

> **Le plus grand risque de cette branche** : le terminal câblé a été refondu pour partager
> son code avec le sans-fil. Déroule donc **aussi** les tests T3 à T13 sur cette branche,
> pour vérifier que rien n'a régressé.
