package com.fourpeople.adhoc.simulation

/**
 * Represents different infrastructure failure modes in emergency scenarios.
 */
enum class InfrastructureFailureMode {
    /** Only mobile data has failed, voice calls and SMS still work */
    MOBILE_DATA_ONLY,
    
    /** Data backbone has failed, no internet but local networks and phone calls work */
    DATA_BACKBONE,
    
    /** Complete failure including telephone networks */
    COMPLETE_FAILURE
}

/**
 * Represents the type of location for the simulation.
 */
enum class LocationType {
    /** Big city center - dense population, many buildings, WiFi networks */
    BIG_CITY_CENTER,
    
    /** Medium city - moderate density, mixed indoor/outdoor */
    MEDIUM_CITY,
    
    /** Village - sparse population, mostly outdoor, few WiFi networks */
    VILLAGE
}

/**
 * Predefined simulation scenario with typical parameters.
 * 
 * @param name Display name of the scenario
 * @param locationType Type of location (city, village, etc.)
 * @param peopleCount Total number of people in the area
 * @param appAdoptionRate Percentage of people with the app (0.0 - 1.0)
 * @param indoorRatio Ratio of people indoors vs outdoors (0.0 - 1.0)
 * @param wifiNetworkDensity Number of WiFi networks per 10 people
 * @param movingPeopleRatio Ratio of people moving (0.0 - 1.0)
 * @param infrastructureFailure Type of infrastructure failure
 * @param verbalTransmissionEnabled Enable verbal transmission for critical events
 * @param verbalTransmissionRadius Radius in meters for verbal transmission
 * @param approachingBehaviorEnabled Informed people approach nearby uninformed people
 * @param approachingRadius Radius in meters where informed people approach others
 */
