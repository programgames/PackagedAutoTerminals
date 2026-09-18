# Troubleshooting

## The refusals, and what each one means

| Message | Meaning |
|---|---|
| **Insert a Package Recipe Holder** | The machine has no holder. The terminal never takes one out of storage on its own: that stays your decision. |
| **This group is full** | Twenty recipes already. That is the limit of the Package Recipe Encoder, not of this mod. |
| **This group changed** | Someone else edited the same group while your editor was open. Reopen the recipe. |
| **This ratio does not divide every slot** | Keep ratio refused, and changed **nothing**. No recipe is left half scaled. |
| **A craft recipe takes one item per cell** | PackagedAuto forces the count to one on a crafting recipe. |
| **Unsaved work** | The first click warns, the second switches. Nothing is lost silently. |
| **Applied to 2, 1 without holder** | The write succeeded, but one machine of the group has no recipe holder. |

## The terminal closes on its own

Out of range, or out of energy. The editor follows the same rule: it closes rather than showing a
recipe it can no longer write.

## The screen does not fit my window

It does now. Both screens lower the GUI scale of the game while they are open, and give it back
when they close. The setting is only changed in memory: your options file is never written.

The editor is 338 pixels tall, and Minecraft only guarantees 320 by 240.

## Two machines that should be one group

Machines join a group only when they carry **exactly** the same recipes. Sharing one recipe is not
enough. If a pair split in two, one side is missing a recipe: open either one and press Save, and
the write repairs both.

## A recipe never runs

Open the **Machines** tab. If the recipe appears in the orphan report, the network has no crafter
of the right tier. See [Diagnostics](Diagnostics).

## Reporting a bug

Open an issue with the versions of Minecraft, Forge, AE2UEL and PackagedAuto, the addons you run,
and the steps that reproduce it. A screenshot of the screen helps more than a description.
