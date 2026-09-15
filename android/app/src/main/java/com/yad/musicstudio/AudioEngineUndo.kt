
// ═══════════════════════════════════════════════════════════════════════════
// TAMBAHAN: Undo/Redo support di AudioEngine
// Tambahkan method ini di dalam object AudioEngine
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Undo aksi terakhir.
 */
fun undo(): Boolean {
    val action = UndoRedoManager.undo() ?: return false
    applyUndoAction(action, isRedo = false)
    return true
}

fun redo(): Boolean {
    val action = UndoRedoManager.redo() ?: return false
    applyUndoAction(action, isRedo = true)
    return true
}

private fun applyUndoAction(action: UndoRedoManager.Action, isRedo: Boolean) {
    when (action.type) {
        "toggle_step" -> {
            val track = action.data["track"] as Int
            val step = action.data["step"] as Int
            patterns[currentPattern][track][step] = !patterns[currentPattern][track][step]
        }
        "clear_pattern" -> {
            // Restore pattern dari snapshot
            @Suppress("UNCHECKED_CAST")
            val snapshot = action.data["snapshot"] as Array<BooleanArray>
            for (t in 0 until TRACKS) {
                for (s in 0 until STEPS) {
                    patterns[currentPattern][t][s] = snapshot[t][s]
                }
            }
        }
        "set_volume" -> {
            val track = action.data["track"] as Int
            val prevVol = action.data["prev_volume"] as Float
            volumes[track] = prevVol
        }
    }
}

/**
 * Snapshot pattern saat ini untuk undo.
 */
fun snapshotCurrentPattern(): Array<BooleanArray> {
    return Array(TRACKS) { t -> patterns[currentPattern][t].copyOf() }
}
