package com.fourpeople.adhoc.trust

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

/**
 * Unit tests for TrustManager class.
 */
class TrustManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var trustManager: TrustManager
    
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
    }
    
    @Test
    fun testGetTrustLevel_unknownContact_returnsUnknown() {
        val trustLevel = trustManager.getTrustLevel("unknown_user")
        
        assertEquals("unknown_user", trustLevel.contactId)
        assertEquals(ContactTrustLevel.UNKNOWN, trustLevel.trustLevel)
        assertFalse(trustLevel.isManuallySet)
    }
    
    @Test
    fun testSetTrustLevel_validLevel_returnsTrue() {
        val result = trustManager.setTrustLevel("alice", ContactTrustLevel.FRIEND, isManuallySet = true)
        
        assertTrue(result)
        
        val trustLevel = trustManager.getTrustLevel("alice")
        assertEquals("alice", trustLevel.contactId)
        assertEquals(ContactTrustLevel.FRIEND, trustLevel.trustLevel)
        assertTrue(trustLevel.isManuallySet)
    }
    
    @Test
    fun testSetTrustLevel_invalidLevel_returnsFalse() {
        val result = trustManager.setTrustLevel("bob", 5, isManuallySet = true)
        
        assertFalse(result)
        
        // Should still be unknown
        val trustLevel = trustManager.getTrustLevel("bob")
        assertEquals(ContactTrustLevel.UNKNOWN, trustLevel.trustLevel)
    }
    
    @Test
    fun testSetTrustLevel_updateExisting_updatesLevel() {
        trustManager.setTrustLevel("charlie", ContactTrustLevel.KNOWN_CONTACT, isManuallySet = false)
        trustManager.setTrustLevel("charlie", ContactTrustLevel.CLOSE_FAMILY, isManuallySet = true)
        
        val trustLevel = trustManager.getTrustLevel("charlie")
        assertEquals(ContactTrustLevel.CLOSE_FAMILY, trustLevel.trustLevel)
        assertTrue(trustLevel.isManuallySet)
    }
    
    @Test
    fun testRemoveTrustLevel_existingContact_returnsTrue() {
        trustManager.setTrustLevel("dave", ContactTrustLevel.FRIEND)
        
        val result = trustManager.removeTrustLevel("dave")
        
        assertTrue(result)
        
        val trustLevel = trustManager.getTrustLevel("dave")
        assertEquals(ContactTrustLevel.UNKNOWN, trustLevel.trustLevel)
    }
    
    @Test
    fun testRemoveTrustLevel_nonExistingContact_returnsFalse() {
        val result = trustManager.removeTrustLevel("nonexistent")
        
        assertFalse(result)
    }
    
    @Test
    fun testGetAllTrustLevels_returnsAllContacts() {
        trustManager.setTrustLevel("alice", ContactTrustLevel.FRIEND)
        trustManager.setTrustLevel("bob", ContactTrustLevel.CLOSE_FAMILY)
        trustManager.setTrustLevel("charlie", ContactTrustLevel.KNOWN_CONTACT)
        
        val allLevels = trustManager.getAllTrustLevels()
        
        assertEquals(3, allLevels.size)
        assertTrue(allLevels.containsKey("alice"))
        assertTrue(allLevels.containsKey("bob"))
        assertTrue(allLevels.containsKey("charlie"))
    }
    
    @Test
    fun testGetContactsByTrustLevel_filtersByLevel() {
        trustManager.setTrustLevel("alice", ContactTrustLevel.FRIEND)
        trustManager.setTrustLevel("bob", ContactTrustLevel.FRIEND)
        trustManager.setTrustLevel("charlie", ContactTrustLevel.CLOSE_FAMILY)
        
        val friends = trustManager.getContactsByTrustLevel(ContactTrustLevel.FRIEND)
        
        assertEquals(2, friends.size)
        assertTrue(friends.any { it.contactId == "alice" })
        assertTrue(friends.any { it.contactId == "bob" })
    }
    
    @Test
    fun testImportKnownContacts_addsNewContacts() {
        val contactIds = listOf("alice", "bob", "charlie")
        
        val imported = trustManager.importKnownContacts(contactIds)
        
        assertEquals(3, imported)
        
        contactIds.forEach { contactId ->
            val trustLevel = trustManager.getTrustLevel(contactId)
            assertEquals(ContactTrustLevel.KNOWN_CONTACT, trustLevel.trustLevel)
            assertFalse(trustLevel.isManuallySet)
        }
    }
    
    @Test
    fun testImportKnownContacts_doesNotOverwriteExisting() {
        trustManager.setTrustLevel("alice", ContactTrustLevel.CLOSE_FAMILY, isManuallySet = true)
        
        val contactIds = listOf("alice", "bob")
        val imported = trustManager.importKnownContacts(contactIds)
        
        assertEquals(1, imported) // Only bob should be imported
        
        val aliceTrust = trustManager.getTrustLevel("alice")
        assertEquals(ContactTrustLevel.CLOSE_FAMILY, aliceTrust.trustLevel)
        assertTrue(aliceTrust.isManuallySet)
        
        val bobTrust = trustManager.getTrustLevel("bob")
        assertEquals(ContactTrustLevel.KNOWN_CONTACT, bobTrust.trustLevel)
    }
    
    @Test
    fun testClearAllTrustLevels_removesAll() {
        trustManager.setTrustLevel("alice", ContactTrustLevel.FRIEND)
        trustManager.setTrustLevel("bob", ContactTrustLevel.CLOSE_FAMILY)
        
        trustManager.clearAllTrustLevels()
        
        val allLevels = trustManager.getAllTrustLevels()
        assertEquals(0, allLevels.size)
    }
    
    @Test
    fun testGetTrustStatistics_calculatesCorrectly() {
        trustManager.setTrustLevel("alice", ContactTrustLevel.FRIEND)
        trustManager.setTrustLevel("bob", ContactTrustLevel.FRIEND)
        trustManager.setTrustLevel("charlie", ContactTrustLevel.CLOSE_FAMILY)
        trustManager.setTrustLevel("dave", ContactTrustLevel.KNOWN_CONTACT)
        
        val stats = trustManager.getTrustStatistics()
        
        assertEquals(4, stats.totalContacts)
        assertEquals(0, stats.unknownCount)
        assertEquals(1, stats.knownContactCount)
        assertEquals(2, stats.friendCount)
        assertEquals(1, stats.closeFamilyCount)
    }
    
    @Test
    fun testSetTrustLevel_withContactTrustLevelObject_works() {
        val contactTrust = ContactTrustLevel(
            contactId = "emily",
            trustLevel = ContactTrustLevel.FRIEND,
            isManuallySet = true
        )
        
        val result = trustManager.setTrustLevel(contactTrust)
        
        assertTrue(result)
        
        val retrieved = trustManager.getTrustLevel("emily")
        assertEquals("emily", retrieved.contactId)
        assertEquals(ContactTrustLevel.FRIEND, retrieved.trustLevel)
        assertTrue(retrieved.isManuallySet)
    }
}
