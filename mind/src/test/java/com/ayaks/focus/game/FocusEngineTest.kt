package com.ayaks.focus.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusEngineTest {
    @Test
    fun `memory sequence becomes longer and completes after six rounds`() {
        val engine = FocusEngine(23)
        var session = engine.start(FocusMode.MEMORY)
        assertEquals(3, session.sequence.size)
        repeat(FocusEngine.MEMORY_ROUNDS) { round ->
            if (session.phase == GamePhase.SHOWING) session = engine.memoryInputReady(session)
            val expectedLength = (3 + round).coerceAtMost(8)
            assertEquals(expectedLength, session.sequence.size)
            session.sequence.forEach { cell ->
                val result = engine.tapMemory(session, cell)
                assertTrue(result.correct)
                session = result.session
            }
        }
        assertEquals(GamePhase.FINISHED, session.phase)
        assertEquals(FocusEngine.MEMORY_ROUNDS, session.completedRounds)
        assertTrue(session.score > 0)
    }

    @Test
    fun `wrong memory answer loses one life without adding score`() {
        val engine = FocusEngine(1)
        var session = engine.start(FocusMode.MEMORY)
        session = engine.memoryInputReady(session)
        val wrong = (0 until FocusEngine.MEMORY_GRID_CELLS).first { it != session.sequence.first() }
        val result = engine.tapMemory(session, wrong)
        assertFalse(result.correct)
        assertEquals(2, result.session.lives)
        assertEquals(0, result.session.score)
        assertEquals(GamePhase.FEEDBACK, result.session.phase)
    }

    @Test
    fun `search round has exactly one target and wrong tap preserves the round`() {
        val engine = FocusEngine(7)
        var session = engine.start(FocusMode.SEARCH)
        repeat(FocusEngine.SEARCH_ROUNDS) { round ->
            assertEquals(if (session.round < 5) 16 else 25, session.searchTiles.size)
            assertEquals(1, session.searchTiles.count { it == session.searchTarget })
            if (round == 0) {
                val wrongCell = session.searchTiles.indexOfFirst { it != session.searchTarget }
                val wrong = engine.tapSearch(session, wrongCell)
                assertFalse(wrong.correct)
                assertEquals(GamePhase.FEEDBACK, wrong.session.phase)
                assertEquals(session.completedRounds, wrong.session.completedRounds)
                session = engine.resumeSearch(wrong.session)
            }
            val targetCell = session.searchTiles.indexOf(session.searchTarget)
            session = engine.tapSearch(session, targetCell).session
            if (round < FocusEngine.SEARCH_ROUNDS - 1) assertEquals(GamePhase.INPUT, session.phase)
        }
        assertEquals(GamePhase.FINISHED, session.phase)
        assertEquals(FocusEngine.SEARCH_ROUNDS, session.completedRounds)
    }

    @Test
    fun `color mode always asks for a different ink color and counts correct choices`() {
        val engine = FocusEngine(13)
        var session = engine.start(FocusMode.COLORS)
        repeat(FocusEngine.COLOR_ROUNDS) { round ->
            assertTrue(session.wordColor in FocusEngine.COLOR_NAMES.indices)
            assertTrue(session.inkColor in FocusEngine.COLOR_NAMES.indices)
            assertTrue(session.wordColor != session.inkColor)
            session = engine.answerColor(session, session.inkColor).session
            if (round < FocusEngine.COLOR_ROUNDS - 1) assertEquals(GamePhase.INPUT, session.phase)
        }
        assertEquals(GamePhase.FINISHED, session.phase)
        assertEquals(FocusEngine.COLOR_ROUNDS, session.completedRounds)
        assertEquals(210, session.score)
    }

    @Test
    fun `timed exercises stop exactly at zero`() {
        val engine = FocusEngine(5)
        var session = engine.start(FocusMode.SEARCH)
        repeat(44) { session = engine.tick(session) }
        assertEquals(1, session.secondsLeft)
        session = engine.tick(session)
        assertEquals(0, session.secondsLeft)
        assertEquals(GamePhase.FINISHED, session.phase)
    }
}
