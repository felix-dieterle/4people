package com.fourpeople.adhoc.mesh

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages mesh network routing using a simplified AODV-like protocol.
 * 
 * Features:
 * - On-demand route discovery
 * - Multi-hop message forwarding
 * - Route maintenance and error handling
 * - Duplicate message detection
 * - Automatic neighbor discovery
 */
class MeshRoutingManager(private val context: Context, private val deviceId: String) {
    
    companion object {
        private const val TAG = "MeshRoutingManager"
        private const val ROUTE_REQUEST_TIMEOUT_MS = 5000L
        private const val HELLO_INTERVAL_MS = 10000L // 10 seconds
        private const val MAINTENANCE_INTERVAL_MS = 30000L // 30 seconds
        private const val MAX_ROUTE_REQUESTS = 3
    }
    
    private val routeTable = RouteTable()
    private val sequenceNumber = AtomicInteger(0)
    
    // Track seen messages to prevent duplicates and loops
    private val seenMessages = ConcurrentHashMap<String, Long>()
    
    // Track pending route requests
    private val pendingRouteRequests = ConcurrentHashMap<String, RouteRequestInfo>()
    
    // Listener for message forwarding
    private var messageForwarder: MessageForwarder? = null
    
    // Listener for received messages
    private var messageListener: MessageListener? = null
    
    init {
        Log.d(TAG, "MeshRoutingManager initialized for device: $deviceId")
    }
    
    /**
     * Sets the message forwarder for sending messages to neighbors.
     */
    fun setMessageForwarder(forwarder: MessageForwarder) {
        messageForwarder = forwarder
    }
    
    /**
     * Sets the listener for received messages.
     */
    fun setMessageListener(listener: MessageListener) {
        messageListener = listener
    }
    
    /**
     * Sends a message to a specific destination.
     * Initiates route discovery if route is unknown.
     */
    fun sendMessage(destinationId: String, payload: String): Boolean {
        Log.d(TAG, "Attempting to send message to $destinationId")
        
        val message = MeshMessage(
            sourceId = deviceId,
            destinationId = destinationId,
            payload = payload,
            messageType = MeshMessage.MessageType.DATA,
            sequenceNumber = sequenceNumber.incrementAndGet()
        )
        
        return sendMessage(message)
    }
    
    /**
     * Sends a broadcast message to all reachable nodes.
     */
    fun broadcastMessage(payload: String): Boolean {
        Log.d(TAG, "Broadcasting message")
        
        val message = MeshMessage(
            sourceId = deviceId,
            destinationId = MeshMessage.BROADCAST_DESTINATION,
            payload = payload,
            messageType = MeshMessage.MessageType.DATA,
            sequenceNumber = sequenceNumber.incrementAndGet()
        )
        
        return forwardToNeighbors(message)
    }
    
    /**
     * Processes a received message.
     * Handles routing, forwarding, and delivery to application layer.
     */
    fun receiveMessage(message: MeshMessage, senderId: String): Boolean {
        // Update neighbor information
        routeTable.addNeighbor(senderId)
        
        // Check for duplicate messages
        if (isDuplicate(message)) {
            Log.d(TAG, "Duplicate message ignored: ${message.messageId}")
            return false
        }
        
        // Mark message as seen
        markAsSeen(message)
        
        // Handle different message types
        return when (message.messageType) {
            MeshMessage.MessageType.DATA -> handleDataMessage(message, senderId)
            MeshMessage.MessageType.ROUTE_REQUEST -> handleRouteRequest(message, senderId)
            MeshMessage.MessageType.ROUTE_REPLY -> handleRouteReply(message, senderId)
            MeshMessage.MessageType.ROUTE_ERROR -> handleRouteError(message, senderId)
            MeshMessage.MessageType.HELLO -> handleHelloMessage(message, senderId)
        }
    }
    
    /**
     * Discovers neighbors by sending HELLO messages.
     */
    fun discoverNeighbors() {
        val helloMessage = MeshMessage(
            sourceId = deviceId,
            destinationId = MeshMessage.BROADCAST_DESTINATION,
            payload = "HELLO",
            messageType = MeshMessage.MessageType.HELLO,
            sequenceNumber = sequenceNumber.incrementAndGet(),
            ttl = 1 // Only one hop
        )
        
        forwardToNeighbors(helloMessage)
        Log.d(TAG, "HELLO message sent for neighbor discovery")
    }
    
