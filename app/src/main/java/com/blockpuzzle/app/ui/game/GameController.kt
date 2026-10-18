package com.blockpuzzle.app.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blockpuzzle.app.data.Achievements
import com.blockpuzzle.app.data.Prefs
import com.blockpuzzle.app.game.GameEngine
import com.blockpuzzle.app.game.GRID_SIZE
import com.blockpuzzle.app.game.MoveResult
import com.blockpuzzle.app.game.Shape
import com.blockpuzzle.app.game.TRAY_SIZE
import com.blockpuzzle.app.sfx.SfxEngine

/** Всплеск комбо для анимации на экране. */
data class ComboFlash(val lines: Int, val multiplier: Int, val gained: Int)

/**
 * Состояние одной партии: движок + зеркала для Compose + награды + сохранение результата.
 * Не ViewModel: экран заблокирован в портретной ориентации, а уход с экрана завершает партию.
 */
class GameController(
    private val prefs: Prefs,
    private val sfx: SfxEngine,
) {
    val engine = GameEngine()
    val fx = FxController()

    val cells = mutableStateListOf<Int>().apply { repeat(GRID_SIZE * GRID_SIZE) { add(0) } }
    val tray = mutableStateListOf<Shape?>(null, null, null)
    val trayColors = mutableStateListOf(0, 0, 0)

    var score by mutableIntStateOf(0)
        private set
    var lines by mutableIntStateOf(0)
        private set
    var combo by mutableIntStateOf(0)
        private set
    var gameOver by mutableStateOf(false)
        private set
    var canUndo by mutableStateOf(false)
        private set
    private var undoUsed = false
    var coinsEarned by mutableIntStateOf(0)
        private set
    var continueGranted by mutableStateOf(false)
        private set
    var doubleCoinsGranted by mutableStateOf(false)
        private set
    var comboFlash by mutableStateOf<ComboFlash?>(null)
        private set
    var lastMove by mutableStateOf<MoveResult?>(null)
        private set

    /** Счётчик ходов: используется как ключ эффектов, чтобы одинаковые ходы не «схлопывались». */
    var moveCounter by mutableIntStateOf(0)
        private set
    var resultSaved = false
        private set

    private var startedAt = System.currentTimeMillis()
    private var savedScore = 0
    private var savedLines = 0
    private var savedCoins = 0
    private var savedPlayTimeMs = 0L
    private var sessionCounted = false

    init {
        startNewGame()
    }

    fun startNewGame() {
        engine.newGame()
        fx.clearAll()
        comboFlash = null
        lastMove = null
        moveCounter = 0
        continueGranted = false
        doubleCoinsGranted = false
        undoUsed = false
        resultSaved = false
        savedScore = 0
        savedLines = 0
        savedCoins = 0
        savedPlayTimeMs = 0L
        sessionCounted = false
        startedAt = System.currentTimeMillis()
        sync()
        gameOver = false
    }

    /** @return true, если ход сделан. */
    fun place(trayIndex: Int, row: Int, col: Int): Boolean {
        val result = engine.place(trayIndex, row, col) ?: run {
            sfx.play(SfxEngine.Sound.REJECT)
            return false
        }
        lastMove = result
        moveCounter++
        sync()
        if (result.clearedLines > 0) {
            sfx.play(if (result.comboMultiplier > 1) SfxEngine.Sound.COMBO else SfxEngine.Sound.CLEAR)
            comboFlash = ComboFlash(result.clearedLines, result.comboMultiplier, result.gainedScore)
            fx.shake(0.45f + 0.12f * result.clearedLines)
        } else {
            sfx.play(SfxEngine.Sound.PLACE)
        }
        if (gameOver) {
            coinsEarned = engine.coinsEarned()
            sfx.play(SfxEngine.Sound.FAIL)
        }
        return true
    }

    fun playPickup() {
        sfx.play(SfxEngine.Sound.PICKUP)
    }

    /** Один бесплатный откат на партию — помогает исправить случайный ход. */
    fun undoLastMove(): Boolean {
        if (undoUsed || !engine.undoLastMove()) return false
        undoUsed = true
        fx.clearAll()
        comboFlash = null
        lastMove = null
        sync()
        sfx.play(SfxEngine.Sound.CLICK)
        return true
    }

    /** Тап по фигуре — ставим её в лучшую доступную позицию. */
    fun autoPlace(trayIndex: Int): Boolean {
        val best = engine.bestPlacement(trayIndex) ?: return false
        return place(trayIndex, best.first, best.second)
    }

    /** Награда «Продолжить» получена: очищаем нижние ряды и играем дальше. */
    fun applyContinue() {
        if (!gameOver) return
        val cleared = engine.continueGame()
        continueGranted = true
        gameOver = false
        sync()
        fx.addCellPops(cleared, isClear = true)
        fx.shake(0.6f)
        sfx.play(SfxEngine.Sound.CLEAR)
    }

    fun applyDoubleCoins() {
        if (doubleCoinsGranted) return
        doubleCoinsGranted = true
        sfx.play(SfxEngine.Sound.COIN)
    }

    /** Сколько монет получит игрок с учётом удвоения. */
    fun finalCoins(): Int = if (doubleCoinsGranted) coinsEarned * 2 else coinsEarned

    /**
     * Сохраняем результат. Метод идемпотентен: если партию продолжали после рекламы,
     * сохранятся только дельты (иначе статистика и монеты задвоились бы).
     */
    fun saveResult() {
        val newCoins = finalCoins()
        val scoreDelta = score - savedScore
        val linesDelta = lines - savedLines
        val coinsDelta = newCoins - savedCoins
        val elapsed = System.currentTimeMillis() - startedAt
        val timeDelta = elapsed - savedPlayTimeMs
        if (scoreDelta <= 0 && linesDelta <= 0 && coinsDelta <= 0) {
            resultSaved = true
            return
        }
        prefs.applyGameResult(
            scoreForBest = score,
            scoreDelta = scoreDelta,
            linesDelta = linesDelta,
            bestCombo = engine.bestCombo,
            playTimeDeltaMs = timeDelta,
            coinsDelta = coinsDelta,
            countGame = !sessionCounted,
        )
        savedScore = score
        savedLines = lines
        savedCoins = newCoins
        savedPlayTimeMs = elapsed
        sessionCounted = true
        resultSaved = true
        Achievements.checkAndUnlock(prefs)
    }

    fun clearComboFlash() {
        comboFlash = null
    }

    fun consumeLastMove() {
        lastMove = null
    }

    private fun sync() {
        for (i in cells.indices) cells[i] = engine.cells[i]
        for (i in 0 until TRAY_SIZE) {
            tray[i] = engine.tray[i]
            trayColors[i] = engine.trayColors[i]
        }
        score = engine.score
        lines = engine.lines
        combo = engine.combo
        gameOver = engine.isGameOver
        canUndo = !undoUsed && engine.canUndoLastMove && !gameOver
    }
}
