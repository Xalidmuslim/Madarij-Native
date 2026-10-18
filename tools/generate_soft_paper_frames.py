#!/usr/bin/env python3
"""Deterministic antique parchment assets for Madarij Android Compose.

Soft paper fibers, mild foxing, uneven tea-stained edges, subtle deckling.
No metallic border, bevel, dark frame, screenshot crop, text, icon or arrow.
All seven translucent WebP files are recreated before each Android build.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageStat
import math
import random

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable-nodpi"
SPECS = {
    "frame_light_wide": ((246, 229, 194), 640, 176, 11),
    "frame_light_compact": ((247, 233, 200), 640, 176, 17),
    "frame_section_numbered": ((247, 231, 197), 640, 176, 23),
    "frame_search": ((249, 236, 208), 640, 176, 31),
    "frame_font_settings": ((247, 231, 198), 320, 176, 41),
    "frame_dark_wide": ((78, 52, 36), 640, 176, 53),
    "frame_dark_compact": ((84, 56, 39), 640, 176, 59),
}


def noise(width, height, cells_x, cells_y, seed):
    rnd = random.Random(seed)
    grid = Image.new("L", (cells_x, cells_y))
    grid.putdata([rnd.randint(0, 255) for _ in range(cells_x * cells_y)])
    return grid.resize((width, height), Image.Resampling.BICUBIC)


def create(name, base, width, height, seed):
    rnd = random.Random(seed)
    pale = not name.startswith("frame_dark")
    alpha_mask = Image.new("L", (width, height), 0)
    pen = ImageDraw.Draw(alpha_mask)
    left, right, top, bottom = 10, width - 11, 10, height - 11
    radius = 23

    def tooth(t, salt):
        return (
            1.9 * math.sin(t / 25 + salt)
            + 1.3 * math.sin(t / 9.6 + salt / 2)
            + .65 * math.sin(t / 3.7 + salt)
        )

    points = []
    for x in range(left + radius, right - radius + 1, 2):
        points.append((x, top + tooth(x, seed)))
    for degrees in range(270, 361, 5):
        a = math.radians(degrees)
        points.append((right - radius + radius * math.cos(a),
                       top + radius + radius * math.sin(a)))
    for y in range(top + radius, bottom - radius + 1, 3):
        points.append((right + tooth(y, seed) * .5, y))
    for degrees in range(0, 91, 5):
        a = math.radians(degrees)
        points.append((right - radius + radius * math.cos(a),
                       bottom - radius + radius * math.sin(a)))
    for x in range(right - radius, left + radius - 1, -2):
        points.append((x, bottom + tooth(x, seed + 8)))
    for degrees in range(90, 181, 5):
        a = math.radians(degrees)
        points.append((left + radius + radius * math.cos(a),
                       bottom - radius + radius * math.sin(a)))
    for y in range(bottom - radius, top + radius - 1, -3):
        points.append((left + tooth(y, seed + 5) * .5, y))
    for degrees in range(180, 271, 5):
        a = math.radians(degrees)
        points.append((left + radius + radius * math.cos(a),
                       top + radius + radius * math.sin(a)))

    pen.polygon(points, fill=255)
    alpha_mask = alpha_mask.filter(ImageFilter.GaussianBlur(.8))

    # Discontinuous foxing patches near corners and outside the text center.
    stains = Image.new("L", (width, height), 0)
    blot = ImageDraw.Draw(stains)
    for _ in range(23 if pale else 10):
        side = rnd.choice(("top", "bottom", "left", "right"))
        if side in ("top", "bottom"):
            x = rnd.randrange(5, width - 5)
            y = rnd.randrange(6, 38) if side == "top" else rnd.randrange(height - 39, height - 4)
        else:
            x = rnd.randrange(6, 36) if side == "left" else rnd.randrange(width - 37, width - 5)
            y = rnd.randrange(4, height - 4)
        rx = rnd.randrange(12, 52)
        ry = rnd.randrange(7, 22)
        blot.ellipse((x - rx, y - ry, x + rx, y + ry),
                     fill=rnd.randrange(65, 170))
    stains = stains.filter(ImageFilter.GaussianBlur(11))

    # Large tonal blooms survive screen downscaling; small grain stays delicate.
    large = noise(width, height, max(4, width // 68), max(3, height // 48), seed * 3)
    grain = noise(width, height, max(5, width // 16), max(5, height // 13), seed * 3 + 1)
    fine = noise(width, height, max(5, width // 3), max(5, height // 3), seed * 3 + 2)
    main = Image.new("RGBA", (width, height))
    px = main.load()
    alpha = alpha_mask.load()
    stain = stains.load()
    lp, gp, fp = large.load(), grain.load(), fine.load()

    for y in range(height):
        for x in range(width):
            opacity = alpha[x, y]
            if opacity < 1:
                px[x, y] = (0, 0, 0, 0)
                continue

            # Medium-strength archival patina: aged edge, quiet legible center.
            distance = max(0, min(x - left, right - x, y - top, bottom - y))
            age = (
                31 * math.exp(-distance / 21)
                + 8 * math.exp(-distance / 4.2)
            ) if pale else 10 * math.exp(-distance / 15)
            corner = min(
                math.hypot(x - left, y - top),
                math.hypot(x - right, y - top),
                math.hypot(x - left, y - bottom),
                math.hypot(x - right, y - bottom),
            )
            age += (
                11 * math.exp(-(corner / 68) ** 2)
            ) if pale else 3 * math.exp(-(corner / 55) ** 2)
            age += (stain[x, y] / 255) * (22 if pale else 7)

            cloudy = (lp[x, y] - 128) * (.075 if pale else .095)
            variegation = (gp[x, y] - 128) * (.049 if pale else .055)
            fibers = (fp[x, y] - 128) * (.025 if pale else .029)
            tint = cloudy + variegation + fibers
            if pale:
                color = (
                    base[0] + tint - age * .70,
                    base[1] + tint * .91 - age * .92,
                    base[2] + tint * .76 - age * 1.04,
                )
            else:
                color = (
                    base[0] + tint - age * .45,
                    base[1] + tint * .75 - age * .40,
                    base[2] + tint * .78 - age * .35,
                )
            rgb = tuple(max(0, min(255, int(round(v)))) for v in color)
            px[x, y] = rgb + (int(opacity * (.98 if pale else 1)),)

    # Add individual translucent fibers on a SEPARATE alpha-composited
    # layer: drawing RGBA directly on the paper would produce dark scratches.
    fiber_layer = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(fiber_layer, "RGBA")
    for _ in range(360 if pale else 180):
        x = rnd.randrange(20, width - 20)
        y = rnd.randrange(20, height - 20)
        length = rnd.uniform(2, 11)
        fiber = (
            (127, 91, 54, rnd.randint(7, 19))
            if pale else (174, 136, 93, rnd.randint(6, 14))
        )
        draw.line((x, y, x + length, y + rnd.uniform(-1.6, 1.6)),
                  fill=fiber, width=1)

    if pale:
        for _ in range(175):
            x = rnd.randrange(14, width - 14)
            y = rnd.randrange(12, height - 12)
            edge_distance = min(x - 10, width - 11 - x, y - 10, height - 11 - y)
            if edge_distance > 30 or rnd.random() > .63:
                continue
            r = rnd.choice((.35, .65, .9, 1.35))
            draw.ellipse((x - r, y - r, x + r, y + r),
                         fill=(133, 86, 43, rnd.randint(13, 37)))
        for _ in range(18):
            y = rnd.choice((
                rnd.randrange(14, 28),
                rnd.randrange(height - 28, height - 14),
            ))
            x = rnd.randrange(22, width - 50)
            length = rnd.randrange(12, 40)
            draw.line((
                x, y,
                x + length / 2, y + rnd.uniform(-1.4, 1.4),
                x + length, y + rnd.uniform(-1.8, 1.8),
            ), fill=(128, 88, 47, rnd.randint(12, 25)), width=1)

    main = Image.alpha_composite(main, fiber_layer)
    # Restore clean alpha outside the hand-worn silhouette.
    main.putalpha(alpha_mask.point(lambda value: round(value * (.98 if pale else 1))))
    shadow_alpha = Image.new("L", (width, height), 0)
    blurred = alpha_mask.filter(ImageFilter.GaussianBlur(3.4)).load()
    sa = shadow_alpha.load()
    for y in range(height - 3):
        for x in range(width):
            sa[x, y + 3] = int(blurred[x, y] * (.12 if pale else .14))
    shadow = Image.new("RGBA", (width, height),
                       ((66, 44, 22) if pale else (30, 16, 12)) + (0,))
    shadow.putalpha(shadow_alpha)
    shadow.alpha_composite(main)

    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / (name + ".webp")
    shadow.save(path, "WEBP", lossless=True, method=5, exact=True)
    with Image.open(path) as verify:
        assert verify.mode == "RGBA", path
        assert verify.getpixel((0, 0))[3] == 0, path
        assert verify.getpixel((width // 2, height // 2))[3] > 245, path
        if pale:
            rgb = verify.convert("RGB")
            center = rgb.crop((width // 3, height // 3,
                               width * 2 // 3, height * 2 // 3))
            edge = rgb.crop((width // 3, 15, width * 2 // 3, 24))
            center_mean = ImageStat.Stat(center).mean
            edge_mean = ImageStat.Stat(edge).mean
            assert sum(center_mean) - sum(edge_mean) > 33, (
                "Antique patina is too weak at the paper edge", path
            )
            assert ImageStat.Stat(center).stddev[0] > 2.5, (
                "The paper center has lost its natural fibers", path
            )
    print("Aged parchment:", path.name)


if __name__ == "__main__":
    for key, args in SPECS.items():
        create(key, *args)
