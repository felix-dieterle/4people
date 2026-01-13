package com.fourpeople.adhoc.trust

import java.io.Serializable

/**
 * Represents a verification (confirmation or rejection) of a message by a contact.
 * 
 * Users can mark messages as confirmed (true/trustworthy) or rejected (false/untrustworthy).
 * Verifications from contacts with higher trust levels carry more weight.
 */
data class MessageVerification(
    val messageId: String,
    val verifierId: String,
    val isConfirmed: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val comment: String = ""
) : Serializable {
    
    companion object {
        private const val serialVersionUID = 1L
    }
}
