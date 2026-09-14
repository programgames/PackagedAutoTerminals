# Interactive test protocol

> **Ground rule.** One test at a time, in order. You answer **OK**, or you describe what you
> see. On the first failure, we fix before moving on: one defect often hides another.
>
> **On failure**: a screenshot is enough. I read the log myself, in `run/logs/latest.log`.
>
> **Code state**: everything marked 🆕 has **never** run in game.

---

## Progress

| Group | State |
|---|---|
| A — The foundation | passed |
| B — Reading and search | passed |
| C — The eye and the name | passed |
| D — The merged editor | passed |
| E — The pair, and its accidents | passed |
| F — The Machines tab | passed |
| G — The wireless terminal | passed |
| H — Settings and real instance | H1 passed, H2 pending |
| I — Folding and crafting machine | 🆕 never tested |
| J — Look of the part and of the items | 🆕 never tested |
| K — Wireless universal terminal (AE2WUT) | 🆕 never tested |
| L — Opening key | 🆕 never tested |
| M — Reading and editing comfort | 🆕 never tested |

Fixes born from these runs: name frame raised to 16 pixels, buttons placed under the preview,
single feedback message trimmed to the frame, grouping by the name given by the player, text
widths computed instead of hard coded, narrowest recipe type chosen on the JEI transfer (D32),
and wiring of the wireless range, link and energy checks, which nobody called.

---

## Preparation

### P1 — Launch

```bash
cd /c/Users/Julien/Desktop/PackagedAutoTerminals && ./gradlew runClient
```

### P2 — The test bench

In a creative superflat world, build this:

| Element | Role in the tests |
|---|---|
| ME Controller, Creative Energy Cell, cables | the network |
| **Packager** and **Unpackager**, connected | the reference pair |
| **Package Recipe Encoder** | encode by hand, for comparison |
| 4 **Package Recipe Holders** | two for the pair, two spare |
| **Package Crafter** and **Ultimate Crafter** | the Machines tab and the diagnostic |
| **Positioned Package Distributor** | check the "Routers" section |
| A plain ME terminal | check that AE2 does see our recipes |
| Iron, gold, redstone, diamonds | something to encode |

Keep a **second Unpackager** aside: it will serve the missing role test.

---

## A — The foundation

### A1 — Loading
Open the **Mods** menu.
→ `PackagedAuto Terminals` appears in the list.

### A2 — Crafting
Look for `PackagedAuto Terminal` in JEI, then craft it.
→ The recipe needs an ME terminal and a Package Recipe Holder.

### A3 — Placement
Place the terminal on a cable, then right click.
→ The screen opens. Width **256**, no trimmed text.

---

## B — Reading and search

### B1 — The pair
Encode the same recipe into both recipe holders with the Encoder, put them into the Packager
and the Unpackager, then open the terminal.
→ **One single** header line, "Packager/Unpackager pair", followed by **one single** recipe
line.

### B2 — The summary
Look at the line above the inventory.
→ "2 machines · 1 recipe". Hovering gives the packet size.

### B3 — Free search
Type the name of the produced item, then `zzz`.
→ The filter follows, and the empty list message fits in the frame.

### B4 — Mod prefix 🆕
Type `@minecraft`.
→ Only the recipes that **produce** an item from Minecraft stay.

### B7 — The search ignores the ingredients 🆕
Type `elite`.
→ Only the recipes that produce an item named Elite stay. "ME Interface" and "Ultimate
Crafting Table", which merely consume an Elite part, no longer appear. See decision D39.

### B5 — Type prefix 🆕
Type `#processing`, then `#ultimate`.
→ The filter follows the recipe type.

### B6 — Leaving the search
Escape, then reopen and press **E** with the field active.
→ The screen closes in both cases.

---

## C — The eye and the name

### C1 — Locating 🆕
Click the **eye**, right of the group line.
→ The screen closes, and both blocks blink cyan for five seconds, even behind a wall.

### C2 — Naming 🆕
Right click the recipe, type `Iron` into the top field, **Enter**.
→ Green message "Group renamed".

### C3 — The name lives
**Back**, then look at the list.
→ The line shows "Iron" instead of "Packager/Unpackager pair".

---

## D — The merged editor 🆕

