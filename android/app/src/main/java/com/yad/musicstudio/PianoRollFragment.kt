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
        rv.layoutManager = LinearLayoutManager(requireContext(),
            LinearLayoutManager.VERTICAL, true)
        rv.adapter = adapter

        view.findViewById<Button>(R.id.btnClearPiano).setOnClickListener {
            PianoRollData.clearPattern(AudioEngine.currentPattern)
            adapter.notifyDataSetChanged()
            updateInfo()
        }

        view.findViewById<Button>(R.id.btnChord)?.setOnClickListener {
            ChordTool.showChordDialog(requireContext()) { rootKey, chordType ->
                ChordTool.insertChord(rootKey, chordType, 0)
                adapter.notifyDataSetChanged()
                updateInfo()
                Toast.makeText(requireContext(),
                    "Chord ${MusicTheory.NOTE_NAMES[rootKey]} ${chordType.displayName}",
                    Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnProgression)?.setOnClickListener {
            ChordTool.showProgressionDialog(requireContext()) { prog ->
                ChordTool.insertProgression(prog)
                adapter.notifyDataSetChanged()
                updateInfo()
                Toast.makeText(requireContext(),
                    "Progression: ${prog.name}", Toast.LENGTH_SHORT).show()
            }
        }

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

        view.findViewById<Button>(R.id.btnArp)?.setOnClickListener {
            ChordTool.showChordDialog(requireContext()) { rootKey, chordType ->
                // FIX: pakai named parameter untuk pattern_type
                ChordTool.arpeggiate(
                    rootKey = rootKey,
                    type = chordType,
                    pattern = AudioEngine.currentPattern,
                    startStep = 0,
                    stepsPerNote = 1,
                    pattern_type = ChordTool.ArpPattern.UP
                )
                adapter.notifyDataSetChanged()
                updateInfo()
                Toast.makeText(requireContext(),
                    "Arpeggio: ${MusicTheory.NOTE_NAMES[rootKey]} ${chordType.displayName}",
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
