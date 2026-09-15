package com.yad.musicstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class PlaylistFragment : Fragment() {

    private lateinit var playlistView: PlaylistView
    private lateinit var tvInfo: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        playlistView = view.findViewById(R.id.playlistView)
        tvInfo = view.findViewById(R.id.tvPlaylistInfo)

        playlistView.setOnBlockClickListener { barStart, patternIndex ->
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Block di bar $barStart (Pattern ${patternIndex + 1})")
                .setItems(arrayOf("🗑️ Hapus block")) { _, _ ->
                    PlaylistData.removeBlock(barStart, patternIndex)
                    playlistView.invalidate()
                    updateInfo()
                }
                .show()
        }

        // Add block button
        view.findViewById<Button>(R.id.btnAddBlock)?.setOnClickListener {
            showAddBlockDialog()
        }

        // Clear button
        view.findViewById<Button>(R.id.btnClearPlaylist)?.setOnClickListener {
            PlaylistData.clear()
            playlistView.invalidate()
            updateInfo()
        }

        // Play song button
        view.findViewById<Button>(R.id.btnPlaySong)?.setOnClickListener {
            Toast.makeText(requireContext(),
                "Song Mode coming soon", Toast.LENGTH_SHORT).show()
        }

        updateInfo()
    }

    private fun showAddBlockDialog() {
        val patternSpinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                (1..AudioEngine.MAX_PATTERNS).map { "Pattern $it" })
        }

        val barInput = EditText(requireContext()).apply {
            hint = "Bar ke- (0-127)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val lengthInput = EditText(requireContext()).apply {
            hint = "Panjang (bar)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText("1")
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            addView(TextView(requireContext()).apply {
                text = "Pattern:"
                setTextColor(0xFFFFFFFF.toInt())
            })
            addView(patternSpinner)
            addView(TextView(requireContext()).apply {
                text = "Bar start:"
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 15, 0, 0)
            })
            addView(barInput)
            addView(TextView(requireContext()).apply {
                text = "Panjang:"
                setTextColor(0xFFFFFFFF.toInt())
                setPadding(0, 15, 0, 0)
            })
            addView(lengthInput)
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("➕ Tambah Block")
            .setView(layout)
            .setPositiveButton("Tambah") { _, _ ->
                val patternIdx = patternSpinner.selectedItemPosition
                val bar = barInput.text.toString().toIntOrNull() ?: 0
                val len = lengthInput.text.toString().toIntOrNull() ?: 1

                val ok = PlaylistData.addBlock(patternIdx, bar, len)
                if (ok) {
                    playlistView.invalidate()
                    updateInfo()
                } else {
                    Toast.makeText(requireContext(),
                        "Overlap atau invalid", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun updateInfo() {
        val blocks = PlaylistData.getAll()
        tvInfo.text = "🎼 ${blocks.size} blocks | Total ${PlaylistData.getTotalBars()} bars"
    }
}
