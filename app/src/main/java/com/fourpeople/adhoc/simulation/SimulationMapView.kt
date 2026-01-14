package com.fourpeople.adhoc.simulation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom view that visualizes the simulation on a map.
 * 
 * Shows:
 * - People (green = has app, gray = no app, yellow = informed)
 * - WiFi networks (blue circles)
 * - Event location (red circle)
 * - 100m detection radius (red outline)
 */
class SimulationMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    companion object {
        // Maximum pan range as a fraction of visible area (0.3 = 30%)
        private const val MAX_PAN_RANGE = 0.3
    }
    
    private var people = listOf<SimulationPerson>()
    private var wifiNetworks = listOf<SimulationWiFi>()
    private var event: SimulationEvent? = null
    
    // Coordinate bounds (original data bounds)
    private var dataMinLat = 0.0
    private var dataMaxLat = 0.0
    private var dataMinLon = 0.0
    private var dataMaxLon = 0.0
    
    // View bounds (affected by pan/zoom)
    private var minLat = 0.0
    private var maxLat = 0.0
    private var minLon = 0.0
    private var maxLon = 0.0
    
    // Pan offset (percentage of view range)
    private var panOffsetX = 0.0
    private var panOffsetY = 0.0
    
    // Paint objects
    private val personWithAppPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }
    
    private val personNoAppPaint = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
    }
    
    private val personInformedPaint = Paint().apply {
        color = Color.rgb(255, 215, 0) // Gold
        style = Paint.Style.FILL
    }
    
    private val personMovingPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val wifiPaint = Paint().apply {
        color = Color.BLUE
        alpha = 80
        style = Paint.Style.FILL
    }
    
    private val wifiOutlinePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    private val eventPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    
    private val eventRadiusPaint = Paint().apply {
        color = Color.RED
        alpha = 50
        style = Paint.Style.FILL
    }
    
    private val eventOutlinePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    
    /**
     * Update the simulation data to display.
     */
    fun updateSimulation(
        people: List<SimulationPerson>,
        wifiNetworks: List<SimulationWiFi>,
        event: SimulationEvent?
    ) {
        this.people = people
        this.wifiNetworks = wifiNetworks
        this.event = event
        
        // Calculate data bounds
        if (people.isNotEmpty()) {
            dataMinLat = people.minOf { it.latitude }
            dataMaxLat = people.maxOf { it.latitude }
            dataMinLon = people.minOf { it.longitude }
            dataMaxLon = people.maxOf { it.longitude }
            
            // Add some padding
            val latPadding = (dataMaxLat - dataMinLat) * 0.1
            val lonPadding = (dataMaxLon - dataMinLon) * 0.1
            dataMinLat -= latPadding
            dataMaxLat += latPadding
            dataMinLon -= lonPadding
            dataMaxLon += lonPadding
            
            // Update view bounds with current pan
            updateViewBounds()
        }
        
        invalidate()
    }
    
    /**
     * Set pan offset for camera movement.
     * @param xPercent Horizontal pan (-1.0 to 1.0, where -1 is left, 1 is right)
     * @param yPercent Vertical pan (-1.0 to 1.0, where -1 is up, 1 is down)
     */
    fun setPanOffset(xPercent: Float, yPercent: Float) {
        panOffsetX = xPercent.toDouble()
        panOffsetY = yPercent.toDouble()
        updateViewBounds()
        invalidate()
    }
    
    /**
     * Update view bounds based on data bounds and pan offset.
     */
    private fun updateViewBounds() {
        val latRange = dataMaxLat - dataMinLat
        val lonRange = dataMaxLon - dataMinLon
        
        // Apply pan offset (inverted for natural camera movement)
        val latOffset = latRange * panOffsetY * MAX_PAN_RANGE
        val lonOffset = lonRange * panOffsetX * MAX_PAN_RANGE
        
        minLat = dataMinLat - latOffset
        maxLat = dataMaxLat - latOffset
        minLon = dataMinLon + lonOffset
        maxLon = dataMaxLon + lonOffset
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (people.isEmpty()) return
        
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        // Draw WiFi networks
        for (wifi in wifiNetworks) {
            val x = latLonToX(wifi.longitude, viewWidth)
            val y = latLonToY(wifi.latitude, viewHeight)
            val radius = metersToPixels(wifi.range, viewWidth, viewHeight)
            
            canvas.drawCircle(x, y, radius, wifiPaint)
            canvas.drawCircle(x, y, radius, wifiOutlinePaint)
        }
        
        // Draw event and detection radius
        event?.let { evt ->
            val x = latLonToX(evt.longitude, viewWidth)
            val y = latLonToY(evt.latitude, viewHeight)
            val radius = metersToPixels(evt.detectionRadius, viewWidth, viewHeight)
            
            // Detection radius
            canvas.drawCircle(x, y, radius, eventRadiusPaint)
            canvas.drawCircle(x, y, radius, eventOutlinePaint)
            
            // Event marker
            canvas.drawCircle(x, y, 15f, eventPaint)
        }
        
        // Draw people
        for (person in people) {
            val x = latLonToX(person.longitude, viewWidth)
            val y = latLonToY(person.latitude, viewHeight)
            
            // Choose color based on state
            val paint = when {
                person.hasReceivedEvent -> personInformedPaint
                person.hasApp -> personWithAppPaint
                else -> personNoAppPaint
            }
            
            canvas.drawCircle(x, y, 8f, paint)
            
            // Draw outline for moving people
            if (person.isMoving) {
                canvas.drawCircle(x, y, 8f, personMovingPaint)
            }
        }
    }
    
    /**
     * Convert longitude to screen X coordinate.
     */
    private fun latLonToX(lon: Double, viewWidth: Float): Float {
        if (maxLon == minLon) return viewWidth / 2
        return ((lon - minLon) / (maxLon - minLon) * viewWidth).toFloat()
    }
    
    /**
     * Convert latitude to screen Y coordinate.
     */
    private fun latLonToY(lat: Double, viewHeight: Float): Float {
        if (maxLat == minLat) return viewHeight / 2
        // Invert Y axis (higher latitude = lower Y)
        return ((maxLat - lat) / (maxLat - minLat) * viewHeight).toFloat()
    }
    
    /**
     * Convert meters to pixels for drawing circles.
     */
    private fun metersToPixels(meters: Double, viewWidth: Float, viewHeight: Float): Float {
        // Approximate conversion using lat/lon ranges
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon
        
        // Use average of lat and lon conversion
        val metersPerLatDegree = 111320.0 // approximately
        val metersPerLonDegree = metersPerLatDegree * Math.cos(Math.toRadians((minLat + maxLat) / 2))
        
        val pixelsPerMeterLat = viewHeight / (latRange * metersPerLatDegree)
        val pixelsPerMeterLon = viewWidth / (lonRange * metersPerLonDegree)
        
        return (meters * (pixelsPerMeterLat + pixelsPerMeterLon) / 2).toFloat()
    }
}
