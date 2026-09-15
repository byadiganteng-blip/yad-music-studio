package com.yad.musicstudio

import android.graphics.Color

/**
 * ScaleHighlight — Helper untuk highlight scale di piano roll.
 */
object ScaleHighlight {

    var enabled: Boolean = false
    var rootKey: Int = 0  // C
    var scaleType: MusicTheory.ScaleType = MusicTheory.ScaleType.MAJOR

    /**
     * Cek apakah key index (0-23) ada di scale aktif.
     */
    fun isInScale(key: Int): Boolean {
        if (!enabled) return false
        val midi = key + 48
        return MusicTheory.isNoteInScale(midi, rootKey, scaleType)
    }

    /**
     * Get color untuk step background berdasarkan scale.
     */
    fun getStepColor(key: Int, step: Int, isOn: Boolean, velocity: Int): Int {
        return when {
            isOn -> when {
                velocity >= 100 -> Color.parseColor("#EF4444")
                velocity >= 60 -> Color.parseColor("#F59E0B")
                else -> Color.parseColor("#FBBF24")
            }
            enabled && isInScale(key) -> {
                // Highlight warna hijau soft untuk note yang ada di scale
                if (step % 4 == 0) Color.parseColor("#2D4A3E")
                else Color.parseColor("#1A2E27")
            }
            else -> {
                if (step % 4 == 0) Color.parseColor("#334155")
                else Color.parseColor("#1E293B")
            }
        }
    }

    /**
     * Dialog untuk pilih scale.
     */
    fun showScaleDialog(
        context: android.content.Context,
        onScaleSelected: (Int, MusicTheory.ScaleType) -> Unit
    ) {
        val rootSpinner = android.widget.Spinner(context).apply {
            adapter = android.widget.ArrayAdapter(context,
                android.R.layout.simple_spinner_dropdown_item,
                MusicTheory.NOTE_NAMES)
            setSelection(rootKey)
        }
        val scaleSpinner = android.widget.Spinner(context).apply {
            adapter = android.widget.ArrayAdapter(context,
                android.R.layout.simple_spinner_dropdown_item,
                MusicTheory.ScaleType.values().map { it.displayName })
            setSelection(scaleType.ordinal)
        }
        val enableSwitch = android.widget.Switch(context).apply {
            text = "Aktifkan Highlight"
            isChecked = enabled
            setTextColor(0xFFFFFFFF.toInt())
        }

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            addView(enableSwitch)
            addView(android.widget.TextView(context).apply {
                text = "Root Note"
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 20, 0, 0)
            })
            addView(rootSpinner)
            addView(android.widget.TextView(context).apply {
                text = "Scale Type"
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 20, 0, 0)
            })
            addView(scaleSpinner)
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("🎼 Scale Highlight")
            .setView(layout)
            .setPositiveButton("Terapkan") { _, _ ->
                enabled = enableSwitch.isChecked
                onScaleSelected(rootSpinner.selectedItemPosition,
                    MusicTheory.ScaleType.values()[scaleSpinner.selectedItemPosition])
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
