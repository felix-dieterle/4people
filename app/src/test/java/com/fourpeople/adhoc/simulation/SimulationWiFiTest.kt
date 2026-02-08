package com.fourpeople.adhoc.simulation

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SimulationWiFi data class.
 */
class SimulationWiFiTest {

    @Test
    fun testSimulationWiFiCreation() {
        val wifi = SimulationWiFi(
            id = "wifi-001",
            latitude = 52.520008,
            longitude = 13.404954,
            range = 50.0
        )

        assertEquals("wifi-001", wifi.id)
        assertEquals(52.520008, wifi.latitude, 0.000001)
        assertEquals(13.404954, wifi.longitude, 0.000001)
        assertEquals(50.0, wifi.range, 0.001)
    }

    @Test
    fun testSimulationWiFiDefaultRange() {
        val wifi = SimulationWiFi(
            id = "wifi-002",
            latitude = 48.1351,
            longitude = 11.5820
        )

        assertEquals(50.0, wifi.range, 0.001)
    }

    @Test
    fun testSimulationWiFiCustomRange() {
        val wifi = SimulationWiFi(
            id = "wifi-003",
            latitude = 51.5074,
            longitude = -0.1278,
            range = 100.0
        )

        assertEquals(100.0, wifi.range, 0.001)
    }

    @Test
    fun testSimulationWiFiNegativeCoordinates() {
        val wifi = SimulationWiFi(
            id = "wifi-004",
            latitude = -33.8688,
            longitude = 151.2093,
            range = 75.0
        )

        assertEquals(-33.8688, wifi.latitude, 0.000001)
        assertEquals(151.2093, wifi.longitude, 0.000001)
    }

    @Test
    fun testSimulationWiFiEquality() {
        val wifi1 = SimulationWiFi(
            id = "wifi-005",
            latitude = 40.7128,
            longitude = -74.0060,
            range = 60.0
        )

        val wifi2 = SimulationWiFi(
            id = "wifi-005",
            latitude = 40.7128,
            longitude = -74.0060,
            range = 60.0
        )

        assertEquals(wifi1, wifi2)
        assertEquals(wifi1.hashCode(), wifi2.hashCode())
    }

    @Test
    fun testSimulationWiFiInequality() {
        val wifi1 = SimulationWiFi(
            id = "wifi-006",
            latitude = 40.7128,
            longitude = -74.0060,
            range = 60.0
        )

        val wifi2 = SimulationWiFi(
            id = "wifi-007",
            latitude = 40.7128,
            longitude = -74.0060,
            range = 60.0
        )

        assertNotEquals(wifi1, wifi2)
    }

    @Test
    fun testSimulationWiFiDifferentLocations() {
        val wifi1 = SimulationWiFi(
            id = "wifi-008",
            latitude = 52.520008,
            longitude = 13.404954,
            range = 50.0
        )

        val wifi2 = SimulationWiFi(
            id = "wifi-008",
            latitude = 48.1351,
            longitude = 11.5820,
            range = 50.0
        )

        assertNotEquals(wifi1, wifi2)
    }

    @Test
    fun testSimulationWiFiDifferentRanges() {
        val wifi1 = SimulationWiFi(
            id = "wifi-009",
            latitude = 52.520008,
            longitude = 13.404954,
            range = 50.0
        )

        val wifi2 = SimulationWiFi(
            id = "wifi-009",
            latitude = 52.520008,
            longitude = 13.404954,
            range = 100.0
        )

        assertNotEquals(wifi1, wifi2)
    }

    @Test
    fun testSimulationWiFiCopy() {
        val original = SimulationWiFi(
            id = "wifi-010",
            latitude = 37.7749,
            longitude = -122.4194,
            range = 75.0
        )

        val copied = original.copy(range = 100.0)

        assertEquals("wifi-010", copied.id)
        assertEquals(37.7749, copied.latitude, 0.000001)
        assertEquals(-122.4194, copied.longitude, 0.000001)
        assertEquals(100.0, copied.range, 0.001)
    }

    @Test
    fun testSimulationWiFiToString() {
        val wifi = SimulationWiFi(
            id = "wifi-011",
            latitude = 48.8566,
            longitude = 2.3522,
            range = 60.0
        )

        val string = wifi.toString()

        assertTrue(string.contains("wifi-011"))
        assertTrue(string.contains("48.8566"))
        assertTrue(string.contains("2.3522"))
        assertTrue(string.contains("60.0"))
    }

    @Test
    fun testSimulationWiFiShortRange() {
        val wifi = SimulationWiFi(
            id = "wifi-012",
            latitude = 0.0,
            longitude = 0.0,
            range = 10.0
        )

        assertEquals(10.0, wifi.range, 0.001)
    }

    @Test
    fun testSimulationWiFiLongRange() {
        val wifi = SimulationWiFi(
            id = "wifi-013",
            latitude = 0.0,
            longitude = 0.0,
            range = 200.0
        )

        assertEquals(200.0, wifi.range, 0.001)
    }

    @Test
    fun testSimulationWiFiZeroRange() {
        val wifi = SimulationWiFi(
            id = "wifi-014",
            latitude = 0.0,
            longitude = 0.0,
            range = 0.0
        )

        assertEquals(0.0, wifi.range, 0.001)
    }

    @Test
    fun testSimulationWiFiSpecialCharactersId() {
        val wifi = SimulationWiFi(
            id = "wifi-special-@#$%",
            latitude = 0.0,
            longitude = 0.0
        )

        assertEquals("wifi-special-@#$%", wifi.id)
    }

    @Test
    fun testSimulationWiFiEmptyId() {
        val wifi = SimulationWiFi(
            id = "",
            latitude = 0.0,
            longitude = 0.0
        )

        assertEquals("", wifi.id)
    }

    @Test
    fun testSimulationWiFiMaxCoordinates() {
        val wifi = SimulationWiFi(
            id = "wifi-max",
            latitude = 90.0,  // Max latitude
            longitude = 180.0  // Max longitude
        )

        assertEquals(90.0, wifi.latitude, 0.000001)
        assertEquals(180.0, wifi.longitude, 0.000001)
    }

    @Test
    fun testSimulationWiFiMinCoordinates() {
        val wifi = SimulationWiFi(
            id = "wifi-min",
            latitude = -90.0,  // Min latitude
            longitude = -180.0  // Min longitude
        )

        assertEquals(-90.0, wifi.latitude, 0.000001)
        assertEquals(-180.0, wifi.longitude, 0.000001)
    }

    @Test
    fun testSimulationWiFiPreciseCoordinates() {
        val wifi = SimulationWiFi(
            id = "wifi-precise",
            latitude = 52.5200081234,
            longitude = 13.4049541234
        )

        assertEquals(52.5200081234, wifi.latitude, 0.0000000001)
        assertEquals(13.4049541234, wifi.longitude, 0.0000000001)
    }
}
