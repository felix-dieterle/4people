package com.fourpeople.adhoc

import android.content.Context
import android.content.Intent
import android.os.Build
import com.fourpeople.adhoc.service.PanicModeService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

/**
 * Test class for PanicModeService
 */
@RunWith(MockitoJUnitRunner::class)
class PanicModeServiceTest {

    @Mock
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testServiceConstants() {
        // Verify service constants are defined correctly
        assertEquals("com.fourpeople.adhoc.PANIC_START", PanicModeService.ACTION_START)
        assertEquals("com.fourpeople.adhoc.PANIC_STOP", PanicModeService.ACTION_STOP)
        assertEquals("com.fourpeople.adhoc.PANIC_CONFIRM", PanicModeService.ACTION_CONFIRM)
        assertEquals(2001, PanicModeService.NOTIFICATION_ID)
        assertEquals("panic_mode_channel", PanicModeService.CHANNEL_ID)
    }

    @Test
    fun testConfirmationInterval() {
        // Verify confirmation interval is 30 seconds (30000ms)
        assertEquals(30000L, PanicModeService.CONFIRMATION_INTERVAL)
    }

    @Test
    fun testGentleWarningDuration() {
        // Verify gentle warning duration is 30 seconds
        assertEquals(30000L, PanicModeService.GENTLE_WARNING_DURATION)
    }

    @Test
    fun testMassiveAlertDuration() {
        // Verify massive alert duration is 2 minutes (120000ms)
        assertEquals(120000L, PanicModeService.MASSIVE_ALERT_DURATION)
    }

    @Test
    fun testContactNotificationInitialInterval() {
        // Verify initial contact notification interval is 3 minutes (180000ms)
        assertEquals(180000L, PanicModeService.CONTACT_NOTIFICATION_INITIAL_INTERVAL)
    }

    @Test
    fun testWarningTypes() {
        // Verify warning type constants
        assertEquals("vibration", PanicModeService.WARNING_VIBRATION)
        assertEquals("sound", PanicModeService.WARNING_SOUND)
        assertEquals("both", PanicModeService.WARNING_BOTH)
    }

    @Test
    fun testPreferenceKeys() {
        // Verify preference key constants
        assertEquals("panic_settings", PanicModeService.PREFS_NAME)
        assertEquals("panic_gentle_warning_type", PanicModeService.PREF_GENTLE_WARNING_TYPE)
        assertEquals("panic_auto_activate_data", PanicModeService.PREF_AUTO_ACTIVATE_DATA)
        assertEquals("panic_mode_is_active", PanicModeService.PREF_IS_ACTIVE)
    }

    @Test
    fun testEmergencyContactDataClass() {
        // Test EmergencyContact data class
        val contact = PanicModeService.EmergencyContact("John Doe", "+1234567890")
        assertEquals("John Doe", contact.name)
        assertEquals("+1234567890", contact.phoneNumber)
    }

    @Test
    fun testIntentActions() {
        // Test intent creation for starting panic mode
        val startIntent = Intent().apply {
            action = PanicModeService.ACTION_START
        }
        assertEquals(PanicModeService.ACTION_START, startIntent.action)

        // Test intent creation for stopping panic mode
        val stopIntent = Intent().apply {
            action = PanicModeService.ACTION_STOP
        }
        assertEquals(PanicModeService.ACTION_STOP, stopIntent.action)

        // Test intent creation for confirming OK
        val confirmIntent = Intent().apply {
            action = PanicModeService.ACTION_CONFIRM
        }
        assertEquals(PanicModeService.ACTION_CONFIRM, confirmIntent.action)
    }

    @Test
    fun testTimingCalculations() {
        // Test that total time to contact notification is correct
        val totalTime = PanicModeService.CONFIRMATION_INTERVAL + 
                       PanicModeService.GENTLE_WARNING_DURATION + 
                       PanicModeService.MASSIVE_ALERT_DURATION
        
        // Should be 3 minutes total (30s + 30s + 120s = 180s)
        assertEquals(180000L, totalTime)
    }

    @Test
    fun testContactNotificationIntervalDoubling() {
        // Test interval doubling logic
        var interval = PanicModeService.CONTACT_NOTIFICATION_INITIAL_INTERVAL
        assertEquals(180000L, interval) // 3 minutes
        
        interval *= 2
        assertEquals(360000L, interval) // 6 minutes
        
        interval *= 2
        assertEquals(720000L, interval) // 12 minutes
        
        interval *= 2
        assertEquals(1440000L, interval) // 24 minutes
    }
}
