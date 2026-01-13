package com.fourpeople.adhoc.trust

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages message verifications (confirmations and rejections) in the emergency network.
 * 
 * This class handles:
 * - Storing and retrieving message verifications
 * - Preventing duplicate verifications from the same user
 * - Persistence using SharedPreferences
 * 
 * Users can confirm (mark as true/trustworthy) or reject (mark as false/untrustworthy) messages.
 * Verifications from contacts with higher trust levels carry more weight in trust calculations.
 */
class MessageVerificationManager(context: Context) {
    
    companion object {
        private const val TAG = "MessageVerificationMgr"
        private const val PREF_NAME = "message_verification_prefs"
        private const val KEY_VERIFICATIONS = "message_verifications"
        // Limit storage to prevent unbounded growth. When exceeded, oldest verifications are removed
        // during cleanup to keep storage at ~80% of limit (800 verifications)
        private const val MAX_STORED_VERIFICATIONS = 1000
    }
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // In-memory cache: messageId -> list of verifications
    private val verifications = ConcurrentHashMap<String, MutableList<MessageVerification>>()
    
    // Track which users verified which messages to prevent duplicates
    private val userVerifications = ConcurrentHashMap<String, MutableSet<String>>()
    
    init {
        loadVerifications()
        Log.d(TAG, "MessageVerificationManager initialized with ${getTotalVerificationCount()} verifications")
    }
    
    /**
     * Adds a verification for a message.
     * 
     * @param messageId The unique identifier of the message
     * @param verifierId The ID of the user making the verification
     * @param isConfirmed True if confirming the message, false if rejecting
     * @param comment Optional comment about the verification
     * @return True if verification was added, false if user already verified this message
     */
    fun addVerification(
        messageId: String,
        verifierId: String,
        isConfirmed: Boolean,
        comment: String = ""
    ): Boolean {
        // Check if user already verified this message
        if (hasUserVerified(messageId, verifierId)) {
            Log.w(TAG, "User $verifierId already verified message $messageId")
            return false
        }
        
        val verification = MessageVerification(
            messageId = messageId,
            verifierId = verifierId,
            isConfirmed = isConfirmed,
            comment = comment
        )
        
        // Add to verifications
        val messageVerifications = verifications.getOrPut(messageId) { mutableListOf() }
        messageVerifications.add(verification)
        
        // Track user verification
        val userMsgSet = userVerifications.getOrPut(verifierId) { mutableSetOf() }
        userMsgSet.add(messageId)
        
        saveVerifications()
        
        val action = if (isConfirmed) "confirmed" else "rejected"
        Log.d(TAG, "Message $messageId $action by $verifierId")
        
        return true
    }
    
    /**
     * Gets all verifications for a specific message.
     */
    fun getVerifications(messageId: String): List<MessageVerification> {
        return verifications[messageId]?.toList() ?: emptyList()
    }
    
    /**
     * Gets all verifications by a specific user.
     */
    fun getVerificationsByUser(verifierId: String): List<MessageVerification> {
        val messageIds = userVerifications[verifierId] ?: return emptyList()
        
        val result = mutableListOf<MessageVerification>()
        messageIds.forEach { messageId ->
            verifications[messageId]?.forEach { verification ->
                if (verification.verifierId == verifierId) {
                    result.add(verification)
                }
            }
        }
        
        return result
    }
    
    /**
     * Checks if a user has already verified a specific message.
     */
    fun hasUserVerified(messageId: String, verifierId: String): Boolean {
        val userMsgSet = userVerifications[verifierId] ?: return false
        return userMsgSet.contains(messageId)
    }
    
    /**
     * Gets verification statistics for a message.
     */
    fun getVerificationStats(messageId: String): VerificationStats {
        val messageVerifications = verifications[messageId] ?: emptyList()
        
        val confirmations = messageVerifications.count { it.isConfirmed }
        val rejections = messageVerifications.count { !it.isConfirmed }
        
        return VerificationStats(
            messageId = messageId,
            totalVerifications = messageVerifications.size,
            confirmations = confirmations,
            rejections = rejections
        )
    }
    
