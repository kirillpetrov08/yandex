package com.blockpuzzle.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

/** Снимок прогресса игрока: всё, что сохраняется между запусками. */
data class PlayerState(
    val coins: Int = 0,
    val bestScore: Int = 0,
    val gamesPlayed: Int = 0,
    val totalScore: Long = 0,
    val totalLines: Int = 0,
    val bestCombo: Int = 0,
    val playTimeMs: Long = 0,
    val totalCoinsEarned: Int = 0,
    val achievements: Set<String> = emptySet(),
    val ownedThemes: Set<String> = setOf(THEME_DEFAULT),
    val equippedTheme: String = THEME_DEFAULT,
    val lastDailyBonusDay: Long = 0L,
) {
    companion object {
        const val THEME_DEFAULT = "default"
    }
}

/** Настройки игрока. */
data class Settings(
    val sound: Boolean = true,
    val haptics: Boolean = true,
    val personalizedAds: Boolean = true,
    val consentDecided: Boolean = false,
)

/**
 * Хранилище прогресса и настроек на SharedPreferences + StateFlow для Compose.
 * Отдельный файл (block_puzzle_prefs) включён в правила резервного копирования.
 */
class Prefs(context: Context) {

    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _player = MutableStateFlow(readPlayer())
    val player: StateFlow<PlayerState> = _player

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<Settings> = _settings

    private fun readPlayer(): PlayerState = PlayerState(
        coins = sp.getInt(KEY_COINS, 0),
        bestScore = sp.getInt(KEY_BEST, 0),
        gamesPlayed = sp.getInt(KEY_GAMES, 0),
        totalScore = sp.getLong(KEY_TOTAL_SCORE, 0L),
        totalLines = sp.getInt(KEY_TOTAL_LINES, 0),
        bestCombo = sp.getInt(KEY_BEST_COMBO, 0),
        playTimeMs = sp.getLong(KEY_PLAY_TIME, 0L),
        totalCoinsEarned = sp.getInt(KEY_TOTAL_COINS, 0),
        achievements = sp.getStringSet(KEY_ACHIEVEMENTS, emptySet()) ?: emptySet(),
        ownedThemes = (sp.getStringSet(KEY_OWNED_THEMES, setOf(PlayerState.THEME_DEFAULT))
            ?: setOf(PlayerState.THEME_DEFAULT)).toSet(),
        equippedTheme = sp.getString(KEY_EQUIPPED_THEME, PlayerState.THEME_DEFAULT)
            ?: PlayerState.THEME_DEFAULT,
        lastDailyBonusDay = sp.getLong(KEY_DAILY_BONUS_DAY, 0L),
    )

    private fun readSettings(): Settings = Settings(
        sound = sp.getBoolean(KEY_SOUND, true),
        haptics = sp.getBoolean(KEY_HAPTICS, true),
        personalizedAds = sp.getBoolean(KEY_PERSONALIZED_ADS, true),
        consentDecided = sp.getBoolean(KEY_CONSENT_DECIDED, false),
    )

    private inline fun edit(block: (PlayerState) -> PlayerState) {
        val updated = block(_player.value)
        _player.value = updated
        sp.edit()
            .putInt(KEY_COINS, updated.coins)
            .putInt(KEY_BEST, updated.bestScore)
            .putInt(KEY_GAMES, updated.gamesPlayed)
            .putLong(KEY_TOTAL_SCORE, updated.totalScore)
            .putInt(KEY_TOTAL_LINES, updated.totalLines)
            .putInt(KEY_BEST_COMBO, updated.bestCombo)
            .putLong(KEY_PLAY_TIME, updated.playTimeMs)
            .putInt(KEY_TOTAL_COINS, updated.totalCoinsEarned)
            .putStringSet(KEY_ACHIEVEMENTS, updated.achievements)
            .putStringSet(KEY_OWNED_THEMES, updated.ownedThemes)
            .putString(KEY_EQUIPPED_THEME, updated.equippedTheme)
            .putLong(KEY_DAILY_BONUS_DAY, updated.lastDailyBonusDay)
            .apply()
    }

    /* ------------------------- монеты и прогресс ------------------------- */

    /** Начисление монет (daily bonus, награда за видео, покупка). */
    fun addCoins(amount: Int) {
        if (amount == 0) return
        edit {
            it.copy(
                coins = (it.coins + amount).coerceAtLeast(0),
                totalCoinsEarned = if (amount > 0) it.totalCoinsEarned + amount else it.totalCoinsEarned,
            )
        }
    }

    fun spendCoins(amount: Int): Boolean {
        if (_player.value.coins < amount) return false
        edit { it.copy(coins = it.coins - amount) }
        return true
    }

