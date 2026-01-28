package com.fourpeople.adhoc.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import java.util.*

/**
 * Infrastructure Health Monitor
 * 
 * Monitors the health and availability of critical infrastructure components:
 * - Bluetooth adapter
 * - WiFi connectivity
 * - Cellular network
 * - Mesh network (active nodes and routing)
 * 
 * Provides real-time status updates and historical tracking of infrastructure health.
 */
class InfrastructureMonitor(private val context: Context) {

    companion object {
        private const val TAG = "InfrastructureMonitor"
        private const val HISTORY_SIZE = 20 // Keep last 20 status checks
    }

    data class InfrastructureStatus(
        val timestamp: Long,
        val bluetoothHealth: HealthStatus,
        val wifiHealth: HealthStatus,
        val cellularHealth: HealthStatus,
        val meshHealth: HealthStatus,
        val overallHealth: HealthStatus
    ) {
        fun toMap(): Map<String, Any> {
            return mapOf(
                "timestamp" to timestamp,
                "bluetooth" to bluetoothHealth.name,
                "wifi" to wifiHealth.name,
                "cellular" to cellularHealth.name,
                "mesh" to meshHealth.name,
                "overall" to overallHealth.name
            )
        }
    }

    enum class HealthStatus {
        HEALTHY,    // Fully operational
        DEGRADED,   // Partially working or weak signal
        FAILED,     // Not working or unavailable
        UNKNOWN     // Status cannot be determined
    }

    private val bluetoothManager: BluetoothManager? = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val wifiManager: WifiManager? = 
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager: ConnectivityManager? = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val telephonyManager: TelephonyManager? = 
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val statusHistory = LinkedList<InfrastructureStatus>()
    private val historyLock = Any()
    private var lastStatus: InfrastructureStatus? = null
    private var meshActiveNodes = 0
    private var meshRoutingActive = false

    /**
     * Perform a health check of all infrastructure components
     */
    fun checkHealth(): InfrastructureStatus {
        val bluetoothHealth = checkBluetoothHealth()
        val wifiHealth = checkWifiHealth()
        val cellularHealth = checkCellularHealth()
        val meshHealth = checkMeshHealth()
        
        val overallHealth = calculateOverallHealth(
            bluetoothHealth, wifiHealth, cellularHealth, meshHealth
        )

        val status = InfrastructureStatus(
            timestamp = System.currentTimeMillis(),
            bluetoothHealth = bluetoothHealth,
            wifiHealth = wifiHealth,
            cellularHealth = cellularHealth,
            meshHealth = meshHealth,
            overallHealth = overallHealth
        )

        // Add to history with synchronization
        synchronized(historyLock) {
            statusHistory.addLast(status)
            if (statusHistory.size > HISTORY_SIZE) {
                statusHistory.removeFirst()
            }

            // Check for status changes
            if (lastStatus != null) {
                detectStatusChanges(lastStatus!!, status)
            }

            lastStatus = status
        }

        Log.d(TAG, "Infrastructure health check: $status")
        
        return status
    }

