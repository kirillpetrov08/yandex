# Материалы для магазинов — Focus

Готовые исходники карточки и медиаматериалы для отдельного приложения `com.ayaks.focus`.

| Материал | Файл |
| --- | --- |
| Русская карточка | [`ru.md`](ru.md) |
| English store listing | [`en.md`](en.md) |
| Data Safety и рекламное раскрытие | [`data-safety.md`](data-safety.md) |
| Политика конфиденциальности (для публикации на сайте) | [`../../site/focus-privacy-policy.html`](../../site/focus-privacy-policy.html) |
| Feature graphic Google Play 1024×500 | [`feature-graphic-1024x500.png`](feature-graphic-1024x500.png) |
| Иконка 512×512 | [`../../mind/store/icon-512.png`](../../mind/store/icon-512.png) |
| Скриншоты 1080×1920 | [`screenshots/`](screenshots/) |
| Воспроизводимый генератор графики | [`generate-screenshots.py`](generate-screenshots.py) |

Скриншоты — подготовленные store-preview изображения, а не необработанные захваты с устройства. Перед отправкой проверьте все реальные тексты и визуальные детали на финальной сборке.

## Пересоздать графику

Нужен Python 3 и Pillow:

```bash
python3 -m venv .venv
. .venv/bin/activate        # Windows: .venv\Scripts\activate
python -m pip install -r store-listing/focus/requirements-screenshots.txt
python store-listing/focus/generate-screenshots.py
```

Генератор создаёт PNG-файлы в этой папке и `screenshots/`. Виртуальное окружение не коммитить.

## Что ещё заполнить в кабинетах

- **Google Play:** приложение → описание/локали из `ru.md` (при публикации на английском — `en.md`), privacy URL, category Games/Puzzle, feature graphic и 2–8 фактических скриншотов. Выполнить content-rating, ads и Data Safety анкеты для итогового AAB; документ `data-safety.md` — ориентир, а не замена обязательной самостоятельной сверке объединённого manifest/SDK disclosures.
- **RuStore:** создать карточку, вставить русские тексты, загрузить иконку, скриншоты и политику. Проставить возраст по актуальной анкете RuStore; не копировать автоматически возрастной рейтинг другого магазина.
- **Оба магазина:** support `kirillpetrov08@gmail.com`, developer `Ayaks`; package `com.ayaks.focus`. Не заявлять медицинские/диагностические свойства.

## Порядок рекламы

Сначала создать приложение `com.ayaks.focus` и один adaptive banner в Yandex Ads dashboard. Скопировать точный production ID в `focus.bannerUnitId` в `gradle.properties`, выполнить release build и проверить баннер в разрешённых неигровых экранах. ID пуст по умолчанию; демо-ID и чужие ID в release не подставляются. Не нажимать на собственные объявления.
