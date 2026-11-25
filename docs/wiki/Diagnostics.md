# Diagnostics

## The Machines tab

The Machines tab opens with the report. The report names every recipe type, among the encoded
recipes, that no crafter on the ME network runs. No other mod reports those recipes.

Each line of the report is orange, because it states a doubt and not a fault. The terminal sees
the ME network only. A crafter outside the ME network can still run the type, next to an
Unpackager or behind a Package Crafting Machine Proxy. The tooltip of the line says so.

Two sections come below the report. The Crafters section lists the Package Crafters of the
network. The Routers section lists the machines that only move packages, which are the Positioned
Package Distributor and the Package Crafting Machine Proxy.

For each machine, the tab gives its name and its state:

| State | Meaning |
|---|---|
| inactive, in red | The machine has no channel. It runs nothing. |
| busy | The machine crafts, or holds items to craft. |
| no recipe, in red | No encoded recipe targets this kind of machine. |
| a number of recipes | The encoded recipes that this kind of machine runs. |

## Why the report matters

PackagedAuto fails quietly. Take a recipe encoded for an Elite table, on a network that owns a
Basic table only. The recipe never runs. Nothing turns red. Nothing reaches the log. The item
never arrives. This fault is frequent in game.

## Reading the report

The tab reports a recipe type when it names a crafting machine that the ME network does not hold.

We write one table per integration. The table maps a recipe type to a crafter class. We need that
table because `IRecipeType.getRepresentation()` returns the source station of the craft, not the
Package Crafter. If a recipe type is absent from the table, the tab reports nothing about it. It
does not guess.

## Finding a machine

The pin button sits on the Patterns tab, not on the Machines tab.

1. Open the Patterns tab.
2. Click the pin button on a group row.

The terminal marks every machine of the group in the world. It draws a tinted cube and a beam that
rises. The mark lasts fifteen seconds, and the `highlightSeconds` configuration entry sets that
duration.
