package com.blockpuzzle.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blockpuzzle.app.BuildConfig
import com.blockpuzzle.app.Config
import com.blockpuzzle.app.R
import com.blockpuzzle.app.ads.AdUnits
import com.blockpuzzle.app.data.Settings
import com.blockpuzzle.app.ui.components.GameDialog
import com.blockpuzzle.app.ui.components.PanelCard
import com.blockpuzzle.app.ui.components.ScreenScaffold
import com.blockpuzzle.app.ui.components.SettingToggleRow
import com.blockpuzzle.app.ui.theme.BoardTheme

@Composable
fun SettingsScreen(
    theme: BoardTheme,
    settings: Settings,
    onSoundChange: (Boolean) -> Unit,
    onHapticsChange: (Boolean) -> Unit,
    onPersonalizedAdsChange: (Boolean) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    ScreenScaffold(theme = theme, title = stringResource(R.string.settings_title), onBack = onBack) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingToggleRow(
                        title = stringResource(R.string.settings_sound),
                        description = null,
                        checked = settings.sound,
                        onCheckedChange = onSoundChange,
                    )
                    SettingToggleRow(
                        title = stringResource(R.string.settings_haptics),
                        description = null,
                        checked = settings.haptics,
                        onCheckedChange = onHapticsChange,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            PanelCard(modifier = Modifier.fillMaxWidth()) {
                SettingToggleRow(
                    title = stringResource(R.string.settings_ads_personalization),
                    description = stringResource(R.string.settings_ads_personalization_desc),
                    checked = settings.personalizedAds,
                    onCheckedChange = onPersonalizedAdsChange,
                )
            }

            Spacer(Modifier.height(12.dp))

            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    SettingsAction(stringResource(R.string.settings_privacy_policy)) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(Config.PRIVACY_POLICY_URL)),
                        )
                    }
                    SettingsAction(stringResource(R.string.settings_about)) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(Config.PLAY_MARKET_URL)),
                        )
                    }
                    SettingsAction(
                        title = stringResource(R.string.settings_reset),
                        dangerous = true,
                        onClick = { showResetDialog = true },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.settings_about_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (AdUnits.testMode) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ads_test_mode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showResetDialog) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xCC05070F)),
            contentAlignment = Alignment.Center,
        ) {
            GameDialog(
                title = stringResource(R.string.settings_reset_title),
                message = stringResource(R.string.settings_reset_message),
                confirmText = stringResource(R.string.settings_reset),
                onConfirm = {
                    showResetDialog = false
                    onResetProgress()
                },
                dismissText = stringResource(R.string.common_cancel),
                onDismiss = { showResetDialog = false },
            )
        }
    }
}

@Composable
private fun SettingsAction(
    title: String,
    dangerous: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = if (dangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}
