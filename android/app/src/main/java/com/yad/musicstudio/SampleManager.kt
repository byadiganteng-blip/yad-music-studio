package com.yad.musicstudio

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * SampleManager — manage library sample audio.
 * Simpan di /files/samples/
 */
object SampleManager {

    private const val TAG = "SampleManager"
    private const val SAMPLES_DIR = "samples"
    private const val INDEX_FILE = "samples_index.json"

    private val samples = mutableListOf<SampleData>()
    private var context: Context? = null

    fun init(ctx: Context) {
        context = ctx
        loadIndex()
        Log.i(TAG, "Initialized with ${samples.size} samples")
    }

    private fun getSamplesDir(): File {
        val dir = File(context?.filesDir, SAMPLES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Import sample dari URI (dari file picker).
     */
    fun importSample(context: Context, uri: android.net.Uri, name: String, trackIndex: Int): SampleData? {
        try {
            val extension = getExtension(context, uri)
            val fileName = "${System.currentTimeMillis()}_${name.replace(" ", "_")}.$extension"
            val destFile = File(getSamplesDir(), fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val size = destFile.length()
            val durationMs = getAudioDuration(destFile)

            val sample = SampleData(
                id = fileName,
                name = name,
                filePath = destFile.absolutePath,
                trackIndex = trackIndex,
                fileSize = size,
                durationMs = durationMs
            )

            samples.add(sample)
            saveIndex()

            Log.i(TAG, "Imported: ${sample.name} (${size / 1024} KB)")
            return sample
        } catch (e: Exception) {
            Log.e(TAG, "Import failed", e)
            return null
        }
    }

    private fun getExtension(context: Context, uri: android.net.Uri): String {
        val mime = context.contentResolver.getType(uri) ?: return "wav"
        return when {
            mime.contains("mpeg") || mime.contains("mp3") -> "mp3"
            mime.contains("wav") -> "wav"
            mime.contains("ogg") -> "ogg"
            mime.contains("aac") || mime.contains("m4a") -> "m4a"
            else -> "wav"
        }
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val mediaPlayer = android.media.MediaPlayer()
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()
            val duration = mediaPlayer.duration.toLong()
            mediaPlayer.release()
            duration
        } catch (e: Exception) {
            Log.e(TAG, "getDuration failed", e)
            0L
        }
    }

    fun getSamplesForTrack(trackIndex: Int): List<SampleData> {
        return samples.filter { it.trackIndex == trackIndex }
    }

    fun getAllSamples(): List<SampleData> = samples.toList()

    fun getSampleById(id: String): SampleData? = samples.find { it.id == id }

    fun deleteSample(sample: SampleData): Boolean {
        return try {
            File(sample.filePath).delete()
            samples.remove(sample)
            saveIndex()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            false
        }
    }

    fun renameSample(sample: SampleData, newName: String): Boolean {
        val idx = samples.indexOfFirst { it.id == sample.id }
        if (idx < 0) return false
        samples[idx] = sample.copy(name = newName)
        saveIndex()
        return true
    }

    private fun saveIndex() {
        try {
            val json = JSONArray()
            for (s in samples) {
                json.put(JSONObject(s.toJson()))
            }
            File(context?.filesDir, INDEX_FILE).writeText(json.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Save index failed", e)
        }
    }

    private fun loadIndex() {
        try {
            val file = File(context?.filesDir, INDEX_FILE)
            if (!file.exists()) return
            val json = JSONArray(file.readText())
            samples.clear()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                val map = mutableMapOf<String, Any>()
                obj.keys().forEach { key -> map[key] = obj.get(key) }
                val sample = SampleData.fromJson(map)
                if (sample.exists()) {
                    samples.add(sample)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Load index failed", e)
        }
    }

    fun clearAll() {
        samples.forEach { File(it.filePath).delete() }
        samples.clear()
        saveIndex()
    }
}
