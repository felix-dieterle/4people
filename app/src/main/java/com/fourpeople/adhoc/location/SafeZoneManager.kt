package com.fourpeople.adhoc.location

import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton store for safe zones accessible across the app.
 * Safe zones are collection points where people can gather during emergencies.
 */
object SafeZoneManager {
    
    private val safeZones = ConcurrentHashMap<String, SafeZone>()
    private val listeners = mutableListOf<SafeZoneUpdateListener>()
    
    /**
     * Interface for listening to safe zone updates.
     */
    interface SafeZoneUpdateListener {
        fun onSafeZoneAdded(safeZone: SafeZone)
        fun onSafeZoneRemoved(safeZoneId: String)
    }
    
    /**
     * Adds a safe zone update listener.
     */
    fun addListener(listener: SafeZoneUpdateListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }
    
    /**
     * Removes a safe zone update listener.
     */
    fun removeListener(listener: SafeZoneUpdateListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }
    
    /**
     * Adds or updates a safe zone.
     */
    fun addSafeZone(safeZone: SafeZone) {
        safeZones[safeZone.id] = safeZone
        
        // Notify all listeners
        synchronized(listeners) {
            listeners.forEach { it.onSafeZoneAdded(safeZone) }
        }
        
        // Clean up invalid safe zones
        cleanupInvalidSafeZones()
    }
    
    /**
     * Removes a safe zone.
     */
    fun removeSafeZone(safeZoneId: String) {
        if (safeZones.remove(safeZoneId) != null) {
            // Notify listeners
            synchronized(listeners) {
                listeners.forEach { it.onSafeZoneRemoved(safeZoneId) }
            }
        }
    }
    
    /**
     * Gets all current safe zones.
     */
    fun getAllSafeZones(): List<SafeZone> {
        return safeZones.values.toList()
    }
    
    /**
     * Gets a specific safe zone.
     */
    fun getSafeZone(safeZoneId: String): SafeZone? {
        return safeZones[safeZoneId]
    }
    
    /**
     * Gets the nearest safe zone to a given location.
     */
    fun getNearestSafeZone(latitude: Double, longitude: Double): SafeZone? {
        return safeZones.values.minByOrNull { safeZone ->
            calculateDistance(latitude, longitude, safeZone.latitude, safeZone.longitude)
        }
    }
    
    /**
     * Clears all safe zones.
     */
    fun clear() {
        val removedIds = safeZones.keys.toList()
        safeZones.clear()
        
        // Notify listeners
        synchronized(listeners) {
            removedIds.forEach { safeZoneId ->
                listeners.forEach { listener ->
                    listener.onSafeZoneRemoved(safeZoneId)
                }
            }
        }
    }
    
    /**
     * Removes invalid safe zones (older than 24 hours).
     */
    private fun cleanupInvalidSafeZones() {
        val invalidIds = safeZones.entries
            .filter { !it.value.isValid() }
            .map { it.key }
        
        invalidIds.forEach { safeZoneId ->
            safeZones.remove(safeZoneId)
            
            // Notify listeners
            synchronized(listeners) {
                listeners.forEach { it.onSafeZoneRemoved(safeZoneId) }
            }
        }
    }
    
    /**
     * Calculates distance between two points in meters using Haversine formula.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
}
