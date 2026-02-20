package com.fourpeople.adhoc

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI test for MainActivity to ensure the main screen renders correctly
 * and basic interactions work as expected.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
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
        // Verify status information is shown
        onView(withId(R.id.bluetoothStatusTextView))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.wifiStatusTextView))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.locationStatusTextView))
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
