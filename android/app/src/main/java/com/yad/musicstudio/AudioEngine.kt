package com.yad.musicstudio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

object AudioEngine {

    private const val TAG = "AudioEngine"
    const val STEPS = 16
    const val TRACKS = 16
    const val PIANO_KEYS = 24
    const val MAX_PATTERNS = 8

    var bpm: Int = 120
    var isPlaying: Boolean = false
        private set
    var isLooping: Boolean = true
    var masterVolume: Float = 0.8f
    var currentPattern: Int = 0

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<Int, Int>()
    private val volumes = FloatArray(TRACKS) { 0.8f }
    private val pans = FloatArray(TRACKS) { 0f }
    private val mutes = BooleanArray(TRACKS) { false }
    private val solos = BooleanArray(TRACKS) { false }
    private val effects = Array(TRACKS) { EffectChain() }
    private val fxSends = FloatArray(TRACKS) { 0.3f }

    // Pattern data: [pattern][track][step]
    private val patterns = Array(MAX_PATTERNS) {
        Array(TRACKS) { BooleanArray(STEPS) }
    }

    // Piano roll: [pattern][key][step]
    private val pianoRoll = Array(MAX_PATTERNS) {
        Array(PIANO_KEYS) { BooleanArray(STEPS) }
    }

    // Synth settings per track
    private val synths = Array(TRACKS) { SynthSettings() }

    private val handler = Handler(Looper.getMainLooper())
    private var currentStep = 0
    private var context: Context? = null

    // 16 drum sounds
    val trackNames = arrayOf(
        "Kick", "Snare", "Hi-Hat", "Clap",
        "Tom Hi", "Tom Lo", "Cymbal", "Rim",
        "Cowbell", "Shaker", "Conga", "Bongo",
        "Crash", "Ride", "Tambourine", "Woodblock"
    )

    fun init(ctx: Context) {
        context = ctx
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(32)
            .setAudioAttributes(attrs)
            .build()

        Log.i(TAG, "AudioEngine v2 initialized — $TRACKS tracks")
    }

    // ─── PATTERN ───
    fun getCurrentPattern(): Array<BooleanArray> = patterns[currentPattern]

    fun toggleStep(track: Int, step: Int) {
        if (track in 0 until TRACKS && step in 0 until STEPS) {
            patterns[currentPattern][track][step] = !patterns[currentPattern][track][step]
        }
    }

    fun clearPattern() {
        for (t in 0 until TRACKS) for (s in 0 until STEPS) {
            patterns[currentPattern][t][s] = false
            pianoRoll[currentPattern][t % PIANO_KEYS][s] = false
        }
    }

    // ─── PIANO ROLL ───
    fun getPianoRoll(): Array<BooleanArray> = pianoRoll[currentPattern]

    fun togglePianoKey(key: Int, step: Int) {
        if (key in 0 until PIANO_KEYS && step in 0 until STEPS) {
            pianoRoll[currentPattern][key][step] = !pianoRoll[currentPattern][key][step]
        }
    }

    // ─── MIXER ───
    fun setVolume(track: Int, vol: Float) { if (track in 0 until TRACKS) volumes[track] = vol.coerceIn(0f, 1f) }
    fun getVolume(track: Int) = volumes[track]

    fun setPan(track: Int, pan: Float) { if (track in 0 until TRACKS) pans[track] = pan.coerceIn(-1f, 1f) }
    fun getPan(track: Int) = pans[track]

    fun setMute(track: Int, m: Boolean) { if (track in 0 until TRACKS) mutes[track] = m }
    fun isMuted(track: Int) = mutes[track]

    fun setSolo(track: Int, s: Boolean) { if (track in 0 until TRACKS) solos[track] = s }
    fun isSolo(track: Int) = solos[track]

    fun setFxSend(track: Int, send: Float) { if (track in 0 until TRACKS) fxSends[track] = send.coerceIn(0f, 1f) }
    fun getFxSend(track: Int) = fxSends[track]

