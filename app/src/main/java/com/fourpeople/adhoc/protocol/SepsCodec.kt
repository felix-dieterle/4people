package com.fourpeople.adhoc.protocol

import android.os.Build
import com.fourpeople.adhoc.BuildConfig
import com.fourpeople.adhoc.location.LocationData
import com.fourpeople.adhoc.location.SafeZone
import com.fourpeople.adhoc.mesh.MeshMessage
import org.json.JSONObject
import java.util.*

/**
 * Codec for converting between internal message formats and SEPS protocol.
 * 
 * This enables interoperability with other emergency apps that implement
 * the Standard Emergency Protocol Specification (SEPS).
 */
object SepsCodec {
    
    private const val APP_ID = "com.fourpeople.adhoc"
    
    /**
     * Converts internal MeshMessage to SEPS format.
     */
    fun meshMessageToSeps(
        meshMessage: MeshMessage,
        deviceId: String
    ): SepsMessage {
        val messageType = when (meshMessage.messageType) {
            MeshMessage.MessageType.DATA -> SepsMessageType.TEXT_MESSAGE
            MeshMessage.MessageType.ROUTE_REQUEST -> SepsMessageType.ROUTE_REQUEST
            MeshMessage.MessageType.ROUTE_REPLY -> SepsMessageType.ROUTE_REPLY
            MeshMessage.MessageType.HELLO -> SepsMessageType.HELLO
            MeshMessage.MessageType.LOCATION_UPDATE -> SepsMessageType.LOCATION_UPDATE
            MeshMessage.MessageType.HELP_REQUEST -> SepsMessageType.HELP_REQUEST
            else -> SepsMessageType.TEXT_MESSAGE
        }
        
        val payload = when (messageType) {
            SepsMessageType.TEXT_MESSAGE -> createTextMessagePayload(meshMessage.payload)
            SepsMessageType.HELLO -> createHelloPayload()
            SepsMessageType.ROUTE_REQUEST -> createRouteRequestPayload(meshMessage)
            SepsMessageType.ROUTE_REPLY -> createRouteReplyPayload(meshMessage)
            else -> JSONObject().apply { put("data", meshMessage.payload) }
        }
        
        return SepsMessage(
            messageId = meshMessage.messageId,
            timestamp = meshMessage.timestamp,
            sender = SepsSender(
                appId = APP_ID,
                deviceId = deviceId,
                appVersion = BuildConfig.VERSION_NAME
            ),
            messageType = messageType,
            routing = SepsRouting(
                ttl = meshMessage.ttl,
                hopCount = meshMessage.hopCount,
                destination = meshMessage.destinationId,
                sequence = meshMessage.sequenceNumber
            ),
            payload = payload
        )
    }
    
    /**
     * Converts SEPS message to internal MeshMessage.
     */
    fun sepsToMeshMessage(sepsMessage: SepsMessage): MeshMessage {
        val messageType = when (sepsMessage.messageType) {
            SepsMessageType.TEXT_MESSAGE -> MeshMessage.MessageType.DATA
            SepsMessageType.ROUTE_REQUEST -> MeshMessage.MessageType.ROUTE_REQUEST
            SepsMessageType.ROUTE_REPLY -> MeshMessage.MessageType.ROUTE_REPLY
            SepsMessageType.HELLO -> MeshMessage.MessageType.HELLO
            SepsMessageType.LOCATION_UPDATE -> MeshMessage.MessageType.LOCATION_UPDATE
            SepsMessageType.HELP_REQUEST -> MeshMessage.MessageType.HELP_REQUEST
            SepsMessageType.EMERGENCY_ALERT -> MeshMessage.MessageType.DATA
            SepsMessageType.SAFE_ZONE -> MeshMessage.MessageType.DATA
        }
        
        val payload = extractPayload(sepsMessage)
        
        return MeshMessage(
            messageId = sepsMessage.messageId,
            sourceId = sepsMessage.sender.deviceId,
            destinationId = sepsMessage.routing.destination,
            payload = payload,
            messageType = messageType,
            timestamp = sepsMessage.timestamp,
            ttl = sepsMessage.routing.ttl,
            sequenceNumber = sepsMessage.routing.sequence,
            hopCount = sepsMessage.routing.hopCount
        )
    }
    
