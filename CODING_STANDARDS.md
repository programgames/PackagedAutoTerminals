# Coding standards

Five rules of judgement. No tool checks them: the review does. Each one comes from a defect that
the audit of 2026-11-25 found more than once (`docs/audit/RAPPORT.md`, section 9).

| Rule | In the code |
|---|---|
| A comment gives the rule and its evidence (`file:line`), never the story of the bug. The story goes into `CHANGELOG.md`. The older comments of `src/main/java` that tell the story of a bug (`FIXED`, `used to`) precede this rule: a change that touches one rewrites it. | — |
| A packet names a recipe by its identity and a machine by its position in the world, never by a list index. | `RecipeWriter.resolve` |
| A gesture that removes a recipe or a holder from a machine asks for a second click, and never answers a right click alone. A gesture that adds needs no second click. One exception: Delete in the editor removes the recipe at once, because the grid keeps the recipe and Save writes it back. A right click that empties a ghost slot of the editor writes nothing into a machine, so it is not a removal. | `SecondClick`, `ContainerPatEditor.deleteCurrent` |
| A write always decides on a fresh scan. A cache serves the display only. | `ContainerPatEditor.scannedGroup` |
| A line of `CHANGELOG.md` or of the wiki gives the exact condition of the behaviour, checked in the code, never in the commit message. | — |
