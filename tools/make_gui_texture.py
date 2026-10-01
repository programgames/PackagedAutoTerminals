"""Generates the GUI sheets of the mod.

Reason: we first reused the AE2 `newinterfaceterminal.png`. Its geometry is made for the
Interface Terminal, and pasted onto our screen it produced grey rectangles across the text.
So we draw our own, whose every coordinate we know.

The palette follows AE2 and the game, so the screens stay native:
  198 panel | 224 list area | 139 slots | 55 borders | 255 highlights

PITFALL fixed: each sheet has **its own** inventory position. A shared constant had shifted
the editor inventory the day the terminal one was re-centred.

Usage:
    python tools/make_gui_texture.py
"""

import os
import struct
import zlib

BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
PANEL = (198, 198, 198, 255)
LIST = (224, 224, 224, 255)
SLOT = (139, 139, 139, 255)
EDGE = (55, 55, 55, 255)
SHADOW = (85, 85, 85, 255)
ARROW = (120, 120, 120, 255)
ICON = (170, 170, 170, 255)
NONE = (0, 0, 0, 0)
# Background of the input fields. Dark on purpose: game text is always drawn with a drop
# shadow, which on a light background reads as a second, offset letter.
FIELD = (26, 26, 26, 255)

# Title band, one shade under the panel, and tab strip. The band separates the chrome from
# the list: before it, the title, the search and the counts floated on the panel with nothing
# telling them apart.
BAND = (186, 186, 186, 255)
TAB_OFF = (166, 166, 166, 255)
STATUS = (210, 210, 210, 255)

SLOT_PITCH = 18
HOTBAR_GAP = 58

# --- Terminal ---------------------------------------------------------------------
SHEET = 512
WIDTH = 320
# The list is drawn for the maximum row count. The game only shows what the screen height
# allows, and picks the bottom of the sheet up right below.
MAX_ROWS = 16
FOOTER = 104

LIST_LEFT = 8
# Title band from 0 to 18, tab strip from 18 to 40, then the list.
# The band is twenty tall, not eighteen. The search field needs fourteen rows with its
# border, and the frame bevel of the window takes the first three of the screen: at eighteen
# the field had to start on the bevel, and its top edge sat outside the band.
BAND_HEIGHT = 20
# Two pixels of panel between the band and the strip, three between the strip and the list.
# Without them the three bands read as one block, and the tabs looked glued to the title.
TAB_STRIP_TOP = BAND_HEIGHT + 2
TAB_HEIGHT = 22
TAB_WIDTH = 92
LIST_TOP = TAB_STRIP_TOP + TAB_HEIGHT + 3
LIST_WIDTH = 288
ROW_HEIGHT = 18
LIST_HEIGHT = MAX_ROWS * ROW_HEIGHT
HEIGHT = LIST_TOP + LIST_HEIGHT + FOOTER

# Status bar: a shallow recess under the list, carrying the counts.
STATUS_LEFT = LIST_LEFT
STATUS_WIDTH = 304
STATUS_HEIGHT = 12

SCROLL_LEFT = 300
SCROLL_WIDTH = 12

SEARCH_LEFT = 150
SEARCH_TOP = 5
SEARCH_WIDTH = 146
SEARCH_HEIGHT = 12

TERMINAL_INVENTORY_LEFT = 79
TERMINAL_INVENTORY_TOP = LIST_TOP + LIST_HEIGHT + 22

# "Locate" icon, stored right of the screen, in the free area of the sheet.
# It is a map pin, and no longer an eye: an eye says "look", a pin says "here it is".
LOCATE_U = 330
LOCATE_V = 4
LOCATE_SIZE = 12

# Collapse chevrons, stored below the pin. Two drawings: collapsed group, expanded group.
CHEVRON_U = 330
CHEVRON_V = 20
CHEVRON_SIZE = 8

# The two states of a tab, stored right of the screen. The screen blits the one it needs,
# which is how the AE2 tab buttons work.
TAB_OFF_U = 330
TAB_OFF_V = 40
TAB_ON_U = 330
TAB_ON_V = 40 + TAB_HEIGHT + 2

