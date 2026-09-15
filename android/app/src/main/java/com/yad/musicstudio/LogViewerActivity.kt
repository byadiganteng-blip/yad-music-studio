package com.yad.musicstudio

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * LogViewerActivity — Lihat log di dalam app.
 */
class LogViewerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout sederhana
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFF0F172A.toInt())
        }

        // Header
        val header = TextView(this).apply {
            text = "📋 Log Viewer"
            textSize = 20f
            setTextColor(0xFF60A5FA.toInt())
            setPadding(0, 0, 0, 16)
        }
        layout.addView(header)

        // Buttons row
        val btnRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        val btnRefresh = Button(this).apply {
            text = "🔄 Refresh"
            textSize = 12f
            setBackgroundColor(0xFF3B82F6.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val btnClear = Button(this).apply {
            text = "🗑️ Clear"
            textSize = 12f
            setBackgroundColor(0xFFEF4444.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        val btnShare = Button(this).apply {
            text = "📤 Share"
            textSize = 12f
            setBackgroundColor(0xFF10B981.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        btnRow.addView(btnRefresh)
        btnRow.addView(btnClear)
        btnRow.addView(btnShare)
        layout.addView(btnRow)

        // Log content
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(0xFF1E293B.toInt())
            setPadding(16, 16, 16, 16)
        }

        val tvLog = TextView(this).apply {
            textSize = 10f
            setTextColor(0xFFCBD5E1.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            setTextIsSelectable(true)
        }

        scrollView.addView(tvLog)
        layout.addView(scrollView, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            0, 1f
        ))

        setContentView(layout)

        // Load log
        fun loadLog() {
            tvLog.text = Logger.readLog()
        }
        loadLog()

        btnRefresh.setOnClickListener {
            loadLog()
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
        }

        btnClear.setOnClickListener {
            Logger.clearLog()
            loadLog()
            Toast.makeText(this, "Log cleared", Toast.LENGTH_SHORT).show()
        }

        btnShare.setOnClickListener {
            val logPath = Logger.getLogPath()
            if (logPath != null) {
                ShareHelper.shareAudio(this, logPath)
            }
        }
    }
}
