"""Draw the launcher icon (96x96) and TV banner (320x180) into res/drawable-xhdpi.

Run with: python3 art.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent / "res" / "drawable-xhdpi"
WORDMARK = "/System/Library/Fonts/Supplemental/Arial Black.ttf"
TEXT = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
BG, GREEN, MUTED = (11, 14, 15), (83, 252, 24), (154, 164, 174)


def block_k(d, x, y, s, fill):
    """A blocky K on an s-by-s grid of 5x5 cells, after Kick's pixel-style lettering."""
    c = s / 5
    cells = [(0, r) for r in range(5)] + [(1, 2), (2, 1), (2, 3), (3, 0), (3, 4)]
    for col, row in cells:
        d.rectangle([x + col * c, y + row * c, x + (col + 1) * c - 1, y + (row + 1) * c - 1], fill=fill)


def icon():
    img = Image.new("RGBA", (96, 96))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([0, 0, 95, 95], radius=18, fill=GREEN)
    block_k(d, 23, 23, 50, BG)
    img.save(OUT / "icon.png")


def banner():
    img = Image.new("RGB", (320, 180), BG)
    d = ImageDraw.Draw(img)
    d.rounded_rectangle([24, 50, 104, 130], radius=14, fill=GREEN)
    block_k(d, 44, 70, 40, BG)
    d.text((122, 50), "KICK", font=ImageFont.truetype(WORDMARK, 46), fill=GREEN)
    d.text((124, 108), "Шууд дамжуулалт", font=ImageFont.truetype(TEXT, 19), fill=MUTED)
    img.save(OUT / "banner.png")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    banner()
