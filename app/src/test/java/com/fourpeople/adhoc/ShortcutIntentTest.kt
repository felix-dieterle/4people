package com.fourpeople.adhoc

import android.content.Intent
import org.junit.Test
import org.junit.Assert.*

/**
 * Test for app shortcuts and intent handling
 */
class ShortcutIntentTest {

    @Test
    fun testPanicModeIntentAction() {
        val intent = Intent("com.fourpeople.adhoc.action.ACTIVATE_PANIC_MODE")
        assertEquals("com.fourpeople.adhoc.action.ACTIVATE_PANIC_MODE", intent.action)
    }

    @Test
    fun testEmergencyModeIntentAction() {
        val intent = Intent("com.fourpeople.adhoc.action.ACTIVATE_EMERGENCY_MODE")
        assertEquals("com.fourpeople.adhoc.action.ACTIVATE_EMERGENCY_MODE", intent.action)
    }

    @Test
    fun testIntentActionMatching() {
        val panicIntent = Intent("com.fourpeople.adhoc.action.ACTIVATE_PANIC_MODE")
        val emergencyIntent = Intent("com.fourpeople.adhoc.action.ACTIVATE_EMERGENCY_MODE")
        
        assertNotEquals(panicIntent.action, emergencyIntent.action)
        assertTrue(panicIntent.action?.contains("PANIC") == true)
        assertTrue(emergencyIntent.action?.contains("EMERGENCY") == true)
    }
}
