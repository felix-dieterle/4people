package com.fourpeople.adhoc

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fourpeople.adhoc.databinding.FragmentEmergencyBinding
import com.fourpeople.adhoc.util.ErrorLogger

/**
 * Fragment for Emergency Mode functionality.
 * Contains emergency communication activation and status monitoring.
 */
class EmergencyFragment : Fragment() {

    private var _binding: FragmentEmergencyBinding? = null
    private val binding get() = _binding!!
    
    // Reference to parent MainActivity for accessing shared functionality
    private val mainActivity: MainActivity
        get() = requireActivity() as MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ErrorLogger.logInfo("EmergencyFragment", "onCreateView called")
        ErrorLogger.logInfo("EmergencyFragment", "Inflating FragmentEmergencyBinding...")
        _binding = FragmentEmergencyBinding.inflate(inflater, container, false)
        ErrorLogger.logInfo("EmergencyFragment", "FragmentEmergencyBinding inflated successfully")
        ErrorLogger.logInfo("EmergencyFragment", "Returning binding.root")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ErrorLogger.logInfo("EmergencyFragment", "onViewCreated called")
        ErrorLogger.logInfo("EmergencyFragment", "Calling setupUI...")
        setupUI()
        ErrorLogger.logInfo("EmergencyFragment", "setupUI completed")
    }

    override fun onResume() {
        super.onResume()
        ErrorLogger.logInfo("EmergencyFragment", "onResume called")
        ErrorLogger.logInfo("EmergencyFragment", "Calling mainActivity.updateEmergencyUI...")
        mainActivity.updateEmergencyUI(binding)
        ErrorLogger.logInfo("EmergencyFragment", "mainActivity.updateEmergencyUI completed")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ErrorLogger.logInfo("EmergencyFragment", "onDestroyView called")
        _binding = null
    }

    private fun setupUI() {
        ErrorLogger.logInfo("EmergencyFragment", "setupUI: Setting up button click listeners...")
        
        binding.activateButton.setOnClickListener {
            ErrorLogger.logInfo("EmergencyFragment", "activateButton clicked")
            mainActivity.handleEmergencyButtonClick()
        }

        binding.emergencyHelpButton.setOnClickListener {
            ErrorLogger.logInfo("EmergencyFragment", "emergencyHelpButton clicked")
            mainActivity.showEmergencyModeHelp()
        }

        binding.viewLocationsButton.setOnClickListener {
            ErrorLogger.logInfo("EmergencyFragment", "viewLocationsButton clicked")
            startActivity(Intent(requireContext(), LocationMapActivity::class.java))
        }

        binding.sendHelpButton.setOnClickListener {
            ErrorLogger.logInfo("EmergencyFragment", "sendHelpButton clicked")
            mainActivity.sendHelpRequest()
        }

        binding.simulationButton.setOnClickListener {
            ErrorLogger.logInfo("EmergencyFragment", "simulationButton clicked")
            startActivity(Intent(requireContext(), SimulationActivity::class.java))
        }
        
        ErrorLogger.logInfo("EmergencyFragment", "setupUI: All button click listeners set up successfully")
    }

    /**
     * Update the UI with current emergency mode status
     */
    fun updateUI(
        isEmergencyActive: Boolean,
        isBluetoothActive: Boolean,
        isWifiActive: Boolean,
        isHotspotActive: Boolean,
        isLocationActive: Boolean,
        isWifiConnected: Boolean,
        infraBluetoothHealth: String,
        infraWifiHealth: String,
        infraCellularHealth: String,
        infraMeshHealth: String,
        infraOverallHealth: String
    ) {
        if (_binding == null) return

        // Update status text
        if (isEmergencyActive) {
            binding.statusTextView.text = "🟢 ${getString(R.string.emergency_active)}"
            binding.activateButton.text = getString(R.string.deactivate_emergency)
            binding.scanningTextView.visibility = View.GONE
            binding.detailsLayout.visibility = View.VISIBLE
            binding.infrastructureHeaderTextView.visibility = View.VISIBLE
            binding.infrastructureStatusLayout.visibility = View.VISIBLE
        } else {
            binding.statusTextView.text = "⚪ ${getString(R.string.emergency_inactive)}"
            binding.activateButton.text = getString(R.string.activate_emergency)
            binding.scanningTextView.visibility = View.VISIBLE
            binding.detailsLayout.visibility = View.GONE
            binding.infrastructureHeaderTextView.visibility = View.GONE
            binding.infrastructureStatusLayout.visibility = View.GONE
        }

        // Update individual status indicators
        binding.bluetoothStatusTextView.text = "Bluetooth: ${if (isBluetoothActive) "Active ✓" else "Inactive"}"
        binding.wifiStatusTextView.text = "WiFi: ${if (isWifiActive) "Active ✓" else "Inactive"}"
        binding.hotspotStatusTextView.text = "Hotspot: ${if (isHotspotActive) "Active ✓" else "Inactive"}"
        binding.wifiConnectionStatusTextView.text = "WiFi Connection: ${if (isWifiConnected) "Connected ✓" else "Not Connected"}"
        binding.locationStatusTextView.text = "Location Sharing: ${if (isLocationActive) "Active ✓" else "Inactive"}"

        // Update infrastructure status
        binding.infraBluetoothStatusTextView.text = "Bluetooth: $infraBluetoothHealth"
        binding.infraWifiStatusTextView.text = "WiFi: $infraWifiHealth"
        binding.infraCellularStatusTextView.text = "Cellular: $infraCellularHealth"
        binding.infraMeshStatusTextView.text = "Mesh Network: $infraMeshHealth"
        binding.infraOverallStatusTextView.text = "Overall Status: $infraOverallHealth"
    }
}
