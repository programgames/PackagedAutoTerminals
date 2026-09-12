"""Génère la planche de l'interface du terminal.

Motif : nous avons d'abord réutilisé `newinterfaceterminal.png` d'AE2. Sa géométrie est
faite pour l'Interface Terminal, avec deux champs de recherche et une zone de liste de
hauteur fixe. Plaquée sur notre fenêtre, elle produisait des rectangles gris en travers du
texte. Nous dessinons donc la nôtre, dont nous connaissons chaque coordonnée.

La palette reprend celle d'AE2 et du jeu, pour que la fenêtre reste native :
  198 panneau | 224 zone de liste | 139 emplacements | 55 bordures | 255 lumières

Usage :
    python tools/make_gui_texture.py
"""

import os
import struct
import zlib

# --- Géométrie de la fenêtre. Ces valeurs sont reprises telles quelles dans
# --- ContainerPatTerminal. Toute modification ici doit y être répercutée.
WIDTH = 195
HEIGHT = 212

LIST_LEFT = 8
LIST_TOP = 18
LIST_WIDTH = 160
ROWS = 6
ROW_HEIGHT = 18
LIST_HEIGHT = ROWS * ROW_HEIGHT

SCROLL_LEFT = 171
SCROLL_WIDTH = 12

PLAYER_INVENTORY_TOP = 130
SLOT_LEFT = 8
SLOT_PITCH = 18
HOTBAR_GAP = 58

SHEET = 256

BLACK = (0, 0, 0, 255)
WHITE = (255, 255, 255, 255)
PANEL = (198, 198, 198, 255)
LIST = (224, 224, 224, 255)
SLOT = (139, 139, 139, 255)
EDGE = (55, 55, 55, 255)
SHADOW = (85, 85, 85, 255)
NONE = (0, 0, 0, 0)


def new_sheet():
    return [[NONE for _ in range(SHEET)] for _ in range(SHEET)]


def fill(px, x0, y0, w, h, color):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            if 0 <= x < SHEET and 0 <= y < SHEET:
                px[y][x] = color


def recess(px, x0, y0, w, h, color):
    """Creuse une zone : bordure sombre en haut et à gauche, lumière en bas et à droite."""
    fill(px, x0, y0, w, h, color)
    fill(px, x0 - 1, y0 - 1, w + 1, 1, EDGE)
    fill(px, x0 - 1, y0 - 1, 1, h + 1, EDGE)
    fill(px, x0, y0 + h, w + 1, 1, WHITE)
    fill(px, x0 + w, y0, 1, h + 1, WHITE)


def build():
    px = new_sheet()

    # Panneau et son biseau, comme les fenêtres du jeu.
    fill(px, 0, 0, WIDTH, HEIGHT, PANEL)
    fill(px, 0, 0, WIDTH, 1, BLACK)
    fill(px, 0, 0, 1, HEIGHT, BLACK)
    fill(px, 1, 1, WIDTH - 2, 2, WHITE)
    fill(px, 1, 1, 2, HEIGHT - 2, WHITE)
    fill(px, WIDTH - 3, 1, 2, HEIGHT - 1, SHADOW)
    fill(px, 1, HEIGHT - 3, WIDTH - 1, 2, SHADOW)
    fill(px, WIDTH - 1, 0, 1, HEIGHT, BLACK)
    fill(px, 0, HEIGHT - 1, WIDTH, 1, BLACK)

    # Zone de liste, et piste de l'ascenseur.
    recess(px, LIST_LEFT, LIST_TOP, LIST_WIDTH, LIST_HEIGHT, LIST)
    recess(px, SCROLL_LEFT, LIST_TOP, SCROLL_WIDTH, LIST_HEIGHT, SLOT)

    # Inventaire du joueur : trois rangées, puis la barre d'accès rapide.
    for row in range(3):
        for column in range(9):
            recess(px, SLOT_LEFT + column * SLOT_PITCH,
                   PLAYER_INVENTORY_TOP + row * SLOT_PITCH, 16, 16, SLOT)
    for column in range(9):
        recess(px, SLOT_LEFT + column * SLOT_PITCH,
               PLAYER_INVENTORY_TOP + HOTBAR_GAP, 16, 16, SLOT)

    return px


