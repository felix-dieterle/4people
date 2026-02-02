package com.fourpeople.adhoc.util

import android.content.Context
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.mockito.Mockito.*

/**
 * Unit tests for LogManager.
 * Tests logging, persistence, and listener functionality.
 */
class LogManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        // Note: Cannot fully test persistence without Android framework
        // These tests focus on in-memory log management logic
    }

    @After
    fun tearDown() {
        // Clean up logs after each test
        LogManager.clearLogs()
    }

    @Test
    fun testLogLevels() {
        // Verify all log levels exist
        val levels = LogManager.LogLevel.values()
        assertEquals(6, levels.size)
        
        assertTrue(levels.contains(LogManager.LogLevel.INFO))
        assertTrue(levels.contains(LogManager.LogLevel.WARNING))
        assertTrue(levels.contains(LogManager.LogLevel.ERROR))
        assertTrue(levels.contains(LogManager.LogLevel.EVENT))
        assertTrue(levels.contains(LogManager.LogLevel.STATE_CHANGE))
        assertTrue(levels.contains(LogManager.LogLevel.MESSAGE))
    }

    @Test
    fun testLogInfo() {
        LogManager.logInfo("TestTag", "Test info message")
        
        val logs = LogManager.getLogEntries()
        assertEquals(1, logs.size)
        assertEquals(LogManager.LogLevel.INFO, logs[0].level)
        assertEquals("TestTag", logs[0].tag)
        assertEquals("Test info message", logs[0].message)
    }

    @Test
    fun testLogWarning() {
        LogManager.logWarning("TestTag", "Test warning message")
        
        val logs = LogManager.getLogEntries()
        assertEquals(1, logs.size)
        assertEquals(LogManager.LogLevel.WARNING, logs[0].level)
    }

    @Test
    fun testLogError() {
        LogManager.logError("TestTag", "Test error message")
        
        val logs = LogManager.getLogEntries()
        assertEquals(1, logs.size)
        assertEquals(LogManager.LogLevel.ERROR, logs[0].level)
    }

    @Test
    fun testLogEvent() {
        LogManager.logEvent("TestTag", "Test event message")
        
        val logs = LogManager.getLogEntries()
        assertEquals(1, logs.size)
        assertEquals(LogManager.LogLevel.EVENT, logs[0].level)
    }

    @Test
    fun testLogStateChange() {
        LogManager.logStateChange("TestTag", "Test state change")
        
        val logs = LogManager.getLogEntries()
        assertEquals(1, logs.size)
        assertEquals(LogManager.LogLevel.STATE_CHANGE, logs[0].level)
    }

    @Test
    fun testLogMessage() {
        LogManager.logMessage("TestTag", "Test message")
        
        val logs = LogManager.getLogEntries()
        assertEquals(1, logs.size)
        assertEquals(LogManager.LogLevel.MESSAGE, logs[0].level)
    }

    @Test
    fun testMultipleLogs() {
        LogManager.logInfo("Tag1", "Message 1")
        LogManager.logWarning("Tag2", "Message 2")
        LogManager.logError("Tag3", "Message 3")
        
        val logs = LogManager.getLogEntries()
        assertEquals(3, logs.size)
    }

    @Test
    fun testLogEntryTimestamp() {
        val beforeTime = System.currentTimeMillis()
        LogManager.logInfo("Test", "Timestamp test")
        val afterTime = System.currentTimeMillis()
        
        val logs = LogManager.getLogEntries()
        val timestamp = logs[0].timestamp
        
        assertTrue(timestamp >= beforeTime)
        assertTrue(timestamp <= afterTime)
    }

    @Test
    fun testLogEntryFormattedTimestamp() {
        LogManager.logInfo("Test", "Formatted timestamp test")
        
        val logs = LogManager.getLogEntries()
        val formatted = logs[0].getFormattedTimestamp()
        
        // Should be in HH:mm:ss.SSS format
        assertTrue(formatted.matches(Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")))
    }

    @Test
    fun testLogEntryFormattedEntry() {
        LogManager.logInfo("TestTag", "Test message")
        
        val logs = LogManager.getLogEntries()
        val formatted = logs[0].getFormattedEntry()
        
        assertTrue(formatted.contains("INFO"))
        assertTrue(formatted.contains("TestTag"))
        assertTrue(formatted.contains("Test message"))
    }

    @Test
    fun testClearLogs() {
        LogManager.logInfo("Test1", "Message 1")
        LogManager.logInfo("Test2", "Message 2")
        LogManager.logInfo("Test3", "Message 3")
        
        assertEquals(3, LogManager.getLogEntries().size)
        
        LogManager.clearLogs()
        
        assertEquals(0, LogManager.getLogEntries().size)
    }

    @Test
    fun testGetLogEntries() {
        LogManager.logInfo("Test", "Message")
        
        val logs = LogManager.getLogEntries()
        
        assertNotNull(logs)
        assertTrue(logs is List)
    }

    @Test
    fun testLogEntryToJson() {
        LogManager.logInfo("TestTag", "Test message")
        
        val logs = LogManager.getLogEntries()
        val json = logs[0].toJson()
        
        assertNotNull(json)
        assertTrue(json.has("timestamp"))
        assertTrue(json.has("level"))
        assertTrue(json.has("tag"))
        assertTrue(json.has("message"))
        
        assertEquals("INFO", json.getString("level"))
        assertEquals("TestTag", json.getString("tag"))
        assertEquals("Test message", json.getString("message"))
    }

    @Test
    fun testLogEntryFromJson() {
        val entry = LogManager.LogEntry(
            timestamp = 1234567890L,
            level = LogManager.LogLevel.ERROR,
            tag = "JsonTest",
            message = "JSON test message"
        )
        
        val json = entry.toJson()
        val reconstructed = LogManager.LogEntry.fromJson(json)
        
        assertEquals(entry.timestamp, reconstructed.timestamp)
        assertEquals(entry.level, reconstructed.level)
        assertEquals(entry.tag, reconstructed.tag)
        assertEquals(entry.message, reconstructed.message)
    }

    @Test
    fun testLogListener() {
        var listenerCalled = false
        var receivedEntry: LogManager.LogEntry? = null
        
        val listener = object : LogManager.LogListener {
            override fun onNewLogEntry(entry: LogManager.LogEntry) {
                listenerCalled = true
                receivedEntry = entry
            }
        }
        
        LogManager.addListener(listener)
        LogManager.logInfo("Test", "Listener test")
        
        assertTrue(listenerCalled)
        assertNotNull(receivedEntry)
        assertEquals("Test", receivedEntry?.tag)
        assertEquals("Listener test", receivedEntry?.message)
        
        LogManager.removeListener(listener)
    }

    @Test
    fun testRemoveListener() {
        var callCount = 0
        
        val listener = object : LogManager.LogListener {
            override fun onNewLogEntry(entry: LogManager.LogEntry) {
                callCount++
            }
        }
        
        LogManager.addListener(listener)
        LogManager.logInfo("Test", "Message 1")
        
        assertEquals(1, callCount)
        
        LogManager.removeListener(listener)
        LogManager.logInfo("Test", "Message 2")
        
        // Should still be 1, not 2
        assertEquals(1, callCount)
    }

    @Test
    fun testMultipleListeners() {
        var listener1Called = false
        var listener2Called = false
        
        val listener1 = object : LogManager.LogListener {
            override fun onNewLogEntry(entry: LogManager.LogEntry) {
                listener1Called = true
            }
        }
        
        val listener2 = object : LogManager.LogListener {
            override fun onNewLogEntry(entry: LogManager.LogEntry) {
                listener2Called = true
            }
        }
        
        LogManager.addListener(listener1)
        LogManager.addListener(listener2)
        
        LogManager.logInfo("Test", "Multiple listeners")
        
        assertTrue(listener1Called)
        assertTrue(listener2Called)
        
        LogManager.removeListener(listener1)
        LogManager.removeListener(listener2)
    }

    @Test
    fun testMaxLogEntriesLimit() {
        // Add more than MAX_LOG_ENTRIES (1000)
        for (i in 1..1100) {
            LogManager.logInfo("Test", "Message $i")
        }
        
        val logs = LogManager.getLogEntries()
        
        // Should be limited to 1000
        assertTrue(logs.size <= 1000)
    }

    @Test
    fun testLogEntryEquality() {
        val entry1 = LogManager.LogEntry(
            timestamp = 1234567890L,
            level = LogManager.LogLevel.INFO,
            tag = "Test",
            message = "Message"
        )
        
        val entry2 = LogManager.LogEntry(
            timestamp = 1234567890L,
            level = LogManager.LogLevel.INFO,
            tag = "Test",
            message = "Message"
        )
        
        assertEquals(entry1, entry2)
    }

    @Test
    fun testLogEntryInequality() {
        val entry1 = LogManager.LogEntry(
            timestamp = 1234567890L,
            level = LogManager.LogLevel.INFO,
            tag = "Test",
            message = "Message 1"
        )
        
        val entry2 = LogManager.LogEntry(
            timestamp = 1234567890L,
            level = LogManager.LogLevel.INFO,
            tag = "Test",
            message = "Message 2"
        )
        
        assertNotEquals(entry1, entry2)
    }

    @Test
    fun testLogOrderPreserved() {
        LogManager.logInfo("Tag1", "First")
        LogManager.logWarning("Tag2", "Second")
        LogManager.logError("Tag3", "Third")
        
        val logs = LogManager.getLogEntries()
        
        assertEquals("First", logs[0].message)
        assertEquals("Second", logs[1].message)
        assertEquals("Third", logs[2].message)
    }

    @Test
    fun testLogWithSpecialCharacters() {
        val specialMessage = "Test with \"quotes\" and \n newlines \t tabs"
        LogManager.logInfo("Test", specialMessage)
        
        val logs = LogManager.getLogEntries()
        assertEquals(specialMessage, logs[0].message)
    }

    @Test
    fun testLogWithEmptyMessage() {
        LogManager.logInfo("Test", "")
        
        val logs = LogManager.getLogEntries()
        assertEquals("", logs[0].message)
    }

    @Test
    fun testLogWithEmptyTag() {
        LogManager.logInfo("", "Message")
        
        val logs = LogManager.getLogEntries()
        assertEquals("", logs[0].tag)
    }
}
