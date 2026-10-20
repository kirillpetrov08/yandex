package com.ayaks.focus

import android.app.Application

class FocusApplication : Application() {
    val sound: com.ayaks.focus.audio.FocusSound by lazy { com.ayaks.focus.audio.FocusSound() }
}