### D1 — Opening
Right click the recipe.
→ The screen follows the Encoder layout. The recipe tab carries a **green frame**.

### D2 — The tabs
Look at the top row.
→ One slot per recipe, showing the produced item, then **one empty slot**.

### D3 — Editing
Change an ingredient, then **Save**.
→ Green message "Applied to 2 machines". The screen **stays open**.

### D4 — No duplicate
Press **Save** a second time.
→ Still a single recipe in the tab row.

### D5 — Creating
Click the **empty slot**, fill the grid, **Save**.
→ A new tab appears, and the terminal will count two recipes.

### D6 — Leaving without saving
Change a slot, then click another tab.
→ Red message "Unsaved recipe". A second click switches.

### D7 — Amount with the wheel
Wheel over a filled slot, then Shift and Ctrl.
→ Steps of 1, 10, then 64. Never below 1.

### D8 — Amount on the keyboard 🆕
**Middle click** on a filled slot, type `128`, **Enter**.
→ The slot shows 128. Escape elsewhere cancels without changing anything.

### D9 — Recipe type
The `<` and `>` arrows under the type name.
→ The name, the icon and the enabled slots change together.

### D10 — Clear and delete
**Clear**, then **Delete**.
→ The grid empties without writing anything; the deletion removes the recipe from both
machines.

### D11 — JEI transfer
Open a recipe in JEI, click its **+** button.
→ The grid fills, and the type switches to the right one.

---

## E — The pair, and its accidents 🆕

### E1 — Recipe on one side only
With the Encoder, remove the recipe from the **Packager** only.
→ In the terminal, the line turns **red**, and the tooltip says to encode it into a Packager
too.

### E2 — Repairing
Right click the red line, then **Save** without changing anything.
→ "Applied to 2 machines", and the line turns black again.

### E3 — Machine without a recipe holder
Remove the recipe holder from the Packager, then click its line in the terminal.
→ Message "Insert a Package Recipe Holder first". **Nothing is pulled from the ME network.**

### E4 — Brand new pair
Empty both recipe holders.
→ One single line, "no recipe yet", and **no** red sentence.

### E5 — Missing role
Place the second Unpackager, connected, with no recipe holder.
→ It appears on its own line: the terminal does not guess when two machines could claim the
same role.

### E6 — Group removal
**Shift + left click** on the pair line.
→ Both recipe holders come back into the ME terminal, with their recipes.

### E7 — Two pairs that share one recipe 🆕
Encode the same recipe into two different pairs, then give each pair one recipe of its own.
→ **Two** lines, one per pair. Before decision D38, the shared recipe soldered the four
machines into a single "Group of 4 machines".

### E8 — Diverging pair 🆕
On a pair carrying two recipes, remove **one** of them from the Packager only.
→ The two sides no longer carry the same recipes, so they split into two lines. Name them with
the same name in the editor: they join again, and the missing recipe is flagged in red.

---

## F — The Machines tab 🆕

### F1 — Switching
Click the **Patterns** button at the top left.
→ It turns into **Machines**.

### F2 — The two sections
→ **Crafters** first, then **Routers** with the Distributor.

### F3 — State
→ Each machine shows "idle" or "busy".

### F4 — Orphan recipe
Encode an **Ultimate** recipe without placing an Ultimate Crafter.
→ A red warning at the top of the list. Place the crafter: it disappears.

### F5 — No false positive
Check that a **Processing** recipe triggers no warning.
→ That type needs no recognised machine.

---

## G — The wireless terminal 🆕

> The riskiest part: it has never run, and the container rework also touches the wired
> terminal. If A to F break, say so before starting G.

### G1 — Crafting
→ The **Wireless PackagedAuto Terminal** is crafted from ours and the AE2 wireless terminal.

### G2 — Without a link
Right click without having linked it.
→ "This terminal is not linked to a network".

### G3 — Linked
Link it to a Wireless Access Point, then right click.
→ The terminal opens, identical to the wired one.

### G4 — Out of range
Walk away.
→ The screen closes by itself. Same from the editor.

---

## I — Folding and crafting machine 🆕

### I1 — Collapsed by default
Open the terminal.
→ Each group takes **one** line. No recipe is visible. A chevron points right, left of the
icon.

### I2 — Expanding
Click the chevron.
→ The chevron points down, and the recipes of the group appear.

