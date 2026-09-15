package com.yad.musicstudio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SampleBrowserFragment : Fragment() {

    private lateinit var adapter: SampleAdapter
    private lateinit var tvInfo: TextView
    private lateinit var spinnerTrack: Spinner

    private val pickAudio = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importFromUri(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sample_browser, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rvSamples)
        tvInfo = view.findViewById(R.id.tvSampleInfo)
        spinnerTrack = view.findViewById(R.id.spinnerTrack)

        adapter = SampleAdapter(
            onPlay = { sample ->
                playSample(sample)
            },
            onLongPress = { sample ->
                showOptionsDialog(sample)
            }
        )
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Track spinner
        spinnerTrack.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            AudioEngine.trackNames)

        // Import button
        view.findViewById<Button>(R.id.btnImportSample).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            pickAudio.launch(intent)
        }

        // Clear all button
        view.findViewById<Button>(R.id.btnClearSamples).setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Hapus semua sample?")
                .setMessage("Semua file sample akan dihapus.")
                .setPositiveButton("Hapus") { _, _ ->
                    SampleManager.clearAll()
                    refreshList()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        refreshList()
    }

    private fun importFromUri(uri: android.net.Uri) {
        val trackIndex = spinnerTrack.selectedItemPosition
        val name = "Sample_${System.currentTimeMillis() % 10000}"

        val sample = SampleManager.importSample(
            requireContext(), uri, name, trackIndex
        )

        if (sample != null) {
            Toast.makeText(requireContext(),
                "✅ Sample di-import: ${sample.name}", Toast.LENGTH_SHORT).show()
            refreshList()
        } else {
            Toast.makeText(requireContext(),
                "❌ Gagal import sample", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playSample(sample: SampleData) {
        try {
            val mp = android.media.MediaPlayer()
            mp.setDataSource(sample.filePath)
            mp.prepare()
            mp.start()
            mp.setOnCompletionListener { it.release() }
        } catch (e: Exception) {
            Toast.makeText(requireContext(),
                "Gagal play: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOptionsDialog(sample: SampleData) {
        val options = arrayOf("▶️ Play", "✏️ Rename", "🗑️ Hapus")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(sample.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> playSample(sample)
                    1 -> showRenameDialog(sample)
                    2 -> showDeleteConfirm(sample)
                }
            }
            .show()
    }

    private fun showRenameDialog(sample: SampleData) {
        val input = EditText(requireContext()).apply {
            setText(sample.name)
            setTextColor(0xFFFFFFFF.toInt())
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Rename Sample")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                SampleManager.renameSample(sample, input.text.toString())
                refreshList()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDeleteConfirm(sample: SampleData) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hapus ${sample.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                SampleManager.deleteSample(sample)
                refreshList()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun refreshList() {
        val samples = SampleManager.getAllSamples()
        adapter.setItems(samples)
        tvInfo.text = "🎵 ${samples.size} samples di library"
    }
}
