package com.fourpeople.adhoc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.fourpeople.adhoc.databinding.FragmentHelpFlowBinding

/**
 * Fragment displaying a flow diagram for a specific app scenario.
 */
class HelpFlowFragment : Fragment() {

    private var _binding: FragmentHelpFlowBinding? = null
    private val binding get() = _binding!!
    private var flowType: Int = HelpActivity.TAB_IDLE_STATE

    companion object {
        private const val ARG_FLOW_TYPE = "flow_type"

        fun newInstance(flowType: Int): HelpFlowFragment {
            val fragment = HelpFlowFragment()
            val args = Bundle()
            args.putInt(ARG_FLOW_TYPE, flowType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            flowType = it.getInt(ARG_FLOW_TYPE, HelpActivity.TAB_IDLE_STATE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpFlowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayFlowDiagram()
    }

    private fun displayFlowDiagram() {
        val (title, content) = when (flowType) {
            HelpActivity.TAB_IDLE_STATE -> getIdleStateFlow()
            HelpActivity.TAB_EMERGENCY_MODE -> getEmergencyModeFlow()
            HelpActivity.TAB_PANIC_MODE -> getPanicModeFlow()
            HelpActivity.TAB_NETWORK_CASCADE -> getNetworkCascadeFlow()
            else -> getIdleStateFlow()
        }

        binding.flowTitle.text = title
        binding.flowContent.text = HtmlCompat.fromHtml(content, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }

    private fun getIdleStateFlow(): Pair<String, String> {
        val title = getString(R.string.help_flow_idle_title)
        val content = getString(R.string.help_flow_idle_content)
        return Pair(title, content)
    }

    private fun getEmergencyModeFlow(): Pair<String, String> {
        val title = getString(R.string.help_flow_emergency_title)
        val content = getString(R.string.help_flow_emergency_content)
        return Pair(title, content)
    }

    private fun getPanicModeFlow(): Pair<String, String> {
        val title = getString(R.string.help_flow_panic_title)
        val content = getString(R.string.help_flow_panic_content)
        return Pair(title, content)
    }

    private fun getNetworkCascadeFlow(): Pair<String, String> {
        val title = getString(R.string.help_flow_cascade_title)
        val content = getString(R.string.help_flow_cascade_content)
        return Pair(title, content)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
