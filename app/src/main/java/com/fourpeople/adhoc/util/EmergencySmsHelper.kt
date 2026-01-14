package com.fourpeople.adhoc.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * Helper class for sending emergency SMS broadcasts to predefined contacts.
 * 
 * IMPORTANT: SMS requires cellular voice network, NOT WiFi or mobile data.
 * - ✅ SMS works when mobile data is down (only needs cellular voice)
 * - ✅ SMS works when internet backbone is down (only needs cellular voice)
 * - ❌ SMS does NOT work over WiFi (requires cellular network)
 * - ❌ SMS does NOT work when cellular network completely fails
 * 
 * SMS can be sent when emergency mode is activated to notify contacts who may not
 * have the app installed. SMS often works when data networks are unavailable,
 * but requires cellular voice network to be operational.
 */
object EmergencySmsHelper {
    
    private const val TAG = "EmergencySmsHelper"
    const val PREF_NAME = "4people_prefs"
    const val PREF_EMERGENCY_CONTACTS = "emergency_contacts"
    const val PREF_SMS_ENABLED = "sms_enabled"
    
    /**
     * Send emergency SMS to all configured contacts.
     * 
     * @param context Application context
     * @param message The emergency message to send
     * @return Number of SMS messages successfully sent
     */
    fun sendEmergencySms(context: Context, message: String): Int {
        // Check if SMS is enabled in settings
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val smsEnabled = prefs.getBoolean(PREF_SMS_ENABLED, false)
        
        if (!smsEnabled) {
            Log.d(TAG, "SMS broadcast is disabled in settings")
            return 0
        }
        
        // Check permission
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "SMS permission not granted")
            return 0
        }
        
        // Get emergency contacts
        val contacts = getEmergencyContacts(context)
        if (contacts.isEmpty()) {
            Log.d(TAG, "No emergency contacts configured")
            return 0
        }
        
        // Send SMS to each contact
        var sentCount = 0
        val smsManager = SmsManager.getDefault()
        
        contacts.forEach { phoneNumber ->
            try {
                // Note: sentIntent and deliveryIntent are null for simplicity
                // In a production system, these could be used to track delivery status
                // However, in an emergency situation, we prioritize sending quickly
                // over tracking delivery. SMS delivery is not guaranteed anyway.
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,  // sentIntent - could add delivery tracking here
                    null   // deliveryIntent - could add delivery confirmation here
                )
                Log.d(TAG, "Emergency SMS sent to: $phoneNumber")
                sentCount++
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS to $phoneNumber", e)
            }
        }
        
        return sentCount
    }
    
    /**
     * Get the list of emergency contacts.
     */
    fun getEmergencyContacts(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val contactsString = prefs.getString(PREF_EMERGENCY_CONTACTS, "") ?: ""
        
        return if (contactsString.isNotEmpty()) {
            contactsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
    }
    
    /**
     * Save emergency contacts.
     */
    fun saveEmergencyContacts(context: Context, contacts: List<String>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_EMERGENCY_CONTACTS, contacts.joinToString(","))
            .apply()
    }
    
    /**
     * Check if SMS broadcast is enabled.
     */
    fun isSmsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SMS_ENABLED, false)
    }
    
    /**
     * Enable or disable SMS broadcast.
     */
    fun setSmsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(PREF_SMS_ENABLED, enabled)
            .apply()
    }
}
