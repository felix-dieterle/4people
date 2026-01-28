package com.fourpeople.adhoc.protocol

import org.json.JSONArray
import org.json.JSONObject

/**
 * Standard Emergency Protocol Specification (SEPS) v1.0 message.
 * 
 * This class represents a SEPS-compliant message that can be exchanged with
 * other emergency apps implementing the same protocol, enabling interoperability.
 */
data class SepsMessage(
    val sepsVersion: String = "1.0",
    val messageId: String,
    val timestamp: Long,
    val sender: SepsSender,
    val messageType: SepsMessageType,
    val routing: SepsRouting,
    val payload: JSONObject,
    val signature: String? = null
) {
    
    /**
     * Converts this SEPS message to JSON format for transmission.
     */
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("seps_version", sepsVersion)
            put("message_id", messageId)
            put("timestamp", timestamp)
            put("sender", sender.toJson())
            put("message_type", messageType.name)
            put("routing", routing.toJson())
            put("payload", payload)
            signature?.let { put("signature", it) }
        }
    }
    
    /**
     * Converts this SEPS message to JSON string.
     */
    fun toJsonString(): String = toJson().toString()
    
    companion object {
        /**
         * Parses a SEPS message from JSON object.
         */
        fun fromJson(json: JSONObject): SepsMessage {
            return SepsMessage(
                sepsVersion = json.getString("seps_version"),
                messageId = json.getString("message_id"),
                timestamp = json.getLong("timestamp"),
                sender = SepsSender.fromJson(json.getJSONObject("sender")),
                messageType = SepsMessageType.valueOf(json.getString("message_type")),
                routing = SepsRouting.fromJson(json.getJSONObject("routing")),
                payload = json.getJSONObject("payload"),
                signature = json.optString("signature").takeIf { it.isNotEmpty() }
            )
        }
        
        /**
         * Parses a SEPS message from JSON string.
         */
        fun fromJsonString(jsonString: String): SepsMessage {
            return fromJson(JSONObject(jsonString))
        }
    }
}

/**
 * Sender identification in SEPS message.
 */
data class SepsSender(
    val appId: String,
    val deviceId: String,
    val appVersion: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("app_id", appId)
            put("device_id", deviceId)
            put("app_version", appVersion)
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): SepsSender {
            return SepsSender(
                appId = json.getString("app_id"),
                deviceId = json.getString("device_id"),
                appVersion = json.getString("app_version")
            )
        }
    }
}

/**
 * Routing metadata in SEPS message.
 */
data class SepsRouting(
    val ttl: Int,
    val hopCount: Int,
    val destination: String,
    val sequence: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("ttl", ttl)
            put("hop_count", hopCount)
            put("destination", destination)
            put("sequence", sequence)
        }
    }
    
    /**
     * Creates a new routing with incremented hop count and decremented TTL.
     */
    fun forward(): SepsRouting {
        return copy(
            ttl = ttl - 1,
            hopCount = hopCount + 1
        )
    }
    
    fun canForward(): Boolean = ttl > 0
    
    companion object {
        const val BROADCAST = "BROADCAST"
        
        fun fromJson(json: JSONObject): SepsRouting {
            return SepsRouting(
                ttl = json.getInt("ttl"),
                hopCount = json.getInt("hop_count"),
                destination = json.getString("destination"),
                sequence = json.getInt("sequence")
            )
        }
    }
}

/**
 * SEPS message types as defined in the specification.
 */
enum class SepsMessageType {
    EMERGENCY_ALERT,
    HELP_REQUEST,
    LOCATION_UPDATE,
    SAFE_ZONE,
    ROUTE_REQUEST,
    ROUTE_REPLY,
    HELLO,
    TEXT_MESSAGE
}

/**
 * Location data following SEPS specification.
 */
data class SepsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val altitude: Double? = null,
    val speed: Double? = null,
    val bearing: Double? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            accuracy?.let { put("accuracy", it) }
            altitude?.let { put("altitude", it) }
            speed?.let { put("speed", it) }
            bearing?.let { put("bearing", it) }
        }
    }
    
    companion object {
        fun fromJson(json: JSONObject): SepsLocation {
            return SepsLocation(
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                accuracy = json.optDouble("accuracy").takeIf { !it.isNaN() },
                altitude = json.optDouble("altitude").takeIf { !it.isNaN() },
                speed = json.optDouble("speed").takeIf { !it.isNaN() },
                bearing = json.optDouble("bearing").takeIf { !it.isNaN() }
            )
        }
    }
}
