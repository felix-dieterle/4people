package com.fourpeople.adhoc.protocol

import com.fourpeople.adhoc.location.LocationData
import com.fourpeople.adhoc.location.SafeZone
import com.fourpeople.adhoc.mesh.MeshMessage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for SEPS protocol codec that converts between internal and SEPS formats.
 */
@RunWith(RobolectricTestRunner::class)
class SepsCodecTest {
    
    @Test
    fun testMeshMessageToSepsConversion() {
        val meshMessage = MeshMessage(
            sourceId = "device1",
            destinationId = "BROADCAST",
            payload = "Hello world",
            messageType = MeshMessage.MessageType.DATA,
            ttl = 10,
            sequenceNumber = 5,
            hopCount = 2
        )
        
        val sepsMessage = SepsCodec.meshMessageToSeps(meshMessage, "device1")
        
        assertEquals(meshMessage.messageId, sepsMessage.messageId)
        assertEquals(meshMessage.timestamp, sepsMessage.timestamp)
        assertEquals("device1", sepsMessage.sender.deviceId)
        assertEquals("com.fourpeople.adhoc", sepsMessage.sender.appId)
        assertEquals(SepsMessageType.TEXT_MESSAGE, sepsMessage.messageType)
        assertEquals(10, sepsMessage.routing.ttl)
        assertEquals(2, sepsMessage.routing.hopCount)
        assertEquals("BROADCAST", sepsMessage.routing.destination)
    }
    
    @Test
    fun testSepsToMeshMessageConversion() {
        val sepsMessage = SepsMessage(
            messageId = "test-id",
            timestamp = System.currentTimeMillis(),
            sender = SepsSender("com.other.app", "device2", "1.0"),
            messageType = SepsMessageType.HELP_REQUEST,
            routing = SepsRouting(8, 3, "BROADCAST", 7),
            payload = org.json.JSONObject().apply {
                put("urgency", "IMMEDIATE")
            }
        )
        
        val meshMessage = SepsCodec.sepsToMeshMessage(sepsMessage)
        
        assertEquals(sepsMessage.messageId, meshMessage.messageId)
        assertEquals(sepsMessage.timestamp, meshMessage.timestamp)
        assertEquals("device2", meshMessage.sourceId)
        assertEquals("BROADCAST", meshMessage.destinationId)
        assertEquals(MeshMessage.MessageType.HELP_REQUEST, meshMessage.messageType)
        assertEquals(8, meshMessage.ttl)
        assertEquals(3, meshMessage.hopCount)
        assertEquals(7, meshMessage.sequenceNumber)
    }
    
    @Test
    fun testCreateEmergencyAlert() {
        val location = SepsLocation(52.5200, 13.4050, 10.0)
        val alert = SepsCodec.createEmergencyAlert(
            deviceId = "device1",
            severity = "EXTREME",
            category = "NATURAL_DISASTER",
            description = "Earthquake detected",
            location = location,
            contactCount = 5
        )
        
        assertEquals(SepsMessageType.EMERGENCY_ALERT, alert.messageType)
        assertEquals("device1", alert.sender.deviceId)
        assertEquals("EXTREME", alert.payload.getString("severity"))
        assertEquals("NATURAL_DISASTER", alert.payload.getString("category"))
        assertEquals("Earthquake detected", alert.payload.getString("description"))
        assertEquals(5, alert.payload.getInt("contacts"))
        
        val payloadLocation = SepsLocation.fromJson(alert.payload.getJSONObject("location"))
        assertEquals(52.5200, payloadLocation.latitude, 0.0001)
        assertEquals(13.4050, payloadLocation.longitude, 0.0001)
    }
    
