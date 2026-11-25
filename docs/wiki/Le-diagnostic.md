# Le diagnostic

## L'onglet Machines

L'onglet Machines s'ouvre sur le rapport. Le rapport nomme chaque type de recette, parmi les
recettes encodées, qu'aucun crafter du réseau ME n'exécute. Aucun autre mod ne signale ces
recettes.

Chaque ligne du rapport est orange, car elle énonce un doute et non une panne. Le terminal ne voit
que le réseau ME. Un crafter hors du réseau ME peut quand même exécuter le type, à côté d'un
Unpackager ou derrière un Package Crafting Machine Proxy. L'infobulle de la ligne le dit.

Deux sections viennent sous le rapport. La section Crafters liste les Package Crafters du réseau.
La section Routers liste les machines qui ne font que déplacer des colis, soit le Positioned
Package Distributor et le Package Crafting Machine Proxy.

Pour chaque machine, l'onglet donne son nom et son état :

| État | Sens |
|---|---|
| inactive, en rouge | La machine n'a pas de canal. Elle n'exécute rien. |
| occupée | La machine fabrique, ou porte des objets à fabriquer. |
| aucune recette, en rouge | Aucune recette encodée ne vise ce genre de machine. |
| un nombre de recettes | Les recettes encodées que ce genre de machine exécute. |

## Pourquoi ce rapport compte

PackagedAuto échoue en silence. Prends une recette encodée pour une table Elite, sur un réseau qui
ne possède qu'une table Basic. La recette ne tourne jamais. Rien ne devient rouge. Rien n'arrive
dans le journal. L'objet n'arrive pas. Cette erreur est fréquente en jeu.

## Lire le rapport

L'onglet signale un type de recette quand il nomme une machine de craft que le réseau ME ne
possède pas.

Nous écrivons une table par intégration. La table relie un type de recette à une classe de
crafter. Cette table est nécessaire car `IRecipeType.getRepresentation()` retourne la station
d'origine du craft, pas le Package Crafter. Si un type de recette est absent de la table, l'onglet
ne signale rien à son sujet. Il ne devine pas.

## Trouver une machine

Le bouton en forme d'épingle se trouve sur l'onglet Patterns, pas sur l'onglet Machines.

1. Ouvre l'onglet Patterns.
2. Clique sur le bouton en forme d'épingle d'une ligne de groupe.

Le terminal marque toutes les machines du groupe dans le monde. Il dessine un cube teinté et un
faisceau qui monte. La marque dure quinze secondes, et l'entrée de configuration
`highlightSeconds` fixe cette durée.
