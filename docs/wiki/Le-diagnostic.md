# Le diagnostic

## L'onglet Machines

Il liste les **Package Crafters** du réseau, leur palier, et si chacun travaille.

Sa vraie valeur est le rapport du dessous : chaque recette encodée qu'**aucun crafter du réseau ne
sait exécuter**.

## Pourquoi cela compte

PackagedAuto échoue en silence. Une recette encodée pour une table Elite, sur un réseau qui ne
possède qu'une Basic, ne tourne tout simplement jamais. Rien ne devient rouge, rien n'est
journalisé, et l'objet n'arrive pas. C'est l'erreur la plus fréquente en jeu, et aucun autre mod
ne la signale.

## Lire le rapport

Une recette est signalée quand son type nomme une machine de craft que le réseau ne possède pas.

La table qui relie un type de recette à une classe de crafter est écrite **par intégration**, car
`IRecipeType.getRepresentation()` retourne la station d'origine du craft, pas le Package Crafter.
Un type inconnu s'affiche comme **non reconnu** et reste muet, plutôt que de mentir.

## Trouver une machine

Le bouton en forme d'épingle marque la machine dans le monde : un cube teinté et un faisceau
vertical, pendant quinze secondes. La durée est l'entrée de config `highlightSeconds`.
