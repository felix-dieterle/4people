package com.fourpeople.adhoc.simulation

import kotlin.math.*
import kotlin.random.Random

/**
 * Engine that manages the simulation of emergency event propagation.
 * 
 * This engine simulates:
 * - People randomly distributed in an area (5-90% with app running)
 * - WiFi networks randomly distributed (mostly static)
 * - People moving with typical walking patterns
 * - Event occurring at a random position
 * - Message propagation through people within 100m
 */
class SimulationEngine(
    private val areaLatMin: Double,
    private val areaLatMax: Double,
    private val areaLonMin: Double,
    private val areaLonMax: Double,
    private val peopleCount: Int,
    private val appAdoptionRate: Double, // 0.05 to 0.90 (5% to 90%)
    private val movingPeopleRatio: Double = 0.3 // 30% of people are moving
) {
    
    companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0
        private const val MESSAGE_PROPAGATION_RADIUS = 100.0 // meters
        private const val WIFI_RANGE = 50.0 // meters
    }
    
    private val people = mutableListOf<SimulationPerson>()
    private val wifiNetworks = mutableListOf<SimulationWiFi>()
    private var event: SimulationEvent? = null
    private var simulationTime: Long = 0L // in milliseconds
    
    // Listeners for state changes
    private val stateChangeListeners = mutableListOf<StateChangeListener>()
    
    interface StateChangeListener {
        fun onSimulationUpdated(
            people: List<SimulationPerson>,
            wifiNetworks: List<SimulationWiFi>,
            event: SimulationEvent?,
            simulationTime: Long
        )
    }
    
    /**
     * Initialize the simulation with random people and WiFi networks.
     */
    fun initialize() {
        people.clear()
        wifiNetworks.clear()
        simulationTime = 0L
        
        // Create people
        for (i in 0 until peopleCount) {
            val lat = areaLatMin + Random.nextDouble() * (areaLatMax - areaLatMin)
            val lon = areaLonMin + Random.nextDouble() * (areaLonMax - areaLonMin)
            val hasApp = Random.nextDouble() < appAdoptionRate
            val isMoving = Random.nextDouble() < movingPeopleRatio
            
            people.add(
                SimulationPerson(
                    id = "person_$i",
                    latitude = lat,
                    longitude = lon,
                    hasApp = hasApp,
                    isMoving = isMoving
                )
            )
        }
        
        // Create WiFi networks (approximately 1 per 10 people)
        val wifiCount = max(1, peopleCount / 10)
        for (i in 0 until wifiCount) {
            val lat = areaLatMin + Random.nextDouble() * (areaLatMax - areaLatMin)
            val lon = areaLonMin + Random.nextDouble() * (areaLonMax - areaLonMin)
            
            wifiNetworks.add(
                SimulationWiFi(
                    id = "wifi_$i",
                    latitude = lat,
                    longitude = lon,
                    range = WIFI_RANGE
                )
            )
        }
        
        notifyListeners()
    }
    
    /**
     * Start an event at a random location.
     */
    fun startEvent() {
        val lat = areaLatMin + Random.nextDouble() * (areaLatMax - areaLatMin)
        val lon = areaLonMin + Random.nextDouble() * (areaLonMax - areaLonMin)
        
        event = SimulationEvent(
            id = "event_${System.currentTimeMillis()}",
            latitude = lat,
            longitude = lon,
            timestamp = simulationTime,
            detectionRadius = MESSAGE_PROPAGATION_RADIUS
        )
        
        // Check for immediate detections
        checkEventDetection()
        notifyListeners()
    }
    
    /**
     * Update the simulation by a time step.
     * 
     * @param deltaTimeMs Time step in milliseconds
     */
    fun update(deltaTimeMs: Long) {
        simulationTime += deltaTimeMs
        
        // Update positions of moving people
        updatePeoplePositions(deltaTimeMs)
        
        // Check for message propagation
        if (event != null) {
            propagateMessages()
        }
        
        notifyListeners()
    }
    
    /**
     * Update positions of people who are moving.
     */
    private fun updatePeoplePositions(deltaTimeMs: Long) {
        val deltaTimeSec = deltaTimeMs / 1000.0
        
        for (person in people) {
            if (!person.isMoving) continue
            
            // Calculate new position based on movement direction and speed
            val distanceMeters = person.movementSpeed * deltaTimeSec
            
            // Convert distance to lat/lon offset
            val deltaLat = (distanceMeters * cos(person.movementDirection)) / EARTH_RADIUS_METERS * (180.0 / PI)
            val deltaLon = (distanceMeters * sin(person.movementDirection)) / 
                          (EARTH_RADIUS_METERS * cos(person.latitude * PI / 180.0)) * (180.0 / PI)
            
            // Update position
            person.latitude += deltaLat
            person.longitude += deltaLon
            
            // Keep people within bounds
            if (person.latitude < areaLatMin || person.latitude > areaLatMax) {
                person.movementDirection = -person.movementDirection
                person.latitude = person.latitude.coerceIn(areaLatMin, areaLatMax)
            }
            if (person.longitude < areaLonMin || person.longitude > areaLonMax) {
                person.movementDirection = PI - person.movementDirection
                person.longitude = person.longitude.coerceIn(areaLonMin, areaLonMax)
            }
            
            // Randomly change direction occasionally (every ~10 seconds on average)
            if (Random.nextDouble() < deltaTimeSec / 10.0) {
                person.movementDirection += (Random.nextDouble() - 0.5) * PI / 2
            }
        }
    }
    
    /**
     * Check if people near the event have detected it.
     */
    private fun checkEventDetection() {
        val currentEvent = event ?: return
        
        for (person in people) {
            if (!person.hasApp || person.hasReceivedEvent) continue
            
            val distance = calculateDistance(
                person.latitude, person.longitude,
                currentEvent.latitude, currentEvent.longitude
            )
            
            if (distance <= currentEvent.detectionRadius) {
                person.hasReceivedEvent = true
                person.eventReceivedTime = simulationTime
            }
        }
    }
    
    /**
     * Propagate messages between people who have received the event.
     */
    private fun propagateMessages() {
        // Find all people who have received the event
        val informedPeople = people.filter { it.hasApp && it.hasReceivedEvent }
        val uninformedPeople = people.filter { it.hasApp && !it.hasReceivedEvent }
        
        // Check if uninformed people are near informed people
        for (uninformed in uninformedPeople) {
            for (informed in informedPeople) {
                val distance = calculateDistance(
                    uninformed.latitude, uninformed.longitude,
                    informed.latitude, informed.longitude
                )
                
                // Direct peer-to-peer propagation (Bluetooth/WiFi Direct range)
                if (distance <= MESSAGE_PROPAGATION_RADIUS) {
                    uninformed.hasReceivedEvent = true
                    uninformed.eventReceivedTime = simulationTime
                    break // Stop once informed
                }
            }
        }
        
        // Also check WiFi network propagation
        for (uninformed in uninformedPeople.filter { !it.hasReceivedEvent }) {
            // Check if person is in range of any WiFi network
            for (wifi in wifiNetworks) {
                val distanceToWifi = calculateDistance(
                    uninformed.latitude, uninformed.longitude,
                    wifi.latitude, wifi.longitude
                )
                
                if (distanceToWifi <= wifi.range) {
                    // Check if any informed person is also in range of this WiFi
                    for (informed in informedPeople) {
                        val informedDistanceToWifi = calculateDistance(
                            informed.latitude, informed.longitude,
                            wifi.latitude, wifi.longitude
                        )
                        
                        if (informedDistanceToWifi <= wifi.range) {
                            uninformed.hasReceivedEvent = true
                            uninformed.eventReceivedTime = simulationTime
                            break
                        }
                    }
                    if (uninformed.hasReceivedEvent) break
                }
            }
        }
    }
    
    /**
     * Calculate distance between two GPS coordinates in meters.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLon = (lon2 - lon1) * PI / 180.0
        
        val a = sin(dLat / 2).pow(2) + 
                cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) * 
                sin(dLon / 2).pow(2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return EARTH_RADIUS_METERS * c
    }
    
    /**
     * Add a listener for simulation state changes.
     */
    fun addStateChangeListener(listener: StateChangeListener) {
        stateChangeListeners.add(listener)
    }
    
    /**
     * Remove a listener for simulation state changes.
     */
    fun removeStateChangeListener(listener: StateChangeListener) {
        stateChangeListeners.remove(listener)
    }
    
    /**
     * Notify all listeners of state change.
     */
    private fun notifyListeners() {
        for (listener in stateChangeListeners) {
            listener.onSimulationUpdated(
                people = people.toList(),
                wifiNetworks = wifiNetworks.toList(),
                event = event,
                simulationTime = simulationTime
            )
        }
    }
    
    /**
     * Get current simulation statistics.
     */
    fun getStatistics(): SimulationStatistics {
        val totalWithApp = people.count { it.hasApp }
        val informed = people.count { it.hasReceivedEvent }
        val uninformed = totalWithApp - informed
        
        return SimulationStatistics(
            totalPeople = people.size,
            peopleWithApp = totalWithApp,
            peopleInformed = informed,
            peopleUninformed = uninformed,
            wifiNetworks = wifiNetworks.size,
            simulationTime = simulationTime,
            eventOccurred = event != null
        )
    }
    
    /**
     * Reset the simulation.
     */
    fun reset() {
        people.clear()
        wifiNetworks.clear()
        event = null
        simulationTime = 0L
        notifyListeners()
    }
}

/**
 * Statistics about the current simulation state.
 */
data class SimulationStatistics(
    val totalPeople: Int,
    val peopleWithApp: Int,
    val peopleInformed: Int,
    val peopleUninformed: Int,
    val wifiNetworks: Int,
    val simulationTime: Long,
    val eventOccurred: Boolean
)
