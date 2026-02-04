package com.fourpeople.adhoc.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito.*

/**
 * Unit tests for BatteryMonitor.
 * Tests battery level reading and adaptive scan interval calculation.
 */
class BatteryMonitorTest {

    private lateinit var context: Context
    private lateinit var batteryIntent: Intent

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        batteryIntent = mock(Intent::class.java)
    }

    @Test
    fun testGetBatteryLevelHigh() {
        // Mock 80% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val batteryLevel = BatteryMonitor.getBatteryLevel(context)
        assertEquals(80, batteryLevel)
    }

    @Test
    fun testGetBatteryLevelMedium() {
        // Mock 35% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(35)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val batteryLevel = BatteryMonitor.getBatteryLevel(context)
        assertEquals(35, batteryLevel)
    }

    @Test
    fun testGetBatteryLevelLow() {
        // Mock 15% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(15)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val batteryLevel = BatteryMonitor.getBatteryLevel(context)
        assertEquals(15, batteryLevel)
    }

    @Test
    fun testGetBatteryLevelCritical() {
        // Mock 5% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(5)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val batteryLevel = BatteryMonitor.getBatteryLevel(context)
        assertEquals(5, batteryLevel)
    }

    @Test
    fun testGetBatteryLevelFull() {
        // Mock 100% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(100)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val batteryLevel = BatteryMonitor.getBatteryLevel(context)
        assertEquals(100, batteryLevel)
    }

    @Test
    fun testGetBatteryLevelDefaultOnError() {
        // Mock error condition (invalid values)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(-1)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(-1)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val batteryLevel = BatteryMonitor.getBatteryLevel(context)
        // Should default to 50% on error
        assertEquals(50, batteryLevel)
    }

    @Test
    fun testGetBatteryLevelNullIntent() {
        // Mock null battery intent
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(null)

        val batteryLevel = BatteryMonitor.getBatteryLevel(context)
        // Should default to 50% when intent is null
        assertEquals(50, batteryLevel)
    }

    @Test
    fun testEmergencyScanIntervalHighBattery() {
        // Mock 80% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getEmergencyScanInterval(context)
        assertEquals(10000L, interval)  // 10 seconds
    }

    @Test
    fun testEmergencyScanIntervalMediumBattery() {
        // Mock 35% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(35)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getEmergencyScanInterval(context)
        assertEquals(20000L, interval)  // 20 seconds
    }

    @Test
    fun testEmergencyScanIntervalLowBattery() {
        // Mock 15% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(15)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getEmergencyScanInterval(context)
        assertEquals(40000L, interval)  // 40 seconds
    }

    @Test
    fun testEmergencyScanIntervalCriticalBattery() {
        // Mock 5% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(5)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getEmergencyScanInterval(context)
        assertEquals(60000L, interval)  // 60 seconds
    }

    @Test
    fun testStandbyScanIntervalHighBattery() {
        // Mock 80% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getStandbyScanInterval(context)
        assertEquals(30000L, interval)  // 30 seconds
    }

    @Test
    fun testStandbyScanIntervalMediumBattery() {
        // Mock 35% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(35)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getStandbyScanInterval(context)
        assertEquals(60000L, interval)  // 60 seconds
    }

    @Test
    fun testStandbyScanIntervalLowBattery() {
        // Mock 15% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(15)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getStandbyScanInterval(context)
        assertEquals(120000L, interval)  // 120 seconds
    }

    @Test
    fun testStandbyScanIntervalCriticalBattery() {
        // Mock 5% battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(5)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        val interval = BatteryMonitor.getStandbyScanInterval(context)
        assertEquals(300000L, interval)  // 300 seconds (5 minutes)
    }

    @Test
    fun testBatteryModeBoundaries() {
        // Test boundary conditions

        // Just above 50%
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(51)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)
        assertEquals(10000L, BatteryMonitor.getEmergencyScanInterval(context))

        // At 50%
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(50)
        assertEquals(20000L, BatteryMonitor.getEmergencyScanInterval(context))

        // At 20%
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(20)
        assertEquals(40000L, BatteryMonitor.getEmergencyScanInterval(context))

        // At 10%
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(10)
        assertEquals(60000L, BatteryMonitor.getEmergencyScanInterval(context))
    }

    @Test
    fun testGetBatteryModeDescription() {
        // High battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)
        
        var description = BatteryMonitor.getBatteryModeDescription(context)
        assertEquals("Normal (Battery > 50%)", description)

        // Medium battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(35)
        description = BatteryMonitor.getBatteryModeDescription(context)
        assertEquals("Medium optimization (Battery 20-50%)", description)

        // Low battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(15)
        description = BatteryMonitor.getBatteryModeDescription(context)
        assertEquals("High optimization (Battery 10-20%)", description)

        // Critical battery
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(5)
        description = BatteryMonitor.getBatteryModeDescription(context)
        assertEquals("Maximum battery saving (Battery < 10%)", description)
    }

    @Test
    fun testScanIntervalsIncreaseWithLowerBattery() {
        // Emergency intervals should increase as battery decreases
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80)
        val interval80 = BatteryMonitor.getEmergencyScanInterval(context)

        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(35)
        val interval35 = BatteryMonitor.getEmergencyScanInterval(context)

        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(15)
        val interval15 = BatteryMonitor.getEmergencyScanInterval(context)

        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(5)
        val interval5 = BatteryMonitor.getEmergencyScanInterval(context)

        assertTrue(interval35 > interval80)
        assertTrue(interval15 > interval35)
        assertTrue(interval5 > interval15)
    }

    @Test
    fun testStandbyIntervalsLongerThanEmergency() {
        // Standby intervals should always be longer than emergency intervals
        `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100)
        `when`(context.registerReceiver(isNull(), any(IntentFilter::class.java)))
            .thenReturn(batteryIntent)

        for (level in listOf(80, 35, 15, 5)) {
            `when`(batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(level)
            
            val emergencyInterval = BatteryMonitor.getEmergencyScanInterval(context)
            val standbyInterval = BatteryMonitor.getStandbyScanInterval(context)
            
            assertTrue("Standby interval should be longer than emergency at $level%",
                standbyInterval > emergencyInterval)
        }
    }
}
