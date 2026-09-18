# Dépannage

## Les refus, et ce que chacun veut dire

| Message | Sens |
|---|---|
| **Insère d'abord un porte-recettes** | La machine n'a pas de porte-recettes. Le terminal n'en sort jamais un du stockage de lui-même : cela reste ta décision. |
| **Ce groupe est plein** | Vingt recettes déjà. C'est la limite du Package Recipe Encoder, pas celle de ce mod. |
| **Ce groupe a changé** | Quelqu'un a modifié le même groupe pendant que ton éditeur était ouvert. Rouvre la recette. |
| **Ce rapport ne tombe pas juste** | Keep ratio a refusé, et n'a **rien** changé. Aucune recette n'est laissée à moitié mise à l'échelle. |
| **Une recette de craft prend un objet par case** | PackagedAuto force le compte à un sur une recette de craft. |
| **Travail non sauvegardé** | Le premier clic avertit, le second change d'onglet. Rien n'est perdu en silence. |
| **Appliqué à 2, 1 sans porte-recettes** | L'écriture a réussi, mais une machine du groupe n'a pas de porte-recettes. |

## Le terminal se ferme tout seul

Hors de portée, ou sans énergie. L'éditeur suit la même règle : il se ferme plutôt que de montrer
une recette qu'il ne peut plus écrire.

## L'écran ne tient pas dans ma fenêtre

Il tient maintenant. Les deux écrans abaissent l'échelle d'interface du jeu tant qu'ils sont
ouverts, et la remettent à la fermeture. Le réglage ne change qu'**en mémoire** : ton fichier
d'options n'est jamais réécrit.

L'éditeur fait 338 pixels de haut, et Minecraft ne garantit que 320 sur 240.

## Deux machines qui devraient former un groupe

Les machines rejoignent un groupe seulement si elles portent **exactement** les mêmes recettes.
Partager une recette ne suffit pas. Si une paire s'est scindée, un côté a une recette de moins :
ouvre l'un ou l'autre et appuie sur Save, l'écriture répare les deux.

## Une recette ne tourne jamais

Ouvre l'onglet **Machines**. Si la recette apparaît dans le rapport des orphelines, le réseau n'a
aucun crafter du bon palier. Voir [Le diagnostic](Le-diagnostic).

## Signaler un bogue

Ouvre une issue avec les versions de Minecraft, Forge, AE2UEL et PackagedAuto, les addons que tu
utilises, et les étapes qui reproduisent le problème. Une capture d'écran aide plus qu'une
description.
