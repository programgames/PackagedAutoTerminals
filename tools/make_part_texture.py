# -*- coding: utf-8 -*-
"""Génère les textures de la part et des items, à la manière d'AE2.

Leçon tirée de la comparaison avec `pattern_terminal_*.png` d'AE2 : les trois calques
d'une part **ne portent aucune couleur**. Ce sont des masques blancs sur fond transparent.
La couleur vient du réseau, par les index de teinte 1, 2 et 3, que le jeu remplace par les
trois nuances de la couleur du câble. Nos anciennes textures étaient peintes en dur, en
turquoise opaque : elles ignoraient la couleur du réseau, et elles rendaient un carré noir
en jeu.

Le dessin reprend la charpente d'AE2 — cadre, séparateur, quatre coins sombres — et change
le seul symbole : un colis avec sa bande adhésive, au lieu du carré creux d'AE2.

Usage :
    python tools/make_part_texture.py
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import png

SIZE = 16
NONE = (0, 0, 0, 0)
MASK = (255, 255, 255, 255)

# --- Palette de l'item sans fil, relevée dans `wireless_pattern_terminal.png` d'AE2 ---
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


# --- Charpente commune, en coordonnées de texture ---------------------------------
# Le modèle montre la texture de 2 à 13. Le dessin tient donc dans 3..12, comme chez AE2.
FRAME = (span(4, 11, 3) + span(4, 11, 12)
         + column(3, 4, 11) + column(12, 4, 11)
         + column(9, 4, 11) + span(3, 12, 9))
CORNERS = [(3, 3), (12, 3), (3, 12), (12, 12)]

# Le colis : contour éclairé, et bande adhésive en travers.
PARCEL_BRIGHT = (span(4, 8, 4) + span(4, 8, 6) + span(4, 8, 8)
                 + [(4, 5), (8, 5), (4, 7), (8, 7)])
PARCEL_FILL = [(5, 5), (6, 5), (7, 5), (5, 7), (6, 7), (7, 7)]

# Les trois panneaux de droite et du bas, comme chez AE2.
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
    """Écran éteint de l'item : un cadre sombre, que les calques teintés recouvrent."""
    px = sheet()
    ring = span(2, 13, 2) + span(2, 13, 13) + column(2, 3, 12) + column(13, 3, 12)
    put(px, ring, SCREEN_OFF)
    return px


def build_wireless():
    """Item sans fil : même charpente que `wireless_pattern_terminal.png` d'AE2."""
    px = sheet()

    # Antenne, en haut à gauche : la pointe rose, puis la tige grise.
    put(px, [(5, 0), (4, 1), (6, 1), (5, 2)], PINK)
    put(px, [(5, 1)], PURPLE)
    put(px, [(5, 3), (5, 4)], STEM)

    # Boîtier : deux rangées de cadre, et un liseré clair sur le bord droit.
    put(px, span(2, 13, 5), BEZEL)
    put(px, span(2, 13, 15), BEZEL)
    put(px, [(2, 5), (13, 5), (2, 15), (13, 15)], BEZEL_DARK)
    for y in range(6, 15):
        put(px, [(2, y)], BEZEL_DARK)
        put(px, [(13, y)], BEZEL_DARK)
        put(px, [(12, y)], EDGE_LIGHT)

    # Écran : deux lignes blanches de cadre, et le colis au milieu.
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
    print("Trois calques de part, l'ecran eteint, et l'item sans fil.")
