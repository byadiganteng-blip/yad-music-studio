package com.yad.musicstudio

/**
 * Compressor — dynamics compressor per track.
 */
object Compressor {

    private const val TRACKS = 16

    data class Settings(
        var threshold: Float = -20f,   // dB
        var ratio: Float = 4f,          // 1:1 to 20:1
        var attackMs: Float = 10f,
        var releaseMs: Float = 100f,
        var makeupGain: Float = 0f,     // dB
        var enabled: Boolean = false
    )

    private val settings = Array(TRACKS) { Settings() }

    fun get(track: Int): Settings {
        return if (track in 0 until TRACKS) settings[track] else Settings()
    }

    fun set(track: Int, s: Settings) {
        if (track in 0 until TRACKS) settings[track] = s
    }

    fun setEnabled(track: Int, enabled: Boolean) {
        if (track in 0 until TRACKS) settings[track].enabled = enabled
    }

    /**
     * Hitung gain reduction untuk volume tertentu.
     */
    fun computeGain(inputDb: Float, track: Int): Float {
        val s = get(track)
        if (!s.enabled) return 1f

        if (inputDb <= s.threshold) return 1f

        // Amount over threshold
        val over = inputDb - s.threshold
        // Compressed
        val outputDb = s.threshold + (over / s.ratio)
        // Gain reduction (dB)
        val reductionDb = outputDb - inputDb
        // Linear
        return Math.pow(10.0, (reductionDb / 20.0)).toFloat()
    }

    fun resetTrack(track: Int) {
        if (track in 0 until TRACKS) settings[track] = Settings()
    }

    fun resetAll() {
        for (t in 0 until TRACKS) resetTrack(t)
    }

    /**
     * Dialog Compressor untuk track.
     */
    fun showDialog(context: android.content.Context, track: Int, onChanged: () -> Unit) {
        val s = get(track)

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val enableSwitch = android.widget.Switch(context).apply {
            text = "Aktifkan Compressor"
            isChecked = s.enabled
            setTextColor(0xFFFFFFFF.toInt())
        }
        layout.addView(enableSwitch)

        // Threshold
        val tvThreshold = android.widget.TextView(context).apply {
            text = "Threshold: ${s.threshold.toInt()} dB"
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 15, 0, 0)
        }
        val sbThreshold = android.widget.SeekBar(context).apply {
            max = 60  // -60 to 0
            progress = (s.threshold + 60).toInt()
        }
        sbThreshold.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, u: Boolean) {
                val v = p - 60f
                tvThreshold.text = "Threshold: ${v.toInt()} dB"
                s.threshold = v
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
        layout.addView(tvThreshold)
        layout.addView(sbThreshold)

        // Ratio
        val tvRatio = android.widget.TextView(context).apply {
            text = "Ratio: ${s.ratio.toInt()}:1"
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 15, 0, 0)
        }
        val sbRatio = android.widget.SeekBar(context).apply {
            max = 19  // 1 to 20
            progress = (s.ratio.toInt() - 1)
        }
        sbRatio.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, u: Boolean) {
                val v = (p + 1).toFloat()
                tvRatio.text = "Ratio: ${v.toInt()}:1"
                s.ratio = v
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
        layout.addView(tvRatio)
        layout.addView(sbRatio)

        // Attack
        val tvAttack = android.widget.TextView(context).apply {
            text = "Attack: ${s.attackMs.toInt()} ms"
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 15, 0, 0)
        }
        val sbAttack = android.widget.SeekBar(context).apply {
            max = 200
            progress = s.attackMs.toInt()
        }
        sbAttack.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, u: Boolean) {
                tvAttack.text = "Attack: $p ms"
                s.attackMs = p.toFloat()
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
        layout.addView(tvAttack)
        layout.addView(sbAttack)

        // Release
        val tvRelease = android.widget.TextView(context).apply {
            text = "Release: ${s.releaseMs.toInt()} ms"
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 15, 0, 0)
        }
        val sbRelease = android.widget.SeekBar(context).apply {
            max = 1000
            progress = s.releaseMs.toInt()
        }
        sbRelease.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, u: Boolean) {
                tvRelease.text = "Release: $p ms"
                s.releaseMs = p.toFloat()
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })
        layout.addView(tvRelease)
        layout.addView(sbRelease)

        android.app.AlertDialog.Builder(context)
            .setTitle("🎛️ Compressor — ${AudioEngine.trackNames[track]}")
            .setView(android.widget.ScrollView(context).apply { addView(layout) })
            .setPositiveButton("Simpan") { _, _ ->
                s.enabled = enableSwitch.isChecked
                set(track, s)
                onChanged()
            }
            .setNeutralButton("Reset") { _, _ ->
                resetTrack(track)
                onChanged()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
