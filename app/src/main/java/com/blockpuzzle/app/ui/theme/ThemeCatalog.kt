package com.blockpuzzle.app.ui.theme

import com.blockpuzzle.app.R

/** Магазин тем: цена в монетах. Первая тема бесплатна и доступна сразу. */
object ThemeCatalog {

    val items: List<ThemeItem> = listOf(
        ThemeItem(ClassicTheme, R.string.theme_default, 0),
        ThemeItem(NeonTheme, R.string.theme_neon, 60),
        ThemeItem(CandyTheme, R.string.theme_candy, 80),
        ThemeItem(OceanTheme, R.string.theme_ocean, 100),
        ThemeItem(ForestTheme, R.string.theme_forest, 140),
        ThemeItem(MonoTheme, R.string.theme_mono, 160),
        ThemeItem(GalaxyTheme, R.string.theme_galaxy, 200),
    )

    fun byId(id: String): ThemeItem = items.firstOrNull { it.theme.id == id } ?: items.first()
}
