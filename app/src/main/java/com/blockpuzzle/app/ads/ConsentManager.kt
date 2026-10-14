package com.blockpuzzle.app.ads

import com.blockpuzzle.app.data.Prefs
import com.yandex.mobile.ads.common.YandexAds

/**
 * Согласие на обработку персональных данных (GDPR/152-ФЗ) и персонализацию рекламы.
 *
 * Как это работает в РСЯ: [YandexAds.setUserConsent] управляет тем, может ли SDK
 * использовать данные устройства для подбора рекламы. Вызывать строго ДО
 * [YandexAds.initialize].
 *
 * Геолокацию не запрашиваем и не передаём: setLocationTracking(false).
 * Если приложение адресовано детям — setAgeRestricted(true) (см. манифест).
 */
class ConsentManager(private val prefs: Prefs) {

    /** Нужно ли спросить согласие: спрашиваем один раз, дальше помним выбор. */
    val needsConsent: Boolean get() = !prefs.consentDecided

    /** Выбор пользователя переживает перезапуск приложения. */
    val personalizedAds: Boolean get() = prefs.personalizedAds

    fun applyToSdk() {
        YandexAds.setUserConsent(prefs.personalizedAds)
        YandexAds.setLocationTracking(false)
        YandexAds.setAgeRestricted(false)
        AdsLog.d("Consent applied: personalized=${prefs.personalizedAds}")
    }

    fun setConsent(granted: Boolean) {
        prefs.personalizedAds = granted
        prefs.consentDecided = true
        // Можно менять и после инициализации — SDK учтёт при следующих запросах.
        YandexAds.setUserConsent(granted)
        AdsLog.i("Consent updated: $granted")
    }
}
