package com.fourpeople.adhoc.location

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After

/**
 * Unit tests for LocationDataStore singleton.
 */
class LocationDataStoreTest {

    @Before
    fun setUp() {
        // Clear any existing data before each test
        LocationDataStore.clear()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        LocationDataStore.clear()
    }

    @Test
    fun testUpdateLocation() {
        val location = LocationData(
            deviceId = "device-001",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )

        LocationDataStore.updateLocation(location)

        val retrieved = LocationDataStore.getLocation("device-001")
        assertNotNull(retrieved)
        assertEquals("device-001", retrieved?.deviceId)
    }

    @Test
    fun testUpdateMultipleLocations() {
        val location1 = LocationData(
            deviceId = "device-001",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )

        val location2 = LocationData(
            deviceId = "device-002",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f
        )

        LocationDataStore.updateLocation(location1)
        LocationDataStore.updateLocation(location2)

        val allLocations = LocationDataStore.getAllLocations()
        assertEquals(2, allLocations.size)
    }

    @Test
    fun testUpdateSameDeviceLocation() {
        val location1 = LocationData(
            deviceId = "device-001",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )

        val location2 = LocationData(
            deviceId = "device-001",
            latitude = 52.525008,
            longitude = 13.409954,
            accuracy = 8.0f
        )

        LocationDataStore.updateLocation(location1)
        LocationDataStore.updateLocation(location2)

        val allLocations = LocationDataStore.getAllLocations()
        assertEquals(1, allLocations.size)

        val retrieved = LocationDataStore.getLocation("device-001")
        assertEquals(52.525008, retrieved?.latitude ?: 0.0, 0.000001)
        assertEquals(8.0f, retrieved?.accuracy)
    }

    @Test
    fun testGetLocation() {
        val location = LocationData(
            deviceId = "device-001",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )

        LocationDataStore.updateLocation(location)

        val retrieved = LocationDataStore.getLocation("device-001")
        assertNotNull(retrieved)
        assertEquals(52.520008, retrieved?.latitude ?: 0.0, 0.000001)
        assertEquals(13.404954, retrieved?.longitude ?: 0.0, 0.000001)
    }

    @Test
    fun testGetNonExistentLocation() {
        val retrieved = LocationDataStore.getLocation("non-existent")
        assertNull(retrieved)
    }

    @Test
    fun testGetAllLocations() {
        LocationDataStore.updateLocation(
            LocationData("device-001", 52.520008, 13.404954, 10.0f)
        )
        LocationDataStore.updateLocation(
            LocationData("device-002", 48.1351, 11.5820, 10.0f)
        )
        LocationDataStore.updateLocation(
            LocationData("device-003", 51.5074, -0.1278, 10.0f)
        )

        val allLocations = LocationDataStore.getAllLocations()
        assertEquals(3, allLocations.size)
    }

    @Test
    fun testGetAllLocationsEmpty() {
        val allLocations = LocationDataStore.getAllLocations()
        assertEquals(0, allLocations.size)
    }

    @Test
    fun testGetHelpRequests() {
        val normalLocation = LocationData(
            deviceId = "device-001",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f,
            isHelpRequest = false
        )

        val helpRequest1 = LocationData(
            deviceId = "device-002",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f,
            isHelpRequest = true,
            helpMessage = "Medical emergency"
        )

        val helpRequest2 = LocationData(
            deviceId = "device-003",
            latitude = 51.5074,
            longitude = -0.1278,
            accuracy = 10.0f,
            isHelpRequest = true,
            helpMessage = "Fire"
        )

        LocationDataStore.updateLocation(normalLocation)
        LocationDataStore.updateLocation(helpRequest1)
        LocationDataStore.updateLocation(helpRequest2)

        val helpRequests = LocationDataStore.getHelpRequests()
        assertEquals(2, helpRequests.size)
        assertTrue(helpRequests.all { it.isHelpRequest })
    }

    @Test
    fun testGetHelpRequestsEmpty() {
        val normalLocation = LocationData(
            deviceId = "device-001",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f,
            isHelpRequest = false
        )

        LocationDataStore.updateLocation(normalLocation)

        val helpRequests = LocationDataStore.getHelpRequests()
        assertEquals(0, helpRequests.size)
    }

    @Test
    fun testClearLocations() {
        LocationDataStore.updateLocation(
            LocationData("device-001", 52.520008, 13.404954, 10.0f)
        )
        LocationDataStore.updateLocation(
            LocationData("device-002", 48.1351, 11.5820, 10.0f)
        )

        assertEquals(2, LocationDataStore.getAllLocations().size)

        LocationDataStore.clear()
        assertEquals(0, LocationDataStore.getAllLocations().size)
    }

    @Test
    fun testLocationUpdateListener() {
        var updatedLocation: LocationData? = null
        var removedDeviceId: String? = null

        val listener = object : LocationDataStore.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                updatedLocation = locationData
            }

