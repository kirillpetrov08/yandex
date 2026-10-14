package com.blockpuzzle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blockpuzzle.app.R
import com.blockpuzzle.app.ui.components.CoinBadge
import com.blockpuzzle.app.ui.components.CoinIcon
import com.blockpuzzle.app.ui.components.PanelCard
import com.blockpuzzle.app.ui.components.PrimaryButton
import com.blockpuzzle.app.ui.components.ScreenScaffold
import com.blockpuzzle.app.ui.components.SecondaryButton
import com.blockpuzzle.app.ui.components.ThemePreview
import com.blockpuzzle.app.ui.theme.BoardTheme

@Composable
fun MenuScreen(
    theme: BoardTheme,
    coins: Int,
    best: Int,
    dailyBonusAvailable: Boolean,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    onTutorial: () -> Unit,
    onDailyBonus: () -> Unit,
) {
    ScreenScaffold(theme = theme, scrollable = true, content = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoinBadge(coins = coins)
                Text(
                    text = stringResource(R.string.menu_best_score) + ": " + best,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(28.dp))

            // Логотип: мини-превью темы вместо картинки — ноль лишних ресурсов в APK.
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(theme.boardBackground, theme.boardCellBackground))),
                contentAlignment = Alignment.Center,
            ) {
                ThemePreview(colors = theme.cellColors, modifier = Modifier.size(76.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 34.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text = stringResource(R.string.menu_play),
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            DailyBonusCard(available = dailyBonusAvailable, coins = coins, onClick = onDailyBonus)

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MenuTile(
                    title = stringResource(R.string.menu_shop),
                    emoji = "🛍",
                    modifier = Modifier.weight(1f),
                    onClick = onShop,
                )
                MenuTile(
                    title = stringResource(R.string.menu_stats),
                    emoji = "🏆",
                    modifier = Modifier.weight(1f),
                    onClick = onStats,
                )
                MenuTile(
                    title = stringResource(R.string.menu_settings),
                    emoji = "⚙",
                    modifier = Modifier.weight(1f),
                    onClick = onSettings,
                )
            }

            Spacer(Modifier.height(12.dp))

            SecondaryButton(
                text = stringResource(R.string.menu_how_to_play),
                onClick = onTutorial,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
        }
    })
}

@Composable
private fun MenuTile(
    title: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DailyBonusCard(available: Boolean, coins: Int, onClick: () -> Unit) {
    if (!available) {
        Text(
            text = stringResource(R.string.daily_bonus_claimed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    PanelCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFD166)),
                contentAlignment = Alignment.Center,
            ) {
                CoinIcon(size = 20.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.daily_bonus_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.menu_daily_bonus),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.ads_video),
                color = Color(0xFF7DE3D4),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
