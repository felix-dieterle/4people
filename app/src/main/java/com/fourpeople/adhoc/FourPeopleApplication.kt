package com.fourpeople.adhoc

import android.app.Application
import android.util.Log
import com.fourpeople.adhoc.util.ErrorLogger
import com.fourpeople.adhoc.util.LogManager

/**
 * Application class for the 4people app.
 * Initializes global components like error logging and crash handling.
 */
class FourPeopleApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            Log.d("FourPeopleApplication", "Application onCreate started")
            
            // Initialize LogManager with persistent storage
            LogManager.initialize(this)
            Log.d("FourPeopleApplication", "LogManager initialized")
            
            // Initialize error logging to file system
            ErrorLogger.initialize(this)
            Log.d("FourPeopleApplication", "ErrorLogger initialized")
            
            // Set up global uncaught exception handler for crash logging
            setupCrashHandler()
            Log.d("FourPeopleApplication", "Crash handler set up")
            
            ErrorLogger.logInfo("FourPeopleApplication", "Application started successfully")
            ErrorLogger.logInfo("FourPeopleApplication", "Log directory: ${ErrorLogger.getLogDirectoryPath()}")
        } catch (e: Exception) {
            Log.e("FourPeopleApplication", "Failed to initialize application", e)
            ErrorLogger.logError("FourPeopleApplication", "Failed to initialize application", e)
        }
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the crash to file system
            ErrorLogger.logCrash(thread, throwable)
            
            // Call the default handler to let the system handle the crash normally
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
