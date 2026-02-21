package com.fourpeople.adhoc

import android.Manifest
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for MainActivity to ensure the main screen renders correctly
 * and basic interactions work as expected.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule(order = 0)
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.SEND_SMS,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMainActivityLaunches() {
        // Verify main UI elements are displayed
        onView(withId(R.id.titleTextView))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.statusTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testEmergencyButtonDisplayed() {
        // Verify emergency activation button is present
        onView(withId(R.id.activateButton))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.activate_emergency)))
    }

    @Test
    fun testSettingsButtonDisplayed() {
        // Verify settings button is accessible
        onView(withId(R.id.settingsButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStatusDetailsDisplayed() {
        // In standby mode the scanning indicator is shown
        onView(withId(R.id.scanningTextView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLocationButtonsDisplayed() {
        // Verify location-related buttons are present
        onView(withId(R.id.viewLocationsButton))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.view_locations)))
        
        onView(withId(R.id.sendHelpButton))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.send_help_request)))
    }

    @Test
    fun testSimulationButtonDisplayed() {
        // Verify simulation button is accessible
        onView(withId(R.id.simulationButton))
            .check(matches(isDisplayed()))
            .check(matches(withText(R.string.open_simulation)))
    }

    @Test
    fun testInfrastructureStatusDisplayed() {
        // Verify infrastructure health monitoring is visible
        onView(withId(R.id.infrastructureHeaderTextView))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.infraOverallStatusTextView))
            .check(matches(isDisplayed()))
    }
}
