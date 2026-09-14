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

## Avancement

| Groupe | État |
|---|---|
| A — Le socle | validé |
| B — Lecture et recherche | validé |
| C — L'œil et le nom | validé |
| D — L'éditeur fusionné | validé |
| E — La paire, et ses accidents | validé |
| F — L'onglet Machines | validé |
| G — Le sans-fil | validé |
| H — Réglages et instance réelle | H1 validé, H2 en attente |
| I — Pliage et machine d'exécution | 🆕 jamais testé |
| J — Apparence de la part et des items | 🆕 jamais testé |
| K — Terminal universel sans fil (AE2WUT) | 🆕 jamais testé |
| L — Touche d'ouverture | 🆕 jamais testé |
| M — Confort de lecture et d'édition | 🆕 jamais testé |

Correctifs nés de ces essais : cadre du nom porté à 16 pixels, boutons calés sous
l'aperçu, message de retour unique et coupé au cadre, regroupement par le nom donné
par le joueur, largeurs de texte calculées au lieu d'être écrites en dur, et choix du
type de recette le plus étroit au transfert JEI (D32), et branchement des contrôles de
portée, de liaison et d'énergie du sans-fil, qui n'étaient appelés par personne.

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

## I — Pliage et machine d'exécution 🆕

### I1 — Plié par défaut
Ouvre le terminal.
→ Chaque groupe tient sur **une** ligne. Aucune recette n'est visible. Un chevron pointe
vers la droite, à gauche de l'icône.

### I2 — Déplier
Clique le chevron.
→ Le chevron pointe vers le bas, et les recettes du groupe apparaissent.

### I3 — La création reste intacte
Clique la ligne du groupe **ailleurs que sur le chevron**.
→ L'éditeur s'ouvre sur une recette neuve, comme au test D5.

### I4 — Groupe sans recette
Regarde une paire vide.
→ Aucun chevron. La place reste vide, et l'icône ne bouge pas.

### I5 — La recherche déplie
Tape le nom d'un objet produit.
→ Le groupe qui porte la recette s'ouvre seul. Les autres restent pliés.

### I6 — Le champ vidé replie
Efface la recherche.
→ Tout se replie, sauf les groupes que tu avais ouverts au chevron.

### I7 — Mémoire de session
Ferme le terminal, puis rouvre-le.
→ Tout est plié de nouveau.

### I8 — Machine dans la liste
Regarde une ligne de recette de type Elite.
→ À droite, l'icône de l'**Elite Package Crafter** remplace le mot « Elite ». L'infobulle
dit « Fabriquée par : Elite Package Crafter ».

### I9 — Machine absente
Retire l'Elite Package Crafter du réseau.
→ L'icône passe en sombre, et l'infobulle vire au rouge : « absent du réseau ».

### I10 — Type sans machine
Regarde une recette **Processing**.
→ Aucune icône à droite. Le mot « Processing » reste, comme avant.

### I11 — Machine dans l'éditeur
Ouvre une recette Elite dans l'éditeur.
→ Sous l'icône du type, l'icône de l'Elite Package Crafter apparaît. Le survol donne son
nom.

### I12 — Le type change, la machine suit
Dans l'éditeur, change le type avec `<` et `>`.
→ L'icône de la machine change à chaque type.

---

## J — Apparence de la part et des items 🆕

### J1 — La part posée
Pose le terminal sur un câble ME.
→ L'écran est **violet fluix**, avec un colis blanc éclatant au milieu. Il ressemble au
Pattern Terminal d'AE2, posé à côté.

### J2 — La couleur suit le réseau
Peins le câble en rouge avec un Color Applicator.
→ L'écran passe au rouge. Les anciennes textures gardaient leur turquoise.

### J3 — L'item dans l'inventaire
Regarde le terminal dans ta barre d'action.
→ Il porte le boîtier d'AE2, et son écran violet. Il n'est **pas** blanc : ce serait le
signe que le gestionnaire de couleur manque.

### J4 — Le terminal sans fil
Regarde l'item sans fil.
→ Antenne rose en haut à gauche, écran violet, colis blanc. Même famille que le Wireless
Pattern Terminal d'AE2.

### J5 — Hors tension
Coupe le courant du réseau.
→ L'écran s'éteint, comme celui des terminaux d'AE2.

---

## K — Terminal universel sans fil (AE2WUT) 🆕

> `run/mods` porte désormais `ae2wut-1.0.5.jar`. Sans lui, ce groupe entier se saute, et le
> reste du mod doit fonctionner comme avant.

### K1 — Sans AE2WUT
Retire `ae2wut-1.0.5.jar` de `run/mods`, puis lance le client.
→ Le jeu démarre. Le journal ne parle pas d'AE2WUT. Aucun mixin ne s'applique.

### K2 — Avec AE2WUT
Remets le jar, puis relance.
→ Le journal porte `AE2WUT detecte, mode 41`. Le jeu démarre.

### K3 — La recette d'assemblage
Dans JEI, cherche le **Wireless Universal Terminal**.
→ Une recette sans forme associe le terminal universel et notre **Terminal PackagedAuto
sans fil**.

### K4 — Assembler
Fabrique cette recette.
→ Le terminal universel sort de la table. Son infobulle, touche Maj enfoncée, liste
« Terminal PackagedAuto sans fil ».

