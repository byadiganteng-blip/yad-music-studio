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
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvTrackName)
        private val container: LinearLayout = itemView.findViewById(R.id.stepsContainer)

        fun bind(track: AudioEngine.Track) {
            tvName.text = track.name
            container.removeAllViews()

            for (step in 0 until AudioEngine.STEPS) {
                val v = View(itemView.context)
                val params = LinearLayout.LayoutParams(52, 52)
                params.marginEnd = 4
                v.layoutParams = params

                v.setBackgroundResource(
                    if (track.steps[step]) R.drawable.step_on
                    else if (step % 4 == 0) R.drawable.step_accent
                    else R.drawable.step_off
                )

                v.setOnClickListener { onStepClick(track.index, step) }
                container.addView(v)
            }
        }
    }
}
