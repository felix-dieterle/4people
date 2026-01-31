package com.fourpeople.adhoc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.fourpeople.adhoc.util.ErrorLogger

/**
 * Adapter for the main ViewPager2 that displays Emergency and Panic mode tabs.
 */
class MainPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val fragmentCallback: (Int, Fragment) -> Unit
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        ErrorLogger.logInfo("MainPagerAdapter", "getItemCount called, returning 2")
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        ErrorLogger.logInfo("MainPagerAdapter", "createFragment called for position: $position")
        
        val fragment = when (position) {
            0 -> {
                ErrorLogger.logInfo("MainPagerAdapter", "Creating EmergencyFragment for position 0")
                EmergencyFragment()
            }
            1 -> {
                ErrorLogger.logInfo("MainPagerAdapter", "Creating PanicFragment for position 1")
                PanicFragment()
            }
            else -> {
                ErrorLogger.logError("MainPagerAdapter", "Invalid position: $position")
                throw IllegalStateException("Invalid position: $position")
            }
        }
        
        ErrorLogger.logInfo("MainPagerAdapter", "Fragment created: ${fragment.javaClass.simpleName}, invoking callback")
        fragmentCallback(position, fragment)
        ErrorLogger.logInfo("MainPagerAdapter", "Callback invoked for position: $position")
        
        return fragment
    }
}