data class SimulationScenario(
    val name: String,
    val locationType: LocationType,
    val peopleCount: Int,
    val appAdoptionRate: Double,
    val indoorRatio: Double,
    val wifiNetworkDensity: Double, // WiFi networks per 10 people
    val movingPeopleRatio: Double,
    val infrastructureFailure: InfrastructureFailureMode,
    val verbalTransmissionEnabled: Boolean = false,
    val verbalTransmissionRadius: Double = 20.0, // meters - shorter than digital
    val approachingBehaviorEnabled: Boolean = false,
    val approachingRadius: Double = 100.0 // meters - line of sight range
) {
    companion object {
        /**
         * Get all predefined scenarios.
         */
        fun getPredefinedScenarios(): List<SimulationScenario> = listOf(
            // Big City Center Scenarios
            SimulationScenario(
                name = "Stadtmitte Großstadt - Nur Mobile Daten ausgefallen",
                locationType = LocationType.BIG_CITY_CENTER,
                peopleCount = 150,
                appAdoptionRate = 0.60, // Higher adoption in big cities
                indoorRatio = 0.70, // Most people in buildings
                wifiNetworkDensity = 2.0, // Many WiFi networks
                movingPeopleRatio = 0.40, // Many people moving
                infrastructureFailure = InfrastructureFailureMode.MOBILE_DATA_ONLY,
                verbalTransmissionEnabled = false, // Not critical yet
                approachingBehaviorEnabled = false
            ),
            
            SimulationScenario(
                name = "Stadtmitte Großstadt - Daten Backbone ausgefallen",
                locationType = LocationType.BIG_CITY_CENTER,
                peopleCount = 150,
                appAdoptionRate = 0.60,
                indoorRatio = 0.70,
                wifiNetworkDensity = 2.0,
                movingPeopleRatio = 0.40,
                infrastructureFailure = InfrastructureFailureMode.DATA_BACKBONE,
                verbalTransmissionEnabled = true, // More critical
                verbalTransmissionRadius = 15.0, // Indoor reduces range
                approachingBehaviorEnabled = true,
                approachingRadius = 50.0 // Limited by buildings
            ),
            
            SimulationScenario(
                name = "Stadtmitte Großstadt - Telefon auch ausgefallen",
                locationType = LocationType.BIG_CITY_CENTER,
                peopleCount = 150,
                appAdoptionRate = 0.60,
                indoorRatio = 0.70,
                wifiNetworkDensity = 2.0,
                movingPeopleRatio = 0.50, // More people moving in panic
                infrastructureFailure = InfrastructureFailureMode.COMPLETE_FAILURE,
                verbalTransmissionEnabled = true, // Very critical
                verbalTransmissionRadius = 15.0,
                approachingBehaviorEnabled = true,
                approachingRadius = 50.0
            ),
            
            // Medium City Scenarios
            SimulationScenario(
                name = "Stadt - Nur Mobile Daten ausgefallen",
                locationType = LocationType.MEDIUM_CITY,
                peopleCount = 80,
                appAdoptionRate = 0.45,
                indoorRatio = 0.50, // Mixed indoor/outdoor
                wifiNetworkDensity = 1.5,
                movingPeopleRatio = 0.30,
                infrastructureFailure = InfrastructureFailureMode.MOBILE_DATA_ONLY,
                verbalTransmissionEnabled = false,
                approachingBehaviorEnabled = false
            ),
            
            SimulationScenario(
                name = "Stadt - Daten Backbone ausgefallen",
                locationType = LocationType.MEDIUM_CITY,
                peopleCount = 80,
                appAdoptionRate = 0.45,
                indoorRatio = 0.50,
                wifiNetworkDensity = 1.5,
                movingPeopleRatio = 0.35,
                infrastructureFailure = InfrastructureFailureMode.DATA_BACKBONE,
                verbalTransmissionEnabled = true,
                verbalTransmissionRadius = 20.0, // More outdoor space
                approachingBehaviorEnabled = true,
                approachingRadius = 75.0
            ),
            
            SimulationScenario(
                name = "Stadt - Telefon auch ausgefallen",
                locationType = LocationType.MEDIUM_CITY,
                peopleCount = 80,
                appAdoptionRate = 0.45,
                indoorRatio = 0.50,
                wifiNetworkDensity = 1.5,
                movingPeopleRatio = 0.40,
                infrastructureFailure = InfrastructureFailureMode.COMPLETE_FAILURE,
                verbalTransmissionEnabled = true,
                verbalTransmissionRadius = 20.0,
                approachingBehaviorEnabled = true,
                approachingRadius = 75.0
            ),
            
            // Village Scenarios
            SimulationScenario(
                name = "Dorf - Nur Mobile Daten ausgefallen",
                locationType = LocationType.VILLAGE,
                peopleCount = 40,
                appAdoptionRate = 0.30, // Lower adoption in rural areas
                indoorRatio = 0.30, // Most people outdoor
                wifiNetworkDensity = 0.8, // Fewer WiFi networks
                movingPeopleRatio = 0.20,
                infrastructureFailure = InfrastructureFailureMode.MOBILE_DATA_ONLY,
                verbalTransmissionEnabled = false,
                approachingBehaviorEnabled = false
            ),
            
            SimulationScenario(
                name = "Dorf - Daten Backbone ausgefallen",
                locationType = LocationType.VILLAGE,
                peopleCount = 40,
                appAdoptionRate = 0.30,
                indoorRatio = 0.30,
                wifiNetworkDensity = 0.8,
                movingPeopleRatio = 0.25,
                infrastructureFailure = InfrastructureFailureMode.DATA_BACKBONE,
                verbalTransmissionEnabled = true,
                verbalTransmissionRadius = 30.0, // Open space, better range
                approachingBehaviorEnabled = true,
                approachingRadius = 150.0 // Better line of sight
            ),
            
            SimulationScenario(
                name = "Dorf - Telefon auch ausgefallen",
                locationType = LocationType.VILLAGE,
                peopleCount = 40,
                appAdoptionRate = 0.30,
                indoorRatio = 0.30,
                wifiNetworkDensity = 0.8,
                movingPeopleRatio = 0.30,
                infrastructureFailure = InfrastructureFailureMode.COMPLETE_FAILURE,
                verbalTransmissionEnabled = true,
                verbalTransmissionRadius = 30.0,
                approachingBehaviorEnabled = true,
                approachingRadius = 150.0
            )
        )
        
        /**
         * Get scenario names for display in UI.
         */
        fun getScenarioNames(): Array<String> {
            return getPredefinedScenarios().map { it.name }.toTypedArray()
        }
        
        /**
         * Get scenario by index.
         */
        fun getScenario(index: Int): SimulationScenario? {
            val scenarios = getPredefinedScenarios()
            return scenarios.getOrNull(index)
        }
    }
}
