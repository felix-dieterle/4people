package com.fourpeople.adhoc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for help pages showing different flow scenarios.
 */
class HelpPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            HelpActivity.TAB_IDLE_STATE -> HelpFlowFragment.newInstance(HelpActivity.TAB_IDLE_STATE)
            HelpActivity.TAB_EMERGENCY_MODE -> HelpFlowFragment.newInstance(HelpActivity.TAB_EMERGENCY_MODE)
            HelpActivity.TAB_PANIC_MODE -> HelpFlowFragment.newInstance(HelpActivity.TAB_PANIC_MODE)
            HelpActivity.TAB_NETWORK_CASCADE -> HelpFlowFragment.newInstance(HelpActivity.TAB_NETWORK_CASCADE)
            else -> HelpFlowFragment.newInstance(HelpActivity.TAB_IDLE_STATE)
        }
    }
}
