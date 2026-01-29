package com.fourpeople.adhoc.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Centralized logging manager for tracking all actions, events, messages, and state changes.
 * Provides a log window interface to view system activity.
 * Logs are persisted across app restarts.
 */
object LogManager {
    
    private const val PREF_NAME = "log_manager_prefs"
    private const val KEY_LOG_ENTRIES = "log_entries"
    private var sharedPreferences: SharedPreferences? = null
    private var isInitialized = false
    
    enum class LogLevel {
        INFO,
        WARNING,
        ERROR,
        EVENT,
        STATE_CHANGE,
        MESSAGE
    }
    
    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val tag: String,
        val message: String
    ) {
        fun getFormattedTimestamp(): String {
            val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
            return dateFormat.format(Date(timestamp))
        }
        
        fun getFormattedEntry(): String {
            return "[${getFormattedTimestamp()}] [${level.name}] $tag: $message"
        }
        
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("level", level.name)
                put("tag", tag)
                put("message", message)
            }
        }
        
        companion object {
            fun fromJson(json: JSONObject): LogEntry {
                return LogEntry(
                    timestamp = json.getLong("timestamp"),
                    level = LogLevel.valueOf(json.getString("level")),
                    tag = json.getString("tag"),
                    message = json.getString("message")
                )
            }
        }
    }
    
    private val logEntries = CopyOnWriteArrayList<LogEntry>()
    private val listeners = CopyOnWriteArrayList<LogListener>()
    private const val MAX_LOG_ENTRIES = 1000
    
    interface LogListener {
        fun onNewLogEntry(entry: LogEntry)
    }
    
    /**
     * Initialize the LogManager with application context for persistent storage
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            loadLogsFromStorage()
            isInitialized = true
        }
    }
    
    /**
     * Load logs from persistent storage
     */
    private fun loadLogsFromStorage() {
        try {
            val logsJson = sharedPreferences?.getString(KEY_LOG_ENTRIES, null)
            if (logsJson != null) {
                val jsonArray = JSONArray(logsJson)
                for (i in 0 until jsonArray.length()) {
                    val entry = LogEntry.fromJson(jsonArray.getJSONObject(i))
                    logEntries.add(entry)
                }
            }
        } catch (e: Exception) {
            // Failed to load logs, start fresh
            android.util.Log.e("LogManager", "Failed to load logs from storage", e)
        }
    }
    
    /**
     * Save logs to persistent storage
     */
    private fun saveLogsToStorage() {
        try {
            val jsonArray = JSONArray()
            logEntries.forEach { entry ->
                jsonArray.put(entry.toJson())
            }
            sharedPreferences?.edit()?.putString(KEY_LOG_ENTRIES, jsonArray.toString())?.apply()
        } catch (e: Exception) {
            android.util.Log.e("LogManager", "Failed to save logs to storage", e)
        }
    }
    
    /**
     * Add a listener to be notified of new log entries
     */
    fun addListener(listener: LogListener) {
        listeners.add(listener)
    }
    
    /**
     * Remove a listener
     */
    fun removeListener(listener: LogListener) {
        listeners.remove(listener)
    }
    
    /**
     * Log an informational message
     */
    fun logInfo(tag: String, message: String) {
        addLogEntry(LogLevel.INFO, tag, message)
    }
    
    /**
     * Log a warning message
     */
    fun logWarning(tag: String, message: String) {
        addLogEntry(LogLevel.WARNING, tag, message)
    }
    
    /**
     * Log an error message
     */
    fun logError(tag: String, message: String) {
        addLogEntry(LogLevel.ERROR, tag, message)
    }
    
    /**
     * Log an event (user action, system event, etc.)
     */
    fun logEvent(tag: String, message: String) {
        addLogEntry(LogLevel.EVENT, tag, message)
    }
    
    /**
     * Log a state change
     */
    fun logStateChange(tag: String, message: String) {
        addLogEntry(LogLevel.STATE_CHANGE, tag, message)
    }
    
    /**
     * Log a message (communication message)
     */
    fun logMessage(tag: String, message: String) {
        addLogEntry(LogLevel.MESSAGE, tag, message)
    }
    
    /**
     * Get all log entries
     */
    fun getLogEntries(): List<LogEntry> {
        return logEntries.toList()
    }
    
    /**
     * Clear all log entries
     */
    fun clearLogs() {
        logEntries.clear()
        saveLogsToStorage()
    }
    
    private fun addLogEntry(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message
        )
        
        // Add to list
        logEntries.add(entry)
        
        // Trim old entries if needed
        while (logEntries.size > MAX_LOG_ENTRIES) {
            logEntries.removeAt(0)
        }
        
        // Save to persistent storage
        saveLogsToStorage()
        
        // Notify listeners
        listeners.forEach { listener ->
            try {
                listener.onNewLogEntry(entry)
            } catch (e: Exception) {
                // Ignore listener errors to prevent cascading failures
            }
        }
    }
}
