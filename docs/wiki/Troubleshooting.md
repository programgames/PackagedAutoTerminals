# Troubleshooting

## The refusals, and what each one means

| Message | Meaning |
|---|---|
| Insert a Package Recipe Holder into this machine first | The machine holds no recipe holder, and the server sets `pullHolderFromNetwork` to `false`. With that setting, the terminal never takes a holder from storage on its own. That choice stays yours. |
| No blank Package Recipe Holder on the network | The machine holds no recipe holder. `pullHolderFromNetwork` is `true`, the default, so the terminal takes a blank holder from the ME network when you save. The network holds none. |
| You are not allowed to build on this network | The AE2 security of the network denies you the Build permission. The terminal does not open without it. |
| Click again to delete this recipe. | The first left click on the cross of a recipe row warns you. A second click on the same recipe, within three seconds, removes it. |
| Shift + left click again to send the holders to the network. | The first Shift + left click on a group row warns you. A second one on the same row, within three seconds, sends the recipe holders of the group back to the network, with their recipes. |
| This group is full: 20 recipes at most | The group already holds the maximum number of recipes. The limit belongs to the Package Recipe Encoder, not to this mod. |
| This group changed: open the recipe again | The recipe that you opened is no longer in the group. Another player, or a change to a machine, removed it. |
| This ratio does not divide every slot | Keep ratio refused the change and changed nothing. No recipe stays half scaled. |
| This ratio would pass %d in a slot | Keep ratio refused the change, because one slot would go over its own maximum. |
| A craft recipe takes one item per cell | PackagedAuto forces the count to one on a crafting recipe. |
| Unsaved recipe. Click again to leave it. | The first click warns you. The second click leaves the recipe. You lose nothing without a message. |
| Applied to 2, 1 without holder | The terminal wrote the recipe, but one machine of the group holds no recipe holder. The terminal took none, because `pullHolderFromNetwork` is `false` or the network holds no blank holder. |
| Applied to 2, 1 holder taken from the network | This is not a refusal. The terminal wrote the recipe, and took one blank holder out of the ME network for a machine that had none. Set `pullHolderFromNetwork` to `false` to keep the holders under your own hand. |
| Nothing was written | No machine of the group took the recipe. |

## The terminal closes on its own

The wireless terminal closes when you leave the range, or when its buffer empties. The wired
terminal closes when it loses its network. Both close when you lose the Build permission on the
network. The editor follows the same rules. It closes. It does not show a recipe that it no
longer writes.

## The screen does not fit your window

Both screens lower the GUI scale of the game while they are open. They restore the old value when
they close. They change the value in memory only, and they never write your options file.

The terminal is 331 pixels tall and shows ten rows, so a 1920 by 1080 window keeps GUI scale 3.
The editor is 338 pixels tall. Minecraft guarantees a window of 320 by 240 pixels only.

## Two machines that belong in one group

The terminal joins two machines that carry exactly the same recipes. One shared recipe is not
enough.

If a group splits in two, one side misses a recipe. Give the same name to both sides:

1. Left click one of the two rows. The editor opens.
2. Type a name in the name field, at the top of the editor.
3. Do the same on the other row, with the same name.

The terminal then joins the two rows by the name, whatever their recipes. The next Save reaches
every machine of the group.

## A recipe never runs

Open the Machines tab. If the recipe type appears in orange in the report at the top of the tab,
the ME network holds no crafter for that type. A crafter outside the ME network can still run it,
next to an Unpackager or behind a Package Crafting Machine Proxy. Read [Diagnostics](Diagnostics)
for that report.

## Reporting a bug

1. Open an issue on the repository.
2. Give the versions of Minecraft, Forge, AE2UEL and PackagedAuto, and the addons that you run.
3. Give the steps that reproduce the fault.
4. Attach a screenshot of the screen.
