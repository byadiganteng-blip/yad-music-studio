package com.yad.musicstudio

/**
 * MusicTheory — Chord progression + Scale patterns.
 */
object MusicTheory {

    // ─── CHORD TYPES ───
    // Interval dari root (dalam semitone)
    enum class ChordType(val displayName: String, val intervals: IntArray) {
        MAJOR("Mayor", intArrayOf(0, 4, 7)),
        MINOR("Minor", intArrayOf(0, 3, 7)),
        DIMINISHED("Dim", intArrayOf(0, 3, 6)),
        AUGMENTED("Aug", intArrayOf(0, 4, 8)),
        SUS2("Sus2", intArrayOf(0, 2, 7)),
        SUS4("Sus4", intArrayOf(0, 5, 7)),
        MAJ7("Maj7", intArrayOf(0, 4, 7, 11)),
        MIN7("Min7", intArrayOf(0, 3, 7, 10)),
        DOM7("7", intArrayOf(0, 4, 7, 10)),
        DIM7("Dim7", intArrayOf(0, 3, 6, 9)),
        MAJ9("Maj9", intArrayOf(0, 4, 7, 11, 14)),
        MIN9("Min9", intArrayOf(0, 3, 7, 10, 14)),
        ADD9("Add9", intArrayOf(0, 4, 7, 14)),
        SIX("6", intArrayOf(0, 4, 7, 9)),
        MIN6("Min6", intArrayOf(0, 3, 7, 9))
    }

    // ─── SCALE PATTERNS ───
    enum class ScaleType(val displayName: String, val intervals: IntArray) {
        MAJOR("Mayor", intArrayOf(0, 2, 4, 5, 7, 9, 11)),
        NATURAL_MINOR("Minor", intArrayOf(0, 2, 3, 5, 7, 8, 10)),
        HARMONIC_MINOR("Harm Minor", intArrayOf(0, 2, 3, 5, 7, 8, 11)),
        MELODIC_MINOR("Mel Minor", intArrayOf(0, 2, 3, 5, 7, 9, 11)),
        DORIAN("Dorian", intArrayOf(0, 2, 3, 5, 7, 9, 10)),
        PHRYGIAN("Phrygian", intArrayOf(0, 1, 3, 5, 7, 8, 10)),
        LYDIAN("Lydian", intArrayOf(0, 2, 4, 6, 7, 9, 11)),
        MIXOLYDIAN("Mixolydian", intArrayOf(0, 2, 4, 5, 7, 9, 10)),
        LOCRIAN("Locrian", intArrayOf(0, 1, 3, 5, 6, 8, 10)),
        PENTATONIC_MAJOR("Pent Major", intArrayOf(0, 2, 4, 7, 9)),
        PENTATONIC_MINOR("Pent Minor", intArrayOf(0, 3, 5, 7, 10)),
        BLUES("Blues", intArrayOf(0, 3, 5, 6, 7, 10)),
        CHROMATIC("Chromatic", IntArray(12) { it })
    }

    // ─── KEY NAMES ───
    val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Dapatkan semua note index dari chord.
     * @param rootKey 0-11 (C=0, C#=1, dst)
     * @param type chord type
     * @param octaveOffset offset oktaf (0 = default)
     * @return list MIDI note number
     */
    fun getChordNotes(rootKey: Int, type: ChordType, octaveOffset: Int = 0): List<Int> {
        val baseMidi = 48 + rootKey + (octaveOffset * 12)  // C3 = 48
        return type.intervals.map { baseMidi + it }
    }

    /**
     * Dapatkan semua note dari scale di range tertentu.
     */
    fun getScaleNotes(rootKey: Int, type: ScaleType, startOctave: Int = 3, numOctaves: Int = 3): List<Int> {
        val result = mutableListOf<Int>()
        for (oct in 0 until numOctaves) {
            for (interval in type.intervals) {
                result.add(48 + rootKey + (oct * 12) + interval)
            }
        }
        return result
    }

    /**
     * Cek apakah MIDI note ada di scale.
     */
    fun isNoteInScale(midiNote: Int, rootKey: Int, type: ScaleType): Boolean {
        val relative = ((midiNote - 48 - rootKey) % 12 + 12) % 12
        return type.intervals.contains(relative)
    }

    /**
     * Chord progression populer.
     */
    data class Progression(val name: String, val chords: List<Pair<Int, ChordType>>)

    val PROGRESSIONS = listOf(
        Progression("I-IV-V-I", listOf(
            0 to ChordType.MAJOR, 5 to ChordType.MAJOR,
            7 to ChordType.MAJOR, 0 to ChordType.MAJOR
        )),
        Progression("I-V-vi-IV (Pop)", listOf(
            0 to ChordType.MAJOR, 7 to ChordType.MAJOR,
            9 to ChordType.MINOR, 5 to ChordType.MAJOR
        )),
        Progression("ii-V-I (Jazz)", listOf(
            2 to ChordType.MIN7, 7 to ChordType.DOM7,
            0 to ChordType.MAJ7
        )),
        Progression("vi-IV-I-V", listOf(
            9 to ChordType.MINOR, 5 to ChordType.MAJOR,
            0 to ChordType.MAJOR, 7 to ChordType.MAJOR
        )),
        Progression("I-vi-IV-V (50s)", listOf(
            0 to ChordType.MAJOR, 9 to ChordType.MINOR,
            5 to ChordType.MAJOR, 7 to ChordType.MAJOR
        )),
        Progression("i-VII-VI-VII (Minor)", listOf(
            0 to ChordType.MINOR, 10 to ChordType.MAJOR,
            8 to ChordType.MAJOR, 10 to ChordType.MAJOR
        ))
    )
}
