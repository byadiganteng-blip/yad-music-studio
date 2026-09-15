package com.yad.musicstudio

import android.content.Context
import android.util.Log
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

/**
 * StemExporter — Export per track (1 file per drum).
 */
object StemExporter {

    private const val TAG = "StemExporter"

    fun exportAllStems(context: Context): List<String> {
        val results = mutableListOf<String>()
        val dir = File(context.filesDir, "exports/stems")
        if (!dir.exists()) dir.mkdirs()

        for (t in 0 until AudioEngine.TRACKS) {
            val file = exportTrack(context, t, dir)
            if (file.isNotEmpty()) results.add(file)
        }
        return results
    }

    private fun exportTrack(context: Context, track: Int, dir: File): String {
        return try {
            val sampleRate = 44100
            val beatMs = 60_000.0 / AudioEngine.bpm
            val stepMs = beatMs / 4
            val totalMs = (stepMs * AudioEngine.STEPS).toInt()
            val numSamples = sampleRate * totalMs / 1000
            val output = FloatArray(numSamples)

            for (step in 0 until AudioEngine.STEPS) {
                if (!AudioEngine.getCurrentPattern()[track][step]) continue
                val startSample = (step * stepMs * sampleRate / 1000).toInt()
                val freq = 200.0 + track * 100
                val durSamples = sampleRate / 4
                for (i in 0 until minOf(durSamples, numSamples - startSample)) {
                    val t2 = i.toDouble() / sampleRate
                    val env = 1.0 - (i.toDouble() / durSamples)
                    val sample = sin(2 * PI * freq * t2) * env *
                            AudioEngine.getVolume(track) * 0.5
                    output[startSample + i] += sample.toFloat()
                }
            }

            val file = File(dir, "stem_${AudioEngine.trackNames[track].replace(" ", "_")}.wav")
            writeWav(file, output, sampleRate)
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "exportTrack $track error", e)
            ""
        }
    }

    private fun writeWav(file: File, samples: FloatArray, sampleRate: Int) {
        val dataSize = samples.size * 2
        val header = java.nio.ByteBuffer.allocate(44)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataSize)
        }

        file.outputStream().use { out ->
            out.write(header.array())
            val buf = java.nio.ByteBuffer.allocate(dataSize)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            for (s in samples) {
                buf.putShort((s * 32767).toInt()
                    .coerceIn(-32768, 32767).toShort())
            }
            out.write(buf.array())
        }
    }
}
