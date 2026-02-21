package com.fourpeople.adhoc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fourpeople.adhoc.databinding.ActivityHelpBinding
import com.google.android.material.tabs.TabLayoutMediator

/**
 * HelpActivity displays flow diagrams and explanations for different app scenarios.
 * Shows visual representations of how the app works in various states.
 */
class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding

    companion object {
        const val EXTRA_INITIAL_TAB = "initial_tab"
        const val TAB_IDLE_STATE = 0
        const val TAB_EMERGENCY_MODE = 1
        const val TAB_NETWORK_CASCADE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViewPager()
        
        // Navigate to requested tab if specified
        val initialTab = intent.getIntExtra(EXTRA_INITIAL_TAB, TAB_IDLE_STATE)
        binding.viewPager.setCurrentItem(initialTab, false)
    }

    private fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.help_title)
    }

    private fun setupViewPager() {
        val adapter = HelpPagerAdapter(this)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                TAB_IDLE_STATE -> getString(R.string.help_tab_idle_state)
                TAB_EMERGENCY_MODE -> getString(R.string.help_tab_emergency_mode)
                TAB_NETWORK_CASCADE -> getString(R.string.help_tab_network_cascade)
                else -> ""
            }
        }.attach()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
