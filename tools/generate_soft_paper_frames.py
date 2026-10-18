#!/usr/bin/env python3
"""Reproducible, transparent paper sheets for Android Compose nine-slice cards.

No metallic trim, dark outlines, corner plates, built-in arrows, or text.
Native Compose supplies all icons and content separately.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math
import random

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "app/src/main/res/drawable-nodpi"
OUT.mkdir(parents=True, exist_ok=True)
SPECS = {
    "frame_light_wide": ((248,237,212),640,176,11),
    "frame_light_compact": ((249,239,218),640,176,17),
    "frame_section_numbered": ((249,238,216),640,176,23),
    "frame_search": ((249,239,220),640,176,31),
    "frame_font_settings": ((248,236,211),320,176,41),
    "frame_dark_wide": ((70,48,35),640,176,53),
    "frame_dark_compact": ((75,52,38),640,176,59),
}

def write_paper(name, base, width, height, seed):
    rnd = random.Random(seed)
    light = not name.startswith("frame_dark")
    shape = Image.new("L",(width,height),0)
    d = ImageDraw.Draw(shape)
    x0,x1,top,bottom = 9,width-10,10,height-12
    rad = 24
    def wobble(x,salt):
        return 1.25*math.sin(x/24+salt)+.85*math.sin(x/9.1+salt*1.8)
    pts=[]
    for x in range(x0+rad,x1-rad+1,3):
        pts.append((x,top+wobble(x,seed)))
    for deg in range(270,361,6):
        a=math.radians(deg)
        pts.append((x1-rad+rad*math.cos(a),top+rad+rad*math.sin(a)))
    for y in range(top+rad,bottom-rad+1,4):
        pts.append((x1+.5*math.sin(y/15+seed),y))
    for deg in range(0,91,6):
        a=math.radians(deg)
        pts.append((x1-rad+rad*math.cos(a),bottom-rad+rad*math.sin(a)))
    for x in range(x1-rad,x0+rad-1,-3):
        pts.append((x,bottom+wobble(x,seed+8)))
    for deg in range(90,181,6):
        a=math.radians(deg)
        pts.append((x0+rad+rad*math.cos(a),bottom-rad+rad*math.sin(a)))
    for y in range(bottom-rad,top+rad-1,-4):
        pts.append((x0+.5*math.sin(y/18+seed),y))
    for deg in range(180,271,6):
        a=math.radians(deg)
        pts.append((x0+rad+rad*math.cos(a),top+rad+rad*math.sin(a)))
    d.polygon(pts, fill=255)
    shape=shape.filter(ImageFilter.GaussianBlur(.9))
    alpha=shape.load()
    shadow_mask=shape.filter(ImageFilter.GaussianBlur(2.5)).load()
    shadow_alpha=Image.new("L",(width,height),0)
    sap=shadow_alpha.load()
    for y in range(height-2):
        for x in range(width):
            sap[x,y+2]=round(shadow_mask[x,y]*(.10 if light else .11))
    shadow=Image.new("RGBA",(width,height),
        ((73,54,35) if light else (25,18,14))+(0,))
    shadow.putalpha(shadow_alpha)
    paper=Image.new("RGBA",(width,height))
    p=paper.load()
    for y in range(height):
        for x in range(width):
            a=alpha[x,y]
            if a<1:
                p[x,y]=base+(0,)
                continue
            distance=min(x-x0,x1-x,y-top,bottom-y)
            age=max(0,7-max(0,distance)*.7)
            cloud=2.1*math.sin(x/61+seed)*math.sin(y/34-seed)+1.2*math.sin(x/113+y/24)
            tint=cloud+rnd.uniform(-3.1,3.1)-age
            if rnd.randrange(1500)==0: tint-=9
            rgb=tuple(max(0,min(255,int(c+tint*factor)))
                for c,factor in zip(base,(1,.92,.75)))
            p[x,y]=rgb+(round(a*(.96 if light else 1.0)),)
    shadow.alpha_composite(paper)
    path=OUT/(name+".webp")
    shadow.save(path,"WEBP",lossless=True,method=5,exact=True)
    with Image.open(path) as verify:
        assert verify.mode=="RGBA"
        assert verify.getpixel((0,0))[3]==0
        assert verify.getpixel((width//2,height//2))[3]>240
    print("Soft alpha parchment:",path.name)

if __name__=="__main__":
    for key,args in SPECS.items():
        write_paper(key,*args)
