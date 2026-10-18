package com.blockpuzzle.app.sfx

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Процедурный звук: короткие эффекты синтезируются в PCM прямо в приложении,
 * поэтому в APK нет ни одного аудиофайла (меньше размер, ноль лицензий на сэмплы).
 *
 * Все методы безопасны: при любой ошибке звук просто не играет.
 */
class SfxEngine {

    private val cache = HashMap<Sound, AudioTrack>()
    @Volatile
    private var enabled = true

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun play(sound: Sound) {
        if (!enabled) return
        try {
            val track = cache.getOrPut(sound) { buildTrack(sound) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            // STATIC-трек переиспользуется: перезагружаем данные и играем заново.
            track.reloadStaticData()
            track.play()
        } catch (_: Throwable) {
            // звук не критичен для игры
        }
    }

    fun release() {
        cache.values.forEach { runCatching { it.release() } }
        cache.clear()
    }

    private fun buildTrack(sound: Sound): AudioTrack {
        val samples = synthesize(sound)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }

    /** Богатый короткий синтез: удар, гармоники и отдельная форма огибающей по событию. */
    private fun synthesize(sound: Sound): ShortArray {
        val durationSeconds = sound.durationMs / 1000.0
        val totalSamples = (SAMPLE_RATE * durationSeconds).toInt().coerceAtLeast(1)
        val data = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val attack = (progress / 0.035).coerceIn(0.0, 1.0)
            val envelope = attack * exp(-sound.decay * progress)
            // Интегрированный sweep: заметно более гладкий pitch bend, без фазового рывка.
            val phase = 2.0 * PI * (
                sound.startFrequency * t +
                    0.5 * (sound.endFrequency - sound.startFrequency) * t * progress
                )
            val fundamental = sin(phase)
            val second = sin(phase * 2.0 + 0.16)
            val third = sin(phase * 3.0 + 0.31)
            val sub = sin(phase * 0.5)
            val accent = when (sound) {
                Sound.PLACE, Sound.PICKUP -> 0.22 * second + 0.09 * third
                Sound.CLEAR -> 0.27 * second + 0.18 * sin(phase * 1.5)
                Sound.COMBO -> 0.3 * second + 0.2 * third + 0.12 * sin(phase * 1.5)
                Sound.FAIL -> 0.26 * sub + 0.08 * second
                Sound.COIN -> 0.2 * second + 0.16 * third
                Sound.CLICK -> 0.12 * second
                Sound.REJECT -> 0.18 * second + 0.13 * sub
            }
            val transient = when (sound) {
                Sound.PLACE -> sin(2.0 * PI * 1550.0 * t) * exp(-progress * 34.0) * 0.14
                Sound.CLEAR, Sound.COMBO -> sin(2.0 * PI * 2100.0 * t) * exp(-progress * 16.0) * 0.08
                Sound.REJECT -> sin(2.0 * PI * 95.0 * t) * exp(-progress * 11.0) * 0.24
                else -> 0.0
            }
            val value = (fundamental * 0.72 + accent + transient) * envelope * sound.volume
            data[i] = (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return data
    }

    enum class Sound(
        val durationMs: Int,
        val startFrequency: Double,
        val endFrequency: Double,
        val decay: Double,
        val volume: Double,
    ) {
        PLACE(70, 420.0, 520.0, 6.0, 0.35),
        CLEAR(220, 660.0, 990.0, 4.0, 0.40),
        COMBO(320, 880.0, 1320.0, 3.2, 0.45),
        FAIL(220, 220.0, 110.0, 3.0, 0.35),
        COIN(160, 1046.0, 1568.0, 4.5, 0.35),
        CLICK(40, 300.0, 260.0, 9.0, 0.28),
        PICKUP(85, 420.0, 690.0, 5.2, 0.24),
        REJECT(95, 185.0, 125.0, 7.0, 0.24),
    }

    companion object {
        private const val SAMPLE_RATE = 44_100
    }
}
