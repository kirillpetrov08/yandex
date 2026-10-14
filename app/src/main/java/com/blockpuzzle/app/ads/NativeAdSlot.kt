package com.blockpuzzle.app.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.blockpuzzle.app.R
import com.yandex.mobile.ads.common.AdBindingResult
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdEventListener
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdOptions
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder

/**
 * Нативная реклама РСЯ, встроенная в дизайн приложения (используется в магазине).
 *
 * Правила размещения нативной рекламы, которые соблюдены:
 *  - все обязательные элементы (метка «реклама», возрастная метка, заголовок,
 *    изображение, кнопка CTA, кнопка обратной связи) переданы в NativeAdViewBinder;
 *  - тексты и ссылки элементов формирует SDK — мы их не меняем;
 *  - блок не кликабелен целиком: кликабельны только элементы объявления;
 *  - блок не перезагружается чаще одного раза в 30 секунд;
 *  - при неудачной привязке креатива блок полностью скрывается.
 */
@Composable
fun YandexNativeAdSlot(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnits.native,
) {
    val context = LocalContext.current
    val initialized by Ads.initialized.collectAsState()
    val holder = remember(adUnitId) { NativeAdHolder(context, adUnitId) }

    DisposableEffect(holder) {
        onDispose { holder.destroy() }
    }

    if (initialized) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { holder.view },
        )
        LaunchedEffect(holder) { holder.load() }
    }
}

/** Держит размеченный блок нативной рекламы и управляет загрузкой/привязкой. */
private class NativeAdHolder(
    private val context: Context,
    private val adUnitId: String,
) {
    private var loader: NativeAdLoader? = null
    private var nativeAd: NativeAd? = null
    private var lastLoadAt = 0L

    private val root: View = LayoutInflater.from(context).inflate(R.layout.native_ad_card, null, false)

    private val nativeAdView: NativeAdView = root as NativeAdView
    private val ageView: TextView = root.findViewById(R.id.native_ad_age)
    private val sponsoredView: TextView = root.findViewById(R.id.native_ad_sponsored)
    private val feedbackView: ImageView = root.findViewById(R.id.native_ad_feedback)
    private val titleView: TextView = root.findViewById(R.id.native_ad_title)
    private val bodyView: TextView = root.findViewById(R.id.native_ad_body)
    private val domainView: TextView = root.findViewById(R.id.native_ad_domain)
    private val iconView: ImageView = root.findViewById(R.id.native_ad_icon)
    private val mediaView: MediaView = root.findViewById(R.id.native_ad_media)
    private val callToActionView: Button = root.findViewById(R.id.native_ad_call_to_action)

    val view: View get() = root

    init {
        root.visibility = View.GONE
    }

    fun load() {
        if (nativeAd != null || loader != null) return
        // Ограничение РСЯ: блок на одном экране не перезагружается чаще, чем раз в 30 секунд.
        if (lastLoadAt != 0L && System.currentTimeMillis() - lastLoadAt < 30_000L) return
        lastLoadAt = System.currentTimeMillis()

        val currentLoader = NativeAdLoader(context)
        loader = currentLoader
        val options = NativeAdOptions.Builder()
            .setShouldLoadImagesAutomatically(true)
            .build()
        currentLoader.loadAd(
            AdRequest.Builder(adUnitId).build(),
            options,
            object : NativeAdLoadListener {
                override fun onAdLoaded(ad: NativeAd) {
                    bind(ad)
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    AdsLog.w("Native($adUnitId): onAdFailedToLoad ${error.code} ${error.description}")
                    loader = null
                    root.visibility = View.GONE
                }
            },
        )
    }

    private fun bind(ad: NativeAd) {
        val binder = NativeAdViewBinder.Builder(nativeAdView)
            .setAgeView(ageView)
            .setSponsoredView(sponsoredView)
            .setFeedbackView(feedbackView)
            .setTitleView(titleView)
            .setBodyView(bodyView)
            .setDomainView(domainView)
            .setIconView(iconView)
            .setMediaView(mediaView)
            .setCallToActionView(callToActionView)
            .build()

        when (ad.bindNativeAd(binder)) {
            is AdBindingResult.Success -> {
                ad.setNativeAdEventListener(object : NativeAdEventListener {
                    override fun onAdClicked() = AdsLog.d("Native($adUnitId): onAdClicked")

                    override fun onImpression(data: ImpressionData?) =
                        AdsLog.d("Native($adUnitId): onAdImpression")
                })
                nativeAd = ad
                root.visibility = View.VISIBLE
                AdsLog.d("Native($adUnitId): bound")
            }

            is AdBindingResult.Failure -> {
                AdsLog.w("Native($adUnitId): binding failed")
                root.visibility = View.GONE
            }
        }
    }

    fun destroy() {
        loader?.cancelLoading()
        loader = null
        nativeAd?.setNativeAdEventListener(null)
        nativeAd = null
        root.visibility = View.GONE
    }
}
