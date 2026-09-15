package com.yad.musicstudio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.sin

/**
 * Mp3Exporter — Export pattern ke MP3.
 *
 * Menggunakan MediaCodec AAC encoder + MediaMuxer.
 * Output: .m4a (AAC) — format MP3 butuh library eksternal (LAME).
 */
object Mp3Exporter {

    private const val TAG = "Mp3Exporter"

    fun exportToM4a(context: Context, patternIndex: Int, filename: String = ""): String {
        return try {
            val sampleRate = 44100
            val bitRate = 192000
            val channels = 1

            // Render audio dulu ke PCM
            val pcmData = renderPattern(patternIndex, sampleRate)

            // Setup encoder
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels
            ).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE,
                    MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }

            val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val dir = File(context.filesDir, "exports")
            if (!dir.exists()) dir.mkdirs()

            val name = if (filename.isNotEmpty()) filename
                else "export_${System.currentTimeMillis()}.m4a"
            val outputFile = File(dir, name)

            val muxer = MediaMuxer(outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var trackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            var inputOffset = 0

            while (true) {
                // Feed input
                if (inputOffset < pcmData.size) {
                    val inIdx = encoder.dequeueInputBuffer(10000)
                    if (inIdx >= 0) {
                        val inBuf = encoder.getInputBuffer(inIdx)!!
                        inBuf.clear()
                        val remaining = pcmData.size - inputOffset
                        val chunkSize = minOf(remaining, inBuf.remaining())
                        inBuf.put(pcmData, inputOffset, chunkSize)
                        inputOffset += chunkSize

                        val ptsUs = (inputOffset.toLong() * 1_000_000L) / (sampleRate * 2)
                        encoder.queueInputBuffer(inIdx, 0, chunkSize, ptsUs, 0)
                    }
                } else {
                    // End of stream
                    val inIdx = encoder.dequeueInputBuffer(10000)
                    if (inIdx >= 0) {
                        encoder.queueInputBuffer(inIdx, 0, 0, 0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputOffset++  // prevent loop
                    }
                }

                // Drain output
                val outIdx = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                when {
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    }
                    outIdx >= 0 -> {
                        val outBuf = encoder.getOutputBuffer(outIdx)!!
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size > 0 && muxerStarted) {
                            outBuf.position(bufferInfo.offset)
                            outBuf.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outIdx, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
            }

            encoder.stop()
            encoder.release()
            muxer.stop()
            muxer.release()

            Log.i(TAG, "Exported: ${outputFile.absolutePath}")
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "export error", e)
            ""
        }
    }

    /**
     * Render pattern ke PCM 16-bit.
     */
    private fun renderPattern(patternIndex: Int, sampleRate: Int): ByteArray {
        val beatMs = 60_000.0 / AudioEngine.bpm
        val stepMs = beatMs / 4
        val totalMs = (stepMs * AudioEngine.STEPS).toInt()
        val numSamples = sampleRate * totalMs / 1000
        val output = FloatArray(numSamples)

        // Drums
        for (step in 0 until AudioEngine.STEPS) {
            val startSample = (step * stepMs * sampleRate / 1000).toInt()
            for (t in 0 until AudioEngine.TRACKS) {
                if (!AudioEngine.getCurrentPattern()[t][step]) continue
                if (AudioEngine.isMuted(t)) continue

                val freq = 200.0 + t * 100
                val durSamples = sampleRate / 4
                for (i in 0 until minOf(durSamples, numSamples - startSample)) {
                    val t2 = i.toDouble() / sampleRate
                    val env = 1.0 - (i.toDouble() / durSamples)
                    val sample = sin(2 * PI * freq * t2) * env *
                            AudioEngine.getVolume(t) * 0.3
                    output[startSample + i] += sample.toFloat()
                }
            }
        }

        // Piano
        val notes = PianoRollData.getNotes(patternIndex)
        for (note in notes) {
            val startSample = (note.step * stepMs * sampleRate / 1000).toInt()
            val freq = 440.0 * Math.pow(2.0, (note.key + 48 - 69) / 12.0)
            val durMs = note.length * stepMs
            val durSamples = (durMs * sampleRate / 1000).toInt()

            for (i in 0 until minOf(durSamples, numSamples - startSample)) {
                val t2 = i.toDouble() / sampleRate
                val env = 1.0 - (i.toDouble() / durSamples)
                val vol = (note.velocity / 127f)
                val sample = sin(2 * PI * freq * t2) * env * vol * 0.3
                output[startSample + i] += sample.toFloat()
            }
        }

        // Limiter + normalize
        var peak = 0f
        for (s in output) if (kotlin.math.abs(s) > peak) peak = kotlin.math.abs(s)
        if (peak > 0) for (i in output.indices) output[i] /= peak

        // Convert to byte array (16-bit PCM, little endian)
        val pcm = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            val s = (output[i] * 32767).toInt().coerceIn(-32768, 32767)
            pcm[i * 2] = (s and 0xFF).toByte()
            pcm[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
        }
        return pcm
    }
}