    /**
     * Результат партии. Передаются дельты, чтобы корректно учитывать
     * партию, продолженную после rewarded-награды (сохранение может быть вызвано дважды).
     */
    fun applyGameResult(
        scoreForBest: Int,
        scoreDelta: Int,
        linesDelta: Int,
        bestCombo: Int,
        playTimeDeltaMs: Long,
        coinsDelta: Int,
        countGame: Boolean,
    ) {
        edit {
            it.copy(
                coins = (it.coins + coinsDelta).coerceAtLeast(0),
                totalCoinsEarned = it.totalCoinsEarned + coinsDelta.coerceAtLeast(0),
                bestScore = maxOf(it.bestScore, scoreForBest),
                gamesPlayed = it.gamesPlayed + if (countGame) 1 else 0,
                totalScore = it.totalScore + scoreDelta.coerceAtLeast(0),
                totalLines = it.totalLines + linesDelta.coerceAtLeast(0),
                bestCombo = maxOf(it.bestCombo, bestCombo),
                playTimeMs = it.playTimeMs + playTimeDeltaMs.coerceAtLeast(0),
            )
        }
    }

    fun claimDailyBonus(coins: Int) {
        edit { it.copy(lastDailyBonusDay = today(), coins = it.coins + coins, totalCoinsEarned = it.totalCoinsEarned + coins) }
    }

    fun ownedThemes(): Set<String> = _player.value.ownedThemes

    fun buyTheme(id: String, price: Int): Boolean {
        val state = _player.value
        if (state.ownedThemes.contains(id)) return true
        if (state.coins < price) return false
        edit { it.copy(coins = it.coins - price, ownedThemes = it.ownedThemes + id, equippedTheme = id) }
        return true
    }

    fun equipTheme(id: String) {
        edit { it.copy(equippedTheme = id) }
    }

    fun unlockAchievements(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val current = _player.value.achievements
        val merged = current + ids
        if (merged.size != current.size) edit { it.copy(achievements = merged) }
    }

    /* ----------------------------- настройки ----------------------------- */

    fun setSound(enabled: Boolean) {
        _settings.value = _settings.value.copy(sound = enabled)
        sp.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun setHaptics(enabled: Boolean) {
        _settings.value = _settings.value.copy(haptics = enabled)
        sp.edit().putBoolean(KEY_HAPTICS, enabled).apply()
    }

    var personalizedAds: Boolean
        get() = _settings.value.personalizedAds
        set(value) {
            _settings.value = _settings.value.copy(personalizedAds = value)
            sp.edit().putBoolean(KEY_PERSONALIZED_ADS, value).apply()
        }

    var consentDecided: Boolean
        get() = _settings.value.consentDecided
        set(value) {
            _settings.value = _settings.value.copy(consentDecided = value)
            sp.edit().putBoolean(KEY_CONSENT_DECIDED, value).apply()
        }

    /** Полный сброс прогресса (не трогает выбор по персонализации рекламы). */
    fun resetProgress() {
        sp.edit()
            .remove(KEY_COINS)
            .remove(KEY_BEST)
            .remove(KEY_GAMES)
            .remove(KEY_TOTAL_SCORE)
            .remove(KEY_TOTAL_LINES)
            .remove(KEY_BEST_COMBO)
            .remove(KEY_PLAY_TIME)
            .remove(KEY_TOTAL_COINS)
            .remove(KEY_ACHIEVEMENTS)
            .remove(KEY_OWNED_THEMES)
            .remove(KEY_EQUIPPED_THEME)
            .remove(KEY_DAILY_BONUS_DAY)
            .apply()
        _player.value = PlayerState()
    }

    /** Ежедневный бонус доступен раз в сутки. */
    fun isDailyBonusAvailable(): Boolean = _player.value.lastDailyBonusDay != today()

    private fun today(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis / 86_400_000L
    }

    companion object {
        const val FILE_NAME = "block_puzzle_prefs"

        private const val KEY_COINS = "coins"
        private const val KEY_BEST = "best"
        private const val KEY_GAMES = "games"
        private const val KEY_TOTAL_SCORE = "total_score"
        private const val KEY_TOTAL_LINES = "total_lines"
        private const val KEY_BEST_COMBO = "best_combo"
        private const val KEY_PLAY_TIME = "play_time"
        private const val KEY_TOTAL_COINS = "total_coins"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val KEY_OWNED_THEMES = "owned_themes"
        private const val KEY_EQUIPPED_THEME = "equipped_theme"
        private const val KEY_DAILY_BONUS_DAY = "daily_bonus_day"
        private const val KEY_SOUND = "sound"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_PERSONALIZED_ADS = "personalized_ads"
        private const val KEY_CONSENT_DECIDED = "consent_decided"
    }
}