    /**
     * Creates a SEPS EMERGENCY_ALERT message.
     */
    fun createEmergencyAlert(
        deviceId: String,
        severity: String = "EXTREME",
        category: String = "INFRASTRUCTURE_FAILURE",
        description: String,
        location: SepsLocation? = null,
        contactCount: Int = 0
    ): SepsMessage {
        val payload = JSONObject().apply {
            put("severity", severity)
            put("category", category)
            put("description", description)
            location?.let { put("location", it.toJson()) }
            put("contacts", contactCount)
        }
        
        return SepsMessage(
            messageId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            sender = SepsSender(APP_ID, deviceId, BuildConfig.VERSION_NAME),
            messageType = SepsMessageType.EMERGENCY_ALERT,
            routing = SepsRouting(
                ttl = 10,
                hopCount = 0,
                destination = SepsRouting.BROADCAST,
                sequence = 0
            ),
            payload = payload
        )
    }
    
    /**
     * Creates a SEPS HELP_REQUEST message.
     */
    fun createHelpRequest(
        deviceId: String,
        urgency: String = "IMMEDIATE",
        helpType: String,
        description: String,
        location: SepsLocation,
        requesterStatus: String = "SAFE_BUT_NEED_HELP"
    ): SepsMessage {
        val payload = JSONObject().apply {
            put("urgency", urgency)
            put("help_type", helpType)
            put("description", description)
            put("location", location.toJson())
            put("requester_status", requesterStatus)
        }
        
        return SepsMessage(
            messageId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            sender = SepsSender(APP_ID, deviceId, BuildConfig.VERSION_NAME),
            messageType = SepsMessageType.HELP_REQUEST,
            routing = SepsRouting(
                ttl = 10,
                hopCount = 0,
                destination = SepsRouting.BROADCAST,
                sequence = 0
            ),
            payload = payload
        )
    }
    
    /**
     * Creates a SEPS LOCATION_UPDATE message from LocationData.
     */
    fun createLocationUpdate(
        deviceId: String,
        locationData: LocationData,
        status: String = "SAFE",
        batteryLevel: Int = 100,
        networkSize: Int = 1
    ): SepsMessage {
        val location = SepsLocation(
            latitude = locationData.latitude,
            longitude = locationData.longitude,
            accuracy = locationData.accuracy.toDouble(),
            altitude = locationData.altitude?.toDouble(),
            speed = locationData.speed?.toDouble(),
            bearing = locationData.bearing?.toDouble()
        )
        
        val payload = JSONObject().apply {
            put("location", location.toJson())
            put("status", status)
            put("battery_level", batteryLevel)
            put("network_size", networkSize)
        }
        
        return SepsMessage(
            messageId = UUID.randomUUID().toString(),
            timestamp = locationData.timestamp,
            sender = SepsSender(APP_ID, deviceId, BuildConfig.VERSION_NAME),
            messageType = SepsMessageType.LOCATION_UPDATE,
            routing = SepsRouting(
                ttl = 10,
                hopCount = 0,
                destination = SepsRouting.BROADCAST,
                sequence = 0
            ),
            payload = payload
        )
    }
    
