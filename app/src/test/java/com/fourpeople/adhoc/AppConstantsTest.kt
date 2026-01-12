package com.fourpeople.adhoc

import com.fourpeople.adhoc.service.AdHocCommunicationService
import com.fourpeople.adhoc.service.StandbyMonitoringService
import org.junit.Test
import org.junit.Assert.*

/**
 * Cross-component integration tests verifying consistency across the app.
 */
class AppConstantsTest {

    @Test
    fun emergencyPatternConsistencyAcrossServices() {
        // Both services should use the same emergency pattern
        assertEquals(
            AdHocCommunicationService.EMERGENCY_SSID_PATTERN,
            StandbyMonitoringService.EMERGENCY_SSID_PATTERN
        )
    }

    @Test
    fun emergencyPatternFormatIsValid() {
        val pattern = AdHocCommunicationService.EMERGENCY_SSID_PATTERN
        
        // Pattern should be "4people-"
        assertEquals("4people-", pattern)
        assertTrue(pattern.endsWith("-"))
        assertTrue(pattern.startsWith("4people"))
    }

    @Test
    fun notificationIdsAreUnique() {
        val adHocNotificationId = AdHocCommunicationService.NOTIFICATION_ID
        val standbyNotificationId = StandbyMonitoringService.NOTIFICATION_ID
        val emergencyNotificationId = StandbyMonitoringService.EMERGENCY_NOTIFICATION_ID
        
        // All notification IDs must be unique
        assertNotEquals(adHocNotificationId, standbyNotificationId)
        assertNotEquals(adHocNotificationId, emergencyNotificationId)
        assertNotEquals(standbyNotificationId, emergencyNotificationId)
    }

    @Test
    fun notificationChannelIdsAreUnique() {
        val adHocChannelId = AdHocCommunicationService.CHANNEL_ID
        val standbyChannelId = StandbyMonitoringService.CHANNEL_ID
        val emergencyChannelId = StandbyMonitoringService.EMERGENCY_CHANNEL_ID
        
        // All channel IDs must be unique
        assertNotEquals(adHocChannelId, standbyChannelId)
        assertNotEquals(adHocChannelId, emergencyChannelId)
        assertNotEquals(standbyChannelId, emergencyChannelId)
    }

    @Test
    fun wifiScanIntervalsAreReasonable() {
        val activeInterval = AdHocCommunicationService.WIFI_SCAN_INTERVAL
        val standbyInterval = StandbyMonitoringService.WIFI_SCAN_INTERVAL
        
        // Standby should scan less frequently than active
        assertTrue(standbyInterval > activeInterval)
        
        // Intervals should be positive
        assertTrue(activeInterval > 0)
        assertTrue(standbyInterval > 0)
        
        // Intervals should be in milliseconds and reasonable
        // Active: 10 seconds, Standby: 30 seconds
        assertEquals(10000L, activeInterval)
        assertEquals(30000L, standbyInterval)
    }

    @Test
    fun serviceActionNamesAreDistinct() {
        val adHocStart = AdHocCommunicationService.ACTION_START
        val adHocStop = AdHocCommunicationService.ACTION_STOP
        val standbyStart = StandbyMonitoringService.ACTION_START
        val standbyStop = StandbyMonitoringService.ACTION_STOP
        
        // All action names should be unique
        val allActions = setOf(adHocStart, adHocStop, standbyStart, standbyStop)
        assertEquals(4, allActions.size)
    }

    @Test
    fun serviceActionNamesFollowConvention() {
        val actions = listOf(
            AdHocCommunicationService.ACTION_START,
            AdHocCommunicationService.ACTION_STOP,
            StandbyMonitoringService.ACTION_START,
            StandbyMonitoringService.ACTION_STOP
        )
        
        // All actions should start with the package name
        actions.forEach { action ->
            assertTrue(action.startsWith("com.fourpeople.adhoc"))
        }
        
        // Start actions should contain "START"
        assertTrue(AdHocCommunicationService.ACTION_START.contains("START"))
        assertTrue(StandbyMonitoringService.ACTION_START.contains("START"))
        
        // Stop actions should contain "STOP"
        assertTrue(AdHocCommunicationService.ACTION_STOP.contains("STOP"))
        assertTrue(StandbyMonitoringService.ACTION_STOP.contains("STOP"))
    }

    @Test
    fun sharedPreferencesNameIsConsistent() {
        val prefName = StandbyMonitoringService.PREF_NAME
        
        // Should follow app naming convention
        assertTrue(prefName.startsWith("4people"))
        assertEquals("4people_prefs", prefName)
    }

    @Test
    fun emergencyPatternMatchingExamples() {
        val pattern = AdHocCommunicationService.EMERGENCY_SSID_PATTERN
        
        // Valid emergency SSIDs
        assertTrue("4people-abc123".startsWith(pattern))
        assertTrue("4people-device-1".startsWith(pattern))
        assertTrue("4people-xyz".startsWith(pattern))
        assertTrue("4people-".startsWith(pattern))
        
        // Invalid emergency SSIDs
        assertFalse("4peopleabc123".startsWith(pattern))
        assertFalse("fourpeople-123".startsWith(pattern))
        assertFalse("MyHomeWiFi".startsWith(pattern))
        assertFalse("public-wifi".startsWith(pattern))
        assertFalse("".startsWith(pattern))
    }

    @Test
    fun batteryOptimizationIntervals() {
        val activeInterval = AdHocCommunicationService.WIFI_SCAN_INTERVAL
        val standbyInterval = StandbyMonitoringService.WIFI_SCAN_INTERVAL
        
        // Standby should be at least 2x longer for battery optimization
        assertTrue(standbyInterval >= activeInterval * 2)
        
        // Standby should be 3x longer in this implementation (30s vs 10s)
        assertEquals(activeInterval * 3, standbyInterval)
    }
}
