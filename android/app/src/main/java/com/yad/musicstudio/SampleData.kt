package com.yad.musicstudio

import java.io.File

/**
 * SampleData — metadata sample audio yang di-load user.
 */
data class SampleData(
    val id: String,
    val name: String,
    val filePath: String,
    val trackIndex: Int,
    val fileSize: Long,
    val durationMs: Long,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun exists(): Boolean = File(filePath).exists()

    fun toJson(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "file_path" to filePath,
        "track_index" to trackIndex,
        "file_size" to fileSize,
        "duration_ms" to durationMs,
        "added_at" to addedAt
    )

    companion object {
        fun fromJson(map: Map<String, Any>): SampleData {
            return SampleData(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                filePath = map["file_path"] as? String ?: "",
                trackIndex = (map["track_index"] as? Number)?.toInt() ?: 0,
                fileSize = (map["file_size"] as? Number)?.toLong() ?: 0L,
                durationMs = (map["duration_ms"] as? Number)?.toLong() ?: 0L,
                addedAt = (map["added_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}
