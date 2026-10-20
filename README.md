# Блоки — головоломка (BlockPuzzle)

Готовая к публикации Android-игра: классическая блочная головоломка 8×8 на Kotlin и Jetpack
Compose с полностью интегрированной рекламой Рекламной сети Яндекса (Yandex Mobile Ads SDK 8.6).

Задача проекта: **деплой в Google Play остаётся единственным ручным шагом**. Код, рекламные точки,
тексты, графика для стора и документация — уже в репозитории.

## Что внутри

| Слой | Что сделано |
| --- | --- |
| Игра | Поле 8×8, лоток из 3 фигур, очистка линий и комбо, 7 тем, монеты, ежедневный бонус, статистика, 8 достижений, процедурный звук (без аудиофайлов) |
| Реклама | Banner (sticky), Interstitial, Rewarded, App Open, Native + частотные политики в одном месте |
| Приватность | Диалог согласия до инициализации SDK, переключатель персонализации в настройках, политика конфиденциальности |
| Сборка | Версии и ID блоков — в `gradle.properties`; подпись из `keystore.properties`; CI-workflow в `docs/ci/android.yml` (собирает APK/AAB и умеет публиковать в Play) |
| Публикация | `store-listing/` (тексты ru/en, Data Safety, App content, release notes, иконка, feature graphic, 8 скриншотов), `app-ads.txt`, `privacy-policy.html` |
| Знания | `docs/` — 13 документов: все форматы РСЯ, правила, модерация, выплаты, стратегия дохода, SDK-шпаргалка, чек-листы, troubleshooting |

## Второе приложение: «Фокус — тренажёр внимания»

В репозитории есть отдельный Android application-модуль `:mind` (`com.ayaks.focus`), самостоятельный лаунчер и собственное хранилище настроек/прогресса. Это не режим внутри BlockPuzzle: приложения устанавливаются отдельно и имеют отдельные version/application ID.

- Kotlin + Jetpack Compose, офлайн-игровой цикл: память, зрительный поиск и Stroop-подобное внимание к цвету.
- Ежедневная задача, локальный счёт/серия/рекорды; звук и вибрация отключаются.
- Один адаптивный баннер РСЯ на неигровых страницах. `focus.bannerUnitId` намеренно пуст: игра работает без SDK-рекламы, пока владелец не укажет свой ID блока в кабинете РСЯ.
- Материалы Google Play/RuStore, 5 скриншотов 1080×1920, feature graphic 1024×500, иконка и политика: [`store-listing/focus/`](store-listing/focus/).
- Инструкция для публикации именно Focus: [`store-listing/focus/DEPLOY.md`](store-listing/focus/DEPLOY.md).
- Команды проверки: `./gradlew :mind:testDebugUnitTest :mind:assembleDebug :mind:assembleRelease :mind:bundleRelease`.

**Важно:** сборка и тесты `:mind` в текущей среде ещё не подтверждены (локально отсутствует Java). Не загружайте APK/AAB в магазин, пока CI не завершит эти задачи успешно и приложение не проверено на устройстве.

## Быстрый старт

Пошаговая инструкция «от нуля до продакшена» — [`DEPLOY.md`](DEPLOY.md).
Полный новичок? Начните с [`docs/14-deploy-beginner.md`](docs/14-deploy-beginner.md):
там расписано, **куда что вводить и где нажимать**, вплоть до установки JDK и секретов GitHub.
Ниже — то же самое в сокращении.

```bash
# 1. (опционально) свои названия/идентификаторы
vim gradle.properties          # app.applicationId, app.versionCode, ads.* (ID блоков РСЯ)

# 2. ключ подписи (потеря ключа = невозможность обновлять приложение!)
keytool -genkeypair -v -keystore upload-keystore.jks -alias upload \
        -keyalg RSA -keysize 4096 -validity 10000
cat > keystore.properties <<'EOF'
storeFile=upload-keystore.jks
storePassword=ВАШ_ПАРОЛЬ
keyAlias=upload
keyPassword=ВАШ_ПАРОЛЬ
EOF

# 3. сборка
./gradlew testDebugUnitTest     # тесты логики игры и рекламных политик
./gradlew assembleDebug         # APK для проверки на телефоне
./gradlew bundleRelease         # app/build/outputs/bundle/release/app-release.aab → в Google Play
```

Без `keystore.properties` релиз соберётся с отладочной подписью — так удобно проверять рекламу,
но в Play такой AAB не примут: добавьте ключ перед загрузкой.

## Что заменить перед публикацией

| Где | Что |
| --- | --- |
| `gradle.properties` | `ads.testMode=false` и пять реальных ID блоков из РСЯ (`R-M-XXXXXXX-Y`) |
| `gradle.properties` | `app.applicationId` — меняется один раз и навсегда; `app.versionCode`/`app.versionName` |
| `Config.kt` | `PRIVACY_POLICY_URL`, `PLAY_MARKET_URL`, `SUPPORT_EMAIL` |
| `app-ads.txt` | шаблон файла подтверждения прав в РСЯ (строки вставляются из фрагмента, скачанного в РСЯ) |
| `store-listing/` | при желании — свои тексты и графика (готовые уже проходят требования Play) |

Полный чек-лист: `docs/12-checklist-release.md`. Пошаговая публикация: [`DEPLOY.md`](DEPLOY.md)
и `docs/10-google-play-release.md`.

## Рекламные точки

| Формат | Где | Ограничения в коде |
| --- | --- | --- |
| App Open | запуск и возврат из фона | не чаще 1 раза в 4 часа |
| Banner (sticky) | меню, магазин, статистика, настройки | не показывается во время партии, высота резервируется заранее |
| Native | магазин | ≤ 1 обновления в 30 сек, обязательные элементы, скрытие при ошибке биндинга |
| Rewarded | «Продолжить», «×2 монеты», бесплатные монеты | только по кнопке, награда — в `onRewarded` |
| Interstitial | старт новой партии после проигрыша | не чаще 60 сек и каждого 2-го проигрыша |

Все пороги — в `gradle.properties` (`ads.interstitial*`, `ads.appOpenIntervalHours`),
логика — в `ads/AdPolicy.kt` и покрыта тестами.

## Документация

Начните с [`docs/README.md`](docs/README.md) — там оглавление из 13 документов и «самое главное
в 10 строках». Ключевые файлы: `docs/01-formats.md` (все форматы РСЯ), `docs/07-rules.md`
(правила и что приводит к бану), `docs/09-monetization-strategy.md` (как получить максимум дохода).

## Лицензии и сторонние компоненты

- Yandex Mobile Ads SDK — по условиям Яндекса (см. `https://yandex.ru/legal/`).
- Звук синтезируется в коде, изображений и шрифтов из сторонних источников в проекте нет.
- Все ресурсы приложения созданы для этого проекта (иконка, скриншоты, тексты).
