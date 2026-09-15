package com.yad.musicstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SequencerFragment : Fragment() {

    private lateinit var zoomableSequencer: ZoomableSequencerView
    private lateinit var tvBpm: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnMetronome: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sequencer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvBpm = view.findViewById(R.id.tvBpm)
        btnPlay = view.findViewById(R.id.btnPlay)
        btnMetronome = view.findViewById(R.id.btnMetronome)
        zoomableSequencer = view.findViewById(R.id.zoomableSequencer)

        zoomableSequencer.setOnStepToggleListener { track, step ->
            AudioEngine.toggleStep(track, step)
            AudioEngine.previewSound(track)
            zoomableSequencer.invalidate()
        }

        // BPM
        val seek = view.findViewById<SeekBar>(R.id.seekBpm)
        seek.max = 300 - 40
        seek.progress = AudioEngine.bpm - 40
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                AudioEngine.setBpm(40 + p)
                tvBpm.text = "${AudioEngine.bpm} BPM"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Play
        btnPlay.setOnClickListener {
            if (AudioEngine.isPlaying) {
                AudioEngine.stop()
                btnPlay.text = "▶ PLAY"
            } else {
                AudioEngine.play()
                btnPlay.text = "⏹ STOP"
            }
        }

        // Zoom
        view.findViewById<Button>(R.id.btnZoomIn).setOnClickListener {
            zoomableSequencer.zoomIn()
        }
        view.findViewById<Button>(R.id.btnZoomOut).setOnClickListener {
            zoomableSequencer.zoomOut()
        }

        // Metronome
        btnMetronome.setOnClickListener {
            Metronome.toggle()
            btnMetronome.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (Metronome.enabled) 0xFF10B981.toInt() else 0xFF3B82F6.toInt()
            )
            Toast.makeText(requireContext(),
                if (Metronome.enabled) "Metronome ON" else "Metronome OFF",
                Toast.LENGTH_SHORT).show()
        }

        // Quantize
        view.findViewById<Button>(R.id.btnQuantize).setOnClickListener {
            Quantizer.showQuantizeDialog(requireContext())
        }

        // Undo/Redo
        view.findViewById<Button>(R.id.btnUndo).setOnClickListener {
            if (AudioEngine.undo()) {
                zoomableSequencer.invalidate()
                Toast.makeText(requireContext(), "↶ Undo", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Tidak ada undo", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnRedo).setOnClickListener {
            if (AudioEngine.redo()) {
                zoomableSequencer.invalidate()
                Toast.makeText(requireContext(), "↷ Redo", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Tidak ada redo", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear
        view.findViewById<Button>(R.id.btnClear).setOnClickListener {
            UndoRedoManager.push(UndoRedoManager.Action(
                type = "clear_pattern",
                data = mapOf("snapshot" to AudioEngine.snapshotCurrentPattern())
            ))
            AudioEngine.clearPattern()
            zoomableSequencer.invalidate()
        }

        // Demo
        view.findViewById<Button>(R.id.btnDemo).setOnClickListener {
            AudioEngine.loadDemoPattern()
            zoomableSequencer.invalidate()
        }

        // Save
        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val name = "project_${System.currentTimeMillis()}"
            val path = AudioEngine.saveProject(name)
            Toast.makeText(requireContext(),
                if (path.isNotEmpty()) "💾 Saved" else "❌ Failed",
                Toast.LENGTH_SHORT).show()
        }

        // Load
        view.findViewById<Button>(R.id.btnLoad).setOnClickListener {
            val projects = AudioEngine.listProjects()
            if (projects.isEmpty()) {
                Toast.makeText(requireContext(), "No projects", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val names = projects.map { it.nameWithoutExtension }.toTypedArray()
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Load Project")
                .setItems(names) { _, which ->
                    AudioEngine.loadProject(projects[which].absolutePath)
                    zoomableSequencer.invalidate()
                }
                .show()
        }

        // Export
        view.findViewById<Button>(R.id.btnExport).setOnClickListener {
            val path = AudioEngine.exportWav()
            Toast.makeText(requireContext(),
                if (path.isNotEmpty()) "🎵 Exported" else "❌ Failed",
                Toast.LENGTH_SHORT).show()
        }
    }
}
