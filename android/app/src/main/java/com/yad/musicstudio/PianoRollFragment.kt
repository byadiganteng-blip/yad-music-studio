package com.yad.musicstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PianoRollFragment : Fragment() {

    private lateinit var adapter: PianoRollAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_piano_roll, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvPianoRoll)
        adapter = PianoRollAdapter { key, step ->
            AudioEngine.togglePianoKey(key, step)
            AudioEngine.playNote(key + 48, 0.8f)
        }
        adapter.setData(AudioEngine.getPianoRoll())
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        rv.adapter = adapter

        view.findViewById<android.widget.Button>(R.id.btnClearPiano).setOnClickListener {
            for (k in 0 until AudioEngine.PIANO_KEYS)
                for (s in 0 until AudioEngine.STEPS)
                    if (AudioEngine.getPianoRoll()[k][s]) AudioEngine.togglePianoKey(k, s)
            adapter.notifyDataSetChanged()
        }
    }
}
