package com.fourpeople.adhoc.util

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized error logging utility that writes errors to the file system.
 * This helps investigate crashes and errors by persisting logs that survive app restarts.
 * 
 * Logs are saved to the Downloads directory as .txt files for easy access and sharing.
 */
object ErrorLogger {
    private const val TAG = "ErrorLogger"
    private const val LOG_DIR_NAME = "4people_logs"
    private const val MAX_LOG_FILES = 10
    private const val MAX_LOG_SIZE_BYTES = 2 * 1024 * 1024 // 2 MB
    
    private var logDirectory: File? = null
    private var currentLogFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    /**
     * Initialize the error logger with the application context.
     * Should be called once during application startup.
     */
    fun initialize(context: Context) {
        try {
            // Try to use the Downloads directory for better accessibility
            logDirectory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ use app-specific directory in Downloads
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), LOG_DIR_NAME)
            } else {
                // Older Android versions use public Downloads directory
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), LOG_DIR_NAME)
            }
            
            // Fallback to internal storage if external storage is not available
            if (logDirectory?.exists() == false && logDirectory?.mkdirs() == false) {
                Log.w(TAG, "Could not create log directory in Downloads, using internal storage")
                logDirectory = File(context.filesDir, LOG_DIR_NAME)
                logDirectory?.mkdirs()
            }
            
            logDirectory?.let { dir ->
                if (!dir.exists()) {
                    dir.mkdirs()
                }
            }
            
            // Create or get current log file
            currentLogFile = getCurrentOrNewLogFile()
            
            // Clean up old log files
            rotateLogsIfNeeded()
            
            val logPath = logDirectory?.absolutePath ?: "unknown"
            Log.d(TAG, "ErrorLogger initialized. Log directory: $logPath")
            
            // Log initialization to file
            logError(TAG, "ErrorLogger initialized at: $logPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ErrorLogger", e)
        }
    }
    
    /**
     * Log an error with a tag and message.
     */
    fun logError(tag: String, message: String) {
        logError(tag, message, null)
    }
    
    /**
     * Log an error with a tag, message, and exception.
     */
    fun logError(tag: String, message: String, throwable: Throwable?) {
        try {
            // Also log to Android's logcat
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
            
            // Write to file
            writeToFile(tag, "ERROR", message, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log error to file", e)
        }
    }
    
    /**
     * Log a warning with a tag and message.
     */
    fun logWarning(tag: String, message: String) {
        try {
            Log.w(tag, message)
            writeToFile(tag, "WARN", message, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log warning to file", e)
        }
    }
    
    /**
     * Log an info message with a tag and message.
     */
    fun logInfo(tag: String, message: String) {
        try {
            Log.i(tag, message)
            writeToFile(tag, "INFO", message, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log info to file", e)
        }
    }
    
    /**
     * Log an uncaught exception (typically from the global exception handler).
     */
    fun logCrash(thread: Thread, throwable: Throwable) {
        try {
            val message = "Uncaught exception in thread ${thread.name}"
            Log.e(TAG, message, throwable)
            writeToFile(TAG, "CRASH", message, throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log crash to file", e)
        }
    }
    
    /**
     * Get all log files, sorted by modification time (newest first).
     */
    fun getLogFiles(): List<File> {
        return try {
            logDirectory?.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }
                ?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get log files", e)
            emptyList()
        }
    }
    
    /**
     * Delete all log files and create a new log file.
     */
    fun clearLogs() {
        try {
            getLogFiles().forEach { it.delete() }
            currentLogFile = getCurrentOrNewLogFile()
            Log.d(TAG, "All log files cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log files", e)
        }
    }
    
    /**
     * Get the path to the log directory for display to the user.
     */
    fun getLogDirectoryPath(): String {
        return logDirectory?.absolutePath ?: "Unknown"
    }
    
    private fun writeToFile(tag: String, level: String, message: String, throwable: Throwable?) {
        synchronized(this) {
            val logFile = currentLogFile
            if (logFile == null) {
                return
            }
            
            try {
                // Check if we need to rotate to a new file
                if (logFile.length() > MAX_LOG_SIZE_BYTES) {
                    currentLogFile = getCurrentOrNewLogFile()
                    rotateLogsIfNeeded()
                }
                
                FileWriter(currentLogFile, true).use { fileWriter ->
                    PrintWriter(fileWriter).use { printWriter ->
                        val timestamp = dateFormat.format(Date())
                        printWriter.println("$timestamp [$level] $tag: $message")
                        
                        if (throwable != null) {
                            throwable.printStackTrace(printWriter)
                        }
                        printWriter.println() // Empty line for readability
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
    }
    
    private fun getCurrentOrNewLogFile(): File {
        // Check if we have an existing log file that's not too large
        val existingFiles = getLogFiles()
        if (existingFiles.isNotEmpty()) {
            val mostRecent = existingFiles.first()
            if (mostRecent.length() < MAX_LOG_SIZE_BYTES) {
                return mostRecent
            }
        }
        
        // Create a new log file with current timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(logDirectory, "4people_log_$timestamp.txt")
    }
    
    private fun rotateLogsIfNeeded() {
        try {
            val logFiles = getLogFiles()
            if (logFiles.size > MAX_LOG_FILES) {
                // Delete oldest files
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old log file: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rotate log files", e)
        }
    }
}