    /**
     * Check Bluetooth adapter health
     */
    private fun checkBluetoothHealth(): HealthStatus {
        return try {
            val adapter = bluetoothManager?.adapter
            when {
                adapter == null -> HealthStatus.FAILED
                !adapter.isEnabled -> HealthStatus.FAILED
                else -> HealthStatus.HEALTHY
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth health", e)
            HealthStatus.UNKNOWN
        }
    }

    /**
     * Check WiFi connectivity health
     */
    private fun checkWifiHealth(): HealthStatus {
        return try {
            val wifiEnabled = wifiManager?.isWifiEnabled ?: false
            
            if (!wifiEnabled) {
                return HealthStatus.FAILED
            }

            // Check if WiFi is connected
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            
            val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            
            if (isWifiConnected) {
                // Check signal strength - may require permissions
                try {
                    val wifiInfo = wifiManager?.connectionInfo
                    val rssi = wifiInfo?.rssi ?: -100
                    
                    return when {
                        rssi > -50 -> HealthStatus.HEALTHY
                        rssi > -70 -> HealthStatus.DEGRADED
                        else -> HealthStatus.DEGRADED
                    }
                } catch (e: SecurityException) {
                    Log.w(TAG, "Permission required to get WiFi signal strength", e)
                    // Assume healthy if connected but can't check signal
                    return HealthStatus.HEALTHY
                }
            } else {
                // WiFi enabled but not connected
                HealthStatus.DEGRADED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking WiFi health", e)
            HealthStatus.UNKNOWN
        }
    }

    /**
     * Check cellular network health
     */
    private fun checkCellularHealth(): HealthStatus {
        return try {
            val network = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(network)
            
            val hasCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            
            if (!hasCellular) {
                // Check if device has cellular capability at all
                val simState = telephonyManager?.simState
                return when (simState) {
                    TelephonyManager.SIM_STATE_READY -> HealthStatus.DEGRADED // Has SIM but not connected
                    TelephonyManager.SIM_STATE_ABSENT -> HealthStatus.FAILED // No SIM
                    else -> HealthStatus.FAILED
                }
            }
            
            // Check signal strength if connected - may require permissions
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Modern API for signal strength
                    val signalStrength = telephonyManager?.signalStrength
                    val level = signalStrength?.level ?: 0
                    
                    return when {
                        level >= 3 -> HealthStatus.HEALTHY  // Good signal (3-4 bars)
                        level >= 1 -> HealthStatus.DEGRADED // Weak signal (1-2 bars)
                        else -> HealthStatus.FAILED          // No signal
                    }
                } else {
                    // For older devices, assume healthy if connected
                    HealthStatus.HEALTHY
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Permission required to get cellular signal strength", e)
                // Assume healthy if connected but can't check signal
                return HealthStatus.HEALTHY
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking cellular health", e)
            HealthStatus.UNKNOWN
        }
    }

    /**
     * Check mesh network health based on active nodes and routing
     */
    private fun checkMeshHealth(): HealthStatus {
        return try {
            when {
                meshActiveNodes == 0 && !meshRoutingActive -> HealthStatus.FAILED
                meshActiveNodes < 2 -> HealthStatus.DEGRADED
                meshActiveNodes >= 2 && meshRoutingActive -> HealthStatus.HEALTHY
                else -> HealthStatus.DEGRADED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking mesh health", e)
            HealthStatus.UNKNOWN
        }
    }

    /**
     * Update mesh network statistics
     */
    fun updateMeshStats(activeNodes: Int, routingActive: Boolean) {
        meshActiveNodes = activeNodes
        meshRoutingActive = routingActive
        Log.d(TAG, "Mesh stats updated: nodes=$activeNodes, routing=$routingActive")
    }

    /**
     * Calculate overall infrastructure health based on individual components
     */
    private fun calculateOverallHealth(
        bluetooth: HealthStatus,
        wifi: HealthStatus,
        cellular: HealthStatus,
        mesh: HealthStatus
    ): HealthStatus {
        val statuses = listOf(bluetooth, wifi, cellular, mesh)
        
        // If any critical infrastructure is failed, overall is degraded
        val failedCount = statuses.count { it == HealthStatus.FAILED }
        val degradedCount = statuses.count { it == HealthStatus.DEGRADED }
        val healthyCount = statuses.count { it == HealthStatus.HEALTHY }
        
        return when {
            // All failed or mostly failed
            failedCount >= 3 -> HealthStatus.FAILED
            // Some components working
            healthyCount >= 2 -> HealthStatus.HEALTHY
            // Mixed state
            healthyCount >= 1 || degradedCount >= 2 -> HealthStatus.DEGRADED
            // Unknown or all failed
            else -> HealthStatus.FAILED
        }
    }

    /**
     * Detect significant status changes and log them
     */
    private fun detectStatusChanges(old: InfrastructureStatus, new: InfrastructureStatus) {
        if (old.bluetoothHealth != new.bluetoothHealth) {
            Log.w(TAG, "Bluetooth status changed: ${old.bluetoothHealth} -> ${new.bluetoothHealth}")
        }
        if (old.wifiHealth != new.wifiHealth) {
            Log.w(TAG, "WiFi status changed: ${old.wifiHealth} -> ${new.wifiHealth}")
        }
        if (old.cellularHealth != new.cellularHealth) {
            Log.w(TAG, "Cellular status changed: ${old.cellularHealth} -> ${new.cellularHealth}")
        }
        if (old.meshHealth != new.meshHealth) {
            Log.w(TAG, "Mesh status changed: ${old.meshHealth} -> ${new.meshHealth}")
        }
        if (old.overallHealth != new.overallHealth) {
            Log.w(TAG, "Overall infrastructure status changed: ${old.overallHealth} -> ${new.overallHealth}")
        }
    }

    /**
     * Get current infrastructure status
     */
    fun getCurrentStatus(): InfrastructureStatus? = lastStatus

    /**
     * Get infrastructure status history
     */
    fun getStatusHistory(): List<InfrastructureStatus> {
        synchronized(historyLock) {
            return statusHistory.toList()
        }
    }

    /**
     * Check if there has been a critical infrastructure failure
     * (transition from HEALTHY/DEGRADED to FAILED)
     */
    fun hasCriticalFailure(): Boolean {
        synchronized(historyLock) {
            if (statusHistory.size < 2) return false
            
            val current = statusHistory.last
            val previous = statusHistory[statusHistory.size - 2]
            
            // Check if overall health degraded to FAILED
            return previous.overallHealth != HealthStatus.FAILED && 
                   current.overallHealth == HealthStatus.FAILED
        }
    }

    /**
     * Get a human-readable description of the current infrastructure status
     */
    fun getStatusDescription(): String {
        val status = lastStatus ?: return "Infrastructure status unknown"
        
        return buildString {
            append("Infrastructure Status:\n")
            append("• Bluetooth: ${status.bluetoothHealth.toDescription()}\n")
            append("• WiFi: ${status.wifiHealth.toDescription()}\n")
            append("• Cellular: ${status.cellularHealth.toDescription()}\n")
            append("• Mesh Network: ${status.meshHealth.toDescription()}\n")
            append("Overall: ${status.overallHealth.toDescription()}")
        }
    }

    private fun HealthStatus.toDescription(): String = when (this) {
        HealthStatus.HEALTHY -> "✓ Operational"
        HealthStatus.DEGRADED -> "⚠ Degraded"
        HealthStatus.FAILED -> "✗ Failed"
        HealthStatus.UNKNOWN -> "? Unknown"
    }
}
