package com.fourpeople.adhoc.mesh

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles Bluetooth communication for mesh networking.
 * 
 * Provides reliable message transport over Bluetooth connections
 * between neighboring devices in the mesh network.
 */
class BluetoothMeshTransport(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val deviceId: String
) {
    
    companion object {
        private const val TAG = "BluetoothMeshTransport"
        private const val NAME = "4people-mesh"
        private val SERVICE_UUID: UUID = UUID.fromString("4e657477-6f72-6b4d-6573-680000000001")
    }
    
    private var acceptThread: AcceptThread? = null
    private val connections = ConcurrentHashMap<String, ConnectedThread>()
    private var messageListener: MessageListener? = null
    
    /**
     * Sets the listener for received messages.
     */
    fun setMessageListener(listener: MessageListener) {
        messageListener = listener
    }
    
    /**
     * Starts listening for incoming Bluetooth connections.
     */
    fun startListening() {
        stopListening()
        acceptThread = AcceptThread()
        acceptThread?.start()
        Log.d(TAG, "Started listening for Bluetooth mesh connections")
    }
    
    /**
     * Stops listening for incoming connections.
     */
    fun stopListening() {
        acceptThread?.cancel()
        acceptThread = null
    }
    
    /**
     * Sends a message to a specific device.
     */
    fun sendMessage(message: MeshMessage, deviceAddress: String): Boolean {
        // Thread-safe connection retrieval or creation
        val connection = synchronized(connections) {
            var conn = connections[deviceAddress]
            
            // Create connection if it doesn't exist or is not connected
            if (conn == null || !conn.isConnected) {
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                conn = ConnectedThread(device)
                conn.start()
                connections[deviceAddress] = conn
            }
            
            conn
        }
        
        return connection.write(message)
    }
    
    /**
     * Closes all connections.
     */
    fun cleanup() {
        stopListening()
        connections.values.forEach { it.cancel() }
        connections.clear()
        Log.d(TAG, "Bluetooth mesh transport cleaned up")
    }
    
    /**
     * Gets the number of active connections.
     */
    fun getConnectionCount(): Int = connections.count { it.value.isConnected }
    
    /**
     * Thread that listens for incoming Bluetooth connections.
     */
    private inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, SERVICE_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Socket's listen() method failed", e)
                null
            }
        }
        
        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                
                socket?.also {
                    // Connection accepted, start communication thread
                    val device = it.remoteDevice
                    val connection = ConnectedThread(device, it)
                    connection.start()
                    connections[device.address] = connection
                    
                    Log.d(TAG, "Incoming connection from ${device.address}")
                }
            }
        }
        
        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
    
    /**
     * Thread that manages a connection to a specific device.
     */
    private inner class ConnectedThread(
        private val device: BluetoothDevice,
        initialSocket: BluetoothSocket? = null
    ) : Thread() {
        
        private var socket: BluetoothSocket? = initialSocket
        private var inputStream: ObjectInputStream? = null
        private var outputStream: ObjectOutputStream? = null
        var isConnected = false
            private set
        
        override fun run() {
            // Connect if not already connected
            if (socket == null) {
                connect()
            }
            
            if (socket == null || !isConnected) {
                Log.w(TAG, "Failed to establish connection to ${device.address}")
                // Remove failed connection from pool
                connections.remove(device.address)
                return
            }
            
            // Keep listening for messages
            try {
                inputStream = ObjectInputStream(socket!!.inputStream)
                outputStream = ObjectOutputStream(socket!!.outputStream)
                
                while (isConnected) {
                    try {
                        val message = inputStream?.readObject() as? MeshMessage
                        
                        if (message != null) {
                            Log.d(TAG, "Message received from ${device.address}: ${message.messageId}")
                            messageListener?.onMessageReceived(message, device.address)
                        }
                    } catch (e: IOException) {
                        Log.w(TAG, "Connection lost to ${device.address}", e)
                        break
                    } catch (e: ClassNotFoundException) {
                        Log.e(TAG, "Invalid message format", e)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error setting up streams", e)
            } finally {
                cancel()
            }
        }
        
        private fun connect() {
            try {
                socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket?.connect()
                isConnected = true
                Log.d(TAG, "Connected to ${device.address}")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to connect to ${device.address}", e)
                try {
                    socket?.close()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Could not close socket", closeException)
                }
                socket = null
            }
        }
        
        fun write(message: MeshMessage): Boolean {
            return try {
                outputStream?.writeObject(message)
                outputStream?.flush()
                Log.d(TAG, "Message sent to ${device.address}: ${message.messageId}")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Error sending message to ${device.address}", e)
                cancel()
                false
            }
        }
        
        fun cancel() {
            isConnected = false
            try {
                inputStream?.close()
                outputStream?.close()
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close connection", e)
            }
            connections.remove(device.address)
        }
    }
    
    /**
     * Interface for handling received messages.
     */
    interface MessageListener {
        fun onMessageReceived(message: MeshMessage, senderAddress: String)
    }
}
