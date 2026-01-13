package com.fourpeople.adhoc.trust

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for trust system data classes.
 */
class TrustDataClassesTest {
    
    // ContactTrustLevel tests
    
    @Test
    fun testContactTrustLevel_validLevels_createsSuccessfully() {
        val unknown = ContactTrustLevel("user1", ContactTrustLevel.UNKNOWN)
        val known = ContactTrustLevel("user2", ContactTrustLevel.KNOWN_CONTACT)
        val friend = ContactTrustLevel("user3", ContactTrustLevel.FRIEND)
        val family = ContactTrustLevel("user4", ContactTrustLevel.CLOSE_FAMILY)
        
        assertEquals(ContactTrustLevel.UNKNOWN, unknown.trustLevel)
        assertEquals(ContactTrustLevel.KNOWN_CONTACT, known.trustLevel)
        assertEquals(ContactTrustLevel.FRIEND, friend.trustLevel)
        assertEquals(ContactTrustLevel.CLOSE_FAMILY, family.trustLevel)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testContactTrustLevel_invalidLevelNegative_throwsException() {
        ContactTrustLevel("user1", -1)
    }
    
    @Test(expected = IllegalArgumentException::class)
    fun testContactTrustLevel_invalidLevelTooHigh_throwsException() {
        ContactTrustLevel("user1", 4)
    }
    
    @Test
    fun testContactTrustLevel_isValidTrustLevel_validatesCorrectly() {
        assertTrue(ContactTrustLevel.isValidTrustLevel(0))
        assertTrue(ContactTrustLevel.isValidTrustLevel(1))
        assertTrue(ContactTrustLevel.isValidTrustLevel(2))
        assertTrue(ContactTrustLevel.isValidTrustLevel(3))
        assertFalse(ContactTrustLevel.isValidTrustLevel(-1))
        assertFalse(ContactTrustLevel.isValidTrustLevel(4))
    }
    
    @Test
    fun testContactTrustLevel_getDescription_returnsCorrectDescriptions() {
        assertEquals("Unknown", ContactTrustLevel.getDescription(0))
        assertEquals("Known Contact", ContactTrustLevel.getDescription(1))
        assertEquals("Friend", ContactTrustLevel.getDescription(2))
        assertEquals("Close/Family", ContactTrustLevel.getDescription(3))
        assertEquals("Invalid", ContactTrustLevel.getDescription(5))
    }
    
    @Test
    fun testContactTrustLevel_getTrustFactor_returnsCorrectValues() {
        assertEquals(0.0, ContactTrustLevel("u1", 0).getTrustFactor(), 0.01)
        assertEquals(0.33, ContactTrustLevel("u2", 1).getTrustFactor(), 0.01)
        assertEquals(0.67, ContactTrustLevel("u3", 2).getTrustFactor(), 0.01)
        assertEquals(1.0, ContactTrustLevel("u4", 3).getTrustFactor(), 0.01)
    }
    
    @Test
    fun testContactTrustLevel_isManuallySet_storesCorrectly() {
        val automatic = ContactTrustLevel("user1", 1, isManuallySet = false)
        val manual = ContactTrustLevel("user2", 2, isManuallySet = true)
        
        assertFalse(automatic.isManuallySet)
        assertTrue(manual.isManuallySet)
    }
    
    @Test
    fun testContactTrustLevel_lastUpdated_storesTimestamp() {
        val before = System.currentTimeMillis()
        val contact = ContactTrustLevel("user1", 1)
        val after = System.currentTimeMillis()
        
        assertTrue(contact.lastUpdated >= before)
        assertTrue(contact.lastUpdated <= after)
    }
    
    // MessageVerification tests
    
    @Test
    fun testMessageVerification_creation_storesAllFields() {
        val verification = MessageVerification(
            messageId = "msg1",
            verifierId = "alice",
            isConfirmed = true,
            comment = "Test comment"
        )
        
        assertEquals("msg1", verification.messageId)
        assertEquals("alice", verification.verifierId)
        assertTrue(verification.isConfirmed)
        assertEquals("Test comment", verification.comment)
    }
    
    @Test
    fun testMessageVerification_defaultComment_isEmpty() {
        val verification = MessageVerification(
            messageId = "msg1",
            verifierId = "alice",
            isConfirmed = false
        )
        
        assertEquals("", verification.comment)
    }
    
    @Test
    fun testMessageVerification_timestamp_isSet() {
        val before = System.currentTimeMillis()
        val verification = MessageVerification("msg1", "alice", true)
        val after = System.currentTimeMillis()
        
        assertTrue(verification.timestamp >= before)
        assertTrue(verification.timestamp <= after)
    }
    
    // MessageTrustEvaluation tests
    
    @Test
    fun testMessageTrustEvaluation_creation_storesAllFields() {
        val evaluation = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "alice",
            senderTrustLevel = ContactTrustLevel.FRIEND,
            hopCount = 2,
            confirmations = 3,
            rejections = 1,
            overallTrustScore = 0.75
        )
        
        assertEquals("msg1", evaluation.messageId)
        assertEquals("alice", evaluation.originalSenderId)
        assertEquals(ContactTrustLevel.FRIEND, evaluation.senderTrustLevel)
        assertEquals(2, evaluation.hopCount)
        assertEquals(3, evaluation.confirmations)
        assertEquals(1, evaluation.rejections)
        assertEquals(0.75, evaluation.overallTrustScore, 0.01)
    }
    
