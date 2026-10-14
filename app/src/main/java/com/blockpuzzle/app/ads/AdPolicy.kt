package com.blockpuzzle.app.ads

/**
 * Частотные политики показов. Правила РСЯ требуют не «спамить» рекламой:
 * межстраничная реклама не должна прерывать игру и показываться слишком часто,
 * App Open — не чаще, чем разумный интервал (по умолчанию 4 часа).
 *
 * Класс чистый и тестируемый: время передаётся лямбдой.
 */
class AdPolicy(
    private val now: () -> Long = { System.currentTimeMillis() },
    private val interstitialIntervalMs: Long = AdUnits.interstitialIntervalMs,
    private val interstitialGamesInterval: Int = AdUnits.interstitialGamesInterval,
    private val appOpenIntervalMs: Long = AdUnits.appOpenIntervalMs,
) {
    // null вместо 0L: время 0 — валидное значение для тестов, поэтому «показов не было»
    // нельзя кодировать нулём (иначе первый же показ не уважал бы интервал).
    private var lastInterstitialShownAt: Long? = null
    private var gamesPlayed = 0
    private var appOpenShownAt: Long? = null
    private var rewardedShownCount = 0

    /** Вызывается, когда пользователь завершил партию. Возвращает true для готовности показа. */
    fun onGameFinished(): Boolean {
        gamesPlayed++
        return canShowInterstitial()
    }

    fun canShowInterstitial(): Boolean {
        if (interstitialGamesInterval > 1 && gamesPlayed % interstitialGamesInterval != 0) return false
        val last = lastInterstitialShownAt ?: return true
        return now() - last >= interstitialIntervalMs
    }

    fun markInterstitialShown() {
        lastInterstitialShownAt = now()
    }

    fun canShowAppOpen(): Boolean {
        val last = appOpenShownAt ?: return true
        return now() - last >= appOpenIntervalMs
    }

    fun markAppOpenShown() {
        appOpenShownAt = now()
    }

    fun markRewardedShown() {
        rewardedShownCount++
    }

    fun rewardedShown(): Int = rewardedShownCount

    /** Для отладки: сбросить историю показов. */
    fun reset() {
        lastInterstitialShownAt = null
        appOpenShownAt = null
        gamesPlayed = 0
    }
}
