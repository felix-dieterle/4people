package com.fourpeople.adhoc.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.ScanResult
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.fourpeople.adhoc.MainActivity
import com.fourpeople.adhoc.R
import com.fourpeople.adhoc.util.BatteryMonitor

/**
 * Standby monitoring service that runs in the background to detect emergency patterns.
 * 
 * This service:
 * - Periodically scans WiFi for emergency networks (4people-*)
 * - Monitors for phone call indicators (brief incoming calls)
 * - Can automatically activate full emergency mode when indicators are detected
 * - Runs with minimal battery consumption using periodic intervals
 */
class StandbyMonitoringService : Service() {

    companion object {
        private const val TAG = "StandbyMonitoring"
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "standby_channel"
        const val EMERGENCY_NOTIFICATION_ID = 1003
        const val EMERGENCY_CHANNEL_ID = "emergency_alert_channel"
        const val ACTION_START = "com.fourpeople.adhoc.STANDBY_START"
        const val ACTION_STOP = "com.fourpeople.adhoc.STANDBY_STOP"
        const val EMERGENCY_SSID_PATTERN = "4people-"
        
        const val PREF_NAME = "4people_prefs"
        const val PREF_AUTO_ACTIVATE = "auto_activate"
    }

    private var isRunning = false
    private lateinit var wifiManager: WifiManager
    private lateinit var preferences: SharedPreferences
    private val handler = Handler(Looper.getMainLooper())

    private val wifiScanRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                scanForEmergencyNetworks()
                // Use adaptive scan interval based on battery level
                val interval = BatteryMonitor.getStandbyScanInterval(applicationContext)
                handler.postDelayed(this, interval)
            }
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                handleWifiScanResults()
            }
        }
    }

    private val phoneIndicatorReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fourpeople.adhoc.PHONE_INDICATOR") {
                Log.d(TAG, "Phone indicator received")
                onEmergencyDetected("Brief phone call", "Phone")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    startStandbyMonitoring()
                }
            }
            ACTION_STOP -> {
                stopStandbyMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startStandbyMonitoring() {
        Log.d(TAG, "Starting standby monitoring")
        isRunning = true
        
        // Start as foreground service with minimal notification
        startForeground(NOTIFICATION_ID, createStandbyNotification())
        
        // Register WiFi scan receiver
        val wifiFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wifiScanReceiver, wifiFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wifiScanReceiver, wifiFilter)
        }
        
        // Register phone indicator receiver
        val phoneFilter = IntentFilter("com.fourpeople.adhoc.PHONE_INDICATOR")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(phoneIndicatorReceiver, phoneFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(phoneIndicatorReceiver, phoneFilter)
        }
        
        // Start periodic WiFi scanning
        handler.post(wifiScanRunnable)
        
        val interval = BatteryMonitor.getStandbyScanInterval(applicationContext)
        val batteryMode = BatteryMonitor.getBatteryModeDescription(applicationContext)
        Log.d(TAG, "Standby monitoring active - scanning every ${interval/1000}s ($batteryMode)")
    }

    private fun stopStandbyMonitoring() {
        Log.d(TAG, "Stopping standby monitoring")
        isRunning = false
        
        handler.removeCallbacks(wifiScanRunnable)
        
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        
        try {
            unregisterReceiver(phoneIndicatorReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    private fun scanForEmergencyNetworks() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing location permission for WiFi scanning")
            return
        }
        
        val success = wifiManager.startScan()
        if (!success) {
            Log.w(TAG, "WiFi scan failed to start")
        }
    }

    private fun handleWifiScanResults() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        val results: List<ScanResult> = wifiManager.scanResults
        
        results.forEach { result ->
            if (result.SSID.startsWith(EMERGENCY_SSID_PATTERN)) {
                Log.d(TAG, "Emergency network detected in standby: ${result.SSID}")
                onEmergencyDetected(result.SSID, "WiFi")
            }
        }
    }

    /**
     * Called when an emergency indicator is detected.
     * Depending on user preferences, either auto-activates or shows notification.
     * 
     * This is an internal method that should only be called by this service.
     */
    private fun onEmergencyDetected(source: String, type: String) {
        Log.d(TAG, "Emergency detected - Source: $source, Type: $type")
        
        // Send broadcast to notify app components
        val broadcastIntent = Intent("com.fourpeople.adhoc.EMERGENCY_DETECTED")
        broadcastIntent.setPackage(packageName)
        broadcastIntent.putExtra("source", source)
        broadcastIntent.putExtra("type", type)
        sendBroadcast(broadcastIntent)
        
        // Check user preference for auto-activation
        val autoActivate = preferences.getBoolean(PREF_AUTO_ACTIVATE, false)
        
        if (autoActivate) {
            activateEmergencyMode(source)
        } else {
            showEmergencyDetectedNotification(source, type)
        }
    }

    private fun activateEmergencyMode(source: String) {
        Log.d(TAG, "Auto-activating emergency mode due to: $source")
        
        // Stop standby monitoring
        stopStandbyMonitoring()
        
        // Start full emergency communication service
        val intent = Intent(this, AdHocCommunicationService::class.java)
        intent.action = AdHocCommunicationService.ACTION_START
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        // Show activation notification
        showEmergencyActivatedNotification(source)
        
        // Stop this service
        stopSelf()
    }

    private fun showEmergencyDetectedNotification(source: String, type: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
            .setContentTitle(getString(R.string.emergency_detected_title))
            .setContentText(getString(R.string.emergency_detected_text, source))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(getString(R.string.emergency_detected_description, type, source)))
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EMERGENCY_NOTIFICATION_ID, notification)
    }

    private fun showEmergencyActivatedNotification(source: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, EMERGENCY_CHANNEL_ID)
            .setContentTitle(getString(R.string.emergency_auto_activated_title))
            .setContentText(getString(R.string.emergency_auto_activated_text, source))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EMERGENCY_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Standby monitoring channel (low priority)
            val standbyChannel = NotificationChannel(
                CHANNEL_ID,
                "Standby Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background monitoring for emergency signals"
            }
            notificationManager.createNotificationChannel(standbyChannel)
            
            // Emergency alert channel (high priority)
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(emergencyChannel)
        }
    }

    private fun createStandbyNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.standby_monitoring_title))
            .setContentText(getString(R.string.standby_monitoring_text))
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}
