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
 * UI test for OfflineMapActivity to ensure offline map displays correctly.
 */
@RunWith(AndroidJUnit4::class)
class OfflineMapActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(
        Intent(ApplicationProvider.getApplicationContext(), OfflineMapActivity::class.java)
    )

    @Test
    fun testOfflineMapActivityLaunches() {
        // Verify offline map screen launches
        onView(withId(R.id.mapView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testMapViewDisplayed() {
        // Verify the map view is present
        onView(withId(R.id.mapView))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLegendDisplayed() {
        // Verify the legend is visible
        onView(withId(R.id.legendLayout))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.legendTitle))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testLegendItemsDisplayed() {
        // Verify legend items are shown
        onView(withId(R.id.legendParticipants))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.legendHelp))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.legendSafeZones))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testCenterButtonDisplayed() {
        // Verify center button is present
        onView(withId(R.id.centerButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testAddSafeZoneFabDisplayed() {
        // Verify FAB for adding safe zones is present
        onView(withId(R.id.addSafeZoneFab))
            .check(matches(isDisplayed()))
    }
}
