package com.blockpuzzle.app.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Реклама при открытии приложения (App Open Ad).
 *
 * Термины РСЯ:
 *  - холодный старт — приложение запускается заново;
 *  - горячий старт — приложение возвращается из фона.
 *
 * Формат предназначен ровно для этих двух моментов, поэтому:
 *  - показываем через ProcessLifecycleOwner (onStart процесса = приложение вышло на передний план);
 *  - не чаще, чем позволяет [AdPolicy] (по умолчанию 4 часа);
 *  - не показываем во время партии: [suppressFor] временно блокирует показ.
 */
class AppOpenAdController(
    private val application: Application,
    private val policy: AdPolicy,
    private val adUnitId: String = AdUnits.appOpen,
) : AppOpenAdLoadListener {

    private val loader = AppOpenAdLoader(application)
    private val adRequest = AdRequest.Builder(adUnitId).build()

    private val loadingInProgress = AtomicBoolean(false)
    private val showingInProgress = AtomicBoolean(false)
    private var activityReference: WeakReference<Activity>? = null
    private var ad: AppOpenAd? = null

    private var suppressUntilMs = 0L

    private val listener = object : AppOpenAdEventListener {
        override fun onAdShown() {
            AdsLog.d("AppOpen: onAdShown")
            showingInProgress.set(false)
        }

        override fun onAdFailedToShow(adError: AdError) {
            AdsLog.w("AppOpen: onAdFailedToShow ${adError.description}")
            showingInProgress.set(false)
            clearAd()
            preload()
        }

        override fun onAdDismissed() {
            AdsLog.d("AppOpen: onAdDismissed")
            clearAd()
            preload()
        }

        override fun onAdClicked() = AdsLog.d("AppOpen: onAdClicked")

        override fun onAdImpression(impressionData: ImpressionData?) = AdsLog.d("AppOpen: onAdImpression")
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                maybeShow()
            }
        })
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                activityReference = WeakReference(activity)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) {
                if (activityReference?.get() === activity) activityReference = null
            }
        })
    }

    /** Предзагрузка. Вызывается один раз после инициализации SDK. */
    fun preload() {
        if (loadingInProgress.compareAndSet(false, true)) {
            loader.loadAd(adRequest, this)
        }
    }

    override fun onAdLoaded(appOpenAd: AppOpenAd) {
        AdsLog.d("AppOpen: onAdLoaded")
        ad = appOpenAd
        loadingInProgress.set(false)
    }

    override fun onAdFailedToLoad(adRequestError: AdRequestError) {
        // Без бесконечных повторов: следующая попытка — при следующем показе/старте.
        AdsLog.w("AppOpen: onAdFailedToLoad ${adRequestError.code} ${adRequestError.description}")
        ad = null
        loadingInProgress.set(false)
    }

    /** Блокировка показов, пока идёт партия или другое полноэкранное объявление. */
    fun suppressFor(durationMs: Long) {
        suppressUntilMs = System.currentTimeMillis() + durationMs
    }

    private fun maybeShow() {
        val activity = activityReference?.get() ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (System.currentTimeMillis() < suppressUntilMs) {
            preload()
            return
        }
        if (!policy.canShowAppOpen()) {
            preload()
            return
        }
        if (showingInProgress.get()) return
        val current = ad
        if (current == null) {
            preload()
            return
        }
        if (showingInProgress.compareAndSet(false, true)) {
            policy.markAppOpenShown()
            current.setAdEventListener(listener)
            try {
                current.show(activity)
            } catch (t: Throwable) {
                AdsLog.w("AppOpen: show failed", t)
                showingInProgress.set(false)
                clearAd()
            }
        }
    }

    private fun clearAd() {
        ad?.setAdEventListener(null)
        ad = null
    }

    fun destroy() {
        runCatching { loader.cancelLoading() }
        clearAd()
    }
}
