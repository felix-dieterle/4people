package com.fourpeople.adhoc.protocol

import android.content.Context
import android.util.Log
import com.fourpeople.adhoc.mesh.MeshMessage
import com.fourpeople.adhoc.mesh.MeshRoutingManager
import org.json.JSONObject

/**
 * Handles SEPS protocol messages and integrates with mesh routing.
 * 
 * This manager:
 * - Detects SEPS-compliant messages from other apps
 * - Converts between SEPS and internal formats
 * - Enables interoperability with third-party emergency apps
 */
class SepsProtocolHandler(
    private val context: Context,
    private val deviceId: String,
    private val meshRoutingManager: MeshRoutingManager
) {
    
    private val TAG = "SepsProtocolHandler"
    
    // Callbacks for SEPS-specific messages
    private var onEmergencyAlertReceived: ((SepsMessage) -> Unit)? = null
    private var onHelpRequestReceived: ((SepsMessage) -> Unit)? = null
    private var onSafeZoneReceived: ((SepsMessage) -> Unit)? = null
    
    // Message deduplication cache (Level 2 compliance feature)
    // Stores recently seen message IDs to prevent duplicate processing
    private val recentMessageIds = mutableSetOf<String>()
    private val maxCacheSize = 1000
    private var lastCacheCleanup = System.currentTimeMillis()
    
    /**
     * Processes incoming data and determines if it's a SEPS message.
     * Returns true if message was handled as SEPS.
     */
    fun processIncomingData(data: String): Boolean {
        return try {
            // Try to parse as SEPS message
            val sepsMessage = SepsMessage.fromJsonString(data)
            
            // Verify it's a valid SEPS message
            if (sepsMessage.sepsVersion.startsWith("1.")) {
                Log.d(TAG, "Received SEPS v${sepsMessage.sepsVersion} message from ${sepsMessage.sender.appId}")
                handleSepsMessage(sepsMessage)
                true
            } else {
                Log.w(TAG, "Unsupported SEPS version: ${sepsMessage.sepsVersion}")
                false
            }
        } catch (e: Exception) {
            // Not a SEPS message, let other handlers process it
            Log.v(TAG, "Data is not a SEPS message: ${e.message}")
            false
        }
    }
    
    /**
     * Handles a received SEPS message with deduplication.
     */
    private fun handleSepsMessage(sepsMessage: SepsMessage) {
        // Check for duplicate message (Level 2 compliance)
        if (isDuplicate(sepsMessage.messageId)) {
            Log.v(TAG, "Ignoring duplicate message ${sepsMessage.messageId}")
            return
        }
        
        // Add to deduplication cache
        addToCache(sepsMessage.messageId)
        
        Log.d(TAG, "Handling ${sepsMessage.messageType} from ${sepsMessage.sender.appId} (${sepsMessage.sender.deviceId})")
        
        when (sepsMessage.messageType) {
            SepsMessageType.EMERGENCY_ALERT -> {
                handleEmergencyAlert(sepsMessage)
            }
            SepsMessageType.HELP_REQUEST -> {
                handleHelpRequest(sepsMessage)
            }
            SepsMessageType.LOCATION_UPDATE -> {
                handleLocationUpdate(sepsMessage)
            }
            SepsMessageType.SAFE_ZONE -> {
                handleSafeZone(sepsMessage)
            }
            SepsMessageType.HELLO -> {
                handleHello(sepsMessage)
            }
            SepsMessageType.ROUTE_REQUEST,
            SepsMessageType.ROUTE_REPLY -> {
                handleRoutingMessage(sepsMessage)
            }
            SepsMessageType.TEXT_MESSAGE -> {
                handleTextMessage(sepsMessage)
            }
        }
        
        // Forward message if needed
        if (sepsMessage.routing.canForward() && 
            sepsMessage.routing.destination != deviceId) {
            forwardSepsMessage(sepsMessage)
        }
    }
    
    /**
     * Checks if a message ID has been seen recently (deduplication).
     */
    private fun isDuplicate(messageId: String): Boolean {
        return recentMessageIds.contains(messageId)
    }
    
    /**
     * Adds a message ID to the deduplication cache.
     */
    private fun addToCache(messageId: String) {
        recentMessageIds.add(messageId)
        
        // Periodic cache cleanup to prevent unbounded growth
        val now = System.currentTimeMillis()
        if (recentMessageIds.size > maxCacheSize || 
            now - lastCacheCleanup > 300000) { // 5 minutes
            // Keep only most recent half by converting to list, taking last, and back to set
            val toKeep = recentMessageIds.toList().takeLast(maxCacheSize / 2)
            recentMessageIds.clear()
            recentMessageIds.addAll(toKeep)
            lastCacheCleanup = now
            Log.d(TAG, "Cleaned message cache, retained ${recentMessageIds.size} entries")
        }
    }
    
    /**
     * Sends a SEPS message through the mesh network.
     */
    fun sendSepsMessage(sepsMessage: SepsMessage) {
        val jsonString = sepsMessage.toJsonString()
        
        // Send through mesh routing using the public API
        meshRoutingManager.sendMessage(
            sepsMessage.routing.destination,
            jsonString
        )
        
        Log.d(TAG, "Sent SEPS ${sepsMessage.messageType} message")
    }
    
    /**
     * Forwards a SEPS message to next hop.
     */
    private fun forwardSepsMessage(sepsMessage: SepsMessage) {
        // Create forwarded version with updated routing
        val forwarded = sepsMessage.copy(
            routing = sepsMessage.routing.forward()
        )
        
        if (forwarded.routing.canForward()) {
            sendSepsMessage(forwarded)
            Log.d(TAG, "Forwarded SEPS message ${sepsMessage.messageId} (hop ${forwarded.routing.hopCount})")
        }
    }
    
    /**
     * Broadcasts emergency alert in SEPS format.
     */
    fun broadcastEmergencyAlert(
        severity: String = "EXTREME",
        category: String = "INFRASTRUCTURE_FAILURE",
        description: String,
        location: SepsLocation? = null
    ) {
        val alert = SepsCodec.createEmergencyAlert(
            deviceId = deviceId,
            severity = severity,
            category = category,
            description = description,
            location = location
        )
        sendSepsMessage(alert)
    }
    
    /**
     * Broadcasts help request in SEPS format.
     */
    fun broadcastHelpRequest(
        urgency: String,
        helpType: String,
        description: String,
        location: SepsLocation
    ) {
        val helpRequest = SepsCodec.createHelpRequest(
            deviceId = deviceId,
            urgency = urgency,
            helpType = helpType,
            description = description,
            location = location
        )
        sendSepsMessage(helpRequest)
    }
    
    // Message handlers
    
    private fun handleEmergencyAlert(sepsMessage: SepsMessage) {
        val payload = sepsMessage.payload
        val severity = payload.optString("severity", "UNKNOWN")
        val category = payload.optString("category", "OTHER")
        val description = payload.optString("description", "")
        
        Log.i(TAG, "Emergency Alert - Severity: $severity, Category: $category, From: ${sepsMessage.sender.appId}")
        
        onEmergencyAlertReceived?.invoke(sepsMessage)
    }
    
    private fun handleHelpRequest(sepsMessage: SepsMessage) {
        val payload = sepsMessage.payload
        val urgency = payload.optString("urgency", "UNKNOWN")
        val helpType = payload.optString("help_type", "OTHER")
        
        Log.i(TAG, "Help Request - Urgency: $urgency, Type: $helpType, From: ${sepsMessage.sender.deviceId}")
        
        onHelpRequestReceived?.invoke(sepsMessage)
    }
    
    private fun handleLocationUpdate(sepsMessage: SepsMessage) {
        // Extract and store location data
        val locationData = SepsCodec.extractLocationData(sepsMessage)
        locationData?.let {
            // Update location store - this will be integrated with LocationDataStore
            Log.d(TAG, "Location update from ${sepsMessage.sender.deviceId}: ${it.latitude}, ${it.longitude}")
        }
    }
    
    private fun handleSafeZone(sepsMessage: SepsMessage) {
        val safeZone = SepsCodec.extractSafeZone(sepsMessage)
        safeZone?.let {
            Log.i(TAG, "Safe zone received: ${it.name} at ${it.latitude}, ${it.longitude}")
            onSafeZoneReceived?.invoke(sepsMessage)
        }
    }
    
    private fun handleHello(sepsMessage: SepsMessage) {
        val payload = sepsMessage.payload
        val capabilities = try {
            val array = payload.getJSONArray("app_capabilities")
            (0 until array.length()).map { array.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        
        Log.d(TAG, "Hello from ${sepsMessage.sender.appId} with capabilities: $capabilities")
        
        // Update neighbor info - capabilities can be used for feature negotiation
    }
    
    private fun handleRoutingMessage(sepsMessage: SepsMessage) {
        // Convert to MeshMessage and let mesh routing handle it
        val meshMessage = SepsCodec.sepsToMeshMessage(sepsMessage)
        // This would be handled by mesh routing manager
        Log.d(TAG, "Routing message: ${sepsMessage.messageType}")
    }
    
    private fun handleTextMessage(sepsMessage: SepsMessage) {
        val text = sepsMessage.payload.optString("text", "")
        Log.d(TAG, "Text message from ${sepsMessage.sender.deviceId}: $text")
    }
    
    // Callback setters
    
    fun setOnEmergencyAlertReceived(callback: (SepsMessage) -> Unit) {
        onEmergencyAlertReceived = callback
    }
    
    fun setOnHelpRequestReceived(callback: (SepsMessage) -> Unit) {
        onHelpRequestReceived = callback
    }
    
    fun setOnSafeZoneReceived(callback: (SepsMessage) -> Unit) {
        onSafeZoneReceived = callback
    }
    
    companion object {
        /**
         * Detects if a device/network name follows SEPS naming convention.
         */
        fun isSepsDevice(name: String): Boolean {
            return name.startsWith("SEPS-")
        }
        
        /**
         * Checks if this app should also advertise SEPS compatibility.
         */
        fun shouldAdvertiseSeps(): Boolean {
            // For now, always advertise SEPS for maximum interoperability
            return true
        }
        
        /**
         * Gets the SEPS-compatible device name.
         */
        fun getSepsDeviceName(deviceId: String): String {
            return "SEPS-4people-$deviceId"
        }
    }
}