### K5 — La molette
Maj + molette sur le terminal universel.
→ Le nom affiché passe par tous les modes absorbés, dont le nôtre.

### K6 — Ouvrir
Règle la molette sur notre mode, lie le terminal à un point d'accès, puis clic droit.
→ Notre fenêtre s'ouvre, avec les recettes du réseau.

### K7 — Non lié
Sur un terminal universel jamais lié, clic droit dans notre mode.
→ Message « Ce terminal n'est lié à aucun réseau ». Aucune fenêtre.

### K8 — Hors de portée
Éloigne-toi du point d'accès, fenêtre ouverte.
→ Elle se referme seule, comme avec notre propre terminal sans fil.

### K9 — Sans énergie
Vide le terminal universel.
→ Message « Pas d'énergie ». Le terminal universel se décharge bien, et non le nôtre.

### K10 — Les autres modes
Repasse sur le mode ME, puis sur le mode Pattern d'AE2.
→ Les fenêtres d'AE2 s'ouvrent normalement. Notre écouteur ne les intercepte pas.

### K12 — L'image du terminal universel
Règle le terminal universel sur notre mode, puis regarde-le dans la barre d'action et en
main.
→ Il porte **notre** image : antenne rose, écran violet, colis blanc. Aucun damier violet et
noir, aucun nom de modèle en surimpression.

### K13 — Les autres modes gardent leur image
Passe à la molette sur le mode ME, puis sur le mode Pattern.
→ Chaque mode retrouve l'image du terminal d'AE2 correspondant.

### K11 — Le mode réglable
Dans `run/config/packagedautoterminals.cfg`, passe `wutModeId` à une autre valeur, puis
relance.
→ Un terminal universel déjà assemblé perd notre mode ; il faut le réassembler. Aucun
plantage.

---

## L — Touche d'ouverture 🆕

### L1 — La touche existe, et n'est liée à rien
Ouvre **Options**, puis **Commandes**.
→ Une catégorie « PackagedAuto Terminals » porte « Ouvrir le terminal sans fil ». Aucune
touche ne lui est assignée.

### L2 — Aucun conflit
Regarde les quatre commandes d'AE2 et celle de Cell Terminal.
→ Aucune ne s'affiche en rouge.

### L3 — Lier
Assigne une touche libre, par exemple `K`.

### L4 — Sans terminal
Vide ton inventaire, puis appuie sur la touche.
→ « Aucun terminal PackagedAuto sans fil dans ton inventaire. »

### L5 — Depuis l'inventaire
Range le terminal sans fil lié dans une case de l'inventaire, hors de la main, puis appuie.
→ La fenêtre s'ouvre.

### L6 — Depuis la main gauche
Place le terminal en main gauche, puis appuie.
→ La fenêtre s'ouvre.

### L7 — Non lié
Avec un terminal jamais lié, appuie.
→ « Ce terminal n'est lié à aucun réseau ». La recherche s'arrête là, et ne passe pas au
suivant.

### L8 — Le terminal universel
Range un terminal universel qui a absorbé notre mode, **réglé sur un autre mode**, puis
appuie.
→ Notre fenêtre s'ouvre. Le terminal universel bascule sur notre mode.

### L9 — La grille de craft survit
Avant L8, règle le terminal universel sur le mode Crafting d'AE2 et remplis sa grille.
Appuie sur notre touche, puis reviens au mode Crafting à la molette.
→ La grille est intacte. C'est l'appel à `nbtChangeB` qui la met en réserve.

### L10 — Priorité
Porte les deux objets à la fois : notre terminal sans fil, et un terminal universel.
→ Notre terminal passe en premier.

---

## M — Confort de lecture et d'édition 🆕

### M1 — Zébrage
Ouvre le terminal, avec au moins quatre lignes.
→ Une rangée sur deux porte un fond très légèrement plus sombre. Le motif ne saute pas
quand tu fais défiler la liste.

### M2 — Surbrillance
Promène la souris sur la liste.
→ La rangée sous le curseur se teinte de bleu clair. Le texte et les icônes restent
lisibles.

### M3 — La croix de la recherche
Tape un texte dans le champ.
→ Une croix apparaît à droite du champ. Elle disparaît quand le champ est vide.

### M4 — Vider d'un clic
Clique la croix.
→ Le champ se vide, il garde le focus, et la liste complète revient.

### M5 — Le compteur
Tape le nom d'un objet produit.
→ À droite de la ligne de résumé, « 2 résultats » s'affiche. Le compteur disparaît quand le
champ est vide.

### M6 — Le point de l'onglet
Dans l'éditeur, change un ingrédient.
→ Un point rouge apparaît dans l'angle de l'onglet ouvert. **Enregistrer** le fait
disparaître.

### M7 — Entrée enregistre
Modifie une case, puis appuie sur **Entrée**.
→ Message vert « Appliqué à 2 machines ». Même effet que le bouton.

### M8 — Entrée sans recette valide
Vide la grille, puis appuie sur **Entrée**.
→ Rien ne part. Le bouton Enregistrer est éteint, et la touche respecte cet état.

### M9 — Échap revient au terminal
Dans l'éditeur, appuie sur **Échap**.
→ La liste du terminal revient. Un second Échap ferme le terminal.

### M10 — Échap depuis le champ du nom
Clique dans le champ du nom, puis appuie sur **Échap**.
→ La liste revient aussi. Le joueur n'est jamais piégé dans le champ.

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
