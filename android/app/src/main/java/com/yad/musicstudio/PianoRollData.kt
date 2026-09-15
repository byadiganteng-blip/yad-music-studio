package com.yad.musicstudio

/**
 * PianoRollData — simpan note dengan velocity + length.
 * Menggantikan BooleanArray sederhana.
 */
object PianoRollData {

    private const val TAG = "PianoRollData"
    const val KEYS = 24
    const val STEPS = 16
    const val PATTERNS = 8

    // Storage: [pattern][key][step] → velocity (0 = kosong, 1-127 = ada)
    // Length disimpan terpisah
    private val velocities = Array(PATTERNS) {
        Array(KEYS) { IntArray(STEPS) { 0 } }
    }
    private val lengths = Array(PATTERNS) {
        Array(KEYS) { IntArray(STEPS) { 1 } }
    }

    fun getVelocity(pattern: Int, key: Int, step: Int): Int {
        return velocities.getOrNull(pattern)
            ?.getOrNull(key)
            ?.getOrNull(step) ?: 0
    }

    fun setVelocity(pattern: Int, key: Int, step: Int, velocity: Int) {
        if (pattern in 0 until PATTERNS && key in 0 until KEYS && step in 0 until STEPS) {
            velocities[pattern][key][step] = velocity.coerceIn(0, 127)
        }
    }

    fun getLength(pattern: Int, key: Int, step: Int): Int {
        return lengths.getOrNull(pattern)
            ?.getOrNull(key)
            ?.getOrNull(step) ?: 1
    }

    fun setLength(pattern: Int, key: Int, step: Int, length: Int) {
        if (pattern in 0 until PATTERNS && key in 0 until KEYS && step in 0 until STEPS) {
            lengths[pattern][key][step] = length.coerceIn(1, 16)
        }
    }

    fun isNoteActive(pattern: Int, key: Int, step: Int): Boolean {
        return getVelocity(pattern, key, step) > 0
    }

    fun toggleNote(pattern: Int, key: Int, step: Int) {
        val current = getVelocity(pattern, key, step)
        if (current > 0) {
            setVelocity(pattern, key, step, 0)
        } else {
            setVelocity(pattern, key, step, 100)  // default velocity
            setLength(pattern, key, step, 1)
        }
    }

    fun clearPattern(pattern: Int) {
        if (pattern in 0 until PATTERNS) {
            for (k in 0 until KEYS) {
                for (s in 0 until STEPS) {
                    velocities[pattern][k][s] = 0
                    lengths[pattern][k][s] = 1
                }
            }
        }
    }

    /**
     * Get semua note aktif di pattern sebagai list NoteData.
     */
    fun getNotes(pattern: Int): List<NoteData> {
        val result = mutableListOf<NoteData>()
        for (k in 0 until KEYS) {
            for (s in 0 until STEPS) {
                val v = getVelocity(pattern, k, s)
                if (v > 0) {
                    result.add(NoteData(
                        key = k,
                        step = s,
                        velocity = v,
                        length = getLength(pattern, k, s)
                    ))
                }
            }
        }
        return result
    }

    /**
     * Set note dari list (untuk load project).
     */
    fun setNotes(pattern: Int, notes: List<NoteData>) {
        clearPattern(pattern)
        for (note in notes) {
            setVelocity(pattern, note.key, note.step, note.velocity)
            setLength(pattern, note.key, note.step, note.length)
        }
    }

    /**
     * Snapshot untuk undo.
     */
    fun snapshot(pattern: Int): Array<IntArray> {
        return Array(KEYS) { k -> velocities[pattern][k].copyOf() }
    }

    /**
     * Restore dari snapshot.
     */
    fun restore(pattern: Int, snapshot: Array<IntArray>) {
        for (k in 0 until KEYS) {
            for (s in 0 until STEPS) {
                velocities[pattern][k][s] = snapshot.getOrNull(k)?.getOrNull(s) ?: 0
            }
        }
    }
}
