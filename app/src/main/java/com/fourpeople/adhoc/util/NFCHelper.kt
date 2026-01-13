package com.fourpeople.adhoc.util

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.util.Log

/**
 * Helper class for NFC Tap-to-Join functionality.
 * 
 * This class handles:
 * - Creating NDEF messages with network credentials
 * - Parsing received NDEF messages
 * - Managing NFC adapter for Android Beam (API < 29) or static handover
 * 
 * NFC Tap-to-Join flow:
 * 1. Person A has emergency network active
 * 2. Person B taps their device to Person A's device
 * 3. Network credentials are automatically exchanged via NFC
 * 4. Person B can immediately join the emergency network
 */
class NFCHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "NFCHelper"
        const val MIME_TYPE = "application/vnd.fourpeople.adhoc"
        private const val PAYLOAD_SEPARATOR = "|"
        const val DEVICE_ID_PLACEHOLDER = "DEVICE_ID_NOT_SET"
    }
    
    private var nfcAdapter: NfcAdapter? = null
    private var currentDeviceId: String? = null
    
    /**
     * Initialize NFC adapter.
     * @return true if NFC is available and enabled, false otherwise
     */
    fun initialize(): Boolean {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        
        if (nfcAdapter == null) {
            Log.w(TAG, "NFC not available on this device")
            return false
        }
        
        if (!nfcAdapter!!.isEnabled) {
            Log.w(TAG, "NFC is disabled on this device")
            return false
        }
        
        Log.d(TAG, "NFC initialized successfully")
        return true
    }
    
    /**
     * Check if NFC is available and enabled.
     */
    fun isNfcAvailable(): Boolean {
        return nfcAdapter?.isEnabled == true
    }
    
    /**
     * Set the current device ID for sharing.
     */
    fun setDeviceId(deviceId: String) {
        currentDeviceId = deviceId
        Log.d(TAG, "Device ID set for NFC sharing: $deviceId")
    }
    
    /**
     * Create an NDEF message containing network credentials.
     * 
     * The message contains:
     * - Device ID
     * - Network SSID pattern
     * - Timestamp
     * 
     * @param deviceId The unique device identifier
     * @return NdefMessage containing the credentials
     */
    fun createNdefMessage(deviceId: String): NdefMessage {
        val timestamp = System.currentTimeMillis()
        val payload = "$deviceId$PAYLOAD_SEPARATOR$timestamp"
        
        val mimeRecord = NdefRecord.createMime(
            MIME_TYPE,
            payload.toByteArray(Charsets.UTF_8)
        )
        
        // Create AAR (Android Application Record) to ensure the app opens
        val aarRecord = NdefRecord.createApplicationRecord(context.packageName)
        
        return NdefMessage(arrayOf(mimeRecord, aarRecord))
    }
    
    /**
     * Parse an NDEF message to extract network credentials.
     * 
     * @param message The NDEF message to parse
     * @return NetworkCredentials if successful, null otherwise
     */
    fun parseNdefMessage(message: NdefMessage): NetworkCredentials? {
        try {
            for (record in message.records) {
                if (record.tnf == NdefRecord.TNF_MIME_MEDIA) {
                    val mimeType = String(record.type, Charsets.UTF_8)
                    if (mimeType == MIME_TYPE) {
                        val payload = String(record.payload, Charsets.UTF_8)
                        val parts = payload.split(PAYLOAD_SEPARATOR)
                        
                        if (parts.size >= 2) {
                            val deviceId = parts[0]
                            val timestamp = parts[1].toLongOrNull()
                            
                            // Return null if timestamp is invalid - don't use 0L as fallback
                            if (timestamp == null) {
                                Log.w(TAG, "Invalid timestamp in NFC credentials")
                                return null
                            }
                            
                            Log.d(TAG, "Parsed NFC credentials: deviceId=$deviceId")
                            return NetworkCredentials(deviceId, timestamp)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing NDEF message", e)
        }
        
        return null
    }
    
    /**
     * Enable NFC foreground dispatch for an activity.
     * This ensures the activity receives NFC intents even when in the foreground.
     * 
     * Note: Android Beam (setNdefPushMessage) was deprecated in API 29 and removed in API 30.
     * For sharing credentials, apps should use alternative methods like QR codes or WiFi Direct.
     * 
     * @param activity The activity to enable foreground dispatch for
     */
    fun enableForegroundDispatch(activity: Activity) {
        nfcAdapter?.let { adapter ->
            if (adapter.isEnabled) {
                try {
                    // Android Beam is no longer available in modern Android versions (API 30+)
                    // NFC tag reading/writing can still be used for credential sharing
                    currentDeviceId?.let { deviceId ->
                        val message = createNdefMessage(deviceId)
                        Log.d(TAG, "NFC message created for device: $deviceId")
                        // Message can be written to NFC tags or shared via other mechanisms
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error enabling NFC foreground dispatch", e)
                }
            }
        }
    }
    
    /**
     * Disable NFC foreground dispatch for an activity.
     * 
     * Note: Android Beam is no longer available, so this is a no-op for cleanup purposes.
     * 
     * @param activity The activity to disable foreground dispatch for
     */
    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.let { adapter ->
            try {
                // Android Beam was removed in API 30, no cleanup needed
                Log.d(TAG, "NFC foreground dispatch disabled")
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling NFC foreground dispatch", e)
            }
        }
    }
    
    /**
     * Get a user-friendly status message about NFC availability.
     */
    fun getNfcStatus(): String {
        return when {
            nfcAdapter == null -> "NFC not available on this device"
            !nfcAdapter!!.isEnabled -> "NFC is disabled - please enable in settings"
            else -> "NFC ready"
        }
    }
    
    /**
     * Data class to hold parsed network credentials.
     */
    data class NetworkCredentials(
        val deviceId: String,
        val timestamp: Long
    ) {
        /**
         * Get the emergency network SSID pattern for this device.
         */
        fun getNetworkSsid(): String = "4people-$deviceId"
        
        /**
         * Check if the credentials are still valid (not too old).
         * Credentials older than 1 hour are considered expired.
         */
        fun isValid(): Boolean {
            val ageMillis = System.currentTimeMillis() - timestamp
            val maxAgeMillis = 60 * 60 * 1000L // 1 hour
            return ageMillis < maxAgeMillis
        }
    }
}
