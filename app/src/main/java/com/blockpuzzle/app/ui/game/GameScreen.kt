package com.blockpuzzle.app.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.blockpuzzle.app.R
import com.blockpuzzle.app.game.GRID_SIZE
import com.blockpuzzle.app.game.TRAY_SIZE
import com.blockpuzzle.app.ui.components.CoinBadge
import com.blockpuzzle.app.ui.components.GameDialog
import com.blockpuzzle.app.ui.components.PrimaryButton
import com.blockpuzzle.app.ui.components.RewardedButton
import com.blockpuzzle.app.ui.components.SecondaryButton
import com.blockpuzzle.app.ui.theme.BoardTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Экран партии: поле, лоток, перетаскивание, комбо-эффекты и окна результата.
 * Рекламные действия приходят снаружи (AppRoot), чтобы экран не зависел от Activity.
 */
@Composable
fun GameScreen(
    controller: GameController,
    theme: BoardTheme,
    coins: Int,
    best: Int,
    rewardedReady: Boolean,
    onExitToMenu: () -> Unit,
    onPlayAgain: () -> Unit,
    onContinueRewardClick: () -> Unit,
    onDoubleCoinsRewardClick: () -> Unit,
    onGameFinished: () -> Unit = {},
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // Рекорд на момент начала партии: prefs обновляются только при сохранении результата.
    val bestAtStart = remember { best }
    val traySpacingPx = with(density) { TRAY_SPACING.toPx() }

    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var boardSizePx by remember { mutableStateOf(0f) }
    var traySlotWidthPx by remember { mutableStateOf(0f) }
    var trayOrigin by remember { mutableStateOf(Offset.Zero) }
    var trayHeightPx by remember { mutableStateOf(0f) }
    var drag by remember { mutableStateOf<DragState?>(null) }
    var ghostCells by remember { mutableStateOf<List<Int>>(emptyList()) }
    var ghostValid by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }

    // Системная кнопка «назад» не выкидывает из партии молча, а спрашивает подтверждение.
    androidx.activity.compose.BackHandler { showQuitDialog = true }

    val cellSizePx = if (boardSizePx > 0f) boardSizePx / GRID_SIZE else 0f
    val trayCellPx = if (traySlotWidthPx > 0f) minOf(traySlotWidthPx / 5.2f, trayHeightPx / 5.2f) else 0f

    // Один кадровый цикл на все анимации: дёшево и предсказуемо по нагрузке.
    LaunchedEffect(controller.fx.hasActiveEffects) {
        if (!controller.fx.hasActiveEffects) return@LaunchedEffect
        var previous = 0L
        while (controller.fx.hasActiveEffects) {
            withFrameNanos { now ->
                if (previous != 0L) {
                    val dt = ((now - previous) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
                    controller.fx.step(dt)
                }
                previous = now
            }
        }
    }

    // Эффекты на ход: вспышки, частицы, всплывающие очки.
    LaunchedEffect(controller.moveCounter) {
        val move = controller.lastMove ?: return@LaunchedEffect
        controller.fx.addCellPops(move.placedCellIndices)
        if (move.clearedLines > 0 && cellSizePx > 0f) {
            val clearedByRow = move.clearedCellIndices
            clearedByRow.chunked(GRID_SIZE).forEach { chunk ->
                val centerX = chunk.sumOf { (it % GRID_SIZE) * cellSizePx.toDouble() }.toFloat() / chunk.size + cellSizePx / 2f
                val centerY = chunk.sumOf { (it / GRID_SIZE) * cellSizePx.toDouble() }.toFloat() / chunk.size + cellSizePx / 2f
                controller.fx.burst(Offset(centerX, centerY), theme.accent, amount = 14, power = 1.1f)
            }
            val gained = move.gainedScore
            val firstCleared = move.clearedCellIndices.firstOrNull() ?: 0
            controller.fx.addScorePop(
                position = Offset(
                    (firstCleared % GRID_SIZE) * cellSizePx + cellSizePx / 2f,
                    (firstCleared / GRID_SIZE) * cellSizePx,
                ),
                text = "+$gained",
                color = theme.accent,
            )
            if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (hapticsEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        controller.consumeLastMove()
    }

    // Партия закончилась — сообщаем частотной политике рекламы (учитываем «каждый N-й проигрыш»).
    LaunchedEffect(controller.gameOver) {
        if (controller.gameOver) onGameFinished()
    }

    // Страховка: результат сохраняется при уходе с экрана (навигация/закрытие приложения).
    DisposableEffect(controller) {
        onDispose { controller.saveResult() }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(theme.screenBackground))
            .onGloballyPositioned { rootOrigin = it.localToRoot(Offset.Zero) }
            .pointerInput(cellSizePx, trayCellPx, boardOrigin) {
                if (cellSizePx <= 0f || trayCellPx <= 0f) return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        // Старт перетаскивания возможен только из лотка.
                        val local = start
                        val slot = slotAt(local, trayOrigin - rootOrigin, traySlotWidthPx, trayHeightPx)
                        if (slot == null) return@detectDragGestures
                        val shape = controller.tray.getOrNull(slot) ?: return@detectDragGestures
                        val slotLeft = trayOrigin.x - rootOrigin.x + slot * (traySlotWidthPx + traySpacingPx)
                        drag = DragState(
                            trayIndex = slot,
                            pointer = local,
                            grabbedAt = Offset((local.x - slotLeft).coerceIn(0f, traySlotWidthPx), local.y - (trayOrigin.y - rootOrigin.y)),
                            shapeWidth = shape.width,
                            shapeHeight = shape.height,
                        )
                        controller.playPickup()
                        ghostCells = emptyList()
                        ghostValid = false
                    },
                    onDrag = { change, delta ->
                        val current = drag ?: return@detectDragGestures
                        change.consume()
                        val moved = current.copy(pointer = current.pointer + delta)
                        drag = moved
                        val target = targetCell(moved, boardOrigin - rootOrigin, cellSizePx)
                        if (target != null) {
                            val snappedTarget = controller.engine.nearestLegalPlacement(
                                moved.trayIndex,
                                target.first,
                                target.second,
                                maxDistance = 1,
                            )
                            val previewTarget = snappedTarget ?: target
                            ghostCells = controller.engine.previewCells(moved.trayIndex, previewTarget.first, previewTarget.second)
                            ghostValid = snappedTarget != null
                        } else {
                            ghostCells = emptyList()
                            ghostValid = false
                        }
                    },
                    onDragEnd = {
                        val current = drag
                        drag = null
                        ghostCells = emptyList()
                        ghostValid = false
                        if (current != null) {
                            val target = targetCell(current, boardOrigin - rootOrigin, cellSizePx)
                            val snappedTarget = target?.let {
                                controller.engine.nearestLegalPlacement(current.trayIndex, it.first, it.second, maxDistance = 1)
                            }
                            if (snappedTarget != null && controller.place(current.trayIndex, snappedTarget.first, snappedTarget.second)) {
                                if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    },
                    onDragCancel = {
                        drag = null
                        ghostCells = emptyList()
                        ghostValid = false
                    },
                )
            },
    ) {
        val boardSide = minOf(maxWidth, maxHeight * 0.58f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameHud(
                score = controller.score,
                best = maxOf(best, controller.score),
                coins = coins,
                lines = controller.lines,
                canUndo = controller.canUndo,
                onUndo = { controller.undoLastMove() },
                onPause = { showQuitDialog = true },
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(boardSide)
                    .graphicsLayer {
                        // Тряска при очистке линий
                        translationX = (controller.fx.shake * 12f) * (if (controller.fx.particles.size % 2 == 0) 1f else -1f)
                        translationY = (controller.fx.shake * 6f) * (if (controller.fx.pops.size % 2 == 0) -1f else 1f)
                    }
                    .onGloballyPositioned { coordinates: LayoutCoordinates ->
                        boardOrigin = coordinates.localToRoot(Offset.Zero)
                        boardSizePx = coordinates.size.width.toFloat()
                    },
            ) {
                BoardCanvas(
                    cells = controller.cells,
                    theme = theme,
                    ghostCells = ghostCells,
                    validGhost = ghostValid,
                    fx = controller.fx,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Комбо-баннер
            AnimatedVisibility(
                visible = controller.comboFlash != null,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                val flash = controller.comboFlash
                if (flash != null) {
                    ComboBanner(flash = flash, theme = theme)
                    LaunchedEffect(flash) {
                        kotlinx.coroutines.delay(900)
                        controller.clearComboFlash()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.game_tray_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .onGloballyPositioned {
                        trayOrigin = it.localToRoot(Offset.Zero)
                        trayHeightPx = it.size.height.toFloat()
                        val total = it.size.width - traySpacingPx * (TRAY_SIZE - 1)
                        traySlotWidthPx = (total / TRAY_SIZE).coerceAtLeast(1f)
                    },
                horizontalArrangement = Arrangement.spacedBy(TRAY_SPACING),
            ) {
                for (slot in 0 until TRAY_SIZE) {
                    val shape = controller.tray.getOrNull(slot)
                    val color = theme.colorFor(controller.trayColors.getOrElse(slot) { 0 })
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(theme.boardCellBackground.copy(alpha = 0.55f))
                            .pointerInput(slot, shape) {
                                if (shape == null) return@pointerInput
                                detectTapGestures {
                                    if (controller.autoPlace(slot)) {
                                        if (hapticsEnabled) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        TrayCanvas(
                            shape = shape,
                            color = color,
                            cellSizePx = trayCellPx,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        // «Летящая» фигура под пальцем
        val dragState = drag
        if (dragState != null && cellSizePx > 0f) {
            val shape = controller.tray.getOrNull(dragState.trayIndex)
            if (shape != null) {
                val liftPx = cellSizePx * 1.35f
                val topLeft = Offset(
                    x = dragState.pointer.x - dragState.grabbedAt.x,
                    y = dragState.pointer.y - dragState.grabbedAt.y - liftPx,
                )
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            // pointerInput висит на этом же Box, поэтому координаты уже локальные.
                            translationX = topLeft.x
                            translationY = topLeft.y
                        }
                        .size(
                            width = with(density) { (shape.width * cellSizePx).toDp() },
                            height = with(density) { (shape.height * cellSizePx).toDp() },
                        ),
                ) {
                    TrayCanvas(
                        shape = shape,
                        color = theme.colorFor(controller.trayColors.getOrElse(dragState.trayIndex) { 0 }),
                        cellSizePx = cellSizePx,
                        modifier = Modifier.fillMaxSize(),
                        highlighted = true,
                    )
                }
            }
        }

        // Окно результата партии
        if (controller.gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC05070F)),
                contentAlignment = Alignment.Center,
            ) {
                GameOverDialog(
                    controller = controller,
                    theme = theme,
                    bestAtStart = bestAtStart,
                    rewardedReady = rewardedReady,
                    onContinue = onContinueRewardClick,
                    onDoubleCoins = onDoubleCoinsRewardClick,
                    onPlayAgain = {
                        controller.saveResult()
                        onPlayAgain()
                    },
                    onMenu = {
                        controller.saveResult()
                        onExitToMenu()
                    },
                )
            }
        }

        if (showQuitDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC05070F)),
                contentAlignment = Alignment.Center,
            ) {
                GameDialog(
                    title = stringResource(R.string.game_quit_title),
                    message = stringResource(R.string.game_quit_message),
                    confirmText = stringResource(R.string.game_quit_leave),
                    onConfirm = {
                        showQuitDialog = false
                        onExitToMenu()
                    },
                    dismissText = stringResource(R.string.game_quit_stay),
                    onDismiss = { showQuitDialog = false },
                )
            }
        }
    }
}

@Composable
private fun GameHud(
    score: Int,
    best: Int,
    coins: Int,
    lines: Int,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onPause: () -> Unit,
) {
    val undoLabel = stringResource(R.string.game_undo)
    val pauseLabel = stringResource(R.string.game_pause_accessibility)
    val displayedScore by animateIntAsState(targetValue = score, animationSpec = tween(260), label = "score")
    val displayedBest by animateIntAsState(targetValue = best, animationSpec = tween(320), label = "best")
    val displayedLines by animateIntAsState(targetValue = lines, animationSpec = tween(260), label = "lines")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.game_score),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = displayedScore.toString(),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.game_best),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = displayedBest.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.game_lines_cleared, displayedLines),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            CoinBadge(coins = coins)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canUndo) Color(0xFF3A7E79) else Color(0x18FFFFFF))
                        .clickable(enabled = canUndo, onClick = onUndo)
                        .semantics { contentDescription = undoLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("↶", color = if (canUndo) Color.White else Color(0x66FFFFFF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .clickable(onClick = onPause)
                        .semantics { contentDescription = pauseLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Ⅱ", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ComboBanner(flash: ComboFlash, theme: BoardTheme) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.accent.copy(alpha = 0.18f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (flash.multiplier > 1) "COMBO x${flash.multiplier}" else "COMBO",
            color = theme.accent,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "+${flash.gained}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun GameOverDialog(
    controller: GameController,
    theme: BoardTheme,
    bestAtStart: Int,
    rewardedReady: Boolean,
    onContinue: () -> Unit,
    onDoubleCoins: () -> Unit,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit,
) {
    val isRecord = controller.score > 0 && controller.score > bestAtStart
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.game_over_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(6.dp))
        if (isRecord) {
            Text(
                text = stringResource(R.string.game_new_record),
                color = theme.accent,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = controller.score.toString(),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.width(16.dp))
            CoinBadge(coins = controller.finalCoins())
        }
        Spacer(Modifier.height(16.dp))

        if (!controller.continueGranted) {
            RewardedButton(
                text = stringResource(R.string.game_continue),
                hint = stringResource(R.string.game_continue_hint),
                ready = rewardedReady,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }
        if (!controller.doubleCoinsGranted) {
            RewardedButton(
                text = stringResource(R.string.game_double_coins),
                hint = stringResource(R.string.game_double_hint),
                ready = rewardedReady,
                onClick = onDoubleCoins,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
        }

        PrimaryButton(
            text = stringResource(R.string.game_play_again),
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = stringResource(R.string.game_main_menu),
            onClick = onMenu,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val TRAY_SPACING = 8.dp

/* ------------------------------- геометрия ------------------------------- */

/** Слот лотка по координате касания. */
private fun slotAt(
    position: Offset,
    trayOriginLocal: Offset,
    slotWidthPx: Float,
    trayHeightPx: Float,
): Int? {
    if (slotWidthPx <= 0f || trayHeightPx <= 0f) return null
    val localX = position.x - trayOriginLocal.x
    val localY = position.y - trayOriginLocal.y
    if (localY < -24f || localY > trayHeightPx + 24f) return null
    if (localX < -24f || localX > slotWidthPx * TRAY_SIZE + 24f) return null
    return (localX / slotWidthPx).toInt().coerceIn(0, TRAY_SIZE - 1)
}

/** Клетка поля, на которую указывает перетаскиваемая фигура (её левый верхний угол + подъём). */
private fun targetCell(drag: DragState, boardOriginLocal: Offset, cellSizePx: Float): Pair<Int, Int>? {
    if (cellSizePx <= 0f) return null
    val lift = cellSizePx * 1.35f
    val left = drag.pointer.x - drag.grabbedAt.x
    val top = drag.pointer.y - drag.grabbedAt.y - lift
    val row = ((top - boardOriginLocal.y) / cellSizePx).roundToInt()
    val col = ((left - boardOriginLocal.x) / cellSizePx).roundToInt()
    if (row < -1 || col < -1) return null
    if (row > GRID_SIZE || col > GRID_SIZE) return null
    if (abs(row) > GRID_SIZE || abs(col) > GRID_SIZE) return null
    return row.coerceIn(0, GRID_SIZE - 1) to col.coerceIn(0, GRID_SIZE - 1)
}