    @Test
    fun testCreateHelpRequest() {
        val location = SepsLocation(52.5210, 13.4060, 5.0)
        val helpRequest = SepsCodec.createHelpRequest(
            deviceId = "device2",
            urgency = "IMMEDIATE",
            helpType = "MEDICAL",
            description = "Person injured",
            location = location,
            requesterStatus = "INJURED"
        )
        
        assertEquals(SepsMessageType.HELP_REQUEST, helpRequest.messageType)
        assertEquals("device2", helpRequest.sender.deviceId)
        assertEquals("IMMEDIATE", helpRequest.payload.getString("urgency"))
        assertEquals("MEDICAL", helpRequest.payload.getString("help_type"))
        assertEquals("Person injured", helpRequest.payload.getString("description"))
        assertEquals("INJURED", helpRequest.payload.getString("requester_status"))
    }
    
    @Test
    fun testCreateLocationUpdate() {
        val locationData = LocationData(
            deviceId = "device3",
            latitude = 52.5200,
            longitude = 13.4050,
            accuracy = 10.0f,
            altitude = 45.0,
            timestamp = System.currentTimeMillis(),
            isHelpRequest = false
        )
        
        val update = SepsCodec.createLocationUpdate(
            deviceId = "device3",
            locationData = locationData,
            status = "SAFE",
            batteryLevel = 75,
            networkSize = 8
        )
        
        assertEquals(SepsMessageType.LOCATION_UPDATE, update.messageType)
        assertEquals("device3", update.sender.deviceId)
        assertEquals("SAFE", update.payload.getString("status"))
        assertEquals(75, update.payload.getInt("battery_level"))
        assertEquals(8, update.payload.getInt("network_size"))
        
        val location = SepsLocation.fromJson(update.payload.getJSONObject("location"))
        assertEquals(52.5200, location.latitude, 0.0001)
        assertEquals(13.4050, location.longitude, 0.0001)
        assertEquals(10.0, location.accuracy!!, 0.01)
    }
    
    @Test
    fun testExtractLocationData() {
        val locationJson = org.json.JSONObject().apply {
            put("latitude", 52.5200)
            put("longitude", 13.4050)
            put("accuracy", 10.0)
            put("altitude", 45.0)
            put("speed", 1.5)
            put("bearing", 180.0)
        }
        
        val payload = org.json.JSONObject().apply {
            put("location", locationJson)
            put("status", "SAFE")
            put("battery_level", 80)
            put("network_size", 5)
        }
        
        val sepsMessage = SepsMessage(
            messageId = "loc-test",
            timestamp = 1706432123456L,
            sender = SepsSender("com.other.app", "device4", "1.0"),
            messageType = SepsMessageType.LOCATION_UPDATE,
            routing = SepsRouting(10, 0, "BROADCAST", 1),
            payload = payload
        )
        
        val locationData = SepsCodec.extractLocationData(sepsMessage)
        
        assertNotNull(locationData)
        assertEquals("device4", locationData!!.deviceId)
        assertEquals(52.5200, locationData.latitude, 0.0001)
        assertEquals(13.4050, locationData.longitude, 0.0001)
        assertEquals(10.0f, locationData.accuracy, 0.01f)
        assertEquals(45.0, locationData.altitude, 0.01)
        assertFalse(locationData.isHelpRequest)
    }
    
    @Test
    fun testExtractLocationDataReturnsNullForWrongType() {
        val sepsMessage = SepsMessage(
            messageId = "test",
            timestamp = System.currentTimeMillis(),
            sender = SepsSender("com.test", "dev1", "1.0"),
            messageType = SepsMessageType.HELLO,
            routing = SepsRouting(10, 0, "BROADCAST", 1),
            payload = org.json.JSONObject()
        )
        
        val locationData = SepsCodec.extractLocationData(sepsMessage)
        assertNull(locationData)
    }
    
