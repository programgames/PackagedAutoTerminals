# L'éditeur

La disposition suit le Package Recipe Encoder, donc tu n'apprends qu'un seul écran. Les
ingrédients sont à gauche. La flèche est au milieu. Les sorties sont à droite. L'aperçu du colis
est en dessous.

Une différence existe. La rangée du haut ne montre pas les cases d'un porte-recettes. Elle montre
les recettes du groupe. Sa dernière case vide crée une nouvelle recette. Tu crées une recette et
tu modifies une recette avec le même geste.

## La rangée d'onglets

La rangée du haut comporte dix colonnes sur deux lignes, donc vingt cases. Un porte-recettes porte
vingt recettes au maximum. Cette limite appartient au Package Recipe Encoder lui-même. Elle vient
de l'entrée `pattern_slots` de la configuration PackagedAuto, et Forge borne cette entrée à 20. Un
pack qui abaisse l'entrée abaisse la limite.

L'onglet de création disparaît dès que le groupe est plein. Le terminal refuse alors une nouvelle
recette, car l'Encodeur ne sait plus rouvrir une telle recette.

Un cadre vert marque l'onglet ouvert. Un point rouge dans le coin de l'onglet ouvert dit que tu
n'as pas sauvegardé le travail.

## Les quantités

Fais un clic gauche sur une case remplie pour ouvrir le panneau de quantité. La molette sur une
case change aussi la quantité, et Maj et Ctrl prennent des pas plus grands.

Keep ratio met à l'échelle toutes les autres cases remplies de la recette par le même facteur. Si
le rapport ne tombe pas juste sur toutes les cases, le terminal ne change rien du tout et le dit.
Aucune recette ne reste à moitié mise à l'échelle. Le terminal refuse aussi un rapport qui pousse
une case au-delà de son propre maximum.

### Une recette de craft prend un objet par case

Le panneau de quantité ne s'ouvre pas sur une recette de Package Crafter. Ces types de recette
sont Basic à Ultimate, Extreme, Combination, Ender, et le type Crafting simple.

Cette restriction ne vient pas de ce mod. Une table de craft consomme exactement un objet par
case. PackagedAuto force le compte à un quand il trouve la recette. Les versions antérieures
ouvraient le panneau sur ces recettes, et le panneau ne changeait rien.

## La case de sortie

Une recette Processing te laisse écrire les sorties toi-même.

Une recette de craft les calcule à ta place. La case de sortie montre le résultat en son centre et
refuse toute modification. L'aperçu du dessous montre les colis que la recette produit.

## JEI

| Geste | Effet |
|---|---|
| Appuyer sur `+` sur une recette JEI | Remplit toute la grille, avec le bon type de recette |
| Glisser un ingrédient | Remplit la case où tu le déposes |

Les deux sens fonctionnent. Le bouton `+` choisit le type de recette le plus précis qui déclare la
catégorie JEI, car plusieurs types PackagedAuto acceptent toutes les catégories.
