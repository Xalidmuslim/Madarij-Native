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
card=ImageEnhance.Color(card).enhance(.60)
card=Image.blend(card,Image.new("RGB",(w,h),(250,240,222)),.45)
p=card.load()
for y in range(h):
    for x in range(w):
        distance=min(x,y,w-1-x,h-1-y)
        shade=max(0,int(8*(1-distance/24)**1.6)) if distance<24 else 0
        if y<24 or y>h-24:
            fold=(math.sin(x/37+math.sin(x/83))+math.sin(x/19)*.4)
            shade+=max(0,int(fold*1.5))
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
# The primary reading action uses just the leather material without the nav frame.
leather=source("navigation_leather.webp")
lw,lh=leather.size
leather_crop=leather.crop((int(lw*.16),int(lh*.24),int(lw*.84),int(lh*.76)))
leather_crop=ImageOps.fit(leather_crop,(860,120),method=Image.Resampling.LANCZOS)
leather_crop=ImageEnhance.Contrast(leather_crop).enhance(.80)
leather_crop=Image.blend(leather_crop,Image.new("RGB",leather_crop.size,(45,28,20)),.30)
leather_crop.save(D/"cta_leather.webp","WEBP",quality=75,method=6)
print("Generated: library_hero.webp, card_paper.webp, paper_sage.webp, cta_leather.webp")
