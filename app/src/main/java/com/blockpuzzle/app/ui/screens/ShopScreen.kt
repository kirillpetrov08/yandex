package com.blockpuzzle.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blockpuzzle.app.R
import com.blockpuzzle.app.ads.YandexNativeAdSlot
import com.blockpuzzle.app.ui.components.CoinBadge
import com.blockpuzzle.app.ui.components.CoinIcon
import com.blockpuzzle.app.ui.components.PanelCard
import com.blockpuzzle.app.ui.components.RewardedButton
import com.blockpuzzle.app.ui.components.ScreenScaffold
import com.blockpuzzle.app.ui.components.ThemePreview
import com.blockpuzzle.app.ui.theme.BoardTheme
import com.blockpuzzle.app.ui.theme.ThemeCatalog
import com.blockpuzzle.app.ui.theme.ThemeItem

@Composable
fun ShopScreen(
    theme: BoardTheme,
    coins: Int,
    ownedThemes: Set<String>,
    equippedThemeId: String,
    rewardedReady: Boolean,
    onSelectTheme: (ThemeItem) -> Unit,
    onFreeCoins: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        theme = theme,
        title = stringResource(R.string.shop_title),
        onBack = onBack,
        trailing = { CoinBadge(coins = coins) },
        scrollable = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Бесплатные монеты за просмотр рекламы: главный источник дохода в магазине.
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PanelCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CoinIcon(size = 28.dp)
                        Spacer(Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.shop_free_coins),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(R.string.ads_video_for_reward),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    RewardedButton(
                        text = stringResource(R.string.ads_watch_ad),
                        hint = stringResource(R.string.ads_reward_coins, com.blockpuzzle.app.ads.AdUnits.REWARDED_COINS),
                        ready = rewardedReady,
                        onClick = onFreeCoins,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.shop_themes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ThemeCatalog.items) { item ->
                    ThemeCard(
                        item = item,
                        coins = coins,
                        owned = ownedThemes.contains(item.theme.id),
                        equipped = equippedThemeId == item.theme.id,
                        onClick = { onSelectTheme(item) },
                    )
                }
                item {
                    // Нативная реклама РСЯ: блок в дизайне приложения, не чаще 1 раза в 30 секунд.
                    YandexNativeAdSlot(modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    item: ThemeItem,
    coins: Int,
    owned: Boolean,
    equipped: Boolean,
    onClick: () -> Unit,
) {
    val affordable = owned || coins >= item.price
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = if (equipped) 2.dp else 0.dp,
                color = if (equipped) item.theme.accent else Color.Transparent,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable(enabled = affordable, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(item.theme.boardBackground),
            contentAlignment = Alignment.Center,
        ) {
            ThemePreview(colors = item.theme.cellColors, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(item.nameResId),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        when {
            equipped -> Text(
                text = stringResource(R.string.shop_equipped),
                color = item.theme.accent,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )

            owned -> Text(
                text = stringResource(R.string.shop_equip),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )

            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 14.dp)
                Spacer(Modifier.size(4.dp))
                Text(
                    text = if (affordable) item.price.toString() else stringResource(R.string.shop_not_enough),
                    color = if (affordable) Color(0xFFFFD166) else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
