"""Draw the launcher icon (96x96) and TV banner (320x180) into res/drawable-xhdpi.

Run with: python3 art.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent / "res" / "drawable-xhdpi"
FONT = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
BG_TOP, BG_BOTTOM = (16, 37, 74), (40, 86, 145)
SUN, CLOUD = (255, 200, 61), (255, 255, 255)


def gradient(w, h):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        t = y / (h - 1)
        c = tuple(round(a + (b - a) * t) for a, b in zip(BG_TOP, BG_BOTTOM))
        for x in range(w):
            px[x, y] = c
    return img


def sun_and_cloud(d, x, y, s):
    """Sun peeking over a cloud; (x, y) is the top-left of an s-by-s box."""
    d.ellipse([x + 0.12 * s, y + 0.10 * s, x + 0.58 * s, y + 0.56 * s], fill=SUN)
    for cx, cy, r in ((0.38, 0.62, 0.17), (0.58, 0.52, 0.22), (0.78, 0.66, 0.14)):
        d.ellipse([x + (cx - r) * s, y + (cy - r) * s, x + (cx + r) * s, y + (cy + r) * s], fill=CLOUD)
    d.rounded_rectangle([x + 0.22 * s, y + 0.62 * s, x + 0.92 * s, y + 0.80 * s], radius=0.09 * s, fill=CLOUD)


def icon():
    img = gradient(96, 96)
    mask = Image.new("L", (96, 96), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, 95, 95], radius=18, fill=255)
    sun_and_cloud(ImageDraw.Draw(img), 4, 6, 88)
    out = Image.new("RGBA", (96, 96))
    out.paste(img, (0, 0), mask)
    out.save(OUT / "icon.png")


def banner():
    img = gradient(320, 180)
    d = ImageDraw.Draw(img)
    sun_and_cloud(d, 14, 34, 112)
    d.text((134, 58), "Цаг агаар", font=ImageFont.truetype(FONT, 34), fill=CLOUD)
    d.text((136, 104), "Улаанбаатар", font=ImageFont.truetype(FONT, 20), fill=(184, 199, 221))
    img.save(OUT / "banner.png")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    banner()
