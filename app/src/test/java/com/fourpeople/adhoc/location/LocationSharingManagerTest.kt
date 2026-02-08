package com.fourpeople.adhoc.location

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Location domain classes.
 * 
 * Note: LocationSharingManager requires Android framework dependencies
 * (Context, LocationManager, LocationListener) which are not available  
 * in unit tests. These tests focus on LocationData which can be tested 
 * without Android dependencies.
 * 
 * Full LocationSharingManager functionality should be tested with 
 * Android instrumentation tests.
 */
class LocationSharingManagerTest {

    @Test
    fun testLocationDataCreation() {
        val locationData = LocationData(
            deviceId = "participant-123",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        assertEquals("participant-123", locationData.deviceId)
        assertEquals(52.520008, locationData.latitude, 0.000001)
        assertEquals(13.404954, locationData.longitude, 0.000001)
        assertEquals(10.0f, locationData.accuracy)
        assertFalse(locationData.isHelpRequest)
        assertFalse(locationData.isForwarded)
    }

    @Test
    fun testLocationDataWithHelpRequest() {
        val testLocation = LocationData(
            deviceId = "test-device",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        val helpRequest = testLocation.copy(
            isHelpRequest = true,
            helpMessage = "Need medical assistance",
            eventRadiusKm = 100.0,
            isForwarded = false
        )
        
        assertTrue(helpRequest.isHelpRequest)
        assertEquals("Need medical assistance", helpRequest.helpMessage)
        assertEquals(100.0, helpRequest.eventRadiusKm, 0.001)
        assertFalse(helpRequest.isForwarded)
    }

    @Test
    fun testLocationDataCopy() {
        val location1 = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        val location2 = location1.copy(
            latitude = 52.525008,
            longitude = 13.409954,
            accuracy = 8.0f
        )
        
        // Original should be unchanged
        assertEquals(52.520008, location1.latitude, 0.000001)
        assertEquals(13.404954, location1.longitude, 0.000001)
        assertEquals(10.0f, location1.accuracy)
        
        // Copy should have new values
        assertEquals("device-1", location2.deviceId)
        assertEquals(52.525008, location2.latitude, 0.000001)
        assertEquals(13.409954, location2.longitude, 0.000001)
        assertEquals(8.0f, location2.accuracy)
    }

    @Test
    fun testHelpRequestWithCustomRadius() {
        val testLocation = LocationData(
            deviceId = "test-device",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        val helpRequest = testLocation.copy(
            isHelpRequest = true,
            helpMessage = "Fire emergency",
            eventRadiusKm = 50.0,
            isForwarded = false
        )
        
        assertEquals(50.0, helpRequest.eventRadiusKm, 0.001)
    }

    @Test
    fun testForwardHelpRequest() {
        val originalRequest = LocationData(
            deviceId = "original-device",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f,
            isHelpRequest = true,
            helpMessage = "Need evacuation",
            eventRadiusKm = 100.0
        )
        
        val forwarderLocation = LocationData(
            deviceId = "forwarder-device",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        val forwardedRequest = forwarderLocation.copy(
            isHelpRequest = true,
            helpMessage = originalRequest.helpMessage,
            eventRadiusKm = 100.0,
            isForwarded = true
        )
        
        assertTrue(forwardedRequest.isHelpRequest)
        assertEquals("Need evacuation", forwardedRequest.helpMessage)
        assertTrue(forwardedRequest.isForwarded)
        assertEquals("forwarder-device", forwardedRequest.deviceId)
    }

    @Test
    fun testLocationDataWithTimestamp() {
        val timestamp = System.currentTimeMillis()
        val location = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f,
            timestamp = timestamp
        )
        
        assertEquals(timestamp, location.timestamp)
    }

    @Test
    fun testLocationDataEquality() {
        val location1 = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        val location2 = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        // Note: timestamps will differ, so they won't be equal
        // This tests that deviceId and coordinates match
        assertEquals(location1.deviceId, location2.deviceId)
        assertEquals(location1.latitude, location2.latitude, 0.000001)
        assertEquals(location1.longitude, location2.longitude, 0.000001)
    }

    @Test
    fun testMultipleHelpRequests() {
        val helpRequest1 = LocationData(
            deviceId = "device-2",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f,
            isHelpRequest = true,
            helpMessage = "Medical emergency"
        )
        
        val helpRequest2 = LocationData(
            deviceId = "device-3",
            latitude = 51.5074,
            longitude = -0.1278,
            accuracy = 10.0f,
            isHelpRequest = true,
            helpMessage = "Fire"
        )
        
        assertTrue(helpRequest1.isHelpRequest)
        assertTrue(helpRequest2.isHelpRequest)
        assertEquals("Medical emergency", helpRequest1.helpMessage)
        assertEquals("Fire", helpRequest2.helpMessage)
    }

    @Test
    fun testLocationDataWithDefaultValues() {
        val location = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        // Verify default values
        assertFalse(location.isHelpRequest)
        assertNull(location.helpMessage)
        assertEquals(0.0, location.eventRadiusKm, 0.001)
        assertFalse(location.isForwarded)
        assertTrue(location.timestamp > 0) // Should have a timestamp
    }
}
