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
 * UI test for HelpActivity to ensure help request screen displays correctly.
 */
@RunWith(AndroidJUnit4::class)
class HelpActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(
        Intent(ApplicationProvider.getApplicationContext(), HelpActivity::class.java)
    )

    @Test
    fun testHelpActivityLaunches() {
        // Verify help screen launches
        onView(withId(R.id.tabLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testTabLayoutDisplayed() {
        // Verify tab layout is present
        onView(withId(R.id.tabLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testViewPagerDisplayed() {
        // Verify view pager is displayed
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }
}
