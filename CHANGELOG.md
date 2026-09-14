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

### Fixed

- Disconnection on a dedicated server when the terminal opened. The network channel name was
  21 characters long, over the 20 character limit of `CPacketCustomPayload`. See decision
  D37.
