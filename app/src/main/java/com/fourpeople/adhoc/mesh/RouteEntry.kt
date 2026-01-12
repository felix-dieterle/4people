package com.fourpeople.adhoc.mesh

/**
 * Represents a routing table entry for a specific destination.
 * 
 * Contains information about how to reach a destination node and the
 * quality/freshness of the route.
 */
data class RouteEntry(
    val destinationId: String,
    val nextHopId: String,
    val hopCount: Int,
    val sequenceNumber: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
) {
    companion object {
        const val ROUTE_TIMEOUT_MS = 30_000L // 30 seconds
    }
    
    /**
     * Checks if the route has expired.
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - timestamp > ROUTE_TIMEOUT_MS
    }
    
    /**
     * Checks if this route is better than another route.
     * A route is better if it has fewer hops or a higher sequence number.
     */
    fun isBetterThan(other: RouteEntry?): Boolean {
        if (other == null || !other.isValid || other.isExpired()) return true
        
        // Higher sequence number means fresher route
        if (sequenceNumber > other.sequenceNumber) return true
        if (sequenceNumber < other.sequenceNumber) return false
        
        // Same sequence number, prefer fewer hops
        return hopCount < other.hopCount
    }
}