    /**
     * Performs periodic maintenance on routing tables.
     */
    fun performMaintenance() {
        routeTable.performMaintenance()
        cleanupSeenMessages()
    }
    
    /**
     * Gets the current neighbor count.
     */
    fun getNeighborCount(): Int = routeTable.getNeighbors().size
    
    /**
     * Gets all known routes.
     */
    fun getKnownRoutes(): Map<String, RouteEntry> = routeTable.getAllRoutes()
    
    // Private helper methods
    
    private fun sendMessage(message: MeshMessage): Boolean {
        val route = routeTable.getRoute(message.destinationId)
        
        if (route != null) {
            // Route exists, forward message
            return forwardMessage(message, route.nextHopId)
        } else {
            // No route, initiate route discovery
            Log.d(TAG, "No route to ${message.destinationId}, initiating route discovery")
            initiateRouteDiscovery(message.destinationId)
            
            // Queue message for later (simplified - in production would have a proper queue)
            // For now, return false indicating message couldn't be sent immediately
            return false
        }
    }
    
    private fun handleDataMessage(message: MeshMessage, senderId: String): Boolean {
        // Add reverse route to source
        addReverseRoute(message.sourceId, senderId, message.hopCount + 1, message.sequenceNumber)
        
        if (message.destinationId == deviceId) {
            // Message is for this device
            Log.d(TAG, "Message delivered: ${message.messageId} from ${message.sourceId}")
            messageListener?.onMessageReceived(message)
            return true
        } else if (message.isBroadcast()) {
            // Broadcast message
            messageListener?.onMessageReceived(message)
            
            if (message.canForward()) {
                forwardToNeighbors(message.forward(), senderId)
            }
            return true
        } else {
            // Forward message to destination
            val route = routeTable.getRoute(message.destinationId)
            
            if (route != null && message.canForward()) {
                return forwardMessage(message.forward(), route.nextHopId)
            } else {
                Log.w(TAG, "Cannot forward message ${message.messageId}: no route or TTL expired")
                sendRouteError(message.sourceId, message.destinationId)
                return false
            }
        }
    }
    
    private fun handleRouteRequest(message: MeshMessage, senderId: String): Boolean {
        Log.d(TAG, "RREQ received from $senderId for ${message.destinationId}")
        
        // Add reverse route to source
        addReverseRoute(message.sourceId, senderId, message.hopCount + 1, message.sequenceNumber)
        
        if (message.destinationId == deviceId) {
            // This device is the destination, send route reply
            sendRouteReply(message.sourceId, senderId)
            return true
        } else {
            // Check if we have a route to destination
            val route = routeTable.getRoute(message.destinationId)
            
            if (route != null) {
                // Send route reply on behalf of destination
                sendRouteReply(message.sourceId, senderId)
                return true
            } else if (message.canForward()) {
                // Forward route request
                forwardToNeighbors(message.forward(), senderId)
                return true
            }
        }
        
        return false
    }
    
    private fun handleRouteReply(message: MeshMessage, senderId: String): Boolean {
        Log.d(TAG, "RREP received from $senderId for ${message.sourceId}")
        
        // Add route to destination (original source of RREQ)
        val route = RouteEntry(
            destinationId = message.sourceId,
            nextHopId = senderId,
            hopCount = message.hopCount + 1,
            sequenceNumber = message.sequenceNumber
        )
        routeTable.addRoute(route)
        
        if (message.destinationId == deviceId) {
            // Route reply reached the originator
            Log.d(TAG, "Route established to ${message.sourceId}")
            return true
        } else if (message.canForward()) {
            // Forward route reply towards originator
            val nextRoute = routeTable.getRoute(message.destinationId)
            if (nextRoute != null) {
                return forwardMessage(message.forward(), nextRoute.nextHopId)
            }
        }
        
        return false
    }
    
    private fun handleRouteError(message: MeshMessage, senderId: String): Boolean {
        Log.d(TAG, "RERR received from $senderId")
        
        // Invalidate routes through the failed node
        val failedDestination = message.payload
        routeTable.removeRoute(failedDestination)
        
        return true
    }
    
