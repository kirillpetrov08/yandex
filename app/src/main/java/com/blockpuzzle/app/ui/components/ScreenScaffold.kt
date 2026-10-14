package com.blockpuzzle.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.blockpuzzle.app.ads.YandexBannerSlot
import com.blockpuzzle.app.ui.theme.BoardTheme

/**
 * Общий каркас экранов меню/магазина/статистики/настроек.
 *
 * Баннер РСЯ закреплён снизу отдельным блоком и НЕ перекрывает контент —
 * это прямое требование правил размещения рекламы.
 */
@Composable
fun ScreenScaffold(
    theme: BoardTheme,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showBanner: Boolean = true,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.screenBackground))
            .statusBarsPadding(),
    ) {
        if (title != null) {
            ScreenHeader(title = title, onBack = onBack, trailing = trailing)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            content = content,
        )
        if (showBanner) {
            YandexBannerSlot(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
            )
        }
    }
}
