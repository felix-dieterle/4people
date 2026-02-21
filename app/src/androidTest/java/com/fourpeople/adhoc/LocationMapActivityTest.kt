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
 * UI test for LocationMapActivity to ensure location list view displays correctly.
 */
@RunWith(AndroidJUnit4::class)
class LocationMapActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule<LocationMapActivity>(
        Intent(ApplicationProvider.getApplicationContext(), LocationMapActivity::class.java)
    )

    @Test
    fun testRecyclerViewDisplayed() {
        // Verify the recycler view for locations is present
        onView(withId(R.id.participantRecyclerView))
            .check(matches(isDisplayed()))
    }
}
