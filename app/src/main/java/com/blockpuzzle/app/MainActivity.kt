package com.blockpuzzle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.blockpuzzle.app.ui.AppRoot

/**
 * Единственная Activity: игра полностью на Compose.
 * Дополнительные Activity не нужны, поэтому рекламные блоки живут в одном процессе.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BlockPuzzleApp
        setContent {
            AppRoot(
                prefs = app.prefs,
                consent = app.consent,
                sfx = app.sfx,
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Освобождаем аудиоресурсы только при реальном выходе из приложения.
        if (isFinishing) (application as BlockPuzzleApp).sfx.release()
    }
}
