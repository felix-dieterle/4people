package com.fourpeople.adhoc.simulation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Custom joystick view for controlling camera panning.
 * Provides directional input (up, down, left, right) for moving the view.
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Joystick state
    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var hatRadius = 0f
    
    // Current joystick position
    private var hatX = 0f
    private var hatY = 0f
    
    // Paint objects
    private val basePaint = Paint().apply {
        color = Color.argb(100, 128, 128, 128)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val baseOutlinePaint = Paint().apply {
        color = Color.argb(150, 64, 64, 64)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    private val hatPaint = Paint().apply {
        color = Color.argb(200, 100, 100, 255)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val hatOutlinePaint = Paint().apply {
        color = Color.argb(255, 50, 50, 200)
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    // Listener for joystick movement
    private var listener: JoystickListener? = null
    
    interface JoystickListener {
        /**
         * Called when joystick is moved.
         * @param xPercent Horizontal position (-1.0 to 1.0, where -1 is left, 1 is right)
         * @param yPercent Vertical position (-1.0 to 1.0, where -1 is up, 1 is down)
         */
        fun onJoystickMoved(xPercent: Float, yPercent: Float)
    }
    
    fun setJoystickListener(listener: JoystickListener) {
        this.listener = listener
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f * 0.75f
        hatRadius = baseRadius * 0.4f
        
        // Initialize hat at center
        hatX = centerX
        hatY = centerY
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw base circle
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, baseOutlinePaint)
        
        // Draw directional lines for reference
        val lineLength = baseRadius * 0.7f
        val linePaint = Paint().apply {
            color = Color.argb(80, 64, 64, 64)
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        
        // Vertical line
        canvas.drawLine(centerX, centerY - lineLength, centerX, centerY + lineLength, linePaint)
        // Horizontal line
        canvas.drawLine(centerX - lineLength, centerY, centerX + lineLength, centerY, linePaint)
        
        // Draw hat (movable part)
        canvas.drawCircle(hatX, hatY, hatRadius, hatPaint)
        canvas.drawCircle(hatX, hatY, hatRadius, hatOutlinePaint)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                // Calculate distance from center
                val dx = event.x - centerX
                val dy = event.y - centerY
                val distance = sqrt(dx * dx + dy * dy)
                
                // Constrain to base radius
                if (distance < baseRadius) {
                    hatX = event.x
                    hatY = event.y
                } else {
                    // Clamp to edge of base circle
                    val angle = atan2(dy, dx)
                    hatX = centerX + baseRadius * kotlin.math.cos(angle)
                    hatY = centerY + baseRadius * kotlin.math.sin(angle)
                }
                
                // Notify listener
                val xPercent = (hatX - centerX) / baseRadius
                val yPercent = (hatY - centerY) / baseRadius
                listener?.onJoystickMoved(xPercent, yPercent)
                
                invalidate()
                return true
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Return to center
                hatX = centerX
                hatY = centerY
                listener?.onJoystickMoved(0f, 0f)
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
