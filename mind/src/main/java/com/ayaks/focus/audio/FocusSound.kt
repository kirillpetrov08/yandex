package com.ayaks.focus.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Small procedural UI cues; no bundled audio files or network streaming. */
class FocusSound {
    private val tracks = mutableMapOf<Cue, AudioTrack>()
    @Volatile private var enabled = true

    fun setEnabled(value: Boolean) { enabled = value }

    fun play(cue: Cue) {
        if (!enabled) return
        runCatching {
            val track = tracks.getOrPut(cue) { create(cue) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.reloadStaticData()
            track.play()
        }
    }

    fun release() {
        tracks.values.forEach { runCatching { it.release() } }
        tracks.clear()
    }

    private fun create(cue: Cue): AudioTrack {
        val count = (RATE * cue.durationMs / 1000).coerceAtLeast(1)
        val samples = ShortArray(count)
        for (i in samples.indices) {
            val t = i.toDouble() / RATE
            val p = i.toDouble() / count
            val phase = 2.0 * PI * (cue.startHz * t + 0.5 * (cue.endHz - cue.startHz) * t * p)
            val envelope = (p / 0.04).coerceIn(0.0, 1.0) * exp(-cue.decay * p)
            val wave = sin(phase) * 0.76 + sin(phase * 2.01) * 0.18 + sin(phase * 3.02) * 0.06
            samples[i] = (wave * envelope * cue.volume * Short.MAX_VALUE).toInt().toShort()
        }
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
            .apply { write(samples, 0, samples.size) }
    }

    enum class Cue(val durationMs: Int, val startHz: Double, val endHz: Double, val decay: Double, val volume: Double) {
        TAP(42, 360.0, 430.0, 8.0, 0.17),
        PICK(72, 470.0, 700.0, 5.5, 0.20),
        CORRECT(145, 620.0, 920.0, 4.0, 0.25),
        WRONG(150, 220.0, 150.0, 5.0, 0.22),
        FINISH(300, 520.0, 1040.0, 3.0, 0.23),
    }

    companion object { private const val RATE = 44_100 }
}
