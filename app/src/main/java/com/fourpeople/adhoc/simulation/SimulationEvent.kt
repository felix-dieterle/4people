package com.fourpeople.adhoc.simulation

/**
 * Represents an emergency event in the simulation.
 *
 * @param id Unique identifier for this event
 * @param latitude Latitude position where event occurred
 * @param longitude Longitude position where event occurred
 * @param timestamp Simulation time when event occurred
 * @param detectionRadius Radius in meters within which people can detect the event
 */
data class SimulationEvent(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val detectionRadius: Double = 100.0 // 100 meters as specified
)
