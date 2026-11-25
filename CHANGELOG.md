# Changelog

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the
numbering follows [SemVer](https://semver.org/).

## [Unreleased]

### Added

- **Edit a recipe holder with no ME network.** A machine that runs on its own, outside any
  network, could only change its recipes at a Package Recipe Encoder block. Sneak and right
  click with the wireless terminal, or click the Encoder icon in the tool strip of either
  terminal: the editor opens with two slots, a main holder and its twin. Save and Delete write
  to both at once, so a Packager and its Unpackager keep the same list. When the two lists
  differ, a Sync button copies every missing recipe into the holder that lacks it, and deletes
  nothing: a blank holder in the twin slot becomes a copy. The holders go back to the
  inventory when the screen closes. The `portableEncoder` setting turns the feature off.
- **Scale every recipe of a group at once.** A machine that gains a parallel upgrade needs all
  of its recipes multiplied, and the amount panel did one recipe at a time. The `×N` button of
  the editor multiplies or divides every processing recipe of the group, outputs included, on
  every machine, or on both holders in holder mode. Craft recipes are locked to one per cell,
  so they are skipped and counted. Nothing is written unless every recipe can be scaled: an
  amount that does not divide, an amount past the limit, or two recipes that would become one
  stop the whole operation, and the message names the recipe.

### Fixed

- **A new recipe keeps the type of the group.** The creation tab restarted on the default
  type every time, and in a pack that disables the Package Crafter, such as MeatballCraft,
  that default was Positioned Processing. A new recipe now takes the type of the tab the
  player came from, and the default falls back to Processing before anything else.
- **A click on the tab already open does nothing.** It used to reload the same recipe and
  resend the whole inventory, so the held item bobbed as if something had happened.
- **The terminal no longer crashes on a small window in some packs** (`this.search is null`).
  When the screen is taller than the window, the mod lowers the GUI scale and builds the
  screen again at the new size. In some packs that second build never ran, and the screen
  was drawn with no widget. The terminal and the editor now finish the build themselves.
- **An empty grid no longer reads "Invalid recipe".** A new recipe opened on that warning
  before the player had placed a single item.
- **The terminal keeps GUI scale 3 on a 1080p screen.** It was 367 pixels tall, seven more
  than that scale leaves, so the game dropped to scale 2 each time it opened. It now shows
  ten rows instead of twelve and is 331 pixels tall.
- **The holder editor speaks the language of the player.** The Holder label stayed in English
  in ten languages and the Sync button in nine, and a single scaled recipe read "1 recipes
  scaled".

## [0.1.0-beta.9] - 2026-10-01

A repair and performance release. It also adds the one thing the mod promised and did not do:
a machine with no recipe holder is served from the ME network.

### Added

- **One gesture makes the two sides of a group equal again.** PackagedAuto needs the same
  recipe in the Packager and in the Unpackager, and the terminal knew how to say that a recipe
  sat on one side only. Fixing it meant opening that recipe and pressing Save, once per
  recipe: a pair that gained a new Unpackager cost twenty openings. The button of a hovered
  group row now shows two turning arrows when the sides have drifted apart, and one click
  copies every missing recipe where it belongs.
- **One button puts the recipe holders in, and takes them out.** Hovering a group row shows
  an arrow: it points into the box while a machine of the group waits for a holder, and out
  of it once they all have one. A new pair used to mean walking to both machines with a
  holder in hand. The holders come from the ME network, so the AE2 energy and security rules
  apply, and only a blank holder ever leaves storage. The `pullHolderFromNetwork` setting does
  not reach this button: that setting governs the holder taken on its own while a recipe is
  saved, and a click is a decision the player has just made.
- **The terminal takes a blank Package Recipe Holder out of the ME network** when a machine
  has none. Until now it answered "insert a holder first", and the player had to walk to the
  machine, which is the walk this mod exists to remove. The extraction goes through
  `NetworkItems.extractOne`, hence through the AE2 energy and security rules. The prototype
  carries no NBT, so only a **blank** holder ever matches: a holder that already lists recipes
  stays in storage. The holder is taken when the recipe is saved, never when the editor opens,
  so an editor closed without saving costs nothing. The new config entry
  `pullHolderFromNetwork` turns the behaviour off, and the old message comes back.
- Unit tests, which the project had none of, against working rule 6 of `CLAUDE.md`.
  Thirty-eight of them cover the grouping of the machines, the search syntax, the packing of
  the messages and the storage of the group names. They run without Minecraft, and
  `gradlew build` runs them.

### Fixed

- **A group row said nothing about the three faults that stop it.** It showed the recipe count
  and nothing else once both roles were there. A Packager that had lost its channel, a machine
  whose recipe holder someone had taken out, and a pair whose two sides had drifted apart all
  read as a healthy "6 recipes" while nothing ran. Each one now takes the chip, in red, and
  the tooltip names the machine at fault.
- **Every refusal of a permission was silent.** Eight checks of `SecurityPermissions.BUILD`
  returned without a word. On a server a player without that permission clicked a row, pressed
  Save, pressed delete, and nothing happened and nothing was said.
- **Saving after the machine was broken was silent too.** `save`, `renameGroup` and
  `deleteCurrent` returned without a message when the group had left the network.
- **A partial return of the recipe holders hid its own count.** When the network refused one
  holder, the terminal said only "the network cannot store the holder", while half of the
  machines had in fact been emptied.
- **Removing a recipe listed twice in one holder took two clicks.** `RecipeWriter` removed the
  first copy only, and the terminal shows the distinct recipes, so the row stayed with nothing
  telling the player why.
- **Nothing warned that naming two groups alike joins them.** The name is the explicit link
  between two groups, and the merge is the feature; the rename now says so before it happens.
- **The missing role message did not mention unloaded chunks.** The terminal lists what the
  AE2 grid holds, and a machine in a chunk Minecraft has unloaded leaves that grid while the
  block stays in the world. The message now says so.
- **The pin closed the screen on nothing when the machines were in another dimension.** It now
  says where they are and stays open.
- **A full group could no longer be edited at all.** A left click on the group row sends
  `ACTION_NEW`, and `ContainerPatTerminal.newRecipe` refused to open the editor once the group
  held twenty recipes. The editor is the only screen that deletes a recipe, so the player
  could not free a slot. The terminal now opens the editor on the first recipe of the group,
  and says that the group is full in the action bar. The cap on **creation** does not move.
- **The group names could erase each other, and two dimensions shared one entry.**
  `WorldServer.init` builds `this.mapStorage = new MapStorage(this.saveHandler)` for every
  dimension, over the **same** `data` folder. Two dimensions therefore held two instances of
  the storage over one file, and the last one to save dropped the names of the other. The key
  was also the position alone, so a Packager at 100, 64, 100 in the Nether shared its name
  with one at 100, 64, 100 in the overworld. Everything now goes through the overworld
  storage, and the key carries the dimension. An entry written before this release is read as
  dimension zero.
- **The editor read the group name in the world of the player**, not in the world of the
  machine. A network that crosses a Quantum Bridge showed an empty name.
- **A rename packet could announce a name of any length.** `ByteBufUtils.readUTF8String` reads
  the length the packet gives, then that many bytes. The length is now clamped before the read.
  The name is also stripped of the section sign, which starts a colour code, and of the control
  characters, one of which is the feedback separator.
- **A write that filled one machine and not the other was called a success.**
  `RecipeWriter.Result.full` was counted and never shown. A pair whose two sides had drifted,
  one at twenty recipes and the other below, took the write on one machine only.
- **"Applied to 0 machines" was drawn in the colour of a success.** A removal that changed
  nothing now says so, and in the colour of a refusal.

### Changed

- **The terminal gets a title band, a tab strip and a status bar.** A single button used to
  toggle the two views, and the title disappeared behind it. Two tabs now carry an item icon
  and a label, the open one raised and the other sunken, the way AE2 draws its own. The title
  stays in a band of its own, with the search field. The counts moved into a shallow recess
  under the list, instead of floating on the panel. The screen grows from 338 to 367 pixels
  tall; `GuiScaleFit` already handles a small window. Two pixels of panel separate the band
  from the strip and three separate the strip from the list, four separate the two tabs, and
  the icon and the label of a tab each keep their air. Without that the three bands read as
  one block and the two tabs read as one wide tab cut in half. The open tab is a closed box
  like the other one: it was drawn with no bottom edge, so it would merge into what sits under
  it, and three pixels of panel separate the strip from the list, so it merged with nothing.
  A tab open at the bottom reads as a drawing left unfinished. The title band is twenty rows
  instead of eighteen: the search field needs fourteen with its border, and the frame bevel of
  the window owns the first three rows, so the field had to start on the bevel and its top
  edge was drawn outside the band. The counts of the status bar keep four pixels from its
  border, as every other text of the screen does.
- **The search field says what it is for, and whether it is listening.** A magnifier sits
  inside it, and a light ring surrounds it while it has focus. It was a plain dark rectangle.
- **The list shows its own hierarchy.** A rule closes each group header, a guide runs in front
  of the recipes of an open group, and a rule follows each section title of the Machines tab.
  The chevron used to be the only sign that a row belonged to the row above.
- **An empty screen teaches instead of stating.** A faded package, the reason, and a second
  line that says what to do next. There was one grey sentence.
- **The gestures of a row are drawn on the row.** Hovering a group shows a button that sends
  its recipe holders back to the network; hovering a recipe shows a pencil and a cross. The
  three gestures existed, and only the wiki named them. The buttons take the place of the
  state chip while the pointer is on the row, so no column moves.
- **A sort order and a filter.** Two buttons at the right of the tab strip: one cycles through
  name, machine and recipe count, the other keeps only the rows that need the player. On a
  network of twenty machines, reading every row to find the wrong one cost more than the
  terminal saves.
- **The state of a row is a chip, not grey text.** A tinted plate with a border, red when the
  player has something to fix. It gives the eye a shape to catch instead of a word to read.
- **The Machines tab says something useful on each row.** It read "idle" on every line, which
  taught the player nothing: `TileCrafter.isBusy` answers true while a craft runs or while
  items sit in the input slots, a craft lasts a handful of ticks, and the terminal samples
  that state once a second. A row now shows, in this order, "inactive" when the machine has no
  channel, "busy" when it really is, "no recipe" when no encoded recipe of the network targets
  it, and otherwise the number of recipes it can run. The first and the third are drawn in the
  warning colour, because the player can act on them. A machine that no recipe targets is the
  mirror of the orphan report above the list.
- **The footer of the Machines tab counted the wrong machines.** The same line was drawn on
  both tabs, and it always counted the Packagers, the Unpackagers and the Packaging Providers.
  It announced twenty two machines under a list of eleven crafters. On the Machines tab it now
  counts the crafters.
- **The terminal list is built once per change, not once per frame.** `drawFG` called
  `buildLines`, which regroups the machines by comparing their recipes two by two, then formats
  every row. On a network of fifty machines carrying twenty recipes each, the screen paid for
  tens of thousands of recipe comparisons per second, for a list that changes about once a
  second. The rows are now rebuilt only when the snapshot, the search, the tab or the unfolded
  groups move.
- **One click costs one recipe rebuild, not ninety.** Every slot write called
  `EditorInventory.updateRecipeInfo`, which resolves a crafting recipe against the whole
  registry. A JEI transfer fills up to 81 slots, and a change of amount that keeps the
  proportions rewrites every filled slot.
- **One click walks the ME grid once, not four times.** Opening the editor used to search the
  machine, count the recipes, then search and scan again inside `openEditor`. A write also
  searched the whole grid once per target machine; the snapshot now carries the machine on the
  server.
- The search parser moved out of the screen into `SearchQuery`, where it is tested. A lone
  `@` or `#`, which is a search being typed, no longer counts as a word to look for.
- `.github/workflows/release.yml` builds the jar itself, publishes the GitHub release from the
  CHANGELOG section, and sends the file to CurseForge. `tools/release.sh` now only runs the
  checks and pushes the tag. The previous version said GitHub could not build this mod, which
  was wrong: `libs/maven` is versioned.

### Removed

- Dead code: `ProviderScanner.translate`, which nobody called and which answered in the
  language of the server, the language key `new_recipe`, which no screen read, one duplicate
  import and ten unused ones.

## [0.1.0-beta.8] - 2026-09-20

A packaging release. No game behaviour changed.

### Added

- `update.json` at the root of the repository, and `updateJSON` on the `@Mod` annotation. Forge
  now marks the mod as outdated in the mod list when a newer version is published.
- `tools/release.sh`: builds the jar, checks the version, the CHANGELOG section and the JDK,
  tags the commit, then creates the GitHub release with the jar attached.
- `.github/workflows/release.yml`: sends the jar of a published GitHub release to CurseForge.
  The build itself stays local, because `libs/maven` is not versioned.
- `docs/CURSEFORGE.md`: the description to paste on the CurseForge project page.
- `src/main/resources/logo.png`, shown by Forge in the mod list, and
  `docs/assets/curseforge-avatar.png`. Both are built by `tools/make_logo.py` from the
  part face textures and the palette of the wireless terminal icon.

### Changed

- `mcmod.info` carries the project URL, the update URL, the logo file, the credits and the
  required mods. Those fields were empty.
- The `@Mod` dependency line states the minimum PackagedAuto version, `1.0.24.73`. AE2UEL keeps
  no bound: it reports its version as `v0.56.5`, and the leading letter breaks a Maven range.

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
