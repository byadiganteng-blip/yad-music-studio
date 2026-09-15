package com.yad.musicstudio

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logger — Log lengkap dengan timestamp, tag, level.
 * Tulis ke logcat + file.
 */
object Logger {

    private const val TAG = "YadMusic"
    private const val LOG_FILENAME = "yad_music.log"
    private const val MAX_LOG_SIZE = 2 * 1024 * 1024L  // 2 MB

    private var context: Context? = null
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(ctx: Context) {
        context = ctx
        try {
            val dir = File(ctx.getExternalFilesDir(null), "logs")
            if (!dir.exists()) dir.mkdirs()
            logFile = File(dir, LOG_FILENAME)

            // Rotate log kalau > 2 MB
            if (logFile!!.exists() && logFile!!.length() > MAX_LOG_SIZE) {
                val backup = File(dir, "yad_music_old.log")
                if (backup.exists()) backup.delete()
                logFile!!.renameTo(backup)
                logFile = File(dir, LOG_FILENAME)
            }

            // Log device info
            i("Logger", "═══════════════════════════════════")
            i("Logger", "SESSION START")
            i("Logger", "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            i("Logger", "Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
            i("Logger", "Log dir: ${dir.absolutePath}")
            i("Logger", "═══════════════════════════════════")
        } catch (e: Exception) {
            Log.e(TAG, "Logger init error", e)
        }
    }

    fun i(tag: String, msg: String) = log("INFO", tag, msg, null)
    fun w(tag: String, msg: String) = log("WARN", tag, msg, null)
    fun e(tag: String, msg: String, ex: Throwable? = null) = log("ERROR", tag, msg, ex)
    fun d(tag: String, msg: String) = log("DEBUG", tag, msg, null)

    private fun log(level: String, tag: String, msg: String, ex: Throwable?) {
        val timestamp = dateFormat.format(Date())
        val line = "[$timestamp] [$level] [$tag] $msg"

        // Logcat
        when (level) {
            "DEBUG" -> Log.d(TAG, line)
            "INFO"  -> Log.i(TAG, line)
            "WARN"  -> Log.w(TAG, line)
            "ERROR" -> Log.e(TAG, line, ex)
        }

        // File
        try {
            logFile?.appendText(line + "\n")
            if (ex != null) {
                logFile?.appendText("  Stack: ${ex.stackTraceToString()}\n")
            }
        } catch (ignored: Exception) {}
    }

    /**
     * Log section separator.
     */
    fun section(title: String) {
        i("═════", "═══════════ $title ═══════════")
    }

    /**
     * Log semua exception di thread.
     */
    fun logException(tag: String, ex: Throwable) {
        e(tag, "Exception: ${ex.message}", ex)
    }

    /**
     * Dapatkan path file log.
     */
    fun getLogPath(): String? = logFile?.absolutePath

    /**
     * Baca isi log file.
     */
    fun readLog(): String {
        return try {
            logFile?.readText() ?: "No log file"
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }

    /**
     * Clear log file.
     */
    fun clearLog() {
        try {
            logFile?.writeText("")
        } catch (ignored: Exception) {}
    }
}
