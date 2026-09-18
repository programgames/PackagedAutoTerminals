# Diagnostics

## The Machines tab

It lists the **Package Crafters** of the network, their tier, and whether each one is busy.

Its real value is the report below: every encoded recipe that **no crafter on the network can
run**.

## Why that matters

PackagedAuto fails quietly. A recipe encoded for an Elite table, on a network that only owns a
Basic one, simply never runs. Nothing turns red, nothing is logged, and the item never arrives.
This is the most frequent mistake in game, and no other mod reports it.

## Reading the report

A recipe is reported when its type names a crafting machine that the network does not hold.

The table that maps a recipe type to a crafter class is written **per integration**, because
`IRecipeType.getRepresentation()` returns the source station of the craft, not the Package
Crafter. An unrecognised type is shown as **unrecognised** and stays silent, rather than lying.

## Finding a machine

The pin button on a row marks the machine in the world: a tinted cube and a beam going up, for
fifteen seconds. The duration is the `highlightSeconds` config entry.
