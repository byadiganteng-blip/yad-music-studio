package com.yad.musicstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MixerFragment : Fragment() {

    private lateinit var adapter: MixerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mixer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvMixer)
        adapter = MixerAdapter(
            onVolumeChange = { track, vol -> AudioEngine.setVolume(track, vol) },
            onPanChange = { track, pan -> AudioEngine.setPan(track, pan) },
            onMuteToggle = { track, m -> AudioEngine.setMute(track, m) },
            onSoloToggle = { track, s -> AudioEngine.setSolo(track, s) },
            onEffectToggle = { track, effect, on -> AudioEngine.setEffect(track, effect, on) },
            onEqClick = { track ->
                Equalizer.showDialog(requireContext(), track) {
                    // Apply EQ multiplier ke volume
                    val mult = Equalizer.getMultiplier(track)
                    val baseVol = AudioEngine.getVolume(track)
                    AudioEngine.setVolume(track, (baseVol * mult).coerceIn(0f, 1f))
                    adapter.notifyDataSetChanged()
                }
            },
            onCompClick = { track ->
                Compressor.showDialog(requireContext(), track) {
                    adapter.notifyDataSetChanged()
                }
            }
        )
        adapter.setTracks(AudioEngine.getTracks())
        rv.layoutManager = LinearLayoutManager(requireContext(),
            LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = adapter
    }
}
