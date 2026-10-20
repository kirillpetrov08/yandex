package com.ayaks.focus.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaks.focus.BuildConfig
import com.ayaks.focus.FocusApplication
import com.ayaks.focus.ads.FocusAds
import com.ayaks.focus.ads.FocusBanner
import com.ayaks.focus.audio.FocusSound
import com.ayaks.focus.data.FocusSettings
import com.ayaks.focus.data.FocusStats
import com.ayaks.focus.data.FocusStore
import com.ayaks.focus.game.AnswerResult
import com.ayaks.focus.game.FocusEngine
import com.ayaks.focus.game.FocusMode
import com.ayaks.focus.game.FocusSession
import com.ayaks.focus.game.GamePhase
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class Page { HOME, MODES, PLAY, RESULT, STATS, SETTINGS, GUIDE }

private val Backdrop = listOf(Color(0xFF10172D), Color(0xFF152541), Color(0xFF10172D))
private val Card = Color(0xFF1B2945)
private val Teal = Color(0xFF8CE6D2)
private val Violet = Color(0xFFB6A2FF)
private val Gold = Color(0xFFFFD27A)
private val SearchColors = FocusEngine.COLOR_HEX.map { Color(it) }

@Composable
fun FocusApp(store: FocusStore) {
    val context = LocalContext.current
    val application = context.applicationContext as FocusApplication
    val sound = remember { application.sound }
    val haptics = LocalHapticFeedback.current
    val stats by store.stats
    val settings by store.settings
    var page by remember { mutableStateOf(Page.HOME) }
    var session by remember { mutableStateOf<FocusSession?>(null) }
    var engine by remember { mutableStateOf(FocusEngine(0)) }
    var paused by remember { mutableStateOf(false) }
    var sessionSaved by remember { mutableStateOf(false) }
    var exitDialog by remember { mutableStateOf(false) }
    var consentDialog by remember { mutableStateOf(false) }
    var isDailySession by remember { mutableStateOf(false) }

    LaunchedEffect(settings.sound) { sound.setEnabled(settings.sound) }
    LaunchedEffect(BuildConfig.ADS_CONFIGURED, settings.consentDecided, settings.personalizedAds) {
        if (!BuildConfig.ADS_CONFIGURED) return@LaunchedEffect
        if (!settings.consentDecided) consentDialog = true
        else FocusAds.initialize(context, settings.personalizedAds)
    }

    fun startGame(mode: FocusMode, daily: Boolean) {
        val seed = if (daily) dailySeed() else System.nanoTime().toInt()
        engine = FocusEngine(seed)
        session = engine.start(mode, daily = daily, seed = seed)
        isDailySession = daily
        sessionSaved = false
        paused = false
        page = Page.PLAY
        sound.play(FocusSound.Cue.PICK)
    }

    fun applySession(next: FocusSession) {
        session = next
        if (next.phase == GamePhase.FINISHED) {
            if (!sessionSaved) {
                store.recordSession(next.score, isDailySession)
                sessionSaved = true
            }
            sound.play(FocusSound.Cue.FINISH)
            page = Page.RESULT
        }
    }

    fun answer(result: AnswerResult) {
        applySession(result.session)
        if (result.session.phase != GamePhase.FINISHED) {
            sound.play(if (result.correct) FocusSound.Cue.CORRECT else FocusSound.Cue.WRONG)
            if (settings.haptics) haptics.performHapticFeedback(if (result.correct) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
        }
    }

    BackHandler(enabled = page == Page.PLAY) {
        if (paused) {
            paused = false
            exitDialog = true
        } else paused = true
    }
    BackHandler(enabled = page != Page.HOME && page != Page.PLAY) { page = Page.HOME }

    LaunchedEffect(page, session?.mode, session?.round, session?.phase, paused) {
        val current = session ?: return@LaunchedEffect
        if (page != Page.PLAY || paused) return@LaunchedEffect
        if (current.mode == FocusMode.MEMORY && current.phase == GamePhase.SHOWING) {
            delay(420)
            current.sequence.forEach { cell ->
                val latest = session ?: return@LaunchedEffect
                if (latest.phase != GamePhase.SHOWING || paused) return@LaunchedEffect
                session = engine.revealCell(latest, cell)
                delay(390)
                session = engine.revealCell(session ?: latest, null)
                delay(145)
            }
            val latest = session ?: return@LaunchedEffect
            if (!paused) session = engine.memoryInputReady(latest)
        }
    }

    LaunchedEffect(page, session?.mode, session?.phase, session?.lives, paused) {
        val current = session ?: return@LaunchedEffect
        if (page != Page.PLAY || paused || current.phase != GamePhase.FEEDBACK) return@LaunchedEffect
        delay(if (current.mode == FocusMode.MEMORY) 620 else 260)
        val latest = session ?: return@LaunchedEffect
        when (latest.mode) {
            FocusMode.MEMORY -> session = engine.retryMemory(latest)
            FocusMode.SEARCH -> session = engine.resumeSearch(latest)
            FocusMode.COLORS -> Unit
        }
    }

    LaunchedEffect(page, session?.mode, session?.phase, paused) {
        val current = session ?: return@LaunchedEffect
        if (page != Page.PLAY || paused || current.phase != GamePhase.INPUT || current.mode == FocusMode.MEMORY) return@LaunchedEffect
        while (true) {
            delay(1_000)
            val latest = session ?: return@LaunchedEffect
            if (page != Page.PLAY || paused || latest.phase != GamePhase.INPUT) return@LaunchedEffect
            applySession(engine.tick(latest))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(Backdrop))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (page == Page.PLAY && session != null) {
                PlayScreen(
                    session = session!!,
                    paused = paused,
                    onBack = { paused = true },
                    onMemoryTap = { cell -> answer(engine.tapMemory(session!!, cell)) },
                    onSearchTap = { cell -> answer(engine.tapSearch(session!!, cell)) },
                    onColorTap = { color -> answer(engine.answerColor(session!!, color)) },
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    when (page) {
                        Page.HOME -> HomeScreen(
                            stats = stats,
                            dailyDone = stats.lastDailyKey == todayKey(),
                            onDaily = { startGame(dailyMode(), daily = true) },
                            onModes = { page = Page.MODES },
                            onStats = { page = Page.STATS },
                            onSettings = { page = Page.SETTINGS },
                            onGuide = { page = Page.GUIDE },
                        )
                        Page.MODES -> ModesScreen(
                            onBack = { page = Page.HOME },
                            onSelect = { startGame(it, daily = false) },
                        )
                        Page.RESULT -> ResultScreen(
                            session = session!!,
                            onAgain = { startGame(session!!.mode, daily = false) },
                            onHome = { page = Page.HOME; session = null },
                            onStats = { page = Page.STATS },
                        )
                        Page.STATS -> StatsScreen(stats = stats, onBack = { page = Page.HOME })
                        Page.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onBack = { page = Page.HOME },
                            onUpdate = {
                                store.updateSettings(it)
                                FocusAds.updateConsent(it.personalizedAds)
                            },
                            onPrivacy = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://kirillpetrov08.github.io/focus-privacy-policy.html")))
                                }
                            },
                        )
                        Page.GUIDE -> GuideScreen(onBack = { page = Page.HOME })
                        Page.PLAY -> Unit
                    }
                }
                FocusBanner()
            }
        }

        if (page == Page.PLAY && paused && !exitDialog) {
            AlertDialog(
                onDismissRequest = { paused = false },
                title = { Text("Пауза") },
                text = { Text("Время остановлено. Отдохни и продолжай, когда будешь готов.") },
                confirmButton = { TextButton(onClick = { paused = false }) { Text("Продолжить") } },
                dismissButton = { TextButton(onClick = { exitDialog = true }) { Text("Завершить") } },
                containerColor = Card,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (exitDialog) {
            AlertDialog(
                onDismissRequest = { exitDialog = false; paused = false },
                title = { Text("Выйти из тренировки?") },
                text = { Text("Текущая тренировка не будет записана. Можно вернуться к ней или начать заново.") },
                confirmButton = {
                    TextButton(onClick = { exitDialog = false; paused = false; session = null; page = Page.HOME }) { Text("Выйти") }
                },
                dismissButton = { TextButton(onClick = { exitDialog = false; paused = false }) { Text("Продолжить") } },
                containerColor = Card,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (consentDialog) {
            AlertDialog(
                onDismissRequest = {
                    store.updateSettings(settings.copy(consentDecided = true, personalizedAds = false))
                    consentDialog = false
                },
                title = { Text("Реклама и данные") },
                text = { Text("Для показа рекламы Яндекс может обрабатывать рекламный идентификатор устройства и технические данные. Геолокацию приложение не запрашивает. Выберите персонализированную рекламу или только контекстную.") },
                confirmButton = {
                    TextButton(onClick = {
                        store.updateSettings(settings.copy(consentDecided = true, personalizedAds = true))
                        consentDialog = false
                    }) { Text("Разрешить персонализацию") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        store.updateSettings(settings.copy(consentDecided = true, personalizedAds = false))
                        consentDialog = false
                    }) { Text("Только контекстная") }
                },
                containerColor = Card,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModesScreen(onBack: () -> Unit, onSelect: (FocusMode) -> Unit) {
    ScreenHeader("Выбери тренировку", onBack)
    Text("Каждая партия короткая, правила объясняются по ходу.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 18.dp))
    FocusMode.entries.forEach { mode ->
        ModeCard(mode, onClick = { onSelect(mode) })
        Spacer(Modifier.height(12.dp))
    }
    InfoNote("Все упражнения работают офлайн. Личный результат хранится только на этом устройстве.")
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun PlayScreen(
    session: FocusSession,
    paused: Boolean,
    onBack: () -> Unit,
    onMemoryTap: (Int) -> Unit,
    onSearchTap: (Int) -> Unit,
    onColorTap: (Int) -> Unit,
) {
    val completed = session.completedRounds
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleAction("‹", "Пауза", onBack)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(session.mode.title, style = MaterialTheme.typography.titleLarge)
                Text(if (session.isDaily) "Задание дня" else "Свободная тренировка", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.score}", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Teal)
                Text("очков", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Раунд ${completed + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("♥ ".repeat(session.lives).trim(), color = Color(0xFFFF8294), fontSize = 16.sp)
            if (session.mode != FocusMode.MEMORY) {
                Spacer(Modifier.width(10.dp))
                Text("${session.secondsLeft} с", color = if (session.secondsLeft <= 10) Color(0xFFFF8792) else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        ProgressTrack(progress = when (session.mode) {
            FocusMode.MEMORY -> completed.toFloat() / FocusEngine.MEMORY_ROUNDS
            FocusMode.SEARCH -> completed.toFloat() / FocusEngine.SEARCH_ROUNDS
            FocusMode.COLORS -> completed.toFloat() / FocusEngine.COLOR_ROUNDS
        })
        Spacer(Modifier.height(20.dp))
        val instruction = when (session.mode) {
            FocusMode.MEMORY -> when (session.phase) {
                GamePhase.SHOWING -> "Смотри на подсветку и запомни порядок"
                GamePhase.INPUT -> "Повтори последовательность"
                GamePhase.FEEDBACK -> "Не страшно — попробуй ещё раз"
                GamePhase.FINISHED -> "Тренировка завершена"
            }
            FocusMode.SEARCH -> "Найди символ: ${FocusEngine.SEARCH_SYMBOLS[session.searchTarget]}"
            FocusMode.COLORS -> "Нажми на цвет, которым написано слово"
        }
        Text(instruction, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(18.dp))

        when (session.mode) {
            FocusMode.MEMORY -> LazyVerticalGrid(
                columns = GridCells.Fixed(FocusEngine.MEMORY_GRID_SIDE),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).height(340.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(FocusEngine.MEMORY_GRID_CELLS) { cell ->
                    val lit = session.litCell == cell
                    Surface(
                        modifier = Modifier.aspectRatio(1f).clickable(
                            enabled = session.phase == GamePhase.INPUT && !paused,
                            onClick = { onMemoryTap(cell) },
                        ).semantics { contentDescription = "Кнопка ${cell + 1}" },
                        shape = RoundedCornerShape(22.dp),
                        color = if (lit) Violet else Color(0xFF202E4B),
                        border = BorderStroke(1.dp, if (lit) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.06f)),
                        shadowElevation = if (lit) 12.dp else 1.dp,
                    ) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { if (lit) Text("✦", color = Color.White, fontSize = 24.sp) } }
                }
            }
            FocusMode.SEARCH -> LazyVerticalGrid(
                columns = GridCells.Fixed(session.gridSize),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).height(if (session.gridSize == 4) 340.dp else 390.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(session.searchTiles) { index, symbolIndex ->
                    Surface(
                        modifier = Modifier.aspectRatio(1f).clickable(enabled = session.phase == GamePhase.INPUT && !paused) { onSearchTap(index) }
                            .semantics { contentDescription = "Знак ${FocusEngine.SEARCH_SYMBOLS[symbolIndex]}" },
                        shape = RoundedCornerShape(if (session.gridSize == 4) 17.dp else 13.dp),
                        color = Color(0xFF1B2945),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.055f)),
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(FocusEngine.SEARCH_SYMBOLS[symbolIndex], fontSize = if (session.gridSize == 4) 30.sp else 23.sp, color = listOf(Teal, Violet, Gold, Color(0xFFFF93A8))[symbolIndex])
                        }
                    }
                }
            }
            FocusMode.COLORS -> {
                Spacer(Modifier.weight(0.32f))
                AnimatedContent(targetState = session.round, label = "stroop-question") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(FocusEngine.COLOR_NAMES[session.wordColor], fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = SearchColors[session.inkColor], textAlign = TextAlign.Center)
                        Text("Не читай слово — выбери цвет букв", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                Spacer(Modifier.height(28.dp))
                FocusEngine.COLOR_NAMES.indices.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { colorIndex ->
                            Button(
                                onClick = { onColorTap(colorIndex) },
                                enabled = session.phase == GamePhase.INPUT && !paused,
                                modifier = Modifier.weight(1f).height(60.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SearchColors[colorIndex], contentColor = Color(0xFF152033)),
                            ) { Text(FocusEngine.COLOR_NAMES[colorIndex].lowercase(), fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.weight(0.6f))
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun ColumnScope.ResultScreen(session: FocusSession, onAgain: () -> Unit, onHome: () -> Unit, onStats: () -> Unit) {
    Spacer(Modifier.height(34.dp))
    LogoMark(Modifier.size(76.dp).align(Alignment.CenterHorizontally))
    Spacer(Modifier.height(20.dp))
    Text("Тренировка завершена", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Text(if (session.isDaily) "Задание дня" else session.mode.title, color = Teal, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(22.dp))
    Surface(shape = RoundedCornerShape(28.dp), color = Card, modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Text("${session.score}", fontSize = 58.sp, fontWeight = FontWeight.ExtraBold, color = Gold)
            Text("очков", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultMetric("${session.completedRounds}", "раундов")
                ResultMetric("${session.streak}", "серия")
                ResultMetric("${session.lives}", "ошибки в запасе")
            }
            Spacer(Modifier.height(16.dp))
            Text("Хорошая работа. Короткая регулярная практика — отличный повод вернуться завтра.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(18.dp))
    PrimaryCta("ЕЩЁ ОДНА ТРЕНИРОВКА", onAgain)
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = onStats, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("МОЯ СТАТИСТИКА") }
    TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("На главную") }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun StatsScreen(stats: FocusStats, onBack: () -> Unit) {
    ScreenHeader("Мой прогресс", onBack)
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Card) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            StatLine("Завершено тренировок", stats.sessions.toString())
            StatLine("Лучший результат", stats.bestScore.toString())
            StatLine("Всего набрано очков", stats.totalPoints.toString())
            StatLine("Серия заданий дня", "${stats.dailyStreak} дней")
            StatLine("Результат сегодня", if (stats.lastDailyKey == todayKey()) stats.dailyBest.toString() else "—")
        }
    }
    Spacer(Modifier.height(14.dp))
    InfoNote("Статистика сохраняется только на этом устройстве. В приложении нет аккаунтов, рейтингов или передачи результатов другим игрокам.")
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SettingsScreen(settings: FocusSettings, onBack: () -> Unit, onUpdate: (FocusSettings) -> Unit, onPrivacy: () -> Unit) {
    ScreenHeader("Настройки", onBack)
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Card) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            PreferenceRow("Звуковые эффекты", "Короткие сигналы при ответе", settings.sound) { onUpdate(settings.copy(sound = it)) }
            PreferenceRow("Виброотклик", "Лёгкая тактильная подсказка", settings.haptics) { onUpdate(settings.copy(haptics = it)) }
            PreferenceRow("Персонализированная реклама", "Можно изменить выбор в любой момент", settings.personalizedAds) {
                onUpdate(settings.copy(personalizedAds = it, consentDecided = true))
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    InfoNote("Реклама показывается только вне игровых раундов. Звук, вибрацию и рекламу можно отключить здесь.")
    TextButton(onClick = onPrivacy, modifier = Modifier.fillMaxWidth()) { Text("Политика конфиденциальности") }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun GuideScreen(onBack: () -> Unit) {
    ScreenHeader("Как это работает", onBack)
    listOf(
        "Выбери ежедневное задание или один из трёх режимов.",
        "В «Памяти» повторяй подсвеченную последовательность.",
        "Во «Внимании» найди один символ, который отличается.",
        "В «Цвете» нажимай цвет текста, а не написанное слово.",
        "Занятие проходит офлайн. Результат сохраняется только на устройстве.",
    ).forEachIndexed { i, text ->
        Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), color = Card) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Teal.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = Teal, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.width(12.dp))
                Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun ModeCard(mode: FocusMode, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = Card,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(Violet.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Text(mode.icon, color = Teal, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(mode.title, style = MaterialTheme.typography.titleMedium)
                Text(mode.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeScreen(
    stats: FocusStats,
    dailyDone: Boolean,
    onDaily: () -> Unit,
    onModes: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onGuide: () -> Unit,
) {
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LogoMark(Modifier.size(52.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("ФОКУС", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
            Text("короткая тренировка для ума", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CircleAction("⚙", "Настройки", onSettings)
    }
    Spacer(Modifier.height(30.dp))
    Text("Привет!", style = MaterialTheme.typography.headlineLarge)
    Spacer(Modifier.height(4.dp))
    Text("Выбери упражнение и удели себе пару минут.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(22.dp))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.background(Brush.linearGradient(listOf(Color(0xFF253B62), Color(0xFF214C55)))).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ЗАДАНИЕ ДНЯ", color = Teal, style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(dailyMode().title, style = MaterialTheme.typography.titleLarge)
                    Text(if (dailyDone) "Сегодня уже выполнено — можно сыграть ещё раз" else "Один новый короткий вызов каждый день", color = Color(0xFFD0D9EA), style = MaterialTheme.typography.bodySmall)
                }
                Text("✦", fontSize = 38.sp, color = Gold)
            }
            Spacer(Modifier.height(18.dp))
            Button(onClick = onDaily, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color(0xFF102B31))) {
                Text(if (dailyDone) "ИГРАТЬ ЕЩЁ РАЗ" else "НАЧАТЬ ТРЕНИРОВКУ", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard("${stats.dailyStreak}", "дней подряд", "⚡", Modifier.weight(1f))
        MetricCard("${stats.bestScore}", "лучший счёт", "✧", Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    ActionCard("Выбрать режим", "Три упражнения на внимание и память", "◈", onModes)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SmallAction("Статистика", "▤", onStats, Modifier.weight(1f))
        SmallAction("Как играть", "?", onGuide, Modifier.weight(1f))
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleAction("‹", "Назад", onBack)
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun LogoMark(modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF9FEAD8), Color(0xFFB4A0FF), Color(0xFFFFD27A)))), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(3.dp).clip(CircleShape).background(Color(0xFF17233D)), contentAlignment = Alignment.Center) {
            Text("✦", color = Teal, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun CircleAction(label: String, description: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(46.dp).clickable(onClick = onClick).semantics { contentDescription = description }, shape = CircleShape, color = Color.White.copy(alpha = 0.07f)) {
        Box(contentAlignment = Alignment.Center) { Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ActionCard(title: String, subtitle: String, symbol: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), color = Card) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, fontSize = 26.sp, color = Violet)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, symbol: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = Card) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, color = Gold, fontSize = 21.sp)
            Spacer(Modifier.width(9.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SmallAction(title: String, symbol: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), color = Card) {
        Column(Modifier.padding(13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(symbol, color = Teal, fontSize = 21.sp)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun InfoNote(text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = Color(0xFF18233C), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.045f))) {
        Text(text, modifier = Modifier.padding(15.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrimaryCta(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color(0xFF102B31))) {
        Text(text, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ResultMetric(value: String, label: String) {
    Surface(shape = RoundedCornerShape(15.dp), color = Color.White.copy(alpha = 0.05f)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Teal, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PreferenceRow(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun ProgressTrack(progress: Float) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(420), label = "progress")
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxWidth(animated).fillMaxSize().clip(CircleShape).background(Brush.horizontalGradient(listOf(Teal, Violet))))
    }
}

private fun dailyMode(): FocusMode {
    val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return FocusMode.entries[day % FocusMode.entries.size]
}

private fun dailySeed(): Int = todayKey().hashCode()
private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
