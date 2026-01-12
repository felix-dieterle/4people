package com.fourpeople.adhoc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver that handles emergency broadcasts from other devices.
 * When an emergency is detected, this can trigger automatic activation.
 */
class EmergencyBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "EmergencyReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.fourpeople.adhoc.EMERGENCY_DETECTED") {
            val source = intent.getStringExtra("source") ?: "unknown"
            Log.d(TAG, "Emergency detected from: $source")
            
            // Notify the user about the detected emergency
            // Could trigger automatic activation or prompt user
            
            // TODO: Implement automatic activation logic or user prompt
        }
    }
}
