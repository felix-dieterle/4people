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
import com.fourpeople.adhoc.util.BatteryMonitor
import com.fourpeople.adhoc.util.EmergencySmsHelper
import com.fourpeople.adhoc.util.ErrorLogger
import com.fourpeople.adhoc.util.FlashlightMorseHelper
import com.fourpeople.adhoc.util.LogManager
import com.fourpeople.adhoc.util.UltrasoundSignalHelper
import com.fourpeople.adhoc.util.NFCHelper
import com.fourpeople.adhoc.util.InfrastructureMonitor
import com.fourpeople.adhoc.mesh.MeshRoutingManager
import com.fourpeople.adhoc.mesh.BluetoothMeshTransport
import com.fourpeople.adhoc.mesh.MeshMessage
import com.fourpeople.adhoc.location.LocationSharingManager
import com.fourpeople.adhoc.location.LocationData
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
        const val ACTION_REQUEST_STATUS = "com.fourpeople.adhoc.REQUEST_STATUS"
        const val EMERGENCY_SSID_PATTERN = "4people-"
        const val WIFI_SCAN_INTERVAL = 10000L // 10 seconds (default for active mode)
        const val STATUS_UPDATE_INTERVAL = 5000L // 5 seconds
        const val ACTION_STATUS_UPDATE = "com.fourpeople.adhoc.STATUS_UPDATE"
        const val EXTRA_BLUETOOTH_ACTIVE = "bluetooth_active"
        const val EXTRA_WIFI_ACTIVE = "wifi_active"
        const val EXTRA_HOTSPOT_ACTIVE = "hotspot_active"
        const val EXTRA_LOCATION_ACTIVE = "location_active"
        const val EXTRA_WIFI_CONNECTED = "wifi_connected"
        const val ACTION_WIDGET_UPDATE = "com.fourpeople.adhoc.WIDGET_UPDATE"
        
        // Infrastructure monitoring
        const val INFRASTRUCTURE_CHECK_INTERVAL = 30000L // 30 seconds
        const val ACTION_INFRASTRUCTURE_STATUS = "com.fourpeople.adhoc.INFRASTRUCTURE_STATUS"
        const val ACTION_INFRASTRUCTURE_FAILURE = "com.fourpeople.adhoc.INFRASTRUCTURE_FAILURE"
        const val EXTRA_INFRA_BLUETOOTH = "infra_bluetooth"
        const val EXTRA_INFRA_WIFI = "infra_wifi"
        const val EXTRA_INFRA_CELLULAR = "infra_cellular"
        const val EXTRA_INFRA_MESH = "infra_mesh"
        const val EXTRA_INFRA_OVERALL = "infra_overall"
        const val EXTRA_INFRA_DESCRIPTION = "infra_description"
        const val CHANNEL_ID_INFRASTRUCTURE = "infrastructure_alerts"
        const val PREF_INFRASTRUCTURE_NOTIFICATIONS = "infrastructure_notifications_enabled"
        
        // Preferences
        const val PREFS_NAME = "emergency_prefs"
        const val PREF_IS_ACTIVE = "emergency_mode_is_active"
        
        /**
         * Check if emergency mode service is currently active
         */
        fun isActive(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_IS_ACTIVE, false)
        }
    }

    private var isRunning = false
    private lateinit var wifiManager: WifiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val handler = Handler(Looper.getMainLooper())
    private var deviceId: String = ""
    private var wifiDirectHelper: WiFiDirectHelper? = null
    private var wifiConnectionHelper: WiFiConnectionHelper? = null
    private var flashlightHelper: FlashlightMorseHelper? = null
    private var ultrasoundHelper: UltrasoundSignalHelper? = null
    private var meshRoutingManager: MeshRoutingManager? = null
    private var bluetoothMeshTransport: BluetoothMeshTransport? = null
    private var nfcHelper: NFCHelper? = null
    private var locationSharingManager: LocationSharingManager? = null
    private var infrastructureMonitor: InfrastructureMonitor? = null
    
    // Status tracking
    private var isBluetoothActive = false
    private var isWifiScanningActive = false
    private var isHotspotActive = false
    private var isWifiConnected = false

    private val wifiScanRunnable = object : Runnable {
        override fun run() {
            scanForEmergencyNetworks()
            // Use adaptive scan interval based on battery level
            val interval = BatteryMonitor.getEmergencyScanInterval(applicationContext)
            handler.postDelayed(this, interval)
        }
    }
    
    private val meshMaintenanceRunnable = object : Runnable {
        override fun run() {
            performMeshMaintenance()
            handler.postDelayed(this, 30000L) // Every 30 seconds
        }
    }
    
    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            // Only continue broadcasting if service is still running
            if (isRunning) {
                broadcastStatusUpdate()
                handler.postDelayed(this, STATUS_UPDATE_INTERVAL)
            }
        }
    }
    
    private val infrastructureCheckRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                performInfrastructureCheck()
                handler.postDelayed(this, INFRASTRUCTURE_CHECK_INTERVAL)
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

    private val helpRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fourpeople.adhoc.SEND_HELP_REQUEST") {
                val message = intent.getStringExtra("help_message")
                val radiusKm = intent.getDoubleExtra("event_radius_km", 100.0)
                handleSendHelpRequest(message, radiusKm)
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
        
        // Initialize WiFi Direct
        wifiDirectHelper = WiFiDirectHelper(applicationContext)
        wifiDirectHelper?.initialize()
        
        // Initialize WiFi Connection Helper
        wifiConnectionHelper = WiFiConnectionHelper(applicationContext)
        
        // Set up WiFi connection status listener
        setupWifiConnectionListener()
        
        // Initialize flashlight and ultrasound helpers
        flashlightHelper = FlashlightMorseHelper(applicationContext)
        ultrasoundHelper = UltrasoundSignalHelper()
        
        // Initialize NFC helper
        nfcHelper = NFCHelper(applicationContext)
        if (nfcHelper?.initialize() == true) {
            Log.d(TAG, "NFC helper initialized")
        }
        
        // Initialize mesh routing
        meshRoutingManager = MeshRoutingManager(applicationContext, deviceId)
        bluetoothMeshTransport = BluetoothMeshTransport(applicationContext, bluetoothAdapter, deviceId)
        
        // Initialize location sharing
        locationSharingManager = LocationSharingManager(applicationContext, deviceId)
        
        // Initialize infrastructure monitor
        infrastructureMonitor = InfrastructureMonitor(applicationContext)
        
        // Set up mesh routing callbacks
        setupMeshRouting()
        
        // Set up location sharing callbacks
        setupLocationSharing()
        
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
            ACTION_REQUEST_STATUS -> {
                // Only broadcast status if service is actually running
                if (isRunning) {
                    broadcastStatusUpdate()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up all resources
        stopEmergencyMode()
        
        // Clean up helpers
        flashlightHelper?.cleanup()
        ultrasoundHelper?.cleanup()
        wifiDirectHelper?.cleanup()
        wifiConnectionHelper?.cleanup()
        
        // Clean up mesh routing
        bluetoothMeshTransport?.cleanup()
        
        // Clean up location sharing
        locationSharingManager?.stopLocationSharing()
        
        Log.d(TAG, "Service destroyed and resources cleaned up")
    }
    
    private fun broadcastStatusUpdate() {
        val intent = Intent(ACTION_STATUS_UPDATE)
        intent.putExtra(EXTRA_BLUETOOTH_ACTIVE, isBluetoothActive)
        intent.putExtra(EXTRA_WIFI_ACTIVE, isWifiScanningActive)
        intent.putExtra(EXTRA_HOTSPOT_ACTIVE, isHotspotActive)
        intent.putExtra(EXTRA_LOCATION_ACTIVE, locationSharingManager?.isLocationSharingActive() ?: false)
        intent.putExtra(EXTRA_WIFI_CONNECTED, isWifiConnected)
        sendBroadcast(intent)
        Log.d(TAG, "Status update broadcast: BT=$isBluetoothActive, WiFi=$isWifiScanningActive, Hotspot=$isHotspotActive, Location=${locationSharingManager?.isLocationSharingActive() ?: false}, WiFiConnected=$isWifiConnected")
    }
    
    private fun broadcastWidgetUpdate() {
        val intent = Intent(ACTION_WIDGET_UPDATE)
        sendBroadcast(intent)
        Log.d(TAG, "Widget update broadcast sent")
    }

    private fun startEmergencyMode() {
        Log.d(TAG, "Starting emergency mode")
        LogManager.logStateChange("AdHocCommService", "Emergency mode starting")
        isRunning = true
        
        // Save active state to preferences
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_IS_ACTIVE, true)
            .apply()
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Activate all communication channels - each wrapped in try-catch for robustness
        try {
            activateBluetooth()
            Log.d(TAG, "Bluetooth activated")
            LogManager.logStateChange("AdHocCommService", "Bluetooth channel activated")
        } catch (e: Exception) {
            ErrorLogger.logError(TAG, "Failed to activate Bluetooth, continuing with other channels", e)
            LogManager.logError("AdHocCommService", "Failed to activate Bluetooth: ${e.message}")
        }
        
        try {
            activateWifiScanning()
            Log.d(TAG, "WiFi scanning activated")
            LogManager.logStateChange("AdHocCommService", "WiFi scanning activated")
        } catch (e: Exception) {
            ErrorLogger.logError(TAG, "Failed to activate WiFi scanning, continuing with other channels", e)
            LogManager.logError("AdHocCommService", "Failed to activate WiFi scanning: ${e.message}")
        }
        
        try {
            activateHotspot()
            Log.d(TAG, "Hotspot activation attempted")
            LogManager.logStateChange("AdHocCommService", "Hotspot activation attempted")
        } catch (e: Exception) {
            ErrorLogger.logError(TAG, "Failed to activate hotspot, continuing with other channels", e)
            LogManager.logError("AdHocCommService", "Failed to activate hotspot: ${e.message}")
        }
        
        try {
            activateWifiDirect()
            Log.d(TAG, "WiFi Direct activated")
            LogManager.logStateChange("AdHocCommService", "WiFi Direct activated")
        } catch (e: Exception) {
            ErrorLogger.logError(TAG, "Failed to activate WiFi Direct, continuing with other channels", e)
            LogManager.logError("AdHocCommService", "Failed to activate WiFi Direct: ${e.message}")
        }
        
        // Activate mesh networking
        try {
            activateMeshNetworking()
            Log.d(TAG, "Mesh networking activated")
        } catch (e: Exception) {
            ErrorLogger.logError(TAG, "Failed to activate mesh networking, continuing with other channels", e)
        }
        
        // Activate flashlight and ultrasound signaling if enabled
        try {
            activateFlashlightSignaling()
            Log.d(TAG, "Flashlight signaling activation attempted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate flashlight signaling, continuing with other channels", e)
        }
        
        try {
            activateUltrasoundSignaling()
            Log.d(TAG, "Ultrasound signaling activation attempted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate ultrasound signaling, continuing with other channels", e)
        }
        
        // Set device ID for NFC sharing
        try {
            nfcHelper?.setDeviceId(deviceId)
            Log.d(TAG, "NFC helper device ID set")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set NFC device ID, continuing with other channels", e)
        }
        
        // Activate location sharing
        try {
            activateLocationSharing()
            Log.d(TAG, "Location sharing activation attempted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to activate location sharing, continuing with other channels", e)
        }
        
        // Broadcast local emergency
        try {
            broadcastEmergencySignal()
            Log.d(TAG, "Emergency signal broadcast")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to broadcast emergency signal, continuing with other channels", e)
        }
        
        // Send emergency SMS if enabled
        try {
            sendEmergencySms()
            Log.d(TAG, "Emergency SMS sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emergency SMS, continuing with other channels", e)
        }
        
        // Notify widgets of state change
        broadcastWidgetUpdate()
        
        // Send initial status update after a short delay to ensure all services are initialized
        handler.postDelayed({
            broadcastStatusUpdate()
        }, 100L)
        
        // Start periodic status updates independently
        handler.postDelayed(statusUpdateRunnable, STATUS_UPDATE_INTERVAL)
        
        // Start infrastructure monitoring
        handler.postDelayed(infrastructureCheckRunnable, INFRASTRUCTURE_CHECK_INTERVAL)
    }

    private fun stopEmergencyMode() {
        Log.d(TAG, "Stopping emergency mode")
        LogManager.logStateChange("AdHocCommService", "Emergency mode stopping")
        isRunning = false
        
        // Save inactive state to preferences
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_IS_ACTIVE, false)
            .apply()
        
        handler.removeCallbacks(wifiScanRunnable)
        handler.removeCallbacks(meshMaintenanceRunnable)
        handler.removeCallbacks(statusUpdateRunnable)
        handler.removeCallbacks(infrastructureCheckRunnable)
        
        safeUnregisterReceiver(wifiScanReceiver)
        safeUnregisterReceiver(bluetoothDiscoveryReceiver)
        safeUnregisterReceiver(helpRequestReceiver)
        
        deactivateBluetooth()
        deactivateWifiDirect()
        deactivateWifiConnection()
        deactivateFlashlightSignaling()
        deactivateUltrasoundSignaling()
        deactivateMeshNetworking()
        deactivateLocationSharing()
        
        // Reset all status flags
        isBluetoothActive = false
        isWifiScanningActive = false
        isHotspotActive = false
        isWifiConnected = false
        
        // Broadcast final status update
        broadcastStatusUpdate()
        
        // Notify widgets of state change
        broadcastWidgetUpdate()
    }
    
    /**
     * Safely unregisters a BroadcastReceiver, ignoring errors if it was never registered.
     */
    private fun safeUnregisterReceiver(receiver: BroadcastReceiver) {
        try {
            unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered - this is fine
        }
    }

    private fun activateBluetooth() {
        isBluetoothActive = false  // Reset before attempting activation
        
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth is disabled, cannot activate")
            broadcastStatusUpdate()
            return
        }
        
        // Check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Missing Bluetooth permissions")
                broadcastStatusUpdate()
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
        
        isBluetoothActive = true
        Log.d(TAG, "Bluetooth activated and discovery started")
        broadcastStatusUpdate()
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
        
        isBluetoothActive = false
        Log.d(TAG, "Bluetooth deactivated")
        broadcastStatusUpdate()
    }

    private fun activateWifiScanning() {
        isWifiScanningActive = false  // Reset before attempting activation
        
        // Register WiFi scan receiver
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, filter)
        
        // Register help request receiver
        val helpFilter = IntentFilter("com.fourpeople.adhoc.SEND_HELP_REQUEST")
        registerReceiver(helpRequestReceiver, helpFilter)
        
        // Start periodic WiFi scanning
        handler.post(wifiScanRunnable)
        
        isWifiScanningActive = true
        Log.d(TAG, "WiFi scanning activated")
        broadcastStatusUpdate()
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
        val emergencyNetworks = mutableListOf<String>()
        
        results.forEach { result ->
            if (result.SSID.startsWith(EMERGENCY_SSID_PATTERN)) {
                Log.d(TAG, "Emergency network detected: ${result.SSID}")
                emergencyNetworks.add(result.SSID)
                notifyEmergencyDetected(result.SSID)
            }
        }
        
        // Attempt to connect to emergency networks if auto-connect is enabled
        if (emergencyNetworks.isNotEmpty()) {
            tryConnectToEmergencyNetworks(emergencyNetworks)
        }
    }
    
    private fun tryConnectToEmergencyNetworks(emergencyNetworks: List<String>) {
        // Check if auto-connect is enabled in preferences
        val prefs = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        val autoConnectEnabled = prefs.getBoolean("wifi_auto_connect_enabled", true)
        
        if (!autoConnectEnabled) {
            Log.d(TAG, "WiFi auto-connect is disabled in settings")
            return
        }
        
        // Check if already connected to an emergency network
        if (wifiConnectionHelper?.isConnectedToEmergencyNetwork() == true) {
            val currentSsid = wifiConnectionHelper?.getCurrentNetworkSsid()
            Log.d(TAG, "Already connected to emergency network: $currentSsid")
            isWifiConnected = true
            broadcastStatusUpdate()
            return
        }
        
        // Try to connect to an available emergency network
        val success = wifiConnectionHelper?.connectToAvailableEmergencyNetwork(emergencyNetworks) ?: false
        if (success) {
            Log.d(TAG, "WiFi connection attempt initiated")
            // Note: Connection status will be updated in next WiFi scan when we verify the connection
            // We don't set isWifiConnected=true here as the connection may still fail
        } else {
            Log.w(TAG, "Failed to initiate WiFi connection")
            isWifiConnected = false
            broadcastStatusUpdate()
        }
    }

    private fun activateHotspot() {
        // Note: Creating WiFi hotspot programmatically requires system permissions
        // or is deprecated in newer Android versions. This is a simplified approach.
        // LocalOnlyHotspot creates a network with a system-generated SSID that cannot
        // be customized to follow the emergency naming pattern.
        
        isHotspotActive = false  // Reset before attempting activation
        Log.d(TAG, "Hotspot activation requested (system-generated SSID)")
        
        // On Android O and above, we can request a local-only hotspot
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                        isHotspotActive = true
                        Log.d(TAG, "Local-only hotspot started")
                        broadcastStatusUpdate()
                        // Note: SSID is system-generated and cannot be customized
                    }

                    override fun onStopped() {
                        isHotspotActive = false
                        Log.d(TAG, "Local-only hotspot stopped")
                        broadcastStatusUpdate()
                    }

                    override fun onFailed(reason: Int) {
                        isHotspotActive = false
                        Log.e(TAG, "Failed to start local-only hotspot: $reason")
                        broadcastStatusUpdate()
                    }
                }, handler)
            } catch (e: SecurityException) {
                isHotspotActive = false
                Log.e(TAG, "Security exception when starting hotspot", e)
                broadcastStatusUpdate()
            }
        } else {
            // Hotspot not available on this Android version
            // isHotspotActive already set to false at line 460
            Log.d(TAG, "Hotspot not available on Android version below O")
            broadcastStatusUpdate()
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
        // Send local broadcast to notify the app (more secure than implicit broadcasts)
        val intent = Intent("com.fourpeople.adhoc.EMERGENCY_DETECTED")
        intent.setPackage(packageName)
        intent.putExtra("source", source)
        sendBroadcast(intent)
    }

    private fun broadcastEmergencySignal() {
        Log.d(TAG, "Broadcasting emergency signal")
        
        // Broadcast emergency message through mesh network
        val emergencyPayload = "EMERGENCY! Device $deviceId requires assistance"
        meshRoutingManager?.broadcastMessage(emergencyPayload)
        
        Log.d(TAG, "Emergency signal broadcast through mesh network")
    }
    
    private fun activateWifiDirect() {
        wifiDirectHelper?.setEmergencyDeviceName(deviceId)
        wifiDirectHelper?.startDiscovery()
        Log.d(TAG, "WiFi Direct activated")
    }
    
    private fun deactivateWifiDirect() {
        wifiDirectHelper?.stopDiscovery()
        Log.d(TAG, "WiFi Direct deactivated")
    }
    
    private fun deactivateWifiConnection() {
        wifiConnectionHelper?.disconnect()
        isWifiConnected = false
        Log.d(TAG, "WiFi connection deactivated")
        broadcastStatusUpdate()
    }
    
    private fun sendEmergencySms() {
        // IMPORTANT: SMS requires cellular signaling network (MAP/SS7), NOT WiFi or mobile data
        // This will work when:
        // ✅ Mobile data is down (cellular signaling still operational)
        // ✅ Internet backbone is down (cellular signaling still operational)
        // ❌ Will NOT work when cellular network completely fails
        val message = "EMERGENCY! 4people app activated. Device ID: $deviceId. I need assistance."
        val sentCount = EmergencySmsHelper.sendEmergencySms(applicationContext, message)
        if (sentCount > 0) {
            Log.d(TAG, "Emergency SMS sent to $sentCount contacts")
        }
    }
    
    private fun activateFlashlightSignaling() {
        val prefs = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("flashlight_morse_enabled", false)
        
        if (!enabled) {
            Log.d(TAG, "Flashlight signaling is disabled in settings")
            return
        }
        
        if (flashlightHelper?.isFlashlightAvailable() == true) {
            flashlightHelper?.startEmergencyIdentificationSignal()
            Log.d(TAG, "Flashlight Morse signaling activated")
        } else {
            Log.w(TAG, "Flashlight not available on this device")
        }
    }
    
    private fun deactivateFlashlightSignaling() {
        flashlightHelper?.stopSignal()
        Log.d(TAG, "Flashlight signaling deactivated")
    }
    
    private fun activateUltrasoundSignaling() {
        val prefs = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        val transmitEnabled = prefs.getBoolean("ultrasound_transmit_enabled", false)
        val listenEnabled = prefs.getBoolean("ultrasound_listen_enabled", true)
        
        if (ultrasoundHelper?.isSupported() == true) {
            if (transmitEnabled) {
                ultrasoundHelper?.startTransmitting()
                Log.d(TAG, "Ultrasound transmission activated")
            }
            
            if (listenEnabled) {
                ultrasoundHelper?.startListening {
                    Log.i(TAG, "Ultrasound emergency signal detected!")
                    notifyEmergencyDetected("Ultrasound Signal")
                }
                Log.d(TAG, "Ultrasound listening activated")
            }
        } else {
            Log.w(TAG, "Ultrasound not supported on this device")
        }
    }
    
    private fun deactivateUltrasoundSignaling() {
        ultrasoundHelper?.stopTransmitting()
        ultrasoundHelper?.stopListening()
        Log.d(TAG, "Ultrasound signaling deactivated")
    }
    
    private fun setupMeshRouting() {
        // Set up Bluetooth mesh transport message listener
        bluetoothMeshTransport?.setMessageListener(object : BluetoothMeshTransport.MessageListener {
            override fun onMessageReceived(message: MeshMessage, senderAddress: String) {
                meshRoutingManager?.receiveMessage(message, senderAddress)
            }
        })
        
        // Set up mesh routing manager callbacks
        meshRoutingManager?.setMessageForwarder(object : MeshRoutingManager.MessageForwarder {
            override fun forwardMessage(message: MeshMessage, nextHopId: String): Boolean {
                return bluetoothMeshTransport?.sendMessage(message, nextHopId) ?: false
            }
        })
        
        meshRoutingManager?.setMessageListener(object : MeshRoutingManager.MessageListener {
            override fun onMessageReceived(message: MeshMessage) {
                handleMeshMessage(message)
            }
        })
        
        Log.d(TAG, "Mesh routing configured")
    }
    
    private fun activateMeshNetworking() {
        // Start Bluetooth mesh transport
        bluetoothMeshTransport?.startListening()
        
        // Start periodic neighbor discovery
        handler.post(meshMaintenanceRunnable)
        
        // Discover neighbors immediately
        meshRoutingManager?.discoverNeighbors()
        
        Log.d(TAG, "Mesh networking activated")
    }
    
    private fun deactivateMeshNetworking() {
        bluetoothMeshTransport?.cleanup()
        Log.d(TAG, "Mesh networking deactivated")
    }
    
    private fun performMeshMaintenance() {
        // Perform route table maintenance
        meshRoutingManager?.performMaintenance()
        
        // Discover neighbors periodically
        meshRoutingManager?.discoverNeighbors()
        
        val neighborCount = meshRoutingManager?.getNeighborCount() ?: 0
        val routeCount = meshRoutingManager?.getKnownRoutes()?.size ?: 0
        
        Log.d(TAG, "Mesh maintenance: $neighborCount neighbors, $routeCount routes")
    }
    
    private fun handleMeshMessage(message: MeshMessage) {
        Log.d(TAG, "Mesh message received from ${message.sourceId}: ${message.payload}")
        LogManager.logMessage("AdHocCommService", "Mesh message from ${message.sourceId}: ${message.messageType.name}")
        
        // Handle different message types
        when (message.messageType) {
            MeshMessage.MessageType.LOCATION_UPDATE -> {
                handleLocationUpdate(message)
            }
            MeshMessage.MessageType.HELP_REQUEST -> {
                handleHelpRequest(message)
            }
            else -> {
                // Notify emergency detection for mesh messages
                notifyEmergencyDetected("Mesh:${message.sourceId}")
                
                // Handle the message payload (could be extended for specific message types)
                when {
                    message.payload.startsWith("EMERGENCY") -> {
                        Log.i(TAG, "Emergency message relayed through mesh network")
                        LogManager.logEvent("AdHocCommService", "Emergency message via mesh from ${message.sourceId}")
                    }
                    message.payload.startsWith("HELLO") -> {
                        // Hello message for neighbor discovery
                    }
                    else -> {
                        Log.d(TAG, "Data message received: ${message.payload}")
                    }
                }
            }
        }
    }
    
    private fun handleLocationUpdate(message: MeshMessage) {
        val locationData = LocationData.fromJson(message.payload)
        if (locationData != null) {
            locationSharingManager?.updateParticipantLocation(locationData)
            Log.d(TAG, "Location updated for device ${locationData.deviceId}")
        } else {
            Log.w(TAG, "Failed to parse location data from message")
        }
    }
    
    private fun handleHelpRequest(message: MeshMessage) {
        val locationData = LocationData.fromJson(message.payload)
        if (locationData != null && locationData.isHelpRequest) {
            // Check if the event is within our radius
            val shouldProcess = locationSharingManager?.shouldProcessEvent(locationData) ?: true
            
            if (shouldProcess) {
                locationSharingManager?.updateParticipantLocation(locationData)
                val radiusInfo = if (locationData.isForwarded) " (forwarded, ${locationData.eventRadiusKm}km)" else " (${locationData.eventRadiusKm}km)"
                Log.i(TAG, "HELP REQUEST from ${locationData.deviceId}$radiusInfo: ${locationData.helpMessage}")
                LogManager.logEvent("AdHocCommService", "Help request from ${locationData.deviceId}$radiusInfo")
                
                // Notify user of help request
                notifyEmergencyDetected("HELP:${locationData.deviceId}")
            } else {
                Log.d(TAG, "Help request from ${locationData.deviceId} is outside event radius (${locationData.eventRadiusKm}km)")
            }
        } else {
            Log.w(TAG, "Failed to parse help request from message")
        }
    }
    
    private fun setupLocationSharing() {
        locationSharingManager?.setLocationUpdateListener(object : LocationSharingManager.LocationUpdateListener {
            override fun onLocationUpdate(locationData: LocationData) {
                // Broadcast location update to all participants
                val payload = locationData.toJson()
                meshRoutingManager?.broadcastMessage(payload, MeshMessage.MessageType.LOCATION_UPDATE)
                Log.d(TAG, "Location update broadcast to network")
            }
        })
    }
    
    private fun setupWifiConnectionListener() {
        wifiConnectionHelper?.setConnectionStatusListener(object : WiFiConnectionHelper.ConnectionStatusListener {
            override fun onConnectionStatusChanged(isConnected: Boolean, ssid: String?) {
                isWifiConnected = isConnected
                Log.d(TAG, "WiFi connection status changed: connected=$isConnected, ssid=$ssid")
                broadcastStatusUpdate()
            }
        })
    }
    
    private fun activateLocationSharing() {
        val success = locationSharingManager?.startLocationSharing() ?: false
        if (success) {
            Log.d(TAG, "Location sharing activated")
        } else {
            Log.w(TAG, "Failed to activate location sharing")
        }
        broadcastStatusUpdate()
    }
    
    private fun deactivateLocationSharing() {
        locationSharingManager?.stopLocationSharing()
        Log.d(TAG, "Location sharing deactivated")
        broadcastStatusUpdate()
    }
    
    private fun handleSendHelpRequest(message: String?, radiusKm: Double = 100.0) {
        val helpRequest = locationSharingManager?.sendHelpRequest(message, radiusKm)
        if (helpRequest != null) {
            // Broadcast help request to network
            val payload = helpRequest.toJson()
            meshRoutingManager?.broadcastMessage(payload, MeshMessage.MessageType.HELP_REQUEST)
            Log.i(TAG, "Help request sent to network with ${radiusKm}km radius: ${message ?: "No message"}")
        } else {
            Log.w(TAG, "Failed to send help request - location not available")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Emergency communication channel
            val name = "Emergency Communication"
            val descriptionText = "Ad-hoc emergency communication is active"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
            
            // Infrastructure alerts channel
            val infraName = "Infrastructure Alerts"
            val infraDescription = "Alerts when critical infrastructure fails"
            val infraImportance = NotificationManager.IMPORTANCE_HIGH
            val infraChannel = NotificationChannel(CHANNEL_ID_INFRASTRUCTURE, infraName, infraImportance).apply {
                description = infraDescription
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(infraChannel)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.emergency_mode_active_notification))
            .setContentText(getString(R.string.emergency_mode_active_text))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
    
    /**
     * Perform infrastructure health check and broadcast status
     */
    private fun performInfrastructureCheck() {
        val monitor = infrastructureMonitor ?: return
        
        // Update mesh network stats before checking
        val activeNodes = meshRoutingManager?.getKnownRoutes()?.size ?: 0
        val routingActive = meshRoutingManager != null
        monitor.updateMeshStats(activeNodes, routingActive)
        
        // Perform health check
        val status = monitor.checkHealth()
        
        // Broadcast infrastructure status
        val intent = Intent(ACTION_INFRASTRUCTURE_STATUS)
        intent.putExtra(EXTRA_INFRA_BLUETOOTH, status.bluetoothHealth.name)
        intent.putExtra(EXTRA_INFRA_WIFI, status.wifiHealth.name)
        intent.putExtra(EXTRA_INFRA_CELLULAR, status.cellularHealth.name)
        intent.putExtra(EXTRA_INFRA_MESH, status.meshHealth.name)
        intent.putExtra(EXTRA_INFRA_OVERALL, status.overallHealth.name)
        intent.putExtra(EXTRA_INFRA_DESCRIPTION, monitor.getStatusDescription())
        sendBroadcast(intent)
        
        // Check for critical failures and send notification if enabled
        if (monitor.hasCriticalFailure()) {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean(PREF_INFRASTRUCTURE_NOTIFICATIONS, true)
            
            if (notificationsEnabled) {
                sendInfrastructureFailureNotification(status)
            }
            
            // Also broadcast failure event
            val failureIntent = Intent(ACTION_INFRASTRUCTURE_FAILURE)
            failureIntent.putExtra(EXTRA_INFRA_OVERALL, status.overallHealth.name)
            failureIntent.putExtra(EXTRA_INFRA_DESCRIPTION, monitor.getStatusDescription())
            sendBroadcast(failureIntent)
        }
    }
    
    /**
     * Send notification when critical infrastructure fails
     */
    private fun sendInfrastructureFailureNotification(status: InfrastructureMonitor.InfrastructureStatus) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val failedComponents = mutableListOf<String>()
        if (status.bluetoothHealth == InfrastructureMonitor.HealthStatus.FAILED) {
            failedComponents.add("Bluetooth")
        }
        if (status.wifiHealth == InfrastructureMonitor.HealthStatus.FAILED) {
            failedComponents.add("WiFi")
        }
        if (status.cellularHealth == InfrastructureMonitor.HealthStatus.FAILED) {
            failedComponents.add("Cellular")
        }
        if (status.meshHealth == InfrastructureMonitor.HealthStatus.FAILED) {
            failedComponents.add("Mesh")
        }
        
        val failureText = if (failedComponents.isNotEmpty()) {
            "Failed: ${failedComponents.joinToString(", ")}"
        } else {
            "Critical infrastructure failure detected"
        }
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_INFRASTRUCTURE)
            .setContentTitle("Infrastructure Failure")
            .setContentText(failureText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3001, notification) // Use unique ID for infrastructure notifications
        
        Log.w(TAG, "Infrastructure failure notification sent: $failureText")
    }
}
