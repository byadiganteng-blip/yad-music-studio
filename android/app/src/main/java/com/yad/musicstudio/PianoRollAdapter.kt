package com.yad.musicstudio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PianoRollAdapter(
    private val onKeyClick: (key: Int, step: Int) -> Unit
) : RecyclerView.Adapter<PianoRollAdapter.VH>() {

    private var data: Array<BooleanArray> = emptyArray()

    fun setData(d: Array<BooleanArray>) {
        data = d
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_piano_key, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        // Reverse: tinggi ke rendah
        val key = data.size - 1 - position
        holder.bind(key)
    }

    override fun getItemCount(): Int = data.size

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
            for (step in 0 until AudioEngine.STEPS) {
                val v = View(itemView.context)
                val params = LinearLayout.LayoutParams(52, LinearLayout.LayoutParams.MATCH_PARENT)
                params.marginEnd = 4
                v.layoutParams = params
                v.setBackgroundResource(
                    if (data[key][step]) R.drawable.step_on
                    else if (step % 4 == 0) R.drawable.step_accent
                    else R.drawable.step_off
                )
                v.setOnClickListener { onKeyClick(key, step) }
                container.addView(v)
            }
        }
    }
}
