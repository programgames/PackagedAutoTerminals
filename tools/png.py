# -*- coding: utf-8 -*-
"""Lecture et ecriture de PNG, sans dependance. Sert aux outils de texture."""
import zlib, struct


def load(path):
    d = open(path, 'rb').read()
    i = 8; idat = b''; w = h = bd = ct = 0; pal = None; trns = None
    while i < len(d):
        ln = struct.unpack('>I', d[i:i + 4])[0]; k = d[i + 4:i + 8]; data = d[i + 8:i + 8 + ln]
        if k == b'IHDR': w, h, bd, ct = struct.unpack('>IIBB', data[:10])
        if k == b'PLTE': pal = data
        if k == b'tRNS': trns = data
        if k == b'IDAT': idat += data
        i += 12 + ln
    raw = zlib.decompress(idat)
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    bpp = max(1, ch * bd // 8); stride = (w * ch * bd + 7) // 8
    out = bytearray(); prev = bytearray(stride); p = 0
    for y in range(h):
        f = raw[p]; p += 1; line = bytearray(raw[p:p + stride]); p += stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]; c = prev[x - bpp] if x >= bpp else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                pp = a + b - c; pa = abs(pp - a); pb = abs(pp - b); pc = abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        out += line; prev = line
    px = []
    for y in range(h):
        row = []
        for x in range(w):
            o = y * stride + x * ch
            if ct == 6: row.append(tuple(out[o:o + 4]))
            elif ct == 2: row.append(tuple(out[o:o + 3]) + (255,))
            elif ct == 3:
                idx = out[y * stride + x]
                row.append(tuple(pal[idx * 3:idx * 3 + 3])
                           + ((trns[idx] if trns and idx < len(trns) else 255),))
            elif ct == 4: row.append((out[o],) * 3 + (out[o + 1],))
            else: row.append((out[o],) * 3 + (255,))
        px.append(row)
    return px


def save(path, px):
    import os
    h = len(px); w = len(px[0])
    raw = b''
    for row in px:
        raw += b'\x00' + b''.join(bytes(c) for c in row)

    def chunk(kind, data):
        return (struct.pack('>I', len(data)) + kind + data
                + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff))

    blob = (b'\x89PNG\r\n\x1a\n'
            + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
            + chunk(b'IDAT', zlib.compress(raw, 9))
            + chunk(b'IEND', b''))
    directory = os.path.dirname(path)
    if directory:
        os.makedirs(directory, exist_ok=True)
    open(path, 'wb').write(blob)


def show(px, limit=16):
    for row in px[:limit]:
        line = ''
        for r, g, b, a in row[:limit]:
            if a < 40: line += '.'
            else:
                v = (r + g + b) // 3
                line += '#' if v > 200 else '+' if v > 120 else '-' if v > 50 else 'o'
        print(line)
