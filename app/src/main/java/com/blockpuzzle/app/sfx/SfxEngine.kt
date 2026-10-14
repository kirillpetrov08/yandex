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

    /** Простая аддитивная генерация: тон + экспоненциальное затухание + обертоны. */
    private fun synthesize(sound: Sound): ShortArray {
        val durationSeconds = sound.durationMs / 1000.0
        val totalSamples = (SAMPLE_RATE * durationSeconds).toInt().coerceAtLeast(1)
        val data = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val envelope = exp(-sound.decay * progress)
            val freq = sound.startFrequency + (sound.endFrequency - sound.startFrequency) * progress
            val fundamental = sin(2 * PI * freq * t)
            val harmonic = 0.35 * sin(2 * PI * freq * 2.0 * t)
            val value = (fundamental + harmonic) * envelope * sound.volume
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
    }

    companion object {
        private const val SAMPLE_RATE = 44_100
    }
}
