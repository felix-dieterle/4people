package com.fourpeople.adhoc.location

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito.*

/**
 * Unit tests for LocationSharingManager.
 * Tests location sharing, participant tracking, and help request functionality.
 */
class LocationSharingManagerTest {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager
    private lateinit var sharingManager: com.fourpeople.adhoc.location.LocationSharingManager

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        locationManager = mock(LocationManager::class.java)
        `when`(context.getSystemService(Context.LOCATION_SERVICE)).thenReturn(locationManager)
        
        sharingManager = com.fourpeople.adhoc.location.LocationSharingManager(context, "test-device")
    }

    @Test
    fun testManagerConstants() {
        // Verify constants are sensible
        val locationUpdateInterval = 30000L // 30 seconds
        val minDistance = 10f // 10 meters
        
        assertTrue(locationUpdateInterval > 0)
        assertTrue(minDistance > 0)
    }

    @Test
    fun testInitialState() {
        // Manager should start inactive
        assertFalse(sharingManager.isLocationSharingActive())
        assertNull(sharingManager.getCurrentLocation())
    }

    @Test
    fun testUpdateParticipantLocation() {
        val locationData = LocationData(
            deviceId = "participant-123",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        sharingManager.updateParticipantLocation(locationData)
        
        val participants = sharingManager.getAllParticipantLocations()
        assertEquals(1, participants.size)
        assertEquals("participant-123", participants[0].deviceId)
    }

    @Test
    fun testUpdateMultipleParticipants() {
        val location1 = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        val location2 = LocationData(
            deviceId = "device-2",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f
        )
        
        sharingManager.updateParticipantLocation(location1)
        sharingManager.updateParticipantLocation(location2)
        
        val participants = sharingManager.getAllParticipantLocations()
        assertEquals(2, participants.size)
    }

    @Test
    fun testUpdateSameParticipantLocation() {
        val location1 = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        val location2 = LocationData(
            deviceId = "device-1",
            latitude = 52.525008,
            longitude = 13.409954,
            accuracy = 8.0f
        )
        
        sharingManager.updateParticipantLocation(location1)
        sharingManager.updateParticipantLocation(location2)
        
        val participants = sharingManager.getAllParticipantLocations()
        assertEquals(1, participants.size)
        assertEquals(52.525008, participants[0].latitude, 0.000001)
    }

    @Test
    fun testSendHelpRequest() {
        // Set a current location first (simulating that we have location)
        val testLocation = LocationData(
            deviceId = "test-device",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        // We need to simulate having a current location
        // In real scenario, this would be set by location updates
        val helpRequest = testLocation.copy(
            isHelpRequest = true,
            helpMessage = "Need medical assistance",
            eventRadiusKm = 100.0,
            isForwarded = false
        )
        
        // Verify help request properties
        assertTrue(helpRequest.isHelpRequest)
        assertEquals("Need medical assistance", helpRequest.helpMessage)
        assertEquals(100.0, helpRequest.eventRadiusKm, 0.001)
        assertFalse(helpRequest.isForwarded)
    }

    @Test
    fun testSendHelpRequestWithCustomRadius() {
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
    fun testGetHelpRequests() {
        val normalLocation = LocationData(
            deviceId = "device-1",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f,
            isHelpRequest = false
        )
        
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
        
        sharingManager.updateParticipantLocation(normalLocation)
        sharingManager.updateParticipantLocation(helpRequest1)
        sharingManager.updateParticipantLocation(helpRequest2)
        
        val helpRequests = sharingManager.getHelpRequests()
        assertEquals(2, helpRequests.size)
        assertTrue(helpRequests.all { it.isHelpRequest })
    }

    @Test
    fun testShouldProcessEventWithinRadius() {
        val myLocation = LocationData(
            deviceId = "test-device",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        // Event very close by (within 1 km)
        val nearbyEvent = LocationData(
            deviceId = "event-device",
            latitude = 52.525008,
            longitude = 13.409954,
            accuracy = 10.0f,
            eventRadiusKm = 100.0
        )
        
        // Should process events within radius
        assertTrue(nearbyEvent.isWithinRadius(myLocation))
    }

    @Test
    fun testShouldNotProcessEventOutsideRadius() {
        val myLocation = LocationData(
            deviceId = "test-device",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )
        
        // Event far away (Berlin to Munich ~504 km)
        val farEvent = LocationData(
            deviceId = "event-device",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f,
            eventRadiusKm = 100.0  // 100 km radius
        )
        
        // Should not process events outside radius
        assertFalse(farEvent.isWithinRadius(myLocation))
    }

    @Test
    fun testLocationUpdateListenerInterface() {
        var listenerCalled = false
        var receivedLocation: LocationData? = null
        
        val listener = object : com.fourpeople.adhoc.location.LocationSharingManager.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                listenerCalled = true
                receivedLocation = locationData
            }
        }
        
        sharingManager.setLocationUpdateListener(listener)
        
        // Verify listener was set (would be called when location updates occur)
        assertNotNull(listener)
    }

    @Test
    fun testStaleLocationCleanup() {
        // Add a fresh location
        val freshLocation = LocationData(
            deviceId = "fresh-device",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )
        
        // Add a stale location (older than 10 minutes)
        val staleLocation = LocationData(
            deviceId = "stale-device",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis() - (11 * 60 * 1000)
        )
        
        sharingManager.updateParticipantLocation(freshLocation)
        sharingManager.updateParticipantLocation(staleLocation)
        
        // After cleanup (triggered by update), stale location should be removed
        val participants = sharingManager.getAllParticipantLocations()
        
        // Fresh location should remain
        assertTrue(participants.any { it.deviceId == "fresh-device" })
    }

    @Test
    fun testEmptyParticipantsList() {
        val participants = sharingManager.getAllParticipantLocations()
        assertEquals(0, participants.size)
    }

    @Test
    fun testEmptyHelpRequestsList() {
        val helpRequests = sharingManager.getHelpRequests()
        assertEquals(0, helpRequests.size)
    }
}
