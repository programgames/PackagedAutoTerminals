# Protocole de test interactif

> **Règle du jeu.** Un test à la fois, dans l'ordre. Tu réponds **OK**, ou tu décris ce que
> tu vois. Au premier échec, on corrige avant d'avancer : un défaut en masque souvent un
> autre.
>
> **En cas d'échec** : une capture suffit. Le journal, je le lis moi-même dans
> `run/logs/latest.log`.
>
> **État du code** : tout ce qui porte 🆕 n'a **jamais** tourné en jeu.

---

## Préparation

### P1 — Lancer

```bash
cd /c/Users/Julien/Desktop/PackagedAutoTerminals && ./gradlew runClient
```

### P2 — Le banc d'essai

Dans un monde créatif superplat, monte ceci :

| Élément | Rôle dans les tests |
|---|---|
| Contrôleur ME, Creative Energy Cell, câbles | le réseau |
| **Packager** et **Unpackager**, reliés | la paire de référence |
| **Package Recipe Encoder** | encoder à la main, pour comparer |
| 4 **Package Recipe Holders** | deux pour la paire, deux en réserve |
| **Package Crafter** et **Ultimate Crafter** | l'onglet Machines et le diagnostic |
| **Positioned Package Distributor** | vérifier la section « Aiguilleurs » |
| Un terminal ME classique | vérifier qu'AE2 voit bien nos recettes |
| Fer, or, redstone, diamants | de quoi encoder |

Garde un **second Unpackager** de côté : il servira au test du rôle manquant.

---

## A — Le socle

### A1 — Chargement
Ouvre le menu **Mods**.
→ `PackagedAuto Terminals` figure dans la liste.

### A2 — Fabrication
Cherche `Terminal PackagedAuto` dans JEI, puis fabrique-le.
→ La recette demande un terminal ME et un Package Recipe Holder.

### A3 — Pose
Pose le terminal sur un câble, puis clic droit.
→ La fenêtre s'ouvre. Largeur **256**, aucun texte coupé.

---

## B — Lecture et recherche

### B1 — La paire
Encode la même recette dans les deux porte-recettes avec l'Encoder, place-les dans le
Packager et l'Unpackager, puis ouvre le terminal.
→ **Une seule ligne** d'en-tête, « Paire Packager/Unpackager », suivie d'**une seule**
ligne de recette.

### B2 — Le résumé
Regarde la ligne au-dessus de l'inventaire.
→ « 2 machines · 1 recette ». Le survol donne la taille du paquet.

### B3 — Recherche libre
Tape le nom de l'objet produit, puis `zzz`.
→ Le filtre suit, et le message de liste vide tient dans le cadre.

### B4 — Préfixe de mod 🆕
Tape `@minecraft`.
→ Seules les recettes dont un objet vient de Minecraft restent.

### B5 — Préfixe de type 🆕
Tape `#processing`, puis `#ultimate`.
→ Le filtre suit le type de la recette.

### B6 — Sortie de la recherche
Échap, puis rouvre et appuie sur **E** avec le champ actif.
→ La fenêtre se ferme dans les deux cas.

---

## C — L'œil et le nom

### C1 — Repérage 🆕
Clique l'**œil**, à droite de la ligne de groupe.
→ La fenêtre se ferme, et les deux blocs clignotent en cyan cinq secondes, même derrière un
mur.

### C2 — Nommer 🆕
Clic droit sur la recette, tape `Fer` dans le champ du haut, **Entrée**.
→ Message vert « Groupe renommé ».

### C3 — Le nom vit
**Retour**, puis regarde la liste.
→ La ligne affiche « Fer » à la place de « Paire Packager/Unpackager ».

---

## D — L'éditeur fusionné 🆕

### D1 — Ouverture
Clic droit sur la recette.
→ La fenêtre reprend la disposition de l'Encoder. L'onglet de la recette porte un **cadre
vert**.

### D2 — Les onglets
Regarde la rangée du haut.
→ Une case par recette, montrant l'objet produit, puis **une case vide**.

### D3 — Modifier
Change un ingrédient, puis **Enregistrer**.
→ Message vert « Appliqué à 2 machines ». La fenêtre **reste ouverte**.

### D4 — Pas de doublon
Appuie une seconde fois sur **Enregistrer**.
→ Toujours une seule recette dans la rangée d'onglets.

### D5 — Créer
Clique la **case vide**, remplis la grille, **Enregistrer**.
→ Un nouvel onglet apparaît, et le terminal comptera deux recettes.

### D6 — Quitter sans enregistrer
Modifie une case, puis clique un autre onglet.
→ Message rouge « Recette non enregistrée ». Un second clic bascule.

