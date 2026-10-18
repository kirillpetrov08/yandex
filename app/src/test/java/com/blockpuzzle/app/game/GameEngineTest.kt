package com.blockpuzzle.app.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun engine(seed: Int = 7) = GameEngine(Random(seed))

    @Test
    fun `new game starts empty with a playable tray`() {
        val game = engine()
        game.newGame()
        assertEquals(0, game.score)
        assertEquals(0, game.filledCount())
        assertEquals(TRAY_SIZE, game.tray.count { it != null })
        assertTrue("хотя бы одна фигура должна помещаться", game.hasAnyMove())
        assertFalse(game.isGameOver)
    }

    @Test
    fun `opening trays stay beginner friendly for first nine placements`() {
        val game = engine(seed = 41)
        game.newGame()
        repeat(3) { batch ->
            game.tray.forEachIndexed { slot, shape ->
                if (shape == null) return@forEachIndexed
                assertTrue("opening piece should be from the gentle pool", shape in Shapes.easySet)
                var placed = false
                for (row in 0 until GRID_SIZE) {
                    for (col in 0 until GRID_SIZE) {
                        if (!placed && game.canPlace(slot, row, col)) {
                            assertNotNull(game.place(slot, row, col))
                            placed = true
                        }
                    }
                }
                assertTrue("generated opening piece must be placeable", placed)
            }
            if (batch < 2) {
                assertTrue("next opening tray should remain gentle", game.tray.filterNotNull().all { it in Shapes.easySet })
            }
        }
        assertEquals(9, game.piecesPlaced)
    }

    @Test
    fun `undo restores board score and piece tray after a move`() {
        val game = engine(seed = 17)
        game.newGame()
        game.setTrayForTest(listOf(Shapes.single, null, null))
        val beforeTray = game.tray
        val result = game.place(0, 2, 3)
        assertNotNull(result)
        assertEquals(1, game.score)
        assertTrue(game.canUndoLastMove)

        assertTrue(game.undoLastMove())
        assertEquals(0, game.score)
        assertEquals(0, game.filledCount())
        assertEquals(beforeTray, game.tray)
        assertFalse(game.canUndoLastMove)
        assertFalse(game.undoLastMove())
    }

    @Test
    fun `placement writes cells and consumes tray slot`() {
        val game = engine()
        game.newGame()
        game.setTrayForTest(listOf(Shapes.single, Shapes.single, null))
        val result = game.place(0, 3, 4)
        assertNotNull(result)
        assertEquals(1, result!!.placedCellIndices.size)
        assertEquals(1, game.cellValue(game.index(3, 4)))
        assertNull(game.tray[0])
        assertEquals(1, game.tray.count { it != null })
        assertEquals(1, game.score)
    }

    @Test
    fun `placement outside the board or on filled cells is rejected`() {
        val game = engine()
        game.newGame()
        game.setTrayForTest(listOf(Shapes.line5H, null, null))
        assertNull(game.place(0, 0, 4)) // 5 клеток не влезают в 8-колоночный ряд с позиции 4
        assertFalse(game.canPlace(0, 0, 4))
        assertTrue(game.canPlace(0, 0, 3))

        game.setCellsForTest(IntArray(GRID_SIZE * GRID_SIZE) { 1 })
        assertFalse(game.canPlace(0, 0, 0))
    }

    @Test
    fun `full row is cleared and scored`() {
        val game = engine()
        game.newGame()
        val cells = IntArray(GRID_SIZE * GRID_SIZE)
        for (col in 0 until GRID_SIZE - 1) cells[game.index(0, col)] = 3
        game.setCellsForTest(cells)
        game.setTrayForTest(listOf(Shapes.single, null, null))

        val result = game.place(0, 0, GRID_SIZE - 1)!!

        assertEquals(1, result.clearedLines)
        assertEquals(GRID_SIZE, result.clearedCellIndices.size)
        assertEquals(0, game.filledCount())
        assertEquals(1, game.lines)
        // 1 клетка поставлена + 10 очков за линию
        assertEquals(11, game.score)
    }

    @Test
    fun `clearing several lines at once gives combo multiplier`() {
        val game = engine()
        game.newGame()
        // Заполнены 7 столбцов из 8 во всех рядах: не хватает только правого столбца
        val cells = IntArray(GRID_SIZE * GRID_SIZE)
        for (row in 0 until 3) {
            for (col in 0 until GRID_SIZE - 1) cells[game.index(row, col)] = 2
        }
        game.setCellsForTest(cells)
        game.setTrayForTest(listOf(Shapes.line3V, null, null))
        val result = game.place(0, 0, GRID_SIZE - 1)!! // вертикальная «3» в правый столбец

        // Она замыкает три ряда: 3 линии за один ход, комбо = 1, множитель = 1
        assertEquals(3, result.clearedLines)
        assertEquals(3, game.lines)
        assertEquals(1, game.combo)
        assertEquals(1, result.comboMultiplier)
        assertEquals(3 + game.linesScore(3), game.score)
    }

    @Test
    fun `combo grows on consecutive clears and resets otherwise`() {
        val game = engine()
        game.newGame()

        fun prepareRow(row: Int) {
            val cells = IntArray(GRID_SIZE * GRID_SIZE)
            for (col in 0 until GRID_SIZE - 1) cells[game.index(row, col)] = 1
            game.setCellsForTest(cells)
            game.setTrayForTest(listOf(Shapes.single, null, null))
        }

        prepareRow(0)
        game.place(0, 0, GRID_SIZE - 1)
        assertEquals(1, game.combo)

        prepareRow(1)
        game.place(0, 1, GRID_SIZE - 1)
        assertEquals(2, game.combo)
        assertEquals(2, game.bestCombo)

        // Ход без очистки линии сбрасывает комбо
        game.setTrayForTest(listOf(Shapes.single, null, null))
        game.place(0, 7, 7)
        assertEquals(0, game.combo)
    }

    @Test
    fun `game over when no piece fits`() {
        val game = engine()
        game.newGame()
        game.setCellsForTest(IntArray(GRID_SIZE * GRID_SIZE) { 1 })
        game.setTrayForTest(listOf(Shapes.single, null, null))
        assertFalse(game.hasAnyMove())
        assertTrue(game.isGameOver)
    }

    @Test
    fun `continue power-up clears bottom rows and keeps playing`() {
        val game = engine()
        game.newGame()
        game.setCellsForTest(IntArray(GRID_SIZE * GRID_SIZE) { 1 })
        game.setTrayForTest(listOf(Shapes.square3, null, null))
        assertFalse(game.hasAnyMove())
        assertTrue(game.isGameOver)

        val cleared = game.continueGame()

        assertTrue(cleared.isNotEmpty())
        assertFalse(game.isGameOver)
        assertEquals(1, game.continuesUsed)
        assertTrue(game.hasAnyMove())
        // Два нижних ряда освобождены
        assertEquals(0, game.cellValue(game.index(7, 7)))
        assertEquals(0, game.cellValue(game.index(6, 0)))
    }

    @Test
    fun `best placement prefers line clear`() {
        val game = engine()
        game.newGame()
        val cells = IntArray(GRID_SIZE * GRID_SIZE)
        for (col in 0 until GRID_SIZE - 1) cells[game.index(2, col)] = 5
        game.setCellsForTest(cells)
        game.setTrayForTest(listOf(Shapes.single, null, null))

        val best = game.bestPlacement(0)
        assertNotNull(best)
        assertEquals(2, best!!.first)
        assertEquals(GRID_SIZE - 1, best.second)
    }

    @Test
    fun `coins earned is at least one`() {
        val game = engine()
        game.newGame()
        assertTrue(game.coinsEarned() >= 1)
    }

    @Test
    fun `tray is refilled only when all three pieces are used`() {
        val game = engine()
        game.newGame()
        val firstTray = game.tray.toList()
        assertTrue(firstTray.isNotEmpty())

        // Оставляем в лотке ещё одну фигуру — перезаполнения быть не должно
        game.setTrayForTest(listOf(Shapes.single, Shapes.single, null))
        game.place(0, 0, 0)
        assertEquals(1, game.tray.count { it != null })
        assertEquals(1, game.tray[1]?.cellCount ?: 0)
    }

    @Test
    fun `generated tray always contains a fitting piece`() {
        repeat(200) { seed ->
            val game = GameEngine(Random(seed))
            game.newGame()
            // Заполняем поле случайно, но оставляем случайные «дыры»
            val cells = IntArray(GRID_SIZE * GRID_SIZE)
            for (i in cells.indices) if (Random(seed + i).nextInt(100) < 70) cells[i] = 1
            game.setCellsForTest(cells)
            game.setTrayForTest(emptyList())
            game.continueGame()
            assertTrue("лоток должен быть играбельным (seed=$seed)", game.hasAnyMove())
        }
    }
}
