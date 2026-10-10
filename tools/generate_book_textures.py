#!/usr/bin/env python3
"""Rebuild book bitmaps at compile time, not during Compose frame rendering."""
from PIL import Image, ImageEnhance, ImageOps
from pathlib import Path
import math
ROOT=Path(__file__).resolve().parents[1]
D=ROOT/"app/src/main/res/drawable-nodpi"
O=ROOT/".book-original-images"
O.mkdir(exist_ok=True)
def source(name):
    src=O/name
    if not src.exists(): src.write_bytes((D/name).read_bytes())
    return Image.open(src).convert("RGB")
paper=source("reference_paper.webp")
hero=source("library_hero.webp")
w,h=hero.size
alpha=Image.new("L",(w,h),255)
px=alpha.load()
for x in range(w):
    edge=4*math.sin(x/21)+2*math.sin(x/8)
    start=h*.54+edge
    stop=h*.98+edge
    for y in range(max(0,int(start)),h):
        t=max(0.,min(1.,(y-start)/(stop-start)))
        px[x,y]=int(255*(1-t*t*(3-2*t)))
result=hero.convert("RGBA")
result.putalpha(alpha)
result.save(D/"library_hero.webp","WEBP",quality=83,method=6)
# Static quiet-center folded-paper frame used by existing card components.
w,h=600,300
card=ImageOps.fit(paper,(w,h),method=Image.Resampling.LANCZOS)
card=ImageEnhance.Color(card).enhance(.7)
card=Image.blend(card,Image.new("RGB",(w,h),(249,238,219)),.38)
p=card.load()
for y in range(h):
    for x in range(w):
        distance=min(x,y,w-1-x,h-1-y)
        shade=max(0,int(10*(1-distance/20)**1.5)) if distance<20 else 0
        if y<32 or y>h-32:
            shade+=max(0,int(math.sin(x/29+math.sin(x/83))*2))
        r,g,b=p[x,y]
        p[x,y]=(max(0,r-shade),max(0,g-shade),max(0,b-shade))
card.save(D/"card_paper.webp","WEBP",quality=74,method=6)
# Old paper fibers preserved, warm desaturated sage-ivory instead of cold green.
# Use the visibly fibrous antique page rather than the nearly flat old sage.
# Keep aged corners but avoid cold green. This texture has no runtime cost.
size=source("paper_sage.webp").size
gray=ImageOps.grayscale(paper.resize(size,Image.Resampling.LANCZOS))
# Restore the antique fibers, worn corners and creases which were too faint.
# Slightly warmer ivory-sage, desaturated and brighter than the warm parchment.
gray=ImageEnhance.Contrast(gray).enhance(1.47)
gray=ImageEnhance.Brightness(gray).enhance(.99)
warm=ImageOps.colorize(gray,black=(176,163,135),white=(253,249,235))
warm=Image.blend(warm,Image.new("RGB",warm.size,(245,241,228)),.12)
warm.save(D/"paper_sage.webp","WEBP",quality=83,method=6)
print("Generated: library_hero.webp, card_paper.webp, paper_sage.webp")