# Small glyphs, on one line under the tabs.
GLYPH_V = 90
SEARCH_ICON_U = 330
SEARCH_ICON_SIZE = 8
EJECT_U = 342
INSERT_U = 414
PENCIL_U = 356
CROSS_U = 370
ACTION_SIZE = 10
SORT_U = 384
FILTER_U = 400
TOOL_SIZE = 12

# The drawing of an empty screen, larger, under the glyphs.
EMPTY_U = 330
EMPTY_V = 108
EMPTY_SIZE = 32

# --- Editor -----------------------------------------------------------------------
# The sheet is 512 by 512: the screen exceeds 256 pixels in both directions.
EDITOR_SHEET = 512
EDITOR_WIDTH = 258
EDITOR_HEIGHT = 338

NAME_LEFT = 8
NAME_TOP = 4
NAME_WIDTH = 162
NAME_HEIGHT = 16

TAB_LEFT = 8
TAB_TOP = 32
TAB_COLUMNS = 10
TAB_ROWS = 2
TAB_COUNT = TAB_COLUMNS * TAB_ROWS

GRID_LEFT = 8
GRID_TOP = 72

RIGHT_COLUMN = 190
OUTPUT_TOP = 112
PREVIEW_TOP = 172

# FIXED: the arrow used to start at 172 and to be 21 pixels wide, so it reached 192 while the
# right column starts at 190. Its point was drawn under the first output slot, and read as cut.
# The free band between the grid, which ends at 169, and that column is twenty pixels wide, so
# the arrow is now twenty wide and starts where the band starts.
EDITOR_ARROW_LEFT = 170
EDITOR_ARROW_TOP = 144
# Shaft length. Shaft plus the seven rows of the point must stay inside the band.
EDITOR_ARROW_SHAFT = 12

EDITOR_INVENTORY_LEFT = 20
EDITOR_INVENTORY_TOP = 256


def sheet(width, height):
    return [[NONE for _ in range(width)] for _ in range(height)]


