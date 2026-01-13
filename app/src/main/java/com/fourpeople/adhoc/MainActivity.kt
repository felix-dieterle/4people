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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fourpeople.adhoc.databinding.ActivityMainBinding
import com.fourpeople.adhoc.service.AdHocCommunicationService
import com.fourpeople.adhoc.service.PanicModeService
import com.fourpeople.adhoc.util.NFCHelper

/**
 * Main activity for the 4people ad-hoc communication app.
 * Allows users to activate emergency communication mode with a single click.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isEmergencyActive = false
    private var isPanicModeActive = false
    private var nfcHelper: NFCHelper? = null

    private val emergencyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.fourpeople.adhoc.EMERGENCY_DETECTED") {
                val source = intent.getStringExtra("source") ?: "unknown"
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

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            toggleEmergencyMode()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        registerEmergencyReceiver()
        setupNFC()
        handleNfcIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        nfcHelper?.enableForegroundDispatch(this)
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
    }

    private fun setupUI() {
        binding.activateButton.setOnClickListener {
            if (checkPermissions()) {
                toggleEmergencyMode()
            } else {
                requestPermissions()
            }
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

        binding.panicModeButton.setOnClickListener {
            togglePanicMode()
        }

        updateUI()
    }

    private fun registerEmergencyReceiver() {
        val filter = IntentFilter("com.fourpeople.adhoc.EMERGENCY_DETECTED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(emergencyReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(emergencyReceiver, filter)
        }
    }

    private fun toggleEmergencyMode() {
        isEmergencyActive = !isEmergencyActive

        val intent = Intent(this, AdHocCommunicationService::class.java)
        if (isEmergencyActive) {
            intent.action = AdHocCommunicationService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            intent.action = AdHocCommunicationService.ACTION_STOP
            startService(intent)
        }

        updateUI()
    }

    private fun updateUI() {
        if (isEmergencyActive) {
            binding.statusTextView.text = getString(R.string.emergency_active)
            binding.activateButton.text = getString(R.string.deactivate_emergency)
            binding.bluetoothStatusTextView.text = getString(R.string.bluetooth_status, getString(R.string.active))
            binding.wifiStatusTextView.text = getString(R.string.wifi_status, getString(R.string.active))
            binding.hotspotStatusTextView.text = getString(R.string.hotspot_status, getString(R.string.active))
            binding.locationStatusTextView.text = getString(R.string.location_sharing_status, getString(R.string.active))
            binding.viewLocationsButton.isEnabled = true
            binding.sendHelpButton.isEnabled = true
        } else {
            binding.statusTextView.text = getString(R.string.emergency_inactive)
            binding.activateButton.text = getString(R.string.activate_emergency)
            binding.bluetoothStatusTextView.text = getString(R.string.bluetooth_status, getString(R.string.inactive))
            binding.wifiStatusTextView.text = getString(R.string.wifi_status, getString(R.string.inactive))
            binding.hotspotStatusTextView.text = getString(R.string.hotspot_status, getString(R.string.inactive))
            binding.locationStatusTextView.text = getString(R.string.location_sharing_status, getString(R.string.inactive))
            binding.viewLocationsButton.isEnabled = false
            binding.sendHelpButton.isEnabled = false
        }
    }

    private fun checkPermissions(): Boolean {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val requiredPermissions = getRequiredPermissions()
        
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

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_explanation)
            .setPositiveButton(R.string.grant_permissions) { _, _ ->
                requestPermissionsLauncher.launch(getRequiredPermissions().toTypedArray())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.permission_explanation)
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

        // Show dialog to enter help message
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.send_help_request)
        builder.setMessage(R.string.help_request_message)

        val input = android.widget.EditText(this)
        input.hint = getString(R.string.help_request_message)
        builder.setView(input)

        builder.setPositiveButton(android.R.string.ok) { _, _ ->
            val message = input.text.toString().takeIf { it.isNotEmpty() }
            
            // Send help request via broadcast to service
            val intent = Intent("com.fourpeople.adhoc.SEND_HELP_REQUEST")
            intent.setPackage(packageName)
            message?.let { intent.putExtra("help_message", it) }
            sendBroadcast(intent)
            
            Toast.makeText(this, R.string.help_request_sent, Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton(android.R.string.cancel, null)
        builder.show()
    }

    private fun togglePanicMode() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        isPanicModeActive = !isPanicModeActive

        val intent = Intent(this, PanicModeService::class.java)
        if (isPanicModeActive) {
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
                    intent.action = PanicModeService.ACTION_START
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent)
                    } else {
                        startService(intent)
                    }
                    updatePanicModeUI()
                    Toast.makeText(this, R.string.panic_active, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.no) { _, _ ->
                    isPanicModeActive = false
                }
                .show()
        } else {
            intent.action = PanicModeService.ACTION_STOP
            startService(intent)
            updatePanicModeUI()
            Toast.makeText(this, R.string.panic_inactive, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePanicModeUI() {
        if (isPanicModeActive) {
            binding.panicModeButton.text = getString(R.string.deactivate_panic)
            binding.panicModeButton.backgroundTintList = 
                ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark)
        } else {
            binding.panicModeButton.text = getString(R.string.activate_panic)
            binding.panicModeButton.backgroundTintList = 
                ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
        }
    }
}
