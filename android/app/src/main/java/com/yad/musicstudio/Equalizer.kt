package com.yad.musicstudio

/**
 * Equalizer — 3-band EQ (Low, Mid, High) per track.
 *
 * Nilai dalam dB: -12 sampai +12.
 */
object Equalizer {

    private const val TRACKS = 16

    // [track][band] — band 0=Low, 1=Mid, 2=High
    private val bands = Array(TRACKS) { FloatArray(3) { 0f } }

    // Frequency range
    const val LOW_CUTOFF = 250f    // Low: < 250 Hz
    const val MID_CUTOFF = 2500f   // Mid: 250-2500 Hz
    // High: > 2500 Hz

    const val MIN_GAIN_DB = -12f
    const val MAX_GAIN_DB = 12f

    fun setGain(track: Int, band: Int, gainDb: Float) {
        if (track in 0 until TRACKS && band in 0..2) {
            bands[track][band] = gainDb.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        }
    }

    fun getGain(track: Int, band: Int): Float {
        return if (track in 0 until TRACKS && band in 0..2)
            bands[track][band] else 0f
    }

    /**
     * Hitung volume multiplier berdasarkan EQ.
     */
    fun getMultiplier(track: Int): Float {
        if (track !in 0 until TRACKS) return 1f
        // Rata-rata gain → multiplier
        val avgGain = (bands[track][0] + bands[track][1] + bands[track][2]) / 3f
        // dB to linear: 10^(dB/20)
        return Math.pow(10.0, (avgGain / 20.0)).toFloat()
    }

    fun resetTrack(track: Int) {
        if (track in 0 until TRACKS) {
            bands[track][0] = 0f
            bands[track][1] = 0f
            bands[track][2] = 0f
        }
    }

    fun resetAll() {
        for (t in 0 until TRACKS) resetTrack(t)
    }

    /**
     * Dialog EQ untuk track tertentu.
     */
    fun showDialog(context: android.content.Context, track: Int, onChanged: () -> Unit) {
        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val bandNames = arrayOf("LOW (< 250 Hz)", "MID (250-2500 Hz)", "HIGH (> 2500 Hz)")
        val seekBars = mutableListOf<android.widget.SeekBar>()

        for (band in 0..2) {
            val label = android.widget.TextView(context).apply {
                text = "${bandNames[band]}: ${bands[track][band].toInt()} dB"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setPadding(0, 10, 0, 0)
            }
            val seek = android.widget.SeekBar(context).apply {
                max = (MAX_GAIN_DB - MIN_GAIN_DB).toInt()  // 24
                progress = (bands[track][band] - MIN_GAIN_DB).toInt()
            }
            seek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, u: Boolean) {
                    val gain = MIN_GAIN_DB + p
                    label.text = "${bandNames[band]}: ${gain.toInt()} dB"
                    setGain(track, band, gain)
                }
                override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
            })
            seekBars.add(seek)
            layout.addView(label)
            layout.addView(seek)
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("🎚️ EQ — ${AudioEngine.trackNames[track]}")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ -> onChanged() }
            .setNeutralButton("Reset") { _, _ ->
                resetTrack(track)
                onChanged()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
