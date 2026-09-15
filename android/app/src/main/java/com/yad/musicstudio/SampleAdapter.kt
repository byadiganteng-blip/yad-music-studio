package com.yad.musicstudio

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * SampleAdapter — List sample audio.
 */
class SampleAdapter(
    private val onPlay: (SampleData) -> Unit,
    private val onLongPress: (SampleData) -> Unit
) : RecyclerView.Adapter<SampleAdapter.VH>() {

    private val items = mutableListOf<SampleData>()

    fun setItems(list: List<SampleData>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(sample: SampleData) {
            text1.text = "🎵 ${sample.name}"
            text1.setTextColor(Color.WHITE)
            text1.textSize = 16f

            val sizeKb = sample.fileSize / 1024
            val durationS = sample.durationMs / 1000.0
            text2.text = "Track ${sample.trackIndex + 1} • ${sizeKb} KB • ${String.format("%.1f", durationS)}s"
            text2.setTextColor(Color.parseColor("#94A3B8"))
            text2.textSize = 12f

            itemView.setOnClickListener { onPlay(sample) }
            itemView.setOnLongClickListener {
                onLongPress(sample)
                true
            }
        }
    }
}
