#!/usr/bin/env python3
"""Create original matte, age-worn, transparent nine-slice frames.

Generated PNGs are drawn from scratch with stdlib only, preserving user text
as live Compose elements and never cropping any reference screenshot.
"""
from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

WIDTH, HEIGHT = 192, 112
FRAMES = {
    "tab_active": (True, 19),
    "tab_inactive": (False, 17),
    "search_frame": (False, 14),
    "filter_active": (True, 16),
    "filter_inactive": (False, 15),
    "section_card": (False, 19),
    "subsection_card": (False, 16),
    "top_small_button": (True, 16),
}
INK = (
    (39, 24, 17), (128, 91, 54), (87, 55, 35),
    (207, 174, 122), (84, 52, 32), (231, 207, 165),
    (148, 116, 79),
)

def hashed(x: int, y: int, seed: int) -> float:
    v = ((x + 71) * 374761393) ^ ((y + 31) * 668265263) ^ ((seed + 13) * 1442695041)
    v = (v ^ (v >> 13)) * 1274126177
    v ^= v >> 16
    return (v & 0xFFFFFFFF) / 0x100000000

def palette() -> tuple[bytes, bytes]:
    colors = [(0, 0, 0, 0)]
    for red, green, blue in INK:
        for value in range(32):
            level = (value - 15) * 2.8
            colors.append((
                max(0, min(255, round(red + level))),
                max(0, min(255, round(green + level * .95))),
                max(0, min(255, round(blue + level * .78))), 255
            ))
    return (
        bytes(n for color in colors for n in color[:3]),
        bytes(color[3] for color in colors),
    )

def color_index(material: int, light: float) -> int:
    return 1 + material * 32 + max(0, min(31, round(light)))

def pixel(x: int, y: int, dark: bool, thickness: int, seed: int) -> int:
    dx, dy = min(x, WIDTH - x - 1), min(y, HEIGHT - y - 1)
    d = min(dx, dy)
    grain = hashed(x, y, seed)
    coarse = hashed(x // 3, y // 3, seed + 5)
    pattern = hashed(x // 9, y // 5, seed + 29)
    corner = dx < 32 and dy < 32

    radius = 12.0
    if dx < radius and dy < radius and math.hypot(radius - dx, radius - dy) > radius + (grain - .5) * 1.5:
        return 0
    if d >= thickness + (pattern - .5) * 2:
        return 0
    if (grain < .027 and d < 2.2) or (coarse > .993 and d < 4.4 and corner):
        return 0

    shade = 16 + (grain - .5) * 8 + (coarse - .5) * 6
    shade += ((HEIGHT - y) / HEIGHT + (WIDTH - x) / WIDTH - 1) * 6
    if d < 1.3:
        material, shade = 0, 9 + shade / 6
    elif d < 3.2:
        material, shade = 4, 10 + shade / 4
    elif d < 4.7:
        material, shade = 1, 16 + shade / 6
    elif d < 6:
        material, shade = 5, 15 + shade / 6
    elif d < 8.5:
        material = 2 if dark else 6
    elif d < 10.2:
        material, shade = 0, 12 + shade / 6
    elif d < 11.4:
        material, shade = 1, 19 + shade / 5
    elif d < 13.2:
        material, shade = 5, 13 + shade / 5
    elif d < thickness - 2.5:
        material = 2 if dark else 3
    elif d < thickness - 1:
        material, shade = 1, 7 + shade / 6
    else:
        material, shade = 4, 12 + shade / 5

    if d > 12 and (pattern < .12 or coarse > .95) and not dark:
        material, shade = 3, 9 + shade / 4
    if (grain < .040 and d > 5 and d < thickness - 1) or (coarse < .025 and d > 8):
        material, shade = (1 if dark else 3), 9 + shade / 4
    if grain > .978 and 3 < d < thickness - 3:
        material, shade = 4, 9 + shade / 4

    if corner:
        cx, cy = dx - 14.5, dy - 14.5
        diamond = abs(cx) + abs(cy)
        if 8.7 < diamond < 10:
            material, shade = 1, 23 + shade / 7
        if math.hypot(cx, cy) < 2.6:
            material, shade = 1, 26
        if 2.6 <= math.hypot(cx, cy) < 3.6:
            material, shade = 0, 11
        if 22 < dx + dy < 24 and dx < 12:
            material, shade = 0, 14
    return color_index(material, shade)

def chunk(name: bytes, content: bytes) -> bytes:
    return (
        struct.pack(">I", len(content)) + name + content +
        struct.pack(">I", zlib.crc32(name + content))
    )

def make_png(dark: bool, thickness: int, seed: int) -> bytes:
    rows = [
        bytes([0] + [pixel(x, y, dark, thickness, seed) for x in range(WIDTH)])
        for y in range(HEIGHT)
    ]
    colors, alpha = palette()
    return b"\x89PNG\r\n\x1a\n" + b"".join((
        chunk(b"IHDR", struct.pack(">IIBBBBB", WIDTH, HEIGHT, 8, 3, 0, 0, 0)),
        chunk(b"PLTE", colors),
        chunk(b"tRNS", alpha),
        chunk(b"IDAT", zlib.compress(b"".join(rows), 9)),
        chunk(b"IEND", b""),
    ))

def generate_antique_frames(target: Path) -> int:
    target.mkdir(parents=True, exist_ok=True)
    for i, (name, (dark, thickness)) in enumerate(FRAMES.items()):
        (target / f"vintage_{name}.png").write_bytes(make_png(dark, thickness, i + 3))
    return len(FRAMES)

if __name__ == "__main__":
    folder = Path(__file__).resolve().parents[1] / "app/src/main/res/drawable-nodpi"
    print(f"Generated {generate_antique_frames(folder)} antique frames")
