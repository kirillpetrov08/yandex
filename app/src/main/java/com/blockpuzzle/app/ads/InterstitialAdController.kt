package com.blockpuzzle.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Межстраничная реклама (Interstitial).
 *
 * Особенности, обязательные по документации РСЯ:
 *  - все вызовы SDK — из главного потока;
 *  - один инстанс [InterstitialAdLoader] на все загрузки (производительность);
 *  - держим сильные ссылки на загрузчик и объявление;
 *  - НЕ перезагружаем объявление из onAdFailedToLoad бесконечно — используем backoff;
 *  - после показа/ошибки вызываем setAdEventListener(null), чтобы не текла память;
 *  - показываем только в естественных паузах (конец партии), не посреди геймплея.
 */
class InterstitialAdController(
    context: Context,
    private val adUnitId: String = AdUnits.interstitial,
    private val retryDelayMs: Long = 30_000L,
) {
    private val appContext: Context = context.applicationContext
    private val loader: InterstitialAdLoader = InterstitialAdLoader(appContext)
    private val handler = Handler(Looper.getMainLooper())

    private var ad: InterstitialAd? = null
    private var loading = false
    private var nextRetryAt = 0L
    private var shownCounter = 0

    private val _state = MutableStateFlow(AdLoadState.IDLE)
    val state: StateFlow<AdLoadState> = _state

    fun preload() {
        if (loading || ad != null) return
        if (nextRetryAt > 0L && System.currentTimeMillis() < nextRetryAt) {
            scheduleRetry(nextRetryAt - System.currentTimeMillis())
            return
        }
        loading = true
        _state.value = AdLoadState.LOADING
        AdsLog.d("Interstitial: loadAd")
        loader.loadAd(
            AdRequest.Builder(adUnitId).build(),
            object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loading = false
                    nextRetryAt = 0L
                    ad = interstitialAd
                    _state.value = AdLoadState.READY
                    AdsLog.d("Interstitial: onAdLoaded")
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    loading = false
                    ad = null
                    _state.value = AdLoadState.FAILED
                    // Повторные попытки строго ограничены, чтобы не «долбить» сеть.
                    nextRetryAt = System.currentTimeMillis() + retryDelayMs
                    AdsLog.w("Interstitial: onAdFailedToLoad ${adRequestError.code} ${adRequestError.description}")
                    scheduleRetry(retryDelayMs)
                }
            },
        )
    }

    /**
     * Показ объявления. [onFinished] вызывается всегда:
     * shown = true — реклама реально была показана и закрыта пользователем.
     */
    fun show(activity: Activity, onFinished: (shown: Boolean) -> Unit) {
        val current = ad
        if (current == null) {
            preload()
            onFinished(false)
            return
        }
        activity.runOnUiThread {
            current.setAdEventListener(object : InterstitialAdEventListener {
                override fun onAdShown() {
                    AdsLog.d("Interstitial: onAdShown")
                }

                override fun onAdFailedToShow(adError: AdError) {
                    AdsLog.w("Interstitial: onAdFailedToShow ${adError.description}")
                    clearAd()
                    preload()
                    onFinished(false)
                }

                override fun onAdDismissed() {
                    AdsLog.d("Interstitial: onAdDismissed")
                    shownCounter++
                    clearAd()
                    preload()
                    onFinished(true)
                }

                override fun onAdClicked() = AdsLog.d("Interstitial: onAdClicked")

                override fun onAdImpression(impressionData: ImpressionData?) {
                    AdsLog.d("Interstitial: onAdImpression")
                }
            })
            try {
                current.show(activity)
            } catch (t: Throwable) {
                AdsLog.w("Interstitial: show failed", t)
                clearAd()
                onFinished(false)
            }
        }
    }

    val isReady: Boolean get() = ad != null

    val shownCount: Int get() = shownCounter

    private fun clearAd() {
        // Обязательно: снимаем слушатель, чтобы освободить ресурсы и избежать утечек.
        ad?.setAdEventListener(null)
        ad = null
    }

    private fun scheduleRetry(delayMs: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ preload() }, delayMs.coerceAtLeast(1_000L))
    }

    fun destroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { loader.cancelLoading() }
        clearAd()
        _state.value = AdLoadState.IDLE
    }
}
