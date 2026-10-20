#!/usr/bin/env python3
"""Generate 1080x1920 Play/RuStore preview artwork. Install Pillow from requirements-screenshots.txt."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

OUT = Path(__file__).parent / "screenshots"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 1080, 1920
BG, PANEL, TILE = (16, 23, 45), (18, 27, 51), (27, 41, 69)
WHITE, MUTED, MINT, PURPLE = (243, 244, 255), (178, 187, 212), (140, 230, 210), (182, 162, 255)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

def font(size, bold=False): return ImageFont.truetype(BOLD if bold else FONT, size)
def text(d, x, y, s, size=28, color=WHITE, bold=False, anchor=None):
    d.text((x,y), s, font=font(size,bold), fill=color, anchor=anchor)
def roundrect(d, box, color, r=24, outline=None, width=2):
    d.rounded_rectangle(box, radius=r, fill=color, outline=outline, width=width)
def centered(d, x, y, s, size=28, color=WHITE, bold=False): text(d,x,y,s,size,color,bold,"mm")
def base(title, subtitle):
    im=Image.new("RGB",(W,H)); pix=im.load()
    top=(16,23,45); bottom=(18,39,62)
    for y in range(H):
        t=y/(H-1); c=tuple(round(top[i]*(1-t)+bottom[i]*t) for i in range(3))
        for x in range(W): pix[x,y]=c
    d=ImageDraw.Draw(im,"RGBA")
    d.ellipse((830,40,1190,400),fill=(182,162,255,12)); d.ellipse((-100,1470,360,1930),fill=(140,230,210,10))
    text(d,76,72,title,56,WHITE,True); text(d,78,146,subtitle,27,MUTED)
    roundrect(d,(58,244,1022,1814),PANEL,48,(255,255,255,34),2)
    text(d,540,1859,"ФОКУС  ·  короткая тренировка для ума",22,MUTED,True,"mm")
    return im,d
def logo(d,x=132,y=322,r=36):
    d.ellipse((x-r,y-r,x+r,y+r),fill=MINT); d.ellipse((x-r+6,y-r+6,x+r-6,y+r-6),fill=(23,35,61)); centered(d,x,y+1,"✦",35,MINT,True)
def save(im,name):
    im.save(OUT/(name+".png"),optimize=True)

# 01 Daily home screen
im,d=base("Пара минут для себя","Короткие упражнения, понятные с первого касания")
logo(d); text(d,190,296,"ФОКУС",34,WHITE,True); text(d,190,337,"короткая тренировка для ума",18,MUTED)
text(d,112,420,"Привет!",48,WHITE,True); text(d,112,480,"Удели себе пару минут.",25,MUTED)
roundrect(d,(96,560,984,1010),(37,61,91),34,(255,255,255,35)); text(d,142,604,"ЗАДАНИЕ ДНЯ",22,MINT,True); text(d,142,666,"Память",40,WHITE,True); text(d,142,725,"Запомни и повтори последовательность",20,(208,217,234)); centered(d,876,701,"✦",72,(255,210,122),True)
roundrect(d,(136,840,944,926),MINT,20); centered(d,540,883,"НАЧАТЬ ТРЕНИРОВКУ",24,(16,43,49),True)
for x,label,value in [(96,"дней подряд","7"),(556,"лучший счёт","420")]:
    roundrect(d,(x,1040,x+428,1215),TILE,24); text(d,x+42,1075,label,20,MUTED); text(d,x+42,1122,value,38,WHITE,True)
roundrect(d,(96,1240,984,1378),TILE,24); text(d,150,1274,"Выбрать режим",28,WHITE,True); text(d,150,1322,"Три упражнения на память и внимание",19,MUTED)
centered(d,540,1507,"Память     Внимание     Цвет",23,MINT,True); centered(d,540,1578,"Офлайн-игра · личная статистика",20,MUTED)
save(im,"phone-ru-01-home")

# 02 Memory
im,d=base("Прокачай память","Запомни световой узор и повтори его")
logo(d); text(d,188,305,"Память",31,WHITE,True); text(d,950,305,"120",29,MINT,True,"ra"); text(d,112,405,"РАУНД 3",20,MUTED,True); text(d,958,405,"♥ ♥ ♥",22,(255,135,146),True,"ra")
roundrect(d,(112,454,968,466),(41,54,83),8); roundrect(d,(112,454,482,466),MINT,8)
centered(d,540,545,"Смотри на подсветку и запомни порядок",24,WHITE,True)
for i in range(9):
    row,col=divmod(i,3); x=196+col*236; y=646+row*236; color=PURPLE if i==4 else MINT if i==1 else TILE
    roundrect(d,(x,y,x+188,y+188),color,30,(255,255,255,30))
    if i in (1,4): centered(d,x+94,y+94,"✦",46,WHITE,True)
centered(d,540,1462,"Повтори световую цепочку",24,MUTED); roundrect(d,(130,1530,950,1610),(255,255,255,12),18); centered(d,540,1570,"Каждый раунд добавляет один шаг",20,MINT,True)
save(im,"phone-ru-02-memory")

# 03 Search, one target has exactly the same tile styling as all others.
im,d=base("Замечай быстрее","Найди один знак среди похожих")
logo(d); text(d,188,305,"Внимание",31,WHITE,True); text(d,950,305,"80",29,MINT,True,"ra"); text(d,112,405,"Найди символ: ◆",26,WHITE,True); text(d,958,405,"32 с   ♥ ♥ ♥",20,MUTED,True,"ra")
roundrect(d,(112,454,968,466),(41,54,83),8); roundrect(d,(112,454,572,466),MINT,8)
seq=[0,2,3,0,1,0,2,3,2,0,3,0,2,0,3,0]; glyphs=["●","◆","▲","✚"]; colors=[MINT,PURPLE,(255,210,122),(255,147,168)]
for i,n in enumerate(seq):
    row,col=divmod(i,4); x=142+col*205; y=550+row*205
    roundrect(d,(x,y,x+170,y+170),TILE,24,(255,255,255,22)); centered(d,x+85,y+85,glyphs[n],41,colors[n],True)
centered(d,540,1450,"Один символ отличается от остальных",22,MUTED); centered(d,540,1520,"Большие области нажатия",20,MINT,True)
save(im,"phone-ru-03-search")

# 04 Stroop color mode
im,d=base("Смотри на цвет","Упражнение на переключение внимания")
logo(d); text(d,188,305,"Цвет",31,WHITE,True); text(d,950,305,"60",29,MINT,True,"ra"); text(d,112,405,"РАУНД 5",20,MUTED,True); text(d,958,405,"28 с   ♥ ♥ ♥",20,MUTED,True,"ra")
roundrect(d,(112,454,968,466),(41,54,83),8); roundrect(d,(112,454,522,466),MINT,8)
centered(d,540,630,"Какого цвета буквы?",27,MUTED); centered(d,540,760,"СИНИЙ",64,(255,209,102),True); centered(d,540,817,"Не читай слово — выбери цвет",21,MUTED)
for i,(label,col) in enumerate(zip(["КРАСНЫЙ","СИНИЙ","ЖЁЛТЫЙ","ЗЕЛЁНЫЙ"],[(255,100,124),(89,197,255),(255,209,102),(98,214,167)])):
    row,cl=divmod(i,2); x=136+cl*420; y=970+row*132; roundrect(d,(x,y,x+388,y+92),col,22); centered(d,x+194,y+46,label,21,(21,32,51),True)
centered(d,540,1430,"Тренируй переключение внимания",22,MUTED); roundrect(d,(130,1500,950,1580),(255,255,255,12),18); centered(d,540,1540,"Звук и вибрацию можно отключить",20,MINT,True)
save(im,"phone-ru-04-color")

# 05 Results
im,d=base("Видь свой прогресс","Личный результат остаётся у тебя")
logo(d,540,360,46); centered(d,540,490,"Тренировка завершена",39,WHITE,True); centered(d,540,548,"Задание дня · Память",23,MINT,True)
roundrect(d,(100,620,980,1185),TILE,32,(255,255,255,22)); centered(d,540,770,"240",104,(255,210,122),True); centered(d,540,830,"очков",24,MUTED)
for x,val,label in [(259,"6","раундов"),(540,"8","серия"),(821,"3","жизни")]:
    roundrect(d,(x-112,900,x+112,1035),(36,54,83),20); centered(d,x,946,val,34,MINT,True); centered(d,x,1000,label,16,MUTED)
roundrect(d,(142,1275,938,1359),MINT,20); centered(d,540,1317,"ЕЩЁ ОДНА ТРЕНИРОВКА",23,(16,43,49),True)
centered(d,540,1450,"История и личный рекорд",22,MUTED); centered(d,540,1520,"Хорошая работа — возвращайся завтра",20,MINT,True)
save(im,"phone-ru-05-results")
# Google Play feature graphic (1024x500), reusable for RuStore product pages.
fw,fh=1024,500
feature=Image.new("RGB",(fw,fh)); fp=feature.load()
for y in range(fh):
    t=y/(fh-1); color=tuple(round((16,23,45)[i]*(1-t)+(25,58,74)[i]*t) for i in range(3))
    for x in range(fw): fp[x,y]=color
fd=ImageDraw.Draw(feature,"RGBA")
fd.ellipse((720,-170,1160,270),fill=(182,162,255,18)); fd.ellipse((-150,320,220,690),fill=(140,230,210,18))
logo(fd,118,135,55); text(fd,205,62,"ФОКУС",28,MINT,True); text(fd,72,218,"Тренируй",59,WHITE,True); text(fd,72,285,"внимание",59,WHITE,True); text(fd,76,374,"Память · поиск · цвет",25,MUTED)
for i,(label,glyph,color) in enumerate([("ПАМЯТЬ","✦",PURPLE),("ПОИСК","◆",MINT),("ЦВЕТ","●",(255,210,122))]):
    x=540+i*150
    roundrect(fd,(x,128,x+128,340),TILE,28,(255,255,255,28))
    centered(fd,x+64,205,glyph,54,color,True); centered(fd,x+64,292,label,16,WHITE,True)
feature.save(OUT.parent/"feature-graphic-1024x500.png",optimize=True)
print("Generated five 1080x1920 previews and a 1024x500 feature graphic in", OUT.parent)