    /**
     * Creates a SEPS SAFE_ZONE message from SafeZone.
     */
    fun createSafeZone(
        deviceId: String,
        safeZone: SafeZone
    ): SepsMessage {
        val location = SepsLocation(
            latitude = safeZone.latitude,
            longitude = safeZone.longitude,
            accuracy = 10.0
        )
        
        val payload = JSONObject().apply {
            put("zone_id", safeZone.id)
            put("zone_type", safeZone.type)
            put("location", location.toJson())
            put("name", safeZone.name)
            safeZone.description?.let { put("description", it) }
            put("amenities", JSONObject()) // Empty for now
        }
        
        return SepsMessage(
            messageId = UUID.randomUUID().toString(),
            timestamp = safeZone.timestamp,
            sender = SepsSender(APP_ID, deviceId, BuildConfig.VERSION_NAME),
            messageType = SepsMessageType.SAFE_ZONE,
            routing = SepsRouting(
                ttl = 10,
                hopCount = 0,
                destination = SepsRouting.BROADCAST,
                sequence = 0
            ),
            payload = payload
        )
    }
    
    /**
     * Extracts LocationData from SEPS LOCATION_UPDATE message.
     */
    fun extractLocationData(sepsMessage: SepsMessage): LocationData? {
        if (sepsMessage.messageType != SepsMessageType.LOCATION_UPDATE) return null
        
        val payload = sepsMessage.payload
        val locationJson = payload.optJSONObject("location") ?: return null
        val location = SepsLocation.fromJson(locationJson)
        
        return LocationData(
            deviceId = sepsMessage.sender.deviceId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy?.toFloat() ?: 0f,
            timestamp = sepsMessage.timestamp,
            altitude = location.altitude?.toFloat(),
            speed = location.speed?.toFloat(),
            bearing = location.bearing?.toFloat(),
            needsHelp = payload.optString("status") != "SAFE"
        )
    }
    
    /**
     * Extracts SafeZone from SEPS SAFE_ZONE message.
     */
    fun extractSafeZone(sepsMessage: SepsMessage): SafeZone? {
        if (sepsMessage.messageType != SepsMessageType.SAFE_ZONE) return null
        
        val payload = sepsMessage.payload
        val locationJson = payload.optJSONObject("location") ?: return null
        val location = SepsLocation.fromJson(locationJson)
        
        return SafeZone(
            id = payload.getString("zone_id"),
            name = payload.getString("name"),
            latitude = location.latitude,
            longitude = location.longitude,
            type = payload.getString("zone_type"),
            description = payload.optString("description").takeIf { it.isNotEmpty() },
            timestamp = sepsMessage.timestamp
        )
    }
    
    // Helper functions
    
    private fun createTextMessagePayload(text: String): JSONObject {
        return JSONObject().apply {
            put("text", text)
            put("priority", "NORMAL")
            put("requires_ack", false)
        }
    }
    
    private fun createHelloPayload(): JSONObject {
        return JSONObject().apply {
            put("app_capabilities", org.json.JSONArray().apply {
                put("SEPS_V1")
                put("LOCATION_SHARING")
                put("MESH_ROUTING")
                put("SAFE_ZONES")
            })
            put("supported_transports", org.json.JSONArray().apply {
                put("BLUETOOTH")
                put("WIFI")
                put("WIFI_DIRECT")
            })
            put("battery_level", 100)
            put("active_participants", 1)
        }
    }
    
    private fun createRouteRequestPayload(meshMessage: MeshMessage): JSONObject {
        return JSONObject().apply {
            put("destination_id", meshMessage.destinationId)
            put("originator_sequence", meshMessage.sequenceNumber)
            put("destination_sequence", 0)
            put("hop_count", meshMessage.hopCount)
        }
    }
    
    private fun createRouteReplyPayload(meshMessage: MeshMessage): JSONObject {
        return JSONObject().apply {
            put("destination_id", meshMessage.destinationId)
            put("destination_sequence", meshMessage.sequenceNumber)
            put("hop_count", meshMessage.hopCount)
            put("lifetime", 30000)
        }
    }
    
    private fun extractPayload(sepsMessage: SepsMessage): String {
        return when (sepsMessage.messageType) {
            SepsMessageType.TEXT_MESSAGE -> {
                sepsMessage.payload.optString("text", "")
            }
            SepsMessageType.EMERGENCY_ALERT -> {
                sepsMessage.payload.optString("description", "Emergency alert")
            }
            else -> sepsMessage.payload.toString()
        }
    }
}
