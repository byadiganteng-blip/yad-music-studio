package com.yad.musicstudio

/**
 * Limiter — master limiter untuk mencegah clipping.
 */
object Limiter {

    var enabled: Boolean = true
    var ceilingDb: Float = -0.3f     // -0.3 dB default
    var releaseMs: Float = 50f

    private var currentGain: Float = 1f

    /**
     * Process satu sample.
     */
    fun process(input: Float): Float {
        if (!enabled) return input

        val ceilingLinear = Math.pow(10.0, (ceilingDb / 20.0)).toFloat()
        val absInput = kotlin.math.abs(input)

        if (absInput > ceilingLinear) {
            // Hitung gain reduction
            val targetGain = ceilingLinear / absInput
            // Smooth gain (simple attack)
            currentGain = if (targetGain < currentGain) {
                targetGain  // fast attack
            } else {
                // slow release
                val releaseRate = 1f - (releaseMs / 1000f)
                currentGain + (targetGain - currentGain) * (1f - releaseRate)
            }
            return input * currentGain
        }

        // Release
        currentGain = currentGain + (1f - currentGain) * 0.01f
        return input * currentGain
    }

    fun reset() {
        currentGain = 1f
    }
}
