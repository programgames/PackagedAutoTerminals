# The editor

The layout follows the **Package Recipe Encoder**, so nothing has to be learned twice: the
ingredients on the left, the arrow, the outputs on the right, the package preview below.

One difference: the top row does not show the slots of a recipe holder, but **the recipes of the
group**. The last empty slot creates one. Creating and editing become the same gesture.

## The tab row

Ten columns over two rows, twenty slots. A recipe holder carries **twenty recipes at most**, which
is the limit of the Encoder itself: the `pattern_slots` entry of the PackagedAuto config, which
Forge bounds to 20.

The creation tab disappears once the group is full. The terminal refuses a twenty first recipe
rather than writing one your Encoder could never open again.

A **green frame** marks the open tab. A **red dot** in its corner means the work is not saved.

## Amounts

Left click a filled slot to open the amount panel. The wheel over a slot works too: Shift and Ctrl
take bigger steps.

**Keep ratio** scales every other filled slot of the recipe by the same factor. A ratio that does
not divide every slot changes **nothing at all** and says so, so no recipe is ever left half
scaled.

### Craft recipes take one item per cell

The amount panel does not open on a recipe of a **Package Crafter**: Basic to Ultimate, Extreme,
Combination, Ender, and the plain Crafting type.

That is not a limit of this mod. A crafting table consumes exactly one item per cell, and
PackagedAuto forces the count back to one when it looks the recipe up. The panel used to open
there and change nothing.

## The output box

A **Processing** recipe lets you write the outputs yourself.

A **craft** recipe computes them: the box shows the crafted result, centred, and refuses any
change. The box below shows the **packages** the recipe produces.

## JEI

| Gesture | Effect |
|---|---|
| `+` on a JEI recipe | Fills the whole grid, with the right recipe type |
| Drag one ingredient | Fills the slot you drop it on |

Both directions work. The `+` button picks the **narrowest** recipe type that declares the JEI
category, because several PackagedAuto types accept every category as a catch all.