    @Test
    fun testMessageTrustEvaluation_defaultValues_work() {
        val evaluation = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "alice",
            senderTrustLevel = ContactTrustLevel.KNOWN_CONTACT,
            hopCount = 1,
            overallTrustScore = 0.5
        )
        
        assertEquals(0, evaluation.confirmations)
        assertEquals(0, evaluation.rejections)
    }
    
    @Test
    fun testMessageTrustEvaluation_getTrustRating_returnsCorrectRatings() {
        assertEquals("Very High", MessageTrustEvaluation.getTrustRating(0.95))
        assertEquals("Very High", MessageTrustEvaluation.getTrustRating(0.80))
        assertEquals("High", MessageTrustEvaluation.getTrustRating(0.70))
        assertEquals("High", MessageTrustEvaluation.getTrustRating(0.60))
        assertEquals("Medium", MessageTrustEvaluation.getTrustRating(0.50))
        assertEquals("Medium", MessageTrustEvaluation.getTrustRating(0.40))
        assertEquals("Low", MessageTrustEvaluation.getTrustRating(0.30))
        assertEquals("Low", MessageTrustEvaluation.getTrustRating(0.20))
        assertEquals("Very Low", MessageTrustEvaluation.getTrustRating(0.10))
        assertEquals("Very Low", MessageTrustEvaluation.getTrustRating(0.0))
    }
    
    @Test
    fun testMessageTrustEvaluation_getTrustEmoji_returnsCorrectEmojis() {
        assertEquals("✅", MessageTrustEvaluation.getTrustEmoji(0.95))
        assertEquals("✅", MessageTrustEvaluation.getTrustEmoji(0.80))
        assertEquals("👍", MessageTrustEvaluation.getTrustEmoji(0.70))
        assertEquals("👍", MessageTrustEvaluation.getTrustEmoji(0.60))
        assertEquals("⚠️", MessageTrustEvaluation.getTrustEmoji(0.50))
        assertEquals("⚠️", MessageTrustEvaluation.getTrustEmoji(0.40))
        assertEquals("⚡", MessageTrustEvaluation.getTrustEmoji(0.30))
        assertEquals("⚡", MessageTrustEvaluation.getTrustEmoji(0.20))
        assertEquals("❌", MessageTrustEvaluation.getTrustEmoji(0.10))
        assertEquals("❌", MessageTrustEvaluation.getTrustEmoji(0.0))
    }
    
    @Test
    fun testMessageTrustEvaluation_isHighTrust_correctForHigh() {
        val evaluation = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "alice",
            senderTrustLevel = ContactTrustLevel.CLOSE_FAMILY,
            hopCount = 0,
            overallTrustScore = 0.75
        )
        
        assertTrue(evaluation.isHighTrust())
    }
    
    @Test
    fun testMessageTrustEvaluation_isHighTrust_correctForLow() {
        val evaluation = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "unknown",
            senderTrustLevel = ContactTrustLevel.UNKNOWN,
            hopCount = 5,
            overallTrustScore = 0.25
        )
        
        assertFalse(evaluation.isHighTrust())
    }
    
    @Test
    fun testMessageTrustEvaluation_isLowTrust_correctForLow() {
        val evaluation = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "unknown",
            senderTrustLevel = ContactTrustLevel.UNKNOWN,
            hopCount = 5,
            overallTrustScore = 0.25
        )
        
        assertTrue(evaluation.isLowTrust())
    }
    
    @Test
    fun testMessageTrustEvaluation_isLowTrust_correctForHigh() {
        val evaluation = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "alice",
            senderTrustLevel = ContactTrustLevel.CLOSE_FAMILY,
            hopCount = 0,
            overallTrustScore = 0.75
        )
        
        assertFalse(evaluation.isLowTrust())
    }
    
    @Test
    fun testMessageTrustEvaluation_timestamp_isSet() {
        val before = System.currentTimeMillis()
        val evaluation = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "alice",
            senderTrustLevel = ContactTrustLevel.FRIEND,
            hopCount = 1,
            overallTrustScore = 0.5
        )
        val after = System.currentTimeMillis()
        
        assertTrue(evaluation.timestamp >= before)
        assertTrue(evaluation.timestamp <= after)
    }
}
