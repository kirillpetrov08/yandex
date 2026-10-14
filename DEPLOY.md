# Деплой: пошагово от нуля до продакшена в Google Play

Это короткая рабочая инструкция «в день деплоя». Подробности по каждому пункту —
в `docs/10-google-play-release.md`, полный чек-лист — в `docs/12-checklist-release.md`.

**Сколько времени:** сборка и публикация — 1–2 часа. Но если у вас **новый личный** аккаунт
разработчика, Google требует перед продакшеном провести закрытое тестирование с 12 тестировщиками
**14 дней подряд** — это планируйте заранее (см. шаг 7).

**Совсем новичкам:** есть отдельный сверхподробный гайд `docs/14-deploy-beginner.md` — там
по шагам «в каком окне что нажать и куда вставить», включая установку JDK, секреты GitHub
и формы Play Console. Этот файл — короткая версия для тех, кто уже понимает термины.

**Что понадобится:**

| Что | Где взять | Стоимость |
| --- | --- | --- |
| Аккаунт Google Play Developer | play.google.com/console | $25 один раз |
| Аккаунт РСЯ (партнёрский) | partner.yandex.ru | бесплатно |
| Ключ подписи (upload key) | создаётся командой в шаге 2 | бесплатно |
| Сайт разработчика | GitHub Pages этого же репозитория | бесплатно |
| JDK 17 + Android Studio | либо не нужно вообще, если собирать в GitHub Actions | бесплатно |

Два пути сборки — выбирайте один:

- **Путь A (проще, если нет Android Studio):** сборка в GitHub Actions, вы только скачиваете готовый AAB.
- **Путь B (классика):** сборка на своём компьютере через `./gradlew bundleRelease`.

---

## Шаг 1. Заполнить свои данные

```bash
# 1.1 gradle.properties — ID приложения и версия
app.applicationId=com.blockpuzzle.app   # ВНИМАНИЕ: после публикации менять нельзя
app.versionCode=1
app.versionName=1.0.0

# 1.2 Config.kt — почта и ссылки
#   PRIVACY_POLICY_URL, PLAY_MARKET_URL, SUPPORT_EMAIL
```

Идентификаторы рекламных блоков вписываются в шаге 5 (до финальной сборки) — иначе получите
две сборки подряд.

## Шаг 2. Ключ подписи

```bash
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias upload \
  -keyalg RSA -keysize 4096 -validity 10000
```

Создайте `keystore.properties` в корне проекта (файл уже в `.gitignore`):

```properties
storeFile=upload-keystore.jks
storePassword=ПАРОЛЬ
keyAlias=upload
keyPassword=ПАРОЛЬ
```

> **Самое важное за весь деплой:** положите `upload-keystore.jks` и пароли в **два независимых
> места** (менеджер паролей + зашифрованная копия в облаке). Потеря ключа без Play App Signing
> означает, что вы больше не сможете выпускать обновления.

Для пути A закодируйте ключ в base64 и добавьте в секреты GitHub
(Settings → Secrets and variables → Actions):

```bash
base64 -w0 upload-keystore.jks      # скопируйте вывод целиком (macOS: base64 -i upload-keystore.jks)
```

| Секрет | Значение |
| --- | --- |
| `KEYSTORE_BASE64` | вывод команды выше |
| `KEYSTORE_PASSWORD` | пароль хранилища |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | пароль ключа |
| `PLAY_SERVICE_ACCOUNT_JSON` | необязательно: JSON сервисного аккаунта Play для автозагрузки (шаг 6Б) |

## Шаг 3. Сборка AAB

### Путь A — GitHub Actions (ничего устанавливать не нужно)

1. Скопируйте `docs/ci/android.yml` в `.github/workflows/android.yml`:
   ```bash
   mkdir -p .github/workflows && cp docs/ci/android.yml .github/workflows/android.yml
   git add .github/workflows/android.yml && git commit -m "ci: сборка и публикация" && git push
   ```
   (В рабочей копии этого проекта файл уже лежит в `.github/workflows/android.yml` — просто
   закоммитьте его: доступ из песочницы не позволяет пушить workflows.)
2. Откройте на GitHub вкладку **Actions → Android CI → Run workflow**.
3. Дождитесь зелёной сборки (5–10 минут) и скачайте артефакт **build-outputs**:
   внутри `bundle/release/app-release.aab` (это и грузится в Play) и `apk/debug/*.apk`
   (это для проверки на телефоне).
