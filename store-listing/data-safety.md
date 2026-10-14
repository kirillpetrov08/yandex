# Data safety: готовые ответы формы

Заполняйте ровно по этой таблице — тогда декларация в Play Console совпадёт с тем, что реально
делает приложение и что написано в политике конфиденциальности.

## Раздел «Data collection and security»

| Вопрос | Ответ | Почему |
| --- | --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** | SDK РСЯ использует рекламный идентификатор и технические данные |
| Is all of the user data collected by your app encrypted in transit? | **Yes** | запросы рекламы идут по HTTPS |
| Do you provide a way for users to request that their data is deleted? | **Yes** | почта `kirillpetrov08@gmail.com`, ответ в течение 30 дней |

## Типы данных

| Категория Play | Тип | Collect | Share | Purpose | Optional |
| --- | --- | --- | --- | --- | --- |
| Device or other IDs | **Device or other IDs** (рекламный ID) | Yes | Yes | Advertising or marketing; Analytics | No |
| App activity | **Other actions** (показы рекламы, клики по рекламе) | No | No | — | — |
| Location | Approximate location | **No** | No | мы не передаём геолокацию (`setLocationTracking(false)`) | — |
| Personal info | Name, email, address, phone | **No** | No | — | — |
| Financial info | — | **No** | — | в игре нет платежей | — |
| Health and fitness | — | **No** | — | — | — |
| Messages, Photos, Files, Contacts, Calendar | — | **No** | — | приложение не запрашивает эти разрешения | — |
| App info and performance | Crash logs | **No** | — | логи падений на устройстве; в Play Console Crashlytics не подключён | — |

Ключевое отличие «collect» и «share»: приложение **не сохраняет** данные у себя (collect = No для
игрового прогресса, он локальный), но рекламный идентификатор **передаётся рекламной сети** (share = Yes).
Это стандартная схема для приложений с рекламой РСЯ.

## Раздел «Ads»

| Вопрос | Ответ |
| --- | --- |
| Does your app contain ads? | **Yes** |
| Ads are present in this app | **Yes** — баннер, межстраничная, с вознаграждением, App Open, нативная |

## Advertising ID

| Вопрос | Ответ |
| --- | --- |
| Does your app use advertising ID? | **Yes** |
| Purpose | Advertising or marketing |

> Если когда-нибудь сделаете версию для детской аудитории (Families Policy) — в этой сборке
> нужно убрать разрешение `com.google.android.gms.permission.AD_ID` из манифеста
> (`tools:node="remove"`) и выставить в форме «No». У нас целевая аудитория 13+.

## Что обязательно должно совпадать

1. Ответы формы ⇔ политика конфиденциальности (`privacy-policy.html`) ⇔ реальное поведение приложения.
2. Если добавите аналитику (AppMetrica/Firebase), анкету нужно обновить: появятся
   «App activity → In-app actions» и, возможно, «Crash logs».
3. Если добавите Play Billing («Убрать рекламу»), появится категория «Purchase history».

Расхождение формы и реальности — типовая причина отклонения обновлений в Play.
