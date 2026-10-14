package com.blockpuzzle.app.data

/**
 * Достижения. Логика живёт в data-слое (не зависит от Android), а тексты
 * подставляются в UI по [Achievement.id] (см. ui/screens/StatsScreen.kt).
 */
data class Achievement(
    val id: String,
    val isUnlocked: (PlayerState) -> Boolean,
)

object Achievements {
    private const val PREFIX = "ach."

    val all: List<Achievement> = listOf(
        Achievement("first_game") { it.gamesPlayed >= 1 },
        Achievement("score_1000") { it.bestScore >= 1000 },
        Achievement("score_5000") { it.bestScore >= 5000 },
        Achievement("lines_100") { it.totalLines >= 100 },
        Achievement("combo_5") { it.bestCombo >= 5 },
        Achievement("games_10") { it.gamesPlayed >= 10 },
        Achievement("games_50") { it.gamesPlayed >= 50 },
        Achievement("coins_500") { it.totalCoinsEarned >= 500 },
    )

    fun key(id: String) = PREFIX + id

    /** Возвращает ключи новых достижений и сохраняет их в хранилище. */
    fun checkAndUnlock(prefs: Prefs): List<String> {
        val state = prefs.player.value
        val unlocked = all.filter { it.isUnlocked(state) }.map { key(it.id) }
        val fresh = unlocked.filterNot { state.achievements.contains(it) }
        if (fresh.isNotEmpty()) prefs.unlockAchievements(fresh)
        return fresh
    }
}
