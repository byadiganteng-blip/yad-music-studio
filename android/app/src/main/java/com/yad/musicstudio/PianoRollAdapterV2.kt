package com.yad.musicstudio

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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

            val isBlack = noteName.contains("#")
            var bgColor = if (isBlack) 0xFF1E293B.toInt() else 0xFF475569.toInt()
            
            // Highlight scale: warna hijau soft untuk note di scale
            if (ScaleHighlight.enabled && ScaleHighlight.isInScale(key)) {
                bgColor = if (isBlack) 0xFF1A4A3A.toInt() else 0xFF2D6B4E.toInt()
            }
            tvNote.setBackgroundColor(bgColor)

            container.removeAllViews()
            val currentPattern = AudioEngine.currentPattern

            for (step in 0 until PianoRollData.STEPS) {
                val v = View(itemView.context)
                val params = LinearLayout.LayoutParams(52, LinearLayout.LayoutParams.MATCH_PARENT)
                params.marginEnd = 4
                v.layoutParams = params

                val velocity = PianoRollData.getVelocity(currentPattern, key, step)
                val isOn = velocity > 0

                v.setBackgroundColor(ScaleHighlight.getStepColor(key, step, isOn, velocity))

                v.setOnClickListener {
                    UndoRedoManager.push(UndoRedoManager.Action(
                        type = "toggle_piano_note",
                        data = mapOf("key" to key, "step" to step)
                    ))
                    PianoRollData.toggleNote(currentPattern, key, step)
                    onNoteToggle(key, step)
                    AudioEngine.playNote(key + 48, 0.8f)
                    notifyItemChanged(PianoRollData.KEYS - 1 - key)
                }

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
                addView(TextView(ctx).apply {
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
                        AudioEngine.currentPattern, key, step, slider.progress
                    )
                    notifyItemChanged(PianoRollData.KEYS - 1 - key)
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }
}
