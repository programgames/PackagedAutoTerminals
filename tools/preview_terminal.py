"""Renders the terminal as the screen draws it, to judge the layout without the game.

The font is not the Minecraft one, so the text is only an approximation of the widths.
Everything else comes from the generated sheet and from the runtime coordinates.
"""
from PIL import Image, ImageDraw, ImageFont

SHEET = Image.open(
    'src/main/resources/assets/packagedautoterminals/textures/guis/pat_terminal.png'
).convert('RGBA')

WIDTH, ROWS, ROW_H = 320, 12, 18
BAND, TAB_TOP, TAB_H, TAB_W, TAB_GAP = 18, 18, 22, 92, 2
LIST_TOP = TAB_TOP + TAB_H + 2
LIST_LEFT, LIST_WIDTH = 8, 288
FOOTER = 104
HEIGHT = LIST_TOP + ROWS * ROW_H + FOOTER
STATUS_TOP = LIST_TOP + ROWS * ROW_H + 4
SHEET_ROWS = 16

TEXT = (0x40, 0x40, 0x40)
DIM = (0x80, 0x80, 0x80)
WARN = (0x80, 0x30, 0x30)
FIELD_TEXT = (0xE0, 0xE0, 0xE0)

screen = Image.new('RGBA', (WIDTH, HEIGHT), (0, 0, 0, 0))
list_bottom = LIST_TOP + ROWS * ROW_H
screen.paste(SHEET.crop((0, 0, WIDTH, list_bottom)), (0, 0))
sheet_bottom = LIST_TOP + SHEET_ROWS * ROW_H
screen.paste(SHEET.crop((0, sheet_bottom, WIDTH, sheet_bottom + FOOTER)), (0, list_bottom))

draw = ImageDraw.Draw(screen)
font = ImageFont.load_default()


def text(x, y, s, color):
    draw.text((x, y), s, font=font, fill=color)


def width_of(s):
    return draw.textlength(s, font=font)


def chip(right, y, label, warning):
    w = width_of(label)
    left = int(right - w - 8)
    top, bottom = y + 2, y + 16
    fill = (0xA0, 0x20, 0x20, 0x2E) if warning else (0, 0, 0, 0x26)
    edge = (0x90, 0x20, 0x20, 0x55) if warning else (0, 0, 0, 0x3D)
    plate = Image.new('RGBA', (int(right - left), bottom - top), fill)
    screen.alpha_composite(plate, (left, top))
    draw.rectangle([left, top, right - 1, bottom - 1], outline=edge)
    text(left + 4, y + 5, label, WARN if warning else TEXT)


# --- the tab strip -----------------------------------------------------------------
for index, (name, active) in enumerate([('Patterns', True), ('Machines', False)]):
    left = LIST_LEFT + index * (TAB_W + TAB_GAP)
    u, v = (330, 64) if active else (330, 40)
    screen.alpha_composite(SHEET.crop((u, v, u + TAB_W, v + TAB_H)), (left, TAB_TOP))
    # The item icon is drawn by the game; a plain square stands in for it here.
    draw.rectangle([left + 6, TAB_TOP + 3, left + 21, TAB_TOP + 18],
                   fill=(0x6b, 0x5f, 0x4a) if index == 0 else (0x4a, 0x5f, 0x6b))
    text(left + 26, TAB_TOP + 8, name, TEXT if active else DIM)

# --- the title band ----------------------------------------------------------------
text(LIST_LEFT, 5, 'PackagedAuto Terminal', TEXT)
text(153, 6, 'iron', FIELD_TEXT)
text(288, 5, 'x', FIELD_TEXT)

# --- the rows ----------------------------------------------------------------------
rows = [
    ('group', 'Packager + Unpackager', '6 recipes', False),
    ('recipe', 'Iron Plate', 'Processing', False),
    ('recipe', 'Gold Plate', 'Processing', False),
    ('group', 'Ingots', 'no Unpackager', True),
    ('group', 'Packaging Provider', 'no recipe yet', False),
    ('section', 'CRAFTERS', '', False),
    ('crafter', 'Combination Package Crafter', '3 recipes', False),
    ('crafter', 'Basic Package Crafter', 'no recipe', True),
    ('crafter', 'Ultimate Package Crafter', 'inactive', True),
]
for index, (kind, label, state, warning) in enumerate(rows):
    y = LIST_TOP + index * ROW_H
    if index % 2 == 1:
        band = Image.new('RGBA', (LIST_WIDTH, ROW_H), (0, 0, 0, 0x14))
        screen.alpha_composite(band, (LIST_LEFT, y))
    if kind == 'section':
        text(LIST_LEFT + 4, y + 5, label, TEXT)
        continue
    if kind == 'recipe':
        text(LIST_LEFT + 40, y + 5, label, TEXT)
        text(LIST_LEFT + LIST_WIDTH - 20 - int(width_of(state)), y + 5, state, DIM)
        continue
    icon_x = LIST_LEFT + (12 if kind == 'group' else 2)
    draw.rectangle([icon_x, y + 1, icon_x + 15, y + 16], fill=(0x55, 0x55, 0x60))
    if kind == 'group':
        draw.polygon([(LIST_LEFT + 2, y + 6), (LIST_LEFT + 8, y + 9),
                      (LIST_LEFT + 2, y + 12)], fill=TEXT)
        text(LIST_LEFT + 32, y + 5, label, TEXT)
        chip(LIST_LEFT + LIST_WIDTH - 40, y, state, warning)
        draw.rectangle([LIST_LEFT + LIST_WIDTH - 34, y + 1,
                        LIST_LEFT + LIST_WIDTH - 19, y + 16], fill=(0x55, 0x60, 0x55))
        screen.alpha_composite(SHEET.crop((330, 4, 342, 16)),
                               (LIST_LEFT + LIST_WIDTH - 16, y + 3))
    else:
        text(LIST_LEFT + 22, y + 5, label, TEXT)
        chip(LIST_LEFT + LIST_WIDTH - 4, y, state, warning)

# --- the status bar ----------------------------------------------------------------
text(LIST_LEFT + 2, STATUS_TOP + 2, '22 machines - 144 recipes', TEXT)
results = '9 results'
text(LIST_LEFT + LIST_WIDTH - int(width_of(results)), STATUS_TOP + 2, results, DIM)

out = screen.resize((WIDTH * 2, HEIGHT * 2), Image.NEAREST)
canvas = Image.new('RGB', (out.width + 40, out.height + 40), (0x2b, 0x2b, 0x33))
canvas.paste(out.convert('RGB'), (20, 20), out)
canvas.save(r'C:\Users\Julien\AppData\Local\Temp\claude\C--Users-Julien-Desktop-PackagedAutoTerminals\36dd2b06-0d4b-4662-8566-b75ebd3e805f\scratchpad\terminal_preview.png')
print('preview written, screen is %d by %d' % (WIDTH, HEIGHT))
