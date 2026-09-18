# Les fluides

## Ce que devient un fluide

Un fluide glissé depuis JEI entre dans une recette sous forme de **Fluid Packet** AE2FC, et non de
seau.

C'est ce que PackagedAuto utilise lui-même. Son addon **PackagedFluidCrafting** se greffe sur
l'Encodeur et transforme un `FluidStack` glissé en `FakeFluids.packFluid2Packet(fluid)`. Le
Packager et le Crafter relisent le paquet par leur gestionnaire d'objets convertisseur.

Un seau dans une case de recette est un **objet** ordinaire. Aucun fluide n'est déplacé pour lui,
et la recette ne tourne jamais. Les versions antérieures de ce mod écrivaient un seau, et c'était
faux.

## Ce qu'il faut

Les deux mods, ensemble :

- **AE2 Fluid Crafting**, qui fournit l'objet paquet ;
- **PackagedFluidCrafting**, qui apprend aux machines à le lire.

Sans eux, un fluide glissé retombe sur son seau rempli, car aucune machine ne saurait lire un
paquet de toute façon.

## Les quantités

Un fluide s'édite en **millibuckets**, jusqu'à un milliard.

| Où | Pas |
|---|---|
| Panneau de quantité | `+10`, `+100`, `+1000` mB |
| Molette | 100 mB, 1000 avec Maj, 10000 avec Ctrl |

La case affiche la quantité en **buckets**, comme tout écran de fluide d'AE2 : 4000 mB donne `4`,
250 mB donne `0.25`, 120000 mB donne `120K`. Le texte rétrécit tout seul et ne déborde jamais.

**Keep ratio** traite fluides et objets ensemble : une recette qui prend 1000 mB d'eau et deux
objets en prend 2000 et quatre.

## Les gaz

Pas encore pris en charge. Le format existe dans AE2 Fluid Crafting, sous `ae2fc:gas_packet`, mais
il ne peut pas être vérifié en jeu sans Mekanism, et ce projet n'écrit rien de mémoire.
