package com.fourpeople.adhoc.location

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for SafeZone data class.
 */
class SafeZoneTest {

    @Test
    fun testSafeZoneCreation() {
        val safeZone = SafeZone(
            id = "sz-001",
            name = "City Hall",
            latitude = 52.520008,
            longitude = 13.404954,
            description = "Main gathering point",
            capacity = 500
        )

        assertEquals("sz-001", safeZone.id)
        assertEquals("City Hall", safeZone.name)
        assertEquals(52.520008, safeZone.latitude, 0.000001)
        assertEquals(13.404954, safeZone.longitude, 0.000001)
        assertEquals("Main gathering point", safeZone.description)
        assertEquals(500, safeZone.capacity)
    }

    @Test
    fun testSafeZoneDefaultValues() {
        val safeZone = SafeZone(
            id = "sz-002",
            name = "Park",
            latitude = 48.1351,
            longitude = 11.5820
        )

        assertNull(safeZone.description)
        assertEquals(0, safeZone.capacity)
    }

    @Test
    fun testSafeZoneValidityFresh() {
        val safeZone = SafeZone(
            id = "sz-003",
            name = "School",
            latitude = 51.5074,
            longitude = -0.1278,
            timestamp = System.currentTimeMillis()
        )

        assertTrue(safeZone.isValid())
    }

    @Test
    fun testSafeZoneValidityStale() {
        // Create a safe zone older than 24 hours
        val oneDayAndOneHourAgo = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        val safeZone = SafeZone(
            id = "sz-004",
            name = "Old Location",
            latitude = 40.7128,
            longitude = -74.0060,
            timestamp = oneDayAndOneHourAgo
        )

        assertFalse(safeZone.isValid())
    }

    @Test
    fun testSafeZoneValidityBoundary() {
        // Create a safe zone at exactly 24 hours ago
        val exactlyOneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        val safeZone = SafeZone(
            id = "sz-005",
            name = "Boundary Test",
            latitude = 35.6762,
            longitude = 139.6503,
            timestamp = exactlyOneDayAgo
        )

        // Should still be valid at exactly 24 hours
        assertTrue(safeZone.isValid())
    }

    @Test
    fun testSafeZoneValidityJustAfterBoundary() {
        // Create a safe zone just over 24 hours ago
        val justOver24Hours = System.currentTimeMillis() - (24 * 60 * 60 * 1000L + 1000L)
        val safeZone = SafeZone(
            id = "sz-006",
            name = "Just Over",
            latitude = 37.7749,
            longitude = -122.4194,
            timestamp = justOver24Hours
        )

        assertFalse(safeZone.isValid())
    }

    @Test
    fun testSafeZoneUnlimitedCapacity() {
        val safeZone = SafeZone(
            id = "sz-007",
            name = "Large Open Area",
            latitude = 48.8566,
            longitude = 2.3522,
            capacity = 0  // 0 means unlimited
        )

        assertEquals(0, safeZone.capacity)
    }

    @Test
    fun testSafeZoneSerializable() {
        val safeZone = SafeZone(
            id = "sz-008",
            name = "Test Zone",
            latitude = 41.9028,
            longitude = 12.4964,
            description = "Test description",
            capacity = 100
        )

        // Verify it's serializable (implements Serializable interface)
        assertTrue(safeZone is java.io.Serializable)
    }

    @Test
    fun testSafeZoneImmutability() {
        val safeZone = SafeZone(
            id = "sz-009",
            name = "Immutable Test",
            latitude = 55.7558,
            longitude = 37.6173,
            description = "Original description",
            capacity = 200
        )

        // Data class is immutable - cannot change values
        // Can only create new instances with copy()
        val modified = safeZone.copy(name = "Modified Name")
        
        assertEquals("Immutable Test", safeZone.name)
        assertEquals("Modified Name", modified.name)
        assertNotEquals(safeZone, modified)
    }

    @Test
    fun testSafeZoneEquality() {
        val safeZone1 = SafeZone(
            id = "sz-010",
            name = "Equality Test",
            latitude = 52.520008,
            longitude = 13.404954,
            description = "Test",
            capacity = 100,
            timestamp = 1234567890L
        )

        val safeZone2 = SafeZone(
            id = "sz-010",
            name = "Equality Test",
            latitude = 52.520008,
            longitude = 13.404954,
            description = "Test",
            capacity = 100,
            timestamp = 1234567890L
        )

        // Data classes with same values should be equal
        assertEquals(safeZone1, safeZone2)
        assertEquals(safeZone1.hashCode(), safeZone2.hashCode())
    }

    @Test
    fun testSafeZoneInequality() {
        val safeZone1 = SafeZone(
            id = "sz-011",
            name = "Zone A",
            latitude = 52.520008,
            longitude = 13.404954
        )

        val safeZone2 = SafeZone(
            id = "sz-012",
            name = "Zone B",
            latitude = 52.520008,
            longitude = 13.404954
        )

        // Different IDs should make them unequal
        assertNotEquals(safeZone1, safeZone2)
    }

    @Test
    fun testSafeZoneToString() {
        val safeZone = SafeZone(
            id = "sz-013",
            name = "ToString Test",
            latitude = 52.520008,
            longitude = 13.404954,
            description = "Test description",
            capacity = 250
        )

        val string = safeZone.toString()
        
        // Data class toString should contain all properties
        assertTrue(string.contains("sz-013"))
        assertTrue(string.contains("ToString Test"))
        assertTrue(string.contains("52.520008"))
        assertTrue(string.contains("13.404954"))
    }

    @Test
    fun testSafeZoneCopy() {
        val original = SafeZone(
            id = "sz-014",
            name = "Original",
            latitude = 52.520008,
            longitude = 13.404954,
            description = "Original description",
            capacity = 300
        )

        val copied = original.copy(capacity = 500)

        assertEquals("sz-014", copied.id)
        assertEquals("Original", copied.name)
        assertEquals(52.520008, copied.latitude, 0.000001)
        assertEquals(13.404954, copied.longitude, 0.000001)
        assertEquals("Original description", copied.description)
        assertEquals(500, copied.capacity)
    }

    @Test
    fun testSafeZoneNegativeCoordinates() {
        val safeZone = SafeZone(
            id = "sz-015",
            name = "Southern Hemisphere",
            latitude = -33.8688,
            longitude = 151.2093,
            description = "Sydney"
        )

        assertEquals(-33.8688, safeZone.latitude, 0.000001)
        assertEquals(151.2093, safeZone.longitude, 0.000001)
    }

    @Test
    fun testSafeZoneNullDescription() {
        val safeZone = SafeZone(
            id = "sz-016",
            name = "No Description",
            latitude = 0.0,
            longitude = 0.0,
            description = null
        )

        assertNull(safeZone.description)
    }

    @Test
    fun testSafeZoneEmptyDescription() {
        val safeZone = SafeZone(
            id = "sz-017",
            name = "Empty Description",
            latitude = 0.0,
            longitude = 0.0,
            description = ""
        )

        assertEquals("", safeZone.description)
    }
}
