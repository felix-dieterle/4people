package com.fourpeople.adhoc.trust

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for MessageVerification data class.
 */
class MessageVerificationTest {

    @Test
    fun testMessageVerificationCreation() {
        val verification = MessageVerification(
            messageId = "msg-001",
            verifierId = "verifier-001",
            isConfirmed = true
        )

        assertEquals("msg-001", verification.messageId)
        assertEquals("verifier-001", verification.verifierId)
        assertTrue(verification.isConfirmed)
        assertEquals("", verification.comment)
        assertTrue(verification.timestamp > 0)
    }

    @Test
    fun testMessageVerificationConfirmed() {
        val verification = MessageVerification(
            messageId = "msg-002",
            verifierId = "verifier-002",
            isConfirmed = true
        )

        assertTrue(verification.isConfirmed)
    }

    @Test
    fun testMessageVerificationRejected() {
        val verification = MessageVerification(
            messageId = "msg-003",
            verifierId = "verifier-003",
            isConfirmed = false
        )

        assertFalse(verification.isConfirmed)
    }

    @Test
    fun testMessageVerificationWithComment() {
        val verification = MessageVerification(
            messageId = "msg-004",
            verifierId = "verifier-004",
            isConfirmed = true,
            comment = "This is accurate information"
        )

        assertEquals("This is accurate information", verification.comment)
    }

    @Test
    fun testMessageVerificationWithoutComment() {
        val verification = MessageVerification(
            messageId = "msg-005",
            verifierId = "verifier-005",
            isConfirmed = false
        )

        assertEquals("", verification.comment)
    }

    @Test
    fun testMessageVerificationTimestamp() {
        val beforeTime = System.currentTimeMillis()
        val verification = MessageVerification(
            messageId = "msg-006",
            verifierId = "verifier-006",
            isConfirmed = true
        )
        val afterTime = System.currentTimeMillis()

        assertTrue(verification.timestamp >= beforeTime)
        assertTrue(verification.timestamp <= afterTime)
    }

    @Test
    fun testMessageVerificationCustomTimestamp() {
        val customTimestamp = 1234567890L
        val verification = MessageVerification(
            messageId = "msg-007",
            verifierId = "verifier-007",
            isConfirmed = true,
            timestamp = customTimestamp
        )

        assertEquals(customTimestamp, verification.timestamp)
    }

    @Test
    fun testMessageVerificationEquality() {
        val verification1 = MessageVerification(
            messageId = "msg-008",
            verifierId = "verifier-008",
            isConfirmed = true,
            timestamp = 1234567890L,
            comment = "Test comment"
        )

        val verification2 = MessageVerification(
            messageId = "msg-008",
            verifierId = "verifier-008",
            isConfirmed = true,
            timestamp = 1234567890L,
            comment = "Test comment"
        )

        assertEquals(verification1, verification2)
        assertEquals(verification1.hashCode(), verification2.hashCode())
    }

    @Test
    fun testMessageVerificationInequality() {
        val verification1 = MessageVerification(
            messageId = "msg-009",
            verifierId = "verifier-009",
            isConfirmed = true
        )

        val verification2 = MessageVerification(
            messageId = "msg-010",
            verifierId = "verifier-010",
            isConfirmed = false
        )

        assertNotEquals(verification1, verification2)
    }

    @Test
    fun testMessageVerificationCopy() {
        val original = MessageVerification(
            messageId = "msg-011",
            verifierId = "verifier-011",
            isConfirmed = true,
            comment = "Original comment"
        )

        val copied = original.copy(isConfirmed = false, comment = "Updated comment")

        assertEquals("msg-011", copied.messageId)
        assertEquals("verifier-011", copied.verifierId)
        assertFalse(copied.isConfirmed)
        assertEquals("Updated comment", copied.comment)
    }

