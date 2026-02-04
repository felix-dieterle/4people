package com.fourpeople.adhoc.simulation

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SimulationPerson data class.
 */
class SimulationPersonTest {

    @Test
    fun testSimulationPersonCreation() {
        val person = SimulationPerson(
            id = "person-001",
            latitude = 52.520008,
            longitude = 13.404954,
            hasApp = true,
            isMoving = false,
            isIndoor = false
        )

        assertEquals("person-001", person.id)
        assertEquals(52.520008, person.latitude, 0.000001)
        assertEquals(13.404954, person.longitude, 0.000001)
        assertTrue(person.hasApp)
        assertFalse(person.isMoving)
        assertFalse(person.isIndoor)
        assertFalse(person.hasReceivedEvent)
        assertEquals(-1, person.eventReceivedTime)
        assertFalse(person.receivedViaVerbal)
        assertFalse(person.isApproaching)
        assertNull(person.targetPerson)
    }

    @Test
    fun testSimulationPersonMovementSpeeds() {
        assertEquals(1.4, SimulationPerson.WALKING_SPEED, 0.001)
        assertEquals(2.0, SimulationPerson.APPROACHING_SPEED, 0.001)
        assertEquals(0.0, SimulationPerson.STATIONARY_SPEED, 0.001)
    }

    @Test
    fun testSimulationPersonStationarySpeed() {
        val person = SimulationPerson(
            id = "person-002",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false
        )

        assertEquals(SimulationPerson.STATIONARY_SPEED, person.movementSpeed, 0.001)
    }

    @Test
    fun testSimulationPersonWalkingSpeed() {
        val person = SimulationPerson(
            id = "person-003",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = true
        )

        assertEquals(SimulationPerson.WALKING_SPEED, person.movementSpeed, 0.001)
    }

    @Test
    fun testSimulationPersonWithApp() {
        val person = SimulationPerson(
            id = "person-004",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false
        )

        assertTrue(person.hasApp)
    }

    @Test
    fun testSimulationPersonWithoutApp() {
        val person = SimulationPerson(
            id = "person-005",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = false,
            isMoving = false
        )

        assertFalse(person.hasApp)
    }

    @Test
    fun testSimulationPersonIndoor() {
        val person = SimulationPerson(
            id = "person-006",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false,
            isIndoor = true
        )

        assertTrue(person.isIndoor)
    }

    @Test
    fun testSimulationPersonOutdoor() {
        val person = SimulationPerson(
            id = "person-007",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false,
            isIndoor = false
        )

        assertFalse(person.isIndoor)
    }

    @Test
    fun testSimulationPersonReceiveEvent() {
        val person = SimulationPerson(
            id = "person-008",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false
        )

        assertFalse(person.hasReceivedEvent)

        person.hasReceivedEvent = true
        person.eventReceivedTime = 1000L

        assertTrue(person.hasReceivedEvent)
        assertEquals(1000L, person.eventReceivedTime)
    }

    @Test
    fun testSimulationPersonVerbalTransmission() {
        val person = SimulationPerson(
            id = "person-009",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = false,
            isMoving = false
        )

        assertFalse(person.receivedViaVerbal)

        person.receivedViaVerbal = true

        assertTrue(person.receivedViaVerbal)
    }

    @Test
    fun testSimulationPersonApproaching() {
        val target = SimulationPerson(
            id = "target",
            latitude = 10.0,
            longitude = 10.0,
            hasApp = false,
            isMoving = false
        )

        val person = SimulationPerson(
            id = "person-010",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = true
        )

        assertFalse(person.isApproaching)
        assertNull(person.targetPerson)

        person.isApproaching = true
        person.targetPerson = target

        assertTrue(person.isApproaching)
        assertNotNull(person.targetPerson)
        assertEquals("target", person.targetPerson?.id)
    }

    @Test
    fun testSimulationPersonMovementDirection() {
        val person = SimulationPerson(
            id = "person-011",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = true
        )

        // Movement direction should be initialized to a valid range [0, 2π)
        assertTrue(person.movementDirection >= 0.0)
        assertTrue(person.movementDirection < 2 * Math.PI)
    }

    @Test
    fun testSimulationPersonUpdatePosition() {
        val person = SimulationPerson(
            id = "person-012",
            latitude = 52.520008,
            longitude = 13.404954,
            hasApp = true,
            isMoving = false
        )

        person.latitude = 52.525008
        person.longitude = 13.409954

        assertEquals(52.525008, person.latitude, 0.000001)
        assertEquals(13.409954, person.longitude, 0.000001)
    }

    @Test
    fun testSimulationPersonCustomMovementDirection() {
        val person = SimulationPerson(
            id = "person-013",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = true,
            movementDirection = Math.PI / 2  // 90 degrees
        )

        assertEquals(Math.PI / 2, person.movementDirection, 0.001)
    }

    @Test
    fun testSimulationPersonMovementSpeedSetByMoving() {
        // When isMoving=true, movementSpeed is automatically set to WALKING_SPEED
        val movingPerson = SimulationPerson(
            id = "person-014",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = true
        )

        assertEquals(SimulationPerson.WALKING_SPEED, movingPerson.movementSpeed, 0.001)
        
        // When isMoving=false, movementSpeed is automatically set to STATIONARY_SPEED
        val stationaryPerson = SimulationPerson(
            id = "person-015",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false
        )

        assertEquals(SimulationPerson.STATIONARY_SPEED, stationaryPerson.movementSpeed, 0.001)
    }

