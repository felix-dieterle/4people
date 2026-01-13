package com.fourpeople.adhoc.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.fourpeople.adhoc.MainActivity
import com.fourpeople.adhoc.R
import com.fourpeople.adhoc.util.FlashlightMorseHelper
import com.fourpeople.adhoc.util.EmergencySmsHelper

/**
 * Panic Mode Service
 * 
 * Manages the panic mode lifecycle with multiple escalation phases:
 * 1. Confirmation phase: User must confirm they're OK at regular intervals
 * 2. Gentle warning phase: Soft notification (vibration) if no confirmation
 * 3. Massive alert phase: Loud alarm, flashlight, full alerts + backend notification
 * 4. Contact notification phase: Progressive notification to emergency contacts
 */
class PanicModeService : Service() {

    companion object {
        private const val TAG = "PanicModeService"
        const val NOTIFICATION_ID = 2001
        const val CHANNEL_ID = "panic_mode_channel"
        const val ACTION_START = "com.fourpeople.adhoc.PANIC_START"
        const val ACTION_STOP = "com.fourpeople.adhoc.PANIC_STOP"
        const val ACTION_CONFIRM = "com.fourpeople.adhoc.PANIC_CONFIRM"
        
        // Timing constants (in milliseconds)
        const val CONFIRMATION_INTERVAL = 30000L // 30 seconds
        const val GENTLE_WARNING_DURATION = 30000L // 30 seconds
        const val MASSIVE_ALERT_DURATION = 120000L // 2 minutes
        const val CONTACT_NOTIFICATION_INITIAL_INTERVAL = 180000L // 3 minutes
        
        // Preferences keys
        const val PREF_GENTLE_WARNING_TYPE = "panic_gentle_warning_type"
        const val PREF_AUTO_ACTIVATE_DATA = "panic_auto_activate_data"
        
        // Warning types
        const val WARNING_VIBRATION = "vibration"
        const val WARNING_SOUND = "sound"
        const val WARNING_BOTH = "both"
    }

    private enum class PanicPhase {
        CONFIRMATION,      // Waiting for user confirmation
        GENTLE_WARNING,    // Gentle warning - vibration only
        MASSIVE_ALERT,     // Full alert - alarm, flashlight, GPS
        CONTACT_NOTIFICATION // Notifying emergency contacts progressively
    }

