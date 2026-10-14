package com.blockpuzzle.app.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Проверяем частотные политики: правила РСЯ запрещают спамить рекламой. */
class AdPolicyTest {

    @Test
    fun `interstitial respects the games interval`() {
        var now = 0L
        val policy = AdPolicy(
            now = { now },
            interstitialIntervalMs = 60_000L,
            interstitialGamesInterval = 2,
            appOpenIntervalMs = 4 * 3_600_000L,
        )

        assertFalse("первый проигрыш — показа нет", policy.onGameFinished())
        assertTrue("второй проигрыш — можно показать", policy.onGameFinished())

        policy.markInterstitialShown()
        assertFalse("сразу после показа — нельзя", policy.canShowInterstitial())

        // Прошло достаточно времени и сыграно нужное число партий
        now += 61_000L
        assertFalse(policy.onGameFinished()) // 3-й проигрыш
        assertTrue(policy.onGameFinished())  // 4-й проигрыш
    }

    @Test
    fun `app open ad is throttle by hours`() {
        var now = 1_000_000L
        val policy = AdPolicy(
            now = { now },
            interstitialIntervalMs = 60_000L,
            interstitialGamesInterval = 1,
            appOpenIntervalMs = 4 * 3_600_000L,
        )

        assertTrue("первый показ разрешён", policy.canShowAppOpen())
        policy.markAppOpenShown()
        assertFalse("второй показ сразу — нельзя", policy.canShowAppOpen())

        now += 3_600_000L
        assertFalse("через час — всё ещё нельзя", policy.canShowAppOpen())

        now += 3 * 3_600_000L + 1
        assertTrue("через 4 часа — можно", policy.canShowAppOpen())
    }

    @Test
    fun `interstitial with interval 1 allows every game but respects timer`() {
        var now = 0L
        val policy = AdPolicy(
            now = { now },
            interstitialIntervalMs = 30_000L,
            interstitialGamesInterval = 1,
            appOpenIntervalMs = 0L,
        )
        assertTrue(policy.onGameFinished())
        policy.markInterstitialShown()
        assertFalse(policy.canShowInterstitial())
        now += 30_001L
        assertTrue("интервал истёк", policy.canShowInterstitial())
    }
}
