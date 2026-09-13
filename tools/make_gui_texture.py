"""Génère les planches d'interface du mod.

Motif : nous avons d'abord réutilisé `newinterfaceterminal.png` d'AE2. Sa géométrie est
faite pour l'Interface Terminal, et plaquée sur notre fenêtre elle produisait des
rectangles gris en travers du texte. Nous dessinons donc les nôtres, dont nous connaissons
chaque coordonnée.

La palette reprend celle d'AE2 et du jeu, pour que les fenêtres restent natives :
  198 panneau | 224 zone de liste | 139 emplacements | 55 bordures | 255 lumières

PIÈGE corrigé : chaque planche a **sa propre** position d'inventaire. Une constante partagée
avait décalé l'inventaire de l'éditeur le jour où celui du terminal a été recentré.

Usage :
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
# Fond des champs de saisie. Sombre à dessein : le texte du jeu est toujours dessiné avec
# une ombre portée, qui sur un fond clair se lit comme une seconde lettre décalée.
FIELD = (26, 26, 26, 255)

SLOT_PITCH = 18
HOTBAR_GAP = 58

# --- Terminal ---------------------------------------------------------------------
SHEET = 512
WIDTH = 320
# La liste est dessinée pour le maximum de rangées. Le jeu n'en affiche que ce que la
# hauteur d'écran permet, et reprend le bas de la planche juste en dessous.
MAX_ROWS = 16
FOOTER = 100

LIST_LEFT = 8
LIST_TOP = 22
LIST_WIDTH = 288
ROW_HEIGHT = 18
LIST_HEIGHT = MAX_ROWS * ROW_HEIGHT
HEIGHT = LIST_TOP + LIST_HEIGHT + FOOTER

SCROLL_LEFT = 300
SCROLL_WIDTH = 12

SEARCH_LEFT = 150
SEARCH_TOP = 4
SEARCH_WIDTH = 146
SEARCH_HEIGHT = 12

TERMINAL_INVENTORY_LEFT = 79
TERMINAL_INVENTORY_TOP = LIST_TOP + LIST_HEIGHT + 16

# Icône « œil », rangée sous la fenêtre, dans la zone libre de la planche.
EYE_U = 330
EYE_V = 4
EYE_SIZE = 12

# --- Éditeur ----------------------------------------------------------------------
# La planche fait 512 sur 512 : la fenêtre dépasse 256 pixels dans les deux sens.
EDITOR_SHEET = 512
EDITOR_WIDTH = 258
EDITOR_HEIGHT = 332

NAME_LEFT = 8
NAME_TOP = 4
NAME_WIDTH = 162
NAME_HEIGHT = 14

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

EDITOR_ARROW_LEFT = 172
EDITOR_ARROW_TOP = 144

EDITOR_INVENTORY_LEFT = 20
EDITOR_INVENTORY_TOP = 250


def sheet(width, height):
    return [[NONE for _ in range(width)] for _ in range(height)]


def fill(px, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if 0 <= y < len(px) and 0 <= x < len(px[0]):
                px[y][x] = color


def recess(px, x0, y0, w, h, color):
    """Creuse une zone : bordure sombre en haut et à gauche, lumière en bas et à droite."""
    fill(px, x0, y0, w, h, color)
    fill(px, x0 - 1, y0 - 1, w + 1, 1, EDGE)
    fill(px, x0 - 1, y0 - 1, 1, h + 1, EDGE)
    fill(px, x0, y0 + h, w + 1, 1, WHITE)
    fill(px, x0 + w, y0, 1, h + 1, WHITE)


def frame(px, width, height):
    """Panneau et biseau, comme les fenêtres du jeu."""
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


def draw_eye(px, x0, y0):
    """Œil : une paupière en amande, et une pupille pleine."""
    lid = [(2, 6), (3, 4), (4, 3), (5, 2), (6, 2), (7, 3), (8, 4), (9, 6),
           (8, 8), (7, 9), (6, 10), (5, 10), (4, 9), (3, 8)]
    for x, y in lid:
        fill(px, x0 + x, y0 + y, 1, 1, EDGE)
    fill(px, x0 + 4, y0 + 5, 4, 3, EDGE)
    fill(px, x0 + 5, y0 + 4, 2, 5, EDGE)


def draw_arrow(px, x0, y0):
    """Flèche vers la droite, comme celle de l'Encoder."""
    fill(px, x0, y0 + 4, 14, 6, ARROW)
    for step in range(7):
        fill(px, x0 + 14 + step, y0 + step, 1, 14 - 2 * step, ARROW)


def draw_package(px, x0, y0):
    """Contour de colis, dessiné au fond des emplacements d'aperçu."""
    points = [(5, 2), (10, 2), (13, 6), (13, 10), (8, 14), (3, 10), (3, 6)]
    for index in range(len(points)):
        ax, ay = points[index]
        bx, by = points[(index + 1) % len(points)]
        steps = max(abs(bx - ax), abs(by - ay), 1)
        for step in range(steps + 1):
            fill(px, x0 + ax + (bx - ax) * step // steps,
                 y0 + ay + (by - ay) * step // steps, 1, 1, ICON)


def build_terminal():
    px = sheet(SHEET, SHEET)
    frame(px, WIDTH, HEIGHT)
    # L'icône vit à droite de la fenêtre, dans la zone libre de la planche.
    draw_eye(px, EYE_U, EYE_V)
    recess(px, SEARCH_LEFT, SEARCH_TOP, SEARCH_WIDTH, SEARCH_HEIGHT, FIELD)
    recess(px, LIST_LEFT, LIST_TOP, LIST_WIDTH, LIST_HEIGHT, LIST)
    recess(px, SCROLL_LEFT, LIST_TOP, SCROLL_WIDTH, LIST_HEIGHT, SLOT)
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
    print("Terminal", WIDTH, "x", HEIGHT, "| jusqu'a", MAX_ROWS, "rangees")
    write_png(os.path.join(base, "pat_editor.png"), build_editor())
    print("Editeur", EDITOR_WIDTH, "x", EDITOR_HEIGHT, "sur une planche", EDITOR_SHEET)
