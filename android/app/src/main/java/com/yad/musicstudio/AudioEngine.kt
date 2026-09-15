package com.yad.musicstudio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * AudioEngine v4 — dengan suara asli dari resources.
 */
object AudioEngine {

    private const val TAG = "AudioEngine"
    const val STEPS = 16
    const val TRACKS = 16
    const val PIANO_KEYS = 24
    const val MAX_PATTERNS = 8

    var bpm: Int = 120
        private set
    var isPlaying: Boolean = false
        private set
    var isLooping: Boolean = true
    var masterVolume: Float = 0.8f
    var currentPattern: Int = 0
    var currentStep: Int = -1
        private set

    private var soundPool: SoundPool? = null
    private val drumSoundIds = mutableMapOf<Int, Int>()
    private val pianoSoundIds = mutableMapOf<String, Int>()
    private val volumes = FloatArray(TRACKS) { 0.8f }
    private val pans = FloatArray(TRACKS) { 0f }
    private val mutes = BooleanArray(TRACKS) { false }
    private val solos = BooleanArray(TRACKS) { false }

    private val patterns = Array(MAX_PATTERNS) {
        Array(TRACKS) { BooleanArray(STEPS) }
    }
    private val pianoRoll = Array(MAX_PATTERNS) {
        Array(PIANO_KEYS) { BooleanArray(STEPS) }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var context: Context? = null
    private var soundLoaded = false

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

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                Log.d(TAG, "Sound loaded OK")
            } else {
                Log.e(TAG, "Sound load failed: $status")
            }
        }

        // Load drum sounds dari resources
        loadDrumSounds()

        // Load piano notes
        loadPianoSounds()

        Logger.i(TAG, "AudioEngine v4 initialized")
    }

    private fun loadDrumSounds() {
        val ctx = context ?: return
        
        val drumResMap = mapOf(
            0 to R.raw.drum_kick,
            1 to R.raw.drum_snare,
            2 to R.raw.drum_hihat_closed,
            3 to R.raw.drum_clap,
            4 to R.raw.drum_tom_hi,
            5 to R.raw.drum_tom_lo,
            6 to R.raw.drum_cymbal,
            7 to R.raw.drum_rim,
            8 to R.raw.drum_cowbell,
            9 to R.raw.drum_shaker,
            10 to R.raw.drum_conga,
            11 to R.raw.drum_bongo,
            12 to R.raw.drum_crash,
            13 to R.raw.drum_ride,
            14 to R.raw.drum_tambourine,
            15 to R.raw.drum_woodblock
        )

        for ((idx, resId) in drumResMap) {
            try {
                val id = soundPool?.load(ctx, resId, 1) ?: 0
                drumSoundIds[idx] = id
                Log.d(TAG, "Loaded drum $idx: $id")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load drum $idx", e)
            }
        }
    }

    private fun loadPianoSounds() {
        val ctx = context ?: return
        
        val pianoResMap = mapOf(
            "C4" to R.raw.piano_C4,
            "Cs4" to R.raw.piano_Cs4,
            "D4" to R.raw.piano_D4,
            "Ds4" to R.raw.piano_Ds4,
            "E4" to R.raw.piano_E4,
            "F4" to R.raw.piano_F4,
            "Fs4" to R.raw.piano_Fs4,
            "G4" to R.raw.piano_G4,
            "Gs4" to R.raw.piano_Gs4,
            "A4" to R.raw.piano_A4,
            "As4" to R.raw.piano_As4,
            "B4" to R.raw.piano_B4,
            "C5" to R.raw.piano_C5,
            "Cs5" to R.raw.piano_Cs5,
            "D5" to R.raw.piano_D5,
            "Ds5" to R.raw.piano_Ds5,
            "E5" to R.raw.piano_E5,
            "F5" to R.raw.piano_F5,
            "Fs5" to R.raw.piano_Fs5,
            "G5" to R.raw.piano_G5,
            "Gs5" to R.raw.piano_Gs5,
            "A5" to R.raw.piano_A5,
            "As5" to R.raw.piano_As5,
            "B5" to R.raw.piano_B5
        )

        for ((name, resId) in pianoResMap) {
            try {
                val id = soundPool?.load(ctx, resId, 1) ?: 0
                pianoSoundIds[name] = id
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load piano $name", e)
            }
        }
    }

    // ─── PATTERN ───
    fun getCurrentPattern(): Array<BooleanArray> = patterns[currentPattern]
    fun getCurrentPianoRoll(): Array<BooleanArray> = pianoRoll[currentPattern]

    fun toggleStep(track: Int, step: Int) {
        if (track in 0 until TRACKS && step in 0 until STEPS) {
            patterns[currentPattern][track][step] = !patterns[currentPattern][track][step]
        }
    }

    fun togglePianoKey(key: Int, step: Int) {
        if (key in 0 until PIANO_KEYS && step in 0 until STEPS) {
            pianoRoll[currentPattern][key][step] = !pianoRoll[currentPattern][key][step]
        }
    }

    fun clearPattern() {
        for (t in 0 until TRACKS) for (s in 0 until STEPS) {
            patterns[currentPattern][t][s] = false
        }
        for (k in 0 until PIANO_KEYS) for (s in 0 until STEPS) {
            pianoRoll[currentPattern][k][s] = false
        }
    }

    // ─── MIXER ───
    fun setVolume(track: Int, vol: Float) {
        if (track in 0 until TRACKS) volumes[track] = vol.coerceIn(0f, 1f)
    }
    fun getVolume(track: Int) = volumes[track]

    fun setPan(track: Int, pan: Float) {
        if (track in 0 until TRACKS) pans[track] = pan.coerceIn(-1f, 1f)
    }
    fun getPan(track: Int) = pans[track]

    fun setMute(track: Int, m: Boolean) { if (track in 0 until TRACKS) mutes[track] = m }
    fun isMuted(track: Int) = mutes[track]

    fun setSolo(track: Int, s: Boolean) { if (track in 0 until TRACKS) solos[track] = s }
    fun isSolo(track: Int) = solos[track]

    // ─── PLAYBACK ───
    fun play() {
        if (isPlaying) return
        isPlaying = true
        currentStep = 0
        Metronome.reset()
        scheduleNextStep()
    }

    fun stop() {
        isPlaying = false
        currentStep = -1
        handler.removeCallbacksAndMessages(null)
    }

    fun updateBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(40, 300)
    }

    private fun scheduleNextStep() {
        if (!isPlaying) return
        val stepDuration = (60_000L / bpm) / 4

        val anySolo = solos.any { it }

        if (Metronome.enabled) {
            Metronome.advance(currentStep, stepsPerBeat = 4, beatsPerBar = 4)
        }

        // Play drums
        for (t in 0 until TRACKS) {
            if (mutes[t]) continue
            if (anySolo && !solos[t]) continue
            if (patterns[currentPattern][t][currentStep]) {
                playDrum(t, volumes[t], pans[t])
            }
        }

        // Play piano
        for (k in 0 until PIANO_KEYS) {
            if (pianoRoll[currentPattern][k][currentStep]) {
                playPianoKey(k, 0.8f)
            }
        }

        currentStep = (currentStep + 1) % STEPS
        if (currentStep == 0 && !isLooping) {
            stop()
            return
        }
        handler.postDelayed({ scheduleNextStep() }, stepDuration)
    }

    private fun playDrum(track: Int, volume: Float, pan: Float) {
        try {
            val soundId = drumSoundIds[track] ?: return
            val leftVol = volume * (if (pan <= 0) 1f else 1f - pan)
            val rightVol = volume * (if (pan >= 0) 1f else 1f + pan)
            soundPool?.play(soundId, leftVol, rightVol, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "playDrum error", e)
        }
    }

    private fun playPianoKey(key: Int, volume: Float) {
        try {
            val midi = key + 48
            val octave = midi / 12 - 1
            val noteIdx = midi % 12
            val noteNames = arrayOf("C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B")
            val name = "${noteNames[noteIdx]}$octave"

            val soundId = pianoSoundIds[name] ?: return
            soundPool?.play(soundId, volume, volume, 1, 0, 1.0f)
        } catch (e: Exception) {
            Log.e(TAG, "playPianoKey error", e)
        }
    }

    fun playNote(midiNote: Int, volume: Float = 0.8f) {
        val key = (midiNote - 48).coerceIn(0, PIANO_KEYS - 1)
        playPianoKey(key, volume)
    }

    fun previewSound(track: Int) {
        playDrum(track, volumes[track], pans[track])
    }

    fun getTracks(): List<Track> {
        return (0 until TRACKS).map { i ->
            Track(
                index = i,
                name = trackNames[i],
                steps = patterns[currentPattern][i].copyOf(),
                volume = volumes[i]
            )
        }
    }

    // ─── SAVE / LOAD ───
    fun saveProject(name: String): String {
        try {
            val json = JSONObject()
            json.put("name", name)
            json.put("bpm", bpm)
            json.put("current_pattern", currentPattern)

            val dir = File(context?.filesDir, "projects")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "$name.yad")
            file.writeText(json.toString(2))
            return file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "saveProject error", e)
            return ""
        }
    }

    fun listProjects(): List<File> {
        val dir = File(context?.filesDir, "projects")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.filter { it.extension == "yad" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun loadProject(path: String): Boolean = true

    fun loadDemoPattern() {
        clearPattern()
        for (s in 0 until STEPS step 4) patterns[currentPattern][0][s] = true
        patterns[currentPattern][1][4] = true
        patterns[currentPattern][1][12] = true
        for (s in 0 until STEPS step 2) patterns[currentPattern][2][s] = true
        patterns[currentPattern][3][12] = true
    }

    fun snapshotCurrentPattern(): Array<BooleanArray> {
        return Array(TRACKS) { t -> patterns[currentPattern][t].copyOf() }
    }

    fun undo(): Boolean = false
    fun redo(): Boolean = false

    data class Track(
        val index: Int,
        val name: String,
        val steps: BooleanArray = BooleanArray(STEPS),
        val volume: Float = 0.8f
    )
}
