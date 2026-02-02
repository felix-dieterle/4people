package com.fourpeople.adhoc.location

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After

/**
 * Unit tests for SafeZoneManager singleton.
 */
class SafeZoneManagerTest {

    @Before
    fun setUp() {
        // Clear any existing safe zones before each test
        SafeZoneManager.clear()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        SafeZoneManager.clear()
    }

    @Test
    fun testAddSafeZone() {
        val safeZone = SafeZone(
            id = "sz-001",
            name = "City Hall",
            latitude = 52.520008,
            longitude = 13.404954,
            description = "Main gathering point"
        )

        SafeZoneManager.addSafeZone(safeZone)

        val zones = SafeZoneManager.getAllSafeZones()
        assertEquals(1, zones.size)
        assertEquals("sz-001", zones[0].id)
    }

    @Test
    fun testAddMultipleSafeZones() {
        val zone1 = SafeZone(
            id = "sz-001",
            name = "Zone 1",
            latitude = 52.520008,
            longitude = 13.404954
        )

        val zone2 = SafeZone(
            id = "sz-002",
            name = "Zone 2",
            latitude = 48.1351,
            longitude = 11.5820
        )

        SafeZoneManager.addSafeZone(zone1)
        SafeZoneManager.addSafeZone(zone2)

        val zones = SafeZoneManager.getAllSafeZones()
        assertEquals(2, zones.size)
    }

    @Test
    fun testUpdateSafeZone() {
        val zone1 = SafeZone(
            id = "sz-001",
            name = "Original Name",
            latitude = 52.520008,
            longitude = 13.404954
        )

        val zone2 = SafeZone(
            id = "sz-001",
            name = "Updated Name",
            latitude = 52.520008,
            longitude = 13.404954
        )

        SafeZoneManager.addSafeZone(zone1)
        SafeZoneManager.addSafeZone(zone2)

        val zones = SafeZoneManager.getAllSafeZones()
        assertEquals(1, zones.size)
        assertEquals("Updated Name", zones[0].name)
    }

    @Test
    fun testGetSafeZone() {
        val safeZone = SafeZone(
            id = "sz-001",
            name = "Test Zone",
            latitude = 52.520008,
            longitude = 13.404954
        )

        SafeZoneManager.addSafeZone(safeZone)

        val retrieved = SafeZoneManager.getSafeZone("sz-001")
        assertNotNull(retrieved)
        assertEquals("Test Zone", retrieved?.name)
    }

    @Test
    fun testGetNonExistentSafeZone() {
        val retrieved = SafeZoneManager.getSafeZone("non-existent")
        assertNull(retrieved)
    }

    @Test
    fun testRemoveSafeZone() {
        val safeZone = SafeZone(
            id = "sz-001",
            name = "To Remove",
            latitude = 52.520008,
            longitude = 13.404954
        )

        SafeZoneManager.addSafeZone(safeZone)
        assertEquals(1, SafeZoneManager.getAllSafeZones().size)

        SafeZoneManager.removeSafeZone("sz-001")
        assertEquals(0, SafeZoneManager.getAllSafeZones().size)
    }

    @Test
    fun testRemoveNonExistentSafeZone() {
        // Should not throw exception
        SafeZoneManager.removeSafeZone("non-existent")
        assertEquals(0, SafeZoneManager.getAllSafeZones().size)
    }

    @Test
    fun testClearSafeZones() {
        SafeZoneManager.addSafeZone(SafeZone("sz-001", "Zone 1", 0.0, 0.0))
        SafeZoneManager.addSafeZone(SafeZone("sz-002", "Zone 2", 1.0, 1.0))
        SafeZoneManager.addSafeZone(SafeZone("sz-003", "Zone 3", 2.0, 2.0))

        assertEquals(3, SafeZoneManager.getAllSafeZones().size)

        SafeZoneManager.clear()
        assertEquals(0, SafeZoneManager.getAllSafeZones().size)
    }

    @Test
    fun testGetNearestSafeZone() {
        // Berlin
        val zone1 = SafeZone(
            id = "sz-001",
            name = "Berlin Zone",
            latitude = 52.520008,
            longitude = 13.404954
        )

        // Munich
        val zone2 = SafeZone(
            id = "sz-002",
            name = "Munich Zone",
            latitude = 48.1351,
            longitude = 11.5820
        )

        SafeZoneManager.addSafeZone(zone1)
        SafeZoneManager.addSafeZone(zone2)

        // Location closer to Berlin
        val nearest = SafeZoneManager.getNearestSafeZone(52.5, 13.4)
        assertNotNull(nearest)
        assertEquals("sz-001", nearest?.id)
    }

    @Test
    fun testGetNearestSafeZoneWhenEmpty() {
        val nearest = SafeZoneManager.getNearestSafeZone(52.520008, 13.404954)
        assertNull(nearest)
    }

    @Test
    fun testGetNearestSafeZoneSingleZone() {
        val zone = SafeZone(
            id = "sz-001",
            name = "Only Zone",
            latitude = 52.520008,
            longitude = 13.404954
        )

        SafeZoneManager.addSafeZone(zone)

        val nearest = SafeZoneManager.getNearestSafeZone(48.1351, 11.5820)
        assertNotNull(nearest)
        assertEquals("sz-001", nearest?.id)
    }

    @Test
    fun testSafeZoneListener() {
        var addedZone: SafeZone? = null
        var removedId: String? = null

        val listener = object : SafeZoneManager.SafeZoneUpdateListener {
            override fun onSafeZoneAdded(safeZone: SafeZone) {
                addedZone = safeZone
            }

            override fun onSafeZoneRemoved(safeZoneId: String) {
                removedId = safeZoneId
            }
        }

        SafeZoneManager.addListener(listener)

        val zone = SafeZone("sz-001", "Test", 0.0, 0.0)
        SafeZoneManager.addSafeZone(zone)

        assertEquals("sz-001", addedZone?.id)

        SafeZoneManager.removeSafeZone("sz-001")

        assertEquals("sz-001", removedId)

        SafeZoneManager.removeListener(listener)
    }

