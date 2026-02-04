package com.fourpeople.adhoc.simulation

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SimulationScenario and related data classes.
 */
class SimulationScenarioTest {

    @Test
    fun testInfrastructureFailureModes() {
        val modes = InfrastructureFailureMode.values()
        assertEquals(3, modes.size)
        
        assertTrue(modes.contains(InfrastructureFailureMode.MOBILE_DATA_ONLY))
        assertTrue(modes.contains(InfrastructureFailureMode.DATA_BACKBONE))
        assertTrue(modes.contains(InfrastructureFailureMode.COMPLETE_FAILURE))
    }

    @Test
    fun testLocationTypes() {
        val types = LocationType.values()
        assertEquals(3, types.size)
        
        assertTrue(types.contains(LocationType.BIG_CITY_CENTER))
        assertTrue(types.contains(LocationType.MEDIUM_CITY))
        assertTrue(types.contains(LocationType.VILLAGE))
    }

    @Test
    fun testSimulationScenarioCreation() {
        val scenario = SimulationScenario(
            name = "Test Scenario",
            locationType = LocationType.MEDIUM_CITY,
            peopleCount = 100,
            appAdoptionRate = 0.5,
            indoorRatio = 0.6,
            wifiNetworkDensity = 1.5,
            movingPeopleRatio = 0.3,
            infrastructureFailure = InfrastructureFailureMode.DATA_BACKBONE
        )

        assertEquals("Test Scenario", scenario.name)
        assertEquals(LocationType.MEDIUM_CITY, scenario.locationType)
        assertEquals(100, scenario.peopleCount)
        assertEquals(0.5, scenario.appAdoptionRate, 0.001)
        assertEquals(0.6, scenario.indoorRatio, 0.001)
        assertEquals(1.5, scenario.wifiNetworkDensity, 0.001)
        assertEquals(0.3, scenario.movingPeopleRatio, 0.001)
        assertEquals(InfrastructureFailureMode.DATA_BACKBONE, scenario.infrastructureFailure)
    }

    @Test
    fun testScenarioDefaultValues() {
        val scenario = SimulationScenario(
            name = "Default Test",
            locationType = LocationType.VILLAGE,
            peopleCount = 50,
            appAdoptionRate = 0.3,
            indoorRatio = 0.4,
            wifiNetworkDensity = 1.0,
            movingPeopleRatio = 0.2,
            infrastructureFailure = InfrastructureFailureMode.MOBILE_DATA_ONLY
        )

        assertFalse(scenario.verbalTransmissionEnabled)
        assertEquals(20.0, scenario.verbalTransmissionRadius, 0.001)
        assertFalse(scenario.approachingBehaviorEnabled)
        assertEquals(100.0, scenario.approachingRadius, 0.001)
    }

    @Test
    fun testScenarioWithVerbalTransmission() {
        val scenario = SimulationScenario(
            name = "Verbal Test",
            locationType = LocationType.BIG_CITY_CENTER,
            peopleCount = 150,
            appAdoptionRate = 0.6,
            indoorRatio = 0.7,
            wifiNetworkDensity = 2.0,
            movingPeopleRatio = 0.4,
            infrastructureFailure = InfrastructureFailureMode.COMPLETE_FAILURE,
            verbalTransmissionEnabled = true,
            verbalTransmissionRadius = 15.0
        )

        assertTrue(scenario.verbalTransmissionEnabled)
        assertEquals(15.0, scenario.verbalTransmissionRadius, 0.001)
    }

    @Test
    fun testScenarioWithApproachingBehavior() {
        val scenario = SimulationScenario(
            name = "Approaching Test",
            locationType = LocationType.VILLAGE,
            peopleCount = 40,
            appAdoptionRate = 0.3,
            indoorRatio = 0.3,
            wifiNetworkDensity = 0.8,
            movingPeopleRatio = 0.3,
            infrastructureFailure = InfrastructureFailureMode.COMPLETE_FAILURE,
            approachingBehaviorEnabled = true,
            approachingRadius = 150.0
        )

        assertTrue(scenario.approachingBehaviorEnabled)
        assertEquals(150.0, scenario.approachingRadius, 0.001)
    }

