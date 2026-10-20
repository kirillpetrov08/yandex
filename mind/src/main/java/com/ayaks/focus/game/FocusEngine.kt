package com.ayaks.focus.game

import kotlin.random.Random

/** Three short, offline exercises. Entertainment and wellness only; not a medical test. */
enum class FocusMode(val title: String, val subtitle: String, val icon: String) {
    MEMORY("Память", "Запомни и повтори последовательность", "✦"),
    SEARCH("Внимание", "Найди нужный знак среди похожих", "⌕"),
    COLORS("Цвет", "Выбери цвет букв, не само слово", "◉"),
}

enum class GamePhase { SHOWING, INPUT, FEEDBACK, FINISHED }

data class FocusSession(
    val mode: FocusMode,
    val round: Int = 1,
    val score: Int = 0,
    val lives: Int = 3,
    val streak: Int = 0,
    val phase: GamePhase = GamePhase.SHOWING,
    val sequence: List<Int> = emptyList(),
    val sequencePosition: Int = 0,
    val litCell: Int? = null,
    val gridSize: Int = 4,
    val searchTiles: List<Int> = emptyList(),
    val searchTarget: Int = 0,
    val wordColor: Int = 0,
    val inkColor: Int = 1,
    val secondsLeft: Int = 45,
    val isDaily: Boolean = false,
    val seed: Int = 0,
    val completedRounds: Int = 0,
)

data class AnswerResult(val session: FocusSession, val correct: Boolean)

/** Pure deterministic rules; random seed is injected so daily rounds can be reproduced. */
class FocusEngine(seed: Int) {
    private val random = Random(seed)

    fun start(mode: FocusMode, daily: Boolean = false, seed: Int = 0): FocusSession =
        makeRound(
            FocusSession(mode = mode, isDaily = daily, seed = seed),
            round = 1,
            phase = if (mode == FocusMode.MEMORY) GamePhase.SHOWING else GamePhase.INPUT,
        )

    fun tapMemory(session: FocusSession, cell: Int): AnswerResult {
        if (session.mode != FocusMode.MEMORY || session.phase != GamePhase.INPUT) return AnswerResult(session, false)
        val expected = session.sequence.getOrNull(session.sequencePosition) ?: return AnswerResult(session, false)
        if (cell != expected) {
            val lives = session.lives - 1
            val ended = lives <= 0
            val next = session.copy(
                lives = lives.coerceAtLeast(0),
                streak = 0,
                phase = if (ended) GamePhase.FINISHED else GamePhase.FEEDBACK,
                sequencePosition = 0,
                litCell = null,
            )
            return AnswerResult(next, false)
        }
        val nextPosition = session.sequencePosition + 1
        if (nextPosition < session.sequence.size) {
            return AnswerResult(session.copy(sequencePosition = nextPosition, streak = session.streak + 1), true)
        }
        val gained = session.sequence.size * 10 + session.streak * 2
        val completed = session.completedRounds + 1
        if (completed >= MEMORY_ROUNDS) {
            return AnswerResult(session.copy(
                score = session.score + gained,
                streak = session.streak + 1,
                completedRounds = completed,
                phase = GamePhase.FINISHED,
                litCell = null,
            ), true)
        }
        return AnswerResult(
            makeRound(
                session.copy(score = session.score + gained, streak = session.streak + 1, completedRounds = completed),
                round = session.round + 1,
                phase = GamePhase.SHOWING,
            ),
            true,
        )
    }

    fun revealCell(session: FocusSession, cell: Int?): FocusSession =
        if (session.mode == FocusMode.MEMORY && session.phase == GamePhase.SHOWING) session.copy(litCell = cell) else session

    fun memoryInputReady(session: FocusSession): FocusSession =
        if (session.mode == FocusMode.MEMORY && session.phase == GamePhase.SHOWING) session.copy(phase = GamePhase.INPUT, litCell = null) else session

    fun retryMemory(session: FocusSession): FocusSession =
        if (session.mode == FocusMode.MEMORY && session.phase == GamePhase.FEEDBACK) session.copy(phase = GamePhase.SHOWING, sequencePosition = 0) else session

    fun resumeSearch(session: FocusSession): FocusSession =
        if (session.mode == FocusMode.SEARCH && session.phase == GamePhase.FEEDBACK) session.copy(phase = GamePhase.INPUT) else session

