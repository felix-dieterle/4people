package com.fourpeople.adhoc.simulation

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for simulation data classes.
 */
class SimulationDataClassesTest {
    
    @Test
    fun testSimulationPersonCreation() {
        val person = SimulationPerson(
            id = "person_1",
            latitude = 52.5200,
            longitude = 13.4050,
            hasApp = true,
            isMoving = false
        )
        
        assertEquals("person_1", person.id)
        assertEquals(52.5200, person.latitude, 0.0001)
        assertEquals(13.4050, person.longitude, 0.0001)
        assertTrue(person.hasApp)
        assertFalse(person.isMoving)
        assertFalse(person.hasReceivedEvent)
        assertEquals(-1L, person.eventReceivedTime)
        assertEquals(0.0, person.movementSpeed, 0.0001)
    }
    
    @Test
    fun testSimulationPersonMoving() {
        val movingPerson = SimulationPerson(
            id = "person_2",
            latitude = 52.5200,
            longitude = 13.4050,
            hasApp = true,
            isMoving = true
        )
        
        assertTrue(movingPerson.isMoving)
        assertEquals(SimulationPerson.WALKING_SPEED, movingPerson.movementSpeed, 0.0001)
    }
    
    @Test
    fun testSimulationPersonEventReceived() {
        val person = SimulationPerson(
            id = "person_3",
            latitude = 52.5200,
            longitude = 13.4050,
            hasApp = true,
            isMoving = false
        )
        
        person.hasReceivedEvent = true
        person.eventReceivedTime = 5000L
        
        assertTrue(person.hasReceivedEvent)
        assertEquals(5000L, person.eventReceivedTime)
    }
    
    @Test
    fun testSimulationWiFiCreation() {
        val wifi = SimulationWiFi(
            id = "wifi_1",
            latitude = 52.5200,
            longitude = 13.4050
        )
        
        assertEquals("wifi_1", wifi.id)
        assertEquals(52.5200, wifi.latitude, 0.0001)
        assertEquals(13.4050, wifi.longitude, 0.0001)
        assertEquals(50.0, wifi.range, 0.0001)
    }
    
    @Test
    fun testSimulationWiFiCustomRange() {
        val wifi = SimulationWiFi(
            id = "wifi_2",
            latitude = 52.5200,
            longitude = 13.4050,
            range = 100.0
        )
        
        assertEquals(100.0, wifi.range, 0.0001)
    }
    
    @Test
    fun testSimulationEventCreation() {
        val event = SimulationEvent(
            id = "event_1",
            latitude = 52.5200,
            longitude = 13.4050,
            timestamp = 1000L
        )
        
        assertEquals("event_1", event.id)
        assertEquals(52.5200, event.latitude, 0.0001)
        assertEquals(13.4050, event.longitude, 0.0001)
        assertEquals(1000L, event.timestamp)
        assertEquals(100.0, event.detectionRadius, 0.0001)
    }
    
    @Test
    fun testSimulationEventCustomRadius() {
        val event = SimulationEvent(
            id = "event_2",
            latitude = 52.5200,
            longitude = 13.4050,
            timestamp = 2000L,
            detectionRadius = 200.0
        )
        
        assertEquals(200.0, event.detectionRadius, 0.0001)
    }
    
    @Test
    fun testSimulationStatisticsCreation() {
        val stats = SimulationStatistics(
            totalPeople = 100,
            peopleWithApp = 50,
            peopleInformed = 20,
            peopleUninformed = 30,
            wifiNetworks = 10,
            simulationTime = 5000L,
            eventOccurred = true
        )
        
        assertEquals(100, stats.totalPeople)
        assertEquals(50, stats.peopleWithApp)
        assertEquals(20, stats.peopleInformed)
        assertEquals(30, stats.peopleUninformed)
        assertEquals(10, stats.wifiNetworks)
        assertEquals(5000L, stats.simulationTime)
        assertTrue(stats.eventOccurred)
    }
    
    @Test
    fun testSimulationPersonMovementConstants() {
        assertEquals(1.4, SimulationPerson.WALKING_SPEED, 0.0001)
        assertEquals(0.0, SimulationPerson.STATIONARY_SPEED, 0.0001)
    }
}
