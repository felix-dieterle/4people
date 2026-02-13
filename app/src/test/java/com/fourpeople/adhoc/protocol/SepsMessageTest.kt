package com.fourpeople.adhoc.protocol

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for SEPS protocol message encoding and decoding.
 */
@RunWith(RobolectricTestRunner::class)
class SepsMessageTest {
    
    @Test
    fun testMessageToJsonConversion() {
        val sender = SepsSender(
            appId = "com.fourpeople.adhoc",
            deviceId = "test-device-123",
            appVersion = "1.0.0"
        )
        
        val routing = SepsRouting(
            ttl = 10,
            hopCount = 0,
            destination = "BROADCAST",
            sequence = 1
        )
        
        val payload = JSONObject().apply {
            put("severity", "EXTREME")
            put("category", "INFRASTRUCTURE_FAILURE")
            put("description", "Test emergency")
        }
        
        val message = SepsMessage(
            messageId = "test-msg-id",
            timestamp = 1706432123456L,
            sender = sender,
            messageType = SepsMessageType.EMERGENCY_ALERT,
            routing = routing,
            payload = payload
        )
        
        val json = message.toJson()
        
        assertEquals("1.0", json.getString("seps_version"))
        assertEquals("test-msg-id", json.getString("message_id"))
        assertEquals(1706432123456L, json.getLong("timestamp"))
        assertEquals("EMERGENCY_ALERT", json.getString("message_type"))
    }
    
    @Test
    fun testMessageFromJsonConversion() {
        val jsonString = """
        {
            "seps_version": "1.0",
            "message_id": "test-msg-id",
            "timestamp": 1706432123456,
            "sender": {
                "app_id": "com.fourpeople.adhoc",
                "device_id": "test-device-123",
                "app_version": "1.0.0"
            },
            "message_type": "EMERGENCY_ALERT",
            "routing": {
                "ttl": 10,
                "hop_count": 0,
                "destination": "BROADCAST",
                "sequence": 1
            },
            "payload": {
                "severity": "EXTREME",
                "category": "INFRASTRUCTURE_FAILURE",
                "description": "Test emergency"
            }
        }
        """.trimIndent()
        
        val message = SepsMessage.fromJsonString(jsonString)
        
        assertEquals("1.0", message.sepsVersion)
        assertEquals("test-msg-id", message.messageId)
        assertEquals(1706432123456L, message.timestamp)
        assertEquals("com.fourpeople.adhoc", message.sender.appId)
        assertEquals("test-device-123", message.sender.deviceId)
        assertEquals(SepsMessageType.EMERGENCY_ALERT, message.messageType)
        assertEquals(10, message.routing.ttl)
        assertEquals(0, message.routing.hopCount)
        assertEquals("BROADCAST", message.routing.destination)
    }
    
    @Test
    fun testMessageRoundTrip() {
        val original = SepsMessage(
            messageId = "round-trip-test",
            timestamp = System.currentTimeMillis(),
            sender = SepsSender("com.test.app", "device-1", "1.0"),
            messageType = SepsMessageType.HELP_REQUEST,
            routing = SepsRouting(8, 2, "device-2", 5),
            payload = JSONObject().apply {
                put("urgency", "IMMEDIATE")
                put("help_type", "MEDICAL")
            }
        )
        
        val json = original.toJsonString()
        val parsed = SepsMessage.fromJsonString(json)
        
        assertEquals(original.messageId, parsed.messageId)
        assertEquals(original.timestamp, parsed.timestamp)
        assertEquals(original.sender.appId, parsed.sender.appId)
        assertEquals(original.messageType, parsed.messageType)
        assertEquals(original.routing.ttl, parsed.routing.ttl)
        assertEquals(original.routing.hopCount, parsed.routing.hopCount)
    }
    
    @Test
    fun testRoutingForward() {
        val routing = SepsRouting(
            ttl = 10,
            hopCount = 0,
            destination = "BROADCAST",
            sequence = 1
        )
        
        val forwarded = routing.forward()
        
        assertEquals(9, forwarded.ttl)
        assertEquals(1, forwarded.hopCount)
        assertEquals("BROADCAST", forwarded.destination)
        assertEquals(1, forwarded.sequence)
    }
    
