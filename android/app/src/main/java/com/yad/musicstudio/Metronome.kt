package com.yad.musicstudio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

object Metronome {

    private const val TAG = "Metronome"

    var enabled: Boolean = false
    var volume: Float = 0.7f
    var accentFirstBeat: Boolean = true

    private val handler = Handler(Looper.getMainLooper())
    private var currentBeat = 0

    fun click(beat: Int, beatsPerBar: Int = 4) {
        if (!enabled) return

        val freq = if (beat == 0 && accentFirstBeat) 1000.0 else 800.0
        val durationMs = 30

        Thread {
            try {
                val sampleRate = 44100
                val numSamples = sampleRate * durationMs / 1000
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // FIX: hapus .pow(2), pakai perkalian
                    val envelope = (1.0 - (i.toDouble() / numSamples)) *
                                   (1.0 - (i.toDouble() / numSamples))
                    val sample = sin(2 * PI * freq * t) * envelope * volume
                    samples[i] = (sample * Short.MAX_VALUE * 0.3)
                        .coerceIn(-32768.0, 32767.0).toInt().toShort()
                }

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    samples.size * 2,
                    AudioTrack.MODE_STATIC
                )
                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong())
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "click error", e)
            }
        }.start()
    }

    fun reset() {
        currentBeat = 0
    }

    fun advance(step: Int, stepsPerBeat: Int = 4, beatsPerBar: Int = 4): Int {
        if (step % stepsPerBeat == 0) {
            val beat = (step / stepsPerBeat) % beatsPerBar
            currentBeat = beat
            click(beat, beatsPerBar)
            return beat
        }
        return currentBeat
    }

    fun toggle() {
        enabled = !enabled
        Log.d(TAG, "Metronome: $enabled")
    }
}
