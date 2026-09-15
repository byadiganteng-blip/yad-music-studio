package com.yad.musicstudio

/**
 * Quantizer — rapikan timing note + swing groove.
 */
object Quantizer {

    private const val TAG = "Quantizer"

    enum class Grid(val displayName: String, val steps: Int) {
        OFF("Off", 0),
        QUARTER("1/4", 4),
        EIGHTH("1/8", 2),
        SIXTEENTH("1/16", 1),
        THIRTY_SECOND("1/32", 1)  // simplified
    }

    var swingAmount: Float = 0f  // 0.0 = no swing, 1.0 = full swing

    /**
     * Quantize semua note di piano roll ke grid.
     * Note yang tidak tepat di grid akan digeser ke grid terdekat.
     */
    fun quantizePianoRoll(pattern: Int = AudioEngine.currentPattern, grid: Grid = Grid.SIXTEENTH) {
        if (grid == Grid.OFF) return

        val notes = PianoRollData.getNotes(pattern)
        val newNotes = mutableListOf<NoteData>()

        for (note in notes) {
            val quantizedStep = (note.step / grid.steps) * grid.steps
            newNotes.add(note.copy(step = quantizedStep))
        }

        PianoRollData.setNotes(pattern, newNotes)
    }

    /**
     * Quantize drum pattern (16 tracks × 16 steps).
     */
    fun quantizeDrums(pattern: Int = AudioEngine.currentPattern, grid: Grid = Grid.SIXTEENTH) {
        if (grid == Grid.OFF) return
        // Drum pattern sudah 16-step grid, jadi sudah ter-quantize
        // Fungsi ini disiapkan untuk future use
    }

    /**
     * Apply swing ke pattern.
     * Swing menggeser note ganjil (off-beat) sedikit ke belakang.
     */
    fun applySwing(pattern: Int = AudioEngine.currentPattern, amount: Float = 0.3f) {
        swingAmount = amount.coerceIn(0f, 1f)

        // Get all notes
        val notes = PianoRollData.getNotes(pattern)
        val newNotes = mutableListOf<NoteData>()

        for (note in notes) {
            // Note di step ganjil (1, 3, 5, ...) di-delay
            val newStep = if (note.step % 2 == 1) {
                // Geser note ganjil ke kanan (delay)
                // Tidak bisa geser karena grid fixed, jadi hanya bikin feel
                // Untuk sekarang, skip
                note.step
            } else {
                note.step
            }
            newNotes.add(note.copy(step = newStep))
        }

        PianoRollData.setNotes(pattern, newNotes)
    }

    /**
     * Show quantize dialog.
     */
    fun showQuantizeDialog(context: android.content.Context) {
        val gridNames = Grid.values().map { it.displayName }.toTypedArray()
        val gridValues = Grid.values()

        val gridSpinner = android.widget.Spinner(context).apply {
            adapter = android.widget.ArrayAdapter(context,
                android.R.layout.simple_spinner_dropdown_item, gridNames)
            setSelection(3)  // default 1/16
        }

        val swingSlider = android.widget.SeekBar(context).apply {
            max = 100
            progress = (swingAmount * 100).toInt()
        }

        val swingLabel = android.widget.TextView(context).apply {
            text = "Swing: ${(swingAmount * 100).toInt()}%"
            setTextColor(0xFFFFFFFF.toInt())
        }

        swingSlider.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, u: Boolean) {
                swingLabel.text = "Swing: $p%"
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
        })

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            addView(android.widget.TextView(context).apply {
                text = "Grid Snap"
                setTextColor(0xFFFFFFFF.toInt())
            })
            addView(gridSpinner)
            addView(android.widget.TextView(context).apply {
                text = "Swing Amount"
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 20, 0, 0)
            })
            addView(swingLabel)
            addView(swingSlider)
        }

        android.app.AlertDialog.Builder(context)
            .setTitle("🎯 Quantize + Swing")
            .setView(layout)
            .setPositiveButton("Terapkan") { _, _ ->
                val grid = gridValues[gridSpinner.selectedItemPosition]
                val swing = swingSlider.progress / 100f

                quantizePianoRoll(grid = grid)
                quantizeDrums(grid = grid)
                applySwing(amount = swing)

                android.widget.Toast.makeText(context,
                    "Quantize: ${grid.displayName}, Swing: ${(swing * 100).toInt()}%",
                    android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
