package com.fourpeople.adhoc.protocol

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for SEPS protocol handler.
 */
class SepsProtocolHandlerTest {
    
    @Test
    fun testIsSepsDevice() {
        assertTrue(SepsProtocolHandler.isSepsDevice("SEPS-4people-abc123"))
        assertTrue(SepsProtocolHandler.isSepsDevice("SEPS-otherapp-xyz789"))
        assertFalse(SepsProtocolHandler.isSepsDevice("4people-abc123"))
        assertFalse(SepsProtocolHandler.isSepsDevice("emergency-device"))
        assertFalse(SepsProtocolHandler.isSepsDevice("SEPX-wrong-prefix"))
    }
    
    @Test
    fun testGetSepsDeviceName() {
        val deviceName = SepsProtocolHandler.getSepsDeviceName("abc123")
        assertEquals("SEPS-4people-abc123", deviceName)
        
        val deviceName2 = SepsProtocolHandler.getSepsDeviceName("xyz-789")
        assertEquals("SEPS-4people-xyz-789", deviceName2)
    }
    
    @Test
    fun testShouldAdvertiseSeps() {
        // Should always return true for maximum interoperability
        assertTrue(SepsProtocolHandler.shouldAdvertiseSeps())
    }
    
    @Test
    fun testSepsDeviceNameFormat() {
        val deviceId = "test-device-123"
        val sepsName = SepsProtocolHandler.getSepsDeviceName(deviceId)
        
        // Should start with SEPS-
        assertTrue(sepsName.startsWith("SEPS-"))
        
        // Should contain the app identifier
        assertTrue(sepsName.contains("4people"))
        
        // Should contain the device ID
        assertTrue(sepsName.contains(deviceId))
        
        // Should be detectable as SEPS device
        assertTrue(SepsProtocolHandler.isSepsDevice(sepsName))
    }
    
    @Test
    fun testLegacyDeviceNameCompatibility() {
        // Legacy 4people device names should NOT be detected as SEPS
        // This ensures backward compatibility
        assertFalse(SepsProtocolHandler.isSepsDevice("4people-abc123"))
        assertFalse(SepsProtocolHandler.isSepsDevice("4people-xyz789"))
    }
    
    @Test
    fun testSepsDeviceNameUniqueness() {
        // Different device IDs should produce different SEPS names
        val name1 = SepsProtocolHandler.getSepsDeviceName("device-1")
        val name2 = SepsProtocolHandler.getSepsDeviceName("device-2")
        
        assertNotEquals(name1, name2)
    }
    
    @Test
    fun testSepsDeviceNameConsistency() {
        // Same device ID should always produce the same SEPS name
        val deviceId = "consistent-device"
        val name1 = SepsProtocolHandler.getSepsDeviceName(deviceId)
        val name2 = SepsProtocolHandler.getSepsDeviceName(deviceId)
        
        assertEquals(name1, name2)
    }
}
