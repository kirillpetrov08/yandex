# App content: что выбирать в формах Play Console

## Ads

- **Does your app contain ads?** — Yes.
- Реклама в приложении: баннер (меню, магазин, статистика, настройки), межстраничная (после партии),
  реклама с вознаграждением (продолжить игру, удвоить монеты, бесплатные монеты),
  App Open (запуск и возврат из фона), нативная (магазин).

## Content rating (анкета IARC)

Ответы для нашей игры:

| Вопрос | Ответ |
| --- | --- |
| Violence / blood / sexual content / profanity / drugs / gambling | No |
| Scary or disturbing content | No |
| Simulated gambling | No |
| User-generated content / user-to-user communication / sharing location | No |
| Allows users to interact or share info | No |
| Digital purchases | No |
| Contains ads | **Yes** |
| Miscellaneous: Does the app contain content that may be considered controversial? | No |

Ожидаемый результат: **3+ / Everyone** (в RU-локализации — «Для всех»).

## Target audience and content

| Вопрос | Ответ | Комментарий |
| --- | --- | --- |
| Age groups | **13+** (только эта категория) | если выбрать «до 13 лет», включится Families Policy: придётся убрать рекламный ID и ограничить рекламу |
| Is your app designed for children? | No | |
| Appeal to children? | No | |
| Store presence: made for families? | No | |
| News app / COVID / government app / financial / health | No | |

## Data safety

Заполняйте по `data-safety.md` — там готовые ответы.

## Privacy policy

- Ссылка: `https://kirillpetrov08.github.io/yandex/privacy-policy.html`
  (файл `privacy-policy.html` лежит в корне репозитория — включите GitHub Pages).
- Та же ссылка должна быть указана в Store listing → Developer website / Privacy policy.

## App access

- Все функции доступны без ограничений: **All functionality is available without special access**.
- Логина нет, поэтому реквизиты для проверяющего не нужны.

## Government apps / Financial features / Health

- Все три раздела: **No**.

## Store settings

| Поле | Значение |
| --- | --- |
| Category | Games → Puzzle |
| Tags | Puzzle (может отличаться; выберите максимум два: Puzzle, Casual) |
| Contact email | kirillpetrov08@gmail.com |
| Website | сайт разработчика (там же `app-ads.txt`) |
| Privacy policy URL | ссылка на политику |

## Подпись (App integrity)

- Включите **Play App Signing** (по умолчанию) — ключ подписи Play хранит у себя,
  ваш upload key нужен только для загрузки AAB.
- После первой загрузки добавьте отпечаток подписи Play в свои сервисы (если появятся серверные
  сервисы, которым нужен SHA-256).

## Порядок заполнения, если хочется «сделать всё за раз»

1. App content (privacy policy → ads → content rating → target audience → data safety → the rest).
2. Store listing (тексты из `ru.md`/`en.md`, графика из `icon-512.png`,
   `feature-graphic-1024x500.png`, `screenshots/`).
3. Store settings (категория, контакты, сайт).
4. Production → Create new release → загрузить AAB → Notes из `release-notes.md`.
