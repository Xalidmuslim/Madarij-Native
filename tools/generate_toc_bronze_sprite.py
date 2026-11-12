#!/usr/bin/env python3
"""Generate one consistent pre-rendered metallic TOC atlas, instead of a collage."""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "app/src/main/res/drawable-nodpi/toc_bronze_sprite.png"
FONTS = (
    ROOT / "app/src/main/res/font/russian_book.ttf",
    Path("/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"),
)
TILE, SCALE = 96, 4
SIDE = TILE * SCALE
SYMBOLS = "0123456789›⌄↳"


def mask_for(ch):
    tile = Image.new("L", (SIDE, SIDE), 0)
    draw = ImageDraw.Draw(tile)
    if ch.isdigit():
        font = next((p for p in FONTS if p.is_file()), None)
        if font is None:
            raise FileNotFoundError("No book font found")
        ink = Image.new("L", (SIDE, SIDE), 0)
        ImageDraw.Draw(ink).text((SIDE // 2, SIDE // 2), ch,
            font=ImageFont.truetype(str(font), 286), anchor="mm", fill=255)
        bounds = ink.getbbox()
        if not bounds:
            raise ValueError("Empty glyph: " + ch)
        ink = ink.crop(bounds)
        ratio = min(59*SCALE/ink.width, 69*SCALE/ink.height)
        dim = (max(1,round(ink.width*ratio)), max(1,round(ink.height*ratio)))
        ink = ink.resize(dim, Image.Resampling.LANCZOS)
        tile.paste(ink, ((SIDE-dim[0])//2,(SIDE-dim[1])//2))
    else:
        thickness = 10*SCALE
        if ch == "›":
            vertices = [(42*SCALE,25*SCALE),(63*SCALE,48*SCALE),(42*SCALE,71*SCALE)]
        elif ch == "⌄":
            vertices = [(25*SCALE,39*SCALE),(48*SCALE,61*SCALE),(71*SCALE,39*SCALE)]
        else:
            vertices = [(26*SCALE,20*SCALE),(26*SCALE,64*SCALE),(68*SCALE,64*SCALE),(55*SCALE,51*SCALE)]
        draw.line(vertices, fill=255, width=thickness, joint="curve")
        r = thickness//2
        for x,y in (vertices[:2] if ch == "↳" else vertices):
            draw.ellipse((x-r,y-r,x+r,y+r),fill=255)
        if ch == "↳":
            draw.polygon([(71*SCALE,64*SCALE),(52*SCALE,49*SCALE),(52*SCALE,79*SCALE)],fill=255)
    return tile


def bronze(ch):
    silhouette = mask_for(ch)
    mask = np.asarray(silhouette,dtype=np.float32)/255
    extrusion = Image.new("L",(SIDE,SIDE),0)
    for step in range(8,0,-1):
        shifted = Image.new("L",(SIDE,SIDE),0)
        shifted.paste(silhouette,(step//2,step))
        extrusion = Image.fromarray(np.maximum(
            np.asarray(extrusion),np.asarray(shifted)).astype("uint8"))
    rear = np.asarray(extrusion,dtype=np.float32)/255
    y,x = np.mgrid[:SIDE,:SIDE].astype(np.float32)
    light = .70*(1-y/(SIDE-1)) + .30*(1-x/(SIDE-1))
    dark = np.array([139,79,36],dtype=np.float32)
    bright = np.array([255,212,125],dtype=np.float32)
    color = dark[None,None,:]+(bright-dark)[None,None,:]*light[:,:,None]
    eroded = np.asarray(silhouette.filter(ImageFilter.MinFilter(29)),dtype=np.float32)/255
    edge = np.clip(mask-eroded,0,1)
    smoothed = np.asarray(silhouette.filter(ImageFilter.GaussianBlur(4)),dtype=np.float32)/255
    gx,gy = np.gradient(smoothed,axis=1),np.gradient(smoothed,axis=0)
    shine = np.clip((-gx*1.6-gy*2.3)*8,0,1)*edge
    shade = np.clip((gx*1.2+gy*1.7)*7,0,1)*edge
    color += shine[:,:,None]*np.array([64,55,36])[None,None,:]
    color -= shade[:,:,None]*np.array([59,48,35])[None,None,:]
    color += (np.sin(x*.073+y*.031)*np.sin(y*.13)*2.5)[:,:,None]
    alpha = mask[:,:,None]
    bg = np.array([82,45,21],dtype=np.float32)
    rgba = np.empty((SIDE,SIDE,4),dtype=np.uint8)
    rgba[:,:,:3] = np.clip(color*alpha + bg[None,None,:]*(1-alpha),0,255)
    rgba[:,:,3] = np.clip(mask*255+rear*(1-mask)*234,0,255)
    return Image.fromarray(rgba,"RGBA").resize((TILE,TILE),Image.Resampling.LANCZOS)


def main():
    sprite = Image.new("RGBA",(TILE*4,TILE*4),(0,0,0,0))
    for i,ch in enumerate(SYMBOLS):
        sprite.alpha_composite(bronze(ch),((i%4)*TILE,(i//4)*TILE))
    DEST.parent.mkdir(parents=True,exist_ok=True)
    sprite.save(DEST,optimize=True)
    print("Uniform TOC atlas generated:", DEST, sprite.size)


if __name__ == "__main__":
    main()
