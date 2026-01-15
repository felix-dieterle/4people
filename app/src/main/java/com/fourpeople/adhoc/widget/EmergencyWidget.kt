package com.fourpeople.adhoc.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.fourpeople.adhoc.R
import com.fourpeople.adhoc.service.AdHocCommunicationService

/**
 * Emergency Widget Provider for one-click emergency mode activation.
 * 
 * This widget provides a home screen shortcut to quickly activate emergency mode
 * without having to open the app. This is critical in emergency situations where
 * speed is essential.
 */
class EmergencyWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_ACTIVATE_EMERGENCY = "com.fourpeople.adhoc.ACTIVATE_EMERGENCY_FROM_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all active widget instances
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_ACTIVATE_EMERGENCY -> {
                // Activate emergency mode
                activateEmergencyMode(context)
            }
            AdHocCommunicationService.ACTION_WIDGET_UPDATE -> {
                // Update all widget instances when service state changes
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = android.content.ComponentName(context, EmergencyWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // Check if emergency mode is currently active
        val isActive = AdHocCommunicationService.isActive(context)
        
        // Create an Intent to activate emergency mode
        val intent = Intent(context, EmergencyWidget::class.java).apply {
            action = ACTION_ACTIVATE_EMERGENCY
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.emergency_widget).apply {
            setOnClickPendingIntent(R.id.widget_icon, pendingIntent)
            setOnClickPendingIntent(R.id.widget_text, pendingIntent)
            
            // Update text based on current state
            val textResId = if (isActive) R.string.deactivate_emergency else R.string.activate_emergency
            setTextViewText(R.id.widget_text, context.getString(textResId))
            
            // Update background color to indicate state
            val backgroundColor = if (isActive) {
                context.getColor(android.R.color.holo_green_light)
            } else {
                context.getColor(android.R.color.white)
            }
            setInt(R.id.widget_background, "setBackgroundColor", backgroundColor)
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun activateEmergencyMode(context: Context) {
        // Toggle emergency mode based on current state
        val isActive = AdHocCommunicationService.isActive(context)
        val serviceIntent = Intent(context, AdHocCommunicationService::class.java).apply {
            action = if (isActive) {
                AdHocCommunicationService.ACTION_STOP
            } else {
                AdHocCommunicationService.ACTION_START
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        // Update all widget instances
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = android.content.ComponentName(context, EmergencyWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
