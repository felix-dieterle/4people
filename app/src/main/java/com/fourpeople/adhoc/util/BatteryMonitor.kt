package com.fourpeople.adhoc.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/**
 * Utility class for monitoring battery level and providing adaptive scan intervals.
 * 
 * Based on battery level, returns appropriate scan intervals to optimize battery life:
 * - Battery > 50%: Fast scanning (10s Emergency / 30s Standby)
 * - Battery 20-50%: Medium scanning (20s Emergency / 60s Standby)
 * - Battery 10-20%: Slow scanning (40s Emergency / 120s Standby)
 * - Battery < 10%: Minimal scanning (60s Emergency / 300s Standby)
 */
object BatteryMonitor {
    
    /**
     * Get the current battery level as a percentage (0-100).
     */
    fun getBatteryLevel(context: Context): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        
        return if (level >= 0 && scale > 0) {
            (level.toFloat() / scale.toFloat() * 100).toInt()
        } else {
            50 // Default to 50% if unable to read (conservative estimate)
        }
    }
    
    /**
     * Get adaptive scan interval for emergency mode based on battery level.
     * Returns interval in milliseconds.
     */
    fun getEmergencyScanInterval(context: Context): Long {
        val batteryLevel = getBatteryLevel(context)
        return when {
            batteryLevel > 50 -> 10000L  // 10 seconds
            batteryLevel > 20 -> 20000L  // 20 seconds
            batteryLevel > 10 -> 40000L  // 40 seconds
            else -> 60000L                // 60 seconds
        }
    }
    
    /**
     * Get adaptive scan interval for standby mode based on battery level.
     * Returns interval in milliseconds.
     */
    fun getStandbyScanInterval(context: Context): Long {
        val batteryLevel = getBatteryLevel(context)
        return when {
            batteryLevel > 50 -> 30000L   // 30 seconds
            batteryLevel > 20 -> 60000L   // 60 seconds
            batteryLevel > 10 -> 120000L  // 120 seconds
            else -> 300000L                // 300 seconds (5 minutes)
        }
    }
    
    /**
     * Get a human-readable description of the current battery optimization mode.
     */
    fun getBatteryModeDescription(context: Context): String {
        val batteryLevel = getBatteryLevel(context)
        return when {
            batteryLevel > 50 -> "Normal (Battery > 50%)"
            batteryLevel > 20 -> "Medium optimization (Battery 20-50%)"
            batteryLevel > 10 -> "High optimization (Battery 10-20%)"
            else -> "Maximum battery saving (Battery < 10%)"
        }
    }
}