    @Test
    fun testRoutingCanForward() {
        val routing1 = SepsRouting(ttl = 5, hopCount = 0, destination = "BROADCAST", sequence = 1)
        assertTrue(routing1.canForward())
        
        val routing2 = SepsRouting(ttl = 0, hopCount = 10, destination = "BROADCAST", sequence = 1)
        assertFalse(routing2.canForward())
        
        val routing3 = SepsRouting(ttl = -1, hopCount = 10, destination = "BROADCAST", sequence = 1)
        assertFalse(routing3.canForward())
    }
    
    @Test
    fun testLocationConversion() {
        val location = SepsLocation(
            latitude = 52.5200,
            longitude = 13.4050,
            accuracy = 10.0,
            altitude = 45.0,
            speed = 1.5,
            bearing = 180.0
        )
        
        val json = location.toJson()
        val parsed = SepsLocation.fromJson(json)
        
        assertEquals(52.5200, parsed.latitude, 0.0001)
        assertEquals(13.4050, parsed.longitude, 0.0001)
        assertEquals(10.0, parsed.accuracy!!, 0.01)
        assertEquals(45.0, parsed.altitude!!, 0.01)
        assertEquals(1.5, parsed.speed!!, 0.01)
        assertEquals(180.0, parsed.bearing!!, 0.01)
    }
    
    @Test
    fun testLocationWithOptionalFields() {
        val location = SepsLocation(
            latitude = 52.5200,
            longitude = 13.4050
        )
        
        val json = location.toJson()
        val parsed = SepsLocation.fromJson(json)
        
        assertEquals(52.5200, parsed.latitude, 0.0001)
        assertEquals(13.4050, parsed.longitude, 0.0001)
        assertNull(parsed.accuracy)
        assertNull(parsed.altitude)
        assertNull(parsed.speed)
        assertNull(parsed.bearing)
    }
    
    @Test
    fun testSenderConversion() {
        val sender = SepsSender(
            appId = "com.example.emergency",
            deviceId = "device-abc-123",
            appVersion = "2.0.5"
        )
        
        val json = sender.toJson()
        val parsed = SepsSender.fromJson(json)
        
        assertEquals("com.example.emergency", parsed.appId)
        assertEquals("device-abc-123", parsed.deviceId)
        assertEquals("2.0.5", parsed.appVersion)
    }
    
    @Test
    fun testAllMessageTypes() {
        val types = SepsMessageType.values()
        
        assertTrue(types.contains(SepsMessageType.EMERGENCY_ALERT))
        assertTrue(types.contains(SepsMessageType.HELP_REQUEST))
        assertTrue(types.contains(SepsMessageType.LOCATION_UPDATE))
        assertTrue(types.contains(SepsMessageType.SAFE_ZONE))
        assertTrue(types.contains(SepsMessageType.ROUTE_REQUEST))
        assertTrue(types.contains(SepsMessageType.ROUTE_REPLY))
        assertTrue(types.contains(SepsMessageType.HELLO))
        assertTrue(types.contains(SepsMessageType.TEXT_MESSAGE))
    }
    
    @Test
    fun testBroadcastConstant() {
        assertEquals("BROADCAST", SepsRouting.BROADCAST)
    }
    
    @Test
    fun testDefaultSepsVersion() {
        val message = SepsMessage(
            messageId = "test",
            timestamp = System.currentTimeMillis(),
            sender = SepsSender("com.test", "dev1", "1.0"),
            messageType = SepsMessageType.HELLO,
            routing = SepsRouting(10, 0, "BROADCAST", 1),
            payload = JSONObject()
        )
        
        assertEquals("1.0", message.sepsVersion)
    }
    
    @Test
    fun testOptionalSignature() {
        val withSignature = SepsMessage(
            messageId = "sig-test",
            timestamp = System.currentTimeMillis(),
            sender = SepsSender("com.test", "dev1", "1.0"),
            messageType = SepsMessageType.HELLO,
            routing = SepsRouting(10, 0, "BROADCAST", 1),
            payload = JSONObject(),
            signature = "base64-encoded-signature"
        )
        
        assertEquals("base64-encoded-signature", withSignature.signature)
        
        val withoutSignature = SepsMessage(
            messageId = "no-sig-test",
            timestamp = System.currentTimeMillis(),
            sender = SepsSender("com.test", "dev1", "1.0"),
            messageType = SepsMessageType.HELLO,
            routing = SepsRouting(10, 0, "BROADCAST", 1),
            payload = JSONObject()
        )
        
        assertNull(withoutSignature.signature)
    }
}
