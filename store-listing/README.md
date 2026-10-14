# Материалы для Google Play

Всё, что нужно для страницы приложения и форм Play Console. Файлы готовы — остаётся скопировать
текст в Play Console и загрузить картинки.

| Файл | Куда идёт в Play Console |
| --- | --- |
| `icon-512.png` | Store listing → App icon (512×512, без прозрачности) |
| `feature-graphic-1024x500.png` | Store listing → Feature graphic (1024×500) |
| `screenshots/phone-ru-*.png` | Store listing → Phone screenshots (русская локализация) |
| `screenshots/phone-en-*.png` | Store listing → Phone screenshots (English) |
| `ru.md` | Store listing → русская локализация (название, описания) |
| `en.md` | Store listing → English (US) (name, descriptions) |
| `data-safety.md` | App content → Data safety (готовые ответы формы) |
| `app-content.md` | App content: объявления, возрастной рейтинг, целевая аудитория |
| `release-notes.md` | Release → Notes (что нового, ru/en) |
| `make-screenshots.py` | не для Play: пересборка скриншотов после редизайна |

Скриншоты сгенерированы как точная копия интерфейса игры: те же цвета `ClassicTheme`,
та же доска 8×8, лоток из трёх фигур и подписи из `strings.xml` (ru/en).
Пересобрать можно скриптом `make-screenshots.py` (нужен ImageMagick: `python3 make-screenshots.py`).
Правила Play требуют, чтобы скриншоты соответствовали реальному интерфейсу, поэтому после
крупного редизайна интерфейса их нужно обновить.

Чек-лист требований Play к графике: иконка 512×512 PNG **без альфа-канала**,
feature graphic ровно 1024×500 без важных деталей по краям, скриншоты телефона —
от 2 до 8 штук, минимум 320 px по короткой стороне (у нас 1080×1920).