    fun tapSearch(session: FocusSession, cell: Int): AnswerResult {
        if (session.mode != FocusMode.SEARCH || session.phase != GamePhase.INPUT) return AnswerResult(session, false)
        val correct = session.searchTiles.getOrNull(cell) == session.searchTarget
        if (!correct) {
            val lives = (session.lives - 1).coerceAtLeast(0)
            return AnswerResult(session.copy(
                lives = lives,
                streak = 0,
                phase = if (lives == 0) GamePhase.FINISHED else GamePhase.FEEDBACK,
            ), false)
        }
        val nextRound = session.completedRounds + 1
        val updated = session.copy(
            score = session.score + 10 + session.streak.coerceAtMost(5) * 2,
            streak = session.streak + 1,
            completedRounds = nextRound,
        )
        return if (nextRound >= SEARCH_ROUNDS) AnswerResult(updated.copy(phase = GamePhase.FINISHED), true)
        else AnswerResult(makeRound(updated, round = updated.round + 1, phase = GamePhase.INPUT), true)
    }

    fun answerColor(session: FocusSession, selectedColor: Int): AnswerResult {
        if (session.mode != FocusMode.COLORS || session.phase != GamePhase.INPUT) return AnswerResult(session, false)
        val correct = selectedColor == session.inkColor
        val nextRound = session.completedRounds + 1
        val lives = if (correct) session.lives else (session.lives - 1).coerceAtLeast(0)
        val updated = session.copy(
            score = session.score + if (correct) 10 + session.streak.coerceAtMost(5) * 2 else 0,
            lives = lives,
            streak = if (correct) session.streak + 1 else 0,
            completedRounds = nextRound,
        )
        return if (lives == 0 || nextRound >= COLOR_ROUNDS) AnswerResult(updated.copy(phase = GamePhase.FINISHED), correct)
        else AnswerResult(makeRound(updated, round = updated.round + 1, phase = GamePhase.INPUT), correct)
    }

    fun tick(session: FocusSession): FocusSession {
        if (session.phase != GamePhase.INPUT || session.mode == FocusMode.MEMORY) return session
        val remaining = (session.secondsLeft - 1).coerceAtLeast(0)
        return session.copy(secondsLeft = remaining, phase = if (remaining == 0) GamePhase.FINISHED else session.phase)
    }

    private fun makeRound(session: FocusSession, round: Int, phase: GamePhase): FocusSession = when (session.mode) {
        FocusMode.MEMORY -> {
            val length = (2 + round).coerceAtMost(8)
            val sequence = List(length) { random.nextInt(MEMORY_GRID_CELLS) }
            session.copy(round = round, sequence = sequence, sequencePosition = 0, litCell = null, phase = phase)
        }
        FocusMode.SEARCH -> {
            val side = if (round < 5) 4 else 5
            val count = side * side
            val target = random.nextInt(SEARCH_SYMBOLS.size)
            val distractors = SEARCH_SYMBOLS.indices.filter { it != target }
            val targetCell = random.nextInt(count)
            val tiles = List(count) { index -> if (index == targetCell) target else distractors[random.nextInt(distractors.size)] }
            session.copy(round = round, gridSize = side, searchTiles = tiles, searchTarget = target, phase = phase)
        }
        FocusMode.COLORS -> {
            val word = random.nextInt(COLOR_NAMES.size)
            val offsets = (1 until COLOR_NAMES.size).toList()
            val ink = (word + offsets[random.nextInt(offsets.size)]) % COLOR_NAMES.size
            session.copy(round = round, wordColor = word, inkColor = ink, phase = phase)
        }
    }

    companion object {
        const val MEMORY_GRID_SIDE = 3
        const val MEMORY_GRID_CELLS = MEMORY_GRID_SIDE * MEMORY_GRID_SIDE
        const val MEMORY_ROUNDS = 6
        const val SEARCH_ROUNDS = 10
        const val COLOR_ROUNDS = 12
        val SEARCH_SYMBOLS = listOf("◆", "●", "▲", "✚")
        val COLOR_NAMES = listOf("КРАСНЫЙ", "СИНИЙ", "ЖЁЛТЫЙ", "ЗЕЛЁНЫЙ")
        val COLOR_HEX = listOf(0xFFFF647C, 0xFF59C5FF, 0xFFFFD166, 0xFF62D6A7)
    }
}
