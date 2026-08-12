#!/usr/bin/env python3
"""Generates assets/pokeface/textures/face/expressions.png.

The atlas is 8 expression columns x 5 style rows of 16x16 tiles. Each tile's top
half is the eye sprite and the bottom half is the mouth sprite; the renderer
picks a half by UV and tints it, so the art here is a white/alpha mask.

Run:  python3 tools/gen_atlas.py
"""
import struct
import zlib
from pathlib import Path

COLS, ROWS, TILE = 8, 5, 16
W, H = COLS * TILE, ROWS * TILE

# Eye shapes per style: list of (x, y, w, h) rectangles in the 8x8 eye half.
EYES = {
    0: [(2, 3, 4, 3)],                       # default: simple block eye
    1: [(1, 2, 6, 5), (2, 3, 2, 2)],         # anime: big eye + highlight cut
    2: [(1, 3, 6, 4), (0, 2, 8, 1)],         # toon: wide eye + lid line
    3: [(1, 4, 6, 2), (5, 3, 2, 1)],         # sharp: narrow slit
    4: [(3, 3, 2, 2)],                       # pixel: minimal
}
# Mouth shapes per style in the 8x8 mouth half.
MOUTHS = {
    0: [(2, 3, 4, 2)],
    1: [(3, 3, 2, 2)],
    2: [(1, 3, 6, 3)],
    3: [(2, 4, 4, 1)],
    4: [(3, 4, 2, 1)],
}
# Per-expression alpha weighting, so e.g. TIRED renders a fainter eye.
EXPRESSION_ALPHA = [255, 255, 255, 255, 255, 235, 200, 235]


def blank():
    return [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]


def fill(px, ox, oy, rect, alpha):
    x, y, w, h = rect
    for j in range(h):
        for i in range(w):
            px[oy + y + j][ox + x + i] = (255, 255, 255, alpha)


def build():
    px = blank()
    for row in range(ROWS):
        for col in range(COLS):
            ox, oy = col * TILE, row * TILE
            alpha = EXPRESSION_ALPHA[col % len(EXPRESSION_ALPHA)]
            for rect in EYES[row]:
                fill(px, ox, oy, rect, alpha)
            for rect in MOUTHS[row]:
                fill(px, ox, oy + TILE // 2, rect, alpha)
    return px


def write_png(path, px):
    raw = b"".join(
        b"\x00" + b"".join(struct.pack("4B", *p) for p in line) for line in px
    )

    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_bytes(png)


if __name__ == "__main__":
    out = Path(__file__).resolve().parent.parent / "src/main/resources/assets/pokeface/textures/face/expressions.png"
    write_png(out, build())
    print(f"wrote {out} ({W}x{H})")
