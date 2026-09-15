package com.yad.musicstudio

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.*

/**
 * ChordTool — Insert chord otomatis ke piano roll.
 */
object ChordTool {

    private const val TAG = "ChordTool"

    /**
     * Insert chord ke pattern.
     * @param rootKey 0-11
     * @param type chord type
     * @param startStep step awal (0-15)
     * @param pattern pattern index
     */
    fun insertChord(
        rootKey: Int,
        type: MusicTheory.ChordType,
        startStep: Int,
        pattern: Int = AudioEngine.currentPattern,
        velocity: Int = 100
    ) {
        val notes = MusicTheory.getChordNotes(rootKey, type)
        for (midiNote in notes) {
            val key = (midiNote - 48).coerceIn(0, PianoRollData.KEYS - 1)
            PianoRollData.setVelocity(pattern, key, startStep, velocity)
            PianoRollData.setLength(pattern, key, startStep, 4)  // 4 step panjang
        }
    }

    /**
     * Insert chord progression ke pattern (spread across steps).
     */
    fun insertProgression(
        progression: MusicTheory.Progression,
        pattern: Int = AudioEngine.currentPattern
    ) {
        val stepsPerChord = PianoRollData.STEPS / progression.chords.size
        progression.chords.forEachIndexed { i, (root, type) ->
            val startStep = i * stepsPerChord
            insertChord(root, type, startStep, pattern)
        }
    }

    /**
     * Bikin arpeggio dari chord (note bergantian).
     */
    fun arpeggiate(
        rootKey: Int,
        type: MusicTheory.ChordType,
        pattern: Int = AudioEngine.currentPattern,
        startStep: Int = 0,
        stepsPerNote: Int = 1,
        pattern_type: ArpPattern = ArpPattern.UP
    ) {
        val notes = MusicTheory.getChordNotes(rootKey, type)
        if (notes.isEmpty()) return

        var step = startStep
        val sequence = when (pattern_type) {
            ArpPattern.UP -> notes
            ArpPattern.DOWN -> notes.reversed()
            ArpPattern.UP_DOWN -> notes + notes.reversed().drop(1).dropLast(1)
            ArpPattern.RANDOM -> notes.shuffled()
        }

        for (midiNote in sequence) {
            if (step >= PianoRollData.STEPS) break
            val key = (midiNote - 48).coerceIn(0, PianoRollData.KEYS - 1)
            PianoRollData.setVelocity(pattern, key, step, 100)
            PianoRollData.setLength(pattern, key, step, stepsPerNote)
            step += stepsPerNote
        }
    }

    enum class ArpPattern {
        UP, DOWN, UP_DOWN, RANDOM
    }

    /**
     * Tampilkan dialog untuk pilih chord.
     */
    fun showChordDialog(context: Context, onChordSelected: (Int, MusicTheory.ChordType) -> Unit) {
        val rootKeys = MusicTheory.NOTE_NAMES
        val chordTypes = MusicTheory.ChordType.values()

        val rootSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context,
                android.R.layout.simple_spinner_dropdown_item, rootKeys)
        }
        val typeSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context,
                android.R.layout.simple_spinner_dropdown_item,
                chordTypes.map { it.displayName })
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            addView(TextView(context).apply {
                text = "Root Note"
                setTextColor(0xFFFFFFFF.toInt())
            })
            addView(rootSpinner)
            addView(TextView(context).apply {
                text = "Chord Type"
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 20, 0, 0)
            })
            addView(typeSpinner)
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("🎼 Insert Chord")
            .setView(layout)
            .setPositiveButton("Insert") { _, _ ->
                val root = rootSpinner.selectedItemPosition
                val type = chordTypes[typeSpinner.selectedItemPosition]
                onChordSelected(root, type)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Dialog untuk pilih progression.
     */
    fun showProgressionDialog(context: Context, onSelected: (MusicTheory.Progression) -> Unit) {
        val names = MusicTheory.PROGRESSIONS.map { it.name }.toTypedArray()
        android.app.AlertDialog.Builder(context)
            .setTitle("🎼 Chord Progression")
            .setItems(names) { _, which ->
                onSelected(MusicTheory.PROGRESSIONS[which])
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
