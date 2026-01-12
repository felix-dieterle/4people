package com.fourpeople.adhoc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import com.fourpeople.adhoc.service.StandbyMonitoringService

/**
 * Receiver that monitors for brief incoming phone calls as emergency indicators.
 * 
 * A brief incoming call (ring but not answered, or quickly hung up) can signal
 * an emergency situation where the caller wants to activate emergency mode
 * on all nearby devices.
 * 
 * Pattern: If a call rings for less than 5 seconds and is not answered,
 * it's treated as an emergency indicator.
 */
class PhoneCallIndicatorReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "PhoneCallIndicator"
        private const val BRIEF_CALL_THRESHOLD_MS = 5000L // 5 seconds
    }
    
    // Instance variables to track call state per receiver instance
    private var callStartTime: Long = 0
    private var lastState: String = TelephonyManager.EXTRA_STATE_IDLE
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    // Call is ringing - record start time
                    callStartTime = System.currentTimeMillis()
                    lastState = TelephonyManager.EXTRA_STATE_RINGING
                    Log.d(TAG, "Call ringing detected")
                }
                
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended or stopped ringing
                    if (lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                        val callDuration = System.currentTimeMillis() - callStartTime
                        
                        if (callDuration > 0 && callDuration < BRIEF_CALL_THRESHOLD_MS) {
                            // Brief call detected - potential emergency indicator
                            Log.d(TAG, "Brief call detected (${callDuration}ms) - possible emergency indicator")
                            handleEmergencyIndicator(context)
                        }
                    }
                    lastState = TelephonyManager.EXTRA_STATE_IDLE
                    callStartTime = 0
                }
                
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call was answered - not an emergency indicator
                    lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
                    callStartTime = 0
                }
            }
        }
    }
    
    private fun handleEmergencyIndicator(context: Context) {
        Log.d(TAG, "Emergency indicator detected via phone call")
        
        // Send broadcast to notify the StandbyMonitoringService
        // The service listens for this specific action
        val broadcastIntent = Intent("com.fourpeople.adhoc.PHONE_INDICATOR")
        broadcastIntent.setPackage(context.packageName)
        context.sendBroadcast(broadcastIntent)
    }
}