4. Проверьте, что в логе шага «Проверка подписи AAB» нет предупреждений: если секреты ключа
   не заданы, AAB подписан debug-ключом и **Play его не примет**.

### Путь B — на своём компьютере

```bash
./gradlew clean testDebugUnitTest     # тесты логики игры и рекламных политик
./gradlew bundleRelease               # AAB  -> app/build/outputs/bundle/release/app-release.aab
./gradlew assembleRelease             # APK  -> app/build/outputs/apk/release/app-release.apk
```

Перед загрузкой в Play обязательно проверьте APK на **реальном телефоне**
(`adb install -r app/build/outputs/apk/release/app-release.apk`): запуск, партия, проигрыш,
кнопки «Продолжить»/«Удвоить монеты», баннер в меню, отсутствие рекламы во время партии.

## Шаг 4. Сайт: политика конфиденциальности и app-ads.txt

Файлы `privacy-policy.html` и `app-ads.txt` лежат в корне этого репозитория. Публикуем их как
**сайт пользователя GitHub Pages** — это даёт короткие адреса в корне домена, где РСЯ ищет
`app-ads.txt`:

1. Создайте **публичный** репозиторий с именем `<логин>.github.io` (у нас — `kirillpetrov08.github.io`):
   github.com/new → Repository name → `kirillpetrov08.github.io` → Public → Create repository.
   Причина: Pages для обычного репозитория на бесплатном плане работает только в публичных
   репозиториях, а сайт пользователя даёт корень домена.
2. **Add file → Upload files** → перетащите `privacy-policy.html` и `app-ads.txt` → Commit changes.
3. Через минуту проверьте в браузере: `https://kirillpetrov08.github.io/privacy-policy.html`
   и `https://kirillpetrov08.github.io/app-ads.txt`.
4. В РСЯ (**Настройки → Общие → app-ads.txt**) возьмите фрагмент своего аккаунта и впишите ID
   партнёра в `app-ads.txt` вместо `YOUR_PARTNER_ID`, затем обновите файл в репозитории сайта.
5. Укажите сайт разработчика в Play Console (Store settings → Developer website) — это адрес,
   по которому РСЯ будет искать файл.

> Почему не сайт проекта: у него адрес `<логин>.github.io/<репозиторий>/` — это подпапка, а РСЯ
> (как и спецификация IAB) ищет `app-ads.txt` в корне домена. Варианты: сайт пользователя (выше),
> свой домен или сайт проекта с обязательной проверкой статуса файла в РСЯ.

## Шаг 5. РСЯ: приложение и рекламные блоки

1. Зарегистрируйтесь/войдите в `partner.yandex.ru`, примите оферту, заполните платёжные данные.
2. **Реклама в приложениях → Приложения → Добавить**: укажите название и **тот же package name**
   (`com.blockpuzzle.app`), что в `gradle.properties`.
3. Создайте 5 блоков и скопируйте их ID:

| Формат | Куда в игре | Ключ в `gradle.properties` |
| --- | --- | --- |
| Баннер (адаптивный, sticky) | меню, магазин, статистика, настройки | `ads.bannerUnitId` |
| Межстраничная реклама | после партии | `ads.interstitialUnitId` |
| Реклама с вознаграждением | «Продолжить», «×2 монеты», бесплатные монеты | `ads.rewardedUnitId` |
| Реклама при открытии приложения | запуск и возврат из фона | `ads.appOpenUnitId` |
| Нативная реклама | магазин | `ads.nativeUnitId` |

4. Впишите ID и **выключите тестовый режим**:

```properties
ads.testMode=false
ads.bannerUnitId=R-M-XXXXXXX-1
ads.interstitialUnitId=R-M-XXXXXXX-2
ads.rewardedUnitId=R-M-XXXXXXX-3
ads.appOpenUnitId=R-M-XXXXXXX-4
ads.nativeUnitId=R-M-XXXXXXX-5
```

5. Отправьте приложение в РСЯ на модерацию. До её прохождения (обычно до 2 суток) показов
   с доходом не будет.

## Шаг 6. Google Play Console

Создайте приложение (**Create app**: «Игра», «Бесплатное»), затем заполните всё в разделе
**App content** — без этого кнопка публикации не активируется:

