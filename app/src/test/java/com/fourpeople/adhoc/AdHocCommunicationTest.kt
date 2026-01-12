package com.fourpeople.adhoc

import com.fourpeople.adhoc.service.AdHocCommunicationService
import org.junit.Test
import org.junit.Assert.*

/**
 * Basic unit tests for ad-hoc communication functionality.
 */
class AdHocCommunicationTest {

    @Test
    fun emergencyPatternIsCorrect() {
        assertEquals("4people-", AdHocCommunicationService.EMERGENCY_SSID_PATTERN)
    }

    @Test
    fun emergencyPatternMatching() {
        val testSSID = "4people-abc123"
        assertTrue(testSSID.startsWith(AdHocCommunicationService.EMERGENCY_SSID_PATTERN))
    }

    @Test
    fun nonEmergencyPatternNotMatching() {
        val testSSID = "MyHomeWiFi"
        assertFalse(testSSID.startsWith(AdHocCommunicationService.EMERGENCY_SSID_PATTERN))
    }

    @Test
    fun wifiScanIntervalIsReasonable() {
        // Should scan every 10 seconds (10000ms)
        assertEquals(10000L, AdHocCommunicationService.WIFI_SCAN_INTERVAL)
    }

    @Test
    fun notificationChannelIdIsSet() {
        assertNotNull(AdHocCommunicationService.CHANNEL_ID)
        assertTrue(AdHocCommunicationService.CHANNEL_ID.isNotEmpty())
    }
}
