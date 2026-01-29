package com.fourpeople.adhoc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fourpeople.adhoc.databinding.FragmentPanicBinding

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
        _binding = FragmentPanicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        mainActivity.updatePanicUI(binding)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        binding.panicModeButton.setOnClickListener {
            mainActivity.togglePanicMode()
        }

        binding.panicHelpButton.setOnClickListener {
            mainActivity.showPanicModeHelp()
        }
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
