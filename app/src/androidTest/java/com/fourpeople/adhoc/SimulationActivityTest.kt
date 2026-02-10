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
 * UI test for SimulationActivity to ensure simulation screen displays correctly.
 */
@RunWith(AndroidJUnit4::class)
class SimulationActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(
        Intent(ApplicationProvider.getApplicationContext(), SimulationActivity::class.java)
    )

    @Test
    fun testSimulationActivityLaunches() {
        // Verify simulation screen launches
        onView(withId(R.id.scenarioSpinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testScenarioSpinnerDisplayed() {
        // Verify scenario selection spinner is present
        onView(withId(R.id.scenarioSpinner))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSimulationCanvasDisplayed() {
        // Verify the simulation canvas is displayed
        onView(withId(R.id.simulationCanvas))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStartButtonDisplayed() {
        // Verify start simulation button is present
        onView(withId(R.id.startButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testStatisticsDisplayed() {
        // Verify statistics section is visible
        onView(withId(R.id.statisticsTextView))
            .check(matches(isDisplayed()))
    }
}
