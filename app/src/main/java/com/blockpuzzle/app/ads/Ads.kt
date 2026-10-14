package com.blockpuzzle.app.ads

import android.app.Activity
import android.app.Application
import com.yandex.mobile.ads.common.YandexAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Единая точка входа в рекламу.
 *
 * Порядок инициализации важен: сначала применяем настройки приватности
 * (YandexAds.setUserConsent и т.п.), только потом YandexAds.initialize().
 */
object Ads {

    private val _initialized = MutableStateFlow(false)

    /** true — SDK проинициализирован, можно создавать загрузчики и запрашивать рекламу. */
    val initialized: StateFlow<Boolean> = _initialized

    lateinit var policy: AdPolicy
        private set
    lateinit var interstitial: InterstitialAdController
        private set
    lateinit var rewarded: RewardedAdController
        private set
    lateinit var appOpen: AppOpenAdController
        private set

    fun init(application: Application, consent: ConsentManager) {
        if (_initialized.value) return
        policy = AdPolicy()
        adsApplication = application

        // 1. Приватность — строго до инициализации SDK.
        consent.applyToSdk()

        // 2. Инициализация SDK. До этого момента запрашивать рекламу нельзя.
        YandexAds.initialize(application) {
            AdsLog.i("Yandex Mobile Ads SDK initialized")
            interstitial = InterstitialAdController(application)
            rewarded = RewardedAdController(application)
            appOpen = AppOpenAdController(application, policy)
            _initialized.value = true
            // Предзагрузка сразу после старта ускоряет первый показ и повышает доход.
            rewarded.preload()
            interstitial.preload()
            appOpen.preload()
        }
        if (AdUnits.testMode) {
            // Логи SDK: adb logcat -v brief '*:S YandexAds'
            YandexAds.enableLogging(true)
        }
    }

    private var adsApplication: Application? = null

    /** Состояние готовности rewarded-рекламы. Безопасно до инициализации SDK. */
    val rewardedState: StateFlow<AdLoadState>
        get() = if (::rewarded.isInitialized) rewarded.state else idleState

    /** Состояние межстраничной рекламы. Безопасно до инициализации SDK. */
    val interstitialState: StateFlow<AdLoadState>
        get() = if (::interstitial.isInitialized) interstitial.state else idleState

    private val idleState = MutableStateFlow(AdLoadState.IDLE)

    /** Показ rewarded-рекламы. Возвращает false, если объявление ещё не готово. */
    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onFinished: (Boolean) -> Unit = {},
    ): Boolean {
        if (!_initialized.value || !::rewarded.isInitialized || !rewarded.isReady) {
            if (::rewarded.isInitialized) rewarded.preload()
            return false
        }
        if (::appOpen.isInitialized) appOpen.suppressFor(130_000L)
        rewarded.show(activity, onReward = {
            policy.markRewardedShown()
            onReward()
        }, onFinished = onFinished)
        return true
    }

    /** Показ межстраничной рекламы в естественной паузе (после партии). */
    fun showInterstitial(activity: Activity, onFinished: (Boolean) -> Unit = {}) {
        if (!_initialized.value) {
            onFinished(false)
            return
        }
        // На время показа полноэкранной рекламы приостанавливаем App Open.
        if (::appOpen.isInitialized) appOpen.suppressFor(70_000L)
        interstitial.show(activity) { shown ->
            if (shown) policy.markInterstitialShown()
            onFinished(shown)
        }
    }

    fun canShowInterstitial(): Boolean = _initialized.value && policy.canShowInterstitial()

    fun onGameFinished() {
        if (_initialized.value) policy.onGameFinished()
    }
}
