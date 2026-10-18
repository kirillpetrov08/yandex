package com.blockpuzzle.app.ui.game

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.blockpuzzle.app.game.GRID_SIZE
import com.blockpuzzle.app.game.Shape
import com.blockpuzzle.app.ui.theme.BoardTheme
import kotlin.math.min

/** Рисует «объёмный» блок со светлой верхней гранью — визуальная фишка игры. */
fun DrawScope.drawBlock(center: Offset, sizePx: Float, color: Color, cornerPx: Float) {
    val radius = CornerRadius(cornerPx, cornerPx)
    // A restrained colored halo makes adjacent colors feel luminous without a bitmap blur.
    drawCircle(color = color.copy(alpha = 0.075f), radius = sizePx * 0.66f, center = center)
    val topLeft = Offset(center.x - sizePx / 2f, center.y - sizePx / 2f)
    // Мягкая тень, нижний кант и верхний блик дают объём без bitmap-ресурсов.
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.22f),
        topLeft = Offset(topLeft.x, topLeft.y + sizePx * 0.075f),
        size = Size(sizePx, sizePx),
        cornerRadius = radius,
    )
    drawRoundRect(
        color = color.copy(alpha = 0.72f),
        topLeft = Offset(topLeft.x, topLeft.y + sizePx * 0.025f),
        size = Size(sizePx, sizePx * 0.975f),
        cornerRadius = radius,
    )
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = Size(sizePx, sizePx * 0.91f),
        cornerRadius = radius,
    )
    // Глянцевая грань и тонкий внутренний блик.
    drawRoundRect(
        color = Color.White.copy(alpha = 0.27f),
        topLeft = Offset(topLeft.x + sizePx * 0.12f, topLeft.y + sizePx * 0.09f),
        size = Size(sizePx * 0.76f, sizePx * 0.18f),
        cornerRadius = CornerRadius(sizePx * 0.12f, sizePx * 0.12f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset(topLeft.x + sizePx * 0.10f, topLeft.y + sizePx * 0.08f),
        size = Size(sizePx * 0.80f, sizePx * 0.80f),
        cornerRadius = radius,
        style = Stroke(width = (sizePx * 0.018f).coerceAtLeast(1f)),
    )
}

/**
 * Игровое поле 8×8. Одна Canvas-отрисовка на весь кадр: быстро даже на слабых устройствах.
 * [ghostCells] — клетки, куда встанет фигура; [validGhost] — можно ли туда поставить.
 */