### D7 — Quantité à la molette
Molette sur une case remplie, puis Maj et Ctrl.
→ Pas de 1, de 10, puis de 64. Jamais sous 1.

### D8 — Quantité au clavier 🆕
**Clic du milieu** sur une case remplie, tape `128`, **Entrée**.
→ La case affiche 128. Échap ailleurs annule sans rien changer.

### D9 — Type de recette
Les flèches `<` et `>` sous le nom du type.
→ Le nom, l'icône et les cases actives changent ensemble.

### D10 — Vider et supprimer
**Vider**, puis **Supprimer**.
→ La grille se vide sans rien écrire ; la suppression retire la recette des deux machines.

### D11 — Transfert JEI
Ouvre une recette dans JEI, clique son bouton **+**.
→ La grille se remplit, et le type bascule sur celui qui convient.

---

## E — La paire, et ses accidents 🆕

### E1 — Recette d'un seul côté
Avec l'Encoder, retire la recette du **Packager** seulement.
→ Dans le terminal, la ligne passe en **rouge**, et l'infobulle dit d'encoder aussi dans un
Packager.

### E2 — Réparer
Clic droit sur la ligne rouge, puis **Enregistrer** sans rien changer.
→ « Appliqué à 2 machines », et la ligne redevient noire.

### E3 — Machine sans porte-recettes
Retire le porte-recettes du Packager, puis clique sa ligne dans le terminal.
→ Message « Insère d'abord un porte-recettes ». **Rien ne sort du réseau ME.**

### E4 — Paire neuve
Vide les deux porte-recettes.
→ Une seule ligne, « aucune recette », et **aucune** phrase rouge.

### E5 — Rôle manquant
Pose le second Unpackager, relié, sans porte-recettes.
→ Il apparaît sur sa propre ligne : le terminal ne devine pas quand deux machines peuvent
prétendre au même rôle.

### E6 — Retrait groupé
**Maj + clic gauche** sur la ligne de la paire.
→ Les deux porte-recettes reviennent dans le terminal ME, avec leurs recettes.

---

## F — L'onglet Machines 🆕

### F1 — Bascule
Clique le bouton **Patterns** en haut à gauche.
→ Il passe à **Machines**.

### F2 — Les deux sections
→ **Crafters** d'abord, puis **Aiguilleurs** avec le Distributor.

### F3 — État
→ Chaque machine affiche « prête » ou « occupée ».

### F4 — Recette orpheline
Encode une recette **Ultimate** sans poser d'Ultimate Crafter.
→ Un avertissement rouge en tête de liste. Pose le crafter : il disparaît.

### F5 — Pas de faux positif
Vérifie qu'une recette **Processing** ne déclenche aucun avertissement.
→ Ce type n'exige aucune machine reconnue.

---

## G — Le sans-fil 🆕

> La partie la plus risquée : elle n'a jamais tourné, et la refonte du conteneur touche
> aussi le terminal câblé. Si A à F cassent, dis-le avant d'attaquer G.

### G1 — Fabrication
→ Le **Terminal PackagedAuto sans fil** se fabrique avec le nôtre et le terminal sans fil
d'AE2.

### G2 — Sans liaison
Clic droit sans l'avoir lié.
→ « Ce terminal n'est lié à aucun réseau ».

### G3 — Lié
Lie-le à un Wireless Access Point, puis clic droit.
→ Le terminal s'ouvre, identique au câblé.

### G4 — Hors de portée
Éloigne-toi.
→ La fenêtre se referme seule. Même chose depuis l'éditeur.

---

## H — Réglages et instance réelle

### H1 — Configuration
Dans `run/config/packagedautoterminals.cfg`, passe `machinesTab` à `false`.
→ Le bouton d'onglet disparaît, et le titre revient.

### H2 — Instance réelle
Copie `build/libs/packagedautoterminals-1.12.2-0.1.0.jar` dans
`H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`.
→ L'instance démarre, et le terminal fonctionne avec ton fork d'AE2UEL.

---

## Là où je m'attends à des ennuis

Par probabilité décroissante :

1. **Les onglets de l'éditeur** (D2) : ils passent par de vrais emplacements, et la
   synchronisation de leurs icônes n'a jamais été observée.
2. **La boîte de quantité** (D8) : sa position vient de l'emplacement cliqué, sans garde
   contre le bord de l'écran.
3. **Le sans-fil** (G) : la refonte du conteneur n'a aucun essai derrière elle.
4. **Le transfert JEI** (D11) : il dépend d'une méthode de PackagedAuto jamais exercée.
5. **Le repérage** (C1) : le rendu dans le monde touche à l'état d'OpenGL.
