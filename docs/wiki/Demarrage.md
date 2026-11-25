# Démarrage

## 1. Fabrique le terminal

Fabrique le PackagedAuto Terminal avec un ME Terminal et un Package Recipe Holder.

## 2. Pose le terminal

Le terminal est une pièce AE2. Une pièce est un bloc fin que tu fixes sur une face d'un câble ME.

1. Vise une face d'un câble ME.
2. Fais un clic droit pour poser le terminal.
3. Alimente le réseau. Sans énergie, la pièce s'éteint et le terminal ne montre aucune machine.

## 3. Ouvre le terminal

Fais un clic droit sur la pièce. L'écran liste chaque Packager, chaque Unpackager et chaque
Packaging Provider du réseau. Le terminal ne s'ouvre que pour un joueur qui détient la permission
Build de la sécurité AE2 sur ce réseau.

Le terminal réunit en un groupe les machines qui travaillent ensemble. Un groupe est normalement
un Packager avec un Unpackager. Quatre règles construisent les groupes :

- Les machines qui portent exactement les mêmes recettes forment un groupe. Une seule recette
  commune ne suffit pas.
- Les machines qui portent le même nom de groupe forment un groupe, quelles que soient leurs
  recettes.
- Une machine vide isolée rejoint le groupe qui attend son rôle.
- Un Packager neuf et un Unpackager neuf, tous deux avec des porte-recettes vides, forment un
  groupe.

Le nom est le seul lien que tu poses toi-même. Lis [Dépannage](Depannage) pour le cas d'un groupe
qui se scinde en deux.

## 4. Écris une recette

1. Fais un clic gauche sur une ligne de groupe. L'éditeur s'ouvre sur une nouvelle recette.
2. Choisis le type de recette avec les flèches à côté de son nom.
3. Remplis la grille. Prends un objet en main, puis fais un clic gauche sur une case.
4. Appuie sur Save.

Tu peux aussi glisser un objet depuis JEI sur une case, au lieu de l'étape 3.

La recette atteint le Packager et l'Unpackager dans le même geste. PackagedAuto exige la même
recette des deux côtés. Si tu n'écris qu'un seul côté, l'automatisation casse sans message.

Pour modifier une recette qui existe déjà, déplie son groupe avec le chevron, puis fais un clic
droit sur la ligne de la recette.

## Les gestes de la liste

| Geste | Effet |
|---|---|
| Clic gauche sur une ligne de groupe | Ouvre l'éditeur sur une nouvelle recette |
| Clic gauche sur le chevron d'une ligne de groupe | Plie ou déplie les recettes du groupe |
| Clic gauche sur le bouton d'une ligne de groupe | Prend des porte-recettes vierges sur le réseau, ou copie de l'autre côté les recettes qui manquent à un côté |
| Maj + clic gauche sur une ligne de groupe, deux fois | Renvoie au réseau les porte-recettes du groupe |
| Clic droit sur une ligne de recette, ou clic gauche sur son crayon | Modifie cette recette |
| Clic gauche sur la croix d'une ligne de recette, deux fois | Retire cette recette du groupe |
| Maj + clic droit sur une ligne de recette, deux fois | Retire cette recette du groupe |
| Clic gauche sur l'épingle d'une ligne de groupe | Marque les machines du groupe dans le monde |

Le bouton d'une ligne de groupe n'apparaît que quand le groupe en a besoin. Une flèche qui entre
dans une boîte signale qu'une machine ne porte pas de porte-recettes. Deux flèches qui tournent
signalent qu'une recette n'est que d'un côté. Le bouton ne fait qu'ajouter : il ne retire jamais un
porte-recettes ni une recette.

Un retrait demande deux clics sur la même ligne, dans les trois secondes. Le premier clic ne
fait qu'afficher un avertissement.

## Le terminal sans fil

Fabrique le Wireless PackagedAuto Terminal avec un PackagedAuto Terminal et un AE2 Wireless
Terminal.

1. Lie le terminal sans fil dans un terminal de sécurité ME.
2. Fais un clic droit avec, n'importe où dans la portée. Il fonctionne dans l'une ou l'autre main.

Le terminal sans fil puise dans sa propre réserve d'énergie, comme tout terminal sans fil d'AE2.
Hors de portée ou sans énergie, l'écran se ferme. Le terminal ne montre pas un réseau qu'il
n'atteint plus.

## Pour aller plus loin

Pour une page de description dans le jeu, appuie sur la touche d'utilisation de JEI sur l'un des
deux objets. Le wiki continue avec [L'éditeur](L-editeur).
