package com.yad.musicstudio

/**
 * Track — data track untuk sequencer/mixer.
 * Dipindah dari AudioEngine.Track agar bisa di-import.
 */
data class Track(
    val index: Int,
    val name: String,
    val steps: BooleanArray = BooleanArray(AudioEngine.STEPS),
    val volume: Float = 0.8f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Track) return false
        return index == other.index
    }
    override fun hashCode(): Int = index
}
