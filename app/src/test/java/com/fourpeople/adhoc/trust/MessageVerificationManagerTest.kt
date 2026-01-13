package com.fourpeople.adhoc.trust

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * Unit tests for MessageVerificationManager class.
 */
class MessageVerificationManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var verificationManager: MessageVerificationManager
    
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
        
        verificationManager = MessageVerificationManager(mockContext)
    }
    
    @Test
    fun testAddVerification_newVerification_returnsTrue() {
        val result = verificationManager.addVerification(
            messageId = "msg1",
            verifierId = "alice",
            isConfirmed = true,
            comment = "Looks good"
        )
        
        assertTrue(result)
    }
    
    @Test
    fun testAddVerification_duplicate_returnsFalse() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        
        val result = verificationManager.addVerification("msg1", "alice", isConfirmed = false)
        
        assertFalse(result)
    }
    
    @Test
    fun testGetVerifications_returnsCorrectVerifications() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg1", "bob", isConfirmed = false)
        verificationManager.addVerification("msg2", "charlie", isConfirmed = true)
        
        val verifications = verificationManager.getVerifications("msg1")
        
        assertEquals(2, verifications.size)
        assertTrue(verifications.any { it.verifierId == "alice" && it.isConfirmed })
        assertTrue(verifications.any { it.verifierId == "bob" && !it.isConfirmed })
    }
    
    @Test
    fun testGetVerifications_noVerifications_returnsEmpty() {
        val verifications = verificationManager.getVerifications("nonexistent")
        
        assertTrue(verifications.isEmpty())
    }
    
    @Test
    fun testGetVerificationsByUser_returnsUserVerifications() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg2", "alice", isConfirmed = false)
        verificationManager.addVerification("msg3", "bob", isConfirmed = true)
        
        val aliceVerifications = verificationManager.getVerificationsByUser("alice")
        
        assertEquals(2, aliceVerifications.size)
        assertTrue(aliceVerifications.all { it.verifierId == "alice" })
    }
    
    @Test
    fun testGetVerificationsByUser_noVerifications_returnsEmpty() {
        val verifications = verificationManager.getVerificationsByUser("nonexistent")
        
        assertTrue(verifications.isEmpty())
    }
    
    @Test
    fun testHasUserVerified_verified_returnsTrue() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        
        assertTrue(verificationManager.hasUserVerified("msg1", "alice"))
    }
    
    @Test
    fun testHasUserVerified_notVerified_returnsFalse() {
        assertFalse(verificationManager.hasUserVerified("msg1", "alice"))
    }
    
    @Test
    fun testGetVerificationStats_calculatesCorrectly() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg1", "bob", isConfirmed = true)
        verificationManager.addVerification("msg1", "charlie", isConfirmed = false)
        
        val stats = verificationManager.getVerificationStats("msg1")
        
        assertEquals("msg1", stats.messageId)
        assertEquals(3, stats.totalVerifications)
        assertEquals(2, stats.confirmations)
        assertEquals(1, stats.rejections)
    }
    
    @Test
    fun testGetVerificationStats_noVerifications_returnsZeros() {
        val stats = verificationManager.getVerificationStats("nonexistent")
        
        assertEquals("nonexistent", stats.messageId)
        assertEquals(0, stats.totalVerifications)
        assertEquals(0, stats.confirmations)
        assertEquals(0, stats.rejections)
    }
    
    @Test
    fun testVerificationStats_getNetScore_calculatesCorrectly() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg1", "bob", isConfirmed = true)
        verificationManager.addVerification("msg1", "charlie", isConfirmed = false)
        
        val stats = verificationManager.getVerificationStats("msg1")
        
        assertEquals(1, stats.getNetScore()) // 2 confirmations - 1 rejection
    }
    
    @Test
    fun testVerificationStats_hasPositiveConsensus_correctForPositive() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg1", "bob", isConfirmed = true)
        verificationManager.addVerification("msg1", "charlie", isConfirmed = false)
        
        val stats = verificationManager.getVerificationStats("msg1")
        
        assertTrue(stats.hasPositiveConsensus())
    }
    
    @Test
    fun testVerificationStats_hasPositiveConsensus_correctForNegative() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = false)
        verificationManager.addVerification("msg1", "bob", isConfirmed = false)
        verificationManager.addVerification("msg1", "charlie", isConfirmed = true)
        
        val stats = verificationManager.getVerificationStats("msg1")
        
        assertFalse(stats.hasPositiveConsensus())
    }
    
    @Test
    fun testRemoveMessageVerifications_existingMessage_returnsTrue() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg1", "bob", isConfirmed = false)
        
        val result = verificationManager.removeMessageVerifications("msg1")
        
        assertTrue(result)
        
        val verifications = verificationManager.getVerifications("msg1")
        assertTrue(verifications.isEmpty())
    }
    
    @Test
    fun testRemoveMessageVerifications_nonExistingMessage_returnsFalse() {
        val result = verificationManager.removeMessageVerifications("nonexistent")
        
        assertFalse(result)
    }
    
    @Test
    fun testRemoveMessageVerifications_updatesUserTracking() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        
        verificationManager.removeMessageVerifications("msg1")
        
        assertFalse(verificationManager.hasUserVerified("msg1", "alice"))
    }
    
    @Test
    fun testClearAllVerifications_removesAll() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg2", "bob", isConfirmed = false)
        
        verificationManager.clearAllVerifications()
        
        assertEquals(0, verificationManager.getTotalVerificationCount())
        assertTrue(verificationManager.getVerifications("msg1").isEmpty())
        assertTrue(verificationManager.getVerifications("msg2").isEmpty())
    }
    
    @Test
    fun testGetTotalVerificationCount_countsCorrectly() {
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        verificationManager.addVerification("msg1", "bob", isConfirmed = false)
        verificationManager.addVerification("msg2", "charlie", isConfirmed = true)
        
        assertEquals(3, verificationManager.getTotalVerificationCount())
    }
    
    @Test
    fun testAddVerification_withComment_storesComment() {
        verificationManager.addVerification(
            messageId = "msg1",
            verifierId = "alice",
            isConfirmed = true,
            comment = "This is definitely correct"
        )
        
        val verifications = verificationManager.getVerifications("msg1")
        
        assertEquals(1, verifications.size)
        assertEquals("This is definitely correct", verifications[0].comment)
    }
    
    @Test
    fun testAddVerification_withoutComment_hasEmptyComment() {
        verificationManager.addVerification(
            messageId = "msg1",
            verifierId = "alice",
            isConfirmed = true
        )
        
        val verifications = verificationManager.getVerifications("msg1")
        
        assertEquals(1, verifications.size)
        assertEquals("", verifications[0].comment)
    }
    
    @Test
    fun testMultipleUsersCanVerifySameMessage() {
        assertTrue(verificationManager.addVerification("msg1", "alice", true))
        assertTrue(verificationManager.addVerification("msg1", "bob", true))
        assertTrue(verificationManager.addVerification("msg1", "charlie", false))
        
        val verifications = verificationManager.getVerifications("msg1")
        assertEquals(3, verifications.size)
    }
    
    @Test
    fun testSameUserCanVerifyDifferentMessages() {
        assertTrue(verificationManager.addVerification("msg1", "alice", true))
        assertTrue(verificationManager.addVerification("msg2", "alice", false))
        assertTrue(verificationManager.addVerification("msg3", "alice", true))
        
        val aliceVerifications = verificationManager.getVerificationsByUser("alice")
        assertEquals(3, aliceVerifications.size)
    }
    
    @Test
    fun testVerificationTimestamp_isSet() {
        val beforeTime = System.currentTimeMillis()
        
        verificationManager.addVerification("msg1", "alice", isConfirmed = true)
        
        val afterTime = System.currentTimeMillis()
        
        val verifications = verificationManager.getVerifications("msg1")
        assertEquals(1, verifications.size)
        
        val timestamp = verifications[0].timestamp
        assertTrue(timestamp >= beforeTime)
        assertTrue(timestamp <= afterTime)
    }
}
