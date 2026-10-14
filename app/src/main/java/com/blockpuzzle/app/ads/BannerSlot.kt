package com.blockpuzzle.app.ads

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.mobile.ads.banner.BannerAdEventListener
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import kotlin.math.roundToInt

/**
 * Адаптивный sticky-баннер РСЯ.
 *
 * Правила, которые здесь соблюдены:
 *  - размер считает SDK: BannerAdSize.sticky(context, ширина в dp) — высота не более 15% экрана;
 *  - баннер не перекрывает контент (мы резервируем под него место в разметке);
 *  - не показываем баннер во время геймплея (только меню/магазин/статистика);
 *  - при уходе с экрана вызываем destroy(), чтобы не было утечек;
 *  - не пытаемся перезагрузить объявление бесконечным циклом при ошибке.
 */
@Composable
fun YandexBannerSlot(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnits.banner,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val initialized by Ads.initialized.collectAsState()

    val holder = remember(adUnitId) { BannerHolder(context, adUnitId) }
    var containerWidthPx by remember { mutableIntStateOf(0) }
    var bannerHeightPx by remember { mutableIntStateOf(0) }

    DisposableEffect(holder) {
        onDispose { holder.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(with(density) { bannerHeightPx.toDp() })
            .onGloballyPositioned { coordinates -> containerWidthPx = coordinates.size.width },
    ) {
        if (initialized && containerWidthPx > 0) {
            val widthDp = with(density) { containerWidthPx.toDp().value.roundToInt() }
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { holder.ensureView() },
            )
            LaunchedEffect(widthDp, initialized) {
                bannerHeightPx = holder.load(widthDp)
            }
        }
    }
}

/** Держит BannerAdView и аккуратно управляет его жизненным циклом. */
private class BannerHolder(
    private val context: Context,
    private val adUnitId: String,
) {
    private var adView: BannerAdView? = null

    private val listener = object : BannerAdEventListener {
        override fun onAdLoaded() {
            AdsLog.d("Banner($adUnitId): onAdLoaded")
        }

        override fun onAdFailedToLoad(adRequestError: AdRequestError) {
            // Повторные попытки не запускаем: SDK сам обновит баннер при следующем запросе.
            AdsLog.w("Banner($adUnitId): onAdFailedToLoad ${adRequestError.code} ${adRequestError.description}")
        }

        override fun onAdClicked() = AdsLog.d("Banner($adUnitId): onAdClicked")

        override fun onImpression(impressionData: ImpressionData?) =
            AdsLog.d("Banner($adUnitId): onAdImpression")
    }

    fun ensureView(): BannerAdView = adView ?: BannerAdView(context).apply {
        setBannerAdEventListener(listener)
        adView = this
    }

    /** Возвращает высоту баннера в пикселях — известна до отправки запроса. */
    fun load(widthDp: Int): Int {
        val view = ensureView()
        val size = BannerAdSize.sticky(context, widthDp)
        view.setAdSize(size)
        view.loadAd(AdRequest.Builder(adUnitId).build())
        return size.getHeightInPixels(context)
    }

    fun destroy() {
        adView?.let {
            it.setBannerAdEventListener(null)
            it.destroy()
        }
        adView = null
    }
}