def write_png(path, px, width=SHEET, height=SHEET):
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


# --- Planche de l'éditeur -------------------------------------------------------
# Elle mesure 256 x 512 : la fenêtre fait 276 pixels de haut, donc plus que les 256
# que suppose `drawTexturedModalRect`. Le dessin passe par
# `drawModalRectWithCustomSizedTexture`, auquel on donne la taille réelle.
EDITOR_WIDTH = 236
EDITOR_HEIGHT = 276
EDITOR_SHEET_HEIGHT = 512

GRID_LEFT = 8
GRID_TOP = 20
OUTPUT_LEFT = 178
OUTPUT_TOP = 20
PREVIEW_LEFT = 178
PREVIEW_TOP = 80
EDITOR_INVENTORY_TOP = 194


def new_editor_sheet():
    return [[NONE for _ in range(SHEET)] for _ in range(EDITOR_SHEET_HEIGHT)]


def build_editor():
    px = new_editor_sheet()

    fill(px, 0, 0, EDITOR_WIDTH, EDITOR_HEIGHT, PANEL)
    fill(px, 0, 0, EDITOR_WIDTH, 1, BLACK)
    fill(px, 0, 0, 1, EDITOR_HEIGHT, BLACK)
    fill(px, 1, 1, EDITOR_WIDTH - 2, 2, WHITE)
    fill(px, 1, 1, 2, EDITOR_HEIGHT - 2, WHITE)
    fill(px, EDITOR_WIDTH - 3, 1, 2, EDITOR_HEIGHT - 1, SHADOW)
    fill(px, 1, EDITOR_HEIGHT - 3, EDITOR_WIDTH - 1, 2, SHADOW)
    fill(px, EDITOR_WIDTH - 1, 0, 1, EDITOR_HEIGHT, BLACK)
    fill(px, 0, EDITOR_HEIGHT - 1, EDITOR_WIDTH, 1, BLACK)

    for row in range(9):
        for column in range(9):
            recess(px, GRID_LEFT + column * 18, GRID_TOP + row * 18, 16, 16, SLOT)
    for row in range(3):
        for column in range(3):
            recess(px, OUTPUT_LEFT + column * 18, OUTPUT_TOP + row * 18, 16, 16, SLOT)
            recess(px, PREVIEW_LEFT + column * 18, PREVIEW_TOP + row * 18, 16, 16, SLOT)

    for row in range(3):
        for column in range(9):
            recess(px, SLOT_LEFT + column * SLOT_PITCH,
                   EDITOR_INVENTORY_TOP + row * SLOT_PITCH, 16, 16, SLOT)
    for column in range(9):
        recess(px, SLOT_LEFT + column * SLOT_PITCH,
               EDITOR_INVENTORY_TOP + HOTBAR_GAP, 16, 16, SLOT)

    return px


def write_png_sized(path, px, width, height):
    """Meme ecriture que write_png, mais pour une planche qui n'est pas carree."""
    write_png(path, px, width, height)


if __name__ == "__main__":
    target = os.path.join("src", "main", "resources", "assets", "packagedautoterminals",
                          "textures", "guis", "pat_terminal.png")
    write_png(target, build())
    print("Ecrit :", target)
    print("Fenetre", WIDTH, "x", HEIGHT, "| liste", ROWS, "rangees de", ROW_HEIGHT)

    editor = os.path.join("src", "main", "resources", "assets", "packagedautoterminals",
                          "textures", "guis", "pat_editor.png")
    write_png_sized(editor, build_editor(), SHEET, EDITOR_SHEET_HEIGHT)
    print("Ecrit :", editor)
    print("Editeur", EDITOR_WIDTH, "x", EDITOR_HEIGHT, "sur une planche", SHEET,
          "x", EDITOR_SHEET_HEIGHT)
