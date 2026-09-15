package com.yad.musicstudio

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object PlaylistData {

    private const val TAG = "PlaylistData"
    const val MAX_BLOCKS = 64
    const val MAX_BARS = 128

    data class PatternBlock(
        val patternIndex: Int,
        val barStart: Int,
        val barLength: Int = 1
    )

    private val blocks = mutableListOf<PatternBlock>()

    // FIX: rename dari totalBars → defaultTotalBars (hindari clash dengan getTotalBars())
    var defaultTotalBars: Int = 16

    fun addBlock(patternIndex: Int, barStart: Int, barLength: Int = 1): Boolean {
        if (blocks.size >= MAX_BLOCKS) return false
        if (patternIndex !in 0 until AudioEngine.MAX_PATTERNS) return false
        if (barStart < 0 || barStart >= MAX_BARS) return false

        val end = barStart + barLength
        val overlap = blocks.any { b ->
            val bEnd = b.barStart + b.barLength
            !(end <= b.barStart || barStart >= bEnd)
        }
        if (overlap) {
            Log.w(TAG, "Overlap detected at bar $barStart")
            return false
        }

        blocks.add(PatternBlock(patternIndex, barStart, barLength))
        blocks.sortBy { it.barStart }
        return true
    }

    fun removeBlock(barStart: Int, patternIndex: Int): Boolean {
        return blocks.removeAll { it.barStart == barStart && it.patternIndex == patternIndex }
    }

    fun removeBlockAt(barStart: Int): Boolean {
        return blocks.removeAll { it.barStart == barStart }
    }

    fun clear() {
        blocks.clear()
    }

    fun getAll(): List<PatternBlock> = blocks.toList()

    fun getPatternAtBar(bar: Int): Int {
        return blocks.firstOrNull {
            bar >= it.barStart && bar < it.barStart + it.barLength
        }?.patternIndex ?: -1
    }

    fun toJson(): JSONArray {
        val arr = JSONArray()
        for (b in blocks) {
            val obj = JSONObject()
            obj.put("pattern", b.patternIndex)
            obj.put("bar_start", b.barStart)
            obj.put("bar_length", b.barLength)
            arr.put(obj)
        }
        return arr
    }

    fun fromJson(arr: JSONArray) {
        blocks.clear()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            blocks.add(PatternBlock(
                patternIndex = obj.getInt("pattern"),
                barStart = obj.getInt("bar_start"),
                barLength = obj.optInt("bar_length", 1)
            ))
        }
        blocks.sortBy { it.barStart }
    }

    // FIX: rename dari getTotalBars() → calculateTotalBars()
    fun calculateTotalBars(): Int {
        val maxBar = blocks.maxOfOrNull { it.barStart + it.barLength } ?: 0
        return maxOf(maxBar, defaultTotalBars)
    }
}
