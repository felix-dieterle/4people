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
        
        if (intent.action == ACTION_ACTIVATE_EMERGENCY) {
            // Activate emergency mode
            activateEmergencyMode(context)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
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
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun activateEmergencyMode(context: Context) {
        // Start the emergency communication service
        val serviceIntent = Intent(context, AdHocCommunicationService::class.java).apply {
            action = AdHocCommunicationService.ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