    fun setEffect(track: Int, effect: EffectType, enabled: Boolean) {
        if (track in 0 until TRACKS) {
            when (effect) {
                EffectType.REVERB -> effects[track].reverb = enabled
                EffectType.DELAY -> effects[track].delay = enabled
                EffectType.DISTORTION -> effects[track].distortion = enabled
                EffectType.CHORUS -> effects[track].chorus = enabled
                EffectType.FILTER -> effects[track].filter = enabled
            }
        }
    }

    // ─── SYNTH ───
    fun getSynth(track: Int): SynthSettings = synths[track]

    fun updateSynth(track: Int, settings: SynthSettings) {
        if (track in 0 until TRACKS) synths[track] = settings
    }

    // ─── PLAYBACK ───
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

    fun setBpm(newBpm: Int) { bpm = newBpm.coerceIn(40, 300) }

    private fun scheduleNextStep() {
        if (!isPlaying) return
        val stepDuration = (60_000L / bpm) / 4

        val anySolo = solos.any { it }

        // Play drum
        for (t in 0 until TRACKS) {
            if (mutes[t]) continue
            if (anySolo && !solos[t]) continue
            if (patterns[currentPattern][t][currentStep]) {
                playSound(t, volumes[t], pans[t])
            }
        }

        // Play piano
        for (k in 0 until PIANO_KEYS) {
            if (pianoRoll[currentPattern][k][currentStep]) {
                playNote(k + 48, 0.8f, 0f)  // offset for piano range
            }
        }

        currentStep = (currentStep + 1) % STEPS
        if (currentStep == 0 && !isLooping) {
            stop()
            return
        }
        handler.postDelayed({ scheduleNextStep() }, stepDuration)
    }

    private fun playSound(track: Int, volume: Float, pan: Float) {
        try {
            val soundId = soundIds[track]
            if (soundId != null && soundId != 0) {
                val leftVol = volume * (if (pan <= 0) 1f else 1f - pan)
                val rightVol = volume * (if (pan >= 0) 1f else 1f + pan)
                soundPool?.play(soundId, leftVol, rightVol, 1, 0, 1f)
            } else {
                generateTone(track, volume, pan)
            }
        } catch (e: Exception) {
            Log.e(TAG, "playSound error", e)
        }
    }

    private fun generateTone(track: Int, volume: Float, pan: Float) {
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = when (track) {
                    0, 4, 5 -> 150
                    1 -> 120
                    2 -> 50
                    3 -> 80
                    else -> 100
                }
                val numSamples = sampleRate * durationMs / 1000
                val samples = ShortArray(numSamples)

                val freq = when (track) {
                    0 -> 60.0
                    1 -> 200.0
                    2 -> 8000.0
                    3 -> 1000.0
                    4 -> 200.0
                    5 -> 120.0
                    6 -> 5000.0
                    7 -> 400.0
                    8 -> 800.0
                    9 -> 4000.0
                    10 -> 250.0
                    11 -> 400.0
                    12 -> 6000.0
                    13 -> 3000.0
                    14 -> 5000.0
                    15 -> 600.0
                    else -> 440.0
                }

                val synth = synths[track]
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    var envelope = 1.0 - (i.toDouble() / numSamples)
                    // ADSR sederhana
                    envelope *= adsr(t, synth)

                    var sample = 0.0
                    when (synth.waveform) {
                        Waveform.SINE -> sample = sin(2 * PI * freq * t)
                        Waveform.SQUARE -> sample = if (sin(2 * PI * freq * t) > 0) 1.0 else -1.0
                        Waveform.SAW -> sample = 2.0 * (t * freq - kotlin.math.floor(0.5 + t * freq))
                        Waveform.TRIANGLE -> sample = 2.0 * kotlin.math.abs(2.0 * (t * freq - kotlin.math.floor(0.5 + t * freq))) - 1.0
                    }

                    // Distortion
                    if (effects[track].distortion) {
                        sample = kotlin.math.tanh(sample * 3.0) / kotlin.math.tanh(3.0)
                    }

                    samples[i] = (sample * Short.MAX_VALUE * envelope * volume * 0.5)
                        .coerceIn(-32768.0, 32767.0).toInt().toShort()
                }

