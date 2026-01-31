package com.fourpeople.adhoc.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.fourpeople.adhoc.R
import com.fourpeople.adhoc.service.PanicModeService
import com.fourpeople.adhoc.util.ErrorLogger

/**
 * Panic Mode Widget Provider for one-click panic mode activation.
 * 
 * This widget provides a home screen shortcut to quickly activate panic mode
 * without having to open the app. This is critical in panic situations where
 * speed is essential.
 */
class PanicWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_ACTIVATE_PANIC = "com.fourpeople.adhoc.ACTIVATE_PANIC_FROM_WIDGET"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        ErrorLogger.logInfo("PanicWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        // Update all active widget instances
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        ErrorLogger.logInfo("PanicWidget", "onReceive called with action: ${intent.action}")
        
        when (intent.action) {
            ACTION_ACTIVATE_PANIC -> {
                // Activate panic mode
                ErrorLogger.logInfo("PanicWidget", "Activating panic mode from widget")
                activatePanicMode(context)
            }
            PanicModeService.ACTION_WIDGET_UPDATE -> {
                // Update all widget instances when service state changes
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = android.content.ComponentName(context, PanicWidget::class.java)
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
        try {
            // Check if panic mode is currently active
            val isActive = PanicModeService.isActive(context)
            
            // Create an Intent to activate panic mode
            val intent = Intent(context, PanicWidget::class.java).apply {
                action = ACTION_ACTIVATE_PANIC
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.panic_widget).apply {
                setOnClickPendingIntent(R.id.panic_widget_button, pendingIntent)
                
                // Update text based on current state
                val textResId = if (isActive) R.string.deactivate_panic else R.string.activate_panic
                setTextViewText(R.id.panic_widget_text, context.getString(textResId))
                
                // Update background color to indicate state - darker red when active
                val backgroundColor = if (isActive) {
                    0xFF8B0000.toInt() // Dark red when active
                } else {
                    0xFFD32F2F.toInt() // Normal red when inactive
                }
                setInt(R.id.panic_widget_button, "setBackgroundColor", backgroundColor)
            }

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            ErrorLogger.logInfo("PanicWidget", "Widget $appWidgetId updated, isActive=$isActive")
        } catch (e: Exception) {
            ErrorLogger.logError("PanicWidget", "Failed to update widget $appWidgetId", e)
        }
    }

    private fun activatePanicMode(context: Context) {
        try {
            // Toggle panic mode based on current state
            val isActive = PanicModeService.isActive(context)
            ErrorLogger.logInfo("PanicWidget", "Panic mode current state: $isActive, toggling...")
            
            val serviceIntent = Intent(context, PanicModeService::class.java).apply {
                action = if (isActive) {
                    PanicModeService.ACTION_STOP
                } else {
                    PanicModeService.ACTION_START
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            // Update all widget instances
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = android.content.ComponentName(context, PanicWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            
            ErrorLogger.logInfo("PanicWidget", "Panic mode toggled successfully")
        } catch (e: Exception) {
            ErrorLogger.logError("PanicWidget", "Failed to activate panic mode", e)
        }
    }
}
