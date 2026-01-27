package com.fourpeople.adhoc.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.fourpeople.adhoc.util.ErrorLogger

/**
 * Helper class for managing WiFi connections to emergency networks.
 * 
 * This helper attempts to connect to emergency WiFi networks (4people-*) that are discovered
 * during scanning, allowing the device to act as both a WiFi client and potentially maintain
 * a hotspot (on supported devices with dual-band WiFi).
 * 
 * Note: Simultaneous hotspot and client mode requires:
 * - Android 8.0+ (API 26+)
 * - Dual-band WiFi capability (5GHz + 2.4GHz)
 * - Some devices may not support this configuration
 */
class WiFiConnectionHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "WiFiConnectionHelper"
        const val EMERGENCY_SSID_PATTERN = "4people-"
    }
    
    /**
     * Listener interface for WiFi connection status updates.
     */
    interface ConnectionStatusListener {
        fun onConnectionStatusChanged(isConnected: Boolean, ssid: String?)
    }
    
    private val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var connectedNetworkId: Int = -1
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var statusListener: ConnectionStatusListener? = null
    
    /**
     * Set the connection status listener to receive updates.
     */
    fun setConnectionStatusListener(listener: ConnectionStatusListener?) {
        statusListener = listener
    }
    
    /**
     * Attempts to connect to an emergency WiFi network.
     * 
     * @param ssid The SSID of the network to connect to (must start with EMERGENCY_SSID_PATTERN)
     * @param password Optional password for the network (null for open networks)
     * @return true if connection attempt was initiated, false otherwise
     */
    fun connectToEmergencyNetwork(ssid: String, password: String? = null): Boolean {
        if (!ssid.startsWith(EMERGENCY_SSID_PATTERN)) {
            Log.w(TAG, "SSID does not match emergency pattern: $ssid")
            return false
        }
        
        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing location permission for WiFi connection")
            return false
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses WifiNetworkSpecifier with ConnectivityManager
            return connectUsingNetworkSpecifier(ssid, password)
        } else {
            // Android 8-9 uses legacy WifiConfiguration API
            return connectUsingLegacyApi(ssid, password)
        }
    }
    
    /**
     * Connects to a WiFi network using WifiNetworkSpecifier (Android 10+).
     */
    private fun connectUsingNetworkSpecifier(ssid: String, password: String?): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }
        
        try {
            val specifierBuilder = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
            
            if (password != null) {
                specifierBuilder.setWpa2Passphrase(password)
            }
            
            val specifier = specifierBuilder.build()
            
            val networkRequest = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Don't require internet
                .setNetworkSpecifier(specifier)
                .build()
            
            // Remove any existing callback
            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
            }
            
            // Create new callback
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "Connected to emergency network: $ssid")
                    statusListener?.onConnectionStatusChanged(true, ssid)
                    // Optionally bind to this network for specific operations
                    // connectivityManager.bindProcessToNetwork(network)
                }
                
                override fun onUnavailable() {
                    Log.w(TAG, "Failed to connect to emergency network: $ssid")
                    statusListener?.onConnectionStatusChanged(false, null)
                }
                
                override fun onLost(network: Network) {
                    Log.d(TAG, "Lost connection to emergency network: $ssid")
                    statusListener?.onConnectionStatusChanged(false, null)
                }
            }
            
            connectivityManager.requestNetwork(networkRequest, networkCallback!!)
            Log.d(TAG, "Connection request initiated for: $ssid")
            return true
            
        } catch (e: Exception) {
            ErrorLogger.logError(TAG, "Error connecting to network using NetworkSpecifier", e)
            return false
        }
    }
    
    /**
     * Connects to a WiFi network using legacy WifiConfiguration API (Android 8-9).
     * 
     * Note: This API is deprecated in Android 10+ and may not work on all devices.
     */
    @Suppress("DEPRECATION")
    private fun connectUsingLegacyApi(ssid: String, password: String?): Boolean {
        try {
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = "\"$ssid\""
            
            if (password != null) {
                wifiConfig.preSharedKey = "\"$password\""
            } else {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }
            
            // Add network
            val networkId = wifiManager.addNetwork(wifiConfig)
            if (networkId == -1) {
                Log.e(TAG, "Failed to add network configuration")
                return false
            }
            
            // Disconnect from current network
            wifiManager.disconnect()
            
            // Enable this network
            val success = wifiManager.enableNetwork(networkId, true)
            if (!success) {
                Log.e(TAG, "Failed to enable network")
                wifiManager.removeNetwork(networkId)
                return false
            }
            
            // Reconnect
            wifiManager.reconnect()
            
            connectedNetworkId = networkId
            Log.d(TAG, "Connection initiated using legacy API for: $ssid")
            return true
            
        } catch (e: SecurityException) {
            ErrorLogger.logError(TAG, "Security exception when connecting to network (requires CHANGE_WIFI_STATE permission)", e)
            return false
        } catch (e: Exception) {
            ErrorLogger.logError(TAG, "Error connecting to network using legacy API", e)
            return false
        }
    }
    
    /**
     * Attempts to automatically connect to the first available emergency network from scan results.
     * 
     * @param emergencyNetworks List of emergency network SSIDs discovered during scanning
     * @return true if connection attempt was initiated, false otherwise
     */
    fun connectToAvailableEmergencyNetwork(emergencyNetworks: List<String>): Boolean {
        if (emergencyNetworks.isEmpty()) {
            Log.d(TAG, "No emergency networks available to connect to")
            return false
        }
        
        // Try to connect to the first available emergency network
        // In the future, this could be enhanced with:
        // - Signal strength comparison to choose the best network
        // - Network preference based on trust/verification
        // - Avoiding already-attempted networks
        val ssid = emergencyNetworks.first()
        Log.d(TAG, "Attempting to connect to emergency network: $ssid")
        
        // Emergency networks are assumed to be open (no password) for accessibility
        // If password-protected networks are needed, this could be extended to:
        // - Store network credentials in shared preferences
        // - Use a predefined password scheme based on device ID
        // - Prompt user for password
        return connectToEmergencyNetwork(ssid, null)
    }
    
    /**
     * Checks if currently connected to an emergency WiFi network.
     */
    fun isConnectedToEmergencyNetwork(): Boolean {
        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing location permission for WiFi connection check")
            return false
        }
        
        try {
            val wifiInfo = wifiManager.connectionInfo
            val currentSsid = wifiInfo?.ssid?.replace("\"", "") ?: ""
            return currentSsid.startsWith(EMERGENCY_SSID_PATTERN)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi connection status", e)
            return false
        }
    }
    
    /**
     * Gets the SSID of the currently connected WiFi network.
     */
    fun getCurrentNetworkSsid(): String? {
        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Missing location permission for getting network SSID")
            return null
        }
        
        try {
            val wifiInfo = wifiManager.connectionInfo
            return wifiInfo?.ssid?.replace("\"", "")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current network SSID", e)
            return null
        }
    }
    
    /**
     * Disconnects from the currently connected emergency network.
     */
    @Suppress("DEPRECATION")
    fun disconnect() {
        try {
            // Unregister network callback for Android 10+
            networkCallback?.let {
                connectivityManager.unregisterNetworkCallback(it)
                networkCallback = null
            }
            
            // Remove network configuration for Android 8-9
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && connectedNetworkId != -1) {
                wifiManager.removeNetwork(connectedNetworkId)
                connectedNetworkId = -1
            }
            
            Log.d(TAG, "Disconnected from emergency network")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from network", e)
        }
    }
    
    /**
     * Cleans up resources.
     */
    fun cleanup() {
        disconnect()
    }
}
