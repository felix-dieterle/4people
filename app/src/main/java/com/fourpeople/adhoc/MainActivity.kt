package com.fourpeople.adhoc

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fourpeople.adhoc.databinding.ActivityMainBinding
import com.fourpeople.adhoc.service.AdHocCommunicationService
import com.fourpeople.adhoc.util.ErrorLogger
import com.fourpeople.adhoc.util.LogManager
import com.fourpeople.adhoc.util.NFCHelper

/**
 * Main activity for the 4people ad-hoc communication app.
 * Allows users to activate emergency communication mode with a single click.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isEmergencyActive = false
    private var nfcHelper: NFCHelper? = null
    private var pendingEmergencyActivation = false
    
    // Status tracking for individual features
    private var isBluetoothActive = false
    private var isWifiActive = false
    private var isHotspotActive = false
    private var isLocationActive = false
    private var isWifiConnected = false
    
    // Infrastructure health tracking
    private var infraBluetoothHealth = "UNKNOWN"
    private var infraWifiHealth = "UNKNOWN"
    private var infraCellularHealth = "UNKNOWN"
    private var infraMeshHealth = "UNKNOWN"
    private var infraOverallHealth = "UNKNOWN"

    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fourpeople.adhoc.EMERGENCY_DETECTED") {
                val source = intent.getStringExtra("source") ?: "unknown"
                LogManager.logMessage("MainActivity", "Emergency detected from: $source")
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Emergency detected: $source",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    private val statusUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AdHocCommunicationService.ACTION_STATUS_UPDATE) {
                val bluetoothChanged = isBluetoothActive != intent.getBooleanExtra(AdHocCommunicationService.EXTRA_BLUETOOTH_ACTIVE, false)
                val wifiChanged = isWifiActive != intent.getBooleanExtra(AdHocCommunicationService.EXTRA_WIFI_ACTIVE, false)
                val hotspotChanged = isHotspotActive != intent.getBooleanExtra(AdHocCommunicationService.EXTRA_HOTSPOT_ACTIVE, false)
                val locationChanged = isLocationActive != intent.getBooleanExtra(AdHocCommunicationService.EXTRA_LOCATION_ACTIVE, false)
                val wifiConnectedChanged = isWifiConnected != intent.getBooleanExtra(AdHocCommunicationService.EXTRA_WIFI_CONNECTED, false)
                
                isBluetoothActive = intent.getBooleanExtra(AdHocCommunicationService.EXTRA_BLUETOOTH_ACTIVE, false)
                isWifiActive = intent.getBooleanExtra(AdHocCommunicationService.EXTRA_WIFI_ACTIVE, false)
                isHotspotActive = intent.getBooleanExtra(AdHocCommunicationService.EXTRA_HOTSPOT_ACTIVE, false)
                isLocationActive = intent.getBooleanExtra(AdHocCommunicationService.EXTRA_LOCATION_ACTIVE, false)
                isWifiConnected = intent.getBooleanExtra(AdHocCommunicationService.EXTRA_WIFI_CONNECTED, false)
                
                // Log state changes
                if (bluetoothChanged) {
                    LogManager.logStateChange("MainActivity", "Bluetooth: ${if (isBluetoothActive) "active" else "inactive"}")
                }
                if (wifiChanged) {
                    LogManager.logStateChange("MainActivity", "WiFi: ${if (isWifiActive) "active" else "inactive"}")
                }
                if (hotspotChanged) {
                    LogManager.logStateChange("MainActivity", "Hotspot: ${if (isHotspotActive) "active" else "inactive"}")
                }
                if (locationChanged) {
                    LogManager.logStateChange("MainActivity", "Location: ${if (isLocationActive) "active" else "inactive"}")
                }
                if (wifiConnectedChanged) {
                    LogManager.logStateChange("MainActivity", "WiFi connection: ${if (isWifiConnected) "connected" else "disconnected"}")
                }
                
                runOnUiThread {
                    updateStatusUI()
                }
            }
        }
    }
    
    private val infrastructureStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AdHocCommunicationService.ACTION_INFRASTRUCTURE_STATUS -> {
                    infraBluetoothHealth = intent.getStringExtra(AdHocCommunicationService.EXTRA_INFRA_BLUETOOTH) ?: "UNKNOWN"
                    infraWifiHealth = intent.getStringExtra(AdHocCommunicationService.EXTRA_INFRA_WIFI) ?: "UNKNOWN"
                    infraCellularHealth = intent.getStringExtra(AdHocCommunicationService.EXTRA_INFRA_CELLULAR) ?: "UNKNOWN"
                    infraMeshHealth = intent.getStringExtra(AdHocCommunicationService.EXTRA_INFRA_MESH) ?: "UNKNOWN"
                    infraOverallHealth = intent.getStringExtra(AdHocCommunicationService.EXTRA_INFRA_OVERALL) ?: "UNKNOWN"
                    LogManager.logStateChange("MainActivity", "Infrastructure status updated: $infraOverallHealth")
                    runOnUiThread {
                        updateInfrastructureStatusUI()
                    }
                }
                AdHocCommunicationService.ACTION_INFRASTRUCTURE_FAILURE -> {
                    val description = intent.getStringExtra(AdHocCommunicationService.EXTRA_INFRA_DESCRIPTION) ?: "Infrastructure failure detected"
                    LogManager.logWarning("MainActivity", "Infrastructure failure: $description")
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "⚠ Infrastructure Failure Detected",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if all mandatory permissions are granted (SMS is optional)
        val mandatoryPermissions = getMandatoryPermissions().filter {
            !shouldFilterBackgroundLocation(it)
        }
        val allMandatoryGranted = mandatoryPermissions.all { permission ->
            permissions[permission] == true
        }
        
        if (allMandatoryGranted) {
            // Check if we need to request background location separately
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                !hasBackgroundLocationPermission()) {
                requestBackgroundLocationPermission()
            } else if (pendingEmergencyActivation) {
                pendingEmergencyActivation = false
                toggleEmergencyMode()
            }
        } else {
            if (pendingEmergencyActivation) {
                pendingEmergencyActivation = false
                showPermissionDeniedDialog()
            }
        }
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (pendingEmergencyActivation) {
            pendingEmergencyActivation = false
            if (checkPermissions()) {
                toggleEmergencyMode()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        LogManager.logInfo("MainActivity", "Application started")
        
        setupUI()
        registerEmergencyReceiver()
        setupNFC()
        handleNfcIntent(intent)
        
        // Request all permissions on startup to ensure they're available
        // for critical services (standby monitoring, boot receiver, etc.)
        requestPermissionsOnStartup()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        nfcHelper?.enableForegroundDispatch(this)
        
        // Check if emergency mode service is running and request status update
        isEmergencyActive = AdHocCommunicationService.isActive(this)
        if (isEmergencyActive) {
            requestServiceStatusUpdate()
        }
        
        // Update UI to reflect current state
        updateUI()
        
        // Check if permissions were revoked and update UI accordingly
        if (!checkPermissions()) {
            // Update UI to reflect that permissions are needed
            updateUI()
        }
    }
    
    override fun onPause() {
        super.onPause()
        nfcHelper?.disableForegroundDispatch(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(emergencyReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        try {
            unregisterReceiver(statusUpdateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        try {
            unregisterReceiver(infrastructureStatusReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }

    private fun setupUI() {
        binding.activateButton.setOnClickListener {
            if (checkPermissions()) {
                toggleEmergencyMode()
            } else {
                pendingEmergencyActivation = true
                requestPermissions()
            }
        }

        binding.emergencyHelpButton.setOnClickListener {
            showEmergencyModeHelp()
        }

        binding.logButton.setOnClickListener {
            startActivity(Intent(this, LogWindowActivity::class.java))
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.viewLocationsButton.setOnClickListener {
            startActivity(Intent(this, LocationMapActivity::class.java))
        }

        binding.sendHelpButton.setOnClickListener {
            sendHelpRequest()
        }

        binding.simulationButton.setOnClickListener {
            startActivity(Intent(this, SimulationActivity::class.java))
        }

        updateUI()
    }

    private fun requestPermissionsOnStartup() {
        // Check if we already have all permissions
        if (checkPermissions()) {
            return
        }
        
        // Request permissions on startup to ensure they're available for background services
        requestPermissions()
    }

    private fun registerEmergencyReceiver() {
        val filter = IntentFilter("com.fourpeople.adhoc.EMERGENCY_DETECTED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(emergencyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(emergencyReceiver, filter)
        }
        
        // Register status update receiver
        val statusFilter = IntentFilter(AdHocCommunicationService.ACTION_STATUS_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusUpdateReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusUpdateReceiver, statusFilter)
        }
        
        // Register infrastructure status receiver
        val infraFilter = IntentFilter().apply {
            addAction(AdHocCommunicationService.ACTION_INFRASTRUCTURE_STATUS)
            addAction(AdHocCommunicationService.ACTION_INFRASTRUCTURE_FAILURE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(infrastructureStatusReceiver, infraFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(infrastructureStatusReceiver, infraFilter)
        }
    }

    private fun toggleEmergencyMode() {
        isEmergencyActive = !isEmergencyActive

        val intent = Intent(this, AdHocCommunicationService::class.java)
        if (isEmergencyActive) {
            LogManager.logEvent("MainActivity", "Emergency mode activated by user")
            intent.action = AdHocCommunicationService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            LogManager.logEvent("MainActivity", "Emergency mode deactivated by user")
            intent.action = AdHocCommunicationService.ACTION_STOP
            startService(intent)
        }

        updateUI()
    }

    private fun updateUI() {
        if (isEmergencyActive) {
            binding.statusTextView.text = "🟢 ${getString(R.string.active_mode)}"
            binding.statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            binding.statusTextView.textSize = 20f
            binding.activateButton.text = getString(R.string.deactivate_emergency)
            binding.activateButton.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark)
            binding.viewLocationsButton.isEnabled = true
            binding.sendHelpButton.isEnabled = true
            binding.detailsLayout.visibility = View.VISIBLE
            binding.scanningTextView.visibility = View.GONE
        } else {
            binding.statusTextView.text = "⚪ ${getString(R.string.standby_mode)}"
            binding.statusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            binding.statusTextView.textSize = 18f
            binding.activateButton.text = getString(R.string.activate_emergency)
            binding.activateButton.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
            binding.viewLocationsButton.isEnabled = false
            binding.sendHelpButton.isEnabled = false
            binding.detailsLayout.visibility = View.GONE
            binding.scanningTextView.visibility = View.VISIBLE
            
            // Reset status flags when emergency is deactivated
            isBluetoothActive = false
            isWifiActive = false
            isHotspotActive = false
            isLocationActive = false
            isWifiConnected = false
        }
        
        // Update individual feature statuses
        updateStatusUI()
    }
    
    private fun updateStatusUI() {
        // Update Bluetooth status
        if (isBluetoothActive) {
            binding.bluetoothStatusTextView.text = "✓ ${getString(R.string.bluetooth_status, getString(R.string.active))}"
            binding.bluetoothStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.bluetoothStatusTextView.text = "○ ${getString(R.string.bluetooth_status, getString(R.string.inactive))}"
            binding.bluetoothStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
        
        // Update WiFi status
        if (isWifiActive) {
            binding.wifiStatusTextView.text = "✓ ${getString(R.string.wifi_status, getString(R.string.active))}"
            binding.wifiStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.wifiStatusTextView.text = "○ ${getString(R.string.wifi_status, getString(R.string.inactive))}"
            binding.wifiStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
        
        // Update Hotspot status
        if (isHotspotActive) {
            binding.hotspotStatusTextView.text = "✓ ${getString(R.string.hotspot_status, getString(R.string.active))}"
            binding.hotspotStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.hotspotStatusTextView.text = "○ ${getString(R.string.hotspot_status, getString(R.string.inactive))}"
            binding.hotspotStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
        
        // Update WiFi Connection status
        if (isWifiConnected) {
            binding.wifiConnectionStatusTextView.text = "✓ ${getString(R.string.wifi_connection_status, getString(R.string.active))}"
            binding.wifiConnectionStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.wifiConnectionStatusTextView.text = "○ ${getString(R.string.wifi_connection_status, getString(R.string.inactive))}"
            binding.wifiConnectionStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
        
        // Update Location sharing status
        if (isLocationActive) {
            binding.locationStatusTextView.text = "✓ ${getString(R.string.location_sharing_status, getString(R.string.active))}"
            binding.locationStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.locationStatusTextView.text = "○ ${getString(R.string.location_sharing_status, getString(R.string.inactive))}"
            binding.locationStatusTextView.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }
    
    private fun updateInfrastructureStatusUI() {
        // Update Bluetooth infrastructure status
        val btStatus = getHealthStatusString(infraBluetoothHealth)
        val btColor = getHealthStatusColor(infraBluetoothHealth)
        binding.infraBluetoothStatusTextView.text = getString(R.string.infra_bluetooth_status, btStatus)
        binding.infraBluetoothStatusTextView.setTextColor(ContextCompat.getColor(this, btColor))
        
        // Update WiFi infrastructure status
        val wifiStatus = getHealthStatusString(infraWifiHealth)
        val wifiColor = getHealthStatusColor(infraWifiHealth)
        binding.infraWifiStatusTextView.text = getString(R.string.infra_wifi_status, wifiStatus)
        binding.infraWifiStatusTextView.setTextColor(ContextCompat.getColor(this, wifiColor))
        
        // Update Cellular infrastructure status
        val cellStatus = getHealthStatusString(infraCellularHealth)
        val cellColor = getHealthStatusColor(infraCellularHealth)
        binding.infraCellularStatusTextView.text = getString(R.string.infra_cellular_status, cellStatus)
        binding.infraCellularStatusTextView.setTextColor(ContextCompat.getColor(this, cellColor))
        
        // Update Mesh infrastructure status
        val meshStatus = getHealthStatusString(infraMeshHealth)
        val meshColor = getHealthStatusColor(infraMeshHealth)
        binding.infraMeshStatusTextView.text = getString(R.string.infra_mesh_status, meshStatus)
        binding.infraMeshStatusTextView.setTextColor(ContextCompat.getColor(this, meshColor))
        
        // Update Overall infrastructure status
        val overallStatus = getHealthStatusString(infraOverallHealth)
        val overallColor = getHealthStatusColor(infraOverallHealth)
        binding.infraOverallStatusTextView.text = getString(R.string.infra_overall_status, overallStatus)
        binding.infraOverallStatusTextView.setTextColor(ContextCompat.getColor(this, overallColor))
    }
    
    private fun getHealthStatusString(health: String): String {
        return when (health) {
            "HEALTHY" -> getString(R.string.infra_healthy)
            "DEGRADED" -> getString(R.string.infra_degraded)
            "FAILED" -> getString(R.string.infra_failed)
            else -> getString(R.string.infra_unknown)
        }
    }
    
    private fun getHealthStatusColor(health: String): Int {
        return when (health) {
            "HEALTHY" -> android.R.color.holo_green_dark
            "DEGRADED" -> android.R.color.holo_orange_dark
            "FAILED" -> android.R.color.holo_red_dark
            else -> android.R.color.darker_gray
        }
    }

    private fun checkPermissions(): Boolean {
        // Check only mandatory permissions (excluding optional ones like SMS)
        val mandatoryPermissions = getMandatoryPermissions().filter {
            !shouldFilterBackgroundLocation(it)
        }
        
        val foregroundPermissionsGranted = mandatoryPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        // For Android 10+, also check background location separately
        val backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasBackgroundLocationPermission()
        } else {
            true
        }
        
        return foregroundPermissionsGranted && backgroundLocationGranted
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true // Not required on older versions
    }
    
    private fun shouldFilterBackgroundLocation(permission: String): Boolean {
        // Background location is checked separately on Android 10+
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
               permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Show rationale for background location
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.background_location_explanation)
                .setPositiveButton(R.string.grant_permissions) { _, _ ->
                    requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun requestPermissions() {
        // Request all permissions including optional ones (like SMS)
        // Even though some permissions are optional for mode activation,
        // we still request them to enable enhanced features like SMS alerts
        val requiredPermissions = getRequiredPermissions().filter {
            !shouldFilterBackgroundLocation(it)
        }
        
        if (requiredPermissions.isNotEmpty() && shouldShowRequestPermissionRationale(requiredPermissions.first())) {
            showPermissionRationaleDialog()
        } else {
            requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        return permissions
    }

    /**
     * Get list of permissions that are mandatory for the app to function.
     * This excludes optional permissions like SMS which enhance functionality but are not required.
     */
    private fun getMandatoryPermissions(): List<String> {
        // Get all required permissions and filter out optional ones
        return getRequiredPermissions().filter { permission ->
            // SMS is optional - user can still use the app without it
            permission != Manifest.permission.SEND_SMS
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_detailed_explanation)
            .setPositiveButton(R.string.grant_permissions) { _, _ ->
                val requiredPermissions = getRequiredPermissions().filter {
                    !shouldFilterBackgroundLocation(it)
                }
                requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_denied_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
    
    private fun setupNFC() {
        nfcHelper = NFCHelper(this)
        if (nfcHelper?.initialize() == true) {
            // NFC is available - set a placeholder device ID
            // The actual device ID will be updated when emergency mode is activated
            nfcHelper?.setDeviceId(NFCHelper.DEVICE_ID_PLACEHOLDER)
        }
    }
    
    private fun handleNfcIntent(intent: Intent?) {
        if (intent?.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val message = rawMessages[0] as NdefMessage
                val credentials = nfcHelper?.parseNdefMessage(message)
                
                if (credentials != null && credentials.isValid()) {
                    onNetworkCredentialsReceived(credentials)
                } else {
                    Toast.makeText(
                        this,
                        "Invalid or expired NFC credentials",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun onNetworkCredentialsReceived(credentials: NFCHelper.NetworkCredentials) {
        val networkSsid = credentials.getNetworkSsid()
        
        AlertDialog.Builder(this)
            .setTitle("NFC Tap-to-Join")
            .setMessage("Emergency network detected via NFC tap:\n\n" +
                    "Network: $networkSsid\n\n" +
                    "Do you want to activate emergency mode and join this network?")
            .setPositiveButton("Join Network") { _, _ ->
                // Activate emergency mode to join the network
                if (checkPermissions()) {
                    if (!isEmergencyActive) {
                        toggleEmergencyMode()
                    }
                    Toast.makeText(
                        this,
                        "Joining emergency network: $networkSsid",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    pendingEmergencyActivation = true
                    requestPermissions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendHelpRequest() {
        if (!isEmergencyActive) {
            Toast.makeText(this, "Please activate emergency mode first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show dialog to enter help message and radius
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.send_help_request)
        
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)
        
        val messageInput = android.widget.EditText(this)
        messageInput.hint = getString(R.string.help_request_message)
        layout.addView(messageInput)
        
        val radiusLabel = android.widget.TextView(this)
        radiusLabel.text = getString(R.string.event_radius_label)
        radiusLabel.setPadding(0, 20, 0, 5)
        layout.addView(radiusLabel)
        
        val radiusInput = android.widget.EditText(this)
        radiusInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        val prefs = getSharedPreferences("emergency_prefs", MODE_PRIVATE)
        val defaultRadius = prefs.getFloat("default_event_radius_km", 100.0f)
        radiusInput.setText(defaultRadius.toString())
        radiusInput.hint = "100"
        layout.addView(radiusInput)
        
        builder.setView(layout)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val message = messageInput.text.toString().takeIf { it.isNotEmpty() }
            val radiusStr = radiusInput.text.toString()
            val radiusKm = radiusStr.toDoubleOrNull()?.coerceIn(1.0, 1000.0) ?: defaultRadius.toDouble()
            
            LogManager.logEvent("MainActivity", "Help request sent with radius: ${radiusKm}km")
            
            // Send help request via broadcast to service
            val intent = Intent("com.fourpeople.adhoc.SEND_HELP_REQUEST")
            intent.setPackage(packageName)
            message?.let { intent.putExtra("help_message", it) }
            intent.putExtra("event_radius_km", radiusKm)
            sendBroadcast(intent)
            
            Toast.makeText(this, getString(R.string.help_request_sent_with_radius, radiusKm), Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun requestServiceStatusUpdate() {
        try {
            val intent = Intent(this, AdHocCommunicationService::class.java)
            intent.action = AdHocCommunicationService.ACTION_REQUEST_STATUS
            startService(intent)
        } catch (e: SecurityException) {
            ErrorLogger.logError("MainActivity", "Failed to request service status update", e)
        } catch (e: Exception) {
            ErrorLogger.logError("MainActivity", "Unexpected error requesting service status", e)
        }
    }

    private fun showEmergencyModeHelp() {
        val intent = Intent(this, HelpActivity::class.java)
        intent.putExtra(HelpActivity.EXTRA_INITIAL_TAB, HelpActivity.TAB_EMERGENCY_MODE)
        startActivity(intent)
    }
}
