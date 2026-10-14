#!/usr/bin/env python3
"""Генерация store-скриншотов игры BlockPuzzle средствами ImageMagick (нужен `convert`).

Рисуем честную копию экранов: та же палитра BoardTheme (ClassicTheme),
те же подписи из strings.xml, та же геометрия доски 8x8 и лотка из 3 фигур.
Каждый скриншот собирается несколькими вызовами convert (draw + текст),
чтобы генерация занимала секунды, а не минуты.
"""
import os
import struct
import subprocess
import tempfile

W, H = 1080, 1920
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "screenshots")
TMP = tempfile.gettempdir()
os.makedirs(OUT, exist_ok=True)

CELLS = ["#FFD166", "#EF476F", "#06D6A0", "#4CC9F0", "#B388FF", "#FF9F45", "#F15BB5"]
EMPTY = "#232A4A"
ACCENT = "#7C5CFF"
TEXT = "#FFFFFF"
MUTED = "#A7ADCF"

FONT = "DejaVu-Sans"
FONT_B = "DejaVu-Sans-Bold"


def convert(args):
    subprocess.run(["convert", *args], check=True)


def canvas(path, glow_color="rgba(124,92,255,0.42)", glow_cy=140):
    bg = os.path.join(TMP, "_bp_bg.png")
    glow = os.path.join(TMP, "_bp_glow.png")
    convert(["-size", f"{W}x{H}", "gradient:#1B2050-#0A0C1C", bg])
    convert(["-size", f"{W}x{H}", "xc:none", "-fill", glow_color,
             "-draw", f"ellipse 540,{glow_cy} 760,420 0,360", "-blur", "0x140", glow])
    convert([bg, glow, "-compose", "over", "-composite", path])


def draw_shapes(path, draws):
    """Один вызов convert со всеми векторными элементами (быстро)."""
    args = [path]
    for d in draws:
        args += ["-draw", d]
    args += [path]
    convert(args)


def rr(x0, y0, x1, y1, r, fill, stroke=None, sw=2):
    s = f"fill {fill} "
    s += f"stroke {stroke} stroke-width {sw} " if stroke else "stroke none "
    s += f"roundrectangle {int(x0)},{int(y0)} {int(x1)},{int(y1)} {r},{r}"
    return s


def text(path, items):
    """items: список (x, y, string, font, size, fill, align) — align: flat|north."""
    args = [path]
    for x, y, s, font, size, fill, align in items:
        args += ["-font", font, "-pointsize", str(size), "-fill", fill]
        if align == "north":
            args += ["-gravity", "north", "-annotate", f"+{x}+{y}", s, "-gravity", "none"]
        else:
            args += ["-annotate", f"+{x}+{y}", s]
    args += [path]
    convert(args)


# --------------------------------- доска ---------------------------------

