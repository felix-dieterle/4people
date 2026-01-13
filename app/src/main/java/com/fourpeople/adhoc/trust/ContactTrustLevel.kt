package com.fourpeople.adhoc.trust

import java.io.Serializable

/**
 * Represents the trust level for a contact in the emergency network.
 * 
 * Trust levels determine how much weight a contact's messages and verifications carry:
 * - Level 0: Unknown sender (no contact relationship)
 * - Level 1: Known contact (automatic from email/phone/messenger contacts)
 * - Level 2: Friend (manually set or suggested based on frequent contact)
 * - Level 3: Close/Family (manually set, highest trust)
 * 
 * Higher trust levels give more weight to:
 * - Direct messages from the contact
 * - Message verifications (confirmations/rejections) by the contact
 */
data class ContactTrustLevel(
    val contactId: String,
    val trustLevel: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isManuallySet: Boolean = false
) : Serializable {
    
    companion object {
        const val UNKNOWN = 0
        const val KNOWN_CONTACT = 1
        const val FRIEND = 2
        const val CLOSE_FAMILY = 3
        
        private const val serialVersionUID = 1L
        
        /**
         * Validates that trust level is within allowed range.
         */
        fun isValidTrustLevel(level: Int): Boolean {
            return level in UNKNOWN..CLOSE_FAMILY
        }
        
        /**
         * Returns a human-readable description of the trust level.
         */
        fun getDescription(level: Int): String {
            return when (level) {
                UNKNOWN -> "Unknown"
                KNOWN_CONTACT -> "Known Contact"
                FRIEND -> "Friend"
                CLOSE_FAMILY -> "Close/Family"
                else -> "Invalid"
            }
        }
    }
    
    init {
        require(isValidTrustLevel(trustLevel)) {
            "Trust level must be between $UNKNOWN and $CLOSE_FAMILY, got $trustLevel"
        }
    }
    
    /**
     * Returns the normalized trust factor (0.0 to 1.0) for calculations.
     */
    fun getTrustFactor(): Double {
        return when (trustLevel) {
            UNKNOWN -> 0.0
            KNOWN_CONTACT -> 0.33
            FRIEND -> 0.67
            CLOSE_FAMILY -> 1.0
            else -> 0.0
        }
    }
}
