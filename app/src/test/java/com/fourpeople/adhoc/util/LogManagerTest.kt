package com.fourpeople.adhoc.util

import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject

/**
 * Unit tests for LogManager.
 * 
 * Note: LogManager uses Android framework classes (Handler, Looper, SharedPreferences)
 * which are not available in unit tests. These tests focus on the LogEntry data class
 * and LogLevel enum which can be tested without Android dependencies.
 * 
 * Full LogManager functionality should be tested with Android instrumentation tests.
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
    fun testLogEntryToJson() {
        val entry = LogManager.LogEntry(
            timestamp = 1234567890L,
            level = LogManager.LogLevel.ERROR,
            tag = "TestTag",
            message = "Test message"
        )
        
        val json = entry.toJson()
        
        assertNotNull(json)
        assertTrue(json.has("timestamp"))
        assertTrue(json.has("level"))
        assertTrue(json.has("tag"))
        assertTrue(json.has("message"))
        
        assertEquals(1234567890L, json.getLong("timestamp"))
        assertEquals("ERROR", json.getString("level"))
        assertEquals("TestTag", json.getString("tag"))
        assertEquals("Test message", json.getString("message"))
    }

    @Test
    fun testLogEntryFromJson() {
        val json = JSONObject().apply {
            put("timestamp", 1234567890L)
            put("level", "WARNING")
            put("tag", "JsonTest")
            put("message", "JSON test message")
        }
        
        val entry = LogManager.LogEntry.fromJson(json)
        
        assertEquals(1234567890L, entry.timestamp)
        assertEquals(LogManager.LogLevel.WARNING, entry.level)
        assertEquals("JsonTest", entry.tag)
        assertEquals("JSON test message", entry.message)
    }

    @Test
    fun testLogEntryRoundTripJson() {
        val original = LogManager.LogEntry(
            timestamp = 9876543210L,
            level = LogManager.LogLevel.EVENT,
            tag = "RoundTrip",
            message = "Round trip test"
        )
        
        val json = original.toJson()
        val reconstructed = LogManager.LogEntry.fromJson(json)
        
        assertEquals(original.timestamp, reconstructed.timestamp)
        assertEquals(original.level, reconstructed.level)
        assertEquals(original.tag, reconstructed.tag)
        assertEquals(original.message, reconstructed.message)
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
        
        // Test JSON serialization with special characters
        val json = entry.toJson()
        val reconstructed = LogManager.LogEntry.fromJson(json)
        assertEquals(specialMessage, reconstructed.message)
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
    fun testAllLogLevelsSerializeCorrectly() {
        val levels = LogManager.LogLevel.values()
        
        for (level in levels) {
            val entry = LogManager.LogEntry(
                timestamp = System.currentTimeMillis(),
                level = level,
                tag = "Test",
                message = "Test message"
            )
            
            val json = entry.toJson()
            val reconstructed = LogManager.LogEntry.fromJson(json)
            
            assertEquals(level, reconstructed.level)
        }
    }
}
