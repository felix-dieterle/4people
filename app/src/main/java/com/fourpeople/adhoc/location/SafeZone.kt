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
        private const val VALIDITY_PERIOD_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    /**
     * Checks if this safe zone is still valid (less than 24 hours old).
     */
    fun isValid(): Boolean {
        return (System.currentTimeMillis() - timestamp) < VALIDITY_PERIOD_MS
    }
}
