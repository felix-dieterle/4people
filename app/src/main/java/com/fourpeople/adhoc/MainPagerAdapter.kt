package com.fourpeople.adhoc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for the main ViewPager2 that displays Emergency and Panic mode tabs.
 */
class MainPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val fragmentCallback: (Int, Fragment) -> Unit
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val fragment = when (position) {
            0 -> EmergencyFragment()
            1 -> PanicFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
        fragmentCallback(position, fragment)
        return fragment
    }
}