    @Test
    fun testSimulationPersonEquality() {
        val person1 = SimulationPerson(
            id = "person-015",
            latitude = 52.520008,
            longitude = 13.404954,
            hasApp = true,
            isMoving = false
        )

        val person2 = SimulationPerson(
            id = "person-015",
            latitude = 52.520008,
            longitude = 13.404954,
            hasApp = true,
            isMoving = false
        )

        // Note: Due to random movement direction, persons may not be equal
        // even with same id and position. This tests the data class structure.
        assertEquals(person1.id, person2.id)
        assertEquals(person1.latitude, person2.latitude, 0.000001)
        assertEquals(person1.longitude, person2.longitude, 0.000001)
    }

    @Test
    fun testSimulationPersonNegativeCoordinates() {
        val person = SimulationPerson(
            id = "person-016",
            latitude = -33.8688,
            longitude = 151.2093,
            hasApp = true,
            isMoving = false
        )

        assertEquals(-33.8688, person.latitude, 0.000001)
        assertEquals(151.2093, person.longitude, 0.000001)
    }

    @Test
    fun testSimulationPersonToString() {
        val person = SimulationPerson(
            id = "person-017",
            latitude = 52.520008,
            longitude = 13.404954,
            hasApp = true,
            isMoving = false
        )

        val string = person.toString()

        assertTrue(string.contains("person-017"))
        assertTrue(string.contains("52.520008"))
        assertTrue(string.contains("13.404954"))
    }

    @Test
    fun testSimulationPersonCopy() {
        val original = SimulationPerson(
            id = "person-018",
            latitude = 52.520008,
            longitude = 13.404954,
            hasApp = true,
            isMoving = false,
            isIndoor = false
        )

        val copied = original.copy(hasApp = false)

        assertEquals("person-018", copied.id)
        assertEquals(52.520008, copied.latitude, 0.000001)
        assertEquals(13.404954, copied.longitude, 0.000001)
        assertFalse(copied.hasApp)
        assertFalse(copied.isMoving)
    }

    @Test
    fun testSimulationPersonEventTimestampInitial() {
        val person = SimulationPerson(
            id = "person-019",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false
        )

        assertEquals(-1, person.eventReceivedTime)
    }

    @Test
    fun testSimulationPersonEventTimestampUpdate() {
        val person = SimulationPerson(
            id = "person-020",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false
        )

        val timestamp = System.currentTimeMillis()
        person.eventReceivedTime = timestamp

        assertEquals(timestamp, person.eventReceivedTime)
    }

    @Test
    fun testSimulationPersonMultipleStateChanges() {
        val person = SimulationPerson(
            id = "person-021",
            latitude = 0.0,
            longitude = 0.0,
            hasApp = true,
            isMoving = false
        )

        // Receive event
        person.hasReceivedEvent = true
        person.eventReceivedTime = 1000L

        // Start approaching
        val target = SimulationPerson(
            id = "target-2",
            latitude = 10.0,
            longitude = 10.0,
            hasApp = false,
            isMoving = false
        )
        person.isApproaching = true
        person.targetPerson = target
        person.movementSpeed = SimulationPerson.APPROACHING_SPEED

        // Verify all states
        assertTrue(person.hasReceivedEvent)
        assertEquals(1000L, person.eventReceivedTime)
        assertTrue(person.isApproaching)
        assertEquals("target-2", person.targetPerson?.id)
        assertEquals(SimulationPerson.APPROACHING_SPEED, person.movementSpeed, 0.001)
    }

    @Test
    fun testSimulationPersonAppAdoptionVariety() {
        // Test different app adoption scenarios
        val withApp = SimulationPerson("p1", 0.0, 0.0, hasApp = true, isMoving = false)
        val withoutApp = SimulationPerson("p2", 0.0, 0.0, hasApp = false, isMoving = false)

        assertTrue(withApp.hasApp)
        assertFalse(withoutApp.hasApp)
    }

    @Test
    fun testSimulationPersonLocationTypeVariety() {
        // Test different location scenarios
        val indoor = SimulationPerson("p1", 0.0, 0.0, true, false, isIndoor = true)
        val outdoor = SimulationPerson("p2", 0.0, 0.0, true, false, isIndoor = false)

        assertTrue(indoor.isIndoor)
        assertFalse(outdoor.isIndoor)
    }

    @Test
    fun testSimulationPersonMovementVariety() {
        // Test different movement scenarios
        val stationary = SimulationPerson("p1", 0.0, 0.0, true, isMoving = false)
        val moving = SimulationPerson("p2", 0.0, 0.0, true, isMoving = true)

        assertFalse(stationary.isMoving)
        assertTrue(moving.isMoving)
        assertEquals(SimulationPerson.STATIONARY_SPEED, stationary.movementSpeed, 0.001)
        assertEquals(SimulationPerson.WALKING_SPEED, moving.movementSpeed, 0.001)
    }
}
