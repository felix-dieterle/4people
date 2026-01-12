package com.fourpeople.adhoc.location

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for LocationData class.
 */
class LocationDataTest {

    @Test
    fun testLocationDataCreation() {
        val location = LocationData(
            deviceId = "device-123",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f,
            altitude = 34.0
        )

        assertEquals("device-123", location.deviceId)
        assertEquals(52.520008, location.latitude, 0.000001)
        assertEquals(13.404954, location.longitude, 0.000001)
        assertEquals(10.0f, location.accuracy)
        assertEquals(34.0, location.altitude, 0.001)
        assertFalse(location.isHelpRequest)
        assertNull(location.helpMessage)
    }

    @Test
    fun testLocationDataWithHelpRequest() {
        val location = LocationData(
            deviceId = "device-456",
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 5.0f,
            isHelpRequest = true,
            helpMessage = "Need medical assistance"
        )

        assertTrue(location.isHelpRequest)
        assertEquals("Need medical assistance", location.helpMessage)
    }

    @Test
    fun testLocationDataToJson() {
        val location = LocationData(
            deviceId = "test-device",
            latitude = 48.8566,
            longitude = 2.3522,
            accuracy = 15.0f,
            altitude = 35.0,
            timestamp = 1234567890L,
            isHelpRequest = false
        )

        val json = location.toJson()
        
        assertTrue(json.contains("\"deviceId\":\"test-device\""))
        assertTrue(json.contains("\"latitude\":48.8566"))
        assertTrue(json.contains("\"longitude\":2.3522"))
        assertTrue(json.contains("\"accuracy\":15.0"))
        assertTrue(json.contains("\"altitude\":35.0"))
        assertTrue(json.contains("\"timestamp\":1234567890"))
        assertTrue(json.contains("\"isHelpRequest\":false"))
    }

    @Test
    fun testLocationDataFromJson() {
        val json = """{"deviceId":"device-789","latitude":51.5074,"longitude":-0.1278,"accuracy":8.0,"altitude":11.0,"timestamp":9876543210,"isHelpRequest":true,"helpMessage":"Emergency"}"""
        
        val location = LocationData.fromJson(json)
        
        assertNotNull(location)
        assertEquals("device-789", location?.deviceId)
        assertEquals(51.5074, location?.latitude ?: 0.0, 0.000001)
        assertEquals(-0.1278, location?.longitude ?: 0.0, 0.000001)
        assertEquals(8.0f, location?.accuracy)
        assertEquals(11.0, location?.altitude ?: 0.0, 0.001)
        assertEquals(9876543210L, location?.timestamp)
        assertTrue(location?.isHelpRequest ?: false)
        assertEquals("Emergency", location?.helpMessage)
    }

    @Test
    fun testLocationDataFromJsonWithoutHelpMessage() {
        val json = """{"deviceId":"device-xyz","latitude":35.6762,"longitude":139.6503,"accuracy":12.0,"altitude":40.0,"timestamp":1111111111,"isHelpRequest":false,"helpMessage":""}"""
        
        val location = LocationData.fromJson(json)
        
        assertNotNull(location)
        assertEquals("device-xyz", location?.deviceId)
        assertFalse(location?.isHelpRequest ?: true)
        assertNull(location?.helpMessage)
    }

    @Test
    fun testLocationDataFromInvalidJson() {
        val invalidJson = """{"invalid": "json"}"""
        
        val location = LocationData.fromJson(invalidJson)
        
        assertNull(location)
    }

    @Test
    fun testLocationDataIsFresh() {
        val freshLocation = LocationData(
            deviceId = "device-fresh",
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis() - 60000 // 1 minute ago
        )

        assertTrue(freshLocation.isFresh())
    }

    @Test
    fun testLocationDataIsStale() {
        val staleLocation = LocationData(
            deviceId = "device-stale",
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis() - (6 * 60 * 1000) // 6 minutes ago
        )

        assertFalse(staleLocation.isFresh())
    }

    @Test
    fun testLocationDataRoundTrip() {
        val original = LocationData(
            deviceId = "roundtrip-test",
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 20.0f,
            altitude = 16.0,
            isHelpRequest = true,
            helpMessage = "Lost in forest"
        )

        val json = original.toJson()
        val parsed = LocationData.fromJson(json)

        assertNotNull(parsed)
        assertEquals(original.deviceId, parsed?.deviceId)
        assertEquals(original.latitude, parsed?.latitude ?: 0.0, 0.000001)
        assertEquals(original.longitude, parsed?.longitude ?: 0.0, 0.000001)
        assertEquals(original.accuracy, parsed?.accuracy)
        assertEquals(original.altitude, parsed?.altitude ?: 0.0, 0.001)
        assertEquals(original.isHelpRequest, parsed?.isHelpRequest)
        assertEquals(original.helpMessage, parsed?.helpMessage)
    }
}
