package com.fourpeople.adhoc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.fourpeople.adhoc.service.AdHocCommunicationService

/**
 * Receiver that starts the ad-hoc communication service in standby mode on boot.
 * This allows the app to be ready for emergency activation from other devices.
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, starting standby mode")
            
            // In standby mode, we would start a lightweight service
            // that listens for emergency broadcasts without full activation
            // For now, we just log the event
            
            // TODO: Implement standby mode service that only listens
            // without actively broadcasting or consuming battery
        }
    }
}