| Раздел | Что указать | Готовые ответы |
| --- | --- | --- |
| Privacy policy | ссылка на `privacy-policy.html` | шаг 4 |
| Ads | «Да, приложение содержит рекламу» | `store-listing/app-content.md` |
| Content rating | анкета IARC, все пункты «Нет» кроме Contains ads | `store-listing/app-content.md` |
| Target audience | только **13+** (иначе Families Policy и придётся убирать рекламный ID) | `store-listing/app-content.md` |
| Data safety | рекламный ID — «собирается, передаётся» | `store-listing/data-safety.md` |
| App access | всё доступно без ограничений | — |

**Store listing** — тексты берите из `store-listing/ru.md` и `store-listing/en.md`, графику из той
же папки: `icon-512.png`, `feature-graphic-1024x500.png`, `screenshots/phone-{ru,en}-*.png`.
Английская локализация обязательна только если вы сами её добавляете, но она расширяет аудиторию.

**Store settings:** категория Games → Puzzle, контактная почта, сайт разработчика (тот же домен,
где лежит `app-ads.txt`).

### Загрузка AAB

- **Вручную:** Release → Testing → **Internal testing** → Create new release → загрузите
  `app-release.aab` → добавьте себя и пару знакомых → Save → **Publish**. Через несколько минут
  появится ссылка для тестировщиков — поставьте сборку и проверьте рекламу на живых блоках.
- **Автоматически (необязательно):** создайте сервисный аккаунт в Google Cloud, дайте ему доступ
  в Play Console (Users and permissions → Invite new users → Release to testing tracks), сохраните
  JSON-ключ в секрет `PLAY_SERVICE_ACCOUNT_JSON`. Тогда workflow сам загрузит AAB в трек `internal`
  черновиком — останется нажать «Публикация» в Play Console.

Дальше переходите к **Production → Create new release** и раскатывайте на 100% (или 10–20%,
если трафик уже есть).

## Шаг 7. Обязательное закрытое тестирование (только для новых личных аккаунтов)

Google требует: **не менее 12 тестировщиков** в закрытом тесте, которые установили приложение,
и **14 дней непрерывного** тестирования — только после этого открывается продакшен.
Соберите список заранее (друзья, знакомые, сообщества разработчиков). Аудитория в 12 человек
должна быть реальной: «накрутка» тестировщиками ведёт к отказу в доступе к продакшену.

Пока идёт тест, полезно: собрать отзывы, поправить тексты, посмотреть Crash-free rate в Play Console
и статистику запросов в РСЯ.

## Шаг 8. Первые сутки после продакшена

- **Play Console → Android vitals:** Crash-free > 99.5%, ANR < 0.5%.
- **РСЯ → Статистика:** появились запросы и показы по каждому блоку; нет сплошных `NO FILL`.
- **App content → Ads/Monetization:** убедиться, что статус приложения в РСЯ стал **Active**.
- Ответить на первые отзывы, проверить, что реклама показывается и **не перекрывает** игровое поле.
- Если нужно обновление: поднимите `app.versionCode`, соберите новый AAB, загрузите — Play
  не примет сборку с тем же номером версии.

## Что дальше (по желанию, когда появится трафик)

- Подключить AppMetrica и смотреть воронку «проигрыш → rewarded → новая партия» (`docs/09`).
- Подключить медиацию Яндекса (In-App Bidding) — самый быстрый способ поднять eCPM (`docs/09`).
- Добавить «Убрать рекламу» через Play Billing (не убирая rewarded).
- Раз в год обновлять `targetSdk` (с 31.08.2026 требуется API 36) и версию SDK Яндекса.

## Если что-то не работает

| Симптом | Куда смотреть |
| --- | --- |
| Реклама не грузится | `docs/13-troubleshooting.md`, логи: `adb logcat -v brief '*:S YandexAds'` |
| Play отклоняет сборку | причина в письме + `docs/07-rules.md` (раздел Google Play) |
| РСЯ не подтверждает приложение | `app-ads.txt` в корне домена + сайт разработчика в Play (`docs/07-rules.md`, раздел 5) |
| РСЯ приостановила показы | платёжные данные и оферта, порог 10 000 ₽ (`docs/08-account-and-payouts.md`) |
| Ошибка подписи AAB | вы собираете без `keystore.properties` / без секретов в Actions (шаг 2) |
