# Шпаргалка по Yandex Mobile Ads SDK 8.6 (Android)

Классы, методы и типичные ошибки — по официальному примеру и документации.

## Подключение

```kotlin
// app/build.gradle.kts
implementation("com.yandex.android:mobileads:8.6.0")
// Все сети медиации одной сборкой:
// implementation("com.yandex.android:mobileads-mediation:8.6.0.0")
```

Требования: Android Studio 2021+, AGP 8.7.0+, `minSdk 21+`, `compileSdk 35+`,
AppMetrica (если используется) 8.5.0+. Репозитории: `google()` и `mavenCentral()`.

## Инициализация

```kotlin
YandexAds.setUserConsent(true)      // до initialize: согласие на персонализацию
YandexAds.setLocationTracking(false)
YandexAds.setAgeRestricted(false)   // COPPA
YandexAds.initialize(application) { /* можно запрашивать рекламу */ }
YandexAds.enableLogging(true)       // только для отладки
YandexAds.showDebugPanel(context)   // панель отладки SDK (в дебаг-сборке)
```

- Автоинициализация: включена по умолчанию; отключается метаданными
  `com.yandex.mobile.ads.AUTOMATIC_SDK_INITIALIZATION=false`.
  Даже при отключенной автоинициализации SDK инициализируется перед первым запросом рекламы.
- Проверка интеграции в логах: `adb logcat -v brief '*:S YandexAds'`
  → `Yandex Mobile Ads 8.6.0 integrated successfully` / `initialized successfully`.
- Релизная сборка падает на lint-проверке `YandexAdsSdkOutdatedVersion`, если версия SDK устарела.

## Баннер

```kotlin
val size = BannerAdSize.sticky(context, widthDp)              // ≤ 15% высоты экрана
val size2 = BannerAdSize.inline(context, widthDp, maxHeightDp) // высота до высоты экрана
val view = BannerAdView(context).apply {
    setAdSize(size)
    setBannerAdEventListener(object : BannerAdEventListener {
        override fun onAdLoaded() {}
        override fun onAdFailedToLoad(error: AdRequestError) {}
        override fun onAdClicked() {}
        override fun onImpression(data: ImpressionData?) {}
    })
}
view.loadAd(AdRequest.Builder(AD_UNIT_ID).build())
val heightPx = size.getHeightInPixels(context) // зарезервировать место заранее
view.destroy()                                  // при уходе с экрана — обязательно
```

## Межстраничная

```kotlin
val loader = InterstitialAdLoader(context)      // один инстанс
loader.loadAd(AdRequest.Builder(AD_UNIT_ID).build(), object : InterstitialAdLoadListener {
    override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
    override fun onAdFailedToLoad(error: AdRequestError) {}
})
// показ
interstitial?.setAdEventListener(object : InterstitialAdEventListener {
    override fun onAdShown() {}
    override fun onAdFailedToShow(adError: AdError) {}
    override fun onAdDismissed() {}
    override fun onAdClicked() {}
    override fun onAdImpression(data: ImpressionData?) {}
})
interstitial?.show(activity)
loader.cancelLoading()                          // при уничтожении экрана
```

## Реклама с вознаграждением

```kotlin
RewardedAdLoader(context).loadAd(AdRequest.Builder(AD_UNIT_ID).build(),
    object : RewardedAdLoadListener {
        override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        override fun onAdFailedToLoad(error: AdRequestError) {}
    })

rewardedAd?.setAdEventListener(object : RewardedAdEventListener {
    override fun onAdShown() {}
    override fun onAdFailedToShow(adError: AdError) {}
    override fun onAdDismissed() {}
    override fun onRewarded(reward: Reward) { /* reward.amount, reward.type — выдать награду */ }
    override fun onAdClicked() {}
    override fun onAdImpression(data: ImpressionData?) {}
})
rewardedAd?.show(activity)
```

## App Open

```kotlin
val loader = AppOpenAdLoader(application)
loader.loadAd(AdRequest.Builder(AD_UNIT_ID).build(), object : AppOpenAdLoadListener {
    override fun onAdLoaded(appOpenAd: AppOpenAd) { appOpen = appOpenAd }
    override fun onAdFailedToLoad(error: AdRequestError) {}
})
appOpen?.setAdEventListener(object : AppOpenAdEventListener {
    override fun onAdShown() {}
    override fun onAdFailedToShow(adError: AdError) {}
    override fun onAdDismissed() {}
    override fun onAdClicked() {}
    override fun onAdImpression(data: ImpressionData?) {}
})
appOpen?.show(activity)
// Показ — по ProcessLifecycleOwner (onStart) и не чаще заданного интервала.
```

## Нативная реклама

```kotlin
val options = NativeAdOptions.Builder().setShouldLoadImagesAutomatically(true).build()
NativeAdLoader(context).loadAd(
    AdRequest.Builder(AD_UNIT_ID).build(), options,
    object : NativeAdLoadListener {
        override fun onAdLoaded(nativeAd: NativeAd) { bind(nativeAd) }
        override fun onAdFailedToLoad(error: AdRequestError) {}
    },
)

private fun bind(nativeAd: NativeAd) {
    val binder = NativeAdViewBinder.Builder(nativeAdView) // com.yandex.mobile.ads.nativeads.NativeAdView
        .setAgeView(ageView).setSponsoredView(sponsoredView).setFeedbackView(feedbackView)
        .setTitleView(titleView).setBodyView(bodyView).setDomainView(domainView)
        .setIconView(iconView).setMediaView(mediaView)    // com.yandex.mobile.ads.nativeads.MediaView
        .setCallToActionView(ctaView).setRatingView(ratingView).setReviewCountView(reviewView)
        .setPriceView(priceView).setWarningView(warningView).setFaviconView(faviconView)
        .build()
    when (nativeAd.bindNativeAd(binder)) {
        is AdBindingResult.Success -> nativeAdView.isVisible = true
        is AdBindingResult.Failure -> nativeAdView.isVisible = false
    }
}
```

Если загружаете несколько объявлений сразу — есть `NativeBulkAdLoader.loadAds(request, count, listener)`.

## AdRequest

```kotlin
AdRequest.Builder(adUnitId)
    .setParameters(mapOf("adf_ownerid" to "…")) // Adfox
    .setBiddingData(bidResponse)                // Open Bidding
    .build()
```

## Демо-блоки (тестовые, без дохода)

`demo-banner-yandex`, `demo-interstitial-yandex`, `demo-rewarded-yandex`,
`demo-appopenad-yandex`, `demo-native-content-yandex`.
Есть также демо-блоки сторонних сетей для медиации: `demo-banner-admob`, `demo-interstitial-mytarget` и т.д.

## Типичные ошибки новичков

| Ошибка | Последствие | Правильно |
| --- | --- | --- |
| Запрос рекламы до `initialize` | нет показа | грузить после колбэка инициализации |
| Пересоздание загрузчиков на каждый запрос | падение производительности, меньше заполнения | один инстанс на всё приложение |
| Повторная загрузка из `onAdFailedToLoad` в цикле | «залипание» запросов, снижение дохода | backoff (у нас 30 сек) |
| Ссылки на объявление в локальных переменных | GC убирает объект, показа нет | strong reference до конца экрана |
| Забыли `destroy()`/`setAdEventListener(null)` | утечка памяти | вызывать при уходе с экрана |
| Вызовы SDK не из главного потока | падения | только main thread |
| Награда выдана в `onAdDismissed` | награда без просмотра | только в `onRewarded` |
| Свой таймер автообновления sticky-баннера | двойные запросы | SDK обновляет сам |
