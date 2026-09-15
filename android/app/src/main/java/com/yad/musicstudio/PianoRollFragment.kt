package com.yad.musicstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PianoRollFragment : Fragment() {

    private lateinit var adapter: PianoRollAdapterV2
    private lateinit var tvInfo: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_piano_roll, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvPianoRoll)
        tvInfo = view.findViewById(R.id.tvPianoInfo)

        adapter = PianoRollAdapterV2(
            onNoteToggle = { key, step ->
                // Note sudah di-toggle di adapter
                updateInfo()
            },
            onNoteLongPress = { key, step ->
                // Handled di adapter
                updateInfo()
            }
        )
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        rv.adapter = adapter

        view.findViewById<Button>(R.id.btnClearPiano).setOnClickListener {
            PianoRollData.clearPattern(AudioEngine.currentPattern)
            adapter.notifyDataSetChanged()
            updateInfo()
        }

        updateInfo()
    }

    private fun updateInfo() {
        val notes = PianoRollData.getNotes(AudioEngine.currentPattern)
        tvInfo.text = "🎹 Piano Roll — ${notes.size} notes | Pattern ${AudioEngine.currentPattern + 1}"
    }
}