    private fun handleHelloMessage(message: MeshMessage, senderId: String): Boolean {
        // Hello message updates neighbor information (already done in receiveMessage)
        Log.d(TAG, "HELLO received from $senderId")
        return true
    }
    
    private fun initiateRouteDiscovery(destinationId: String) {
        val rreqMessage = MeshMessage(
            sourceId = deviceId,
            destinationId = destinationId,
            payload = "",
            messageType = MeshMessage.MessageType.ROUTE_REQUEST,
            sequenceNumber = sequenceNumber.incrementAndGet()
        )
        
        forwardToNeighbors(rreqMessage)
        
        // Track pending request
        pendingRouteRequests[destinationId] = RouteRequestInfo(
            timestamp = System.currentTimeMillis(),
            attempts = 1
        )
        
        Log.d(TAG, "Route discovery initiated for $destinationId")
    }
    
    private fun sendRouteReply(destinationId: String, nextHopId: String) {
        val rrepMessage = MeshMessage(
            sourceId = deviceId,
            destinationId = destinationId,
            payload = "",
            messageType = MeshMessage.MessageType.ROUTE_REPLY,
            sequenceNumber = sequenceNumber.incrementAndGet()
        )
        
        forwardMessage(rrepMessage, nextHopId)
        Log.d(TAG, "Route reply sent to $destinationId via $nextHopId")
    }
    
    private fun sendRouteError(sourceId: String, failedDestination: String) {
        val rerrMessage = MeshMessage(
            sourceId = deviceId,
            destinationId = sourceId,
            payload = failedDestination,
            messageType = MeshMessage.MessageType.ROUTE_ERROR,
            sequenceNumber = sequenceNumber.incrementAndGet()
        )
        
        val route = routeTable.getRoute(sourceId)
        if (route != null) {
            forwardMessage(rerrMessage, route.nextHopId)
            Log.d(TAG, "Route error sent to $sourceId")
        }
    }
    
    private fun forwardMessage(message: MeshMessage, nextHopId: String): Boolean {
        messageForwarder?.let {
            return it.forwardMessage(message, nextHopId)
        }
        
        Log.w(TAG, "No message forwarder configured")
        return false
    }
    
    private fun forwardToNeighbors(message: MeshMessage, excludeId: String? = null): Boolean {
        val neighbors = routeTable.getNeighbors().filter { it != excludeId }
        
        if (neighbors.isEmpty()) {
            Log.d(TAG, "No neighbors to forward message to")
            return false
        }
        
        messageForwarder?.let {
            neighbors.forEach { neighborId ->
                it.forwardMessage(message, neighborId)
            }
            return true
        }
        
        Log.w(TAG, "No message forwarder configured")
        return false
    }
    
    private fun addReverseRoute(sourceId: String, nextHopId: String, hopCount: Int, seqNum: Int) {
        val route = RouteEntry(
            destinationId = sourceId,
            nextHopId = nextHopId,
            hopCount = hopCount,
            sequenceNumber = seqNum
        )
        routeTable.addRoute(route)
    }
    
    private fun isDuplicate(message: MeshMessage): Boolean {
        val key = "${message.sourceId}-${message.messageId}"
        return seenMessages.containsKey(key)
    }
    
    private fun markAsSeen(message: MeshMessage) {
        val key = "${message.sourceId}-${message.messageId}"
        seenMessages[key] = System.currentTimeMillis()
    }
    
    private fun cleanupSeenMessages() {
        val now = System.currentTimeMillis()
        val timeout = 60000L // 60 seconds
        
        val expiredKeys = seenMessages.filter { (_, timestamp) ->
            now - timestamp > timeout
        }.keys
        
        expiredKeys.forEach { seenMessages.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            Log.d(TAG, "Cleaned up ${expiredKeys.size} expired message records")
        }
    }
    
    /**
     * Interface for forwarding messages to neighboring devices.
     */
    interface MessageForwarder {
        fun forwardMessage(message: MeshMessage, nextHopId: String): Boolean
    }
    
    /**
     * Interface for handling received messages at application layer.
     */
    interface MessageListener {
        fun onMessageReceived(message: MeshMessage)
    }
    
    private data class RouteRequestInfo(
        val timestamp: Long,
        val attempts: Int
    )
}
