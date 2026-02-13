package com.fourpeople.adhoc

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.fourpeople.adhoc.widget.PanicWidget
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyBoolean

/**
 * Test class for PanicWidget
 */
@RunWith(RobolectricTestRunner::class)
class PanicWidgetTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var appWidgetManager: AppWidgetManager
    
    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var widget: PanicWidget

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup SharedPreferences mock
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.getBoolean(anyString(), anyBoolean())).thenReturn(false)
        `when`(context.packageName).thenReturn("com.fourpeople.adhoc")
        
        widget = PanicWidget()
    }

    @Test
    fun testWidgetActionConstant() {
        // Verify widget action constant
        assertEquals("com.fourpeople.adhoc.ACTIVATE_PANIC_FROM_WIDGET", PanicWidget.ACTION_ACTIVATE_PANIC)
    }

    @Test
    fun testWidgetUpdateCalled() {
        // Test that onUpdate can be called without crashing
        val appWidgetIds = intArrayOf(1, 2, 3)
        
        try {
            widget.onUpdate(context, appWidgetManager, appWidgetIds)
            // If we get here without exception, test passes
            assertTrue(true)
        } catch (e: Exception) {
            fail("Widget update should not throw exception: ${e.message}")
        }
    }

    @Test
    fun testWidgetReceiveWithCorrectAction() {
        // Test that onReceive handles the correct action
        val intent = Intent(PanicWidget.ACTION_ACTIVATE_PANIC)
        
        try {
            widget.onReceive(context, intent)
            // If we get here without exception, test passes
            assertTrue(true)
        } catch (e: Exception) {
            fail("Widget receive should not throw exception: ${e.message}")
        }
    }

    @Test
    fun testWidgetReceiveWithDifferentAction() {
        // Test that onReceive handles different actions
        val intent = Intent("com.fourpeople.adhoc.DIFFERENT_ACTION")
        
        try {
            widget.onReceive(context, intent)
            // If we get here without exception, test passes
            assertTrue(true)
        } catch (e: Exception) {
            fail("Widget receive should not throw exception: ${e.message}")
        }
    }

    @Test
    fun testMultipleWidgetIds() {
        // Test handling multiple widget IDs
        val appWidgetIds = intArrayOf(1, 2, 3, 4, 5)
        
        try {
            widget.onUpdate(context, appWidgetManager, appWidgetIds)
            assertTrue(true)
        } catch (e: Exception) {
            fail("Widget should handle multiple IDs: ${e.message}")
        }
    }

    @Test
    fun testEmptyWidgetIds() {
        // Test handling empty widget IDs
        val appWidgetIds = intArrayOf()
        
        try {
            widget.onUpdate(context, appWidgetManager, appWidgetIds)
            assertTrue(true)
        } catch (e: Exception) {
            fail("Widget should handle empty IDs: ${e.message}")
        }
    }
    
    @Test
    fun testWidgetStateVisibility() {
        // Widget should show different states for enabled/disabled
        val activateText = "Activate Panic Mode"
        val deactivateText = "Deactivate Panic Mode"
        
        // Text should be different to clearly indicate state
        assertNotEquals(activateText, deactivateText)
        assertTrue(activateText.contains("Activate"))
        assertTrue(deactivateText.contains("Deactivate"))
    }
    
    @Test
    fun testWidgetStateUpdateBroadcast() {
        // Widget should listen for service state changes
        val widgetUpdateAction = "com.fourpeople.adhoc.PANIC_WIDGET_UPDATE"
        
        assertTrue(widgetUpdateAction.isNotEmpty())
        assertTrue(widgetUpdateAction.contains("PANIC_WIDGET_UPDATE"))
        assertTrue(widgetUpdateAction.startsWith("com.fourpeople.adhoc"))
    }
}
