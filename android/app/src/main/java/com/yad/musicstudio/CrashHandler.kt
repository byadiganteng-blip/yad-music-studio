package com.yad.musicstudio

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CrashHandler — Catch semua uncaught exception dan log ke file.
 */
object CrashHandler {

    private const val TAG = "CrashHandler"
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e(TAG, "═══════════ CRASH ═══════════")
                Log.e(TAG, "Thread: ${thread.name}")
                Log.e(TAG, "Exception: ${throwable.javaClass.name}")
                Log.e(TAG, "Message: ${throwable.message}")
                Log.e(TAG, throwable.stackTraceToString())

                // Log ke Logger
                Logger.e("CRASH", "Thread: ${thread.name}", throwable)

                // Log ke file crash terpisah
                saveCrashLog(throwable, thread.name)
            } catch (e: Exception) {
                Log.e(TAG, "Error logging crash", e)
            } finally {
                // Panggil handler default (biar system kill app)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        Logger.i(TAG, "CrashHandler installed")
    }

    private fun saveCrashLog(throwable: Throwable, threadName: String) {
        try {
            val ctx = context ?: return
            val dir = File(ctx.getExternalFilesDir(null), "logs")
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val file = File(dir, "crash_$timestamp.txt")

            val sw = StringWriter()
            val pw = PrintWriter(sw)
            pw.println("═══════════════════════════════════")
            pw.println("CRASH REPORT")
            pw.println("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            pw.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            pw.println("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            pw.println("Thread: $threadName")
            pw.println("═══════════════════════════════════")
            pw.println()
            pw.println("Exception: ${throwable.javaClass.name}")
            pw.println("Message: ${throwable.message}")
            pw.println()
            pw.println("Stack trace:")
            throwable.printStackTrace(pw)
            pw.flush()

            file.writeText(sw.toString())
            Log.e(TAG, "Crash log saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving crash log", e)
        }
    }
}
