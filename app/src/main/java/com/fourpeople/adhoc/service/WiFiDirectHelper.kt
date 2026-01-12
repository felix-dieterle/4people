package com.fourpeople.adhoc.service

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat

/**
 * Helper class for managing WiFi Direct (WiFi P2P) connections.
 * 
 * WiFi Direct provides:
 * - Higher speed than Bluetooth
 * - Greater range than Bluetooth
 * - Direct device-to-device connections without a router
 * 
 * This is used as an additional communication channel alongside Bluetooth and WiFi scanning.
 */
class WiFiDirectHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "WiFiDirectHelper"
        const val EMERGENCY_DEVICE_NAME_PREFIX = "4people-"
    }
    
    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var isDiscovering = false
    
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    // Check if WiFi P2P is enabled
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    Log.d(TAG, "WiFi P2P state changed: ${if (isEnabled) "enabled" else "disabled"}")
                }
                
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    // Request updated peer list
                    requestPeers()
                }
                
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    // Connection state changed
                    Log.d(TAG, "WiFi P2P connection changed")
                }
                
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    // This device's details have changed
                    val device: WifiP2pDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE, WifiP2pDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    }
                    Log.d(TAG, "This device changed: ${device?.deviceName}")
                }
            }
        }
    }
    
    private val peerListListener = WifiP2pManager.PeerListListener { peers: WifiP2pDeviceList? ->
        peers?.deviceList?.forEach { device ->
            Log.d(TAG, "WiFi P2P peer found: ${device.deviceName}")
            if (device.deviceName.startsWith(EMERGENCY_DEVICE_NAME_PREFIX)) {
                Log.d(TAG, "Emergency WiFi P2P device detected: ${device.deviceName}")
                onEmergencyDeviceFound(device)
            }
        }
    }
    
    /**
     * Initialize WiFi Direct manager and register receivers.
     */
    fun initialize() {
        try {
            wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            channel = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)
            
            if (wifiP2pManager == null || channel == null) {
                Log.w(TAG, "WiFi P2P not available on this device")
                return
            }
            
            // Register broadcast receiver
            val intentFilter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
                addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, intentFilter)
            }
            
            Log.d(TAG, "WiFi Direct initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WiFi Direct", e)
        }
    }
    
    /**
     * Start discovering WiFi Direct peers.
     */
    fun startDiscovery() {
        if (wifiP2pManager == null || channel == null) {
            Log.w(TAG, "Cannot start discovery - WiFi P2P not initialized")
            return
        }
        
        // Check for location permission (required for WiFi P2P discovery)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted for WiFi P2P discovery")
            return
        }
        
        if (isDiscovering) {
            Log.d(TAG, "Discovery already in progress")
            return
        }
        
        wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "WiFi P2P discovery started")
                isDiscovering = true
            }
            
            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "WiFi P2P discovery failed: $reasonCode")
                isDiscovering = false
            }
        })
    }
    
    /**
     * Stop discovering WiFi Direct peers.
     */
    fun stopDiscovery() {
        if (wifiP2pManager == null || channel == null) {
            return
        }
        
        if (!isDiscovering) {
            return
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        wifiP2pManager?.stopPeerDiscovery(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "WiFi P2P discovery stopped")
                isDiscovering = false
            }
            
            override fun onFailure(reasonCode: Int) {
                Log.e(TAG, "Failed to stop WiFi P2P discovery: $reasonCode")
            }
        })
    }
    
    /**
     * Request the current list of peers.
     */
    private fun requestPeers() {
        if (wifiP2pManager == null || channel == null) {
            return
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        wifiP2pManager?.requestPeers(channel, peerListListener)
    }
    
    /**
     * Set the device name to include the emergency prefix.
     * 
     * Note: This uses reflection to call an unofficial API method.
     * If this fails on future Android versions, the device will still
     * be discoverable but with the default device name. This is acceptable
     * as WiFi Direct is a supplementary communication channel.
     * 
     * The reflection approach is necessary because setDeviceName() is not
     * in the public API. If it fails, WiFi Direct discovery will still work,
     * just without the custom emergency name pattern.
     */
    fun setEmergencyDeviceName(deviceId: String) {
        if (wifiP2pManager == null || channel == null) {
            Log.w(TAG, "WiFi P2P not initialized, cannot set device name")
            return
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted, cannot set device name")
            return
        }
        
        val emergencyName = "$EMERGENCY_DEVICE_NAME_PREFIX$deviceId"
        
        try {
            // Attempt to set device name via reflection
            // This may not work on all devices/Android versions - that's OK
            val setDeviceNameMethod = wifiP2pManager?.javaClass?.getMethod(
                "setDeviceName",
                WifiP2pManager.Channel::class.java,
                String::class.java,
                WifiP2pManager.ActionListener::class.java
            )
            
            if (setDeviceNameMethod != null) {
                setDeviceNameMethod.invoke(wifiP2pManager, channel, emergencyName, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d(TAG, "WiFi P2P device name set to: $emergencyName")
                    }
                    
                    override fun onFailure(reasonCode: Int) {
                        Log.w(TAG, "Failed to set WiFi P2P device name: $reasonCode (graceful degradation: using default name)")
                    }
                })
            } else {
                Log.w(TAG, "setDeviceName method not found (graceful degradation: using default name)")
            }
        } catch (e: NoSuchMethodException) {
            Log.w(TAG, "setDeviceName method not available on this Android version (graceful degradation: using default name)")
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception when setting device name (graceful degradation: using default name)")
        } catch (e: Exception) {
            Log.w(TAG, "Cannot set device name via reflection (graceful degradation: using default name): ${e.javaClass.simpleName}")
        }
        // Note: Failure to set device name is non-critical - WiFi Direct discovery
        // will still work, devices just won't have the emergency pattern in their name
    }
    
    /**
     * Clean up resources.
     */
    fun cleanup() {
        try {
            stopDiscovery()
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Called when an emergency device is found.
     */
    private fun onEmergencyDeviceFound(device: WifiP2pDevice) {
        // Send broadcast to notify app components
        val intent = Intent("com.fourpeople.adhoc.EMERGENCY_DETECTED")
        intent.setPackage(context.packageName)
        intent.putExtra("source", device.deviceName)
        intent.putExtra("type", "WiFi Direct")
        context.sendBroadcast(intent)
    }
}
