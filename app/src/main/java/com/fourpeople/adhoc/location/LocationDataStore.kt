package com.fourpeople.adhoc.location

import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton store for location data accessible across the app.
 * This allows activities to access location data collected by the service.
 */
object LocationDataStore {
    
    private const val LOCATION_STALENESS_MS = 10 * 60 * 1000L // 10 minutes
    
    private val participantLocations = ConcurrentHashMap<String, LocationData>()
    private val listeners = mutableListOf<LocationUpdateListener>()
    
    /**
     * Interface for listening to location updates.
     */
    interface LocationUpdateListener {
        fun onLocationUpdate(locationData: LocationData)
        fun onLocationRemoved(deviceId: String)
    }
    
    /**
     * Adds a location update listener.
     */
    fun addListener(listener: LocationUpdateListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }
    
    /**
     * Removes a location update listener.
     */
    fun removeListener(listener: LocationUpdateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    /**
     * Updates a participant's location.
     */
    fun updateLocation(locationData: LocationData) {
        participantLocations[locationData.deviceId] = locationData
        
        // Notify all listeners
        synchronized(listeners) {
            listeners.forEach { it.onLocationUpdate(locationData) }
        }
        
        // Clean up stale locations
        cleanupStaleLocations()
    }
    
    /**
     * Gets all current participant locations.
     */
    fun getAllLocations(): List<LocationData> {
        return participantLocations.values.toList()
    }
    
    /**
     * Gets a specific participant's location.
     */
    fun getLocation(deviceId: String): LocationData? {
        return participantLocations[deviceId]
    }
    
    /**
     * Gets all help requests.
     */
    fun getHelpRequests(): List<LocationData> {
        return participantLocations.values.filter { it.isHelpRequest }
    }
    
    /**
     * Clears all location data.
     */
    fun clear() {
        val removedIds = participantLocations.keys.toList()
        participantLocations.clear()
        
        // Notify listeners
        synchronized(listeners) {
            removedIds.forEach { deviceId ->
                listeners.forEach { listener ->
                    listener.onLocationRemoved(deviceId)
                }
            }
        }
    }
    
    /**
     * Removes stale locations (older than 10 minutes).
     */
    private fun cleanupStaleLocations() {
        val staleThreshold = System.currentTimeMillis() - LOCATION_STALENESS_MS
        val staleIds = participantLocations.entries
            .filter { it.value.timestamp < staleThreshold }
            .map { it.key }
        
        staleIds.forEach { deviceId ->
            participantLocations.remove(deviceId)
            
            // Notify listeners
            synchronized(listeners) {
                listeners.forEach { it.onLocationRemoved(deviceId) }
            }
        }
    }
}
