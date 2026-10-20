package com.ayaks.focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.ayaks.focus.data.FocusStore
import com.ayaks.focus.ui.FocusApp
import com.ayaks.focus.ui.FocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = FocusStore(applicationContext)
        setContent {
            FocusTheme {
                FocusApp(store = remember { store })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) (application as FocusApplication).sound.release()
    }
}