                val audioTrack = AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
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
                Log.e(TAG, "generateTone error", e)
            }
        }.start()
    }

    private fun adsr(t: Double, synth: SynthSettings): Double {
        return when {
            t < synth.attack -> t / synth.attack
            t < synth.attack + synth.decay -> 1.0 - (1.0 - synth.sustain) * ((t - synth.attack) / synth.decay)
            else -> synth.sustain
        }
    }

    fun playNote(midiNote: Int, volume: Float = 0.8f, pan: Float = 0f) {
        val freq = 440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)
        Thread {
            try {
                val sampleRate = 44100
                val durationMs = 500
                val numSamples = sampleRate * durationMs / 1000
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = 1.0 - (i.toDouble() / numSamples)
                    val sample = sin(2 * PI * freq * t) * envelope * volume * 0.5
                    samples[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(-32768, 32767).toShort()
                }

                val audioTrack = AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
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
                Log.e(TAG, "playNote error", e)
            }
        }.start()
    }

    fun previewSound(track: Int) {
        playSound(track, volumes[track], pans[track])
    }

    // ─── SAVE / LOAD ───
    fun saveProject(name: String): String {
        try {
            val json = JSONObject()
            json.put("name", name)
            json.put("bpm", bpm)
            json.put("master_volume", masterVolume)
            json.put("current_pattern", currentPattern)
            json.put("timestamp", System.currentTimeMillis())

            // Patterns
            val patsArr = JSONArray()
            for (p in 0 until MAX_PATTERNS) {
                val patObj = JSONObject()
                val tracksArr = JSONArray()
                for (t in 0 until TRACKS) {
                    val stepsArr = JSONArray()
                    for (s in 0 until STEPS) stepsArr.put(patterns[p][t][s])
                    tracksArr.put(stepsArr)
                }
                patObj.put("drums", tracksArr)

                val pianoArr = JSONArray()
                for (k in 0 until PIANO_KEYS) {
                    val stepsArr = JSONArray()
                    for (s in 0 until STEPS) stepsArr.put(pianoRoll[p][k][s])
                    pianoArr.put(stepsArr)
                }
                patObj.put("piano", pianoArr)
                patsArr.put(patObj)
            }
            json.put("patterns", patsArr)

            // Mixer
            val mixerArr = JSONArray()
            for (t in 0 until TRACKS) {
                val m = JSONObject()
                m.put("volume", volumes[t])
                m.put("pan", pans[t])
                m.put("mute", mutes[t])
                m.put("solo", solos[t])
                m.put("fx_send", fxSends[t])
                mixerArr.put(m)
            }
            json.put("mixer", mixerArr)

            val dir = File(context?.filesDir, "projects")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$name.yad")
            file.writeText(json.toString(2))
            Log.i(TAG, "Saved: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "saveProject error", e)
            return ""
        }
    }

    fun loadProject(path: String): Boolean {
        try {
            val file = File(path)
            if (!file.exists()) return false
            val json = JSONObject(file.readText())
            bpm = json.optInt("bpm", 120)
            masterVolume = json.optDouble("master_volume", 0.8).toFloat()
            currentPattern = json.optInt("current_pattern", 0)

            val patsArr = json.getJSONArray("patterns")
            for (p in 0 until minOf(patsArr.length(), MAX_PATTERNS)) {
                val patObj = patsArr.getJSONObject(p)
                val drums = patObj.getJSONArray("drums")
                for (t in 0 until minOf(drums.length(), TRACKS)) {
                    val steps = drums.getJSONArray(t)
                    for (s in 0 until minOf(steps.length(), STEPS)) {
                        patterns[p][t][s] = steps.getBoolean(s)
                    }
                }
                val piano = patObj.optJSONArray("piano")
                if (piano != null) {
                    for (k in 0 until minOf(piano.length(), PIANO_KEYS)) {
                        val steps = piano.getJSONArray(k)
                        for (s in 0 until minOf(steps.length(), STEPS)) {
                            pianoRoll[p][k][s] = steps.getBoolean(s)
                        }
                    }
                }
            }

            val mixerArr = json.optJSONArray("mixer")
            if (mixerArr != null) {
                for (t in 0 until minOf(mixerArr.length(), TRACKS)) {
                    val m = mixerArr.getJSONObject(t)
                    volumes[t] = m.optDouble("volume", 0.8).toFloat()
                    pans[t] = m.optDouble("pan", 0.0).toFloat()
                    mutes[t] = m.optBoolean("mute", false)
                    solos[t] = m.optBoolean("solo", false)
                    fxSends[t] = m.optDouble("fx_send", 0.3).toFloat()
                }
            }
            Log.i(TAG, "Loaded: ${file.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "loadProject error", e)
            return false
        }
    }

    fun loadDemoPattern() {
        clearPattern()
        for (s in 0 until STEPS step 4) patterns[currentPattern][0][s] = true
        patterns[currentPattern][1][4] = true
        patterns[currentPattern][1][12] = true
        for (s in 0 until STEPS step 2) patterns[currentPattern][2][s] = true
        patterns[currentPattern][3][12] = true
        patterns[currentPattern][6][2] = true
        patterns[currentPattern][6][10] = true
    }

    // ─── EXPORT WAV ───
    fun exportWav(): String {
        try {
            val sampleRate = 44100
            val beatMs = 60_000.0 / bpm
            val stepMs = beatMs / 4
            val totalMs = (stepMs * STEPS).toInt()
            val numSamples = sampleRate * totalMs / 1000
            val output = FloatArray(numSamples)

            // Render setiap step
            for (step in 0 until STEPS) {
                val startSample = (step * stepMs * sampleRate / 1000).toInt()
                for (t in 0 until TRACKS) {
                    if (!patterns[currentPattern][t][step]) continue
                    if (mutes[t]) continue

                    val freq = 200.0 + t * 100
                    val durSamples = sampleRate / 4  // 250ms per hit
                    for (i in 0 until minOf(durSamples, numSamples - startSample)) {
                        val t2 = i.toDouble() / sampleRate
                        val env = 1.0 - (i.toDouble() / durSamples)
                        val sample = sin(2 * PI * freq * t2) * env * volumes[t] * 0.3
                        output[startSample + i] += sample.toFloat()
                    }
                }
            }

            // Normalize
            var peak = 0f
            for (s in output) if (kotlin.math.abs(s) > peak) peak = kotlin.math.abs(s)
            if (peak > 0) for (i in output.indices) output[i] /= peak

            // Write WAV
            val dir = File(context?.filesDir, "exports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "export_${System.currentTimeMillis()}.wav")

            val dataSize = numSamples * 2
            val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
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
                val buf = java.nio.ByteBuffer.allocate(dataSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                for (s in output) buf.putShort((s * 32767).toInt().coerceIn(-32768, 32767).toShort())
                out.write(buf.array())
            }
            Log.i(TAG, "Exported: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "exportWav error", e)
            return ""
        }
    }

    fun listProjects(): List<File> {
        val dir = File(context?.filesDir, "projects")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.filter { it.extension == "yad" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    data class SynthSettings(
        var waveform: Waveform = Waveform.SINE,
        var attack: Double = 0.01,
        var decay: Double = 0.1,
        var sustain: Double = 0.7,
        var release: Double = 0.2,
        var cutoff: Double = 8000.0,
        var resonance: Double = 0.5
    )

    enum class Waveform { SINE, SQUARE, SAW, TRIANGLE }
    enum class EffectType { REVERB, DELAY, DISTORTION, CHORUS, FILTER }

    data class EffectChain(
        var reverb: Boolean = false,
        var delay: Boolean = false,
        var distortion: Boolean = false,
        var chorus: Boolean = false,
        var filter: Boolean = false
    )
}
