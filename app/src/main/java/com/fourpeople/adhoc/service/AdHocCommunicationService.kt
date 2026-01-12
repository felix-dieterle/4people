package com.fourpeople.adhoc.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import java.util.*

/**
 * Service for managing ad-hoc emergency communication.
 * 
 * This service:
 * - Activates Bluetooth discovery and makes device discoverable
 * - Scans for WiFi networks with the pattern "4people-<id>"
 * - Attempts to create a WiFi hotspot with emergency naming pattern
 * - Listens for emergency broadcasts from other devices
 */
class AdHocCommunicationService : Service() {

    companion object {
        private const val TAG = "AdHocCommService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "emergency_channel"
        const val ACTION_START = "com.fourpeople.adhoc.START"
        const val ACTION_STOP = "com.fourpeople.adhoc.STOP"
        const val WIFI_SCAN_INTERVAL = 10000L // 10 seconds
        const val EMERGENCY_SSID_PATTERN = "4people-"
    }

    private var isRunning = false
    private lateinit var wifiManager: WifiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var deviceId: String = ""

    private val wifiScanRunnable = object : Runnable {
        override fun run() {
            scanForEmergencyNetworks()
            handler.postDelayed(this, WIFI_SCAN_INTERVAL)
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                handleWifiScanResults()
            }
        }
    }

    private val bluetoothDiscoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        handleBluetoothDeviceFound(it)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        // Generate unique device ID
        deviceId = UUID.randomUUID().toString().substring(0, 8)
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (!isRunning) {
                    startEmergencyMode()
                }
            }
            ACTION_STOP -> {
                stopEmergencyMode()
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startEmergencyMode() {
        Log.d(TAG, "Starting emergency mode")
        isRunning = true
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Activate all communication channels
        activateBluetooth()
        activateWifiScanning()
        activateHotspot()
        
        // Broadcast local emergency
        broadcastEmergencySignal()
    }

    private fun stopEmergencyMode() {
        Log.d(TAG, "Stopping emergency mode")
        isRunning = false
        
        handler.removeCallbacks(wifiScanRunnable)
        
        try {
            unregisterReceiver(wifiScanReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        
        try {
            unregisterReceiver(bluetoothDiscoveryReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        
        deactivateBluetooth()
    }

    private fun activateBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth is disabled, cannot activate")
            return
        }
        
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Missing Bluetooth permissions")
                return
            }
        }
        
        // Register for Bluetooth discovery
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothDiscoveryReceiver, filter)
        
        // Start discovery
        bluetoothAdapter.startDiscovery()
        
        // Make device discoverable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
                // Set device name to emergency pattern
                bluetoothAdapter.name = "${EMERGENCY_SSID_PATTERN}$deviceId"
            }
        } else {
            bluetoothAdapter.name = "${EMERGENCY_SSID_PATTERN}$deviceId"
        }
        
        Log.d(TAG, "Bluetooth activated and discovery started")
    }

    private fun deactivateBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        
        Log.d(TAG, "Bluetooth deactivated")
    }

    private fun activateWifiScanning() {
        // Register WiFi scan receiver
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, filter)
        
        // Start periodic WiFi scanning
        handler.post(wifiScanRunnable)
        
        Log.d(TAG, "WiFi scanning activated")
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
                Log.d(TAG, "Emergency network detected: ${result.SSID}")
                notifyEmergencyDetected(result.SSID)
            }
        }
    }

    private fun activateHotspot() {
        // Note: Creating WiFi hotspot programmatically requires system permissions
        // or is deprecated in newer Android versions. This is a simplified approach.
        // In production, you would need to use WifiManager.LocalOnlyHotspotReservation
        // for Android O+ or guide users to manually enable hotspot.
        
        Log.d(TAG, "Hotspot activation requested (may require manual setup on newer Android versions)")
        
        // On Android O and above, we can request a local-only hotspot
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                        Log.d(TAG, "Local-only hotspot started")
                        reservation?.let {
                            Log.d(TAG, "Hotspot SSID: ${it.wifiConfiguration?.SSID}")
                        }
                    }

                    override fun onStopped() {
                        Log.d(TAG, "Local-only hotspot stopped")
                    }

                    override fun onFailed(reason: Int) {
                        Log.e(TAG, "Failed to start local-only hotspot: $reason")
                    }
                }, handler)
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception when starting hotspot", e)
            }
        }
    }

    private fun handleBluetoothDeviceFound(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val deviceName = device.name ?: "Unknown"
        Log.d(TAG, "Bluetooth device found: $deviceName")
        
        if (deviceName.startsWith(EMERGENCY_SSID_PATTERN)) {
            Log.d(TAG, "Emergency Bluetooth device detected: $deviceName")
            notifyEmergencyDetected(deviceName)
        }
    }

    private fun notifyEmergencyDetected(source: String) {
        // Send broadcast to notify the app
        val intent = Intent("com.fourpeople.adhoc.EMERGENCY_DETECTED")
        intent.putExtra("source", source)
        sendBroadcast(intent)
    }

    private fun broadcastEmergencySignal() {
        Log.d(TAG, "Broadcasting emergency signal")
        // This would send out emergency broadcasts
        // Implementation depends on the communication protocol
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Emergency Communication"
            val descriptionText = "Ad-hoc emergency communication is active"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.emergency_notification_title))
            .setContentText(getString(R.string.emergency_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