    @Test
    fun testMultipleListeners() {
        var listener1Called = false
        var listener2Called = false

        val listener1 = object : SafeZoneManager.SafeZoneUpdateListener {
            override fun onSafeZoneAdded(safeZone: SafeZone) {
                listener1Called = true
            }

            override fun onSafeZoneRemoved(safeZoneId: String) {}
        }

        val listener2 = object : SafeZoneManager.SafeZoneUpdateListener {
            override fun onSafeZoneAdded(safeZone: SafeZone) {
                listener2Called = true
            }

            override fun onSafeZoneRemoved(safeZoneId: String) {}
        }

        SafeZoneManager.addListener(listener1)
        SafeZoneManager.addListener(listener2)

        SafeZoneManager.addSafeZone(SafeZone("sz-001", "Test", 0.0, 0.0))

        assertTrue(listener1Called)
        assertTrue(listener2Called)

        SafeZoneManager.removeListener(listener1)
        SafeZoneManager.removeListener(listener2)
    }

    @Test
    fun testRemoveListenerDoesNotAffectOthers() {
        var listener1Called = false
        var listener2Called = false

        val listener1 = object : SafeZoneManager.SafeZoneUpdateListener {
            override fun onSafeZoneAdded(safeZone: SafeZone) {
                listener1Called = true
            }

            override fun onSafeZoneRemoved(safeZoneId: String) {}
        }

        val listener2 = object : SafeZoneManager.SafeZoneUpdateListener {
            override fun onSafeZoneAdded(safeZone: SafeZone) {
                listener2Called = true
            }

            override fun onSafeZoneRemoved(safeZoneId: String) {}
        }

        SafeZoneManager.addListener(listener1)
        SafeZoneManager.addListener(listener2)
        SafeZoneManager.removeListener(listener1)

        SafeZoneManager.addSafeZone(SafeZone("sz-001", "Test", 0.0, 0.0))

        assertFalse(listener1Called)
        assertTrue(listener2Called)

        SafeZoneManager.removeListener(listener2)
    }

    @Test
    fun testCleanupInvalidSafeZones() {
        // Add a valid safe zone
        val validZone = SafeZone(
            id = "sz-valid",
            name = "Valid Zone",
            latitude = 52.520008,
            longitude = 13.404954,
            timestamp = System.currentTimeMillis()
        )

        // Add an invalid safe zone (older than 24 hours)
        val invalidZone = SafeZone(
            id = "sz-invalid",
            name = "Invalid Zone",
            latitude = 48.1351,
            longitude = 11.5820,
            timestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        )

        SafeZoneManager.addSafeZone(validZone)
        SafeZoneManager.addSafeZone(invalidZone)

        // Adding a new zone triggers cleanup
        val anotherZone = SafeZone(
            id = "sz-another",
            name = "Another Zone",
            latitude = 51.5074,
            longitude = -0.1278,
            timestamp = System.currentTimeMillis()
        )
        SafeZoneManager.addSafeZone(anotherZone)

        val zones = SafeZoneManager.getAllSafeZones()
        
        // Invalid zone should be removed
        assertFalse(zones.any { it.id == "sz-invalid" })
        assertTrue(zones.any { it.id == "sz-valid" })
        assertTrue(zones.any { it.id == "sz-another" })
    }

    @Test
    fun testClearNotifiesListeners() {
        var removedIds = mutableListOf<String>()

        val listener = object : SafeZoneManager.SafeZoneUpdateListener {
            override fun onSafeZoneAdded(safeZone: SafeZone) {}

            override fun onSafeZoneRemoved(safeZoneId: String) {
                removedIds.add(safeZoneId)
            }
        }

        SafeZoneManager.addListener(listener)

        SafeZoneManager.addSafeZone(SafeZone("sz-001", "Zone 1", 0.0, 0.0))
        SafeZoneManager.addSafeZone(SafeZone("sz-002", "Zone 2", 1.0, 1.0))

        SafeZoneManager.clear()

        assertEquals(2, removedIds.size)
        assertTrue(removedIds.contains("sz-001"))
        assertTrue(removedIds.contains("sz-002"))

        SafeZoneManager.removeListener(listener)
    }

    @Test
    fun testAddSameListenerTwice() {
        var callCount = 0

        val listener = object : SafeZoneManager.SafeZoneUpdateListener {
            override fun onSafeZoneAdded(safeZone: SafeZone) {
                callCount++
            }

            override fun onSafeZoneRemoved(safeZoneId: String) {}
        }

        SafeZoneManager.addListener(listener)
        SafeZoneManager.addListener(listener)  // Add same listener again

        SafeZoneManager.addSafeZone(SafeZone("sz-001", "Test", 0.0, 0.0))

        // Should only be called once (no duplicates)
        assertEquals(1, callCount)

        SafeZoneManager.removeListener(listener)
    }

    @Test
    fun testThreadSafety() {
        // Test concurrent access (basic test - not exhaustive)
        val threads = List(10) { index ->
            Thread {
                SafeZoneManager.addSafeZone(
                    SafeZone(
                        id = "sz-$index",
                        name = "Zone $index",
                        latitude = index.toDouble(),
                        longitude = index.toDouble()
                    )
                )
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        val zones = SafeZoneManager.getAllSafeZones()
        assertEquals(10, zones.size)
    }
}
