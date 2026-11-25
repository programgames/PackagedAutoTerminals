# Dépannage

## Les refus, et ce que chacun veut dire

| Message | Sens |
|---|---|
| Insère d'abord un porte-recettes dans cette machine | La machine ne porte pas de porte-recettes, et le serveur règle `pullHolderFromNetwork` sur `false`. Avec ce réglage, le terminal ne retire jamais un porte-recettes du stockage de lui-même. Ce choix reste le tien. |
| Aucun porte-recettes vierge sur le réseau | La machine ne porte pas de porte-recettes. `pullHolderFromNetwork` vaut `true`, la valeur par défaut : le terminal prend donc un porte-recettes vierge sur le réseau ME à la sauvegarde. Le réseau n'en contient aucun. |
| Tu n’as pas le droit de construire sur ce réseau | La sécurité AE2 du réseau te refuse la permission Build. Le terminal ne s'ouvre pas sans elle. |
| Clique à nouveau pour supprimer cette recette. | Le premier clic gauche sur la croix d'une ligne de recette t'avertit. Un second clic sur la même recette, dans les trois secondes, la supprime. |
| Maj + clic gauche à nouveau pour renvoyer les porte-recettes au réseau. | Le premier Maj + clic gauche sur une ligne de groupe t'avertit. Un second sur la même ligne, dans les trois secondes, renvoie au réseau les porte-recettes du groupe, avec leurs recettes. |
| Ce groupe est plein : 20 recettes au maximum | Le groupe porte déjà le nombre maximum de recettes. La limite appartient au Package Recipe Encoder, pas à ce mod. |
| Ce groupe a changé : rouvre la recette | La recette que tu as ouverte n'est plus dans le groupe. Un autre joueur, ou un changement sur une machine, l'a retirée. |
| Ce rapport ne tombe pas juste sur tous les slots | Keep ratio a refusé le changement et n'a rien changé. Aucune recette ne reste à moitié mise à l'échelle. |
| Ce rapport dépasserait %d dans un slot | Keep ratio a refusé le changement, car une case passerait au-delà de son propre maximum. |
| Une recette de craft prend un objet par case | PackagedAuto force le compte à un sur une recette de craft. |
| Recette non enregistrée. Clique à nouveau pour la quitter. | Le premier clic t'avertit. Le second clic quitte la recette. Tu ne perds rien sans message. |
| Appliqué à 2, 1 sans porte-recettes | Le terminal a écrit la recette, mais une machine du groupe ne porte pas de porte-recettes. Le terminal n'en a pris aucun, car `pullHolderFromNetwork` vaut `false` ou le réseau ne contient aucun porte-recettes vierge. |
| Appliqué à 2, 1 porte-recettes pris sur le réseau | Ce n'est pas un refus. Le terminal a écrit la recette, et il a pris un porte-recettes vierge sur le réseau ME pour une machine qui n'en avait pas. Règle `pullHolderFromNetwork` sur `false` pour garder les porte-recettes sous ta propre main. |
| Aucune écriture | Aucune machine du groupe n'a pris la recette. |

## Le terminal se ferme tout seul

Le terminal sans fil se ferme quand tu quittes la portée, ou quand sa réserve se vide. Le terminal
câblé se ferme quand il perd son réseau. Les deux se ferment quand tu perds la permission Build sur
le réseau. L'éditeur suit les mêmes règles. Il se ferme. Il ne montre pas une recette qu'il n'écrit
plus.

## L'écran ne tient pas dans ta fenêtre

Les deux écrans abaissent l'échelle d'interface du jeu tant qu'ils sont ouverts. Ils rendent
l'ancienne valeur à la fermeture. Ils ne changent la valeur qu'en mémoire, et ils ne réécrivent
jamais ton fichier d'options.

Le terminal fait 331 pixels de haut et montre dix lignes : une fenêtre de 1920 sur 1080 garde donc
l'échelle d'interface 3. L'éditeur fait 338 pixels de haut. Minecraft ne garantit qu'une fenêtre
de 320 sur 240 pixels.

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

Ouvre l'onglet Machines. Si le type de la recette apparaît en orange dans le rapport en haut de
l'onglet, le réseau ME ne possède aucun crafter pour ce type. Un crafter hors du réseau ME peut
quand même l'exécuter, à côté d'un Unpackager ou derrière un Package Crafting Machine Proxy. Lis
[Le diagnostic](Le-diagnostic) pour ce rapport.

## Signaler un bogue

1. Ouvre une issue sur le dépôt.
2. Donne les versions de Minecraft, Forge, AE2UEL et PackagedAuto, et les addons que tu utilises.
3. Donne les étapes qui reproduisent le problème.
4. Joins une capture d'écran.
