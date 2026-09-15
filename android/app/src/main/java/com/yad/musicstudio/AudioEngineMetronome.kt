package com.yad.musicstudio

/**
 * Metronome integration untuk AudioEngine.
 * 
 * Update method scheduleNextStep() di AudioEngine untuk 
 * memanggil Metronome.advance() setiap step.
 */

object AudioEngineMetronome {

    /**
     * Panggil ini di scheduleNextStep() setiap step.
     */
    fun onStep(step: Int) {
        if (Metronome.enabled) {
            Metronome.advance(step, stepsPerBeat = 4, beatsPerBar = 4)
        }
    }

    /**
     * Panggil saat play() dipanggil.
     */
    fun onPlayStart() {
        Metronome.reset()
    }
}
