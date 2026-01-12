package com.fourpeople.adhoc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.fourpeople.adhoc.service.StandbyMonitoringService

/**
 * Receiver that starts the standby monitoring service on device boot.
 * This allows the app to be ready for emergency activation from other devices.
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            Log.d(TAG, "Boot completed, starting standby monitoring service")
            
            val serviceIntent = Intent(context, StandbyMonitoringService::class.java)
            serviceIntent.action = StandbyMonitoringService.ACTION_START
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "Standby monitoring service started")
        }
    }
}
