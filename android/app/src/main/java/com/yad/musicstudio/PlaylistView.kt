package com.yad.musicstudio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * PlaylistView — Timeline untuk arrange pattern.
 */
class PlaylistView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var barWidth = 80f
    private var trackHeight = 60f
    private var offsetX = 0f
    private var offsetY = 0f

    private val bgPaint = Paint().apply { color = Color.parseColor("#0F172A") }
    private val barLinePaint = Paint().apply {
        color = Color.parseColor("#334155")
        strokeWidth = 1f
    }
    private val blockPaint = Paint().apply {
        color = Color.parseColor("#3B82F6")
        style = Paint.Style.FILL
    }
    private val blockStrokePaint = Paint().apply {
        color = Color.parseColor("#60A5FA")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        isAntiAlias = true
    }
    private val barNumPaint = Paint().apply {
        color = Color.parseColor("#94A3B8")
        textSize = 20f
        isAntiAlias = true
    }

    // Pattern colors
    private val patternColors = intArrayOf(
        0xFFEF4444.toInt(),  // Red
        0xFFF59E0B.toInt(),  // Orange
        0xFFFBBF24.toInt(),  // Yellow
        0xFF10B981.toInt(),  // Green
        0xFF06B6D4.toInt(),  // Cyan
        0xFF3B82F6.toInt(),  // Blue
        0xFF8B5CF6.toInt(),  // Purple
        0xFFEC4899.toInt()   // Pink
    )

    private var onBlockClick: ((barStart: Int, patternIndex: Int) -> Unit)? = null
    private var onBlockLongPress: ((barStart: Int, patternIndex: Int) -> Unit)? = null

    fun setOnBlockClickListener(l: (Int, Int) -> Unit) { onBlockClick = l }
    fun setOnBlockLongPressListener(l: (Int, Int) -> Unit) { onBlockLongPress = l }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        canvas.save()
        canvas.translate(offsetX, offsetY)

        val totalBars = PlaylistData.getTotalBars()
        val totalHeight = trackHeight * 8  // 8 track rows untuk pattern

        // Grid lines (bar)
        for (b in 0..totalBars) {
            val x = b * barWidth
            canvas.drawLine(x, 0f, x, totalHeight, barLinePaint)
            // Bar number
            canvas.drawText("$b", x + 5f, 20f, barNumPaint)
        }

        // Row lines (pattern slot)
        for (p in 0..8) {
            val y = p * trackHeight
            canvas.drawLine(0f, y, totalBars * barWidth, y, barLinePaint)
        }

        // Pattern blocks
        val blocks = PlaylistData.getAll()
        for (block in blocks) {
            val x = block.barStart * barWidth
            val y = block.patternIndex * trackHeight
            val w = block.barLength * barWidth
            val h = trackHeight

            // Block background
            val color = patternColors.getOrElse(block.patternIndex) { 0xFF3B82F6.toInt() }
            blockPaint.color = color

            val rect = RectF(x + 2f, y + 2f, x + w - 2f, y + h - 2f)
            canvas.drawRoundRect(rect, 8f, 8f, blockPaint)
            canvas.drawRoundRect(rect, 8f, 8f, blockStrokePaint)

            // Pattern name
            val name = "P${block.patternIndex + 1}"
            canvas.drawText(name, x + 15f, y + h / 2f + 8f, textPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val localX = event.x - offsetX
                val localY = event.y - offsetY

                val bar = (localX / barWidth).toInt()
                val patternSlot = (localY / trackHeight).toInt()

                if (bar in 0 until PlaylistData.MAX_BARS && patternSlot in 0..7) {
                    // Cari block di posisi ini
                    val existingBlock = PlaylistData.getAll().firstOrNull {
                        bar >= it.barStart && bar < it.barStart + it.barLength &&
                        patternSlot == it.patternIndex
                    }
                    if (existingBlock != null) {
                        onBlockClick?.invoke(existingBlock.barStart, existingBlock.patternIndex)
                    }
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
