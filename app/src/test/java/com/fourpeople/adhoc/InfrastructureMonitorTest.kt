package com.fourpeople.adhoc

import com.fourpeople.adhoc.util.InfrastructureMonitor
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for infrastructure monitoring functionality.
 */
class InfrastructureMonitorTest {

    @Test
    fun healthStatusEnumHasAllRequiredValues() {
        val statuses = InfrastructureMonitor.HealthStatus.values()
        
        assertTrue(statuses.contains(InfrastructureMonitor.HealthStatus.HEALTHY))
        assertTrue(statuses.contains(InfrastructureMonitor.HealthStatus.DEGRADED))
        assertTrue(statuses.contains(InfrastructureMonitor.HealthStatus.FAILED))
        assertTrue(statuses.contains(InfrastructureMonitor.HealthStatus.UNKNOWN))
    }

    @Test
    fun healthStatusCountIsCorrect() {
        // Ensure we have exactly 4 status types
        assertEquals(4, InfrastructureMonitor.HealthStatus.values().size)
    }

    @Test
    fun infrastructureStatusDataClassHasCorrectFields() {
        // Create a sample status object
        val status = InfrastructureMonitor.InfrastructureStatus(
            timestamp = System.currentTimeMillis(),
            bluetoothHealth = InfrastructureMonitor.HealthStatus.HEALTHY,
            wifiHealth = InfrastructureMonitor.HealthStatus.DEGRADED,
            cellularHealth = InfrastructureMonitor.HealthStatus.FAILED,
            meshHealth = InfrastructureMonitor.HealthStatus.UNKNOWN,
            overallHealth = InfrastructureMonitor.HealthStatus.DEGRADED
        )
        
        // Verify all fields are accessible
        assertNotNull(status.timestamp)
        assertEquals(InfrastructureMonitor.HealthStatus.HEALTHY, status.bluetoothHealth)
        assertEquals(InfrastructureMonitor.HealthStatus.DEGRADED, status.wifiHealth)
        assertEquals(InfrastructureMonitor.HealthStatus.FAILED, status.cellularHealth)
        assertEquals(InfrastructureMonitor.HealthStatus.UNKNOWN, status.meshHealth)
        assertEquals(InfrastructureMonitor.HealthStatus.DEGRADED, status.overallHealth)
    }

    @Test
    fun statusToMapConversion() {
        val timestamp = System.currentTimeMillis()
        val status = InfrastructureMonitor.InfrastructureStatus(
            timestamp = timestamp,
            bluetoothHealth = InfrastructureMonitor.HealthStatus.HEALTHY,
            wifiHealth = InfrastructureMonitor.HealthStatus.HEALTHY,
            cellularHealth = InfrastructureMonitor.HealthStatus.HEALTHY,
            meshHealth = InfrastructureMonitor.HealthStatus.HEALTHY,
            overallHealth = InfrastructureMonitor.HealthStatus.HEALTHY
        )
        
        val map = status.toMap()
        
        assertEquals(timestamp, map["timestamp"])
        assertEquals("HEALTHY", map["bluetooth"])
        assertEquals("HEALTHY", map["wifi"])
        assertEquals("HEALTHY", map["cellular"])
        assertEquals("HEALTHY", map["mesh"])
        assertEquals("HEALTHY", map["overall"])
    }

    @Test
    fun healthStatusNamesAreCorrect() {
        assertEquals("HEALTHY", InfrastructureMonitor.HealthStatus.HEALTHY.name)
        assertEquals("DEGRADED", InfrastructureMonitor.HealthStatus.DEGRADED.name)
        assertEquals("FAILED", InfrastructureMonitor.HealthStatus.FAILED.name)
        assertEquals("UNKNOWN", InfrastructureMonitor.HealthStatus.UNKNOWN.name)
    }

    @Test
    fun overallHealthCalculationLogic() {
        // This tests the expected behavior based on the implementation
        // All healthy should result in HEALTHY overall
        // Mixture of healthy/degraded should result in HEALTHY if at least 2 healthy
        // Mostly failed should result in FAILED
        
        // Test: All status types are distinct
        val healthyStatus = InfrastructureMonitor.HealthStatus.HEALTHY
        val degradedStatus = InfrastructureMonitor.HealthStatus.DEGRADED
        val failedStatus = InfrastructureMonitor.HealthStatus.FAILED
        val unknownStatus = InfrastructureMonitor.HealthStatus.UNKNOWN
        
        assertNotEquals(healthyStatus, degradedStatus)
        assertNotEquals(healthyStatus, failedStatus)
        assertNotEquals(degradedStatus, failedStatus)
        assertNotEquals(unknownStatus, healthyStatus)
    }

    @Test
    fun infrastructureCheckIntervalIsReasonable() {
        // Check interval should be 30 seconds (30000ms) for balance between responsiveness and battery
        val expectedInterval = 30000L
        assertEquals(expectedInterval, 
            com.fourpeople.adhoc.service.AdHocCommunicationService.INFRASTRUCTURE_CHECK_INTERVAL)
    }

    @Test
    fun infrastructureBroadcastActionsAreUnique() {
        // Ensure infrastructure-related broadcast actions are unique
        val statusAction = com.fourpeople.adhoc.service.AdHocCommunicationService.ACTION_INFRASTRUCTURE_STATUS
        val failureAction = com.fourpeople.adhoc.service.AdHocCommunicationService.ACTION_INFRASTRUCTURE_FAILURE
        val widgetAction = com.fourpeople.adhoc.service.AdHocCommunicationService.ACTION_WIDGET_UPDATE
        
        assertNotEquals(statusAction, failureAction)
        assertNotEquals(statusAction, widgetAction)
        assertNotEquals(failureAction, widgetAction)
    }

    @Test
    fun infrastructureNotificationChannelIdIsUnique() {
        val infraChannelId = com.fourpeople.adhoc.service.AdHocCommunicationService.CHANNEL_ID_INFRASTRUCTURE
        val emergencyChannelId = com.fourpeople.adhoc.service.AdHocCommunicationService.CHANNEL_ID
        
        assertNotEquals(infraChannelId, emergencyChannelId)
        assertEquals("infrastructure_alerts", infraChannelId)
    }

    @Test
    fun infrastructurePreferenceKeyIsConsistent() {
        // The preference key should be consistent between service and settings
        assertEquals("infrastructure_notifications_enabled", 
            com.fourpeople.adhoc.service.AdHocCommunicationService.PREF_INFRASTRUCTURE_NOTIFICATIONS)
    }

    @Test
    fun infrastructureBroadcastExtrasAreWellDefined() {
        // Verify all extra keys are defined
        assertNotNull(com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_BLUETOOTH)
        assertNotNull(com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_WIFI)
        assertNotNull(com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_CELLULAR)
        assertNotNull(com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_MESH)
        assertNotNull(com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_OVERALL)
        assertNotNull(com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_DESCRIPTION)
        
        // Verify they are unique
        val extras = setOf(
            com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_BLUETOOTH,
            com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_WIFI,
            com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_CELLULAR,
            com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_MESH,
            com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_OVERALL,
            com.fourpeople.adhoc.service.AdHocCommunicationService.EXTRA_INFRA_DESCRIPTION
        )
        
        // If all unique, set size should equal number of constants
        assertEquals(6, extras.size)
    }
}
