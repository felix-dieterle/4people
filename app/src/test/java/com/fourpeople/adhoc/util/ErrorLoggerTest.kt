package com.fourpeople.adhoc.util

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class ErrorLoggerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var tempDir: File
    
    @Before
    fun setUp() {
        // Create a temporary directory for testing
        tempDir = createTempDir("error_logger_test")
        `when`(mockContext.filesDir).thenReturn(tempDir)
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
        Thread.sleep(100) // Ensure different timestamps
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
    }
    
    @Test
    fun testLogRotation() {
        ErrorLogger.initialize(mockContext)
        
        // Create multiple log files by logging many errors
        // This is a basic test - in reality, we'd need to mock file size
        for (i in 1..7) {
            ErrorLogger.logError("TestTag", "Error $i")
        }
        
        val logFiles = ErrorLogger.getLogFiles()
        // The rotation should keep max 5 files, but we need to trigger size limit
        // This is a simple check that rotation doesn't crash
        assertTrue("Should have log files", logFiles.isNotEmpty())
    }
}