    @Test
    fun testMessageVerificationToString() {
        val verification = MessageVerification(
            messageId = "msg-012",
            verifierId = "verifier-012",
            isConfirmed = true,
            comment = "Test"
        )

        val string = verification.toString()

        assertTrue(string.contains("msg-012"))
        assertTrue(string.contains("verifier-012"))
        assertTrue(string.contains("true"))
    }

    @Test
    fun testMessageVerificationSerializable() {
        val verification = MessageVerification(
            messageId = "msg-013",
            verifierId = "verifier-013",
            isConfirmed = true
        )

        assertTrue(verification is java.io.Serializable)
    }

    @Test
    fun testEmptyMessageId() {
        val verification = MessageVerification(
            messageId = "",
            verifierId = "verifier-014",
            isConfirmed = true
        )

        assertEquals("", verification.messageId)
    }

    @Test
    fun testEmptyVerifierId() {
        val verification = MessageVerification(
            messageId = "msg-015",
            verifierId = "",
            isConfirmed = true
        )

        assertEquals("", verification.verifierId)
    }

    @Test
    fun testLongComment() {
        val longComment = "This is a very long comment. ".repeat(100)
        val verification = MessageVerification(
            messageId = "msg-016",
            verifierId = "verifier-016",
            isConfirmed = true,
            comment = longComment
        )

        assertEquals(longComment, verification.comment)
    }

    @Test
    fun testSpecialCharactersInComment() {
        val specialComment = "Comment with \"quotes\", \nnewlines, and \ttabs"
        val verification = MessageVerification(
            messageId = "msg-017",
            verifierId = "verifier-017",
            isConfirmed = true,
            comment = specialComment
        )

        assertEquals(specialComment, verification.comment)
    }

    @Test
    fun testMultipleVerificationsForSameMessage() {
        val verification1 = MessageVerification(
            messageId = "msg-018",
            verifierId = "verifier-018a",
            isConfirmed = true
        )

        val verification2 = MessageVerification(
            messageId = "msg-018",
            verifierId = "verifier-018b",
            isConfirmed = false
        )

        // Same message, different verifiers
        assertEquals(verification1.messageId, verification2.messageId)
        assertNotEquals(verification1.verifierId, verification2.verifierId)
        assertNotEquals(verification1, verification2)
    }

    @Test
    fun testSameVerifierMultipleMessages() {
        val verification1 = MessageVerification(
            messageId = "msg-019a",
            verifierId = "verifier-019",
            isConfirmed = true
        )

        val verification2 = MessageVerification(
            messageId = "msg-019b",
            verifierId = "verifier-019",
            isConfirmed = true
        )

        // Same verifier, different messages
        assertEquals(verification1.verifierId, verification2.verifierId)
        assertNotEquals(verification1.messageId, verification2.messageId)
        assertNotEquals(verification1, verification2)
    }

    @Test
    fun testVerificationChangeOverTime() {
        // Simulating a verifier changing their mind
        val firstVerification = MessageVerification(
            messageId = "msg-020",
            verifierId = "verifier-020",
            isConfirmed = true,
            timestamp = 1000L,
            comment = "Initially seems correct"
        )

        val updatedVerification = MessageVerification(
            messageId = "msg-020",
            verifierId = "verifier-020",
            isConfirmed = false,
            timestamp = 2000L,
            comment = "Upon further review, this is incorrect"
        )

        assertEquals(firstVerification.messageId, updatedVerification.messageId)
        assertEquals(firstVerification.verifierId, updatedVerification.verifierId)
        assertNotEquals(firstVerification.isConfirmed, updatedVerification.isConfirmed)
        assertTrue(updatedVerification.timestamp > firstVerification.timestamp)
    }

    @Test
    fun testNonAsciiCharactersInComment() {
        val unicodeComment = "Verification: 确认 ✓ ❌ 🚨"
        val verification = MessageVerification(
            messageId = "msg-021",
            verifierId = "verifier-021",
            isConfirmed = true,
            comment = unicodeComment
        )

        assertEquals(unicodeComment, verification.comment)
    }
}
