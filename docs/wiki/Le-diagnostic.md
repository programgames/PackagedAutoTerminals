# Le diagnostic

## L'onglet Machines

L'onglet Machines s'ouvre sur le rapport. Le rapport nomme chaque recette encodée qu'aucun crafter
du réseau n'exécute. Aucun autre mod ne signale ces recettes.

Deux sections viennent sous le rapport. La section Crafters liste les Package Crafters du réseau.
Pour chacun, elle donne le nom de la machine et son état, qui est inactif, occupé ou au repos. La
section Routers liste les machines qui ne font que déplacer des colis, soit le Positioned Package
Distributor et le Package Crafting Machine Proxy.

## Pourquoi ce rapport compte

PackagedAuto échoue en silence. Prends une recette encodée pour une table Elite, sur un réseau qui
ne possède qu'une table Basic. La recette ne tourne jamais. Rien ne devient rouge. Rien n'arrive
dans le journal. L'objet n'arrive pas. Cette erreur est fréquente en jeu.

## Lire le rapport

L'onglet signale une recette quand le type de cette recette nomme une machine de craft que le
réseau ne possède pas.

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
