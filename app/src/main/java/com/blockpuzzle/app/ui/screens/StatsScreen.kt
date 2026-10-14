package com.blockpuzzle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockpuzzle.app.R
import com.blockpuzzle.app.data.Achievements
import com.blockpuzzle.app.data.PlayerState
import com.blockpuzzle.app.ui.components.PanelCard
import com.blockpuzzle.app.ui.components.ScreenScaffold
import com.blockpuzzle.app.ui.components.StatRow
import com.blockpuzzle.app.ui.theme.BoardTheme

@Composable
fun StatsScreen(
    theme: BoardTheme,
    player: PlayerState,
    onBack: () -> Unit,
) {
    ScreenScaffold(theme = theme, title = stringResource(R.string.stats_title), onBack = onBack) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    StatRow(stringResource(R.string.stats_games), player.gamesPlayed.toString())
                    StatRow(stringResource(R.string.stats_best), player.bestScore.toString())
                    StatRow(stringResource(R.string.stats_total_score), player.totalScore.toString())
                    StatRow(stringResource(R.string.stats_lines), player.totalLines.toString())
                    StatRow(stringResource(R.string.stats_best_combo), "x${player.bestCombo}")
                    StatRow(
                        stringResource(R.string.stats_play_time),
                        stringResource(R.string.stats_time_format, (player.playTimeMs / 60_000L).toInt()),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.stats_achievements),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Achievements.all.forEach { achievement ->
                    val unlocked = player.achievements.contains(Achievements.key(achievement.id))
                    AchievementRow(
                        title = stringResource(titleResFor(achievement.id)),
                        description = stringResource(descResFor(achievement.id)),
                        unlocked = unlocked,
                        accent = theme.accent,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AchievementRow(title: String, description: String, unlocked: Boolean, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (unlocked) accent.copy(alpha = 0.25f) else Color(0x1AFFFFFF)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (unlocked) "★" else "☆",
                color = if (unlocked) accent else Color(0x66FFFFFF),
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun titleResFor(id: String): Int = when (id) {
    "first_game" -> R.string.ach_first_game
    "score_1000" -> R.string.ach_score_1000
    "score_5000" -> R.string.ach_score_5000
    "lines_100" -> R.string.ach_lines_100
    "combo_5" -> R.string.ach_combo_5
    "games_10" -> R.string.ach_games_10
    "games_50" -> R.string.ach_games_50
    else -> R.string.ach_coins_500
}

private fun descResFor(id: String): Int = when (id) {
    "first_game" -> R.string.ach_first_game_desc
    "score_1000" -> R.string.ach_score_1000_desc
    "score_5000" -> R.string.ach_score_5000_desc
    "lines_100" -> R.string.ach_lines_100_desc
    "combo_5" -> R.string.ach_combo_5_desc
    "games_10" -> R.string.ach_games_10_desc
    "games_50" -> R.string.ach_games_50_desc
    else -> R.string.ach_coins_500_desc
}
