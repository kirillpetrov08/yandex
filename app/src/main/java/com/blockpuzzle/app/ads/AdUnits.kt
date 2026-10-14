package com.blockpuzzle.app.ads

import com.blockpuzzle.app.BuildConfig

/**
 * Все идентификаторы рекламных блоков и частотные настройки.
 *
 * Значения приходят из BuildConfig, а тот — из gradle.properties.
 * Переключение на реальные блоки РСЯ:
 *   1) в интерфейсе РСЯ (partner.yandex.ru) создайте блоки нужных форматов;
 *   2) впишите их ID (вида R-M-1234567-1) в gradle.properties;
 *   3) поставьте ads.testMode=false.
 *
 * Демонстрационные блоки (гарантированно отдают тестовые креативы):
 *   demo-banner-yandex, demo-interstitial-yandex, demo-rewarded-yandex,
 *   demo-appopenad-yandex, demo-native-content-yandex
 */
object AdUnits {
    val banner: String = BuildConfig.AD_UNIT_BANNER
    val interstitial: String = BuildConfig.AD_UNIT_INTERSTITIAL
    val rewarded: String = BuildConfig.AD_UNIT_REWARDED
    val appOpen: String = BuildConfig.AD_UNIT_APP_OPEN
    val native: String = BuildConfig.AD_UNIT_NATIVE

    /** true — используются демо-блоки (в логах SDK это видно). */
    val testMode: Boolean = BuildConfig.ADS_TEST_MODE

    /** Минимальный интервал между межстраничными показами. */
    val interstitialIntervalMs: Long = BuildConfig.AD_INTERSTITIAL_INTERVAL_SECONDS * 1000L

    /** Показывать межстраничную рекламу каждый N-й проигрыш. */
    val interstitialGamesInterval: Int = BuildConfig.AD_INTERSTITIAL_GAMES_INTERVAL.coerceAtLeast(1)

    /** Минимальный интервал между показами App Open. */
    val appOpenIntervalMs: Long = BuildConfig.AD_APP_OPEN_INTERVAL_HOURS * 3_600_000L

    /** Любой блок в тестовом режиме — повод показать пометку в настройках. */
    const val REWARDED_COINS: Int = 50
    const val DAILY_BONUS_COINS: Int = 25
}
