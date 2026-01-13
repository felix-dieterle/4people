package com.fourpeople.adhoc.trust

import java.io.Serializable

/**
 * Represents the trust evaluation result for a message.
 * 
 * This combines multiple factors to produce an overall trust score:
 * - Sender's trust level
 * - Number of hops from original sender
 * - Confirmations and rejections from other contacts
 */
data class MessageTrustEvaluation(
    val messageId: String,
    val originalSenderId: String,
    val senderTrustLevel: Int,
    val hopCount: Int,
    val confirmations: Int = 0,
    val rejections: Int = 0,
    val overallTrustScore: Double,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable {
    
    companion object {
        private const val serialVersionUID = 1L
        
        /**
         * Returns a human-readable trust rating.
         */
        fun getTrustRating(score: Double): String {
            return when {
                score >= 0.8 -> "Very High"
                score >= 0.6 -> "High"
                score >= 0.4 -> "Medium"
                score >= 0.2 -> "Low"
                else -> "Very Low"
            }
        }
        
        /**
         * Returns an emoji indicator for the trust level.
         */
        fun getTrustEmoji(score: Double): String {
            return when {
                score >= 0.8 -> "✅"
                score >= 0.6 -> "👍"
                score >= 0.4 -> "⚠️"
                score >= 0.2 -> "⚡"
                else -> "❌"
            }
        }
    }
    
    /**
     * Returns true if the message has high trust and should be prominently displayed.
     */
    fun isHighTrust(): Boolean = overallTrustScore >= 0.6
    
    /**
     * Returns true if the message has low trust and should be treated with caution.
     */
    fun isLowTrust(): Boolean = overallTrustScore < 0.4
}