    @Test
    fun testGetPredefinedScenarios() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        
        // Should have 9 scenarios (3 location types × 3 failure modes)
        assertEquals(9, scenarios.size)
    }

    @Test
    fun testPredefinedScenariosCoverAllCombinations() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        
        // Check that all location types are represented
        val locationTypes = scenarios.map { it.locationType }.toSet()
        assertEquals(3, locationTypes.size)
        assertTrue(locationTypes.contains(LocationType.BIG_CITY_CENTER))
        assertTrue(locationTypes.contains(LocationType.MEDIUM_CITY))
        assertTrue(locationTypes.contains(LocationType.VILLAGE))
        
        // Check that all failure modes are represented
        val failureModes = scenarios.map { it.infrastructureFailure }.toSet()
        assertEquals(3, failureModes.size)
        assertTrue(failureModes.contains(InfrastructureFailureMode.MOBILE_DATA_ONLY))
        assertTrue(failureModes.contains(InfrastructureFailureMode.DATA_BACKBONE))
        assertTrue(failureModes.contains(InfrastructureFailureMode.COMPLETE_FAILURE))
    }

    @Test
    fun testGetScenarioNames() {
        val names = SimulationScenario.getScenarioNames()
        
        assertEquals(9, names.size)
        assertTrue(names.all { it.isNotEmpty() })
    }

    @Test
    fun testGetScenarioByIndex() {
        val scenario0 = SimulationScenario.getScenario(0)
        assertNotNull(scenario0)
        
        val scenario1 = SimulationScenario.getScenario(1)
        assertNotNull(scenario1)
        
        assertNotEquals(scenario0, scenario1)
    }

    @Test
    fun testGetScenarioByInvalidIndex() {
        val scenario = SimulationScenario.getScenario(999)
        assertNull(scenario)
    }

    @Test
    fun testGetScenarioByNegativeIndex() {
        val scenario = SimulationScenario.getScenario(-1)
        assertNull(scenario)
    }

    @Test
    fun testBigCityScenarios() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        val bigCityScenarios = scenarios.filter { it.locationType == LocationType.BIG_CITY_CENTER }
        
        assertEquals(3, bigCityScenarios.size)
        
        // Big city should have higher people count
        assertTrue(bigCityScenarios.all { it.peopleCount >= 100 })
        
        // Big city should have higher app adoption
        assertTrue(bigCityScenarios.all { it.appAdoptionRate >= 0.5 })
        
        // Big city should have higher indoor ratio
        assertTrue(bigCityScenarios.all { it.indoorRatio >= 0.6 })
        
        // Big city should have more WiFi networks
        assertTrue(bigCityScenarios.all { it.wifiNetworkDensity >= 1.5 })
    }

    @Test
    fun testVillageScenarios() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        val villageScenarios = scenarios.filter { it.locationType == LocationType.VILLAGE }
        
        assertEquals(3, villageScenarios.size)
        
        // Village should have lower people count
        assertTrue(villageScenarios.all { it.peopleCount <= 50 })
        
        // Village should have lower app adoption
        assertTrue(villageScenarios.all { it.appAdoptionRate <= 0.4 })
        
        // Village should have lower indoor ratio
        assertTrue(villageScenarios.all { it.indoorRatio <= 0.4 })
        
        // Village should have fewer WiFi networks
        assertTrue(villageScenarios.all { it.wifiNetworkDensity <= 1.0 })
    }

    @Test
    fun testMobileDataOnlyScenarios() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        val mobileDataOnlyScenarios = scenarios.filter { 
            it.infrastructureFailure == InfrastructureFailureMode.MOBILE_DATA_ONLY 
        }
        
        assertEquals(3, mobileDataOnlyScenarios.size)
        
        // Mobile data only scenarios should not enable verbal transmission
        assertTrue(mobileDataOnlyScenarios.all { !it.verbalTransmissionEnabled })
        
        // Mobile data only scenarios should not enable approaching behavior
        assertTrue(mobileDataOnlyScenarios.all { !it.approachingBehaviorEnabled })
    }

    @Test
    fun testCompleteFailureScenarios() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        val completeFailureScenarios = scenarios.filter { 
            it.infrastructureFailure == InfrastructureFailureMode.COMPLETE_FAILURE 
        }
        
        assertEquals(3, completeFailureScenarios.size)
        
        // Complete failure scenarios should enable verbal transmission
        assertTrue(completeFailureScenarios.all { it.verbalTransmissionEnabled })
        
        // Complete failure scenarios should enable approaching behavior
        assertTrue(completeFailureScenarios.all { it.approachingBehaviorEnabled })
    }

    @Test
    fun testScenarioParameterRanges() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        
        scenarios.forEach { scenario ->
            // App adoption rate should be between 0 and 1
            assertTrue("${scenario.name} has invalid appAdoptionRate", 
                scenario.appAdoptionRate >= 0.0 && scenario.appAdoptionRate <= 1.0)
            
            // Indoor ratio should be between 0 and 1
            assertTrue("${scenario.name} has invalid indoorRatio",
                scenario.indoorRatio >= 0.0 && scenario.indoorRatio <= 1.0)
            
            // Moving people ratio should be between 0 and 1
            assertTrue("${scenario.name} has invalid movingPeopleRatio",
                scenario.movingPeopleRatio >= 0.0 && scenario.movingPeopleRatio <= 1.0)
            
            // WiFi network density should be positive
            assertTrue("${scenario.name} has invalid wifiNetworkDensity",
                scenario.wifiNetworkDensity >= 0.0)
            
            // People count should be positive
            assertTrue("${scenario.name} has invalid peopleCount",
                scenario.peopleCount > 0)
        }
    }

    @Test
    fun testVerbalTransmissionRadiusLogic() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        
        // Indoor scenarios should have shorter verbal transmission radius
        val indoorScenarios = scenarios.filter { it.indoorRatio > 0.6 && it.verbalTransmissionEnabled }
        indoorScenarios.forEach { scenario ->
            assertTrue("${scenario.name} should have shorter verbal range indoors",
                scenario.verbalTransmissionRadius <= 20.0)
        }
        
        // Outdoor scenarios should have longer verbal transmission radius
        val outdoorScenarios = scenarios.filter { it.indoorRatio < 0.4 && it.verbalTransmissionEnabled }
        outdoorScenarios.forEach { scenario ->
            assertTrue("${scenario.name} should have longer verbal range outdoors",
                scenario.verbalTransmissionRadius >= 20.0)
        }
    }

    @Test
    fun testApproachingRadiusLogic() {
        val scenarios = SimulationScenario.getPredefinedScenarios()
        
        // City scenarios should have shorter approaching radius (buildings block sight)
        val cityScenarios = scenarios.filter { 
            it.locationType == LocationType.BIG_CITY_CENTER && it.approachingBehaviorEnabled 
        }
        cityScenarios.forEach { scenario ->
            assertTrue("${scenario.name} should have shorter approaching range in city",
                scenario.approachingRadius <= 75.0)
        }
        
        // Village scenarios should have longer approaching radius (open space)
        val villageScenarios = scenarios.filter { 
            it.locationType == LocationType.VILLAGE && it.approachingBehaviorEnabled 
        }
        villageScenarios.forEach { scenario ->
            assertTrue("${scenario.name} should have longer approaching range in village",
                scenario.approachingRadius >= 100.0)
        }
    }

    @Test
    fun testScenarioEquality() {
        val scenario1 = SimulationScenario(
            name = "Test",
            locationType = LocationType.MEDIUM_CITY,
            peopleCount = 100,
            appAdoptionRate = 0.5,
            indoorRatio = 0.6,
            wifiNetworkDensity = 1.5,
            movingPeopleRatio = 0.3,
            infrastructureFailure = InfrastructureFailureMode.DATA_BACKBONE
        )

        val scenario2 = SimulationScenario(
            name = "Test",
            locationType = LocationType.MEDIUM_CITY,
            peopleCount = 100,
            appAdoptionRate = 0.5,
            indoorRatio = 0.6,
            wifiNetworkDensity = 1.5,
            movingPeopleRatio = 0.3,
            infrastructureFailure = InfrastructureFailureMode.DATA_BACKBONE
        )

        assertEquals(scenario1, scenario2)
    }

    @Test
    fun testScenarioInequality() {
        val scenario1 = SimulationScenario(
            name = "Test 1",
            locationType = LocationType.MEDIUM_CITY,
            peopleCount = 100,
            appAdoptionRate = 0.5,
            indoorRatio = 0.6,
            wifiNetworkDensity = 1.5,
            movingPeopleRatio = 0.3,
            infrastructureFailure = InfrastructureFailureMode.DATA_BACKBONE
        )

        val scenario2 = SimulationScenario(
            name = "Test 2",
            locationType = LocationType.VILLAGE,
            peopleCount = 50,
            appAdoptionRate = 0.3,
            indoorRatio = 0.3,
            wifiNetworkDensity = 0.8,
            movingPeopleRatio = 0.2,
            infrastructureFailure = InfrastructureFailureMode.MOBILE_DATA_ONLY
        )

        assertNotEquals(scenario1, scenario2)
    }
}
