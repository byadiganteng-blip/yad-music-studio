package com.yad.musicstudio

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class MixerAdapter(
    private val onVolumeChange: (Int, Float) -> Unit,
    private val onPanChange: (Int, Float) -> Unit,
    private val onMuteToggle: (Int, Boolean) -> Unit,
    private val onSoloToggle: (Int, Boolean) -> Unit,
    private val onEffectToggle: (Int, AudioEngine.EffectType, Boolean) -> Unit
) : RecyclerView.Adapter<MixerAdapter.VH>() {

    private var tracks: List<AudioEngine.Track> = emptyList()

    fun setTracks(t: List<AudioEngine.Track>) {
        tracks = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mixer_channel, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(tracks[position])
    }

    override fun getItemCount(): Int = tracks.size

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvMixerName)
        private val seekVol: SeekBar = itemView.findViewById(R.id.seekVolume)
        private val seekPan: SeekBar = itemView.findViewById(R.id.seekPan)
        private val tvVol: TextView = itemView.findViewById(R.id.tvVolumeValue)
        private val btnMute: Button = itemView.findViewById(R.id.btnMute)
        private val btnSolo: Button = itemView.findViewById(R.id.btnSolo)
        private val cbReverb: CheckBox = itemView.findViewById(R.id.cbReverb)
        private val cbDelay: CheckBox = itemView.findViewById(R.id.cbDelay)
        private val cbDist: CheckBox = itemView.findViewById(R.id.cbDistortion)

        fun bind(track: AudioEngine.Track) {
            val i = track.index
            tvName.text = track.name
            seekVol.max = 100
            seekVol.progress = (AudioEngine.getVolume(i) * 100).toInt()
            tvVol.text = "${(AudioEngine.getVolume(i) * 100).toInt()}%"

            seekVol.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                    tvVol.text = "$p%"
                    onVolumeChange(i, p / 100f)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            seekPan.max = 200
            seekPan.progress = ((AudioEngine.getPan(i) + 1f) * 100).toInt()
            seekPan.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                    onPanChange(i, (p / 100f) - 1f)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            btnMute.text = if (AudioEngine.isMuted(i)) "🔇" else "🔊"
            btnMute.setOnClickListener {
                val newMute = !AudioEngine.isMuted(i)
                onMuteToggle(i, newMute)
                btnMute.text = if (newMute) "🔇" else "🔊"
            }
            btnMute.setBackgroundColor(if (AudioEngine.isMuted(i)) 0xFFEF4444.toInt() else 0xFF3B82F6.toInt())

            btnSolo.text = "S"
            btnSolo.setBackgroundColor(if (AudioEngine.isSolo(i)) 0xFFFBBF24.toInt() else 0xFF334155.toInt())
            btnSolo.setOnClickListener {
                val newSolo = !AudioEngine.isSolo(i)
                onSoloToggle(i, newSolo)
                btnSolo.setBackgroundColor(if (newSolo) 0xFFFBBF24.toInt() else 0xFF334155.toInt())
            }

            cbReverb.setOnCheckedChangeListener { _, checked -> onEffectToggle(i, AudioEngine.EffectType.REVERB, checked) }
            cbDelay.setOnCheckedChangeListener { _, checked -> onEffectToggle(i, AudioEngine.EffectType.DELAY, checked) }
            cbDist.setOnCheckedChangeListener { _, checked -> onEffectToggle(i, AudioEngine.EffectType.DISTORTION, checked) }
        }
    }
}
