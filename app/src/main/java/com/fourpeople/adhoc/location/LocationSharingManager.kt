package com.fourpeople.adhoc.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages location sharing in the emergency network.
 * 
 * Features:
 * - Automatic GPS location capture
 * - Broadcasting location to all network participants
 * - Tracking locations of all participants
 * - Help request with location
 */
class LocationSharingManager(
    private val context: Context,
    private val deviceId: String
) {
    
    companion object {
        private const val TAG = "LocationSharingManager"
        private const val LOCATION_UPDATE_INTERVAL_MS = 30000L // 30 seconds
        private const val MIN_DISTANCE_METERS = 10f // 10 meters
    }
    
    private var locationManager: LocationManager? = null
    private var currentLocation: LocationData? = null
    private var isActive = false
    
    // Store locations of all participants
    private val participantLocations = ConcurrentHashMap<String, LocationData>()
    
    // Listener for location updates to broadcast
    private var locationUpdateListener: LocationUpdateListener? = null
    
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }
        
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Deprecated but required for compatibility
        }
        
        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Location provider enabled: $provider")
        }
        
        override fun onProviderDisabled(provider: String) {
            Log.w(TAG, "Location provider disabled: $provider")
        }
    }
    
    /**
     * Interface for location update callbacks.
     */
    interface LocationUpdateListener {
        fun onLocationUpdate(locationData: LocationData)
    }
    
    /**
     * Sets the listener for location updates.
     */
    fun setLocationUpdateListener(listener: LocationUpdateListener) {
        locationUpdateListener = listener
    }
    
    /**
     * Starts location tracking and broadcasting.
     */
    fun startLocationSharing(): Boolean {
        if (isActive) {
            Log.d(TAG, "Location sharing already active")
            return true
        }
        
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return false
        }
        
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        try {
            // Try GPS first for better accuracy
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LOCATION_UPDATE_INTERVAL_MS,
                    MIN_DISTANCE_METERS,
                    locationListener
                )
                Log.d(TAG, "GPS location updates requested")
            }
            
            // Also use network provider as fallback
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    LOCATION_UPDATE_INTERVAL_MS,
                    MIN_DISTANCE_METERS,
                    locationListener
                )
                Log.d(TAG, "Network location updates requested")
            }
            
            // Get last known location immediately
            val lastLocation = getLastKnownLocation()
            lastLocation?.let { handleLocationUpdate(it) }
            
            isActive = true
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception requesting location updates", e)
            return false
        }
    }
    
    /**
     * Stops location tracking.
     */
    fun stopLocationSharing() {
        if (!isActive) return
        
        try {
            locationManager?.removeUpdates(locationListener)
            isActive = false
            Log.d(TAG, "Location sharing stopped")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception removing location updates", e)
        }
    }
    
    /**
     * Gets the current device location.
     */
    fun getCurrentLocation(): LocationData? = currentLocation
    
    /**
     * Checks if location sharing is currently active.
     */
    fun isLocationSharingActive(): Boolean = isActive
    
    /**
     * Updates location of a participant from received broadcast.
     */
    fun updateParticipantLocation(locationData: LocationData) {
        participantLocations[locationData.deviceId] = locationData
        Log.d(TAG, "Updated location for device ${locationData.deviceId}")
        
        // Clean up stale locations (older than 10 minutes)
        cleanupStaleLocations()
    }
    
    /**
     * Gets all participant locations.
     */
    fun getAllParticipantLocations(): List<LocationData> {
        return participantLocations.values.toList()
    }
    
    /**
     * Sends a help request with current location.
     */
    fun sendHelpRequest(message: String?): LocationData? {
        val location = currentLocation ?: return null
        
        return location.copy(
            isHelpRequest = true,
            helpMessage = message
        )
    }
    
    /**
     * Gets locations of participants requesting help.
     */
    fun getHelpRequests(): List<LocationData> {
        return participantLocations.values.filter { it.isHelpRequest }
    }
    
    private fun handleLocationUpdate(location: Location) {
        val locationData = LocationData(
            deviceId = deviceId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            altitude = location.altitude,
            timestamp = System.currentTimeMillis()
        )
        
        currentLocation = locationData
        Log.d(TAG, "Location updated: lat=${location.latitude}, lon=${location.longitude}")
        
        // Notify listener to broadcast the location
        locationUpdateListener?.onLocationUpdate(locationData)
    }
    
    private fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        
        return try {
            val gpsLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            
            // Return the most accurate location
            when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.accuracy < networkLocation.accuracy) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last known location", e)
            null
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun cleanupStaleLocations() {
        val tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000)
        participantLocations.entries.removeAll { it.value.timestamp < tenMinutesAgo }
    }
}
