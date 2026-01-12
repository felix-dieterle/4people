package com.fourpeople.adhoc.location

import java.io.Serializable

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
    val helpMessage: String? = null
) : Serializable {
    
    companion object {
        private const val serialVersionUID = 1L
        
        /**
         * Creates LocationData from JSON string.
         */
        fun fromJson(json: String): LocationData? {
            return try {
                val deviceIdMatch = Regex(""""deviceId":"([^"]+)"""").find(json)
                val latMatch = Regex(""""latitude":(-?[\d.]+)""").find(json)
                val lonMatch = Regex(""""longitude":(-?[\d.]+)""").find(json)
                val accMatch = Regex(""""accuracy":([\d.]+)""").find(json)
                val altMatch = Regex(""""altitude":(-?[\d.]+)""").find(json)
                val timeMatch = Regex(""""timestamp":(\d+)""").find(json)
                val helpMatch = Regex(""""isHelpRequest":(true|false)""").find(json)
                val msgMatch = Regex(""""helpMessage":"([^"]*)"""").find(json)
                
                if (deviceIdMatch != null && latMatch != null && lonMatch != null && 
                    accMatch != null && altMatch != null && timeMatch != null && helpMatch != null) {
                    LocationData(
                        deviceId = deviceIdMatch.groupValues[1],
                        latitude = latMatch.groupValues[1].toDouble(),
                        longitude = lonMatch.groupValues[1].toDouble(),
                        accuracy = accMatch.groupValues[1].toFloat(),
                        altitude = altMatch.groupValues[1].toDouble(),
                        timestamp = timeMatch.groupValues[1].toLong(),
                        isHelpRequest = helpMatch.groupValues[1].toBoolean(),
                        helpMessage = msgMatch?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Converts location data to JSON string for transmission.
     * Properly escapes special characters to prevent JSON injection.
     */
    fun toJson(): String {
        val escapedDeviceId = deviceId.replace("\\", "\\\\").replace("\"", "\\\"")
        val escapedHelpMessage = (helpMessage ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"deviceId":"$escapedDeviceId","latitude":$latitude,"longitude":$longitude,"accuracy":$accuracy,"altitude":$altitude,"timestamp":$timestamp,"isHelpRequest":$isHelpRequest,"helpMessage":"$escapedHelpMessage"}"""
    }
    
    /**
     * Checks if this location is still fresh (less than 5 minutes old).
     */
    fun isFresh(): Boolean {
        val fiveMinutesInMs = 5 * 60 * 1000
        return (System.currentTimeMillis() - timestamp) < fiveMinutesInMs
    }
}
