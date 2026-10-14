package com.blockpuzzle.app.ads

import android.util.Log
import com.blockpuzzle.app.BuildConfig

/**
 * Единая точка логирования рекламы.
 *
 * Проверка интеграции SDK: adb logcat -v brief '*:S YandexAds'
 * (SDK сам печатает диагностику под тегом YandexAds).
 */
object AdsLog {
    const val TAG = "YandexAds"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
    }
}