    private var currentPhase = PanicPhase.CONFIRMATION
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())
    
    private var lastConfirmationTime = 0L
    private var gentleWarningStartTime = 0L
    private var massiveAlertStartTime = 0L
    private var contactNotificationStartTime = 0L
    private var contactNotificationInterval = CONTACT_NOTIFICATION_INITIAL_INTERVAL
    private var currentContactIndex = 0
    
    private var vibrator: Vibrator? = null
    private var flashlightHelper: FlashlightMorseHelper? = null
    private var mediaPlayer: MediaPlayer? = null
    private var locationManager: LocationManager? = null
    private var lastKnownLocation: Location? = null
    private var lastSignalStrength: Int = 0

    private val confirmationCheckRunnable = object : Runnable {
        override fun run() {
            checkConfirmationStatus()
            handler.postDelayed(this, 5000L) // Check every 5 seconds
        }
    }

    private val contactNotificationRunnable = object : Runnable {
        override fun run() {
            notifyNextContact()
            // Double the interval for next contact
            contactNotificationInterval *= 2
            handler.postDelayed(this, contactNotificationInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        flashlightHelper = FlashlightMorseHelper(applicationContext)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        createNotificationChannel()
        
        Log.d(TAG, "PanicModeService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    startPanicMode()
                }
            }
            ACTION_STOP -> {
                stopPanicMode()
                stopSelf()
            }
            ACTION_CONFIRM -> {
                confirmOk()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopPanicMode()
        flashlightHelper?.cleanup()
        mediaPlayer?.release()
        Log.d(TAG, "PanicModeService destroyed")
    }

    private fun startPanicMode() {
        Log.d(TAG, "Starting panic mode")
        isRunning = true
        currentPhase = PanicPhase.CONFIRMATION
        lastConfirmationTime = System.currentTimeMillis()
        
        // Start foreground service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Activate GPS
        activateGPS()
        
        // Start confirmation check loop
        handler.post(confirmationCheckRunnable)
        
        updateNotification()
    }

    private fun stopPanicMode() {
        Log.d(TAG, "Stopping panic mode")
        isRunning = false
        
        // Stop all handlers
        handler.removeCallbacks(confirmationCheckRunnable)
        handler.removeCallbacks(contactNotificationRunnable)
        
        // Stop all alerts
        stopGentleWarning()
        stopMassiveAlert()
    }

    private fun confirmOk() {
        Log.d(TAG, "User confirmed OK")
        lastConfirmationTime = System.currentTimeMillis()
        currentPhase = PanicPhase.CONFIRMATION
        
        // Reset contact notification state
        currentContactIndex = 0
        contactNotificationInterval = CONTACT_NOTIFICATION_INITIAL_INTERVAL
        handler.removeCallbacks(contactNotificationRunnable)
        
        // Stop any ongoing alerts
        stopGentleWarning()
        stopMassiveAlert()
        
        updateNotification()
    }

    private fun checkConfirmationStatus() {
        if (!isRunning) return
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastConfirmation = currentTime - lastConfirmationTime
        
        when (currentPhase) {
            PanicPhase.CONFIRMATION -> {
                if (timeSinceLastConfirmation >= CONFIRMATION_INTERVAL) {
                    // No confirmation - start gentle warning
                    startGentleWarning()
                }
            }
            PanicPhase.GENTLE_WARNING -> {
                if (timeSinceLastConfirmation >= CONFIRMATION_INTERVAL + GENTLE_WARNING_DURATION) {
                    // Still no confirmation - escalate to massive alert
                    startMassiveAlert()
                }
            }
            PanicPhase.MASSIVE_ALERT -> {
                if (timeSinceLastConfirmation >= CONFIRMATION_INTERVAL + GENTLE_WARNING_DURATION + MASSIVE_ALERT_DURATION) {
                    // Still no confirmation - start contacting emergency contacts
                    startContactNotification()
                }
            }
            PanicPhase.CONTACT_NOTIFICATION -> {
                // Keep running contact notification loop
            }
        }
    }

    private fun startGentleWarning() {
        Log.d(TAG, "Starting gentle warning phase")
        currentPhase = PanicPhase.GENTLE_WARNING
        gentleWarningStartTime = System.currentTimeMillis()
        
        val warningType = getSharedPreferences("panic_settings", Context.MODE_PRIVATE)
            .getString(PREF_GENTLE_WARNING_TYPE, WARNING_VIBRATION) ?: WARNING_VIBRATION
        
        when (warningType) {
            WARNING_VIBRATION, WARNING_BOTH -> {
                startGentleVibration()
            }
        }
        
        when (warningType) {
            WARNING_SOUND, WARNING_BOTH -> {
                startGentleSound()
            }
        }
        
        updateNotification()
    }

    private fun stopGentleWarning() {
        vibrator?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startGentleVibration() {
        val pattern = longArrayOf(0, 500, 500, 500, 500) // Vibrate-pause pattern
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun startGentleSound() {
        // Use a gentle notification sound
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, notificationUri)
                isLooping = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
                setVolume(0.3f, 0.3f) // Gentle volume
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting gentle sound", e)
        }
    }

    private fun startMassiveAlert() {
        Log.d(TAG, "Starting massive alert phase")
        currentPhase = PanicPhase.MASSIVE_ALERT
        massiveAlertStartTime = System.currentTimeMillis()
        
        // Stop gentle warnings
        stopGentleWarning()
        
        // Start flashlight blinking
        flashlightHelper?.startSOSSignal()
        
        // Start loud alarm
        startAlarmSound()
        
        // Start strong vibration
        startStrongVibration()
        
        // Capture current location and signal strength
        captureLocationAndSignal()
        
        // Auto-activate mobile data and WiFi if enabled
        val autoActivateData = getSharedPreferences("panic_settings", Context.MODE_PRIVATE)
            .getBoolean(PREF_AUTO_ACTIVATE_DATA, false)
        
        if (autoActivateData) {
            activateMobileDataAndWifi()
        }
        
        // Send backend notification
        sendBackendNotification()
        
        updateNotification()
    }

    private fun stopMassiveAlert() {
        flashlightHelper?.stopSOSSignal()
        vibrator?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun startAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                }
                setVolume(1.0f, 1.0f) // Full volume
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting alarm sound", e)
        }
    }

    private fun startStrongVibration() {
        val pattern = longArrayOf(0, 1000, 500, 1000, 500) // Strong vibrate-pause pattern
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun startContactNotification() {
        Log.d(TAG, "Starting contact notification phase")
        currentPhase = PanicPhase.CONTACT_NOTIFICATION
        contactNotificationStartTime = System.currentTimeMillis()
        currentContactIndex = 0
        contactNotificationInterval = CONTACT_NOTIFICATION_INITIAL_INTERVAL
        
        // Start the contact notification loop
        handler.post(contactNotificationRunnable)
        
        updateNotification()
    }

    private fun notifyNextContact() {
        val emergencyContacts = getEmergencyContacts()
        
        if (emergencyContacts.isEmpty()) {
            Log.w(TAG, "No emergency contacts configured")
            return
        }
        
        if (currentContactIndex >= emergencyContacts.size) {
            Log.d(TAG, "All contacts notified, restarting from first contact")
            currentContactIndex = 0
        }
        
        val contact = emergencyContacts[currentContactIndex]
        currentContactIndex++
        
        Log.d(TAG, "Notifying contact: ${contact.name} (index: ${currentContactIndex - 1})")
        
        // Send notification via all channels
        sendContactNotification(contact)
    }

    private fun sendContactNotification(contact: EmergencyContact) {
        val locationStr = lastKnownLocation?.let {
            "Location: ${it.latitude}, ${it.longitude}"
        } ?: "Location: unavailable"
        
        val message = "PANIC MODE ALERT!\n\n" +
                "Emergency panic mode activated.\n" +
                "$locationStr\n" +
                "Signal strength: $lastSignalStrength dBm\n\n" +
                "Please check on me immediately!"
        
        // Send SMS using SmsManager directly for individual contact
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == 
            PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = android.telephony.SmsManager.getDefault()
                smsManager.sendTextMessage(
                    contact.phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
                Log.d(TAG, "Panic alert SMS sent to ${contact.name}: ${contact.phoneNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send panic SMS to ${contact.phoneNumber}", e)
            }
        }
        
        // TODO: Add other notification channels (email, push notification, etc.)
    }

    private fun activateGPS() {
        // GPS location is captured in captureLocationAndSignal()
        // The LocationManager is already initialized
        Log.d(TAG, "GPS activation requested")
    }

    private fun captureLocationAndSignal() {
        // Capture location
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
                lastKnownLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                Log.d(TAG, "Location captured: $lastKnownLocation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing location", e)
        }
        
        // Capture signal strength
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            // Note: Getting signal strength requires additional permissions and listeners
            // For now, we'll use a placeholder
            lastSignalStrength = -75 // Placeholder value
            Log.d(TAG, "Signal strength: $lastSignalStrength dBm")
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing signal strength", e)
        }
    }

    private fun activateMobileDataAndWifi() {
        try {
            // Activate WiFi
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (!wifiManager.isWifiEnabled) {
                // Note: On Android 10+, apps cannot enable WiFi programmatically
                // This will only work on older versions
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
                Log.d(TAG, "WiFi activation attempted")
            }
            
            // Note: Mobile data cannot be toggled programmatically by apps
            // This requires system-level permissions
            Log.d(TAG, "Mobile data and WiFi activation requested")
        } catch (e: Exception) {
            Log.e(TAG, "Error activating data connections", e)
        }
    }

    private fun sendBackendNotification() {
        // TODO: Implement backend API call
        // This should send location, signal strength, and panic mode status to backend
        Log.d(TAG, "Backend notification: location=${lastKnownLocation}, signal=$lastSignalStrength")
        
        // For now, just log the data that would be sent
        val backendData = mapOf(
            "event" to "panic_mode_alert",
            "latitude" to (lastKnownLocation?.latitude ?: 0.0),
            "longitude" to (lastKnownLocation?.longitude ?: 0.0),
            "signal_strength" to lastSignalStrength,
            "timestamp" to System.currentTimeMillis()
        )
        
        Log.d(TAG, "Backend data to send: $backendData")
    }

    private fun getEmergencyContacts(): List<EmergencyContact> {
        // Get emergency contacts from shared preferences (same as EmergencySmsHelper)
        val contacts = EmergencySmsHelper.getEmergencyContacts(applicationContext)
        
        // Convert phone numbers to EmergencyContact objects
        return contacts.mapIndexed { index, phoneNumber ->
            EmergencyContact("Contact ${index + 1}", phoneNumber)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Panic Mode",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Panic mode notifications"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val confirmIntent = Intent(this, PanicModeService::class.java).apply {
            action = ACTION_CONFIRM
        }
        val confirmPendingIntent = PendingIntent.getService(
            this,
            0,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, PanicModeService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val phaseText = when (currentPhase) {
            PanicPhase.CONFIRMATION -> "Waiting for confirmation"
            PanicPhase.GENTLE_WARNING -> "Gentle warning - Please confirm!"
            PanicPhase.MASSIVE_ALERT -> "MASSIVE ALERT - Confirm immediately!"
            PanicPhase.CONTACT_NOTIFICATION -> "Notifying emergency contacts"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Panic Mode Active")
            .setContentText(phaseText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "I'm OK", confirmPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    data class EmergencyContact(
        val name: String,
        val phoneNumber: String
    )
}
