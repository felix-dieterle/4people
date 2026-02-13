package com.fourpeople.adhoc.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for LogManager.
 * 
 * Note: LogManager uses Android framework classes (Handler, Looper, SharedPreferences, JSONObject)
 * which are not available in unit tests. These tests focus on the LogEntry data class
 * and LogLevel enum basic properties that can be tested without Android dependencies.
 * 
 * Full LogManager functionality (including JSON serialization, logging methods, persistence)
 * should be tested with Android instrumentation tests.
 */
class LogManagerTest {

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
    fun testLogEntryCreation() {
        val timestamp = System.currentTimeMillis()
        val entry = LogManager.LogEntry(
            timestamp = timestamp,
            level = LogManager.LogLevel.INFO,
            tag = "TestTag",
            message = "Test message"
        )

        assertEquals(timestamp, entry.timestamp)
        assertEquals(LogManager.LogLevel.INFO, entry.level)
        assertEquals("TestTag", entry.tag)
        assertEquals("Test message", entry.message)
    }

    @Test
    fun testLogEntryFormattedTimestamp() {
        val entry = LogManager.LogEntry(
            timestamp = 1234567890000L,
            level = LogManager.LogLevel.INFO,
            tag = "Test",
            message = "Message"
        )
        
        val formatted = entry.getFormattedTimestamp()
        
        // Should be in HH:mm:ss.SSS format
        assertTrue(formatted.matches(Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")))
    }

    @Test
    fun testLogEntryFormattedEntry() {
        val entry = LogManager.LogEntry(
            timestamp = System.currentTimeMillis(),
            level = LogManager.LogLevel.INFO,
            tag = "TestTag",
            message = "Test message"
        )
        
        val formatted = entry.getFormattedEntry()
        
        assertTrue(formatted.contains("INFO"))
        assertTrue(formatted.contains("TestTag"))
        assertTrue(formatted.contains("Test message"))
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
    fun testLogEntryWithSpecialCharacters() {
        val specialMessage = "Test with \"quotes\" and \n newlines \t tabs"
        val entry = LogManager.LogEntry(
            timestamp = System.currentTimeMillis(),
            level = LogManager.LogLevel.INFO,
            tag = "Test",
            message = specialMessage
        )
        
        assertEquals(specialMessage, entry.message)
    }

    @Test
    fun testLogEntryWithEmptyMessage() {
        val entry = LogManager.LogEntry(
            timestamp = System.currentTimeMillis(),
            level = LogManager.LogLevel.INFO,
            tag = "Test",
            message = ""
        )
        
        assertEquals("", entry.message)
    }

    @Test
    fun testLogEntryWithEmptyTag() {
        val entry = LogManager.LogEntry(
            timestamp = System.currentTimeMillis(),
            level = LogManager.LogLevel.INFO,
            tag = "",
            message = "Message"
        )
        
        assertEquals("", entry.tag)
    }

    @Test
    fun testAllLogLevelsCreateCorrectly() {
        val levels = LogManager.LogLevel.values()
        
        for (level in levels) {
            val entry = LogManager.LogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                tag = "Test",
                message = "Test message for ${level.name}"
            )
            
            assertEquals(level, entry.level)
            assertEquals("Test", entry.tag)
            assertTrue(entry.message.contains(level.name))
        }
    }
}
