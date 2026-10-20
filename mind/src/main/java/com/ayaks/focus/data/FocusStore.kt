package com.ayaks.focus.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FocusStats(
    val sessions: Int = 0,
    val bestScore: Int = 0,
    val totalPoints: Long = 0L,
    val dailyStreak: Int = 0,
    val lastDailyKey: String = "",
    val dailyBest: Int = 0,
)

data class FocusSettings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val personalizedAds: Boolean = false,
    val consentDecided: Boolean = false,
)

class FocusStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("focus_local", Context.MODE_PRIVATE)
    private val _stats = mutableStateOf(readStats())
    val stats: State<FocusStats> get() = _stats
    private val _settings = mutableStateOf(readSettings())
    val settings: State<FocusSettings> get() = _settings

    private fun readStats() = FocusStats(
        sessions = prefs.getInt("sessions", 0),
        bestScore = prefs.getInt("best", 0),
        totalPoints = prefs.getLong("total", 0L),
        dailyStreak = prefs.getInt("streak", 0),
        lastDailyKey = prefs.getString("daily_key", "") ?: "",
        dailyBest = prefs.getInt("daily_best", 0),
    )

    private fun readSettings() = FocusSettings(
        sound = prefs.getBoolean("sound", true),
        haptics = prefs.getBoolean("haptics", true),
        personalizedAds = prefs.getBoolean("personalized_ads", false),
        consentDecided = prefs.getBoolean("consent_decided", false),
    )

    fun recordSession(score: Int, daily: Boolean) {
        val today = todayKey()
        val current = _stats.value
        var streak = current.dailyStreak
        var dailyKey = current.lastDailyKey
        var dailyBest = current.dailyBest
        if (daily && current.lastDailyKey != today) {
            streak = if (current.lastDailyKey == yesterdayKey()) streak + 1 else 1
            dailyKey = today
            dailyBest = score
        } else if (daily) {
            dailyBest = maxOf(dailyBest, score)
        }
        _stats.value = current.copy(
            sessions = current.sessions + 1,
            bestScore = maxOf(current.bestScore, score),
            totalPoints = current.totalPoints + score,
            dailyStreak = streak,
            lastDailyKey = dailyKey,
            dailyBest = dailyBest,
        )
        prefs.edit()
            .putInt("sessions", _stats.value.sessions)
            .putInt("best", _stats.value.bestScore)
            .putLong("total", _stats.value.totalPoints)
            .putInt("streak", streak)
            .putString("daily_key", dailyKey)
            .putInt("daily_best", dailyBest)
            .apply()
    }

    fun updateSettings(settings: FocusSettings) {
        _settings.value = settings
        prefs.edit()
            .putBoolean("sound", settings.sound)
            .putBoolean("haptics", settings.haptics)
            .putBoolean("personalized_ads", settings.personalizedAds)
            .putBoolean("consent_decided", settings.consentDecided)
            .apply()
    }

    fun isDailyComplete(): Boolean = _stats.value.lastDailyKey == todayKey()

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun yesterdayKey(): String {
        val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}
