package com.fourpeople.adhoc.simulation

/**
 * Represents a WiFi access point in the simulation.
 *
 * @param id Unique identifier for this WiFi AP
 * @param latitude Latitude position of the WiFi AP
 * @param longitude Longitude position of the WiFi AP
 * @param range Transmission range in meters
 */
data class SimulationWiFi(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val range: Double = 50.0 // Default WiFi range in meters
)
