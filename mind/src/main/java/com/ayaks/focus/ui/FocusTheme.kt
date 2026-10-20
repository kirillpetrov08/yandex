package com.ayaks.focus.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ayaks.focus.R

private val Ink = Color(0xFFF3F4FF)
private val Muted = Color(0xFFB2BBD4)
private val FocusFont = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)
private val Defaults = Typography()
private val FocusTypography = Typography(
    displayLarge = Defaults.displayLarge.copy(fontFamily = FocusFont, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    displayMedium = Defaults.displayMedium.copy(fontFamily = FocusFont),
    displaySmall = Defaults.displaySmall.copy(fontFamily = FocusFont),
    headlineLarge = Defaults.headlineLarge.copy(fontFamily = FocusFont, fontWeight = FontWeight.ExtraBold),
    headlineMedium = Defaults.headlineMedium.copy(fontFamily = FocusFont, fontWeight = FontWeight.Bold),
    headlineSmall = Defaults.headlineSmall.copy(fontFamily = FocusFont),
    titleLarge = Defaults.titleLarge.copy(fontFamily = FocusFont, fontWeight = FontWeight.Bold),
    titleMedium = Defaults.titleMedium.copy(fontFamily = FocusFont, fontWeight = FontWeight.SemiBold),
    titleSmall = Defaults.titleSmall.copy(fontFamily = FocusFont),
    bodyLarge = Defaults.bodyLarge.copy(fontFamily = FocusFont),
    bodyMedium = Defaults.bodyMedium.copy(fontFamily = FocusFont),
    bodySmall = Defaults.bodySmall.copy(fontFamily = FocusFont),
    labelLarge = Defaults.labelLarge.copy(fontFamily = FocusFont, fontWeight = FontWeight.Bold),
    labelMedium = Defaults.labelMedium.copy(fontFamily = FocusFont),
    labelSmall = Defaults.labelSmall.copy(fontFamily = FocusFont),
)

private val FocusColors = darkColorScheme(
    primary = Color(0xFF8CE6D2),
    onPrimary = Color(0xFF102B31),
    secondary = Color(0xFFB6A2FF),
    onSecondary = Color(0xFF211943),
    tertiary = Color(0xFFFFD27A),
    background = Color(0xFF10172D),
    onBackground = Ink,
    surface = Color(0xFF19233D),
    onSurface = Ink,
    surfaceVariant = Color(0xFF25314E),
    onSurfaceVariant = Muted,
    error = Color(0xFFFF8792),
)

@Composable
fun FocusTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FocusColors, typography = FocusTypography, content = content)
}
