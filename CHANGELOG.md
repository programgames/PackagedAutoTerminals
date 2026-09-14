# Changelog

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the
numbering follows [SemVer](https://semver.org/).

## [Unreleased]

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
