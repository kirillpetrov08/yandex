package com.blockpuzzle.app.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Реклама с вознаграждением (Rewarded video).
 *
 * Самый доходный формат: пользователь сам инициирует показ ради награды.
 * Награда выдаётся ТОЛЬКО в onRewarded() — это гарантия от РСЯ, что просмотр засчитан.
 *
 * Правила:
 *  - один [RewardedAdLoader] на приложение;
 *  - не перезагружать бесконечно при ошибках;
 *  - награду выдавать в onRewarded, а не в onAdDismissed;
 *  - при ошибке показа всегда сообщать UI, чтобы кнопка вернулась в рабочее состояние.
 */
class RewardedAdController(
    context: Context,
    private val adUnitId: String = AdUnits.rewarded,
    private val retryDelayMs: Long = 30_000L,
) {
    private val appContext: Context = context.applicationContext
    private val loader: RewardedAdLoader = RewardedAdLoader(appContext)
    private val handler = Handler(Looper.getMainLooper())

    private var ad: RewardedAd? = null
    private var loading = false
    private var nextRetryAt = 0L

    private val _state = MutableStateFlow(AdLoadState.IDLE)

    /** Состояние готовности: READY — кнопку «Смотреть рекламу» можно показывать. */
    val state: StateFlow<AdLoadState> = _state

    fun preload() {
        if (loading || ad != null) return
        if (nextRetryAt > 0L && System.currentTimeMillis() < nextRetryAt) {
            scheduleRetry(nextRetryAt - System.currentTimeMillis())
            return
        }
        loading = true
        _state.value = AdLoadState.LOADING
        loader.loadAd(
            AdRequest.Builder(adUnitId).build(),
            object : RewardedAdLoadListener {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    loading = false
                    nextRetryAt = 0L
                    ad = rewardedAd
                    _state.value = AdLoadState.READY
                    AdsLog.d("Rewarded: onAdLoaded")
                }

                override fun onAdFailedToLoad(adRequestError: AdRequestError) {
                    loading = false
                    ad = null
                    _state.value = AdLoadState.FAILED
                    nextRetryAt = System.currentTimeMillis() + retryDelayMs
                    AdsLog.w("Rewarded: onAdFailedToLoad ${adRequestError.code} ${adRequestError.description}")
                    scheduleRetry(retryDelayMs)
                }
            },
        )
    }

    /**
     * Показ. [onReward] — награда подтверждена РСЯ; [onFinished] — показ закончился
     * (rewarded = true/false, чтобы UI знал, выдавать ли бонус).
     */
    fun show(
        activity: Activity,
        onReward: () -> Unit,
        onFinished: (rewarded: Boolean) -> Unit,
    ) {
        val current = ad
        if (current == null) {
            preload()
            onFinished(false)
            return
        }
        activity.runOnUiThread {
            var granted = false
            current.setAdEventListener(object : RewardedAdEventListener {
                override fun onAdShown() = AdsLog.d("Rewarded: onAdShown")

                override fun onAdFailedToShow(adError: AdError) {
                    AdsLog.w("Rewarded: onAdFailedToShow ${adError.description}")
                    clearAd()
                    preload()
                    onFinished(false)
                }

                override fun onAdDismissed() {
                    AdsLog.d("Rewarded: onAdDismissed granted=$granted")
                    clearAd()
                    preload()
                    onFinished(granted)
                }

                override fun onRewarded(reward: Reward) {
                    granted = true
                    AdsLog.d("Rewarded: onRewarded ${reward.amount} ${reward.type}")
                    onReward()
                }

                override fun onAdClicked() = AdsLog.d("Rewarded: onAdClicked")

                override fun onAdImpression(impressionData: ImpressionData?) = AdsLog.d("Rewarded: onAdImpression")
            })
            try {
                current.show(activity)
            } catch (t: Throwable) {
                AdsLog.w("Rewarded: show failed", t)
                clearAd()
                onFinished(false)
            }
        }
    }

    val isReady: Boolean get() = ad != null

    private fun clearAd() {
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
