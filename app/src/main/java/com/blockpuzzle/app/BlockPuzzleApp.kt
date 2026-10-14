package com.blockpuzzle.app

import android.app.Application
import com.blockpuzzle.app.ads.Ads
import com.blockpuzzle.app.ads.ConsentManager
import com.blockpuzzle.app.data.Prefs
import com.blockpuzzle.app.sfx.SfxEngine

/**
 * Точка входа приложения.
 *
 * Инициализация рекламы вынесена в [Ads.init] и вызывается только после того,
 * как известно согласие пользователя (или сразу, если выбор уже сделан ранее):
 * порядок «сначала приватность, потом SDK» обязателен для РСЯ.
 */
class BlockPuzzleApp : Application() {

    lateinit var prefs: Prefs
        private set
    lateinit var consent: ConsentManager
        private set
    lateinit var sfx: SfxEngine
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        consent = ConsentManager(prefs)
        sfx = SfxEngine().apply { setEnabled(prefs.settings.value.sound) }

        if (prefs.settings.value.consentDecided) {
            // Согласие уже получено в прошлых запусках — можно инициализировать SDK сразу,
            // это ускоряет первый показ рекламы и повышает доход.
            Ads.init(this, consent)
        }
    }
}
