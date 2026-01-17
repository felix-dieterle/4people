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

    @Test
    fun testLocationDataJsonEscaping() {
        val location = LocationData(
            deviceId = "test\"device\\with\nspecial\tcharacters",
            latitude = 0.0,
            longitude = 0.0,
            accuracy = 10.0f,
            isHelpRequest = true,
            helpMessage = "Help\nme\tplease\"urgent\\"
        )

        val json = location.toJson()
        
        // Verify that special characters are properly escaped
        assertTrue(json.contains("\\\""))  // Escaped quotes
        assertTrue(json.contains("\\\\"))  // Escaped backslashes
        assertTrue(json.contains("\\n"))   // Escaped newlines
        assertTrue(json.contains("\\t"))   // Escaped tabs
        assertFalse(json.contains("\n"))   // No raw newlines
        assertFalse(json.contains("\t"))   // No raw tabs

        // Verify round-trip preserves data
        val parsed = LocationData.fromJson(json)
        assertNotNull(parsed)
        assertEquals(location.deviceId, parsed?.deviceId)
        assertEquals(location.helpMessage, parsed?.helpMessage)
    }

    @Test
    fun testEventRadiusDefault() {
        val location = LocationData(
            deviceId = "device-123",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )

        assertEquals(100.0, location.eventRadiusKm, 0.001)
        assertFalse(location.isForwarded)
    }

    @Test
    fun testEventRadiusCustom() {
        val location = LocationData(
            deviceId = "device-456",
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 5.0f,
            eventRadiusKm = 50.0,
            isForwarded = true
        )

        assertEquals(50.0, location.eventRadiusKm, 0.001)
        assertTrue(location.isForwarded)
    }

    @Test
    fun testDistanceCalculation() {
        // Berlin coordinates
        val berlin = LocationData(
            deviceId = "berlin",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )

        // Munich coordinates (approximately 504 km from Berlin)
        val munich = LocationData(
            deviceId = "munich",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f
        )

        val distance = berlin.distanceToKm(munich)
        
        // Distance should be approximately 504 km (±10 km tolerance)
        assertTrue(distance > 494.0 && distance < 514.0)
    }

    @Test
    fun testDistanceCalculationSameLocation() {
        val location1 = LocationData(
            deviceId = "loc1",
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 5.0f
        )

        val location2 = LocationData(
            deviceId = "loc2",
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 5.0f
        )

        val distance = location1.distanceToKm(location2)
        
        // Distance should be very close to 0
        assertTrue(distance < 0.001)
    }

    @Test
    fun testIsWithinRadius() {
        // New York coordinates
        val newYork = LocationData(
            deviceId = "ny",
            latitude = 40.7128,
            longitude = -74.0060,
            accuracy = 5.0f,
            eventRadiusKm = 100.0
        )

        // Philadelphia coordinates (approximately 130 km from New York)
        val philadelphia = LocationData(
            deviceId = "philly",
            latitude = 39.9526,
            longitude = -75.1652,
            accuracy = 5.0f
        )

        // Philadelphia is outside 100km radius
        assertFalse(newYork.isWithinRadius(philadelphia))
    }

    @Test
    fun testIsWithinRadiusInside() {
        // London coordinates
        val london = LocationData(
            deviceId = "london",
            latitude = 51.5074,
            longitude = -0.1278,
            accuracy = 5.0f,
            eventRadiusKm = 200.0
        )

        // Cambridge coordinates (approximately 80 km from London)
        val cambridge = LocationData(
            deviceId = "cambridge",
            latitude = 52.2053,
            longitude = 0.1218,
            accuracy = 5.0f
        )

        // Cambridge is within 200km radius
        assertTrue(london.isWithinRadius(cambridge))
    }

    @Test
    fun testEventRadiusToJson() {
        val location = LocationData(
            deviceId = "test-radius",
            latitude = 48.8566,
            longitude = 2.3522,
            accuracy = 15.0f,
            altitude = 35.0,
            timestamp = 1234567890L,
            isHelpRequest = true,
            helpMessage = "Emergency",
            eventRadiusKm = 75.0,
            isForwarded = true
        )

        val json = location.toJson()
        
        assertTrue(json.contains("\"eventRadiusKm\":75.0"))
        assertTrue(json.contains("\"isForwarded\":true"))
    }

    @Test
    fun testEventRadiusFromJson() {
        val json = """{"deviceId":"device-radius","latitude":51.5074,"longitude":-0.1278,"accuracy":8.0,"altitude":11.0,"timestamp":9876543210,"isHelpRequest":true,"helpMessage":"Emergency","eventRadiusKm":150.0,"isForwarded":true}"""
        
        val location = LocationData.fromJson(json)
        
        assertNotNull(location)
        assertEquals(150.0, location?.eventRadiusKm ?: 0.0, 0.001)
        assertTrue(location?.isForwarded ?: false)
    }

    @Test
    fun testEventRadiusFromJsonWithoutRadiusField() {
        // Test backward compatibility - JSON without radius field should default to 100.0
        val json = """{"deviceId":"device-old","latitude":51.5074,"longitude":-0.1278,"accuracy":8.0,"altitude":11.0,"timestamp":9876543210,"isHelpRequest":false,"helpMessage":""}"""
        
        val location = LocationData.fromJson(json)
        
        assertNotNull(location)
        assertEquals(100.0, location?.eventRadiusKm ?: 0.0, 0.001)
        assertFalse(location?.isForwarded ?: true)
    }

    @Test
    fun testEventRadiusRoundTrip() {
        val original = LocationData(
            deviceId = "roundtrip-radius",
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 20.0f,
            altitude = 16.0,
            isHelpRequest = true,
            helpMessage = "Help needed",
            eventRadiusKm = 250.0,
            isForwarded = true
        )

        val json = original.toJson()
        val parsed = LocationData.fromJson(json)

        assertNotNull(parsed)
        assertEquals(original.eventRadiusKm, parsed?.eventRadiusKm ?: 0.0, 0.001)
        assertEquals(original.isForwarded, parsed?.isForwarded)
    }
}
