package com.fourpeople.adhoc.trust

import android.content.Context
import com.fourpeople.adhoc.mesh.MeshMessage

/**
 * Example integration showing how to use the trust system with mesh messages.
 * 
 * This example demonstrates:
 * 1. Setting up trust levels for contacts
 * 2. Evaluating trust for received messages
 * 3. Adding verifications from users
 * 4. Filtering messages by trust level
 */
class TrustSystemExample(context: Context) {
    
    private val trustManager = TrustManager(context)
    private val calculator = MessageTrustCalculator(trustManager)
    private val verificationManager = MessageVerificationManager(context)
    
    /**
     * Example: Initial setup of trust levels for known contacts.
     */
    fun setupInitialTrustLevels() {
        // Import contacts from phone book (automatically level 1)
        val phoneContacts = listOf("alice", "bob", "charlie", "dave")
        trustManager.importKnownContacts(phoneContacts)
        
        // Manually set higher trust levels for close contacts
        trustManager.setTrustLevel("mom", ContactTrustLevel.CLOSE_FAMILY, isManuallySet = true)
        trustManager.setTrustLevel("dad", ContactTrustLevel.CLOSE_FAMILY, isManuallySet = true)
        trustManager.setTrustLevel("best_friend", ContactTrustLevel.FRIEND, isManuallySet = true)
        trustManager.setTrustLevel("colleague", ContactTrustLevel.FRIEND, isManuallySet = true)
    }
    
    /**
     * Example: Evaluate trust for a received mesh message.
     */
    fun evaluateReceivedMessage(message: MeshMessage): MessageTrustEvaluation {
        // Get any existing verifications for this message
        val verifications = verificationManager.getVerifications(message.messageId)
        
        // Calculate trust score (now includes connection security)
        val evaluation = calculator.evaluateMessage(
            messageId = message.messageId,
            originalSenderId = message.sourceId,
            hopCount = message.hopCount,
            verifications = verifications,
            hasInsecureHop = message.hasInsecureHop
        )
        
        return evaluation
    }
    
    /**
     * Example: Handle a user confirming a message as true/trustworthy.
     */
    fun confirmMessage(messageId: String, currentUserId: String, comment: String = "") {
        verificationManager.addVerification(
            messageId = messageId,
            verifierId = currentUserId,
            isConfirmed = true,
            comment = comment
        )
    }
    
    /**
     * Example: Handle a user rejecting a message as false/untrustworthy.
     */
    fun rejectMessage(messageId: String, currentUserId: String, comment: String = "") {
        verificationManager.addVerification(
            messageId = messageId,
            verifierId = currentUserId,
            isConfirmed = false,
            comment = comment
        )
    }
    
    /**
     * Example: Filter messages to show only high-trust ones.
     */
    fun getHighTrustMessages(messages: List<MeshMessage>): List<Pair<MeshMessage, MessageTrustEvaluation>> {
        val evaluatedMessages = messages.map { message ->
            val evaluation = evaluateReceivedMessage(message)
            message to evaluation
        }
        
        // Filter for high trust (score >= 0.6)
        return evaluatedMessages.filter { (_, eval) -> eval.isHighTrust() }
    }
    
    /**
     * Example: Sort messages by trust score (most trustworthy first).
     */
    fun sortMessagesByTrust(messages: List<MeshMessage>): List<Pair<MeshMessage, MessageTrustEvaluation>> {
        val evaluatedMessages = messages.map { message ->
            val evaluation = evaluateReceivedMessage(message)
            message to evaluation
        }
        
        return evaluatedMessages.sortedByDescending { (_, eval) -> eval.overallTrustScore }
    }
    
    /**
     * Example: Get a human-readable trust summary for a message.
     */
    fun getTrustSummary(message: MeshMessage): String {
        val evaluation = evaluateReceivedMessage(message)
        
        val senderTrustDesc = ContactTrustLevel.getDescription(evaluation.senderTrustLevel)
        val trustRating = MessageTrustEvaluation.getTrustRating(evaluation.overallTrustScore)
        val emoji = MessageTrustEvaluation.getTrustEmoji(evaluation.overallTrustScore)
        
        return buildString {
            append("$emoji Trust: $trustRating\n")
            append("Sender: $senderTrustDesc\n")
            append("Hops: ${evaluation.hopCount}\n")
            append("Score: ${"%.2f".format(evaluation.overallTrustScore)}\n")
            
            if (evaluation.confirmations > 0 || evaluation.rejections > 0) {
                append("Verifications: ")
                append("✓${evaluation.confirmations} ")
                append("✗${evaluation.rejections}")
            }
        }
    }
    
    /**
     * Example: Display trust statistics.
     */
    fun getTrustStatisticsSummary(): String {
        val stats = trustManager.getTrustStatistics()
        
        return buildString {
            appendLine("Trust Statistics:")
            appendLine("Total Contacts: ${stats.totalContacts}")
            appendLine("Unknown: ${stats.unknownCount}")
            appendLine("Known: ${stats.knownContactCount}")
            appendLine("Friends: ${stats.friendCount}")
            appendLine("Close/Family: ${stats.closeFamilyCount}")
        }
    }
}
