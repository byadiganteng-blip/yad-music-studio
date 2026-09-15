package com.yad.musicstudio

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * PianoRollAdapterV2 — Adapter dengan velocity color + length.
 */
class PianoRollAdapterV2(
    private val onNoteToggle: (key: Int, step: Int) -> Unit,
    private val onNoteLongPress: (key: Int, step: Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<PianoRollAdapterV2.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_piano_key, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        // Reverse: tinggi ke rendah
        val key = PianoRollData.KEYS - 1 - position
        holder.bind(key)
    }

    override fun getItemCount(): Int = PianoRollData.KEYS

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNote: TextView = itemView.findViewById(R.id.tvNote)
        private val container: LinearLayout = itemView.findViewById(R.id.pianoSteps)

        fun bind(key: Int) {
            val notes = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
            val octave = key / 12 + 4
            val noteName = notes[key % 12] + octave
            tvNote.text = noteName

            // Warna hitam untuk sharp
            val isBlack = noteName.contains("#")
            tvNote.setBackgroundColor(if (isBlack) 0xFF1E293B.toInt() else 0xFF475569.toInt())

            container.removeAllViews()
            val currentPattern = AudioEngine.currentPattern

            for (step in 0 until PianoRollData.STEPS) {
                val v = View(itemView.context)
                val params = LinearLayout.LayoutParams(52, LinearLayout.LayoutParams.MATCH_PARENT)
                params.marginEnd = 4
                v.layoutParams = params

                val velocity = PianoRollData.getVelocity(currentPattern, key, step)
                val length = PianoRollData.getLength(currentPattern, key, step)

                // Warna berdasarkan velocity
                v.setBackgroundColor(when {
                    velocity >= 100 -> Color.parseColor("#EF4444")  // Keras - merah
                    velocity >= 60 -> Color.parseColor("#F59E0B")   // Sedang - oranye
                    velocity > 0 -> Color.parseColor("#FBBF24")     // Pelan - kuning
                    step % 4 == 0 -> Color.parseColor("#334155")    // Accent
                    else -> Color.parseColor("#1E293B")             // Off
                })

                // Draw border untuk indikasi length
                if (velocity > 0 && length > 1) {
                    v.setBackgroundResource(android.R.drawable.editbox_background)
                    v.setBackgroundColor(when {
                        velocity >= 100 -> Color.parseColor("#EF4444")
                        velocity >= 60 -> Color.parseColor("#F59E0B")
                        else -> Color.parseColor("#FBBF24")
                    })
                }

                // Tap = toggle
                v.setOnClickListener {
                    // Push undo
                    UndoRedoManager.push(UndoRedoManager.Action(
                        type = "toggle_piano_note",
                        data = mapOf("key" to key, "step" to step)
                    ))
                    PianoRollData.toggleNote(currentPattern, key, step)
                    onNoteToggle(key, step)
                    AudioEngine.playNote(key + 48, 0.8f)
                    notifyItemChanged(PianoRollData.KEYS - 1 - key)
                }

                // Long press = ubah velocity
                v.setOnLongClickListener {
                    showVelocityDialog(itemView, key, step)
                    true
                }

                container.addView(v)
            }
        }

        private fun showVelocityDialog(parent: View, key: Int, step: Int) {
            val ctx = parent.context
            val slider = android.widget.SeekBar(ctx)
            val currentVel = PianoRollData.getVelocity(
                AudioEngine.currentPattern, key, step
            ).coerceAtLeast(50)

            slider.max = 127
            slider.progress = currentVel

            val padding = (16 * ctx.resources.displayMetrics.density).toInt()
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding)
                addView(android.widget.TextView(ctx).apply {
                    text = "Velocity: $currentVel"
                    setTextColor(Color.WHITE)
                    textSize = 14f
                })
                addView(slider)
            }

            android.app.AlertDialog.Builder(ctx)
                .setTitle("Ubah Velocity")
                .setView(container)
                .setPositiveButton("OK") { _, _ ->
                    PianoRollData.setVelocity(
                        AudioEngine.currentPattern, key, step,
                        slider.progress
                    )
                    // Set length default
                    if (slider.progress > 0) {
                        PianoRollData.setLength(
                            AudioEngine.currentPattern, key, step, 1
                        )
                    }
                    notifyItemChanged(PianoRollData.KEYS - 1 - key)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