def fill(px, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if 0 <= y < len(px) and 0 <= x < len(px[0]):
                px[y][x] = color


def recess(px, x0, y0, w, h, color):
    """Recesses an area: dark border on top and left, highlight on bottom and right."""
    fill(px, x0, y0, w, h, color)
    fill(px, x0 - 1, y0 - 1, w + 1, 1, EDGE)
    fill(px, x0 - 1, y0 - 1, 1, h + 1, EDGE)
    fill(px, x0, y0 + h, w + 1, 1, WHITE)
    fill(px, x0 + w, y0, 1, h + 1, WHITE)


def frame(px, width, height):
    """Panel and bevel, like the game screens."""
    fill(px, 0, 0, width, height, PANEL)
    fill(px, 0, 0, width, 1, BLACK)
    fill(px, 0, 0, 1, height, BLACK)
    fill(px, 1, 1, width - 2, 2, WHITE)
    fill(px, 1, 1, 2, height - 2, WHITE)
    fill(px, width - 3, 1, 2, height - 1, SHADOW)
    fill(px, 1, height - 3, width - 1, 2, SHADOW)
    fill(px, width - 1, 0, 1, height, BLACK)
    fill(px, 0, height - 1, width, 1, BLACK)


def player_inventory(px, left, top):
    for row in range(3):
        for column in range(9):
            recess(px, left + column * SLOT_PITCH, top + row * SLOT_PITCH, 16, 16, SLOT)
    for column in range(9):
        recess(px, left + column * SLOT_PITCH, top + HOTBAR_GAP, 16, 16, SLOT)


def draw_pin(px, x0, y0):
    """Map pin: a round head with a hole, a tail down to a point, and a ground shadow.

    The eye that came before it read as "watch", which is not what the button does. A pin
    is the mark every map uses for "the thing you look for is here".

    Each row lists the filled columns. The hole is cut afterwards, so the head reads as a
    ring and not as a blob.
    """
    body = {
        0: (4, 7),
        1: (3, 8),
        2: (2, 9),
        3: (2, 9),
        4: (2, 9),
        5: (2, 9),
        6: (2, 9),
        7: (3, 8),
        8: (4, 7),
        9: (5, 6),
        10: (5, 6),
    }
    hole = {3: (5, 6), 4: (4, 7), 5: (4, 7), 6: (5, 6)}

    for row, (left, right) in body.items():
        for x in range(left, right + 1):
            fill(px, x0 + x, y0 + row, 1, 1, EDGE)
    for row, (left, right) in hole.items():
        for x in range(left, right + 1):
            fill(px, x0 + x, y0 + row, 1, 1, NONE)

    # The shadow places the pin on the ground. Without it the drawing floats.
    fill(px, x0 + 4, y0 + 11, 4, 1, ICON)


def draw_chevron_right(px, x0, y0):
    """Chevron pointing right: the group is collapsed."""
    for step in range(4):
        fill(px, x0 + 2 + step, y0 + step, 1, 7 - 2 * step, EDGE)


def draw_chevron_down(px, x0, y0):
    """Chevron pointing down: the group is expanded."""
    for step in range(4):
        fill(px, x0 + step, y0 + 2 + step, 7 - 2 * step, 1, EDGE)


def draw_arrow(px, x0, y0):
    """Arrow pointing right, like the Encoder one."""
    fill(px, x0, y0 + 4, EDITOR_ARROW_SHAFT, 6, ARROW)
    for step in range(7):
        fill(px, x0 + EDITOR_ARROW_SHAFT + step, y0 + step, 1, 14 - 2 * step, ARROW)


def draw_package(px, x0, y0):
    """Package outline, drawn at the bottom of the preview slots."""
    points = [(5, 2), (10, 2), (13, 6), (13, 10), (8, 14), (3, 10), (3, 6)]
    for index in range(len(points)):
        ax, ay = points[index]
        bx, by = points[(index + 1) % len(points)]
        steps = max(abs(bx - ax), abs(by - ay), 1)
        for step in range(steps + 1):
            fill(px, x0 + ax + (bx - ax) * step // steps,
                 y0 + ay + (by - ay) * step // steps, 1, 1, ICON)


def draw_tab(px, x0, y0, active):
    """One tab of the strip, in its two states.

    Both are closed boxes. The active one takes the colour of the panel and a raised bevel,
    so it reads as the sheet in front; the inactive one is a shade darker and keeps a shadow
    under its edge, so it reads as a sheet behind.

    FIXED: the active tab used to be drawn with no bottom edge, so it would merge into what
    sits below it. Three pixels of panel separate the strip from the list, so it merged with
    nothing, and a tab open at the bottom reads as a drawing left unfinished.
    """
    body = PANEL if active else TAB_OFF
    fill(px, x0, y0, TAB_WIDTH, TAB_HEIGHT, body)
    # Outer edge, on three sides. The bottom stays open on the active tab.
    fill(px, x0, y0, TAB_WIDTH, 1, EDGE)
    fill(px, x0, y0, 1, TAB_HEIGHT, EDGE)
    fill(px, x0 + TAB_WIDTH - 1, y0, 1, TAB_HEIGHT, EDGE)
    # Inner bevel: light where the light comes from, shadow opposite.
    fill(px, x0 + 1, y0 + 1, TAB_WIDTH - 2, 1, WHITE)
    fill(px, x0 + 1, y0 + 1, 1, TAB_HEIGHT - 2, WHITE)
    fill(px, x0 + TAB_WIDTH - 2, y0 + 2, 1, TAB_HEIGHT - 3, SHADOW)
    fill(px, x0, y0 + TAB_HEIGHT - 1, TAB_WIDTH, 1, EDGE)
    if not active:
        # The tab behind keeps a shadow inside its edge, which sinks it one step further.
        fill(px, x0 + 1, y0 + TAB_HEIGHT - 2, TAB_WIDTH - 2, 1, SHADOW)


def draw_magnifier(px, x0, y0):
    """Magnifier, eight by eight: a ring and a handle. It says what the field is for."""
    ring = [(2, 0), (3, 0), (1, 1), (4, 1), (0, 2), (5, 2), (0, 3), (5, 3),
            (1, 4), (4, 4), (2, 5), (3, 5)]
    for x, y in ring:
        fill(px, x0 + x, y0 + y, 1, 1, ICON)
    for step in range(3):
        fill(px, x0 + 4 + step, y0 + 4 + step, 1, 1, ICON)


def draw_eject(px, x0, y0):
    """An arrow leaving an open box: the recipe holder goes back to the network."""
    arrow = {0: (4, 5), 1: (3, 6), 2: (2, 7), 3: (4, 5), 4: (4, 5), 5: (4, 5)}
    for row, (left, right) in arrow.items():
        fill(px, x0 + left, y0 + row, right - left + 1, 1, EDGE)
    for row in range(6, 9):
        fill(px, x0, y0 + row, 1, 1, EDGE)
        fill(px, x0 + ACTION_SIZE - 1, y0 + row, 1, 1, EDGE)
    fill(px, x0, y0 + 9, ACTION_SIZE, 1, EDGE)


def draw_insert(px, x0, y0):
    """An arrow entering an open box: the recipe holders come from the network."""
    shaft = {0: (4, 5), 1: (4, 5), 2: (4, 5), 3: (2, 7), 4: (3, 6), 5: (4, 5)}
    for row, (left, right) in shaft.items():
        fill(px, x0 + left, y0 + row, right - left + 1, 1, EDGE)
    for row in range(6, 9):
        fill(px, x0, y0 + row, 1, 1, EDGE)
        fill(px, x0 + ACTION_SIZE - 1, y0 + row, 1, 1, EDGE)
    fill(px, x0, y0 + 9, ACTION_SIZE, 1, EDGE)


def draw_pencil(px, x0, y0):
    """Pencil: the row is edited."""
    for step in range(6):
        fill(px, x0 + 2 + step, y0 + 6 - step, 2, 1, EDGE)
    fill(px, x0 + 1, y0 + 7, 2, 2, EDGE)
    fill(px, x0 + 1, y0 + 9, 1, 1, ICON)


def draw_cross(px, x0, y0):
    """Cross: the row is removed."""
    for step in range(7):
        fill(px, x0 + 2 + step, y0 + 1 + step, 1, 1, EDGE)
        fill(px, x0 + 8 - step, y0 + 1 + step, 1, 1, EDGE)


def draw_sort(px, x0, y0):
    """Three bars of falling length, next to a down arrow: the sort order."""
    fill(px, x0 + 1, y0 + 2, 6, 1, EDGE)
    fill(px, x0 + 1, y0 + 5, 4, 1, EDGE)
    fill(px, x0 + 1, y0 + 8, 2, 1, EDGE)
    fill(px, x0 + 9, y0 + 1, 1, 8, EDGE)
    for step in range(3):
        fill(px, x0 + 7 + step, y0 + 8 - step, 1, 1, EDGE)
        fill(px, x0 + 11 - step, y0 + 8 - step, 1, 1, EDGE)


def draw_filter(px, x0, y0):
    """An exclamation mark inside a frame: show only what needs attention."""
    fill(px, x0 + 1, y0 + 1, TOOL_SIZE - 2, 1, EDGE)
    fill(px, x0 + 1, y0 + TOOL_SIZE - 2, TOOL_SIZE - 2, 1, EDGE)
    fill(px, x0 + 1, y0 + 1, 1, TOOL_SIZE - 2, EDGE)
    fill(px, x0 + TOOL_SIZE - 2, y0 + 1, 1, TOOL_SIZE - 2, EDGE)
    fill(px, x0 + 5, y0 + 3, 2, 4, EDGE)
    fill(px, x0 + 5, y0 + 8, 2, 2, EDGE)


def draw_empty_glyph(px, x0, y0):
    """A large package outline, for the screen that lists nothing."""
    points = [(11, 3), (21, 3), (28, 11), (28, 21), (16, 29), (4, 21), (4, 11)]
    for index in range(len(points)):
        ax, ay = points[index]
        bx, by = points[(index + 1) % len(points)]
        steps = max(abs(bx - ax), abs(by - ay), 1)
        for step in range(steps + 1):
            fill(px, x0 + ax + (bx - ax) * step // steps,
                 y0 + ay + (by - ay) * step // steps, 1, 1, ICON)
    # The seam of the lid, so the shape reads as a package and not as a stone.
    fill(px, x0 + 6, y0 + 10, 20, 1, ICON)


def build_terminal():
    px = sheet(SHEET, SHEET)
    frame(px, WIDTH, HEIGHT)
    # The icon lives right of the screen, in the free area of the sheet.
    draw_pin(px, LOCATE_U, LOCATE_V)
    draw_chevron_right(px, CHEVRON_U, CHEVRON_V)
    draw_chevron_down(px, CHEVRON_U + 10, CHEVRON_V)
    draw_tab(px, TAB_OFF_U, TAB_OFF_V, False)
    draw_tab(px, TAB_ON_U, TAB_ON_V, True)
    draw_magnifier(px, SEARCH_ICON_U, GLYPH_V)
    draw_eject(px, EJECT_U, GLYPH_V)
    draw_insert(px, INSERT_U, GLYPH_V)
    draw_pencil(px, PENCIL_U, GLYPH_V)
    draw_cross(px, CROSS_U, GLYPH_V)
    draw_sort(px, SORT_U, GLYPH_V)
    draw_filter(px, FILTER_U, GLYPH_V)
    draw_empty_glyph(px, EMPTY_U, EMPTY_V)

    # The title band, one shade under the panel, closed by a shadow line.
    fill(px, 3, 3, WIDTH - 6, BAND_HEIGHT - 3, BAND)
    fill(px, 3, BAND_HEIGHT - 1, WIDTH - 6, 1, SHADOW)
    fill(px, 3, BAND_HEIGHT, WIDTH - 6, 1, WHITE)

    recess(px, SEARCH_LEFT, SEARCH_TOP, SEARCH_WIDTH, SEARCH_HEIGHT, FIELD)
    recess(px, LIST_LEFT, LIST_TOP, LIST_WIDTH, LIST_HEIGHT, LIST)
    recess(px, SCROLL_LEFT, LIST_TOP, SCROLL_WIDTH, LIST_HEIGHT, SLOT)
    recess(px, STATUS_LEFT, LIST_TOP + LIST_HEIGHT + 4, STATUS_WIDTH, STATUS_HEIGHT, STATUS)
    player_inventory(px, TERMINAL_INVENTORY_LEFT, TERMINAL_INVENTORY_TOP)
    return px


def build_editor():
    px = sheet(EDITOR_SHEET, EDITOR_SHEET)
    frame(px, EDITOR_WIDTH, EDITOR_HEIGHT)

    recess(px, NAME_LEFT + 2, NAME_TOP + 2, NAME_WIDTH - 4, NAME_HEIGHT - 4, FIELD)

    for row in range(TAB_ROWS):
        for column in range(TAB_COLUMNS):
            recess(px, TAB_LEFT + column * SLOT_PITCH, TAB_TOP + row * SLOT_PITCH,
                   16, 16, SLOT)

    for row in range(9):
        for column in range(9):
            recess(px, GRID_LEFT + column * SLOT_PITCH, GRID_TOP + row * SLOT_PITCH,
                   16, 16, SLOT)

    draw_arrow(px, EDITOR_ARROW_LEFT, EDITOR_ARROW_TOP)

    for row in range(3):
        for column in range(3):
            recess(px, RIGHT_COLUMN + column * SLOT_PITCH, OUTPUT_TOP + row * SLOT_PITCH,
                   16, 16, SLOT)
            x = RIGHT_COLUMN + column * SLOT_PITCH
            y = PREVIEW_TOP + row * SLOT_PITCH
            recess(px, x, y, 16, 16, SLOT)
            draw_package(px, x, y)

    player_inventory(px, EDITOR_INVENTORY_LEFT, EDITOR_INVENTORY_TOP)
    return px


def write_png(path, px):
    height = len(px)
    width = len(px[0])
    raw = b""
    for row in px:
        raw += b"\x00" + b"".join(bytes(c) for c in row)

    def chunk(kind, data):
        return (struct.pack(">I", len(data)) + kind + data
                + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff))

    blob = (b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9))
            + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(blob)


if __name__ == "__main__":
    base = os.path.join("src", "main", "resources", "assets", "packagedautoterminals",
                        "textures", "guis")
    write_png(os.path.join(base, "pat_terminal.png"), build_terminal())
    print("Terminal", WIDTH, "x", HEIGHT, "| up to", MAX_ROWS, "rows")
    write_png(os.path.join(base, "pat_editor.png"), build_editor())
    print("Editor", EDITOR_WIDTH, "x", EDITOR_HEIGHT, "on a", EDITOR_SHEET, "sheet")
