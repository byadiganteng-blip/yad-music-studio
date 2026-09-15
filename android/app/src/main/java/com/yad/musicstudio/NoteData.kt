package com.yad.musicstudio

/**
 * NoteData — data satu note di piano roll.
 *
 * velocity: 0-127 (MIDI standard)
 * length: dalam step (1 = 1 step, 2 = 2 step, dst)
 */
data class NoteData(
    var key: Int,           // 0-23 (key index)
    var step: Int,          // 0-15 (step index)
    var velocity: Int = 100, // 0-127
    var length: Int = 1     // dalam step
) {
    fun toJson(): Map<String, Int> = mapOf(
        "key" to key,
        "step" to step,
        "velocity" to velocity,
        "length" to length
    )

    companion object {
        fun fromJson(map: Map<String, Any>): NoteData {
            return NoteData(
                key = (map["key"] as? Number)?.toInt() ?: 0,
                step = (map["step"] as? Number)?.toInt() ?: 0,
                velocity = (map["velocity"] as? Number)?.toInt() ?: 100,
                length = (map["length"] as? Number)?.toInt() ?: 1
            )
        }
    }
}
