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
 * - Verbal transmission in critical scenarios
 * - Approaching behavior where informed people seek out uninformed people
 */
class SimulationEngine(
    private val areaLatMin: Double,
    private val areaLatMax: Double,
    private val areaLonMin: Double,
    private val areaLonMax: Double,
    private val peopleCount: Int,
    private val appAdoptionRate: Double, // 0.05 to 0.90 (5% to 90%)
    private val movingPeopleRatio: Double = 0.3, // 30% of people are moving
    private val indoorRatio: Double = 0.0, // Ratio of people indoors
    private val wifiNetworkDensity: Double = 1.0, // WiFi networks per 10 people
    private val verbalTransmissionEnabled: Boolean = false,
    private val verbalTransmissionRadius: Double = 20.0, // meters
    private val approachingBehaviorEnabled: Boolean = false,
    private val approachingRadius: Double = 100.0, // meters
    private val infrastructureFailure: InfrastructureFailureMode = InfrastructureFailureMode.MOBILE_DATA_ONLY
) {
    
    /**
     * Secondary constructor for scenario-based initialization.
     */
    constructor(
        areaLatMin: Double,
        areaLatMax: Double,
        areaLonMin: Double,
        areaLonMax: Double,
        scenario: SimulationScenario
    ) : this(
        areaLatMin = areaLatMin,
        areaLatMax = areaLatMax,
        areaLonMin = areaLonMin,
        areaLonMax = areaLonMax,
        peopleCount = scenario.peopleCount,
        appAdoptionRate = scenario.appAdoptionRate,
        movingPeopleRatio = scenario.movingPeopleRatio,
        indoorRatio = scenario.indoorRatio,
        wifiNetworkDensity = scenario.wifiNetworkDensity,
        verbalTransmissionEnabled = scenario.verbalTransmissionEnabled,
        verbalTransmissionRadius = scenario.verbalTransmissionRadius,
        approachingBehaviorEnabled = scenario.approachingBehaviorEnabled,
        approachingRadius = scenario.approachingRadius,
        infrastructureFailure = scenario.infrastructureFailure
    )
    
    companion object {
        private const val EARTH_RADIUS_METERS = 6371000.0
        private const val MESSAGE_PROPAGATION_RADIUS = 100.0 // meters
        private const val WIFI_RANGE = 50.0 // meters
        private const val INDOOR_SIGNAL_ATTENUATION = 0.6 // Indoor reduces range by 40%
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
            val isIndoor = Random.nextDouble() < indoorRatio
            
            people.add(
                SimulationPerson(
                    id = "person_$i",
                    latitude = lat,
                    longitude = lon,
                    hasApp = hasApp,
                    isMoving = isMoving,
                    isIndoor = isIndoor
                )
            )
        }
        
        // Create WiFi networks based on density
        val wifiCount = max(1, (peopleCount * wifiNetworkDensity / 10.0).toInt())
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
        
        // Update approaching behavior
        if (approachingBehaviorEnabled && event != null) {
            updateApproachingBehavior()
        }
        
        // Update positions of moving people
        updatePeoplePositions(deltaTimeMs)
        
        // Check for message propagation
        if (event != null) {
            propagateMessages()
            
            // Verbal transmission for critical scenarios
            if (verbalTransmissionEnabled) {
                propagateVerbalMessages()
            }
        }
        
        notifyListeners()
    }
    
    /**
     * Update positions of people who are moving.
     */
    private fun updatePeoplePositions(deltaTimeMs: Long) {
        val deltaTimeSec = deltaTimeMs / 1000.0
        
        for (person in people) {
            // Move both normally moving people and those approaching others
            if (!person.isMoving && !person.isApproaching) continue
            
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
            
            // Only change direction randomly if not approaching someone
            if (!person.isApproaching && Random.nextDouble() < deltaTimeSec / 10.0) {
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
        // When someone with WiFi gets notified, ALL others in the same WiFi network
        // should be notified immediately (instant propagation)
        for (wifi in wifiNetworks) {
            // First, check if any informed person is in range of this WiFi
            val hasInformedInRange = informedPeople.any { informed ->
                val distance = calculateDistance(
                    informed.latitude, informed.longitude,
                    wifi.latitude, wifi.longitude
                )
                distance <= wifi.range
            }
            
            // If yes, immediately notify ALL uninformed people in range of this WiFi
            if (hasInformedInRange) {
                for (uninformed in uninformedPeople) {
                    val distanceToWifi = calculateDistance(
                        uninformed.latitude, uninformed.longitude,
                        wifi.latitude, wifi.longitude
                    )
                    
                    if (distanceToWifi <= wifi.range) {
                        uninformed.hasReceivedEvent = true
                        uninformed.eventReceivedTime = simulationTime
                    }
                }
            }
        }
    }
    
    /**
     * Update approaching behavior for informed people.
     * Informed people will approach nearby uninformed people to inform them verbally.
     */
    private fun updateApproachingBehavior() {
        // Cache informed and uninformed people to avoid creating new lists
        val informedPeople = people.asSequence().filter { it.hasApp && it.hasReceivedEvent }
        val uninformedPeople = people.asSequence().filter { it.hasApp && !it.hasReceivedEvent }.toList()
        
        for (informed in informedPeople) {
            // Check if already approaching someone
            if (informed.isApproaching && informed.targetPerson != null) {
                // Check if target was already informed
                if (informed.targetPerson?.hasReceivedEvent == true) {
                    // Find new target
                    informed.isApproaching = false
                    informed.targetPerson = null
                    informed.movementSpeed = if (informed.isMoving) SimulationPerson.WALKING_SPEED else SimulationPerson.STATIONARY_SPEED
                }
                continue
            }
            
            // Look for nearby uninformed people within approaching radius
            for (uninformed in uninformedPeople) {
                if (uninformed.hasReceivedEvent) continue
                
                val distance = calculateDistance(
                    informed.latitude, informed.longitude,
                    uninformed.latitude, uninformed.longitude
                )
                
                // Check if within approaching radius and line of sight (not indoors if target is indoors)
                if (distance <= approachingRadius) {
                    // Start approaching this person
                    informed.isApproaching = true
                    informed.targetPerson = uninformed
                    informed.movementSpeed = SimulationPerson.APPROACHING_SPEED
                    
                    // Calculate direction to target (angle from north)
                    val dLat = uninformed.latitude - informed.latitude
                    val dLon = uninformed.longitude - informed.longitude
                    informed.movementDirection = atan2(dLon, dLat)
                    break
                }
            }
        }
    }
    
    /**
     * Propagate messages via verbal transmission.
     * This simulates people telling others about the event verbally.
     * 
     * Note: Verbal transmission does not require the listener to have the app,
     * simulating real-world verbal communication during emergencies.
     */
    private fun propagateVerbalMessages() {
        // Use sequences to avoid creating intermediate lists
        val informedPeople = people.asSequence().filter { it.hasReceivedEvent }
        val uninformedPeople = people.asSequence().filter { !it.hasReceivedEvent }.toList()
        
        for (uninformed in uninformedPeople) {
            for (informed in informedPeople) {
                val distance = calculateDistance(
                    uninformed.latitude, uninformed.longitude,
                    informed.latitude, informed.longitude
                )
                
                // Apply range modifiers based on indoor/outdoor status
                var effectiveRange = verbalTransmissionRadius
                
                // Both indoors or one indoor reduces range
                if (uninformed.isIndoor || informed.isIndoor) {
                    effectiveRange *= INDOOR_SIGNAL_ATTENUATION
                }
                
                // Verbal transmission has shorter range than digital
                if (distance <= effectiveRange) {
                    uninformed.hasReceivedEvent = true
                    uninformed.eventReceivedTime = simulationTime
                    uninformed.receivedViaVerbal = true
                    break
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
            eventOccurred = event != null,
            infrastructureFailure = infrastructureFailure,
            smsAvailable = isSmsAvailable()
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
    
    /**
     * Check if SMS is available based on current infrastructure failure mode.
     * 
     * SMS requires cellular signaling network (MAP/SS7 protocol):
     * - ✅ Available when only mobile data fails (cellular signaling works)
     * - ✅ Available when data backbone fails (cellular signaling works)
     * - ❌ NOT available when cellular network completely fails
     * 
     * Note: SMS uses the cellular signaling channel, not the voice channel,
     * but they typically fail together in infrastructure failures.
     * 
     * @return true if SMS can be sent in current infrastructure state
     */
    fun isSmsAvailable(): Boolean {
        return SimulationStatistics.isSmsAvailableForMode(infrastructureFailure)
    }
    
    /**
     * Get the current infrastructure failure mode.
     */
    fun getInfrastructureFailureMode(): InfrastructureFailureMode {
        return infrastructureFailure
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
    val eventOccurred: Boolean,
    val infrastructureFailure: InfrastructureFailureMode,
    val smsAvailable: Boolean
) {
    companion object {
        /**
         * Compute SMS availability based on infrastructure failure mode.
         * 
         * This is the single source of truth for SMS availability logic.
         * SMS uses cellular signaling network (MAP/SS7), which typically
         * remains available when cellular infrastructure is operational.
         * 
         * @param mode The infrastructure failure mode
         * @return true if SMS is available in this mode
         */
        fun isSmsAvailableForMode(mode: InfrastructureFailureMode): Boolean {
            return when (mode) {
                InfrastructureFailureMode.MOBILE_DATA_ONLY -> true  // Cellular signaling works
                InfrastructureFailureMode.DATA_BACKBONE -> true     // Cellular signaling works
                InfrastructureFailureMode.COMPLETE_FAILURE -> false // No cellular network
            }
        }
    }
}