@Composable
fun BoardCanvas(
    cells: List<Int>,
    theme: BoardTheme,
    ghostCells: List<Int>,
    validGhost: Boolean,
    fx: FxController,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        // Observe the animation clock: Fx values are mutable model objects, so the clock
        // explicitly invalidates this Canvas while any pop, burst, or score label is alive.
        @Suppress("UNUSED_VARIABLE")
        val observedAnimationFrame = fx.animationFrame
        val cell = size.width / GRID_SIZE
        val gap = cell * 0.07f
        val block = cell - gap

        drawRoundRect(
            color = theme.boardBackground,
            cornerRadius = CornerRadius(cell * 0.5f, cell * 0.5f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.035f),
            cornerRadius = CornerRadius(cell * 0.5f, cell * 0.5f),
            style = Stroke(width = (cell * 0.035f).coerceAtLeast(1f)),
        )

        val emptyColor = theme.boardCellBackground
        for (index in 0 until GRID_SIZE * GRID_SIZE) {
            val row = index / GRID_SIZE
            val col = index % GRID_SIZE
            val center = Offset(col * cell + cell / 2f, row * cell + cell / 2f)
            val value = cells.getOrElse(index) { 0 }
            if (value == 0) {
                drawRoundRect(
                    color = emptyColor,
                    topLeft = Offset(center.x - block / 2f, center.y - block / 2f),
                    size = Size(block, block),
                    cornerRadius = CornerRadius(gap * 1.6f, gap * 1.6f),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.025f),
                    topLeft = Offset(center.x - block / 2f, center.y - block / 2f),
                    size = Size(block, block),
                    cornerRadius = CornerRadius(gap * 1.6f, gap * 1.6f),
                    style = Stroke(width = (cell * 0.018f).coerceAtLeast(1f)),
                )
            } else {
                val pop = fx.cellPops.firstOrNull { it.cellIndex == index && !it.isClear }
                val progress = pop?.progress?.coerceIn(0f, 1f) ?: 1f
                val punch = if (pop == null) 0f else kotlin.math.sin(progress * Math.PI).toFloat() * 0.14f
                val scale = if (pop == null) 1f else 0.78f + 0.22f * progress + punch
                drawBlock(center, block * scale, theme.colorFor(value), gap * 1.6f)
            }
        }

        // Подсветка места, куда встанет фигура
        if (ghostCells.isNotEmpty()) {
            val ghostColor = if (validGhost) theme.accent.copy(alpha = 0.45f) else Color(0xFFFF5252).copy(alpha = 0.35f)
            ghostCells.forEach { index ->
                if (index in 0 until GRID_SIZE * GRID_SIZE) {
                    val row = index / GRID_SIZE
                    val col = index % GRID_SIZE
                    val topLeft = Offset(col * cell + gap / 2f, row * cell + gap / 2f)
                    drawRoundRect(
                        color = ghostColor,
                        topLeft = topLeft,
                        size = Size(block, block),
                        cornerRadius = CornerRadius(gap * 1.6f, gap * 1.6f),
                        style = Stroke(width = cell * 0.06f),
                    )
                    drawRoundRect(
                        color = ghostColor.copy(alpha = 0.18f),
                        topLeft = topLeft,
                        size = Size(block, block),
                        cornerRadius = CornerRadius(gap * 1.6f, gap * 1.6f),
                    )
                }
            }
        }

        // Вспышка при очистке линий
        fx.cellPops.filter { it.isClear }.forEach { pop ->
            val index = pop.cellIndex
            if (index in 0 until GRID_SIZE * GRID_SIZE) {
                val row = index / GRID_SIZE
                val col = index % GRID_SIZE
                val center = Offset(col * cell + cell / 2f, row * cell + cell / 2f)
                val progress = min(1f, pop.progress)
                val scale = 1f + 0.35f * progress
                val ringRadius = block * (0.28f + progress * 0.9f)
                drawCircle(
                    color = theme.accent.copy(alpha = 0.72f * (1f - progress)),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = (cell * 0.055f * (1f - progress * 0.45f)).coerceAtLeast(1f)),
                )
                drawBlock(center, block * scale, Color.White.copy(alpha = 0.75f * (1f - progress)), gap * 1.6f)
            }
        }

        // Частицы
        fx.particles.forEach { particle ->
            val alpha = (1f - particle.progress).coerceIn(0f, 1f)
            rotate(degrees = particle.rotation, pivot = particle.position) {
                drawRoundRect(
                    color = particle.color.copy(alpha = alpha),
                    topLeft = Offset(particle.position.x - particle.size / 2f, particle.position.y - particle.size / 2f),
                    size = Size(particle.size, particle.size),
                    cornerRadius = CornerRadius(particle.size * 0.3f, particle.size * 0.3f),
                )
            }
        }

        // Всплывающие очки (рисуем системным Paint — текст в Canvas)
        if (fx.pops.isNotEmpty()) {
            val paint = Paint().apply {
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            fx.pops.forEach { pop ->
                val alpha = (1f - pop.progress).coerceIn(0f, 1f)
                paint.textSize = cell * 0.55f
                paint.color = pop.color.toArgb()
                paint.alpha = (alpha * 255).toInt()
                drawContext.canvas.nativeCanvas.drawText(
                    pop.text,
                    pop.position.x,
                    pop.position.y - pop.progress * cell * 1.2f,
                    paint,
                )
            }
        }
    }
}

/** Лоток с тремя фигурами. Каждую можно тащить или просто ткнуть (авторазмещение). */
@Composable
fun TrayCanvas(
    shape: Shape?,
    color: Color,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    emptyPercent: Float = 0f,
) {
    Canvas(modifier = modifier) {
        if (shape == null) return@Canvas
        // Появление новой фигуры: лёгкий «выезд» снизу.
        val alpha = 1f - emptyPercent * 0.35f
        val offsetY = emptyPercent * cellSizePx * 0.6f
        val widthPx = shape.width * cellSizePx
        val heightPx = shape.height * cellSizePx
        val originX = (size.width - widthPx) / 2f
        val originY = (size.height - heightPx) / 2f + offsetY
        for (row in 0 until shape.height) {
            for (col in 0 until shape.width) {
                if (!shape.isFilled(row, col)) continue
                val center = Offset(
                    originX + col * cellSizePx + cellSizePx / 2f,
                    originY + row * cellSizePx + cellSizePx / 2f,
                )
                drawBlock(
                    center = center,
                    sizePx = cellSizePx * 0.88f,
                    color = if (highlighted) color.copy(alpha = alpha.coerceIn(0f, 1f)) else color.copy(alpha = 0.85f),
                    cornerPx = cellSizePx * 0.22f,
                )
            }
        }
    }
}

/** Рамка слота лотка. */
@Composable
fun TraySlotBackground(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
        )
    }
}
