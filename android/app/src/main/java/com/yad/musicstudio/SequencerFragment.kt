package com.yad.musicstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SequencerFragment : Fragment() {

    private lateinit var adapter: SequencerAdapter
    private lateinit var tvBpm: TextView
    private lateinit var btnPlay: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sequencer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvSequencer)
        tvBpm = view.findViewById(R.id.tvBpm)
        btnPlay = view.findViewById(R.id.btnPlay)

        adapter = SequencerAdapter { track, step ->
            AudioEngine.toggleStep(track, step)
            AudioEngine.previewSound(track)
        }
        adapter.setTracks(getTracks())
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

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

        btnPlay.setOnClickListener {
            if (AudioEngine.isPlaying) {
                AudioEngine.stop()
                btnPlay.text = "▶ PLAY"
            } else {
                AudioEngine.play()
                btnPlay.text = "⏹ STOP"
            }
        }

        view.findViewById<Button>(R.id.btnClear).setOnClickListener {
            AudioEngine.clearPattern()
            refresh()
        }
        view.findViewById<Button>(R.id.btnDemo).setOnClickListener {
            AudioEngine.loadDemoPattern()
            refresh()
        }
        view.findViewById<Button>(R.id.btnSave).setOnClickListener {
            val name = "project_${System.currentTimeMillis()}"
            val path = AudioEngine.saveProject(name)
            Toast.makeText(requireContext(), if (path.isNotEmpty()) "💾 Saved!" else "❌ Failed", Toast.LENGTH_SHORT).show()
        }
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
                    refresh()
                }
                .show()
        }
        view.findViewById<Button>(R.id.btnExport).setOnClickListener {
            val path = AudioEngine.exportWav()
            Toast.makeText(requireContext(), if (path.isNotEmpty()) "🎵 Exported WAV!" else "❌ Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTracks(): List<AudioEngine.Track> {
        return (0 until AudioEngine.TRACKS).map { i ->
            AudioEngine.Track(
                index = i,
                name = AudioEngine.trackNames[i],
                steps = AudioEngine.getCurrentPattern()[i].copyOf(),
                volume = AudioEngine.getVolume(i)
            )
        }
    }

    private fun refresh() {
        adapter.setTracks(getTracks())
        adapter.notifyDataSetChanged()
    }
}
