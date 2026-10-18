package com.blockpuzzle.app.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.blockpuzzle.app.R
import com.blockpuzzle.app.ads.AdLoadState
import com.blockpuzzle.app.ads.AdUnits
import com.blockpuzzle.app.ads.Ads
import com.blockpuzzle.app.ads.ConsentManager
import com.blockpuzzle.app.data.Prefs
import com.blockpuzzle.app.sfx.SfxEngine
import com.blockpuzzle.app.ui.components.GameDialog
import com.blockpuzzle.app.ui.game.GameController
import com.blockpuzzle.app.ui.game.GameScreen
import com.blockpuzzle.app.ui.screens.MenuScreen
import com.blockpuzzle.app.ui.screens.SettingsScreen
import com.blockpuzzle.app.ui.screens.ShopScreen
import com.blockpuzzle.app.ui.screens.StatsScreen
import com.blockpuzzle.app.ui.screens.TutorialScreen
import com.blockpuzzle.app.ui.theme.BlockPuzzleTheme
import com.blockpuzzle.app.ui.theme.ThemeCatalog

private enum class Screen { Menu, Game, Shop, Stats, Settings, Tutorial }

/**
 * Корень приложения: навигация, согласие на данные, реклама и все экраны.
 */
@Composable
fun AppRoot(
    prefs: Prefs,
    consent: ConsentManager,
    sfx: SfxEngine,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val application = remember(context) { context.applicationContext as android.app.Application }

    val player by prefs.player.collectAsState()
    val settings by prefs.settings.collectAsState()
    val rewardedState by Ads.rewardedState.collectAsState()

    var screen by remember { mutableStateOf(Screen.Menu) }
    var showConsentDialog by remember { mutableStateOf(false) }
    val controller = remember { GameController(prefs, sfx) }

    val boardTheme = remember(player.equippedTheme) { ThemeCatalog.byId(player.equippedTheme).theme }

    /**
     * Показ rewarded-рекламы. Награда выдаётся только за реальный просмотр:
     * если объявление не готово, честно сообщаем об этом пользователю.
     */
    val showRewardedOrToast: (() -> Unit) -> Unit = { onReward ->
        val currentActivity = activity
        if (currentActivity == null) {
            onReward()
        } else {
            val started = Ads.showRewarded(currentActivity, onReward = onReward) { rewarded ->
                if (!rewarded) toast(context, context.getString(R.string.ads_unavailable))
            }
            if (!started) toast(context, context.getString(R.string.ads_loading))
        }
    }

    // Инициализация рекламы: сначала согласие, только потом SDK.
    LaunchedEffect(Unit) {
        if (consent.needsConsent) {
            showConsentDialog = true
        } else {
            Ads.init(application, consent)
        }
    }

    // Синхронизация звука с настройками.
    LaunchedEffect(settings.sound) { sfx.setEnabled(settings.sound) }

    // «Назад» из вторичных экранов — в меню. Во время партии назад обрабатывает GameScreen.
    androidx.activity.compose.BackHandler(enabled = screen != Screen.Menu && screen != Screen.Game) {
        screen = Screen.Menu
    }

    BlockPuzzleTheme(theme = boardTheme) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0C0F1F))) {
            when (screen) {
                Screen.Menu -> MenuScreen(
                    theme = boardTheme,
                    coins = player.coins,
                    best = player.bestScore,
                    dailyBonusAvailable = prefs.isDailyBonusAvailable(),
                    onPlay = {
                        controller.startNewGame()
                        screen = Screen.Game
                    },
                    onShop = { screen = Screen.Shop },
                    onStats = { screen = Screen.Stats },
                    onSettings = { screen = Screen.Settings },
                    onTutorial = { screen = Screen.Tutorial },
                    onDailyBonus = {
                        prefs.claimDailyBonus(AdUnits.DAILY_BONUS_COINS)
                        sfx.play(SfxEngine.Sound.COIN)
                        toast(
                            context,
                            context.getString(R.string.daily_bonus_reward, AdUnits.DAILY_BONUS_COINS),
                        )
                    },
                )

                Screen.Game -> GameScreen(
                    controller = controller,
                    theme = boardTheme,
                    coins = player.coins,
                    best = player.bestScore,
                    rewardedReady = rewardedState == AdLoadState.READY,
                    hapticsEnabled = settings.haptics,
                    onGameFinished = { Ads.onGameFinished() },
                    onExitToMenu = { screen = Screen.Menu },
                    onPlayAgain = {
                        // Межстраничная реклама — только в естественной паузе между партиями
                        // и с учётом частотной политики (интервал + каждый N-й проигрыш).
                        val startNextGame = {
                            controller.startNewGame()
                            screen = Screen.Game
                        }
                        if (Ads.canShowInterstitial() && activity != null) {
                            Ads.showInterstitial(activity) { startNextGame() }
                        } else {
                            startNextGame()
                        }
                    },
                    onContinueRewardClick = {
                        showRewardedOrToast { controller.applyContinue() }
                    },
                    onDoubleCoinsRewardClick = {
                        showRewardedOrToast { controller.applyDoubleCoins() }
                    },
                )

                Screen.Shop -> ShopScreen(
                    theme = boardTheme,
                    coins = player.coins,
                    ownedThemes = player.ownedThemes,
                    equippedThemeId = player.equippedTheme,
                    rewardedReady = rewardedState == AdLoadState.READY,
                    onSelectTheme = { item ->
                        if (player.ownedThemes.contains(item.theme.id)) {
                            prefs.equipTheme(item.theme.id)
                        } else if (!prefs.buyTheme(item.theme.id, item.price)) {
                            toast(context, context.getString(R.string.shop_not_enough))
                        }
                        sfx.play(SfxEngine.Sound.CLICK)
                    },
                    onFreeCoins = { showRewardedOrToast { prefs.addCoins(AdUnits.REWARDED_COINS) } },
                    onBack = { screen = Screen.Menu },
                )

                Screen.Stats -> StatsScreen(
                    theme = boardTheme,
                    player = player,
                    onBack = { screen = Screen.Menu },
                )

                Screen.Settings -> SettingsScreen(
                    theme = boardTheme,
                    settings = settings,
                    onSoundChange = { prefs.setSound(it) },
                    onHapticsChange = { prefs.setHaptics(it) },
                    onPersonalizedAdsChange = { consent.setConsent(it) },
                    onResetProgress = { prefs.resetProgress() },
                    onBack = { screen = Screen.Menu },
                )

                Screen.Tutorial -> TutorialScreen(
                    theme = boardTheme,
                    onBack = { screen = Screen.Menu },
                )
            }

            if (showConsentDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE605070F)),
                    contentAlignment = Alignment.Center,
                ) {
                    GameDialog(
                        title = stringResource(R.string.consent_title),
                        message = stringResource(R.string.consent_message),
                        confirmText = stringResource(R.string.consent_accept_all),
                        onConfirm = {
                            consent.setConsent(true)
                            showConsentDialog = false
                            Ads.init(application, consent)
                        },
                        dismissText = stringResource(R.string.consent_essential_only),
                        onDismiss = {
                            consent.setConsent(false)
                            showConsentDialog = false
                            Ads.init(application, consent)
                        },
                    )
                }
            }
        }
    }

}

private fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
