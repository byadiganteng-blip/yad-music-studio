package com.yad.musicstudio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * PeakMeterView — Visual level meter untuk track.
 */
class PeakMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var level: Float = 0f  // 0.0 to 1.0
    private var peak: Float = 0f

    private val bgPaint = Paint().apply { color = Color.parseColor("#1E293B") }
    private val greenPaint = Paint().apply { color = Color.parseColor("#10B981") }
    private val yellowPaint = Paint().apply { color = Color.parseColor("#FBBF24") }
    private val redPaint = Paint().apply { color = Color.parseColor("#EF4444") }
    private val peakPaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
    }

    fun setLevel(l: Float) {
        level = l.coerceIn(0f, 1f)
        if (level > peak) peak = level
        invalidate()
    }

    fun resetPeak() {
        peak = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Level bar (vertical, bottom up)
        val barH = h * level
        val y = h - barH

        // Color based on level
        val paint = when {
            level > 0.9f -> redPaint
            level > 0.7f -> yellowPaint
            else -> greenPaint
        }

        canvas.drawRect(2f, y, w - 2f, h - 2f, paint)

        // Peak indicator
        val peakY = h - (h * peak)
        canvas.drawLine(2f, peakY, w - 2f, peakY, peakPaint)
    }
}
