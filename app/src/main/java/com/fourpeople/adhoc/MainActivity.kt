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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.fourpeople.adhoc.databinding.ActivityMainTabsBinding
import com.fourpeople.adhoc.databinding.FragmentEmergencyBinding
import com.fourpeople.adhoc.databinding.FragmentPanicBinding
import com.fourpeople.adhoc.service.AdHocCommunicationService
import com.fourpeople.adhoc.service.PanicModeService
import com.fourpeople.adhoc.util.ErrorLogger
import com.fourpeople.adhoc.util.LogManager
import com.fourpeople.adhoc.util.NFCHelper
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

/**
 * Main activity for the 4people ad-hoc communication app.
 * Uses tab-based UI to separate Emergency and Panic modes.
 * Displays activity log at the bottom, visible across all tabs.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainTabsBinding
    private var isEmergencyActive = false
    private var isPanicModeActive = false
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
    
    // Log adapter
    private lateinit var logAdapter: LogAdapter
    private var isLogExpanded = false
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
        binding = ActivityMainTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        LogManager.logInfo("MainActivity", "Application started with tab-based UI")
        
        setupUI()
        setupTabs()
        setupLogView()
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
        
        // Check if panic mode service is actually running and update state
        isPanicModeActive = PanicModeService.isActive(this)
        updatePanicModeUI()
        
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
        LogManager.removeListener(logListener)
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
        binding.logButton.setOnClickListener {
            startActivity(Intent(this, LogWindowActivity::class.java))
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun setupTabs() {
        // Set up ViewPager2 with adapter
        val adapter = MainPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        // Link TabLayout with ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Emergency"
                1 -> "Panic"
                else -> ""
            }
        }.attach()
        
        LogManager.logEvent("MainActivity", "Tab-based UI initialized")
    }
    
    private fun setupLogView() {
        // Initialize log RecyclerView
        binding.logRecyclerView.layoutManager = LinearLayoutManager(this)
        logAdapter = LogAdapter(LogManager.getLogEntries().toMutableList())
        binding.logRecyclerView.adapter = logAdapter
        
        // Scroll to bottom
        if (logAdapter.itemCount > 0) {
            binding.logRecyclerView.scrollToPosition(logAdapter.itemCount - 1)
        }
        
        // Register listener for new log entries
        LogManager.addListener(logListener)
        
        // Setup expand/collapse button
        binding.expandLogButton.setOnClickListener {
            toggleLogExpansion()
        }
        
        LogManager.logEvent("MainActivity", "Persistent log view initialized")
    }
    
    private fun toggleLogExpansion() {
        isLogExpanded = !isLogExpanded
        val layoutParams = binding.logRecyclerView.layoutParams
        if (isLogExpanded) {
            layoutParams.height = resources.displayMetrics.heightPixels / 2
            binding.expandLogButton.setImageResource(android.R.drawable.arrow_down_float)
        } else {
            layoutParams.height = (120 * resources.displayMetrics.density).toInt()
            binding.expandLogButton.setImageResource(android.R.drawable.arrow_up_float)
        }
        binding.logRecyclerView.layoutParams = layoutParams
    }
    
    private val logListener = object : LogManager.LogListener {
        override fun onNewLogEntry(entry: LogManager.LogEntry) {
            runOnUiThread {
                logAdapter.addEntry(entry)
                // Auto-scroll to bottom
                binding.logRecyclerView.scrollToPosition(logAdapter.itemCount - 1)
            }
        }
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
        // Update emergency fragment if it exists
        updateEmergencyFragment()
        // Update panic fragment if it exists
        updatePanicFragment()
    }
    
    private fun updateEmergencyFragment() {
        val fragment = supportFragmentManager.findFragmentByTag("f0") as? EmergencyFragment
        fragment?.updateUI(
            isEmergencyActive,
            isBluetoothActive,
            isWifiActive,
            isHotspotActive,
            isLocationActive,
            isWifiConnected,
            infraBluetoothHealth,
            infraWifiHealth,
            infraCellularHealth,
            infraMeshHealth,
            infraOverallHealth
        )
    }
    
    private fun updatePanicFragment() {
        val fragment = supportFragmentManager.findFragmentByTag("f1") as? PanicFragment
        fragment?.updateUI(isPanicModeActive)
    }
    
    /**
     * Called by EmergencyFragment to update its UI with binding
     */
    fun updateEmergencyUI(binding: FragmentEmergencyBinding) {
        // Just trigger the fragment update which will use its own binding
        updateEmergencyFragment()
    }
    
    /**
     * Called by PanicFragment to update its UI with binding
     */
    fun updatePanicUI(binding: FragmentPanicBinding) {
        // Just trigger the fragment update which will use its own binding
        updatePanicFragment()
    }
    
    private fun updateStatusUI() {
        // Status UI is now handled by fragments
        updateEmergencyFragment()
    }
    
    private fun updateInfrastructureStatusUI() {
        // Infrastructure status UI is now handled by fragments
        updateEmergencyFragment()
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

    private fun togglePanicMode() {
        if (!checkPermissions()) {
            // Panic mode requires permissions but not emergency mode activation
            // Show a specific message for panic mode
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_required)
                .setMessage(R.string.panic_permission_message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requestPermissions()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        val intent = Intent(this, PanicModeService::class.java)
        if (!isPanicModeActive) {
            // Show confirmation dialog before activating panic mode
            AlertDialog.Builder(this)
                .setTitle(R.string.activate_panic)
                .setMessage("Activating panic mode will:\n\n" +
                        "• Request confirmation every 30 seconds\n" +
                        "• Alert with vibration if no confirmation\n" +
                        "• Escalate to full alarm if still no response\n" +
                        "• Notify emergency contacts if necessary\n\n" +
                        "Are you sure you want to activate panic mode?")
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    isPanicModeActive = true
                    LogManager.logEvent("MainActivity", "Panic mode activated by user")
                    intent.action = PanicModeService.ACTION_START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    updatePanicModeUI()
                    Toast.makeText(this, R.string.panic_active, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
        } else {
            isPanicModeActive = false
            LogManager.logEvent("MainActivity", "Panic mode deactivated by user")
            intent.action = PanicModeService.ACTION_STOP
            startService(intent)
            updatePanicModeUI()
            Toast.makeText(this, R.string.panic_inactive, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePanicModeUI() {
        updatePanicFragment()
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

    private fun showPanicModeHelp() {
        val intent = Intent(this, HelpActivity::class.java)
        intent.putExtra(HelpActivity.EXTRA_INITIAL_TAB, HelpActivity.TAB_PANIC_MODE)
        startActivity(intent)
    }
    
    // Public methods for fragments to call
    
    /**
     * Handle emergency button click from EmergencyFragment
     */
    fun handleEmergencyButtonClick() {
        if (checkPermissions()) {
            toggleEmergencyMode()
        } else {
            pendingEmergencyActivation = true
            requestPermissions()
        }
    }
    
    /**
     * Log adapter for displaying logs in the bottom panel
     */
    private class LogAdapter(
        private val entries: MutableList<LogManager.LogEntry>
    ) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
        
        class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView: android.widget.TextView = view as android.widget.TextView
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
            val textView = android.widget.TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 4, 8, 4)
                textSize = 12f
            }
            return LogViewHolder(textView)
        }
        
        override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
            val entry = entries[position]
            holder.textView.text = entry.getFormattedEntry()
            
            // Color code by level
            val colorResId = when (entry.level) {
                LogManager.LogLevel.ERROR -> android.R.color.holo_red_dark
                LogManager.LogLevel.WARNING -> android.R.color.holo_orange_dark
                LogManager.LogLevel.EVENT -> android.R.color.holo_blue_dark
                else -> android.R.color.darker_gray
            }
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.context, colorResId))
        }
        
        override fun getItemCount(): Int = entries.size
        
        fun addEntry(entry: LogManager.LogEntry) {
            entries.add(entry)
            notifyItemInserted(entries.size - 1)
        }
    }
}
