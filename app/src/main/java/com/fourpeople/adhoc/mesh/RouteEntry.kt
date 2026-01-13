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
    val isValid: Boolean = true,
    val connectionSecurity: ConnectionSecurity = ConnectionSecurity.UNKNOWN
) {
    companion object {
        const val ROUTE_TIMEOUT_MS = 30_000L // 30 seconds
    }
    
    /**
     * Represents the security level of a connection in the route.
     * 
     * SECURE: Encrypted connection (e.g., Bluetooth with pairing, WPA2/WPA3 WiFi)
     * INSECURE: Unencrypted connection (e.g., open WiFi, unpaired Bluetooth)
     * UNKNOWN: Security level not determined or mixed security in path
     * 
     * Note: This is a rudimentary implementation. Future improvements could include:
     * - Multiple security levels (e.g., LOW, MEDIUM, HIGH)
     * - Per-hop security tracking for the entire path
     * - Certificate validation for end-to-end encryption
     * - Dynamic security assessment based on connection type
     */
    enum class ConnectionSecurity {
        SECURE,    // Encrypted/authenticated connection
        INSECURE,  // Unencrypted or unauthenticated connection
        UNKNOWN    // Security status not determined
    }
    
    /**
     * Checks if the route has expired.
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - timestamp > ROUTE_TIMEOUT_MS
    }
    
    /**
     * Checks if the route uses a secure connection.
     */
    fun isSecure(): Boolean {
        return connectionSecurity == ConnectionSecurity.SECURE
    }
    
    /**
     * Checks if this route is better than another route.
     * A route is better if it has fewer hops or a higher sequence number.
     * If routes are otherwise equal, prefer secure connections.
     */
    fun isBetterThan(other: RouteEntry?): Boolean {
        if (other == null || !other.isValid || other.isExpired()) return true
        
        // Higher sequence number means fresher route
        if (sequenceNumber > other.sequenceNumber) return true
        if (sequenceNumber < other.sequenceNumber) return false
        
        // Same sequence number, prefer fewer hops
        if (hopCount < other.hopCount) return true
        if (hopCount > other.hopCount) return false
        
        // Same hops, prefer secure connections
        if (connectionSecurity == ConnectionSecurity.SECURE && 
            other.connectionSecurity != ConnectionSecurity.SECURE) return true
        
        return false
    }
}
