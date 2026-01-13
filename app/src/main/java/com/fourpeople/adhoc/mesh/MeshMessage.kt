package com.fourpeople.adhoc.mesh

import java.io.Serializable
import java.util.*

/**
 * Represents a message that can be routed through the mesh network.
 * 
 * Messages are forwarded from device to device until they reach their destination
 * or exceed the maximum hop count (TTL).
 */
data class MeshMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val sourceId: String,
    val destinationId: String,
    val payload: String,
    val messageType: MessageType = MessageType.DATA,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = DEFAULT_TTL,
    val sequenceNumber: Int = 0,
    val hopCount: Int = 0,
    val hasInsecureHop: Boolean = false
) : Serializable {
    
    companion object {
        const val DEFAULT_TTL = 10
        const val BROADCAST_DESTINATION = "BROADCAST"
        private const val serialVersionUID = 2L // Incremented due to schema changes
    }
    
    /**
     * Creates a copy of the message with incremented hop count and decremented TTL.
     * Used when forwarding the message to the next hop.
     * 
     * @param isSecureHop Whether the current hop uses a secure connection
     */
    fun forward(isSecureHop: Boolean = true): MeshMessage {
        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1,
            hasInsecureHop = hasInsecureHop || !isSecureHop
        )
    }
    
    /**
     * Checks if the message should still be forwarded.
     */
    fun canForward(): Boolean = ttl > 0
    
    /**
     * Checks if this is a broadcast message.
     */
    fun isBroadcast(): Boolean = destinationId == BROADCAST_DESTINATION
    
    /**
     * Checks if all hops in the message path are secure.
     */
    fun isFullySecure(): Boolean = !hasInsecureHop
    
    enum class MessageType {
        DATA,              // Regular data message
        ROUTE_REQUEST,     // Route discovery request (RREQ)
        ROUTE_REPLY,       // Route discovery reply (RREP)
        ROUTE_ERROR,       // Route error notification (RERR)
        HELLO,             // Periodic hello message for neighbor discovery
        LOCATION_UPDATE,   // Location sharing broadcast
        HELP_REQUEST       // Emergency help request with location
    }
}
