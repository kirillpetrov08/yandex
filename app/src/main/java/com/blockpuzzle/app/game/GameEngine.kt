package com.blockpuzzle.app.game

import kotlin.random.Random

const val GRID_SIZE = 8
const val TRAY_SIZE = 3

/** Результат одного хода — всё, что нужно для анимаций, звука и счёта. */
data class MoveResult(
    val placedCellIndices: List<Int>,
    val clearedCellIndices: List<Int>,
    val clearedLines: Int,
    val gainedScore: Int,
    val comboMultiplier: Int,
    val isCombo: Boolean,
)

/**
 * Логика «блочной» головоломки 8×8: три фигуры в лотке, заполненные ряды и столбцы
 * исчезают. Класс полностью детерминирован при заданном Random — покрыт unit-тестами.
 *
 * Ячейка хранит индекс цвета (0 — пусто), чтобы фигуры сохраняли свой цвет на поле.
 */
class GameEngine(private val random: Random = Random.Default) {

    val cells = IntArray(GRID_SIZE * GRID_SIZE)
    private val traySlots = arrayOfNulls<Shape>(TRAY_SIZE)
    private val trayColorSlots = IntArray(TRAY_SIZE)

    var score: Int = 0
        private set
    var lines: Int = 0
        private set

    /** Текущая серия ходов с очисткой линий (0 — серии нет). */
    var combo: Int = 0
        private set
    var bestCombo: Int = 0
        private set
    /** Партия закончилась, если ни одна оставшаяся фигура не помещается на поле. */
    val isGameOver: Boolean
        get() = !hasAnyMove()

    /** Сколько раз игрок уже пользовался «Продолжить» в этой партии. */
    var continuesUsed: Int = 0
        private set

    val tray: List<Shape?> get() = traySlots.toList()
    val trayColors: List<Int> get() = trayColorSlots.toList()

    fun newGame() {
        cells.fill(0)
        score = 0
        lines = 0
        combo = 0
        bestCombo = 0
        continuesUsed = 0
        fillTray(forceEasyStart = true)
    }

    fun index(row: Int, col: Int): Int = row * GRID_SIZE + col

    fun rowOf(index: Int): Int = index / GRID_SIZE

    fun colOf(index: Int): Int = index % GRID_SIZE

    fun cellValue(index: Int): Int = cells[index]

    fun filledCount(): Int = cells.count { it != 0 }

