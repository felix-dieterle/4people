package com.fourpeople.adhoc

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for SettingsActivity to ensure settings screen displays correctly.
 */
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(
        Intent(ApplicationProvider.getApplicationContext(), SettingsActivity::class.java)
    )

    @Test
    fun testStandbyMonitoringSwitchDisplayed() {
        // Verify standby monitoring switch is present
        onView(withId(R.id.standbyMonitoringSwitch))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAutoActivationSwitchDisplayed() {
        // Verify auto-activation switch is present
        onView(withId(R.id.autoActivationSwitch))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSettingsLabelsDisplayed() {
        // Verify setting labels are visible
        onView(withId(R.id.standbyMonitoringLabel))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.autoActivationLabel))
            .check(matches(isDisplayed()))
    }
}
