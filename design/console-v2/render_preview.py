"""Render review mockups, NOT Minecraft screenshots, with the shipped Havoc art/fonts.

Run with Pillow installed:
  python design/console-v2/render_preview.py --output /path/to/previews
No game files are modified and no external imagery is downloaded.
"""
from argparse import ArgumentParser
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/havoc"
WIDTH, HEIGHT = 1920, 1080
TEXT = (243, 246, 251, 255)
MUTED = (184, 194, 207, 255)
LABELS = ("Одиночная игра", "Мультиплеер", "Аккаунты", "Настройки", "Выход")


def font(size, weight="regular"):
    return ImageFont.truetype(str(ASSETS / f"fonts/inter-{weight}.otf"), size)


def text(image, content, x, y, size, weight="regular", color=TEXT):
    ImageDraw.Draw(image).text((x, y), content, font=font(size, weight), fill=color, anchor="lt")


def tracking(image, content, x, y, size, spacing, color=MUTED):
    draw = ImageDraw.Draw(image)
    face = font(size, "medium")
    for character in content:
        draw.text((x, y), character, font=face, fill=color, anchor="lt")
        x += draw.textlength(character, font=face) + spacing


def round_rect(image, box, radius, fill=None, outline=None, width=1):
    ImageDraw.Draw(image).rounded_rectangle(box, radius, fill=fill, outline=outline, width=width)


def gradient(image, start, end, horizontal=True):
    length = image.width if horizontal else image.height
    strip = Image.new("RGBA", (length, 1) if horizontal else (1, length))
    strip.putdata([
        tuple(round(a + (b - a) * i / max(1, length - 1)) for a, b in zip(start, end))
        for i in range(length)
    ])
    image.alpha_composite(strip.resize(image.size))


def art(name):
    return Image.open(ASSETS / f"textures/gui/{name}.png").convert("RGBA")


def icon(image, kind, cx, cy, size=40, color=TEXT):
    """Simple, consistent utility symbols (no unrelated pink illustration or heart)."""
    d = ImageDraw.Draw(image)
    stroke = max(2, round(size / 15))
    if kind == "profile":
        r = size * .18
        d.ellipse((cx-r, cy-size*.37, cx+r, cy-size*.01), outline=color, width=stroke)
        d.arc((cx-size*.38, cy-size*.02, cx+size*.38, cy+size*.66), 180, 360, fill=color, width=stroke)
    elif kind == "settings":
        # Use the same settings icon resource already shipped in the client.
        im = Image.open(ASSETS / "textures/gui/icons/settings.png").convert("RGBA")
        im = im.resize((size, size), Image.Resampling.LANCZOS)
        alpha = im.getchannel("A")
        solid = Image.new("RGBA", im.size, color)
        solid.putalpha(alpha)
        image.alpha_composite(solid, (round(cx-size/2), round(cy-size/2)))
    elif kind == "power":
        r = size * .36
        d.arc((cx-r, cy-r, cx+r, cy+r), -48, 228, fill=color, width=stroke)
        d.line((cx, cy-size*.48, cx, cy), fill=color, width=stroke)


def cover_tile(image, x, y, size, index, selected):
    if index < 2:
        # Crop to cover; never squeeze a landscape into a square.
        source = art("console_background" if index == 0 else "console_servers")
        tile = ImageOps.fit(source, (size, size), Image.Resampling.LANCZOS, centering=(.76, .48))
        gradient(tile, (3, 8, 16, 0), (3, 8, 16, 60), horizontal=False)
    else:
        tile = Image.new("RGBA", (size, size), (17, 25, 39, 255))
        gradient(tile, (53, 70, 97, 38), (5, 10, 19, 110), horizontal=False)
        icon(tile, ("profile", "settings", "power")[index-2], size/2, size/2, round(size*.37))
    mask = Image.new("L", tile.size)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, size-1, size-1), radius=18, fill=255)
    shadow = Image.new("RGBA", image.size)
    round_rect(shadow, (x-2, y+8, x+size+2, y+size+13), 20, (0, 0, 0, 100))
    image.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(12)))
    image.paste(tile, (x, y), mask)
    if selected:
        round_rect(image, (x-7, y-7, x+size+7, y+size+7), 25, outline=(190, 204, 224, 235), width=2)
        # No permanent double border or neon glow.


def pill(image, label, x, y, width=290):
    round_rect(image, (x, y, x+width, y+66), 33, (31, 37, 49, 255))
    face = font(24, "semibold")
    length = ImageDraw.Draw(image).textlength(label, font=face)
    text(image, label, x+(width-length)/2, y+21, 24, "semibold")


def render(selected, output):
    image = ImageOps.fit(art("console_background" if selected == 0 else "console_servers"),
                         (WIDTH, HEIGHT), Image.Resampling.LANCZOS)
    gradient(image, (5, 9, 16, 233), (5, 9, 16, 35))
    gradient(image, (3, 7, 14, 26), (3, 7, 14, 157), horizontal=False)
    top = Image.new("RGBA", (WIDTH, 330))
    gradient(top, (3, 7, 14, 115), (3, 7, 14, 0), horizontal=False)
    image.alpha_composite(top)

    text(image, "Игры", 96, 49, 31, "semibold")
    text(image, "Коллекция", 241, 50, 30, color=(161, 171, 187, 255))
    icon(image, "settings", 1570, 64, 29)
    round_rect(image, (1629, 40, 1677, 88), 24, (35, 45, 61, 255))
    icon(image, "profile", 1653, 65, 28)
    text(image, "14:26", 1740, 48, 31, color=(214, 221, 232, 255))

    x = 96
    selected_rect = None
    for index in range(5):
        size = 180 if index == selected else 108
        cover_tile(image, x, 135, size, index, index == selected)
        if index == selected:
            selected_rect = (x, size)
        x += size + 18
    x, size = selected_rect
    text(image, LABELS[selected], x+size+20, 266, 27, color=(218, 225, 236, 255))

    # Compact franchise/section lockup, not a full-width marketing slogan.
    tracking(image, "HAVOC", 174, 566, 17, 5, (166, 181, 202, 255))
    text(image, "MINECRAFT", 170, 603, 60, "bold")
    text(image, LABELS[selected], 174, 681, 30, color=(218, 226, 240, 255))
    description = "Мир, в который хочется вернуться." if selected == 0 else "Любимые серверы. Новые приключения."
    text(image, description, 174, 757, 23, color=(180, 192, 210, 255))
    pill(image, "Играть" if selected == 0 else "Выбрать сервер", 174, 836)
    round_rect(image, (481, 836, 547, 902), 33, (31, 37, 49, 255))
    for cx in (503, 514, 525):
        ImageDraw.Draw(image).ellipse((cx-2, 867, cx+2, 871), fill=TEXT)
    tracking(image, "JAVA EDITION", 1623, 849, 13, 2, (141, 157, 180, 255))
    text(image, "Minecraft 26.2", 1623, 876, 18, color=(178, 190, 210, 255))

    text(image, "← →   Выбор      Enter   Открыть      Tab   Навигация", 174, 1018, 16,
         color=(137, 153, 177, 255))
    text(image, "Дизайн-превью v2 · не скриншот Minecraft", 1375, 1020, 15,
         color=(137, 153, 177, 255))
    output.parent.mkdir(parents=True, exist_ok=True)
    image.convert("RGB").save(output, optimize=True)
    return output


if __name__ == "__main__":
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "build/design/console-v2")
    args = parser.parse_args()
    for selected, filename in enumerate(("main-menu-worlds-v2.png", "main-menu-servers-v2.png")):
        print(render(selected, args.output / filename))
