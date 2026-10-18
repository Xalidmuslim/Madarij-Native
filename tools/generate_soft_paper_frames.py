#!/usr/bin/env python3
"""Extract the four user-approved antique-paper images without redrawing them.

Approved transparent source artwork is bundled as an RGBA WebP sprite:
  row 0: wide antiqued paper card
  row 1: compact parchment button
  row 2: search field
  row 3: small settings button
Each row is 160 px tall. The atlas is stored under tools/assets/.
This generator runs before Android resource compilation. It leaves the two
existing DARK paper buttons untouched to retain their contrast and labels.
"""
from pathlib import Path
from PIL import Image, ImageStat

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable-nodpi"
ATLAS = ROOT / "tools/assets/approved_paper_sprite.webp"
# Old dark resources: frame_dark_compact and frame_dark_wide are preserved.
TILES = {
    "frame_light_wide": (0, 512),
    "frame_light_compact": (1, 320),
    "frame_section_numbered": (0, 512),
    "frame_search": (2, 512),
    "frame_font_settings": (3, 320),
}

def verify_tile(name, tile):
    assert tile.mode == "RGBA", name
    w, h = tile.size
    assert tile.getpixel((0, 0))[3] == 0, "No opaque rectangle outside paper: " + name
    assert tile.getpixel((w//2, h//2))[3] >= 252, "Solid readable center: " + name
    r, g, b, _ = tile.getpixel((w//2, h//2))
    assert min(r,g,b)>170 and r>g>b, "Warm ivory center: " + name
    # Natural creases survive at the edges, but the type area must remain clean.
    center = tile.crop((w//4,h//3,w*3//4,h*2//3)).convert("RGB")
    assert ImageStat.Stat(center).stddev[0] < 15, "Middle too distracting: " + name

def main():
    if not ATLAS.is_file():
        raise SystemExit("Required original approved parchment sprite is missing: " + str(ATLAS))
    with Image.open(ATLAS) as sheet:
        sheet = sheet.convert("RGBA")
        assert sheet.size == (512, 640), f"Unexpected sprite dimensions: {sheet.size}"
        OUT.mkdir(parents=True, exist_ok=True)
        for name, (row, width) in TILES.items():
            tile = sheet.crop((0, row*160, width, (row+1)*160))
            # Keep a little alpha margin so wrinkles and torn fibers are not
            # clipped by the rectangular bitmap bounds.
            card = Image.new("RGBA", (width+8, 168), (0,0,0,0))
            card.alpha_composite(tile, (4,4))
            verify_tile(name, card)
            target = OUT / (name + ".webp")
            card.save(target, "WEBP", lossless=True, method=5, exact=True)
            with Image.open(target) as reread:
                verify_tile(name, reread.convert("RGBA"))
            print("Approved antique paper:", target.name)

if __name__ == "__main__":
    main()
