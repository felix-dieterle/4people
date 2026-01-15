package com.fourpeople.adhoc

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fourpeople.adhoc.databinding.ActivityOfflineMapBinding
import com.fourpeople.adhoc.location.LocationData
import com.fourpeople.adhoc.location.LocationDataStore
import com.fourpeople.adhoc.location.LocationSharingManager
import com.fourpeople.adhoc.location.SafeZone
import com.fourpeople.adhoc.location.SafeZoneManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.UUID

/**
 * Activity to display an offline OSM-based map with emergency network features.
 * 
 * Features:
 * - Display all network participants on the map
 * - Show help requests with special markers
 * - Mark and display safe zones (collection points)
 * - Show routes to nearest safe zones
 */
class OfflineMapActivity : AppCompatActivity(), 
    LocationDataStore.LocationUpdateListener,
    SafeZoneManager.SafeZoneUpdateListener {

    private lateinit var binding: ActivityOfflineMapBinding
    private val safeZones = mutableListOf<SafeZone>()
    private val participantMarkers = mutableMapOf<String, Marker>()
    private val safeZoneMarkers = mutableMapOf<String, Marker>()
    private var currentLocationMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var myDeviceId: String = ""
    
    companion object {
        private const val DEFAULT_ZOOM = 15.0
        private const val DEFAULT_LAT = 51.5074 // London as default
        private const val DEFAULT_LON = -0.1278
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get device ID (consistent with service)
        myDeviceId = android.provider.Settings.Secure.getString(
            contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        ).substring(0, 8)
        
        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            applicationContext,
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
        
        binding = ActivityOfflineMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Emergency Network Map"

        setupMap()
        setupListeners()
        
        // Register for location updates
        LocationDataStore.addListener(this)
        SafeZoneManager.addListener(this)
        
        // Load existing locations
        loadExistingLocations()
        
        // Load existing safe zones
        loadExistingSafeZones()
        
        // Load sample data for demonstration
        loadSampleSafeZones()
    }
    
    private fun loadExistingLocations() {
        val locations = LocationDataStore.getAllLocations()
        locations.forEach { addParticipantMarker(it) }
    }
    
    private fun loadExistingSafeZones() {
        val zones = SafeZoneManager.getAllSafeZones()
        zones.forEach { safeZone ->
            safeZones.add(safeZone)
            addSafeZoneMarker(safeZone)
        }
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK) // Standard OSM tiles
            setMultiTouchControls(true)
            
            // Set default view
            controller.setZoom(DEFAULT_ZOOM)
            controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LON))
        }
    }

    private fun setupListeners() {
        // Center on my location button
        binding.centerButton.setOnClickListener {
            centerOnCurrentLocation()
        }
        
        // Add safe zone FAB
        binding.addSafeZoneFab.setOnClickListener {
            showAddSafeZoneDialog()
        }
    }

    /**
     * Centers the map on the current user location.
     */
    private fun centerOnCurrentLocation() {
        // Get our own location from LocationDataStore
        val myLocation = LocationDataStore.getLocation(myDeviceId)
        
        if (myLocation != null) {
            val geoPoint = GeoPoint(myLocation.latitude, myLocation.longitude)
            binding.mapView.controller.animateTo(geoPoint)
            Toast.makeText(this, "Centered on your location", Toast.LENGTH_SHORT).show()
        } else {
            // Fallback: try to get any recent location
            val anyLocation = LocationDataStore.getAllLocations()
                .maxByOrNull { it.timestamp }
            
            if (anyLocation != null) {
                val geoPoint = GeoPoint(anyLocation.latitude, anyLocation.longitude)
                binding.mapView.controller.animateTo(geoPoint)
                Toast.makeText(this, "Centered on most recent location", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No location data available yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Shows dialog to add a new safe zone at the current map center.
     */
    private fun showAddSafeZoneDialog() {
        val mapCenter = binding.mapView.mapCenter as GeoPoint
        val input = android.widget.EditText(this)
        input.hint = "Safe zone name"
        
        AlertDialog.Builder(this)
            .setTitle("Add Safe Zone")
            .setMessage("Add safe zone at current map position?")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text.toString().ifEmpty { "Safe Zone" }
                addSafeZone(name, mapCenter.latitude, mapCenter.longitude)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Adds a safe zone to the map.
     */
    private fun addSafeZone(name: String, latitude: Double, longitude: Double) {
        val safeZone = SafeZone(
            id = UUID.randomUUID().toString(),
            name = name,
            latitude = latitude,
            longitude = longitude,
            description = "Emergency collection point"
        )
        
        safeZones.add(safeZone)
        
        // Add to global safe zone manager
        SafeZoneManager.addSafeZone(safeZone)
        
        Toast.makeText(this, "Safe zone added: $name", Toast.LENGTH_SHORT).show()
    }

    /**
     * Adds a marker for a safe zone on the map.
     */
    private fun addSafeZoneMarker(safeZone: SafeZone) {
        val marker = Marker(binding.mapView).apply {
            position = GeoPoint(safeZone.latitude, safeZone.longitude)
            title = "🏠 ${safeZone.name}"
            snippet = safeZone.description ?: "Safe zone"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            
            // Set click listener to show route
            setOnMarkerClickListener { clickedMarker, _ ->
                showRouteToSafeZone(safeZone)
                clickedMarker.showInfoWindow()
                true
            }
        }
        
        binding.mapView.overlays.add(marker)
        safeZoneMarkers[safeZone.id] = marker
        binding.mapView.invalidate()
    }

    /**
     * Shows a route to the specified safe zone.
     */
    private fun showRouteToSafeZone(safeZone: SafeZone) {
        // Remove existing route
        routePolyline?.let { binding.mapView.overlays.remove(it) }
        
        // Get current location (prefer our own, fallback to most recent)
        val myLocation = LocationDataStore.getLocation(myDeviceId)
            ?: LocationDataStore.getAllLocations().maxByOrNull { it.timestamp }
        
        if (myLocation == null) {
            Toast.makeText(this, "Current location not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        val currentPos = GeoPoint(myLocation.latitude, myLocation.longitude)
        val targetPos = GeoPoint(safeZone.latitude, safeZone.longitude)
        
        // Create a simple straight line route (in real app, would use routing algorithm)
        routePolyline = Polyline().apply {
            addPoint(currentPos)
            addPoint(targetPos)
            outlinePaint.color = Color.BLUE
            outlinePaint.strokeWidth = 5f
        }
        
        binding.mapView.overlays.add(routePolyline)
        binding.mapView.invalidate()
        
        Toast.makeText(
            this, 
            "Route to ${safeZone.name} (${calculateDistance(currentPos, targetPos)} m)", 
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Adds a participant marker to the map.
     */
    private fun addParticipantMarker(locationData: LocationData) {
        // Remove existing marker for this participant
        participantMarkers[locationData.deviceId]?.let { 
            binding.mapView.overlays.remove(it) 
        }
        
        val marker = Marker(binding.mapView).apply {
            position = GeoPoint(locationData.latitude, locationData.longitude)
            
            if (locationData.isHelpRequest) {
                title = "🆘 ${locationData.deviceId}"
                snippet = locationData.helpMessage ?: "Help needed!"
                icon = ContextCompat.getDrawable(
                    this@OfflineMapActivity, 
                    android.R.drawable.ic_dialog_alert
                )
            } else {
                title = "🟢 ${locationData.deviceId}"
                snippet = "Network participant"
            }
            
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        
        binding.mapView.overlays.add(marker)
        participantMarkers[locationData.deviceId] = marker
        binding.mapView.invalidate()
    }

    /**
     * Updates all participant locations on the map.
     */
    private fun updateParticipants(locations: List<LocationData>) {
        locations.forEach { addParticipantMarker(it) }
    }

    /**
     * Loads sample safe zones for demonstration.
     */
    private fun loadSampleSafeZones() {
        // Add a few sample safe zones around the default location
        addSafeZone("Community Center", DEFAULT_LAT + 0.01, DEFAULT_LON)
        addSafeZone("Town Hall", DEFAULT_LAT - 0.01, DEFAULT_LON + 0.01)
        addSafeZone("Hospital", DEFAULT_LAT, DEFAULT_LON - 0.01)
    }

    /**
     * Calculates approximate distance between two points in meters.
     */
    private fun calculateDistance(start: GeoPoint, end: GeoPoint): Int {
        val earthRadius = 6371000.0 // meters
        
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val deltaLat = Math.toRadians(end.latitude - start.latitude)
        val deltaLon = Math.toRadians(end.longitude - start.longitude)
        
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return (earthRadius * c).toInt()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Unregister from location updates
        LocationDataStore.removeListener(this)
        SafeZoneManager.removeListener(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    // LocationDataStore.LocationUpdateListener implementation
    override fun onLocationUpdate(locationData: LocationData) {
        runOnUiThread {
            addParticipantMarker(locationData)
        }
    }
    
    override fun onLocationRemoved(deviceId: String) {
        runOnUiThread {
            participantMarkers[deviceId]?.let { marker ->
                binding.mapView.overlays.remove(marker)
                participantMarkers.remove(deviceId)
                binding.mapView.invalidate()
            }
        }
    }
    
    // SafeZoneManager.SafeZoneUpdateListener implementation
    override fun onSafeZoneAdded(safeZone: SafeZone) {
        runOnUiThread {
            if (!safeZones.any { it.id == safeZone.id }) {
                safeZones.add(safeZone)
                addSafeZoneMarker(safeZone)
            }
        }
    }
    
    override fun onSafeZoneRemoved(safeZoneId: String) {
        runOnUiThread {
            safeZoneMarkers[safeZoneId]?.let { marker ->
                binding.mapView.overlays.remove(marker)
                safeZoneMarkers.remove(safeZoneId)
                safeZones.removeAll { it.id == safeZoneId }
                binding.mapView.invalidate()
            }
        }
    }
}
