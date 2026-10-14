# Реклама при открытии приложения (App Open Ad)

## Что это

Специальный формат для монетизации **экрана загрузки** приложения. Объявление можно закрыть в любой
момент, и в нём показывается кнопка «Перейти в приложение». Предназначен для трёх моментов:

- запуск приложения (холодный старт);
- выход приложения на передний план (foreground);
- возврат в приложение из фона.

В терминах SDK: **холодный старт** — создание новой сессии приложения, **горячий старт** —
возврат из фона, когда процесс уже жив.

## Как это правильно подключать

Рекомендованный Яндексом подход — наблюдать за жизненным циклом **процесса** и показывать
объявление, когда приложение вышло на передний план:

```kotlin
// 1. Слушаем жизненный цикл процесса, а не конкретной Activity
ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) { maybeShow() }
})

// 2. Запоминаем текущую Activity через ActivityLifecycleCallbacks
application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
    override fun onActivityStarted(activity: Activity) { activityReference = WeakReference(activity) }
    // остальные методы пустые
})

// 3. Показываем, если объявление загружено, Activity жива и пользователь не в игре
private fun maybeShow() {
    val activity = activityReference?.get() ?: return
    if (activity.isFinishing || activity.isDestroyed) return
    if (!policy.canShowAppOpen()) return
    ad?.let { it.setAdEventListener(listener); it.show(activity) }
}
```

Плюс защита от «долбёжки»:

- `AtomicBoolean` для флага загрузки (второй `loadAd` не запускается, пока идёт первый);
- `suppressFor(ms)` — временная блокировка показов, пока идёт партия или другое полноэкранное объявление;
- интервал между показами (у нас 4 часа, `ads.appOpenIntervalHours`).

## Требования и нюансы

1. **Не показывайте App Open на каждом переключении экранов** — только на старте и возврате из фона.
2. **Не считайте App Open заменой interstitial**: у него отдельный сценарий и отдельный блок в РСЯ.
3. **Не показывайте его в первые секунды после другого полноэкранного формата.**
4. Пользователь может закрыть объявление мгновенно — UI должен быть к этому готов
   (не запускайте анимации и таймеры, которые предполагают, что пользователь «вернётся позже»).
5. Инициализируйте SDK как можно раньше (в `Application.onCreate`), иначе к моменту показа
   объявление не успеет загрузиться и показ не состоится.

## Реализация в проекте

`app/src/main/java/com/blockpuzzle/app/ads/AppOpenAdController.kt`:

- регистрирует `ProcessLifecycleOwner` + `ActivityLifecycleCallbacks`;
- предзагружает объявление сразу после инициализации SDK;
- показ — только если `AdPolicy.canShowAppOpen()` (интервал 4 часа);
- `suppressFor(70_000)` вызывается перед показом interstitial и `suppressFor(130_000)` перед rewarded,
  чтобы форматы не «наезжали» друг на друга;
- после закрытия объявления сразу грузится следующее.

## Проверка

```
adb logcat -v brief '*:S YandexAds'
# ожидайте: Ad type appopenad was integrated successfully
```

Демо-блок для отладки: `demo-appopenad-yandex`.
