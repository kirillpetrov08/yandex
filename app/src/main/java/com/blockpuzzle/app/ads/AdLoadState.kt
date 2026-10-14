package com.blockpuzzle.app.ads

/** Состояние загрузки рекламного объявления — используется UI для кнопок награды. */
enum class AdLoadState {
    IDLE,
    LOADING,
    READY,
    FAILED;

    val isReady: Boolean get() = this == READY
}