### I3 — Creation stays intact
Click the group line **anywhere but on the chevron**.
→ The editor opens on a new recipe, as in test D5.

### I4 — Group with no recipe
Look at an empty pair.
→ No chevron. The space stays empty, and the icon does not move.

### I5 — Search expands
Type the name of a produced item.
→ The group holding the recipe opens by itself. The others stay collapsed.

### I6 — Clearing the field collapses
Clear the search.
→ Everything collapses again, except the groups you had opened with the chevron.

### I7 — Session memory
Close the terminal, then reopen it.
→ Everything is collapsed again.

### I8 — Machine on the group row
Look at a group whose recipes are all of the Elite type.
→ On the group row, left of the eye, the **Elite Package Crafter** icon appears. The tooltip
of that row says "Crafted by: Elite Package Crafter".

### I9 — Machine absent
Remove the Elite Package Crafter from the network.
→ The icon turns dark, and the tooltip line turns red: "not on the network".

### I10 — Group with two machines
Put an Elite recipe and an Ultimate recipe into the same group.
→ **No icon** on the group row. Two different machines cannot be named by one icon.

### I13 — The recipe rows carry the type name 🆕
Look at a recipe row.
→ On the right, the short name of the type, such as "Elite". The machine icon is no longer
there: it moved to the group row.

### I14 — The Patterns button 🆕
Look at the top left corner of the screen.
→ The button is whole. Its top edge no longer touches the frame of the screen.

### I11 — Machine in the editor
Open an Elite recipe in the editor.
→ Below the type icon, the Elite Package Crafter icon appears. Hovering gives its name.

### I12 — The type changes, the machine follows
In the editor, change the type with `<` and `>`.
→ The machine icon changes with each type.

---

## J — Look of the part and of the items 🆕

### J1 — The placed part
Place the terminal on an ME cable.
→ The screen is **fluix purple**, with a bright white package in the middle. It looks like the
AE2 Pattern Terminal, placed next to it.

### J2 — The colour follows the network
Paint the cable red with a Color Applicator.
→ The screen turns red. The old textures kept their turquoise.

### J3 — The item in the inventory
Look at the terminal in your hotbar.
→ It carries the AE2 case, and its purple screen. It is **not** white: that would mean the
colour handler is missing.

### J4 — The wireless terminal
Look at the wireless item.
→ Pink antenna at the top left, purple screen, white package. Same family as the AE2 Wireless
Pattern Terminal.

### J5 — Unpowered
Cut the power of the network.
→ The screen goes dark, like the AE2 terminals.

---

## K — Wireless universal terminal (AE2WUT) 🆕

> `run/mods` now carries `ae2wut-1.0.5.jar`. Without it, this whole group is skipped, and the
> rest of the mod must work as before.

### K1 — Without AE2WUT
Remove `ae2wut-1.0.5.jar` from `run/mods`, then start the client.
→ The game starts. The log says nothing about AE2WUT. No mixin applies.

### K2 — With AE2WUT
Put the jar back, then restart.
→ The log carries `AE2WUT detected, mode 41`. The game starts.

### K3 — The assembly recipe
In JEI, look for the **Wireless Universal Terminal**.
→ A shapeless recipe combines the universal terminal and our **Wireless PackagedAuto
Terminal**.

### K4 — Assembling
Craft that recipe.
→ The universal terminal comes out of the table. Its tooltip, with Shift held, lists
"Wireless PackagedAuto Terminal".

### K5 — The wheel
Shift + wheel on the universal terminal.
→ The displayed name walks through every absorbed mode, ours included.

### K6 — Opening
Set the wheel to our mode, link the terminal to an access point, then right click.
→ Our screen opens, with the recipes of the network.

### K7 — Not linked
On a universal terminal that was never linked, right click in our mode.
→ Message "This terminal is not linked to a network". No screen.

### K8 — Out of range
Walk away from the access point, with the screen open.
→ It closes by itself, as with our own wireless terminal.

### K9 — Out of energy
Empty the universal terminal.
→ Message "No power". The universal terminal is the one that discharges, not ours.

### K10 — The other modes
Switch back to the ME mode, then to the AE2 Pattern mode.
→ The AE2 screens open normally. Our listener does not intercept them.