    /** Помещается ли фигура из слота [trayIndex] в позицию (row, col) левым верхним углом. */
    fun canPlace(trayIndex: Int, row: Int, col: Int): Boolean {
        val shape = traySlots.getOrNull(trayIndex) ?: return false
        if (row < 0 || col < 0) return false
        if (row + shape.height > GRID_SIZE || col + shape.width > GRID_SIZE) return false
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (!shape.isFilled(r, c)) continue
                if (cells[index(row + r, col + c)] != 0) return false
            }
        }
        return true
    }

    /** Индексы клеток поля, которые займёт фигура (для превью и анимации). */
    fun previewCells(trayIndex: Int, row: Int, col: Int): List<Int> {
        val shape = traySlots.getOrNull(trayIndex) ?: return emptyList()
        if (row + shape.height > GRID_SIZE || col + shape.width > GRID_SIZE) return emptyList()
        val result = ArrayList<Int>(shape.cellCount)
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (shape.isFilled(r, c)) result.add(index(row + r, col + c))
            }
        }
        return result
    }

    /** Поставить фигуру. Возвращает null, если ход невозможен. */
    fun place(trayIndex: Int, row: Int, col: Int): MoveResult? {
        if (isGameOver) return null
        if (!canPlace(trayIndex, row, col)) return null

        val shape = traySlots.getOrNull(trayIndex) ?: return null
        val color = trayColorSlots[trayIndex]

        val placed = ArrayList<Int>(shape.cellCount)
        for (r in 0 until shape.height) {
            for (c in 0 until shape.width) {
                if (!shape.isFilled(r, c)) continue
                val cellIndex = index(row + r, col + c)
                cells[cellIndex] = color
                placed.add(cellIndex)
            }
        }

        traySlots[trayIndex] = null
        trayColorSlots[trayIndex] = 0

        // Очистка линий
        val fullRows = (0 until GRID_SIZE).filter { r -> (0 until GRID_SIZE).all { c -> cells[index(r, c)] != 0 } }
        val fullCols = (0 until GRID_SIZE).filter { c -> (0 until GRID_SIZE).all { r -> cells[index(r, c)] != 0 } }
        val clearedLines = fullRows.size + fullCols.size

        val clearedCells = ArrayList<Int>()
        if (clearedLines > 0) {
            fullRows.forEach { r -> (0 until GRID_SIZE).forEach { c -> clearedCells.add(index(r, c)) } }
            fullCols.forEach { c -> (0 until GRID_SIZE).forEach { r -> clearedCells.add(index(r, c)) } }
            clearedCells.distinct().forEach { cells[it] = 0 }
        }

        val isCombo = clearedLines > 0
        combo = if (isCombo) combo + 1 else 0
        if (combo > bestCombo) bestCombo = combo

        val multiplier = combo.coerceIn(1, MAX_COMBO_MULTIPLIER)
        val gained = shape.cellCount + if (isCombo) linesScore(clearedLines) * multiplier else 0
        score += gained
        lines += clearedLines

        if (traySlots.all { it == null }) fillTray(forceEasyStart = false)

        return MoveResult(
            placedCellIndices = placed,
            clearedCellIndices = clearedCells.distinct(),
            clearedLines = clearedLines,
            gainedScore = gained,
            comboMultiplier = multiplier,
            isCombo = isCombo,
        )
    }

    /** Есть ли ход хотя бы одной фигурой из лотка. */
    fun hasAnyMove(): Boolean {
        if (traySlots.all { it == null }) return true
        for (slot in traySlots.indices) {
            val shape = traySlots[slot] ?: continue
            for (r in 0..(GRID_SIZE - shape.height)) {
                for (c in 0..(GRID_SIZE - shape.width)) {
                    if (canPlace(slot, r, c)) return true
                }
            }
        }
        return false
    }

    /**
     * Награда «Продолжить»: очищаем два нижних ряда, выдаём новый лоток
     * и продолжаем партию. Возвращает индексы очищенных клеток для анимации.
     */
    fun continueGame(): List<Int> {
        val cleared = ArrayList<Int>()
        for (r in (GRID_SIZE - 2) until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                val i = index(r, c)
                if (cells[i] != 0) {
                    cells[i] = 0
                    cleared.add(i)
                }
            }
        }
        combo = 0
        continuesUsed++
        fillTray(forceEasyStart = true)
        if (!hasAnyMove()) {
            // Крайне редкий случай: даже после очистки хода нет — чистим ещё два ряда.
            for (r in (GRID_SIZE - 4) until (GRID_SIZE - 2)) {
                for (c in 0 until GRID_SIZE) {
                    val i = index(r, c)
                    if (cells[i] != 0) {
                        cells[i] = 0
                        cleared.add(i)
                    }
                }
            }
            fillTray(forceEasyStart = true)
        }
        return cleared
    }

    /**
     * Лучшая позиция для фигуры (для режима «тап по фигуре»):
     * сначала максимизируем число собираемых линий, затем выбираем позицию выше.
     */
    fun bestPlacement(trayIndex: Int): Pair<Int, Int>? {
        val shape = traySlots.getOrNull(trayIndex) ?: return null
        var best: Pair<Int, Int>? = null
        var bestScore = Int.MIN_VALUE
        for (r in 0..(GRID_SIZE - shape.height)) {
            for (c in 0..(GRID_SIZE - shape.width)) {
                if (!canPlace(trayIndex, r, c)) continue
                val candidate = countLinesIfPlaced(trayIndex, r, c) * 100 - r
                if (candidate > bestScore) {
                    bestScore = candidate
                    best = r to c
                }
            }
        }
        return best
    }

    /** Сколько линий соберётся, если поставить фигуру в указанную позицию. */
    fun countLinesIfPlaced(trayIndex: Int, row: Int, col: Int): Int {
        val shape = traySlots.getOrNull(trayIndex) ?: return 0
        if (!canPlace(trayIndex, row, col)) return 0
        var result = 0
        for (r in 0 until shape.height) {
            val gridRow = row + r
            var full = true
            for (c in 0 until GRID_SIZE) {
                val filled = cells[index(gridRow, c)] != 0 ||
                    (c in col until (col + shape.width) && shape.isFilled(r, c - col))
                if (!filled) {
                    full = false
                    break
                }
            }
            if (full) result++
        }
        for (c in 0 until shape.width) {
            val gridCol = col + c
            var full = true
            for (r in 0 until GRID_SIZE) {
                val filled = cells[index(r, gridCol)] != 0 ||
                    (r in row until (row + shape.height) && shape.isFilled(r - row, c))
                if (!filled) {
                    full = false
                    break
                }
            }
            if (full) result++
        }
        return result
    }

    /** Очки за одновременную очистку n линий: 1→10, 2→30, 3→60, 4→100, 5→150. */
    fun linesScore(n: Int): Int = if (n <= 0) 0 else n * 10 + (n - 1) * n * 5

    /** Монеты за партию: зависит от очков и линий, минимум 1. */
    fun coinsEarned(): Int = (score / 100 + lines / 2).coerceAtLeast(1)

    /* ----------------------------- генерация лотка ----------------------------- */

    private fun fillTray(forceEasyStart: Boolean) {
        val generated = generateTray(forceEasyStart)
        generated.forEachIndexed { i, shape ->
            traySlots[i] = shape
            trayColorSlots[i] = 1 + random.nextInt(COLOR_COUNT)
        }
    }

    /**
     * Три фигуры. Гарантируем, что хотя бы одна помещается на поле —
     * иначе игрок получил бы «нечестный» проигрыш сразу после выдачи лотка.
     */
    private fun generateTray(forceEasyStart: Boolean): List<Shape> {
        val fillRatio = filledCount().toFloat() / cells.size
        repeat(MAX_GENERATION_ATTEMPTS) {
            val candidate = List(TRAY_SIZE) { pickShape(fillRatio, forceEasyStart) }
            // Лоток принимаем только если хотя бы одна фигура реально помещается.
            if (candidate.any { fitsAnywhere(it) }) return candidate
        }
        // Страховка: всегда можно поставить одиночный блок.
        return List(TRAY_SIZE) { Shapes.single }
    }

    private fun fitsAnywhere(shape: Shape): Boolean {
        if (shape.height > GRID_SIZE || shape.width > GRID_SIZE) return false
        for (r in 0..(GRID_SIZE - shape.height)) {
            for (c in 0..(GRID_SIZE - shape.width)) {
                var fits = true
                loop@ for (sr in 0 until shape.height) {
                    for (sc in 0 until shape.width) {
                        if (shape.isFilled(sr, sc) && cells[index(r + sr, c + sc)] != 0) {
                            fits = false
                            break@loop
                        }
                    }
                }
                if (fits) return true
            }
        }
        return false
    }

    private fun pickShape(fillRatio: Float, easy: Boolean): Shape {
        if (easy) return Shapes.easySet[random.nextInt(Shapes.easySet.size)]
        val maxCells = when {
            fillRatio > 0.78f -> 3
            fillRatio > 0.65f -> 4
            fillRatio > 0.5f -> 5
            else -> 9
        }
        val pool = Shapes.all.filter { it.cellCount <= maxCells }
        val safePool = pool.ifEmpty { listOf(Shapes.single) }
        val totalWeight = safePool.sumOf { it.weight }
        var roll = random.nextInt(totalWeight)
        for (shape in safePool) {
            roll -= shape.weight
            if (roll < 0) return shape
        }
        return safePool.last()
    }

    /* --------------------------- хуки для тестов --------------------------- */

    /** Только для unit-тестов: подставить нужный лоток. */
    internal fun setTrayForTest(shapes: List<Shape?>) {
        for (i in 0 until TRAY_SIZE) {
            traySlots[i] = shapes.getOrNull(i)
            trayColorSlots[i] = if (shapes.getOrNull(i) != null) 1 else 0
        }
    }

    /** Только для unit-тестов: задать состояние поля. */
    internal fun setCellsForTest(values: IntArray) {
        values.copyInto(cells)
    }

    companion object {
        const val COLOR_COUNT = 7
        const val MAX_COMBO_MULTIPLIER = 8
        private const val MAX_GENERATION_ATTEMPTS = 40
    }
}
