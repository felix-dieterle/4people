package com.fourpeople.adhoc.simulation

/**
 * Represents a person in the simulation.
 *
 * @param id Unique identifier for this person
 * @param latitude Current latitude position
 * @param longitude Current longitude position
 * @param hasApp Whether this person has the app running
 * @param isMoving Whether this person is moving
 * @param hasReceivedEvent Whether this person has received the event notification
 * @param eventReceivedTime Timestamp when event was received (in simulation time)
 */
data class SimulationPerson(
    val id: String,
    var latitude: Double,
    var longitude: Double,
    val hasApp: Boolean,
    val isMoving: Boolean,
    var hasReceivedEvent: Boolean = false,
    var eventReceivedTime: Long = -1,
    var movementDirection: Double = Math.random() * 2 * Math.PI, // Random initial direction
    var movementSpeed: Double = 0.0 // meters per second
) {
    companion object {
        // Movement speeds in meters per second
        const val WALKING_SPEED = 1.4 // ~5 km/h
        const val STATIONARY_SPEED = 0.0
    }
    
    init {
        movementSpeed = if (isMoving) WALKING_SPEED else STATIONARY_SPEED
    }
}
