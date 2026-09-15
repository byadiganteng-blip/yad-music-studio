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
            onEffectToggle = { track, effect, on -> AudioEngine.setEffect(track, effect, on) }
        )
        adapter.setTracks((0 until AudioEngine.TRACKS).map { i ->
            AudioEngine.Track(
                index = i,
                name = AudioEngine.trackNames[i],
                steps = BooleanArray(0),
                volume = AudioEngine.getVolume(i)
            )
        })
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = adapter
    }
}