### K12 — The universal terminal icon
Set the universal terminal to our mode, then look at it in the hotbar and in hand.
→ It carries **our** icon: pink antenna, purple screen, white package. No purple and black
checkerboard, no model name written over it.

### K13 — The other modes keep their icon
Switch with the wheel to the ME mode, then to the Pattern mode.
→ Each mode gets the icon of the matching AE2 terminal back.

### K11 — The adjustable mode
In `run/config/packagedautoterminals.cfg`, set `wutModeId` to another value, then restart.
→ An already assembled universal terminal loses our mode; it must be reassembled. No crash.

---

## L — Opening key 🆕

### L1 — The key exists, and is bound to nothing
Open **Options**, then **Controls**.
→ A "PackagedAuto Terminals" category carries "Open the wireless terminal". No key is assigned
to it.

### L2 — No conflict
Look at the four AE2 controls and the Cell Terminal one.
→ None of them shows in red.

### L3 — Binding
Assign a free key, for example `K`.

### L4 — Without a terminal
Empty your inventory, then press the key.
→ "No wireless PackagedAuto terminal in your inventory."

### L5 — From the inventory
Put the linked wireless terminal into an inventory slot, out of hand, then press.
→ The screen opens.

### L6 — From the offhand
Put the terminal in the offhand, then press.
→ The screen opens.

### L7 — Not linked
With a terminal that was never linked, press.
→ "This terminal is not linked to a network". The search stops there, and does not move to the
next one.

### L8 — The universal terminal
Carry a universal terminal that absorbed our mode, **set to another mode**, then press.
→ Our screen opens. The universal terminal switches to our mode.

### L9 — The crafting grid survives
Before L8, set the universal terminal to the AE2 Crafting mode and fill its grid. Press our
key, then go back to the Crafting mode with the wheel.
→ The grid is intact. The `nbtChangeB` call is what stores it.

### L10 — Priority
Carry both items at once: our wireless terminal, and a universal terminal.
→ Our terminal comes first.

---

## M — Reading and editing comfort 🆕

### M1 — Striping
Open the terminal, with at least four lines.
→ Every other row carries a slightly darker background. The pattern does not jump when you
scroll the list.

### M2 — Highlight
Move the mouse over the list.
→ The row under the cursor turns light blue. The text and the icons stay readable.

### M3 — The search cross
Type some text into the field.
→ A cross appears at the right of the field. It disappears when the field is empty.

### M4 — Clear in one click
Click the cross.
→ The field empties, it keeps focus, and the full list comes back.

### M5 — The counter
Type the name of a produced item.
→ At the right of the summary line, "2 results" appears. The counter disappears when the field
is empty.

### M6 — The tab dot
In the editor, change an ingredient.
→ A red dot appears in the corner of the open tab. **Save** makes it disappear.

### M7 — Enter saves
Change a slot, then press **Enter**.
→ Green message "Applied to 2 machines". Same effect as the button.

### M8 — Enter with no valid recipe
Clear the grid, then press **Enter**.
→ Nothing is sent. The Save button is disabled, and the key honours that state.

### M9 — Escape goes back to the terminal
In the editor, press **Escape**.
→ The terminal list comes back. A second Escape closes the terminal.

### M10 — Escape from the name field
Click into the name field, then press **Escape**.
→ The list comes back too. The player is never trapped in the field.

---

## H — Settings and real instance

### H1 — Configuration
In `run/config/packagedautoterminals.cfg`, set `machinesTab` to `false`.
→ The tab button disappears, and the title comes back.

### H2 — Real instance
Copy `build/libs/packagedautoterminals-1.12.2-0.1.0.jar` into
`H:\PrismLauncher\instances\cleanroom-0.5.17-alpha\minecraft\mods`.
→ The instance starts, and the terminal works with your AE2UEL fork.

---

## Where I expect trouble

In decreasing order of probability:

1. **The editor tabs** (D2): they go through real slots, and the synchronisation of their
   icons has never been observed.
2. **The amount box** (D8): its position comes from the clicked slot, with no guard against the
   screen edge.
3. **The wireless terminal** (G): the container rework has no test behind it.
4. **The JEI transfer** (D11): it relies on a PackagedAuto method that was never exercised.
5. **Locating** (C1): rendering in the world touches the OpenGL state.
