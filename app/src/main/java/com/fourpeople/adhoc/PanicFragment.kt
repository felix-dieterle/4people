package com.fourpeople.adhoc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fourpeople.adhoc.databinding.FragmentPanicBinding
import com.fourpeople.adhoc.util.ErrorLogger

/**
 * Fragment for Panic Mode functionality.
 * Contains panic mode activation and controls.
 */
class PanicFragment : Fragment() {

    private var _binding: FragmentPanicBinding? = null
    private val binding get() = _binding!!
    
    // Reference to parent MainActivity for accessing shared functionality
    private val mainActivity: MainActivity
        get() = requireActivity() as MainActivity

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ErrorLogger.logInfo("PanicFragment", "onCreateView called")
        ErrorLogger.logInfo("PanicFragment", "Inflating FragmentPanicBinding...")
        _binding = FragmentPanicBinding.inflate(inflater, container, false)
        ErrorLogger.logInfo("PanicFragment", "FragmentPanicBinding inflated successfully")
        ErrorLogger.logInfo("PanicFragment", "Returning binding.root")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ErrorLogger.logInfo("PanicFragment", "onViewCreated called")
        ErrorLogger.logInfo("PanicFragment", "Calling setupUI...")
        setupUI()
        ErrorLogger.logInfo("PanicFragment", "setupUI completed")
    }

    override fun onResume() {
        super.onResume()
        ErrorLogger.logInfo("PanicFragment", "onResume called")
        ErrorLogger.logInfo("PanicFragment", "Calling mainActivity.updatePanicUI...")
        mainActivity.updatePanicUI(binding)
        ErrorLogger.logInfo("PanicFragment", "mainActivity.updatePanicUI completed")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ErrorLogger.logInfo("PanicFragment", "onDestroyView called")
        _binding = null
    }

    private fun setupUI() {
        ErrorLogger.logInfo("PanicFragment", "setupUI: Setting up button click listeners...")
        
        binding.panicModeButton.setOnClickListener {
            ErrorLogger.logInfo("PanicFragment", "panicModeButton clicked")
            mainActivity.togglePanicMode()
        }

        binding.panicHelpButton.setOnClickListener {
            ErrorLogger.logInfo("PanicFragment", "panicHelpButton clicked")
            mainActivity.showPanicModeHelp()
        }
        
        ErrorLogger.logInfo("PanicFragment", "setupUI: All button click listeners set up successfully")
    }

    /**
     * Update the UI with current panic mode status
     */
    fun updateUI(isPanicModeActive: Boolean) {
        if (_binding == null) return

        if (isPanicModeActive) {
            binding.panicModeButton.text = "🔴 ${getString(R.string.deactivate_panic)}"
            binding.panicModeButton.backgroundTintList = 
                requireContext().getColorStateList(android.R.color.holo_green_dark)
        } else {
            binding.panicModeButton.text = getString(R.string.activate_panic)
            binding.panicModeButton.backgroundTintList =
                requireContext().getColorStateList(android.R.color.holo_red_dark)
        }
    }
}
