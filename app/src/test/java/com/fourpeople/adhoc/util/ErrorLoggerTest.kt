package com.fourpeople.adhoc.util

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
class ErrorLoggerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var tempDir: File
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // Create a temporary directory for testing
        tempDir = createTempDirectory("error_logger_test").toFile()
        `when`(mockContext.getFilesDir()).thenReturn(tempDir)
    }
    
    @Test
    fun testInitialize() {
        // Test that initialization creates the log directory
        ErrorLogger.initialize(mockContext)
        
        val logDir = File(tempDir, "error_logs")
        assertTrue("Log directory should be created", logDir.exists())
        assertTrue("Log directory should be a directory", logDir.isDirectory)
    }
    
    @Test
    fun testLogError() {
        ErrorLogger.initialize(mockContext)
        
        // Log an error
        ErrorLogger.logError("TestTag", "Test error message")
        
        // Verify that a log file was created
        val logFiles = ErrorLogger.getLogFiles()
        assertTrue("At least one log file should exist", logFiles.isNotEmpty())
    }
    
    @Test
    fun testLogErrorWithException() {
        ErrorLogger.initialize(mockContext)
        
        val exception = RuntimeException("Test exception")
        ErrorLogger.logError("TestTag", "Test error with exception", exception)
        
        // Verify that a log file was created
        val logFiles = ErrorLogger.getLogFiles()
        assertTrue("At least one log file should exist", logFiles.isNotEmpty())
        
        // Verify content contains exception info
        val content = logFiles.first().readText()
        assertTrue("Log should contain error message", content.contains("Test error with exception"))
        assertTrue("Log should contain exception", content.contains("RuntimeException"))
    }
    
    @Test
    fun testLogWarning() {
        ErrorLogger.initialize(mockContext)
        
        ErrorLogger.logWarning("TestTag", "Test warning message")
        
        // Verify that a log file was created
        val logFiles = ErrorLogger.getLogFiles()
        assertTrue("At least one log file should exist", logFiles.isNotEmpty())
        
        val content = logFiles.first().readText()
        assertTrue("Log should contain warning", content.contains("WARN"))
    }
    
    @Test
    fun testLogCrash() {
        ErrorLogger.initialize(mockContext)
        
        val thread = Thread.currentThread()
        val throwable = OutOfMemoryError("Test crash")
        ErrorLogger.logCrash(thread, throwable)
        
        // Verify that a log file was created
        val logFiles = ErrorLogger.getLogFiles()
        assertTrue("At least one log file should exist", logFiles.isNotEmpty())
        
        val content = logFiles.first().readText()
        assertTrue("Log should contain crash info", content.contains("CRASH"))
        assertTrue("Log should contain thread name", content.contains(thread.name))
    }
    
    @Test
    fun testGetLogFiles() {
        ErrorLogger.initialize(mockContext)
        
        // Log multiple errors
        ErrorLogger.logError("Tag1", "Error 1")
        ErrorLogger.logError("Tag2", "Error 2")
        
        val logFiles = ErrorLogger.getLogFiles()
        assertTrue("Should have at least one log file", logFiles.isNotEmpty())
        
        // Files should be sorted by modification time (newest first)
        if (logFiles.size > 1) {
            assertTrue("Files should be sorted newest first", 
                logFiles[0].lastModified() >= logFiles[1].lastModified())
        }
    }
    
    @Test
    fun testClearLogs() {
        ErrorLogger.initialize(mockContext)
        
        // Log some errors
        ErrorLogger.logError("TestTag", "Error 1")
        ErrorLogger.logError("TestTag", "Error 2")
        
        // Verify files exist
        assertTrue("Log files should exist before clear", ErrorLogger.getLogFiles().isNotEmpty())
        
        // Clear logs
        ErrorLogger.clearLogs()
        
        // Verify files are deleted
        assertTrue("Log files should be cleared", ErrorLogger.getLogFiles().isEmpty())
        
        // Verify logging still works after clearing
        ErrorLogger.logError("TestTag", "Error after clear")
        assertTrue("Should be able to log after clearing", ErrorLogger.getLogFiles().isNotEmpty())
    }
    
    @Test
    fun testBasicLogFileManagement() {
        ErrorLogger.initialize(mockContext)
        
        // Create multiple log files by logging many errors
        for (i in 1..7) {
            ErrorLogger.logError("TestTag", "Error $i")
        }
        
        val logFiles = ErrorLogger.getLogFiles()
        // This is a basic test that verifies logging doesn't crash with multiple entries
        assertTrue("Should have log files", logFiles.isNotEmpty())
    }
    
    @Test
    fun testLoggingWithoutInitialization() {
        // Logging should not crash even if ErrorLogger is not initialized
        // The methods should handle null gracefully
        ErrorLogger.logError("TestTag", "Error without init")
        ErrorLogger.logWarning("TestTag", "Warning without init")
        
        // No exception should be thrown
        assertTrue("Test should complete without exception", true)
    }
}