            override fun onLocationRemoved(deviceId: String) {
                removedDeviceId = deviceId
            }
        }

        LocationDataStore.addListener(listener)

        val location = LocationData(
            deviceId = "device-001",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f
        )

        LocationDataStore.updateLocation(location)

        assertNotNull(updatedLocation)
        assertEquals("device-001", updatedLocation?.deviceId)

        LocationDataStore.clear()

        assertEquals("device-001", removedDeviceId)

        LocationDataStore.removeListener(listener)
    }

    @Test
    fun testMultipleListeners() {
        var listener1Called = false
        var listener2Called = false

        val listener1 = object : LocationDataStore.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                listener1Called = true
            }

            override fun onLocationRemoved(deviceId: String) {}
        }

        val listener2 = object : LocationDataStore.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                listener2Called = true
            }

            override fun onLocationRemoved(deviceId: String) {}
        }

        LocationDataStore.addListener(listener1)
        LocationDataStore.addListener(listener2)

        LocationDataStore.updateLocation(
            LocationData("device-001", 52.520008, 13.404954, 10.0f)
        )

        assertTrue(listener1Called)
        assertTrue(listener2Called)

        LocationDataStore.removeListener(listener1)
        LocationDataStore.removeListener(listener2)
    }

    @Test
    fun testRemoveListenerDoesNotAffectOthers() {
        var listener1Called = false
        var listener2Called = false

        val listener1 = object : LocationDataStore.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                listener1Called = true
            }

            override fun onLocationRemoved(deviceId: String) {}
        }

        val listener2 = object : LocationDataStore.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                listener2Called = true
            }

            override fun onLocationRemoved(deviceId: String) {}
        }

        LocationDataStore.addListener(listener1)
        LocationDataStore.addListener(listener2)
        LocationDataStore.removeListener(listener1)

        LocationDataStore.updateLocation(
            LocationData("device-001", 52.520008, 13.404954, 10.0f)
        )

        assertFalse(listener1Called)
        assertTrue(listener2Called)

        LocationDataStore.removeListener(listener2)
    }

    @Test
    fun testAddSameListenerTwice() {
        var callCount = 0

        val listener = object : LocationDataStore.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                callCount++
            }

            override fun onLocationRemoved(deviceId: String) {}
        }

        LocationDataStore.addListener(listener)
        LocationDataStore.addListener(listener)  // Add same listener again

        LocationDataStore.updateLocation(
            LocationData("device-001", 52.520008, 13.404954, 10.0f)
        )

        // Should only be called once (no duplicates)
        assertEquals(1, callCount)

        LocationDataStore.removeListener(listener)
    }

    @Test
    fun testClearNotifiesListeners() {
        var removedIds = mutableListOf<String>()

        val listener = object : LocationDataStore.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {}

            override fun onLocationRemoved(deviceId: String) {
                removedIds.add(deviceId)
            }
        }

        LocationDataStore.addListener(listener)

        LocationDataStore.updateLocation(
            LocationData("device-001", 52.520008, 13.404954, 10.0f)
        )
        LocationDataStore.updateLocation(
            LocationData("device-002", 48.1351, 11.5820, 10.0f)
        )

        LocationDataStore.clear()

        assertEquals(2, removedIds.size)
        assertTrue(removedIds.contains("device-001"))
        assertTrue(removedIds.contains("device-002"))

        LocationDataStore.removeListener(listener)
    }

    @Test
    fun testStaleLocationCleanup() {
        // Add a fresh location
        val freshLocation = LocationData(
            deviceId = "device-fresh",
            latitude = 52.520008,
            longitude = 13.404954,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )

        // Add a stale location (older than 10 minutes)
        val staleLocation = LocationData(
            deviceId = "device-stale",
            latitude = 48.1351,
            longitude = 11.5820,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis() - (11 * 60 * 1000)
        )

        LocationDataStore.updateLocation(freshLocation)
        LocationDataStore.updateLocation(staleLocation)

        // Adding a new location triggers cleanup
        val anotherLocation = LocationData(
            deviceId = "device-another",
            latitude = 51.5074,
            longitude = -0.1278,
            accuracy = 10.0f,
            timestamp = System.currentTimeMillis()
        )
        LocationDataStore.updateLocation(anotherLocation)

        val locations = LocationDataStore.getAllLocations()

        // Stale location should be removed
        assertFalse(locations.any { it.deviceId == "device-stale" })
        assertTrue(locations.any { it.deviceId == "device-fresh" })
        assertTrue(locations.any { it.deviceId == "device-another" })
    }

    @Test
    fun testThreadSafety() {
        // Test concurrent access (basic test - not exhaustive)
        val threads = List(10) { index ->
            Thread {
                LocationDataStore.updateLocation(
                    LocationData(
                        deviceId = "device-$index",
                        latitude = index.toDouble(),
                        longitude = index.toDouble(),
                        accuracy = 10.0f
                    )
                )
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val locations = LocationDataStore.getAllLocations()
        assertEquals(10, locations.size)
    }
}
