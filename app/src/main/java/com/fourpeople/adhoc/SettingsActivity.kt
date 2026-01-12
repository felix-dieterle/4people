package com.fourpeople.adhoc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fourpeople.adhoc.databinding.ActivitySettingsBinding
import com.fourpeople.adhoc.service.StandbyMonitoringService

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
    }

    private fun loadSettings() {
        val preferences = getSharedPreferences(StandbyMonitoringService.PREF_NAME, Context.MODE_PRIVATE)
        val autoActivate = preferences.getBoolean(StandbyMonitoringService.PREF_AUTO_ACTIVATE, false)
        
        binding.autoActivateSwitch.isChecked = autoActivate
        
        // Check if standby monitoring is enabled (we can check if service is running)
        // For simplicity, we'll default to false
        binding.standbyMonitoringSwitch.isChecked = false
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
}