    /**
     * Removes all verifications for a specific message.
     * Useful when cleaning up old messages.
     */
    fun removeMessageVerifications(messageId: String): Boolean {
        val removed = verifications.remove(messageId)
        
        if (removed != null && removed.isNotEmpty()) {
            // Clean up user verification tracking
            removed.forEach { verification ->
                userVerifications[verification.verifierId]?.remove(messageId)
            }
            
            saveVerifications()
            Log.d(TAG, "Removed ${removed.size} verifications for message $messageId")
            return true
        }
        
        return false
    }
    
    /**
     * Clears all verifications.
     */
    fun clearAllVerifications() {
        verifications.clear()
        userVerifications.clear()
        saveVerifications()
        Log.d(TAG, "All verifications cleared")
    }
    
    /**
     * Gets the total number of verifications across all messages.
     */
    fun getTotalVerificationCount(): Int {
        return verifications.values.sumOf { it.size }
    }
    
    /**
     * Cleans up old verifications to prevent unbounded growth.
     * Removes verifications for the oldest messages if total count exceeds limit.
     */
    fun cleanupOldVerifications() {
        val totalCount = getTotalVerificationCount()
        
        if (totalCount > MAX_STORED_VERIFICATIONS) {
            // Sort messages by oldest verification timestamp
            val messagesByAge = verifications.entries
                .sortedBy { entry -> entry.value.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE }
            
            var removedCount = 0
            for ((messageId, _) in messagesByAge) {
                if (getTotalVerificationCount() <= MAX_STORED_VERIFICATIONS * 0.8) {
                    break // Keep 80% after cleanup
                }
                
                removeMessageVerifications(messageId)
                removedCount++
            }
            
            if (removedCount > 0) {
                Log.d(TAG, "Cleaned up verifications for $removedCount old messages")
            }
        }
    }
    
    // Private helper methods
    
    private fun loadVerifications() {
        try {
            val json = preferences.getString(KEY_VERIFICATIONS, null)
            if (json != null) {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val verification = MessageVerification(
                        messageId = obj.getString("messageId"),
                        verifierId = obj.getString("verifierId"),
                        isConfirmed = obj.getBoolean("isConfirmed"),
                        timestamp = obj.getLong("timestamp"),
                        comment = obj.optString("comment", "")
                    )
                    
                    // Add to verifications
                    val messageVerifications = verifications.getOrPut(verification.messageId) { mutableListOf() }
                    messageVerifications.add(verification)
                    
                    // Track user verification
                    val userMsgSet = userVerifications.getOrPut(verification.verifierId) { mutableSetOf() }
                    userMsgSet.add(verification.messageId)
                }
                
                Log.d(TAG, "Loaded ${getTotalVerificationCount()} verifications from storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading verifications", e)
        }
    }
    
    private fun saveVerifications() {
        try {
            val jsonArray = JSONArray()
            verifications.values.forEach { messageVerifications ->
                messageVerifications.forEach { verification ->
                    val obj = JSONObject()
                    obj.put("messageId", verification.messageId)
                    obj.put("verifierId", verification.verifierId)
                    obj.put("isConfirmed", verification.isConfirmed)
                    obj.put("timestamp", verification.timestamp)
                    obj.put("comment", verification.comment)
                    jsonArray.put(obj)
                }
            }
            
            preferences.edit()
                .putString(KEY_VERIFICATIONS, jsonArray.toString())
                .apply()
            
            Log.d(TAG, "Saved ${getTotalVerificationCount()} verifications to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving verifications", e)
        }
    }
    
    /**
     * Statistics about verifications for a message.
     */
    data class VerificationStats(
        val messageId: String,
        val totalVerifications: Int,
        val confirmations: Int,
        val rejections: Int
    ) {
        /**
         * Returns the net verification score (confirmations - rejections).
         */
        fun getNetScore(): Int = confirmations - rejections
        
        /**
         * Returns true if confirmations outnumber rejections.
         */
        fun hasPositiveConsensus(): Boolean = confirmations > rejections
    }
}
