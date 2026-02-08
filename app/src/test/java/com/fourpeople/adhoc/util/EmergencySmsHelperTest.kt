package com.fourpeople.adhoc.util

import android.content.Context
import android.content.SharedPreferences
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.mockito.Mockito.*

/**
 * Unit tests for EmergencySmsHelper.
 * Tests SMS configuration and contact management.
 */
class EmergencySmsHelperTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @Before
    fun setUp() {
        context = mock(Context::class.java)
        sharedPreferences = mock(SharedPreferences::class.java)
        editor = mock(SharedPreferences.Editor::class.java)

        `when`(context.getSharedPreferences(EmergencySmsHelper.PREF_NAME, Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
    }

    @Test
    fun testConstants() {
        assertEquals("4people_prefs", EmergencySmsHelper.PREF_NAME)
        assertEquals("emergency_contacts", EmergencySmsHelper.PREF_EMERGENCY_CONTACTS)
        assertEquals("sms_enabled", EmergencySmsHelper.PREF_SMS_ENABLED)
    }

    @Test
    fun testGetEmergencyContactsEmpty() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(0, contacts.size)
    }

    @Test
    fun testGetEmergencyContactsNull() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn(null)

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(0, contacts.size)
    }

    @Test
    fun testGetEmergencyContactsSingle() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(1, contacts.size)
        assertEquals("+1234567890", contacts[0])
    }

    @Test
    fun testGetEmergencyContactsMultiple() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890,+0987654321,+1122334455")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(3, contacts.size)
        assertEquals("+1234567890", contacts[0])
        assertEquals("+0987654321", contacts[1])
        assertEquals("+1122334455", contacts[2])
    }

    @Test
    fun testGetEmergencyContactsWithWhitespace() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890 , +0987654321 , +1122334455")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(3, contacts.size)
        assertEquals("+1234567890", contacts[0])
        assertEquals("+0987654321", contacts[1])
        assertEquals("+1122334455", contacts[2])
    }

    @Test
    fun testGetEmergencyContactsFiltersEmpty() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890,,+0987654321")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(2, contacts.size)
        assertFalse(contacts.contains(""))
    }

    @Test
    fun testSaveEmergencyContactsEmpty() {
        EmergencySmsHelper.saveEmergencyContacts(context, emptyList())

        verify(editor).putString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, "")
        verify(editor).apply()
    }

    @Test
    fun testSaveEmergencyContactsSingle() {
        EmergencySmsHelper.saveEmergencyContacts(context, listOf("+1234567890"))

        verify(editor).putString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, "+1234567890")
        verify(editor).apply()
    }

    @Test
    fun testSaveEmergencyContactsMultiple() {
        EmergencySmsHelper.saveEmergencyContacts(
            context,
            listOf("+1234567890", "+0987654321", "+1122334455")
        )

        verify(editor).putString(
            EmergencySmsHelper.PREF_EMERGENCY_CONTACTS,
            "+1234567890,+0987654321,+1122334455"
        )
        verify(editor).apply()
    }

    @Test
    fun testIsSmsEnabledDefault() {
        `when`(sharedPreferences.getBoolean(EmergencySmsHelper.PREF_SMS_ENABLED, false))
            .thenReturn(false)

        val enabled = EmergencySmsHelper.isSmsEnabled(context)

        assertFalse(enabled)
    }

    @Test
    fun testIsSmsEnabledTrue() {
        `when`(sharedPreferences.getBoolean(EmergencySmsHelper.PREF_SMS_ENABLED, false))
            .thenReturn(true)

        val enabled = EmergencySmsHelper.isSmsEnabled(context)

        assertTrue(enabled)
    }

    @Test
    fun testIsSmsEnabledFalse() {
        `when`(sharedPreferences.getBoolean(EmergencySmsHelper.PREF_SMS_ENABLED, false))
            .thenReturn(false)

        val enabled = EmergencySmsHelper.isSmsEnabled(context)

        assertFalse(enabled)
    }

    @Test
    fun testSetSmsEnabledTrue() {
        EmergencySmsHelper.setSmsEnabled(context, true)

        verify(editor).putBoolean(EmergencySmsHelper.PREF_SMS_ENABLED, true)
        verify(editor).apply()
    }

    @Test
    fun testSetSmsEnabledFalse() {
        EmergencySmsHelper.setSmsEnabled(context, false)

        verify(editor).putBoolean(EmergencySmsHelper.PREF_SMS_ENABLED, false)
        verify(editor).apply()
    }

    @Test
    fun testRoundTripContactsSave() {
        val originalContacts = listOf("+1234567890", "+0987654321")
        
        // Save contacts
        EmergencySmsHelper.saveEmergencyContacts(context, originalContacts)
        
        // Verify the string that was saved
        verify(editor).putString(
            EmergencySmsHelper.PREF_EMERGENCY_CONTACTS,
            "+1234567890,+0987654321"
        )
    }

    @Test
    fun testContactListParsing() {
        // Simulate saved string
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890,+0987654321,+1122334455")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(3, contacts.size)
        assertTrue(contacts.contains("+1234567890"))
        assertTrue(contacts.contains("+0987654321"))
        assertTrue(contacts.contains("+1122334455"))
    }

    @Test
    fun testSaveAndRetrieveContacts() {
        val testContacts = listOf("+1111111111", "+2222222222")
        
        // Setup mock to return saved value
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1111111111,+2222222222")

        // Save (mock will verify)
        EmergencySmsHelper.saveEmergencyContacts(context, testContacts)

        // Retrieve
        val retrieved = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(testContacts.size, retrieved.size)
        assertEquals(testContacts[0], retrieved[0])
        assertEquals(testContacts[1], retrieved[1])
    }

    @Test
    fun testInternationalPhoneNumbers() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+49301234567,+441234567890,+861234567890")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(3, contacts.size)
        assertEquals("+49301234567", contacts[0])  // Germany
        assertEquals("+441234567890", contacts[1])  // UK
        assertEquals("+861234567890", contacts[2])  // China
    }

    @Test
    fun testToggleSmsEnabled() {
        // Enable
        EmergencySmsHelper.setSmsEnabled(context, true)
        verify(editor).putBoolean(EmergencySmsHelper.PREF_SMS_ENABLED, true)

        // Disable
        EmergencySmsHelper.setSmsEnabled(context, false)
        verify(editor).putBoolean(EmergencySmsHelper.PREF_SMS_ENABLED, false)
    }

    @Test
    fun testEmptyStringInContactList() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890, ,+0987654321")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        // Empty strings should be filtered out
        assertEquals(2, contacts.size)
        assertEquals("+1234567890", contacts[0])
        assertEquals("+0987654321", contacts[1])
    }

    @Test
    fun testOnlyWhitespaceContactList() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("  ,  ,  ")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        // Should result in empty list
        assertEquals(0, contacts.size)
    }

    @Test
    fun testSingleContactWithTrailingComma() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890,")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(1, contacts.size)
        assertEquals("+1234567890", contacts[0])
    }

    @Test
    fun testSingleContactWithLeadingComma() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn(",+1234567890")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        assertEquals(1, contacts.size)
        assertEquals("+1234567890", contacts[0])
    }

    @Test
    fun testDuplicateContactsAllowed() {
        `when`(sharedPreferences.getString(EmergencySmsHelper.PREF_EMERGENCY_CONTACTS, ""))
            .thenReturn("+1234567890,+1234567890")

        val contacts = EmergencySmsHelper.getEmergencyContacts(context)

        // Duplicates are allowed (not filtered)
        assertEquals(2, contacts.size)
        assertEquals("+1234567890", contacts[0])
        assertEquals("+1234567890", contacts[1])
    }
}
