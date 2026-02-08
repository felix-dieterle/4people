package com.fourpeople.adhoc.trust

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ContactTrustLevel data class.
 */
class ContactTrustLevelTest {

    @Test
    fun testTrustLevelConstants() {
        assertEquals(0, ContactTrustLevel.UNKNOWN)
        assertEquals(1, ContactTrustLevel.KNOWN_CONTACT)
        assertEquals(2, ContactTrustLevel.FRIEND)
        assertEquals(3, ContactTrustLevel.CLOSE_FAMILY)
    }

    @Test
    fun testContactTrustLevelCreation() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-001",
            trustLevel = ContactTrustLevel.FRIEND
        )

        assertEquals("contact-001", trustLevel.contactId)
        assertEquals(ContactTrustLevel.FRIEND, trustLevel.trustLevel)
        assertFalse(trustLevel.isManuallySet)
        assertTrue(trustLevel.lastUpdated > 0)
    }

    @Test
    fun testContactTrustLevelUnknown() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-002",
            trustLevel = ContactTrustLevel.UNKNOWN
        )

        assertEquals(ContactTrustLevel.UNKNOWN, trustLevel.trustLevel)
        assertEquals(0.0, trustLevel.getTrustFactor(), 0.001)
    }

    @Test
    fun testContactTrustLevelKnownContact() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-003",
            trustLevel = ContactTrustLevel.KNOWN_CONTACT
        )

        assertEquals(ContactTrustLevel.KNOWN_CONTACT, trustLevel.trustLevel)
        assertEquals(0.33, trustLevel.getTrustFactor(), 0.001)
    }

    @Test
    fun testContactTrustLevelFriend() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-004",
            trustLevel = ContactTrustLevel.FRIEND
        )

        assertEquals(ContactTrustLevel.FRIEND, trustLevel.trustLevel)
        assertEquals(0.67, trustLevel.getTrustFactor(), 0.001)
    }

    @Test
    fun testContactTrustLevelCloseFamily() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-005",
            trustLevel = ContactTrustLevel.CLOSE_FAMILY
        )

        assertEquals(ContactTrustLevel.CLOSE_FAMILY, trustLevel.trustLevel)
        assertEquals(1.0, trustLevel.getTrustFactor(), 0.001)
    }

    @Test
    fun testContactTrustLevelManuallySet() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-006",
            trustLevel = ContactTrustLevel.FRIEND,
            isManuallySet = true
        )

        assertTrue(trustLevel.isManuallySet)
    }

    @Test
    fun testContactTrustLevelAutoSet() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-007",
            trustLevel = ContactTrustLevel.KNOWN_CONTACT,
            isManuallySet = false
        )

        assertFalse(trustLevel.isManuallySet)
    }

    @Test
    fun testContactTrustLevelTimestamp() {
        val beforeTime = System.currentTimeMillis()
        val trustLevel = ContactTrustLevel(
            contactId = "contact-008",
            trustLevel = ContactTrustLevel.FRIEND
        )
        val afterTime = System.currentTimeMillis()

        assertTrue(trustLevel.lastUpdated >= beforeTime)
        assertTrue(trustLevel.lastUpdated <= afterTime)
    }

    @Test
    fun testContactTrustLevelCustomTimestamp() {
        val customTimestamp = 1234567890L
        val trustLevel = ContactTrustLevel(
            contactId = "contact-009",
            trustLevel = ContactTrustLevel.FRIEND,
            lastUpdated = customTimestamp
        )

        assertEquals(customTimestamp, trustLevel.lastUpdated)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testContactTrustLevelInvalidNegative() {
        ContactTrustLevel(
            contactId = "contact-010",
            trustLevel = -1
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testContactTrustLevelInvalidTooHigh() {
        ContactTrustLevel(
            contactId = "contact-011",
            trustLevel = 4
        )
    }

    @Test
    fun testIsValidTrustLevel() {
        assertTrue(ContactTrustLevel.isValidTrustLevel(0))
        assertTrue(ContactTrustLevel.isValidTrustLevel(1))
        assertTrue(ContactTrustLevel.isValidTrustLevel(2))
        assertTrue(ContactTrustLevel.isValidTrustLevel(3))
        
        assertFalse(ContactTrustLevel.isValidTrustLevel(-1))
        assertFalse(ContactTrustLevel.isValidTrustLevel(4))
        assertFalse(ContactTrustLevel.isValidTrustLevel(100))
    }

    @Test
    fun testGetDescription() {
        assertEquals("Unknown", ContactTrustLevel.getDescription(ContactTrustLevel.UNKNOWN))
        assertEquals("Known Contact", ContactTrustLevel.getDescription(ContactTrustLevel.KNOWN_CONTACT))
        assertEquals("Friend", ContactTrustLevel.getDescription(ContactTrustLevel.FRIEND))
        assertEquals("Close/Family", ContactTrustLevel.getDescription(ContactTrustLevel.CLOSE_FAMILY))
        assertEquals("Invalid", ContactTrustLevel.getDescription(-1))
        assertEquals("Invalid", ContactTrustLevel.getDescription(4))
    }

    @Test
    fun testTrustFactorRange() {
        // All trust factors should be between 0.0 and 1.0
        for (level in 0..3) {
            val trustLevel = ContactTrustLevel("test", level)
            val factor = trustLevel.getTrustFactor()
            assertTrue(factor >= 0.0)
            assertTrue(factor <= 1.0)
        }
    }

    @Test
    fun testTrustFactorOrdering() {
        // Higher trust levels should have higher trust factors
        val unknown = ContactTrustLevel("test1", ContactTrustLevel.UNKNOWN)
        val known = ContactTrustLevel("test2", ContactTrustLevel.KNOWN_CONTACT)
        val friend = ContactTrustLevel("test3", ContactTrustLevel.FRIEND)
        val family = ContactTrustLevel("test4", ContactTrustLevel.CLOSE_FAMILY)

        assertTrue(known.getTrustFactor() > unknown.getTrustFactor())
        assertTrue(friend.getTrustFactor() > known.getTrustFactor())
        assertTrue(family.getTrustFactor() > friend.getTrustFactor())
    }

    @Test
    fun testContactTrustLevelEquality() {
        val trustLevel1 = ContactTrustLevel(
            contactId = "contact-012",
            trustLevel = ContactTrustLevel.FRIEND,
            lastUpdated = 1234567890L,
            isManuallySet = true
        )

        val trustLevel2 = ContactTrustLevel(
            contactId = "contact-012",
            trustLevel = ContactTrustLevel.FRIEND,
            lastUpdated = 1234567890L,
            isManuallySet = true
        )

        assertEquals(trustLevel1, trustLevel2)
        assertEquals(trustLevel1.hashCode(), trustLevel2.hashCode())
    }

    @Test
    fun testContactTrustLevelInequality() {
        val trustLevel1 = ContactTrustLevel(
            contactId = "contact-013",
            trustLevel = ContactTrustLevel.FRIEND
        )

        val trustLevel2 = ContactTrustLevel(
            contactId = "contact-014",
            trustLevel = ContactTrustLevel.FRIEND
        )

        assertNotEquals(trustLevel1, trustLevel2)
    }

    @Test
    fun testContactTrustLevelCopy() {
        val original = ContactTrustLevel(
            contactId = "contact-015",
            trustLevel = ContactTrustLevel.KNOWN_CONTACT,
            isManuallySet = false
        )

        val copied = original.copy(trustLevel = ContactTrustLevel.FRIEND, isManuallySet = true)

        assertEquals("contact-015", copied.contactId)
        assertEquals(ContactTrustLevel.FRIEND, copied.trustLevel)
        assertTrue(copied.isManuallySet)
    }

    @Test
    fun testContactTrustLevelToString() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-016",
            trustLevel = ContactTrustLevel.FRIEND
        )

        val string = trustLevel.toString()

        assertTrue(string.contains("contact-016"))
        assertTrue(string.contains(ContactTrustLevel.FRIEND.toString()))
    }

    @Test
    fun testContactTrustLevelSerializable() {
        val trustLevel = ContactTrustLevel(
            contactId = "contact-017",
            trustLevel = ContactTrustLevel.FRIEND
        )

        assertTrue(trustLevel is java.io.Serializable)
    }

    @Test
    fun testEmptyContactId() {
        val trustLevel = ContactTrustLevel(
            contactId = "",
            trustLevel = ContactTrustLevel.KNOWN_CONTACT
        )

        assertEquals("", trustLevel.contactId)
    }

    @Test
    fun testSpecialCharactersInContactId() {
        val specialId = "contact-@#$%^&*()"
        val trustLevel = ContactTrustLevel(
            contactId = specialId,
            trustLevel = ContactTrustLevel.FRIEND
        )

        assertEquals(specialId, trustLevel.contactId)
    }

    @Test
    fun testVeryLongContactId() {
        val longId = "a".repeat(1000)
        val trustLevel = ContactTrustLevel(
            contactId = longId,
            trustLevel = ContactTrustLevel.FRIEND
        )

        assertEquals(longId, trustLevel.contactId)
    }
}
