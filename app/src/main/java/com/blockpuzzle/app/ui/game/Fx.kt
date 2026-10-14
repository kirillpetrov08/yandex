package com.blockpuzzle.app.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Всплывающая цифра очков над клеткой. */
data class ScorePop(
    val id: Long,
    val position: Offset,
    val text: String,
    val color: Color,
    var progress: Float = 0f,
)

/** Частица для «взрыва» при очистке линий. */
data class Particle(
    val id: Long,
    var position: Offset,
    var velocity: Offset,
    val color: Color,
    val size: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var progress: Float = 0f,
)

/** Клетка, которая схлопывается (постановка фигуры или очистка). */
data class CellPop(
    val cellIndex: Int,
    var progress: Float = 0f,
    val isClear: Boolean = false,
)

/** Состояние перетаскивания фигуры из лотка. */
data class DragState(
    val trayIndex: Int,
    val pointer: Offset,
    val grabbedAt: Offset,
    val shapeWidth: Int,
    val shapeHeight: Int,
)

/**
 * Вся «сочность» игры: частицы, всплывающие очки, тряска экрана.
 * Обновляется одним кадровым циклом (см. GameScreen) — без лишних корутин.
 */
class FxController {

    val pops = mutableStateListOf<ScorePop>()
    val particles = mutableStateListOf<Particle>()
    val cellPops = mutableStateListOf<CellPop>()

    var shake by mutableFloatStateOf(0f)

    private var idCounter = 0L

    fun addCellPops(indices: List<Int>, isClear: Boolean = false) {
        indices.forEach { index -> cellPops.add(CellPop(cellIndex = index, isClear = isClear)) }
    }

    fun addScorePop(position: Offset, text: String, color: Color) {
        pops.add(ScorePop(id = idCounter++, position = position, text = text, color = color))
    }

    fun burst(center: Offset, color: Color, amount: Int = 18, power: Float = 1f) {
        repeat(amount) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = (140f + Random.nextFloat() * 420f) * power
            particles.add(
                Particle(
                    id = idCounter++,
                    position = center,
                    velocity = Offset(cos(angle) * speed, sin(angle) * speed - 60f),
                    color = color,
                    size = 6f + Random.nextFloat() * 10f,
                    rotation = Random.nextFloat() * 360f,
                    rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                ),
            )
        }
    }

    fun shake(power: Float) {
        shake = maxOf(shake, power)
    }

    /** Один кадр анимации: [dtSeconds] — длительность кадра. */
    fun step(dtSeconds: Float, gravity: Float = 1400f) {
        var i = 0
        while (i < pops.size) {
            val pop = pops[i]
            pop.progress += dtSeconds * 1.6f
            if (pop.progress >= 1f) pops.removeAt(i) else i++
        }

        i = 0
        while (i < particles.size) {
            val p = particles[i]
            p.progress += dtSeconds * 1.4f
            p.position = Offset(
                p.position.x + p.velocity.x * dtSeconds,
                p.position.y + p.velocity.y * dtSeconds,
            )
            p.velocity = Offset(p.velocity.x * 0.985f, p.velocity.y + gravity * dtSeconds)
            p.rotation += p.rotationSpeed * dtSeconds
            if (p.progress >= 1f) particles.removeAt(i) else i++
        }

        i = 0
        while (i < cellPops.size) {
            val pop = cellPops[i]
            pop.progress += dtSeconds * 5f
            if (pop.progress >= 1f) cellPops.removeAt(i) else i++
        }

        if (shake > 0f) shake = (shake - dtSeconds * 6f).coerceAtLeast(0f)
    }

    fun clearAll() {
        pops.clear()
        particles.clear()
        cellPops.clear()
        shake = 0f
    }
}
