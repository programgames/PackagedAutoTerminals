# Changelog

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the
numbering follows [SemVer](https://semver.org/).

## [Unreleased]

## [0.1.0-beta.7] - 2026-09-18

A documentation release. No Java file changed.

### Fixed

- The tooltip of the terminal named the wrong mouse button, in the sixteen languages. It read
  "Right click a group to edit its recipes". A group row answers the left click, which opens the
  editor on a new recipe, and a recipe row answers the right click. Read in
  `GuiPatTerminal.mouseClicked`: `mouseButton == 0` on a group header sends `ACTION_NEW`, and
  `mouseButton == 1` needs a recipe under the pointer.
- The JEI description pages carried the same error, and they placed the crafter list above the
  orphan report. `GuiPatTerminal.buildMachineLines` adds the report first.

### Changed

- The README, the eighteen wiki pages and the in-game help of the sixteen languages are rewritten
  in plain English: short sentences, active voice, simple tenses, one word for one meaning.
- The wiki now states the four rules that build a group, the repair of a split group by the group
  name, the two sections of the Machines tab, the pin button of the Patterns tab, and the recipe
  of the wireless terminal. Each one was absent or wrong before.
- Two claims that no source in the repository supports are softened. The orphan recipe is a
  frequent mistake in game, not the most frequent one. An unpowered terminal darkens and lists no
  machine, instead of refusing to open.

## [0.1.0-beta.6] - 2026-09-18

A repair release. **0.1.0-beta.5 carried a regression**; use this one instead.

### Fixed

- **The output box of the editor stayed empty**, whatever the recipe. The client builds its editor
  with no recipe type and receives the type through `@GuiSync`, which arrives **after** the slot
  contents. Every arriving slot called `updateRecipeInfo`, and that method cleared the nine output
  slots whenever the type was unknown, so the client wiped the slots the server had just sent. The
  server never sent them again: it believed the client already had them.

  The same chain disabled **Save**, since the recipe then read as invalid on the client. Pressing
  it sent nothing and said nothing. When the type is unknown, the editor now clears the package
  preview and nothing else.
- The arrow between the grid and the outputs was drawn 21 pixels wide from 172, so it reached 192
  while the right column starts at 190. Its point sat under the first output slot and read as cut.
  The free band is twenty pixels; the arrow is now nineteen, with a pixel of air on each side.

## [0.1.0-beta.5] - 2026-09-18

A quality release. Five agents audited the mod; twelve findings survived an adversarial pass.

### Fixed

- **Every message carrying a number printed "Format error"** instead of the count. `Feedback`
  packed its arguments as text and handed the text back, while eleven language entries expect
  `%d`. Read in `Locale.formatMessage` of Forge 14.23.5.2847: the resulting
  `IllegalFormatConversionException` is caught and the line becomes `Format error: ...`. A token
  that reads as a whole number now comes back as a number.
- **The wireless terminal used from the offhand opened nothing**, or opened another terminal held
  in the main hand. The slot passed to the GUI handler was always the main-hand hotbar slot. The
  AE2WUT path had the same defect, and there it also stopped AE2WUT from opening its own screen.
- The terminal drew three of its own refusals in the colour of a success. Each screen carried its
  own list of keyword **substrings**, and the two had drifted. The list is now exact, and lives in
  `Feedback.isRefusal`.
- Saving replaced the recipe at a **position** in the group, not the recipe the editor opened. A
  second player editing the same group could make a save destroy a recipe nobody meant to touch.
  Every write now names the recipe, and refuses when the group no longer carries it.
- A group could be pushed past the twenty recipes a Package Recipe Encoder can show. Every
  addition now stops at `RecipeWriter.maxRecipes`, read from the PackagedAuto config at run time,
  and the creation tab disappears once the group is full.
- `PacketEditorFill` read its entry count from the wire and looped on it. A crafted packet could
  hold the decoding thread. The count is clamped to the size of the editor.

### Changed

- The `Feedback` separator is written as `` instead of a raw invisible byte.
- The GUI scale logic lived twice, copied byte for byte between the two screens. It moved into
  `GuiScaleFit`.
- Dead code removed: `GuiPatTerminal.stateOf`, and four Javadoc blocks that described another
  method.

## [0.1.0-beta.4] - 2026-09-18

### Fixed

- A fluid dragged from JEI into the editor wrote the **bucket** of that fluid, and the recipe
  never ran. It now writes an **AE2FC Fluid Packet**, which is what `PackagedFluidCrafting`
  itself writes, and what the Packager and the Crafter read back. The bucket stays as the
  fallback when that addon is absent.
- The package preview of the editor sat on a **red** background. AE2 paints every slot it judges
  invalid with a red veil. Removing the `isItemValid` override was not enough: `AppEngSlot`
  delegates that test to the inventory, which answers "not editable" for a preview slot. The slot
  now answers `Valid` itself.
- The output box of a craft recipe was a **black square**. The nine output slots stayed empty,
  and the "disabled" veil covered them. They now show the crafted result, centred, the way the
  Package Recipe Encoder does.
- The amount panel opened on a craft recipe, where the amount cannot move: PackagedAuto forces
  one item per cell. The panel no longer opens there, and a message says why.
- The green frame of the open tab flew off the screen, to the right. Its placement assumed a
  single row of tabs; the row has ten columns over two rows.
- The plate of the amount panel rendered at **40%** of its grey, so the dark text on it was
  unreadable. `GlStateManager` caches the last colour it set; the cache and OpenGL had drifted
  apart, and the call inside `drawRect` was skipped. The panel now resynchronises the two.
- Every button of the mod lost its bottom bevel. `GuiButton.drawButton` blits `height` rows from
  the **top** of a texture that is twenty rows tall, so any shorter button is cut. The new
  `PatButton` blits the top half from the top and the bottom half from the bottom.
- A slot holding a fluid packet showed a small **1**, the item count of the packet, instead of
  the amount of fluid. It now shows the amount, drawn by the AE2 `FluidStackSizeRenderer`, which
  counts in buckets and shrinks its own text so it never overflows the slot.
- The terminal and the editor did not fit a small Minecraft window. `ScaledResolution` only
  guarantees 320 by 240 logical pixels, and the editor is 338 tall. Both screens now take the GUI
  scale down while they are open, and give it back on closing. The setting is changed in memory
  only.
- A JEI tooltip covered the buttons of the amount panel. JEI now knows the panel rectangle,
  through `IAdvancedGuiHandler.getGuiExtraAreas`.

### Changed

- The package preview shows the **packages** the recipe produces, not the crafted items again.
  That is the rule of the Encoder.
- The tab arrows scroll by a whole row of ten, not by one slot. A one-slot step read as
  "the arrow deleted my last recipe". An arrow that cannot move is greyed out.

### Added

- Fluid amounts are edited in millibuckets. The amount panel, the wheel and the proportions all
  work on a fluid slot, up to one billion mB. The panel steps become 10, 100 and 1000 mB.

## [0.1.0-beta.3] - 2026-09-17

### Added

- Fourteen new translations: German, Spanish, Italian, Brazilian Portuguese, Dutch, Polish,
  Czech, Russian, Ukrainian, Turkish, Japanese, Korean and Traditional Chinese. With English,
  French and Simplified Chinese, the mod now ships sixteen languages.
- The Simplified Chinese file, contributed in #1, gains the keys added since.

### Fixed

- Simplified Chinese: `summary_machines` printed `%dd` instead of `%d`.

## [0.1.0-beta.2] - 2026-09-17

### Added

- Drag and drop from JEI into one slot of the editor, for items and for fluids. A fluid
  enters the grid as its filled bucket.
- Amount panel in the editor, opened with a left click on a filled slot, for the inputs and
  for the outputs. Six step buttons, a field, Set and Cancel.
- "Keep ratio" box in the amount panel. The new amount scales every other filled slot of the
  recipe. A ratio that does not divide every slot changes nothing, and says so.
- Config entry `highlightSeconds`: how long the machines stay marked in the world.

### Changed

- The left click on a filled slot of the editor opens the amount panel instead of emptying
  the slot. The right click still empties it, and so does the amount zero.
- The "locate" button is a map pin, and no longer an eye. An eye says "look"; a pin says
  "here it is".
- The mark in the world gains a tinted cube and a beam going up, and lasts fifteen seconds
  instead of five.
- The amount panel takes the look of the game: grey plate with the vanilla bevel, real
  vanilla buttons with their click sound, the item and its name on the title line, and a
  tick box drawn like a slot.

### Fixed

- The "Keep ratio" tick was invisible. `drawRect` was given a colour with no alpha byte, so
  it painted nothing. The box worked, and looked dead.
- Black square behind the "locate" button, in the terminal list.
- Dark veil over the machine icon of a group row.

## [0.1.0-beta.1] - 2026-09-15

### Added

- Wired terminal that lists the PackagedAuto machines of the ME network and their recipes.
- Detailed tooltip: inputs, outputs, machine position.
- Removal of a recipe from the terminal, with Shift and right click.
- Recipe editor, opened with a right click, following the Package Recipe Encoder layout.
- Startup diagnostics: registered recipe types, and translation check.
- Creation of a recipe from the terminal, taking a blank recipe holder from the ME network.
- Sending a recipe holder back to the network, with Shift and left click.
- Amount adjustment with the wheel, up to 4096.
- Recipe transfer from JEI into the editor.
- Search field: by machine, by type, by input or by output.
- Machines tab: crafters of the network, busy state, and **diagnostic of orphan recipes**.
- Configuration file: refresh interval, AE2WUT mode id, Machines tab, tooltip size.
- Crafting recipe: one ME terminal and one Package Recipe Holder.

### Changed

- Machines now group only when they carry **exactly** the same recipes. Sharing one recipe
  used to be enough, and soldered separate pairs into a single line of six machines or more.
  See decision D38.
- The search reads the **produced** items only. Typing `elite` no longer returns every recipe
  that merely consumes an Elite part. See decision D39.
- The crafting machine icon moved from each recipe row to the group row, where the Unpackager
  is. The recipe rows carry the short type name again.

### Fixed

- Disconnection on a dedicated server when the terminal opened. The network channel name was
  21 characters long, over the 20 character limit of `CPacketCustomPayload`. See decision
  D37.
- Missing chevron on the first row of the terminal. The row striping left the colour
  multiplier on an alpha of 0.078, and the alpha test discarded the whole icon.
- The Patterns button was cut by the frame of the screen.
