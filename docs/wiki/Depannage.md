# Dépannage

## Les refus, et ce que chacun veut dire

| Message | Sens |
|---|---|
| Insère d'abord un porte-recettes dans cette machine | La machine ne porte pas de porte-recettes. Le terminal n'en retire jamais un du stockage de lui-même. Ce choix reste le tien. |
| Ce groupe est plein : 20 recettes au maximum | Le groupe porte déjà le nombre maximum de recettes. La limite appartient au Package Recipe Encoder, pas à ce mod. |
| Ce groupe a changé : rouvre la recette | La recette que tu as ouverte n'est plus dans le groupe. Un autre joueur, ou un changement sur une machine, l'a retirée. |
| Ce rapport ne tombe pas juste sur tous les slots | Keep ratio a refusé le changement et n'a rien changé. Aucune recette ne reste à moitié mise à l'échelle. |
| Ce rapport dépasserait %d dans un slot | Keep ratio a refusé le changement, car une case passerait au-delà de son propre maximum. |
| Une recette de craft prend un objet par case | PackagedAuto force le compte à un sur une recette de craft. |
| Recette non enregistrée. Clique à nouveau pour la quitter. | Le premier clic t'avertit. Le second clic quitte la recette. Tu ne perds rien sans message. |
| Appliqué à 2, 1 sans porte-recettes | Le terminal a écrit la recette, mais une machine du groupe ne porte pas de porte-recettes. |
| Aucune écriture | Aucune machine du groupe n'a pris la recette. |

## Le terminal se ferme tout seul

Le terminal sans fil se ferme quand tu quittes la portée, ou quand sa réserve se vide. Le terminal
câblé se ferme quand il perd son réseau. L'éditeur suit les mêmes règles. Il se ferme. Il ne montre
pas une recette qu'il n'écrit plus.

## L'écran ne tient pas dans ta fenêtre

Les deux écrans abaissent l'échelle d'interface du jeu tant qu'ils sont ouverts. Ils rendent
l'ancienne valeur à la fermeture. Ils ne changent la valeur qu'en mémoire, et ils ne réécrivent
jamais ton fichier d'options.

L'éditeur fait 338 pixels de haut. Minecraft ne garantit qu'une fenêtre de 320 sur 240 pixels.

## Deux machines qui appartiennent à un seul groupe

Le terminal réunit deux machines qui portent exactement les mêmes recettes. Une seule recette
commune ne suffit pas.

Si un groupe se scinde en deux, un côté a une recette de moins. Donne le même nom aux deux côtés :

1. Fais un clic gauche sur l'une des deux lignes. L'éditeur s'ouvre.
2. Tape un nom dans le champ de nom, en haut de l'éditeur.
3. Fais de même sur l'autre ligne, avec le même nom.

Le terminal réunit alors les deux lignes par le nom, quelles que soient leurs recettes. La
sauvegarde suivante atteint toutes les machines du groupe.

## Une recette ne tourne jamais

Ouvre l'onglet Machines. Si la recette apparaît dans le rapport en haut de l'onglet, le réseau ne
possède aucun crafter pour ce type de recette. Lis [Le diagnostic](Le-diagnostic) pour ce rapport.

## Signaler un bogue

1. Ouvre une issue sur le dépôt.
2. Donne les versions de Minecraft, Forge, AE2UEL et PackagedAuto, et les addons que tu utilises.
3. Donne les étapes qui reproduisent le problème.
4. Joins une capture d'écran.
