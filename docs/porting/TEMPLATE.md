# Port to Minecraft <version>

Copy this file to `docs/porting/<version>.md` and fill it as the work advances. The batches and
the gates are defined in `docs/PORTING.md`.

## Identity

| Element | Value | Evidence |
|---|---|---|
| Minecraft | | |
| Loader and version | | |
| Build JDK | | |
| Mappings | | |
| AE2 artifact and version | | |
| PackagedAuto artifact and version | | |
| Recipe viewer and version | | |
| Optional addons present | | |
| API family (A, B, C, D) | | |

## P0 answers

Record the file path and the line for every answer. An answer without evidence does not count.

| # | Question | Answer | Evidence |
|---|---|---|---|
| 1 | Package Recipe Holder item, or recipes in the machine? | | |
| 2 | Where does the recipe list live? | | |
| 3 | Which interface exposes a recipe type? | | |
| 4 | Does AE2 expose a part API? | | |
| 5 | Does AE2 expose a grid scan by machine type? | | |
| 6 | Which menu class backs the Pattern Access Terminal? | | |
| 7 | Which recipe viewer API, and does the transfer handler fit? | | |
| 8 | Is there a wireless terminal API open to third parties? | | |

## Batch log

One block per batch. Add the date and the gate results.

### P0 — Reconnaissance

- State:
- Date:
- G1 build: n/a
- G2 launch: n/a
- Notes:

### P1 — Build skeleton and dev environment

- State:
- Date:
- G1 build:
- G2 launch:
- Deviations from `docs/PORTING.md`:

### P2 — Registration and presence in game

- State:
- Date:
- G3 tests A1, A2, A3:
- Notes:

### P3 — Read-only terminal

- State:
- Date:
- G3 tests B1 to B7, C1 to C3:
- Notes:

### P4 — Editing

- State:
- Date:
- G3 tests D and following:
- NBT comparison against a hand-encoded recipe:
- Notes:

### P5 — Wireless

- State:
- Date:
- G3:
- Notes:

### P6 — Recipe viewer integration

- State:
- Date:
- G3:
- Notes:

### P7 — Polish and release

- State:
- Date:
- G4 real instance:
- Jar name produced:

## Pieces dropped on this target

| Piece | Reason |
|---|---|
| | |

## Open problems

| # | Problem | Blocks | Next step |
|---|---|---|---|
| | | | |
