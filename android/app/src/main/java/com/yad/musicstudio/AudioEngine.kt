package com.yad.musicstudio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object AudioEngine {

    private const val TAG = "AudioEngine"
    private const val STEPS = 16
    private const val TRACKS = 8

    var bpm: Int = 120
    var isPlaying: Boolean = false
        private set

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<Int, Int>()
    private val volumes = FloatArray(TRACKS) { 0.8f }
    private val pattern = Array(TRACKS) { BooleanArray(STEPS) }

    private val handler = Handler(Looper.getMainLooper())
    private var currentStep = 0
    private var context: Context? = null

    private val trackNames = arrayOf(
        "Kick", "Snare", "Hi-Hat", "Clap",
        "Tom", "Cymbal", "Rim", "Cowbell"
    )

    // ─── Sound resources (pakai built-in Android sound dulu) ───
    private val soundResources = arrayOf(
        android.R.raw.media_volume,  // placeholder
        android.R.raw.media_volume,
        android.R.raw.media_volume,
        android.R.raw.media_volume,
        android.R.raw.media_volume,
        android.R.raw.media_volume,
        android.R.raw.media_volume,
        android.R.raw.media_volume,
    )

    fun init(ctx: Context) {
        context = ctx

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(16)
            .setAudioAttributes(attrs)
            .build()

        // Load sounds (nanti ganti dengan sample real)
        // Untuk sekarang pakai system tone
        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) Log.i(TAG, "Sound loaded")
        }

        // Placeholder: pakai ToneGenerator atau sample dari res/raw
        // Di production, load dari res/raw/kick.wav, snare.wav, dll

        Log.i(TAG, "AudioEngine initialized")
    }

    fun getTracks(): List<Track> {
        return (0 until TRACKS).map { i ->
            Track(
                index = i,
                name = trackNames[i],
                steps = pattern[i].copyOf(),
                volume = volumes[i]
            )
        }
    }

    fun toggleStep(track: Int, step: Int) {
        if (track in 0 until TRACKS && step in 0 until STEPS) {
            pattern[track][step] = !pattern[track][step]
        }
    }

    fun setVolume(track: Int, vol: Float) {
        if (track in 0 until TRACKS) {
            volumes[track] = vol.coerceIn(0f, 1f)
        }
    }

    fun clearAll() {
        for (t in 0 until TRACKS) {
            for (s in 0 until STEPS) {
                pattern[t][s] = false
            }
        }
    }

    fun play() {
        if (isPlaying) return
        isPlaying = true
        currentStep = 0
        scheduleNextStep()
    }

    fun stop() {
        isPlaying = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun scheduleNextStep() {
        if (!isPlaying) return

        val stepDuration = (60_000L / bpm) / 4  // 16th note

        // Play sounds for current step
        for (t in 0 until TRACKS) {
            if (pattern[t][currentStep]) {
                playSound(t, volumes[t])
            }
        }

        currentStep = (currentStep + 1) % STEPS
        handler.postDelayed({ scheduleNextStep() }, stepDuration)
    }

    private fun playSound(track: Int, volume: Float) {
        try {
            val soundId = soundIds[track]
            if (soundId != null) {
                soundPool?.play(soundId, volume, volume, 1, 0, 1f)
            } else {
                // Fallback: generate simple tone
                playSimpleTone(track, volume)
            }
        } catch (e: Exception) {
            Log.e(TAG, "playSound error", e)
        }
    }

    private fun playSimpleTone(track: Int, volume: Float) {
        // Generate tone sederhana pakai AudioTrack
        // (Untuk placeholder — di production pakai sample real)
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = when (track) {
                    0 -> 100  // kick
                    1 -> 80   // snare
                    2 -> 40   // hihat
                    3 -> 60   // clap
                    else -> 50
                }
                val numSamples = sampleRate * durationMs / 1000
                val samples = ShortArray(numSamples)

                val freq = when (track) {
                    0 -> 60.0    // kick
                    1 -> 200.0   // snare
                    2 -> 8000.0  // hihat
                    3 -> 1000.0  // clap
                    4 -> 150.0   // tom
                    5 -> 5000.0  // cymbal
                    6 -> 400.0   // rim
                    7 -> 800.0   // cowbell
                    else -> 440.0
                }

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = 1.0 - (i.toDouble() / numSamples)
                    val sample = (kotlin.math.sin(2 * Math.PI * freq * t) *
                            Short.MAX_VALUE * envelope * volume).toInt()
                    samples[i] = sample.coerceIn(-32768, 32767).toShort()
                }

                val audioTrack = android.media.AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    samples.size * 2,
                    android.media.AudioTrack.MODE_STATIC
                )
                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                Thread.sleep(durationMs.toLong())
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "playSimpleTone error", e)
            }
        }.start()
    }

    fun previewSound(track: Int) {
        playSound(track, volumes[track])
    }

    // ─── SAVE / LOAD ───
    fun savePattern(name: String) {
        try {
            val json = JSONObject()
            json.put("bpm", bpm)
            json.put("name", name)
            json.put("timestamp", System.currentTimeMillis())

            val tracksArr = JSONArray()
            for (t in 0 until TRACKS) {
                val trackObj = JSONObject()
                trackObj.put("name", trackNames[t])
                trackObj.put("volume", volumes[t])
                val stepsArr = JSONArray()
                for (s in 0 until STEPS) {
                    stepsArr.put(pattern[t][s])
                }
                trackObj.put("steps", stepsArr)
                tracksArr.put(trackObj)
            }
            json.put("tracks", tracksArr)

            val dir = File(context?.filesDir, "patterns")
            if (!dir.exists()) dir.mkdirs()
            File(dir, name).writeText(json.toString(2))
            Log.i(TAG, "Saved: $name")
        } catch (e: Exception) {
            Log.e(TAG, "savePattern error", e)
        }
    }

    fun loadLatest(): String? {
        try {
            val dir = File(context?.filesDir, "patterns")
            if (!dir.exists()) return null
            val latest = dir.listFiles()
                ?.filter { it.extension == "json" }
                ?.maxByOrNull { it.lastModified() }
                ?: return null

            val json = JSONObject(latest.readText())
            bpm = json.optInt("bpm", 120)

            val tracksArr = json.getJSONArray("tracks")
            for (t in 0 until minOf(tracksArr.length(), TRACKS)) {
                val trackObj = tracksArr.getJSONObject(t)
                volumes[t] = trackObj.optDouble("volume", 0.8).toFloat()
                val stepsArr = trackObj.getJSONArray("steps")
                for (s in 0 until minOf(stepsArr.length(), STEPS)) {
                    pattern[t][s] = stepsArr.getBoolean(s)
                }
            }
            return latest.name
        } catch (e: Exception) {
            Log.e(TAG, "loadLatest error", e)
            return null
        }
    }

    fun loadDemoPattern() {
        // Pattern demo: 4-on-the-floor + snare + hihat
        clearAll()

        // Kick on every 4 steps
        for (s in 0 until STEPS step 4) pattern[0][s] = true
        // Snare on 4 and 12
        pattern[1][4] = true; pattern[1][12] = true
        // Hi-hat on every 2 steps
        for (s in 0 until STEPS step 2) pattern[2][s] = true
        // Clap on 12
        pattern[3][12] = true
    }

    data class Track(
        val index: Int,
        val name: String,
        val steps: BooleanArray,
        val volume: Float
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Track) return false
            return index == other.index
        }
        override fun hashCode(): Int = index
    }
}
