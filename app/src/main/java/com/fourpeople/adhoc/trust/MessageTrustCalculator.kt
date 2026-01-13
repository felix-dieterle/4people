package com.fourpeople.adhoc.trust

import android.util.Log
import kotlin.math.max
import kotlin.math.min

/**
 * Calculates trust scores for messages in the emergency network.
 * 
 * The trust score is calculated based on multiple factors:
 * 1. Sender's trust level (higher = more trustworthy)
 * 2. Number of hops from original sender (fewer hops = more trustworthy)
 * 3. Message verifications (confirmations/rejections weighted by verifier trust level)
 * 
 * Algorithm Details:
 * - Base Trust: Derived from sender's trust level and hop count
 * - Direct messages from high-trust contacts have the highest scores
 * - Each hop reduces trust slightly (information degradation)
 * - Verifications from trusted contacts can increase or decrease the score
 * - Final score is normalized to 0.0 - 1.0 range
 * 
 * Example Scenarios:
 * 1. Direct message from close family (trust level 3, 0 hops) = ~0.95 trust score
 * 2. Message from friend via 2 hops (trust level 2, 2 hops) = ~0.55 trust score
 * 3. Message from unknown via 5 hops (trust level 0, 5 hops) = ~0.05 trust score
 * 4. Low trust message confirmed by 2 close contacts = score increases by ~0.30
 */
class MessageTrustCalculator(private val trustManager: TrustManager) {
    
    companion object {
        private const val TAG = "MessageTrustCalculator"
        
        // Weight factors for the algorithm (adjusted to accommodate security factor)
        private const val SENDER_TRUST_WEIGHT = 0.5 // 50% weight on sender trust
        private const val HOP_COUNT_WEIGHT = 0.3 // 30% weight on hop count
        private const val CONNECTION_SECURITY_WEIGHT = 0.1 // 10% weight on connection security
        
        // Hop penalty: each hop reduces trust score
        private const val HOP_PENALTY_FACTOR = 0.1 // 10% reduction per hop
        private const val MAX_HOP_PENALTY = 0.5 // Maximum 50% reduction from hops
        
        // Verification adjustment factors
        private const val VERIFICATION_WEIGHT = 0.15 // Max 15% adjustment from verifications
        
        // Connection security bonus/penalty
        private const val INSECURE_CONNECTION_PENALTY = -0.1 // Penalty for insecure paths
    }
    
    /**
     * Calculates the trust score for a message.
     * 
     * @param originalSenderId The ID of the contact who originally sent the message
     * @param hopCount Number of hops the message traveled (0 = direct)
     * @param verifications List of verifications for this message
     * @param hasInsecureHop Whether the message path contains at least one insecure connection
     * @return Trust score between 0.0 (no trust) and 1.0 (complete trust)
     */
    fun calculateTrustScore(
        originalSenderId: String,
        hopCount: Int,
        verifications: List<MessageVerification> = emptyList(),
        hasInsecureHop: Boolean = false
    ): Double {
        // Get sender's trust level
        val senderTrust = trustManager.getTrustLevel(originalSenderId)
        val senderTrustFactor = senderTrust.getTrustFactor()
        
        // Calculate base trust from sender
        val baseTrustFromSender = senderTrustFactor * SENDER_TRUST_WEIGHT
        
        // Calculate hop penalty (more hops = less trust)
        val hopPenalty = min(hopCount * HOP_PENALTY_FACTOR, MAX_HOP_PENALTY)
        val hopScore = max(0.0, (1.0 - hopPenalty)) * HOP_COUNT_WEIGHT
        
        // Calculate connection security score
        val securityScore = if (hasInsecureHop) {
            INSECURE_CONNECTION_PENALTY
        } else {
            CONNECTION_SECURITY_WEIGHT
        }
        
        // Calculate base score before verifications
        val baseScore = baseTrustFromSender + hopScore + securityScore
        
        // Calculate verification adjustment
        val verificationAdjustment = calculateVerificationAdjustment(verifications)
        
        // Final score with verification adjustment
        val finalScore = (baseScore + verificationAdjustment).coerceIn(0.0, 1.0)
        
        Log.d(TAG, "Trust score calculated: sender=${senderTrust.trustLevel}, hops=$hopCount, " +
                "secure=${!hasInsecureHop}, base=${"%.2f".format(baseScore)}, " +
                "verifications=${verifications.size}, final=${"%.2f".format(finalScore)}")
        
        return finalScore
    }
    
