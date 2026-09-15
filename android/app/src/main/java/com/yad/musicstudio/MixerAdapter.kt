package com.yad.musicstudio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MixerAdapter(
    private val onVolumeChange: (track: Int, volume: Float) -> Unit
) : RecyclerView.Adapter<MixerAdapter.VH>() {

    private var tracks: List<AudioEngine.Track> = emptyList()

    fun setTracks(t: List<AudioEngine.Track>) {
        tracks = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mixer_channel, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvMixerName)
        private val seek: SeekBar = itemView.findViewById(R.id.seekVolume)
        private val tvVol: TextView = itemView.findViewById(R.id.tvVolumeValue)

        fun bind(track: AudioEngine.Track) {
            tvName.text = track.name
            seek.max = 100
            seek.progress = (track.volume * 100).toInt()
            tvVol.text = "${(track.volume * 100).toInt()}%"

            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                    tvVol.text = "$p%"
                    onVolumeChange(track.index, p / 100f)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
    }
}
