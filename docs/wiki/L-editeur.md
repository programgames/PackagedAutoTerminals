# L'éditeur

La disposition suit le **Package Recipe Encoder**, donc rien ne s'apprend deux fois : les
ingrédients à gauche, la flèche, les sorties à droite, l'aperçu du colis en dessous.

Une différence : la rangée du haut ne montre pas les cases d'un porte-recettes, mais **les
recettes du groupe**. La dernière case vide en crée une. Créer et modifier deviennent le même
geste.

## La rangée d'onglets

Dix colonnes sur deux lignes, vingt cases. Un porte-recettes porte **vingt recettes au maximum**,
ce qui est la limite de l'Encodeur lui-même : l'entrée `pattern_slots` de la config PackagedAuto,
que Forge borne à 20.

L'onglet de création disparaît dès que le groupe est plein. Le terminal refuse une vingt et
unième recette plutôt que d'en écrire une que ton Encodeur ne pourrait jamais rouvrir.

Un **cadre vert** marque l'onglet ouvert. Un **point rouge** dans son coin dit que le travail
n'est pas sauvegardé.

## Les quantités

Clic gauche sur une case remplie pour ouvrir le panneau de quantité. La molette sur une case
fonctionne aussi : Maj et Ctrl prennent des pas plus grands.

**Keep ratio** met à l'échelle toutes les autres cases remplies de la recette. Un rapport qui ne
tombe pas juste sur toutes les cases ne change **rien du tout**, et le dit. Aucune recette n'est
laissée à moitié mise à l'échelle.

### Une recette de craft prend un objet par case

Le panneau de quantité ne s'ouvre pas sur une recette de **Package Crafter** : Basic à Ultimate,
Extreme, Combination, Ender, et le type Crafting simple.

Ce n'est pas une limite de ce mod. Une table de craft consomme exactement un objet par case, et
PackagedAuto force le compte à un quand il cherche la recette. Le panneau s'ouvrait autrefois là,
et ne changeait rien.

## La case de sortie

Une recette **Processing** te laisse écrire les sorties toi-même.

Une recette de **craft** les calcule : la case montre le résultat, centré, et refuse toute
modification. La case du dessous montre les **colis** que la recette produit.

## JEI

| Geste | Effet |
|---|---|
| `+` sur une recette JEI | Remplit toute la grille, avec le bon type de recette |
| Glisser un ingrédient | Remplit la case où tu le déposes |

Les deux sens fonctionnent. Le bouton `+` choisit le type de recette **le plus précis** qui
déclare la catégorie JEI, car plusieurs types PackagedAuto acceptent toutes les catégories.