    /**
     * Creates a full MessageTrustEvaluation for a message.
     * 
     * @param messageId The unique identifier of the message
     * @param originalSenderId The ID of the contact who originally sent the message
     * @param hopCount Number of hops the message traveled
     * @param verifications List of verifications for this message
     * @param hasInsecureHop Whether the message path contains at least one insecure connection
     * @return Complete trust evaluation with all details
     */
    fun evaluateMessage(
        messageId: String,
        originalSenderId: String,
        hopCount: Int,
        verifications: List<MessageVerification> = emptyList(),
        hasInsecureHop: Boolean = false
    ): MessageTrustEvaluation {
        val senderTrust = trustManager.getTrustLevel(originalSenderId)
        val trustScore = calculateTrustScore(originalSenderId, hopCount, verifications, hasInsecureHop)
        
        val confirmations = verifications.count { it.isConfirmed }
        val rejections = verifications.count { !it.isConfirmed }
        
        return MessageTrustEvaluation(
            messageId = messageId,
            originalSenderId = originalSenderId,
            senderTrustLevel = senderTrust.trustLevel,
            hopCount = hopCount,
            confirmations = confirmations,
            rejections = rejections,
            overallTrustScore = trustScore
        )
    }
    
    /**
     * Calculates the adjustment to the base score from message verifications.
     * 
     * Confirmations from trusted contacts increase the score.
     * Rejections from trusted contacts decrease the score.
     * Each verification is weighted by the verifier's trust level.
     */
    private fun calculateVerificationAdjustment(verifications: List<MessageVerification>): Double {
        if (verifications.isEmpty()) {
            return 0.0
        }
        
        var totalAdjustment = 0.0
        var totalWeight = 0.0
        
        verifications.forEach { verification ->
            val verifierTrust = trustManager.getTrustLevel(verification.verifierId)
            val verifierWeight = verifierTrust.getTrustFactor()
            
            if (verifierWeight > 0.0) {
                // Confirmations add positive adjustment, rejections add negative
                val adjustment = if (verification.isConfirmed) verifierWeight else -verifierWeight
                totalAdjustment += adjustment
                totalWeight += verifierWeight
            }
        }
        
        // Normalize and limit to max verification weight
        if (totalWeight > 0.0) {
            val normalizedAdjustment = (totalAdjustment / totalWeight) * VERIFICATION_WEIGHT
            return normalizedAdjustment.coerceIn(-VERIFICATION_WEIGHT, VERIFICATION_WEIGHT)
        }
        
        return 0.0
    }
    
    /**
     * Compares two messages and returns which one is more trustworthy.
     * 
     * @return Positive if message1 is more trustworthy, negative if message2 is more trustworthy, 0 if equal
     */
    fun compareTrust(
        message1: MessageTrustEvaluation,
        message2: MessageTrustEvaluation
    ): Int {
        return message1.overallTrustScore.compareTo(message2.overallTrustScore)
    }
    
    /**
     * Filters messages by minimum trust score.
     * 
     * @param evaluations List of message evaluations
     * @param minTrustScore Minimum trust score (0.0 - 1.0)
     * @return Filtered list of messages with trust score >= minTrustScore
     */
    fun filterByMinTrust(
        evaluations: List<MessageTrustEvaluation>,
        minTrustScore: Double
    ): List<MessageTrustEvaluation> {
        return evaluations.filter { it.overallTrustScore >= minTrustScore }
    }
    
    /**
     * Sorts messages by trust score in descending order (most trustworthy first).
     */
    fun sortByTrust(evaluations: List<MessageTrustEvaluation>): List<MessageTrustEvaluation> {
        return evaluations.sortedByDescending { it.overallTrustScore }
    }
}
