package com.yad.musicstudio

import android.util.Log

/**
 * UndoRedoManager — Simpan history aksi, bisa undo/redo.
 */
object UndoRedoManager {

    private const val TAG = "UndoRedo"
    private const val MAX_HISTORY = 50

    data class Action(
        val type: String,          // "toggle_step", "set_volume", "clear", dll
        val data: Map<String, Any>, // data spesifik
        val timestamp: Long = System.currentTimeMillis()
    )

    private val undoStack = mutableListOf<Action>()
    private val redoStack = mutableListOf<Action>()

    fun push(action: Action) {
        undoStack.add(action)
        if (undoStack.size > MAX_HISTORY) {
            undoStack.removeAt(0)
        }
        redoStack.clear()  // reset redo saat ada aksi baru
        Log.d(TAG, "Pushed: ${action.type} (undo=${undoStack.size}, redo=${redoStack.size})")
    }

    fun undo(): Action? {
        if (undoStack.isEmpty()) return null
        val action = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(action)
        Log.d(TAG, "Undo: ${action.type}")
        return action
    }

    fun redo(): Action? {
        if (redoStack.isEmpty()) return null
        val action = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(action)
        Log.d(TAG, "Redo: ${action.type}")
        return action
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun getUndoCount(): Int = undoStack.size
    fun getRedoCount(): Int = redoStack.size
}
