package com.fourpeople.adhoc.location

import java.io.Serializable
import kotlin.math.*

/**
 * Represents location data for a device in the emergency network.
 */
data class LocationData(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val isHelpRequest: Boolean = false,
    val helpMessage: String? = null,
    val eventRadiusKm: Double = 100.0, // Default radius in kilometers
    val isForwarded: Boolean = false
) : Serializable {
    
    companion object {
        private const val serialVersionUID = 2L // Incremented due to schema changes
        private const val EARTH_RADIUS_KM = 6371.0 // Earth's radius in kilometers
        
        /**
         * Creates LocationData from JSON string.
         */
        fun fromJson(json: String): LocationData? {
            return try {
                // Updated regex to handle escaped quotes properly
                // Use (?:[^"\\]|\\.)* to match any character except unescaped quotes
                val deviceIdMatch = Regex(""""deviceId":"((?:[^"\\]|\\.)*?)"""").find(json)
                val latMatch = Regex(""""latitude":(-?[\d.]+)""").find(json)
                val lonMatch = Regex(""""longitude":(-?[\d.]+)""").find(json)
                val accMatch = Regex(""""accuracy":([\d.]+)""").find(json)
                val altMatch = Regex(""""altitude":(-?[\d.]+)""").find(json)
                val timeMatch = Regex(""""timestamp":(\d+)""").find(json)
                val helpMatch = Regex(""""isHelpRequest":(true|false)""").find(json)
                val msgMatch = Regex(""""helpMessage":"((?:[^"\\]|\\.)*?)"""").find(json)
                val radiusMatch = Regex(""""eventRadiusKm":([\d.]+)""").find(json)
                val forwardedMatch = Regex(""""isForwarded":(true|false)""").find(json)
                
                if (deviceIdMatch != null && latMatch != null && lonMatch != null && 
                    accMatch != null && altMatch != null && timeMatch != null && helpMatch != null) {
                    LocationData(
                        deviceId = unescapeJsonString(deviceIdMatch.groupValues[1]),
                        latitude = latMatch.groupValues[1].toDouble(),
                        longitude = lonMatch.groupValues[1].toDouble(),
                        accuracy = accMatch.groupValues[1].toFloat(),
                        altitude = altMatch.groupValues[1].toDouble(),
                        timestamp = timeMatch.groupValues[1].toLong(),
                        isHelpRequest = helpMatch.groupValues[1].toBoolean(),
                        helpMessage = msgMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }?.let { unescapeJsonString(it) },
                        eventRadiusKm = radiusMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: 100.0,
                        isForwarded = forwardedMatch?.groupValues?.get(1)?.toBoolean() ?: false
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Unescapes a JSON string value.
         * Uses a two-pass approach with a unique placeholder to avoid interference between replacements.
         */
        private fun unescapeJsonString(str: String): String {
            // Use an unlikely Unicode character sequence as placeholder
            val placeholder = "\uFFFF\uFFFE"
            return str
                .replace("\\\\", placeholder)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\b", "\b")
                .replace("\\f", "\u000C")
                // Replace placeholder with single backslash
                .replace(placeholder, "\\")
        }
    }
    
    /**
     * Converts location data to JSON string for transmission.
     * Properly escapes special characters to prevent JSON injection.
     */
    fun toJson(): String {
        val escapedDeviceId = escapeJsonString(deviceId)
        val escapedHelpMessage = escapeJsonString(helpMessage ?: "")
        return """{"deviceId":"$escapedDeviceId","latitude":$latitude,"longitude":$longitude,"accuracy":$accuracy,"altitude":$altitude,"timestamp":$timestamp,"isHelpRequest":$isHelpRequest,"helpMessage":"$escapedHelpMessage","eventRadiusKm":$eventRadiusKm,"isForwarded":$isForwarded}"""
    }
    
    private fun escapeJsonString(str: String): String {
        return str
            .replace("\\", "\\\\")  // Must be first to avoid double-escaping
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
    }
    
    /**
     * Checks if this location is still fresh (less than 5 minutes old).
     */
    fun isFresh(): Boolean {
        val fiveMinutesInMs = 5 * 60 * 1000
        return (System.currentTimeMillis() - timestamp) < fiveMinutesInMs
    }
    
    /**
     * Calculates the distance in kilometers to another location using the Haversine formula.
     */
    fun distanceToKm(other: LocationData): Double {
        return distanceToKm(other.latitude, other.longitude)
    }
    
    /**
     * Calculates the distance in kilometers to a given latitude/longitude using the Haversine formula.
     */
    fun distanceToKm(otherLat: Double, otherLon: Double): Double {
        val lat1Rad = latitude * PI / 180.0
        val lat2Rad = otherLat * PI / 180.0
        val dLat = (otherLat - latitude) * PI / 180.0
        val dLon = (otherLon - longitude) * PI / 180.0
        
        val a = sin(dLat / 2).pow(2) + 
                cos(lat1Rad) * cos(lat2Rad) * 
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_KM * c
    }
    
    /**
     * Checks if the given location is within the event radius.
     */
    fun isWithinRadius(otherLocation: LocationData): Boolean {
        return distanceToKm(otherLocation) <= eventRadiusKm
    }
    
    /**
     * Checks if the given coordinates are within the event radius.
     */
    fun isWithinRadius(otherLat: Double, otherLon: Double): Boolean {
        return distanceToKm(otherLat, otherLon) <= eventRadiusKm
    }
}
