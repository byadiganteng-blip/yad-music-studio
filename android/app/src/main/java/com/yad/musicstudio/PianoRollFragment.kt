package com.yad.musicstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
            onNoteToggle = { _, _ -> updateInfo() },
            onNoteLongPress = { _, _ -> updateInfo() }
        )
        rv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, true)
        rv.adapter = adapter

        // Clear button
        view.findViewById<Button>(R.id.btnClearPiano).setOnClickListener {
            PianoRollData.clearPattern(AudioEngine.currentPattern)
            adapter.notifyDataSetChanged()
            updateInfo()
        }

        // Chord button
        view.findViewById<Button>(R.id.btnChord)?.setOnClickListener {
            ChordTool.showChordDialog(requireContext()) { rootKey, chordType ->
                ChordTool.insertChord(rootKey, chordType, 0)
                adapter.notifyDataSetChanged()
                updateInfo()
                Toast.makeText(requireContext(),
                    "Insert chord ${MusicTheory.NOTE_NAMES[rootKey]} ${chordType.displayName}",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Progression button
        view.findViewById<Button>(R.id.btnProgression)?.setOnClickListener {
            ChordTool.showProgressionDialog(requireContext()) { prog ->
                ChordTool.insertProgression(prog)
                adapter.notifyDataSetChanged()
                updateInfo()
                Toast.makeText(requireContext(),
                    "Insert progression: ${prog.name}",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Scale button
        view.findViewById<Button>(R.id.btnScale)?.setOnClickListener {
            ScaleHighlight.showScaleDialog(requireContext()) { rootKey, scaleType ->
                ScaleHighlight.rootKey = rootKey
                ScaleHighlight.scaleType = scaleType
                adapter.notifyDataSetChanged()
                Toast.makeText(requireContext(),
                    "Scale: ${MusicTheory.NOTE_NAMES[rootKey]} ${scaleType.displayName}",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Arp button
        view.findViewById<Button>(R.id.btnArp)?.setOnClickListener {
            ChordTool.showChordDialog(requireContext()) { rootKey, chordType ->
                ChordTool.arpeggiate(rootKey, chordType, 0, 1, ChordTool.ArpPattern.UP)
                adapter.notifyDataSetChanged()
                updateInfo()
                Toast.makeText(requireContext(),
                    "Arpeggio UP: ${MusicTheory.NOTE_NAMES[rootKey]} ${chordType.displayName}",
                    Toast.LENGTH_SHORT).show()
            }
        }

        updateInfo()
    }

    private fun updateInfo() {
        val notes = PianoRollData.getNotes(AudioEngine.currentPattern)
        val scaleInfo = if (ScaleHighlight.enabled) {
            " | ${MusicTheory.NOTE_NAMES[ScaleHighlight.rootKey]} ${ScaleHighlight.scaleType.displayName}"
        } else ""
        tvInfo.text = "🎹 ${notes.size} notes | Pattern ${AudioEngine.currentPattern + 1}$scaleInfo"
    }
}
