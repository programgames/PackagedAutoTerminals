# Diagnostics

## The Machines tab

The Machines tab opens with the report. The report names every encoded recipe that no crafter on
the network runs. No other mod reports those recipes.

Two sections come below the report. The Crafters section lists the Package Crafters of the
network. For each one, it gives the name of the machine and its state, which is inactive, busy or
idle. The Routers section lists the machines that only move packages, which are the Positioned
Package Distributor and the Package Crafting Machine Proxy.

## Why the report matters

PackagedAuto fails quietly. Take a recipe encoded for an Elite table, on a network that owns a
Basic table only. The recipe never runs. Nothing turns red. Nothing reaches the log. The item
never arrives. This fault is frequent in game.

## Reading the report

The tab reports a recipe when the type of that recipe names a crafting machine that the network
does not hold.

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
