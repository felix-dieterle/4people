package com.fourpeople.adhoc.location

import java.io.Serializable

/**
 * Represents a safe zone (collection point) in the emergency network.
 * Safe zones are predefined or user-marked locations where people can gather.
 */
data class SafeZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val description: String? = null,
    val capacity: Int = 0, // Approximate capacity, 0 = unlimited
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    
    companion object {
        private const val serialVersionUID = 1L
    }
    
    /**
     * Checks if this safe zone is still valid (less than 24 hours old).
     */
    fun isValid(): Boolean {
        val twentyFourHoursInMs = 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - timestamp) < twentyFourHoursInMs
    }
}
