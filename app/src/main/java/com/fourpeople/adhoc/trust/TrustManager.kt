package com.fourpeople.adhoc.trust

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages contact trust levels for the emergency network.
 * 
 * This class handles:
 * - Storing and retrieving contact trust levels
 * - CRUD operations for trust levels
 * - Persistence using SharedPreferences
 * 
 * Trust levels can be set manually or automatically (e.g., from contact list import).
 */
class TrustManager(context: Context) {
    
    companion object {
        private const val TAG = "TrustManager"
        private const val PREF_NAME = "trust_manager_prefs"
        private const val KEY_TRUST_LEVELS = "contact_trust_levels"
    }
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    // In-memory cache of trust levels
    private val trustLevels = mutableMapOf<String, ContactTrustLevel>()
    
    init {
        loadTrustLevels()
        Log.d(TAG, "TrustManager initialized with ${trustLevels.size} contacts")
    }
    
    /**
     * Gets the trust level for a contact.
     * Returns UNKNOWN (0) if no trust level is set.
     */
    fun getTrustLevel(contactId: String): ContactTrustLevel {
        return trustLevels[contactId] ?: ContactTrustLevel(
            contactId = contactId,
            trustLevel = ContactTrustLevel.UNKNOWN,
            isManuallySet = false
        )
    }
    
    /**
     * Sets the trust level for a contact.
     */
    fun setTrustLevel(contactId: String, trustLevel: Int, isManuallySet: Boolean = true): Boolean {
        if (!ContactTrustLevel.isValidTrustLevel(trustLevel)) {
            Log.w(TAG, "Invalid trust level: $trustLevel for contact: $contactId")
            return false
        }
        
        val contactTrust = ContactTrustLevel(
            contactId = contactId,
            trustLevel = trustLevel,
            lastUpdated = System.currentTimeMillis(),
            isManuallySet = isManuallySet
        )
        
        trustLevels[contactId] = contactTrust
        saveTrustLevels()
        
        Log.d(TAG, "Trust level set for $contactId: ${ContactTrustLevel.getDescription(trustLevel)} (manual: $isManuallySet)")
        return true
    }
    
    /**
     * Sets trust level using ContactTrustLevel object.
     */
    fun setTrustLevel(contactTrust: ContactTrustLevel): Boolean {
        trustLevels[contactTrust.contactId] = contactTrust
        saveTrustLevels()
        
        Log.d(TAG, "Trust level set for ${contactTrust.contactId}: ${ContactTrustLevel.getDescription(contactTrust.trustLevel)}")
        return true
    }
    
    /**
     * Removes trust level for a contact (resets to UNKNOWN).
     */
    fun removeTrustLevel(contactId: String): Boolean {
        val removed = trustLevels.remove(contactId)
        if (removed != null) {
            saveTrustLevels()
            Log.d(TAG, "Trust level removed for $contactId")
            return true
        }
        return false
    }
    
    /**
     * Gets all contacts with their trust levels.
     */
    fun getAllTrustLevels(): Map<String, ContactTrustLevel> {
        return trustLevels.toMap()
    }
    
    /**
     * Gets contacts with a specific trust level.
     */
    fun getContactsByTrustLevel(trustLevel: Int): List<ContactTrustLevel> {
        return trustLevels.values.filter { it.trustLevel == trustLevel }
    }
    
    /**
     * Imports known contacts with KNOWN_CONTACT (level 1) trust level.
     * This would typically be called when importing from phone contacts.
     * Only adds contacts that don't already have a trust level.
     */
    fun importKnownContacts(contactIds: List<String>): Int {
        var imported = 0
        
        contactIds.forEach { contactId ->
            if (!trustLevels.containsKey(contactId)) {
                trustLevels[contactId] = ContactTrustLevel(
                    contactId = contactId,
                    trustLevel = ContactTrustLevel.KNOWN_CONTACT,
                    isManuallySet = false
                )
                imported++
            }
        }
        
        if (imported > 0) {
            saveTrustLevels()
            Log.d(TAG, "Imported $imported known contacts")
        }
        
        return imported
    }
    
    /**
     * Clears all trust levels.
     */
    fun clearAllTrustLevels() {
        trustLevels.clear()
        saveTrustLevels()
        Log.d(TAG, "All trust levels cleared")
    }
    
    /**
     * Gets statistics about trust levels.
     */
    fun getTrustStatistics(): TrustStatistics {
        val byLevel = mutableMapOf<Int, Int>()
        for (level in ContactTrustLevel.UNKNOWN..ContactTrustLevel.CLOSE_FAMILY) {
            byLevel[level] = 0
        }
        
        trustLevels.values.forEach { contact ->
            // Defensive programming: use safe access and default to 0
            byLevel[contact.trustLevel] = (byLevel[contact.trustLevel] ?: 0) + 1
        }
        
        return TrustStatistics(
            totalContacts = trustLevels.size,
            unknownCount = byLevel[ContactTrustLevel.UNKNOWN] ?: 0,
            knownContactCount = byLevel[ContactTrustLevel.KNOWN_CONTACT] ?: 0,
            friendCount = byLevel[ContactTrustLevel.FRIEND] ?: 0,
            closeFamilyCount = byLevel[ContactTrustLevel.CLOSE_FAMILY] ?: 0
        )
    }
    
    // Private helper methods
    
    private fun loadTrustLevels() {
        try {
            val json = preferences.getString(KEY_TRUST_LEVELS, null)
            if (json != null) {
                val jsonArray = JSONArray(json)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val contactTrust = ContactTrustLevel(
                        contactId = obj.getString("contactId"),
                        trustLevel = obj.getInt("trustLevel"),
                        lastUpdated = obj.getLong("lastUpdated"),
                        isManuallySet = obj.getBoolean("isManuallySet")
                    )
                    trustLevels[contactTrust.contactId] = contactTrust
                }
                Log.d(TAG, "Loaded ${trustLevels.size} trust levels from storage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading trust levels", e)
        }
    }
    
    private fun saveTrustLevels() {
        try {
            val jsonArray = JSONArray()
            trustLevels.values.forEach { contactTrust ->
                val obj = JSONObject()
                obj.put("contactId", contactTrust.contactId)
                obj.put("trustLevel", contactTrust.trustLevel)
                obj.put("lastUpdated", contactTrust.lastUpdated)
                obj.put("isManuallySet", contactTrust.isManuallySet)
                jsonArray.put(obj)
            }
            
            preferences.edit()
                .putString(KEY_TRUST_LEVELS, jsonArray.toString())
                .apply()
            
            Log.d(TAG, "Saved ${trustLevels.size} trust levels to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving trust levels", e)
        }
    }
    
    /**
     * Statistics about contact trust levels.
     */
    data class TrustStatistics(
        val totalContacts: Int,
        val unknownCount: Int,
        val knownContactCount: Int,
        val friendCount: Int,
        val closeFamilyCount: Int
    )
}
