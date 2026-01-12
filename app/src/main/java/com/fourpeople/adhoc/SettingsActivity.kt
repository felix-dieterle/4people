package com.fourpeople.adhoc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fourpeople.adhoc.databinding.ActivitySettingsBinding
import com.fourpeople.adhoc.service.StandbyMonitoringService
import com.fourpeople.adhoc.util.EmergencySmsHelper

/**
 * Settings activity for configuring emergency communication behavior.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        binding.autoActivateSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveAutoActivateSetting(isChecked)
        }

        binding.standbyMonitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
            toggleStandbyMonitoring(isChecked)
        }
        
        binding.smsEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            EmergencySmsHelper.setSmsEnabled(this, isChecked)
        }
        
        binding.configureContactsButton.setOnClickListener {
            showConfigureContactsDialog()
        }
        
        // Flashlight Morse code setting
        binding.flashlightMorseSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveFlashlightMorseSetting(isChecked)
        }
        
        // Ultrasound signaling settings
        binding.ultrasoundTransmitSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveUltrasoundTransmitSetting(isChecked)
        }
        
        binding.ultrasoundListenSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveUltrasoundListenSetting(isChecked)
        }
    }

    private fun loadSettings() {
        val preferences = getSharedPreferences(StandbyMonitoringService.PREF_NAME, Context.MODE_PRIVATE)
        val autoActivate = preferences.getBoolean(StandbyMonitoringService.PREF_AUTO_ACTIVATE, false)
        
        binding.autoActivateSwitch.isChecked = autoActivate
        
        // Check if standby monitoring is enabled (we can check if service is running)
        // For simplicity, we'll default to false
        binding.standbyMonitoringSwitch.isChecked = false
        
        // Load SMS settings
        binding.smsEnabledSwitch.isChecked = EmergencySmsHelper.isSmsEnabled(this)
        updateContactsDisplay()
        
        // Load flashlight and ultrasound settings
        val emergencyPrefs = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        binding.flashlightMorseSwitch.isChecked = emergencyPrefs.getBoolean("flashlight_morse_enabled", false)
        binding.ultrasoundTransmitSwitch.isChecked = emergencyPrefs.getBoolean("ultrasound_transmit_enabled", false)
        binding.ultrasoundListenSwitch.isChecked = emergencyPrefs.getBoolean("ultrasound_listen_enabled", true)
    }

    private fun saveAutoActivateSetting(enabled: Boolean) {
        val preferences = getSharedPreferences(StandbyMonitoringService.PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit().putBoolean(StandbyMonitoringService.PREF_AUTO_ACTIVATE, enabled).apply()
    }

    private fun toggleStandbyMonitoring(enabled: Boolean) {
        val intent = Intent(this, StandbyMonitoringService::class.java)
        
        if (enabled) {
            intent.action = StandbyMonitoringService.ACTION_START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            intent.action = StandbyMonitoringService.ACTION_STOP
            startService(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    private fun showConfigureContactsDialog() {
        val contacts = EmergencySmsHelper.getEmergencyContacts(this)
        val contactsText = contacts.joinToString(", ")  // Use comma separator for consistency
        
        val input = android.widget.EditText(this)
        input.setText(contactsText)
        input.hint = "Enter phone numbers (comma-separated)"
        
        AlertDialog.Builder(this)
            .setTitle("Emergency Contacts")
            .setMessage("Enter phone numbers to notify via SMS when emergency mode is activated.\nSeparate multiple numbers with commas:")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString()
                val newContacts = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                EmergencySmsHelper.saveEmergencyContacts(this, newContacts)
                updateContactsDisplay()
                Toast.makeText(this, "Contacts saved", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun updateContactsDisplay() {
        val contacts = EmergencySmsHelper.getEmergencyContacts(this)
        val count = contacts.size
        binding.contactsCountText.text = "$count contact(s) configured"
    }
    
    private fun saveFlashlightMorseSetting(enabled: Boolean) {
        val preferences = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("flashlight_morse_enabled", enabled).apply()
        Toast.makeText(this, if (enabled) "Flashlight signaling enabled" else "Flashlight signaling disabled", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveUltrasoundTransmitSetting(enabled: Boolean) {
        val preferences = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("ultrasound_transmit_enabled", enabled).apply()
        Toast.makeText(this, if (enabled) "Ultrasound transmission enabled" else "Ultrasound transmission disabled", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveUltrasoundListenSetting(enabled: Boolean) {
        val preferences = getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("ultrasound_listen_enabled", enabled).apply()
        Toast.makeText(this, if (enabled) "Ultrasound listening enabled" else "Ultrasound listening disabled", Toast.LENGTH_SHORT).show()
    }
}
