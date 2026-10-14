package com.blockpuzzle.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Палитра игрового поля: 7 цветов фигур + фон доски + общий фон экрана. */
data class BoardTheme(
    val id: String,
    val cellColors: List<Color>,
    val boardBackground: Color,
    val boardCellBackground: Color,
    val screenBackground: List<Color>,
    val accent: Color,
) {
    fun colorFor(index: Int): Color =
        if (index <= 0) boardCellBackground else cellColors[(index - 1) % cellColors.size]
}

private fun c(hex: Long) = Color(hex)

val ClassicTheme = BoardTheme(
    id = "default",
    cellColors = listOf(
        c(0xFFFFD166), c(0xFFEF476F), c(0xFF06D6A0), c(0xFF4CC9F0),
        c(0xFFB388FF), c(0xFFFF9F45), c(0xFFF15BB5),
    ),
    boardBackground = c(0xFF161A33),
    boardCellBackground = c(0xFF232A4A),
    screenBackground = listOf(c(0xFF141838), c(0xFF0A0C1C)),
    accent = c(0xFF7C5CFF),
)

val NeonTheme = BoardTheme(
    id = "neon",
    cellColors = listOf(
        c(0xFF00E5FF), c(0xFFFF2D95), c(0xFF7CFF00), c(0xFFFFE600),
        c(0xFF8A5CFF), c(0xFFFF6A00), c(0xFF00FFC2),
    ),
    boardBackground = c(0xFF0B0F1A),
    boardCellBackground = c(0xFF1A2233),
    screenBackground = listOf(c(0xFF0D1224), c(0xFF04060E)),
    accent = c(0xFF00E5FF),
)

val CandyTheme = BoardTheme(
    id = "candy",
    cellColors = listOf(
        c(0xFFFF8FAB), c(0xFFFFC2D1), c(0xFF9BF6FF), c(0xFFCAFFBF),
        c(0xFFFFD6A5), c(0xFFBDB2FF), c(0xFFFFADAD),
    ),
    boardBackground = c(0xFF3B2E52),
    boardCellBackground = c(0xFF52406E),
    screenBackground = listOf(c(0xFF4A3468), c(0xFF241832)),
    accent = c(0xFFFF8FAB),
)

val OceanTheme = BoardTheme(
    id = "ocean",
    cellColors = listOf(
        c(0xFF48CAE4), c(0xFF90E0EF), c(0xFF0077B6), c(0xFF00B4D8),
        c(0xFFADE8F4), c(0xFF0096C7), c(0xFFCAF0F8),
    ),
    boardBackground = c(0xFF0B2545),
    boardCellBackground = c(0xFF12335C),
    screenBackground = listOf(c(0xFF0A2A4A), c(0xFF04121F)),
    accent = c(0xFF48CAE4),
)

val ForestTheme = BoardTheme(
    id = "forest",
    cellColors = listOf(
        c(0xFF95D5B2), c(0xFF74C69D), c(0xFFD9ED92), c(0xFFB5E48C),
        c(0xFF40916C), c(0xFFF4A261), c(0xFFE9C46A),
    ),
    boardBackground = c(0xFF12271C),
    boardCellBackground = c(0xFF1D3B2A),
    screenBackground = listOf(c(0xFF16311F), c(0xFF08150D)),
    accent = c(0xFF95D5B2),
)

val GalaxyTheme = BoardTheme(
    id = "galaxy",
    cellColors = listOf(
        c(0xFF9D4EDD), c(0xFF7B2CBF), c(0xFF48CAE4), c(0xFFFF6BD6),
        c(0xFFC77DFF), c(0xFF5390D9), c(0xFFF72585),
    ),
    boardBackground = c(0xFF140B2B),
    boardCellBackground = c(0xFF251A47),
    screenBackground = listOf(c(0xFF241146), c(0xFF0A0518)),
    accent = c(0xFFC77DFF),
)

val MonoTheme = BoardTheme(
    id = "mono",
    cellColors = listOf(
        c(0xFFE0E1DD), c(0xFFB8BCC4), c(0xFF9AA0AA), c(0xFF7C8290),
        c(0xFFD6D8DD), c(0xFFA8AEB8), c(0xFFCED2D9),
    ),
    boardBackground = c(0xFF1B1B1F),
    boardCellBackground = c(0xFF2B2B31),
    screenBackground = listOf(c(0xFF232327), c(0xFF0E0E10)),
    accent = c(0xFFE0E1DD),
)

/** Все доступные темы магазина. Цена — в монетах. */
data class ThemeItem(val theme: BoardTheme, val nameResId: Int, val price: Int)

val LocalBoardTheme = staticCompositionLocalOf { ClassicTheme }

private val DarkScheme = darkColorScheme(
    primary = c(0xFFB9A6FF),
    onPrimary = c(0xFF1B1035),
    primaryContainer = c(0xFF3A2C6E),
    onPrimaryContainer = c(0xFFEDE7FF),
    secondary = c(0xFF7DE3D4),
    onSecondary = c(0xFF062A26),
    background = c(0xFF0C0F1F),
    onBackground = c(0xFFF2F3FA),
    surface = c(0xFF171B31),
    onSurface = c(0xFFF2F3FA),
    surfaceVariant = c(0xFF232A4A),
    onSurfaceVariant = c(0xFFBFC4DD),
    error = c(0xFFFF6B81),
)

@Composable
fun BlockPuzzleTheme(theme: BoardTheme = ClassicTheme, content: @Composable () -> Unit) {
    // Игра задумана как «тёмная»: светлая схема ухудшает читаемость мелких блоков.
    CompositionLocalProvider(LocalBoardTheme provides theme) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = BlockPuzzleTypography,
            content = content,
        )
    }
}

private val BlockPuzzleTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.sp),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = Typography().titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = Typography().titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Typography().labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = FontFamily.SansSerif),
)

/** Общие размеры интерфейса. */
object Dimens {
    val screenPadding = 16.dp
    val cardCorner = 20.dp
    val blockCorner = 6.dp
    val bannerReservedHeight = 56.dp
}
