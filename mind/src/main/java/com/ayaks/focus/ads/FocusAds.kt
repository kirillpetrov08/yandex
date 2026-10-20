package com.ayaks.focus.ads

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.ayaks.focus.BuildConfig
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.YandexAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

object FocusAds {
    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized

    fun initialize(context: Context, personalizedAds: Boolean) {
        if (!BuildConfig.ADS_CONFIGURED || _initialized.value) return
        val app = context.applicationContext as android.app.Application
        YandexAds.setUserConsent(personalizedAds)
        YandexAds.setLocationTracking(false)
        YandexAds.setAgeRestricted(false)
        YandexAds.initialize(app) { _initialized.value = true }
    }

    fun updateConsent(personalizedAds: Boolean) {
        if (BuildConfig.ADS_CONFIGURED) YandexAds.setUserConsent(personalizedAds)
    }
}

/** One adaptive sticky banner on non-game screens only. It never overlays content. */
@Composable
fun FocusBanner(modifier: Modifier = Modifier) {
    if (!BuildConfig.ADS_CONFIGURED) return
    val context = LocalContext.current
    val density = LocalDensity.current
    val initialized by FocusAds.initialized.collectAsState()
    val holder = remember { BannerHolder(context, BuildConfig.BANNER_AD_UNIT_ID) }
    var widthPx by remember { mutableIntStateOf(0) }
    var heightPx by remember { mutableIntStateOf(0) }

    DisposableEffect(holder) { onDispose { holder.destroy() } }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { heightPx.toDp() })
            .onGloballyPositioned { widthPx = it.size.width },
    ) {
        if (initialized && widthPx > 0) {
            val widthDp = with(density) { widthPx.toDp().value.roundToInt() }
            AndroidView(modifier = Modifier.fillMaxWidth(), factory = { holder.ensureView() })
            androidx.compose.runtime.LaunchedEffect(widthDp, initialized) {
                heightPx = holder.load(widthDp)
            }
        }
    }
}

private class BannerHolder(private val context: Context, private val unitId: String) {
    var adView: BannerAdView? = null
        private set

    private val listener = object : BannerAdEventListener {
        override fun onAdLoaded() = Unit
        override fun onAdFailedToLoad(adRequestError: AdRequestError) = Unit
        override fun onAdClicked() = Unit
        override fun onImpression(impressionData: ImpressionData?) = Unit
    }

    fun ensureView(): BannerAdView = adView ?: BannerAdView(context).apply {
        setBannerAdEventListener(listener)
        adView = this
    }

    fun load(widthDp: Int): Int {
        val view = ensureView()
        val size = BannerAdSize.sticky(context, widthDp)
        view.setAdSize(size)
        view.loadAd(AdRequest.Builder(unitId).build())
        return size.getHeightInPixels(context)
    }

    fun destroy() {
        adView?.let { it.setBannerAdEventListener(null); it.destroy() }
        adView = null
    }
}
