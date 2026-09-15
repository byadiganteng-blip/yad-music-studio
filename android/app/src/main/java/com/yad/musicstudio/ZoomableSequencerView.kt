package com.yad.musicstudio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class ZoomableSequencerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var steps = 16
    private var tracks = 16
    private var stepSize = 60f
    private var trackHeight = 60f

    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f
    private var minScale = 0.5f
    private var maxScale = 3.0f

    private val bgPaint = Paint().apply { color = Color.parseColor("#0F172A") }
    private val stepOffPaint = Paint().apply { color = Color.parseColor("#1E293B") }
    private val stepAccentPaint = Paint().apply { color = Color.parseColor("#334155") }
    private val stepOnPaint = Paint().apply { color = Color.parseColor("#EF4444") }
    private val stepCurrentPaint = Paint().apply { color = Color.parseColor("#FBBF24") }
    private val borderPaint = Paint().apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val trackNamePaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        textSize = 20f
        isAntiAlias = true
    }

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(minScale, maxScale)
                invalidate()
                return true
            }
        })

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isPanning = false

    private var onStepToggle: ((track: Int, step: Int) -> Unit)? = null

    fun setOnStepToggleListener(listener: (track: Int, step: Int) -> Unit) {
        onStepToggle = listener
    }

    fun zoomIn() {
        scaleFactor = (scaleFactor * 1.2f).coerceAtMost(maxScale)
        invalidate()
    }

    fun zoomOut() {
        scaleFactor = (scaleFactor / 1.2f).coerceAtLeast(minScale)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val effStep = stepSize * scaleFactor
        val effTrack = trackHeight * scaleFactor

        canvas.save()
        canvas.translate(offsetX, offsetY)

        val currentPattern = AudioEngine.getCurrentPattern()
        val currentPlaying = AudioEngine.currentStep
        val nameWidth = 90f * scaleFactor

        for (t in 0 until tracks) {
            canvas.drawText(
                AudioEngine.trackNames[t],
                10f, (t * effTrack) + effTrack * 0.7f,
                trackNamePaint
            )

            for (s in 0 until steps) {
                val x = nameWidth + s * effStep
                val y = t * effTrack

                val isOn = currentPattern.getOrNull(t)?.getOrNull(s) == true
                val isCurrent = (s == currentPlaying)
                val isAccent = (s % 4 == 0)

                val paint = when {
                    isCurrent -> stepCurrentPaint
                    isOn -> stepOnPaint
                    isAccent -> stepAccentPaint
                    else -> stepOffPaint
                }

                val pad = 2f * scaleFactor
                canvas.drawRect(
                    x + pad, y + pad,
                    x + effStep - pad, y + effTrack - pad,
                    paint
                )
                canvas.drawRect(
                    x + pad, y + pad,
                    x + effStep - pad, y + effTrack - pad,
                    borderPaint
                )
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val effStep = stepSize * scaleFactor
        val effTrack = trackHeight * scaleFactor
        val nameWidth = 90f * scaleFactor

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isPanning = false
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                isPanning = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    offsetX += dx
                    offsetY += dy
                    isPanning = true
                    invalidate()
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!isPanning && !scaleDetector.isInProgress) {
                    val localX = event.x - offsetX - nameWidth
                    val localY = event.y - offsetY

                    val step = (localX / effStep).toInt()
                    val track = (localY / effTrack).toInt()

                    if (step in 0 until steps && track in 0 until tracks) {
                        UndoRedoManager.push(UndoRedoManager.Action(
                            type = "toggle_step",
                            data = mapOf("track" to track, "step" to step)
                        ))
                        onStepToggle?.invoke(track, step)
                        invalidate()
                    }
                }
                isPanning = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
