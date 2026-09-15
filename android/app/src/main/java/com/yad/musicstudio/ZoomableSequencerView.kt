package com.yad.musicstudio

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/**
 * ZoomableSequencerView — Custom view sequencer dengan zoom + pan.
 * 
 * Gestures:
 * - Pinch zoom: zoom in/out
 * - Two-finger drag: pan horizontal
 * - Tap: toggle step
 */
class ZoomableSequencerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    // Config
    private var steps = 16
    private var tracks = 16
    private var stepSize = 60f
    private var trackHeight = 60f

    // View state
    private var scaleFactor = 1.0f
    private var offsetX = 0f
    private var offsetY = 0f
    private var minScale = 0.5f
    private var maxScale = 3.0f

    // Paints
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
        textSize = 24f
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
    private var currentStep = -1

    fun setOnStepToggleListener(listener: (track: Int, step: Int) -> Unit) {
        onStepToggle = listener
    }

    fun setPattern(pattern: Array<BooleanArray>) {
        invalidate()
    }

    fun setCurrentStep(step: Int) {
        currentStep = step
        invalidate()
    }

    fun zoomIn() {
        scaleFactor = (scaleFactor * 1.2f).coerceAtMost(maxScale)
        invalidate()
    }

    fun zoomOut() {
        scaleFactor = (scaleFactor / 1.2f).coerceAtLeast(minScale)
        invalidate()
    }

    fun resetZoom() {
        scaleFactor = 1.0f
        offsetX = 0f
        offsetY = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val effectiveStepSize = stepSize * scaleFactor
        val effectiveTrackHeight = trackHeight * scaleFactor

        canvas.save()
        canvas.translate(offsetX, offsetY)

        val currentPattern = AudioEngine.getCurrentPattern()
        val currentPlayingStep = AudioEngine.getCurrentStepPublic()

        val nameWidth = 80f * scaleFactor

        for (t in 0 until tracks) {
            // Track name
            canvas.drawText(
                AudioEngine.trackNames[t],
                10f, (t * effectiveTrackHeight) + effectiveTrackHeight * 0.7f,
                trackNamePaint
            )

            // Steps
            for (s in 0 until steps) {
                val x = nameWidth + s * effectiveStepSize
                val y = t * effectiveTrackHeight

                val isOn = currentPattern.getOrNull(t)?.getOrNull(s) == true
                val isCurrentPlaying = (s == currentPlayingStep)
                val isAccent = (s % 4 == 0)

                val paint = when {
                    isCurrentPlaying -> stepCurrentPaint
                    isOn -> stepOnPaint
                    isAccent -> stepAccentPaint
                    else -> stepOffPaint
                }

                val pad = 2f * scaleFactor
                canvas.drawRect(
                    x + pad, y + pad,
                    x + effectiveStepSize - pad, y + effectiveTrackHeight - pad,
                    paint
                )

                // Border
                canvas.drawRect(
                    x + pad, y + pad,
                    x + effectiveStepSize - pad, y + effectiveTrackHeight - pad,
                    borderPaint
                )
            }
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        val effectiveStepSize = stepSize * scaleFactor
        val effectiveTrackHeight = trackHeight * scaleFactor
        val nameWidth = 80f * scaleFactor

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
                    // Pan dengan 2 jari
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
                    // Tap → toggle step
                    val localX = event.x - offsetX - nameWidth
                    val localY = event.y - offsetY

                    val step = (localX / effectiveStepSize).toInt()
                    val track = (localY / effectiveTrackHeight).toInt()

                    if (step in 0 until steps && track in 0 until tracks) {
                        // Push undo
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
