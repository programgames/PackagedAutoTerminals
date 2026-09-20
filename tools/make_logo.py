"""Builds the mod logo and the CurseForge avatar from the terminal textures.

The art is drawn at 64 by 64, then scaled with nearest neighbour, so the pixels stay
square. The palette comes from the wireless terminal item icon, so the logo and the
item agree.

    python tools/make_logo.py

It writes src/main/resources/logo.png (256x256, transparent) and
docs/assets/curseforge-avatar.png (512x512, opaque).
"""

from PIL import Image
import os

BASE = "src/main/resources/assets/packagedautoterminals/textures/"

CASING_EDGE = (53, 49, 41, 255)    # dark frame of the wireless item
CASING = (76, 72, 65, 255)         # body of the wireless item
SCREEN = (97, 78, 171, 255)        # purple screen of the wireless item
SCREEN_DEEP = (48, 38, 88, 255)    # the unlit part of the face
GLYPH_MID = (186, 176, 232, 255)   # the medium layer of the part face
GLYPH_HI = (255, 255, 255, 255)    # the bright layer of the part face
ANTENNA = (255, 128, 215, 255)     # the pink aerial of the wireless item


def mask(name, size):
    """Reads a part layer and scales its white shape, keeping the hard pixel edges."""
    im = Image.open(BASE + "parts/" + name).convert("RGBA")
    return im.resize((size, size), Image.NEAREST)


def tint(layer, colour):
    out = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    px, op = layer.load(), out.load()
    for y in range(layer.size[1]):
        for x in range(layer.size[0]):
            if px[x, y][3] > 0:
                op[x, y] = colour
    return out


def box(img, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), colour)


def build():
    art = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # The casing, drawn as three nested squares, like the item icon.
    box(art, 2, 2, 61, 61, CASING_EDGE)
    box(art, 4, 4, 59, 59, CASING)
    box(art, 6, 6, 57, 57, CASING_EDGE)
    box(art, 7, 7, 56, 56, SCREEN_DEEP)
    box(art, 8, 8, 55, 55, SCREEN)

    # The three layers of the part face, at three times their size, centred.
    for name, colour in (
        ("pat_terminal_dark.png", SCREEN_DEEP),
        ("pat_terminal_medium.png", GLYPH_MID),
        ("pat_terminal_bright.png", GLYPH_HI),
    ):
        art.alpha_composite(tint(mask(name, 48), colour), (8, 8))

    # The aerial: it tells the wireless terminal apart on a small icon.
    box(art, 31, 0, 32, 5, CASING)
    box(art, 29, 0, 34, 1, ANTENNA)
    return art


def main():
    art = build()

    os.makedirs("docs/assets", exist_ok=True)
    art.resize((256, 256), Image.NEAREST).save("src/main/resources/logo.png")

    avatar = Image.new("RGBA", (512, 512), (26, 24, 33, 255))
    avatar.alpha_composite(art.resize((448, 448), Image.NEAREST), (32, 32))
    avatar.convert("RGB").save("docs/assets/curseforge-avatar.png")

    print("src/main/resources/logo.png 256x256")
    print("docs/assets/curseforge-avatar.png 512x512")


if __name__ == "__main__":
    main()
