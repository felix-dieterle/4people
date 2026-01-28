package com.fourpeople.adhoc.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Centralized logging manager for tracking all actions, events, messages, and state changes.
 * Provides a log window interface to view system activity.
 */
object LogManager {
    
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
    }
    
    private val logEntries = CopyOnWriteArrayList<LogEntry>()
    private val listeners = CopyOnWriteArrayList<LogListener>()
    private const val MAX_LOG_ENTRIES = 1000
    
    interface LogListener {
        fun onNewLogEntry(entry: LogEntry)
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
