package com.fourpeople.adhoc.trust

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for MessageTrustCalculator class.
 */
@RunWith(RobolectricTestRunner::class)
class MessageTrustCalculatorTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var trustManager: TrustManager
    private lateinit var calculator: MessageTrustCalculator
    
    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPreferences = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences)
        `when`(mockPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }
        `when`(mockPreferences.getString(anyString(), any())).thenReturn(null)
        
        trustManager = TrustManager(mockContext)
        calculator = MessageTrustCalculator(trustManager)
        
        // Set up test contacts
        trustManager.setTrustLevel("family", ContactTrustLevel.CLOSE_FAMILY)
        trustManager.setTrustLevel("friend", ContactTrustLevel.FRIEND)
        trustManager.setTrustLevel("known", ContactTrustLevel.KNOWN_CONTACT)
    }
    
    @Test
    fun testCalculateTrustScore_directFromFamily_veryHigh() {
        val score = calculator.calculateTrustScore(
            originalSenderId = "family",
            hopCount = 0,
            verifications = emptyList()
        )
        
        // Trust factor: 1.0 * 0.5 (sender) + 1.0 * 0.3 (hops) + 0.1 (security) = 0.9
        assertEquals(0.90, score, 0.01)
    }
    
    @Test
    fun testCalculateTrustScore_directFromFriend_high() {
        val score = calculator.calculateTrustScore(
            originalSenderId = "friend",
            hopCount = 0,
            verifications = emptyList()
        )
        
        // Trust factor: 0.67 * 0.5 (sender) + 1.0 * 0.3 (hops) + 0.1 (security) = 0.735
        assertEquals(0.735, score, 0.01)
    }
    
    @Test
    fun testCalculateTrustScore_directFromKnown_medium() {
        val score = calculator.calculateTrustScore(
            originalSenderId = "known",
            hopCount = 0,
            verifications = emptyList()
        )
        
        // Trust factor: 0.33 * 0.5 (sender) + 1.0 * 0.3 (hops) + 0.1 (security) = 0.565
        assertEquals(0.565, score, 0.01)
    }
    
    @Test
    fun testCalculateTrustScore_directFromUnknown_low() {
        val score = calculator.calculateTrustScore(
            originalSenderId = "unknown",
            hopCount = 0,
            verifications = emptyList()
        )
        
        // Trust factor: 0.0 * 0.5 (sender) + 1.0 * 0.3 (hops) + 0.1 (security) = 0.40
        assertEquals(0.40, score, 0.01)
    }
    
    @Test
    fun testCalculateTrustScore_friendWith2Hops_decent() {
        val score = calculator.calculateTrustScore(
            originalSenderId = "friend",
            hopCount = 2,
            verifications = emptyList()
        )
        
        // Sender: 0.67 * 0.5 = 0.335
        // Hop penalty: 2 * 0.1 = 0.2
        // Hop score: (1.0 - 0.2) * 0.3 = 0.24
        // Security: 0.1
        // Total: 0.335 + 0.24 + 0.1 = 0.675
        assertEquals(0.675, score, 0.01)
    }
    
    @Test
    fun testCalculateTrustScore_unknownWith5Hops_veryLow() {
        val score = calculator.calculateTrustScore(
            originalSenderId = "unknown",
            hopCount = 5,
            verifications = emptyList()
        )
        
        // Sender: 0.0 * 0.5 = 0.0
        // Hop penalty: 5 * 0.1 = 0.5 (max penalty)
        // Hop score: (1.0 - 0.5) * 0.3 = 0.15
        // Security: 0.1
        // Total: 0.0 + 0.15 + 0.1 = 0.25
        assertEquals(0.25, score, 0.01)
    }
    
    @Test
    fun testCalculateTrustScore_maxHopPenalty_capped() {
        val score1 = calculator.calculateTrustScore("unknown", 5, emptyList())
        val score2 = calculator.calculateTrustScore("unknown", 10, emptyList())
        
        // Both should have same score due to max penalty cap
        assertEquals(score1, score2, 0.01)
    }
    
    @Test
    fun testCalculateTrustScore_withFamilyConfirmation_increases() {
        val verifications = listOf(
            MessageVerification("msg1", "family", isConfirmed = true)
        )
        
        val scoreWithoutVerification = calculator.calculateTrustScore("known", 3, emptyList())
        val scoreWithVerification = calculator.calculateTrustScore("known", 3, verifications)
        
        assertTrue(scoreWithVerification > scoreWithoutVerification)
    }
    
    @Test
    fun testCalculateTrustScore_withFamilyRejection_decreases() {
        val verifications = listOf(
            MessageVerification("msg1", "family", isConfirmed = false)
        )
        
        val scoreWithoutVerification = calculator.calculateTrustScore("friend", 1, emptyList())
        val scoreWithVerification = calculator.calculateTrustScore("friend", 1, verifications)
        
        assertTrue(scoreWithVerification < scoreWithoutVerification)
    }
    
    @Test
    fun testCalculateTrustScore_multipleConfirmations_weightedCorrectly() {
        val verifications = listOf(
            MessageVerification("msg1", "family", isConfirmed = true),   // weight 1.0
            MessageVerification("msg1", "friend", isConfirmed = true)     // weight 0.67
        )
        
        val score = calculator.calculateTrustScore("known", 3, verifications)
        
        // Base score for known with 3 hops
        val baseScore = calculator.calculateTrustScore("known", 3, emptyList())
        
        // Should have positive adjustment
        assertTrue(score > baseScore)
        
        // Adjustment should be capped at 0.15
        assertTrue(score - baseScore <= 0.16) // Small tolerance
    }
    
    @Test
    fun testCalculateTrustScore_mixedVerifications_balanced() {
        val verifications = listOf(
            MessageVerification("msg1", "family", isConfirmed = true),   // +1.0
            MessageVerification("msg1", "friend", isConfirmed = false)   // -0.67
        )
        
        val scoreWithoutVerification = calculator.calculateTrustScore("known", 1, emptyList())
        val scoreWithVerification = calculator.calculateTrustScore("known", 1, verifications)
        
        // Net is positive, so score should increase slightly
        assertTrue(scoreWithVerification > scoreWithoutVerification)
    }
    
    @Test
    fun testCalculateTrustScore_verificationsFromUnknown_noEffect() {
        val verifications = listOf(
            MessageVerification("msg1", "unknown_verifier", isConfirmed = true)
        )
        
        val scoreWithoutVerification = calculator.calculateTrustScore("friend", 1, emptyList())
        val scoreWithVerification = calculator.calculateTrustScore("friend", 1, verifications)
        
        // Unknown verifiers (trust factor 0) should not affect score
        assertEquals(scoreWithoutVerification, scoreWithVerification, 0.01)
    }
    
    @Test
    fun testEvaluateMessage_createsCompleteEvaluation() {
        val verifications = listOf(
            MessageVerification("msg1", "family", isConfirmed = true),
            MessageVerification("msg1", "friend", isConfirmed = false)
        )
        
        val evaluation = calculator.evaluateMessage(
            messageId = "msg1",
            originalSenderId = "known",
            hopCount = 2,
            verifications = verifications
        )
        
        assertEquals("msg1", evaluation.messageId)
        assertEquals("known", evaluation.originalSenderId)
        assertEquals(ContactTrustLevel.KNOWN_CONTACT, evaluation.senderTrustLevel)
        assertEquals(2, evaluation.hopCount)
        assertEquals(1, evaluation.confirmations)
        assertEquals(1, evaluation.rejections)
        assertTrue(evaluation.overallTrustScore > 0.0)
        assertTrue(evaluation.overallTrustScore <= 1.0)
    }
    
    @Test
    fun testCompareTrust_comparesCorrectly() {
        val eval1 = MessageTrustEvaluation(
            messageId = "msg1",
            originalSenderId = "family",
            senderTrustLevel = ContactTrustLevel.CLOSE_FAMILY,
            hopCount = 0,
            overallTrustScore = 0.9
        )
        
        val eval2 = MessageTrustEvaluation(
            messageId = "msg2",
            originalSenderId = "unknown",
            senderTrustLevel = ContactTrustLevel.UNKNOWN,
            hopCount = 5,
            overallTrustScore = 0.3
        )
        
        assertTrue(calculator.compareTrust(eval1, eval2) > 0)
        assertTrue(calculator.compareTrust(eval2, eval1) < 0)
        assertEquals(0, calculator.compareTrust(eval1, eval1))
    }
    
    @Test
    fun testFilterByMinTrust_filtersCorrectly() {
        val evaluations = listOf(
            MessageTrustEvaluation("msg1", "family", 3, 0, overallTrustScore = 0.9),
            MessageTrustEvaluation("msg2", "friend", 2, 1, overallTrustScore = 0.7),
            MessageTrustEvaluation("msg3", "known", 1, 3, overallTrustScore = 0.5),
            MessageTrustEvaluation("msg4", "unknown", 0, 5, overallTrustScore = 0.2)
        )
        
        val filtered = calculator.filterByMinTrust(evaluations, 0.6)
        
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.overallTrustScore >= 0.6 })
    }
    
    @Test
    fun testSortByTrust_sortsDescending() {
        val evaluations = listOf(
            MessageTrustEvaluation("msg1", "unknown", 0, 5, overallTrustScore = 0.2),
            MessageTrustEvaluation("msg2", "family", 3, 0, overallTrustScore = 0.9),
            MessageTrustEvaluation("msg3", "friend", 2, 1, overallTrustScore = 0.7)
        )
        
        val sorted = calculator.sortByTrust(evaluations)
        
        assertEquals("msg2", sorted[0].messageId) // 0.9
        assertEquals("msg3", sorted[1].messageId) // 0.7
        assertEquals("msg1", sorted[2].messageId) // 0.2
    }
    
    @Test
    fun testCalculateTrustScore_neverExceedsOne() {
        // Even with many confirmations, score should not exceed 1.0
        val verifications = listOf(
            MessageVerification("msg1", "family", isConfirmed = true),
            MessageVerification("msg1", "friend", isConfirmed = true),
            MessageVerification("msg1", "known", isConfirmed = true)
        )
        
        val score = calculator.calculateTrustScore("family", 0, verifications)
        
        assertTrue(score <= 1.0)
    }
    
    @Test
    fun testCalculateTrustScore_neverBelowZero() {
        // Even with many rejections, score should not go below 0.0
        val verifications = listOf(
            MessageVerification("msg1", "family", isConfirmed = false),
            MessageVerification("msg1", "friend", isConfirmed = false),
            MessageVerification("msg1", "known", isConfirmed = false)
        )
        
        val score = calculator.calculateTrustScore("unknown", 10, verifications)
        
        assertTrue(score >= 0.0)
    }
}
