package com.yad.musicstudio

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SequencerAdapter(
    private val onStepClick: (track: Int, step: Int) -> Unit
) : RecyclerView.Adapter<SequencerAdapter.VH>() {

    private var tracks: List<AudioEngine.Track> = emptyList()

    fun setTracks(t: List<AudioEngine.Track>) {
        tracks = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTrackName: TextView = itemView.findViewById(R.id.tvTrackName)
        private val stepsContainer: LinearLayout = itemView.findViewById(R.id.stepsContainer)

        fun bind(track: AudioEngine.Track) {
            tvTrackName.text = track.name
            stepsContainer.removeAllViews()

            for (step in 0 until 16) {
                val stepView = View(itemView.context)
                val size = 60
                val margin = 2
                val params = LinearLayout.LayoutParams(size, size)
                params.marginEnd = margin

                stepView.layoutParams = params
                stepView.setBackgroundColor(
                    if (track.steps[step]) Color.parseColor("#EF4444")
                    else if (step % 4 == 0) Color.parseColor("#334155")
                    else Color.parseColor("#1E293B")
                )

                stepView.setOnClickListener {
                    onStepClick(track.index, step)
                }

                stepsContainer.addView(stepView)
            }
        }
    }
}