    @Test
    fun testCreateSafeZone() {
        val safeZone = SafeZone(
            id = "zone-1",
            name = "City Hall Shelter",
            latitude = 52.5200,
            longitude = 13.4050,
            description = "Emergency shelter with medical facilities",
            timestamp = System.currentTimeMillis()
        )
        
        val sepsMessage = SepsCodec.createSafeZone("device5", safeZone)
        
        assertEquals(SepsMessageType.SAFE_ZONE, sepsMessage.messageType)
        assertEquals("zone-1", sepsMessage.payload.getString("zone_id"))
        assertEquals("City Hall Shelter", sepsMessage.payload.getString("name"))
        assertEquals("SHELTER", sepsMessage.payload.getString("zone_type"))
        assertEquals("Emergency shelter with medical facilities", 
                     sepsMessage.payload.getString("description"))
    }
    
    @Test
    fun testExtractSafeZone() {
        val locationJson = org.json.JSONObject().apply {
            put("latitude", 52.5200)
            put("longitude", 13.4050)
            put("accuracy", 10.0)
        }
        
        val payload = org.json.JSONObject().apply {
            put("zone_id", "zone-2")
            put("zone_type", "MEDICAL")
            put("location", locationJson)
            put("name", "Hospital")
            put("description", "Emergency medical center")
        }
        
        val sepsMessage = SepsMessage(
            messageId = "zone-test",
            timestamp = 1706432123456L,
            sender = SepsSender("com.other.app", "device6", "1.0"),
            messageType = SepsMessageType.SAFE_ZONE,
            routing = SepsRouting(10, 0, "BROADCAST", 1),
            payload = payload
        )
        
        val safeZone = SepsCodec.extractSafeZone(sepsMessage)
        
        assertNotNull(safeZone)
        assertEquals("zone-2", safeZone!!.id)
        assertEquals("Hospital", safeZone.name)
        assertEquals("Emergency medical center", safeZone.description)
        assertEquals(52.5200, safeZone.latitude, 0.0001)
        assertEquals(13.4050, safeZone.longitude, 0.0001)
    }
    
    @Test
    fun testMessageTypeConversions() {
        // Test all message type conversions
        val conversions = mapOf(
            MeshMessage.MessageType.DATA to SepsMessageType.TEXT_MESSAGE,
            MeshMessage.MessageType.ROUTE_REQUEST to SepsMessageType.ROUTE_REQUEST,
            MeshMessage.MessageType.ROUTE_REPLY to SepsMessageType.ROUTE_REPLY,
            MeshMessage.MessageType.HELLO to SepsMessageType.HELLO,
            MeshMessage.MessageType.LOCATION_UPDATE to SepsMessageType.LOCATION_UPDATE,
            MeshMessage.MessageType.HELP_REQUEST to SepsMessageType.HELP_REQUEST
        )
        
        conversions.forEach { (meshType, expectedSepsType) ->
            val meshMessage = MeshMessage(
                sourceId = "test",
                destinationId = "BROADCAST",
                payload = "test",
                messageType = meshType
            )
            
            val sepsMessage = SepsCodec.meshMessageToSeps(meshMessage, "test")
            assertEquals("Failed for $meshType", expectedSepsType, sepsMessage.messageType)
        }
    }
    
    @Test
    fun testRoundTripConversion() {
        val originalMesh = MeshMessage(
            sourceId = "device1",
            destinationId = "device2",
            payload = "Test message",
            messageType = MeshMessage.MessageType.DATA,
            ttl = 10,
            sequenceNumber = 5,
            hopCount = 2
        )
        
        // Convert to SEPS and back
        val seps = SepsCodec.meshMessageToSeps(originalMesh, "device1")
        val convertedMesh = SepsCodec.sepsToMeshMessage(seps)
        
        assertEquals(originalMesh.messageId, convertedMesh.messageId)
        assertEquals(originalMesh.sourceId, convertedMesh.sourceId)
        assertEquals(originalMesh.destinationId, convertedMesh.destinationId)
        assertEquals(originalMesh.messageType, convertedMesh.messageType)
        assertEquals(originalMesh.ttl, convertedMesh.ttl)
        assertEquals(originalMesh.hopCount, convertedMesh.hopCount)
        assertEquals(originalMesh.sequenceNumber, convertedMesh.sequenceNumber)
    }
}
