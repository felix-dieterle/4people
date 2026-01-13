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
    
    private var people = listOf<SimulationPerson>()
    private var wifiNetworks = listOf<SimulationWiFi>()
    private var event: SimulationEvent? = null
    
    // Coordinate bounds
    private var minLat = 0.0
    private var maxLat = 0.0
    private var minLon = 0.0
    private var maxLon = 0.0
    
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
        
        // Calculate bounds
        if (people.isNotEmpty()) {
            minLat = people.minOf { it.latitude }
            maxLat = people.maxOf { it.latitude }
            minLon = people.minOf { it.longitude }
            maxLon = people.maxOf { it.longitude }
            
            // Add some padding
            val latPadding = (maxLat - minLat) * 0.1
            val lonPadding = (maxLon - minLon) * 0.1
            minLat -= latPadding
            maxLat += latPadding
            minLon -= lonPadding
            maxLon += lonPadding
        }
        
        invalidate()
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
