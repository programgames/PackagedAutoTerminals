# Les fluides

## Ce que devient un fluide

Un fluide glissé depuis JEI entre dans une recette sous forme de Fluid Packet AE2FC. Un Fluid
Packet est un objet qui porte un fluide. Ce n'est pas un seau.

PackagedAuto seul refuse un fluide. Son addon PackagedFluidCrafting modifie l'Encodeur par un
mixin. Un mixin est du code qu'un mod ajoute dans la classe d'un autre mod. L'addon transforme un
`FluidStack` glissé en `FakeFluids.packFluid2Packet(fluid)`. Le Packager et le Crafter relisent le
fluide dans le paquet.

Un seau dans une case de recette reste un objet ordinaire. Aucun fluide ne se déplace pour lui, et
la recette ne tourne jamais. Les versions antérieures de ce mod écrivaient un seau, et c'était
faux.

## Ce qu'il faut

Les fluides exigent deux mods. AE2 Fluid Crafting fournit l'objet paquet. PackagedFluidCrafting
apprend aux machines à lire le paquet. Installe les deux.

Sans eux, un fluide glissé devient son seau rempli, car aucune machine ne lit un paquet de toute
façon.

## Les quantités

Tu édites un fluide en millibuckets (mB), jusqu'à un milliard.

| Où | Pas |
|---|---|
| Panneau de quantité | `+10`, `+100`, `+1000` mB |
| Molette | 100 mB, 1000 mB avec Maj, 10000 mB avec Ctrl |

La case affiche la quantité en buckets, comme tout autre écran de fluide d'AE2. 4000 mB donne `4`,
250 mB donne `0.25`, et 120000 mB donne `120K`. Le texte rétrécit tout seul et ne déborde jamais
de la case.

Keep ratio traite les fluides et les objets ensemble. Une recette qui prend 1000 mB d'eau et deux
objets devient une recette qui prend 2000 mB et quatre objets.

## Les gaz

Les gaz ne passent pas encore. Le format existe dans AE2 Fluid Crafting, sous `ae2fc:gas_packet`.
Personne ne l'a vérifié en jeu sans Mekanism. Ce projet n'écrit rien de mémoire.