def board_draws(ox, oy, cells, size=88, gap=11, pad=20, cols=8, rows=8):
    bw = pad * 2 + cols * size + (cols - 1) * gap
    bh = pad * 2 + rows * size + (rows - 1) * gap
    draws = [rr(ox - 8, oy - 8, ox + bw + 8, oy + bh + 8, 30,
                "rgba(255,255,255,0.05)", "rgba(255,255,255,0.10)", 2)]
    for r in range(rows):
        for c in range(cols):
            x = ox + pad + c * (size + gap)
            y = oy + pad + r * (size + gap)
            color = cells.get((r, c), EMPTY)
            draws.append(rr(x, y, x + size, y + size, size // 4, color))
            if color != EMPTY:
                draws.append(rr(x + 9, y + 9, x + size // 2, y + 16, 6, "rgba(255,255,255,0.20)"))
    return draws, bw, bh


def shape_draws(cx, cy, mask, cell=40, gap=4, color="#FFD166", radius=10):
    rows = len(mask)
    cols = max(len(r) for r in mask)
    ox = cx - (cols * cell + (cols - 1) * gap) / 2
    oy = cy - (rows * cell + (rows - 1) * gap) / 2
    draws = []
    for r, line in enumerate(mask):
        for c, ch in enumerate(line):
            if ch != "#":
                continue
            x = ox + c * (cell + gap)
            y = oy + r * (cell + gap)
            draws.append(rr(x, y, x + cell, y + cell, radius, color))
    return draws


def tray_draws(shapes, y, slot_w=300, gap=20, h=140, x0=90):
    draws = []
    for i, sh in enumerate(shapes):
        x = x0 + i * (slot_w + gap)
        draws.append(rr(x, y, x + slot_w, y + h, 28,
                        "rgba(255,255,255,0.04)", "rgba(255,255,255,0.12)", 2))
        if sh:
            mask, color = sh
            draws += shape_draws(x + slot_w / 2, y + h / 2, mask, cell=38, gap=3, color=color)
    return draws


# ------------------------------- элементы UI -------------------------------

def pill_draws(x, y, w, h, label, value, value_color=TEXT, size=32):
    """Пилюля в две строки: подпись сверху мелким, значение снизу крупным."""
    draws = [rr(x, y, x + w, y + h, 24, "rgba(255,255,255,0.07)",
                "rgba(255,255,255,0.16)", 2)]
    texts = [
        (x + 26, y + 32, label, FONT, size - 8, MUTED, "flat"),
        (x + 26, y + h - 22, str(value), FONT_B, size, value_color, "flat"),
    ]
    return draws, texts


def button_draws(x, y, w, h, label, sub=None, fill=ACCENT, size=40, radius=30):
    draws = [rr(x, y, x + w, y + h, radius, fill)]
    texts = []
    if sub:
        texts.append((0, y + 24, label, FONT_B, size, TEXT, "north"))
        texts.append((0, y + 84, sub, FONT, 30, "rgba(255,255,255,0.85)", "north"))
    else:
        texts.append((0, y + h // 2 - size // 2 - 6, label, FONT_B, size, TEXT, "north"))
    return draws, texts


def banner_draws(y, label, texts):
    draws = [rr(60, y, W - 60, y + 120, 16, "rgba(255,255,255,0.06)",
                "rgba(255,255,255,0.22)", 3)]
    texts.append((0, y + 40, label, FONT, 34, MUTED, "north"))
    return draws


# -------------------------------- экраны --------------------------------

def screen_gameplay(path, lang):
    canvas(path)
    board = {
        (0, 1): CELLS[2], (0, 2): CELLS[2],
        (1, 0): CELLS[0], (1, 1): CELLS[4], (1, 3): CELLS[3],
        (2, 4): CELLS[1], (2, 5): CELLS[1], (2, 6): CELLS[5],
        (3, 2): CELLS[4], (3, 3): CELLS[4], (3, 7): CELLS[6],
        (4, 0): CELLS[5], (4, 5): CELLS[3], (4, 6): CELLS[3], (4, 7): CELLS[6],
        (5, 1): CELLS[2], (5, 2): CELLS[0], (5, 6): CELLS[1],
        (6, 3): CELLS[3], (6, 4): CELLS[0], (6, 5): CELLS[5],
        (7, 0): CELLS[6], (7, 1): CELLS[6], (7, 5): CELLS[4],
    }
    draws, texts = [], []
    draws += board_draws(90, 560, board)[0]
    for x, (lab, val, col) in zip((80, 400, 720),
                                  (("Очки", "1 240", "#FFFFFF") if lang == "ru"
                                   else ("Score", "1 240", "#FFFFFF"),
                                   ("Рекорд", "3 480", "#FFFFFF") if lang == "ru"
                                   else ("Best", "3 480", "#FFFFFF"),
                                   ("Комбо", "×3", "#06D6A0") if lang == "ru"
                                   else ("Combo", "×3", "#06D6A0"))):
        d, t = pill_draws(x, 220, 280, 96, lab, val, value_color=col)
        draws += d
        texts += t
    texts.append((0, 400, "Собери ряд — он исчезнет" if lang == "ru"
                  else "Fill a row to clear it", FONT_B, 50, TEXT, "north"))
    draws += tray_draws([
        (["####", "####"], CELLS[3]),
        (["###", "#..", "#.."], CELLS[0]),
        (["#", "##"], CELLS[1]),
    ], y=1420)
    d, t = button_draws(390, 1650, 300, 96, "Пауза" if lang == "ru" else "Pause",
                        fill="rgba(255,255,255,0.12)", size=38)
    draws += d
    texts += t
    draw_shapes(path, draws)
    text(path, texts)


def screen_combo(path, lang):
    canvas(path, glow_color="rgba(6,214,160,0.35)")
    board = {}
    for r in range(8):
        for c in range(7):
            board[(r, c)] = CELLS[(r + c) % 7]
    draws, texts = [], []
    draws += board_draws(90, 560, board)[0]
    for x, (lab, val, col) in zip((80, 400, 720),
                                  (("Очки", "5 610", "#FFFFFF") if lang == "ru"
                                   else ("Score", "5 610", "#FFFFFF"),
                                   ("Рекорд", "5 610", "#FFFFFF") if lang == "ru"
                                   else ("Best", "5 610", "#FFFFFF"),
                                   ("Комбо", "×4", "#06D6A0") if lang == "ru"
                                   else ("Combo", "×4", "#06D6A0"))):
        d, t = pill_draws(x, 220, 280, 96, lab, val, value_color=col)
        draws += d
        texts += t
    texts.append((0, 400, "Убирай несколько линий сразу" if lang == "ru"
                  else "Clear several lines at once", FONT_B, 46, TEXT, "north"))
    # подсветка столбца, который собирается
    draws.append(rr(800, 550, 935, 1350, 26, "rgba(6,214,160,0.28)"))
    draws += tray_draws([
        (["##", "##"], CELLS[6]),
        (["#", "#", "#"], CELLS[2]),
        (["###", "..#"], CELLS[4]),
    ], y=1420)
    d, t = button_draws(390, 1650, 300, 96, "Пауза" if lang == "ru" else "Pause",
                        fill="rgba(255,255,255,0.12)", size=38)
    draws += d
    texts += t
    draw_shapes(path, draws)
    text(path, texts)


def screen_shop(path, lang):
    canvas(path)
    draws, texts = [], []
    texts.append((0, 140, "Магазин" if lang == "ru" else "Shop", FONT_B, 56, TEXT, "north"))
    d, t = pill_draws(380, 240, 320, 96,
                      "Монеты" if lang == "ru" else "Coins", "320", value_color="#FFD166")
    draws += d
    texts += t
    items = ([("Классика", "Выбрано"), ("Неон", "60"), ("Карамель", "80"), ("Океан", "100"),
              ("Лес", "140"), ("Моно", "160")]
             if lang == "ru" else
             [("Classic", "Equipped"), ("Neon", "60"), ("Candy", "80"), ("Ocean", "100"),
              ("Forest", "140"), ("Mono", "160")])
    for i, (name, price) in enumerate(items):
        col, row = i % 2, i // 2
        x = 90 + col * 460
        y = 390 + row * 280
        draws.append(rr(x, y, x + 420, y + 240, 32, "rgba(255,255,255,0.06)",
                        "rgba(255,255,255,0.14)", 2))
        for k in range(6):
            tx = x + 26 + (k % 3) * 70
            ty = y + 32 + (k // 3) * 70
            draws.append(rr(tx, ty, tx + 58, ty + 58, 15, CELLS[(i + k) % 7]))
        texts.append((x + 280, y + 88, name, FONT_B, 38, TEXT, "flat"))
        color = "#06D6A0" if price in ("Выбрано", "Equipped") else "#FFD166"
        texts.append((x + 280, y + 152, price, FONT, 34, color, "flat"))
    draws += banner_draws(1700, "Реклама" if lang == "ru" else "Ad", texts)
    draw_shapes(path, draws)
    text(path, texts)


def screen_gameover(path, lang):
    canvas(path, glow_color="rgba(239,71,111,0.35)")
    board = {(r, c): CELLS[(r * 3 + c) % 7] for r in range(8) for c in range(8)}
    for key in [(7, 0), (7, 2), (6, 5), (5, 1)]:
        board.pop(key, None)
    draws, texts = [], []
    draws += board_draws(90, 280, board, size=72, gap=9, pad=18)[0]
    draws.append("fill rgba(10,12,28,0.44) stroke none rectangle 0,0 1080,1920")
    draws.append(rr(80, 620, 1000, 1790, 40, "rgba(20,24,50,0.985)",
                    "rgba(124,92,255,0.45)", 3))
    texts.append((0, 680, "Игра окончена" if lang == "ru" else "Game over",
                  FONT_B, 62, TEXT, "north"))
    texts.append((0, 772, "Новый рекорд!" if lang == "ru" else "New record!",
                  FONT_B, 46, "#FFD166", "north"))
    d, t = pill_draws(140, 850, 380, 96, "Очки" if lang == "ru" else "Score", "6 120")
    draws += d
    texts += t
    d, t = pill_draws(560, 850, 380, 96, "Монеты" if lang == "ru" else "Coins", "+18",
                      value_color="#FFD166")
    draws += d
    texts += t
    for y, lab, sub, fill in (
            (990, "Продолжить" if lang == "ru" else "Continue",
             "Смотреть рекламу" if lang == "ru" else "Watch an ad", "#1F9D6B"),
            (1160, "Удвоить монеты" if lang == "ru" else "Double coins",
             "+18 → +36", "#1F9D6B"),
            (1330, "Играть снова" if lang == "ru" else "Play again", None, ACCENT),
            (1500, "В меню" if lang == "ru" else "Main menu", None, "rgba(255,255,255,0.12)")):
        d, t = button_draws(140, y, 800, 150, lab, sub, fill=fill)
        draws += d
        texts += t
    texts.append((0, 1690, "Награда — только за полный просмотр" if lang == "ru"
                  else "Reward only after a full view", FONT, 30, MUTED, "north"))
    draw_shapes(path, draws)
    text(path, texts)


SCREENS = {
    "01-gameplay": screen_gameplay,
    "02-combo": screen_combo,
    "03-shop": screen_shop,
    "04-game-over": screen_gameover,
}

if __name__ == "__main__":
    for lang in ("ru", "en"):
        for name, fn in SCREENS.items():
            out = f"{OUT}/phone-{lang}-{name}.png"
            fn(out, lang)
            convert([out, "-strip", "-define", "png:compression-level=9", out])
            b = open(out, "rb").read()
            w, h = struct.unpack(">II", b[16:24])
            print(f"{os.path.basename(out)}: {w}x{h} {len(b) // 1024} KB")
    print("DONE")
