# Getting started

## 1. Craft the terminal

Craft the PackagedAuto Terminal from one ME Terminal and one Package Recipe Holder.

## 2. Place the terminal

The terminal is an AE2 part. A part is a thin block that you attach to a face of an ME cable.

1. Aim at a face of an ME cable.
2. Right click to place the terminal.
3. Power the network. Without power, the part goes dark and the terminal shows no machine.

## 3. Open the terminal

Right click the part. The screen lists every Packager, every Unpackager and every Packaging
Provider of the network.

The terminal joins the machines that work together into a group. A group is normally one Packager
with one Unpackager. Four rules build the groups:

- Machines that carry exactly the same recipes join one group. One shared recipe is not enough.
- Machines that carry the same group name join one group, whatever their recipes.
- A single empty machine joins the group that waits for its role.
- A new Packager and a new Unpackager, both with empty holders, join one group.

The name is the only link that you set yourself. Read [Troubleshooting](Troubleshooting) for the
case of a group that splits in two.

## 4. Write a recipe

1. Left click a group row. The editor opens on a new recipe.
2. Choose the recipe type with the arrows next to its name.
3. Fill the grid. Take an item in hand, then left click a cell.
4. Press Save.

You can also drag an item out of JEI onto a cell, instead of step 3.

The recipe reaches the Packager and the Unpackager in the same gesture. PackagedAuto needs the
same recipe on both sides. If you write one side only, the automation breaks without a message.

To edit a recipe that already exists, right click its row in the list.

## The wireless terminal

Craft the Wireless PackagedAuto Terminal from one PackagedAuto Terminal and one AE2 Wireless
Terminal.

1. Link the wireless terminal in an ME Security Terminal.
2. Right click with it anywhere in range. It works from either hand.

The wireless terminal draws power from its own buffer, like every other wireless terminal of AE2.
Out of range or out of power, the screen closes. The terminal does not show a network that it no
longer reaches.

## Where to look next

For a short description page in game, press the JEI usage key on either item. The wiki continues
with [The editor](The-editor).
