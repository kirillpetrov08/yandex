package com.blockpuzzle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.blockpuzzle.app.R
import com.blockpuzzle.app.ui.components.PanelCard
import com.blockpuzzle.app.ui.components.PrimaryButton
import com.blockpuzzle.app.ui.components.ScreenScaffold
import com.blockpuzzle.app.ui.theme.BoardTheme

@Composable
fun TutorialScreen(
    theme: BoardTheme,
    onBack: () -> Unit,
) {
    ScreenScaffold(theme = theme, title = stringResource(R.string.tutorial_title), onBack = onBack) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TutorialStep(1, theme, R.string.tutorial_1_title, R.string.tutorial_1_desc)
            TutorialStep(2, theme, R.string.tutorial_2_title, R.string.tutorial_2_desc)
            TutorialStep(3, theme, R.string.tutorial_3_title, R.string.tutorial_3_desc)
            TutorialStep(4, theme, R.string.tutorial_4_title, R.string.tutorial_4_desc)
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = stringResource(R.string.tutorial_got_it),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TutorialStep(step: Int, theme: BoardTheme, titleRes: Int, descriptionRes: Int) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.boardBackground),
                contentAlignment = Alignment.Center,
            ) {
                StepIllustration(step = step, theme = theme)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${step}. " + stringResource(titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Простые пиктограммы шагов — рисуем кодом, без картинок в APK. */
@Composable
private fun StepIllustration(step: Int, theme: BoardTheme) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(44.dp)) {
        val cell = size.width / 4f
        fun square(col: Int, row: Int, color: Color) {
            drawRoundRect(
                color = color,
                topLeft = Offset(col * cell + cell * 0.1f, row * cell + cell * 0.1f),
                size = Size(cell * 0.8f, cell * 0.8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.22f),
            )
        }
        drawRoundRect(
            color = theme.boardCellBackground,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cell * 0.3f),
        )
        when (step) {
            1 -> {
                square(0, 0, theme.colorFor(1))
                square(1, 0, theme.colorFor(2))
                square(2, 2, theme.colorFor(3).copy(alpha = 0.4f))
            }

            2 -> {
                repeat(4) { col -> square(col, 2, theme.colorFor(1)) }
            }

            3 -> {
                repeat(4) { col -> square(col, 1, theme.colorFor(2)) }
                repeat(4) { col -> square(col, 3, theme.colorFor(3)) }
            }

            else -> {
                // «Ходов нет» — почти заполненное поле без места для фигуры.
                square(0, 3, theme.colorFor(4))
                square(2, 0, theme.colorFor(5))
                square(3, 3, theme.colorFor(6))
                square(1, 1, theme.colorFor(7).copy(alpha = 0.5f))
            }
        }
    }
}
