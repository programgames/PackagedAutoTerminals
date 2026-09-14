# -*- coding: utf-8 -*-
"""Generates the part and item textures, the AE2 way.

Lesson learned from comparing with the AE2 `pattern_terminal_*.png`: the three layers of a
part **carry no colour at all**. They are white masks on a transparent background. The colour
comes from the network, through tint indexes 1, 2 and 3, which the game replaces with the
three shades of the cable colour. Our old textures were painted in hard, opaque turquoise:
they ignored the network colour, and they rendered as a black square in game.

The drawing follows the AE2 frame — border, separator, four dark corners — and changes the
single symbol: a package with its tape, instead of the hollow AE2 square.

Usage:
    python tools/make_part_texture.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png

SIZE = 16
NONE = (0, 0, 0, 0)
MASK = (255, 255, 255, 255)

# --- Palette of the wireless item, read from the AE2 `wireless_pattern_terminal.png` ---
PURPLE = (97, 78, 171, 255)
WHITE = (255, 255, 255, 255)
BEZEL = (76, 72, 65, 255)
BEZEL_DARK = (53, 49, 41, 255)
EDGE_LIGHT = (122, 122, 122, 255)
PINK = (255, 128, 215, 255)
STEM = (64, 64, 64, 255)
SCREEN_OFF = (51, 46, 37, 255)


def sheet():
    return [[NONE for _ in range(SIZE)] for _ in range(SIZE)]


def put(px, points, color=MASK):
    for x, y in points:
        px[y][x] = color


def span(x0, x1, y):
    return [(x, y) for x in range(x0, x1 + 1)]


def column(x, y0, y1):
    return [(x, y) for y in range(y0, y1 + 1)]


def box(x0, y0, x1, y1):
    return [(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)]


# --- Shared frame, in texture coordinates -----------------------------------------
# The model shows the texture from 2 to 13. The drawing therefore fits in 3..12, as in AE2.
FRAME = (span(4, 11, 3) + span(4, 11, 12)
         + column(3, 4, 11) + column(12, 4, 11)
         + column(9, 4, 11) + span(3, 12, 9))
CORNERS = [(3, 3), (12, 3), (3, 12), (12, 12)]

# The package: lit outline, and tape across it.
PARCEL_BRIGHT = (span(4, 8, 4) + span(4, 8, 6) + span(4, 8, 8)
                 + [(4, 5), (8, 5), (4, 7), (8, 7)])
PARCEL_FILL = [(5, 5), (6, 5), (7, 5), (5, 7), (6, 7), (7, 7)]

# The three panels on the right and at the bottom, as in AE2.
SIDE_PANELS = box(10, 4, 11, 8) + box(4, 8, 8, 8)[:0] + box(4, 10, 8, 11) + box(10, 10, 11, 11)


def build_bright():
    px = sheet()
    put(px, PARCEL_BRIGHT)
    put(px, SIDE_PANELS)
    return px


def build_medium():
    px = sheet()
    put(px, FRAME)
    put(px, PARCEL_FILL)
    return px


def build_dark():
    px = sheet()
    put(px, CORNERS)
    return px


def build_item_front():
    """Dark screen of the item: a dark frame, covered by the tinted layers."""
    px = sheet()
    ring = span(2, 13, 2) + span(2, 13, 13) + column(2, 3, 12) + column(13, 3, 12)
    put(px, ring, SCREEN_OFF)
    return px


def build_wireless():
    """Wireless item: same frame as the AE2 `wireless_pattern_terminal.png`."""
    px = sheet()

    # Antenna, top left: the pink tip, then the grey stem.
    put(px, [(5, 0), (4, 1), (6, 1), (5, 2)], PINK)
    put(px, [(5, 1)], PURPLE)
    put(px, [(5, 3), (5, 4)], STEM)

    # Case: two rows of frame, and a light edge on the right side.
    put(px, span(2, 13, 5), BEZEL)
    put(px, span(2, 13, 15), BEZEL)
    put(px, [(2, 5), (13, 5), (2, 15), (13, 15)], BEZEL_DARK)
    for y in range(6, 15):
        put(px, [(2, y)], BEZEL_DARK)
        put(px, [(13, y)], BEZEL_DARK)
        put(px, [(12, y)], EDGE_LIGHT)

    # Screen: two white frame lines, and the package in the middle.
    put(px, span(3, 11, 6), WHITE)
    put(px, span(3, 11, 14), WHITE)
    put(px, box(3, 7, 11, 13), PURPLE)
    put(px, span(4, 10, 8) + span(4, 10, 10) + span(4, 10, 12), WHITE)
    put(px, [(4, 9), (10, 9), (4, 11), (10, 11)], WHITE)
    return px


if __name__ == "__main__":
    base = os.path.join("src", "main", "resources", "assets", "packagedautoterminals",
                        "textures")
    parts = os.path.join(base, "parts")
    png.save(os.path.join(parts, "pat_terminal_bright.png"), build_bright())
    png.save(os.path.join(parts, "pat_terminal_medium.png"), build_medium())
    png.save(os.path.join(parts, "pat_terminal_dark.png"), build_dark())
    png.save(os.path.join(base, "items", "part", "pat_terminal.png"), build_item_front())
    png.save(os.path.join(base, "items", "wireless_pat_terminal.png"), build_wireless())
    print("Three part layers, the dark screen, and the wireless item.")
