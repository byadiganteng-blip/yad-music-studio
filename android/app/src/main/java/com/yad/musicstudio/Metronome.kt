package com.yad.musicstudio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.PI
import kotlin.math.sin

/**
 * Metronome — klik audio dengan aksen beat.
 * 
 * Time signature: 4/4 default
 * Aksen: beat 1 = tinggi, beat 2-4 = rendah
 */
object Metronome {

    private const val TAG = "Metronome"

    var enabled: Boolean = false
    var volume: Float = 0.7f
    var accentFirstBeat: Boolean = true

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var currentBeat = 0

    /**
     * Mainkan klik untuk beat tertentu.
     * @param beat 0-based (0 = beat pertama)
     * @param beatsPerBar jumlah beat per bar (default 4)
     */
    fun click(beat: Int, beatsPerBar: Int = 4) {
        if (!enabled) return

        // Frekuensi: beat 1 = 1000Hz, beat lain = 800Hz
        val freq = if (beat == 0 && accentFirstBeat) 1000.0 else 800.0
        val durationMs = 30

        Thread {
            try {
                val sampleRate = 44100
                val numSamples = sampleRate * durationMs / 1000
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = (1.0 - (i.toDouble() / numSamples)).pow(2) // decay
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

    /**
     * Reset beat counter (dipanggil saat start playback).
     */
    fun reset() {
        currentBeat = 0
    }

    /**
     * Advance beat (dipanggil setiap step).
     */
    fun advance(step: Int, stepsPerBeat: Int = 4, beatsPerBar: Int = 4): Int {
        // Step 0, 4, 8, 12 = beat 0, 1, 2, 3
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
