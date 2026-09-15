package com.yad.musicstudio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var sequencerView: RecyclerView
    private lateinit var mixerView: RecyclerView
    private lateinit var tvBpm: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnPlay: Button

    private val sequencerAdapter = SequencerAdapter { track, step ->
        onStepToggled(track, step)
    }
    private val mixerAdapter = MixerAdapter { track, volume ->
        onVolumeChanged(track, volume)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudioPermission()
        AudioEngine.init(this)

        sequencerView = findViewById(R.id.sequencerView)
        mixerView = findViewById(R.id.mixerView)
        tvBpm = findViewById(R.id.tvBpm)
        tvStatus = findViewById(R.id.tvStatus)
        btnPlay = findViewById(R.id.btnPlay)

        setupSequencer()
        setupMixer()
        setupControls()
    }

    private fun setupSequencer() {
        sequencerAdapter.setTracks(AudioEngine.getTracks())
        sequencerView.layoutManager = LinearLayoutManager(this)
        sequencerView.adapter = sequencerAdapter
    }

    private fun setupMixer() {
        mixerAdapter.setTracks(AudioEngine.getTracks())
        mixerView.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )
        mixerView.adapter = mixerAdapter
    }

    private fun setupControls() {
        tvBpm.text = "${AudioEngine.bpm} BPM"

        findViewById<SeekBar>(R.id.seekBpm).apply {
            max = 180 - 60
            progress = AudioEngine.bpm - 60
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, u: Boolean) {
                    AudioEngine.bpm = 60 + p
                    tvBpm.text = "${AudioEngine.bpm} BPM"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }

        btnPlay.setOnClickListener {
            if (AudioEngine.isPlaying) {
                AudioEngine.stop()
                btnPlay.text = "▶️ PLAY"
                tvStatus.text = "⏹️ Stopped"
            } else {
                AudioEngine.play()
                btnPlay.text = "⏹️ STOP"
                tvStatus.text = "▶️ Playing..."
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            AudioEngine.clearAll()
            sequencerAdapter.notifyDataSetChanged()
            tvStatus.text = "🗑️ Cleared"
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val name = "pattern_${System.currentTimeMillis()}.json"
            AudioEngine.savePattern(name)
            tvStatus.text = "💾 Saved: $name"
        }

        findViewById<Button>(R.id.btnLoad).setOnClickListener {
            val latest = AudioEngine.loadLatest()
            sequencerAdapter.notifyDataSetChanged()
            tvStatus.text = if (latest != null) "📂 Loaded: $latest" else "❌ No saved pattern"
        }

        findViewById<Button>(R.id.btnDemo).setOnClickListener {
            AudioEngine.loadDemoPattern()
            sequencerAdapter.notifyDataSetChanged()
            tvStatus.text = "🎵 Demo pattern loaded"
        }
    }

    private fun onStepToggled(track: Int, step: Int) {
        AudioEngine.toggleStep(track, step)
        sequencerAdapter.notifyItemChanged(track)
        AudioEngine.previewSound(track)
    }

    private fun onVolumeChanged(track: Int, volume: Float) {
        AudioEngine.setVolume(track, volume)
    }

    private fun requestAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioEngine.stop()
    }
}
