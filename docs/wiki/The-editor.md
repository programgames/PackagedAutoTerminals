# The editor

The layout follows the Package Recipe Encoder, so you learn one screen only. The ingredients sit
on the left. The arrow sits in the middle. The outputs sit on the right. The package preview sits
below.

One difference exists. The top row does not show the slots of a recipe holder. It shows the
recipes of the group. Its last empty slot creates a new recipe. You create a recipe and you edit a
recipe with the same gesture.

## The tab row

The top row holds ten columns over two rows, so it holds twenty slots. A recipe holder carries
twenty recipes at most. This limit belongs to the Package Recipe Encoder itself. It comes from the
`pattern_slots` entry of the PackagedAuto configuration, and Forge bounds that entry to 20. A pack
that lowers the entry lowers the limit.

The creation tab disappears once the group is full. The terminal then refuses a new recipe,
because the Encoder cannot open such a recipe again.

A green frame marks the open tab. A red dot in the corner of the open tab means that you did not
save the work.

## Amounts

Left click a filled slot to open the amount panel. The wheel over a slot changes the amount too,
and Shift and Ctrl take bigger steps.

Keep ratio scales every other filled slot of the recipe by the same factor. If the ratio does not
divide every slot, the terminal changes nothing at all and says so. No recipe stays half scaled.
The terminal also refuses a ratio that pushes a slot over its own maximum.

### A craft recipe takes one item per cell

The amount panel does not open on a recipe of a Package Crafter. These recipe types are Basic to
Ultimate, Extreme, Combination, Ender, and the plain Crafting type.

This restriction does not come from this mod. A crafting table consumes exactly one item per cell.
PackagedAuto forces the count back to one when it finds the recipe. Earlier versions opened the
panel on these recipes, and the panel changed nothing.

## The output box

A Processing recipe lets you write the outputs yourself.

A craft recipe computes them instead. The output box shows the crafted result in its center and
refuses every change. The preview below shows the packages that the recipe produces.

## JEI

| Gesture | Effect |
|---|---|
| Press `+` on a JEI recipe | Fills the whole grid, with the right recipe type |
| Drag one ingredient | Fills the slot that you drop it on |

Both directions work. The `+` button picks the narrowest recipe type that declares the JEI
category, because several PackagedAuto types accept every category.
