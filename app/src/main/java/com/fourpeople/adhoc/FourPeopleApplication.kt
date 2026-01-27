package com.fourpeople.adhoc

import android.app.Application
import com.fourpeople.adhoc.util.ErrorLogger

/**
 * Application class for the 4people app.
 * Initializes global components like error logging and crash handling.
 */
class FourPeopleApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize error logging to file system
        ErrorLogger.initialize(this)
        
        // Set up global uncaught exception handler for crash logging
        setupCrashHandler()
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
