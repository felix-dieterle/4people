package com.fourpeople.adhoc

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fourpeople.adhoc.databinding.ActivityMainBinding
import com.fourpeople.adhoc.service.AdHocCommunicationService

/**
 * Main activity for the 4people ad-hoc communication app.
 * Allows users to activate emergency communication mode with a single click.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isEmergencyActive = false

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

        updateUI()
    }

    private fun registerEmergencyReceiver() {
        val filter = IntentFilter("com.fourpeople.adhoc.EMERGENCY_DETECTED")
        registerReceiver(emergencyReceiver, filter)
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
        } else {
            binding.statusTextView.text = getString(R.string.emergency_inactive)
            binding.activateButton.text = getString(R.string.activate_emergency)
            binding.bluetoothStatusTextView.text = getString(R.string.bluetooth_status, getString(R.string.inactive))
            binding.wifiStatusTextView.text = getString(R.string.wifi_status, getString(R.string.inactive))
            binding.hotspotStatusTextView.text = getString(R.string.hotspot_status, getString(R.string.inactive))
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
            Manifest.permission.SEND_SMS
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
}
