package com.sensacare.app.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * ConnectionManager - Singleton class to manage ET492 device connections
 * 
 * This class addresses the following issues:
 * 1. Prevents connection jumping/dropping by maintaining a single connection state
 * 2. Handles reconnection gracefully with backoff strategy
 * 3. Prevents app crashes on second connect attempt with proper state management
 * 4. Manages device state with a proper state machine
 * 5. Implements connection timeout and retry logic
 * 6. Saves connection state persistently using SharedPreferences
 * 7. Handles Bluetooth state changes with BroadcastReceiver
 * 8. Implements proper cleanup on disconnect
 */
class ConnectionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ConnectionManager"
        private const val PREFS_NAME = "SensaCareConnectionPrefs"
        private const val KEY_DEVICE_ADDRESS = "device_address"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_LAST_CONNECTED = "last_connected_time"
        private const val MAX_RETRY_COUNT = 3
        private const val CONNECTION_TIMEOUT_MS = 15000L // 15 seconds
        private const val RECONNECT_BASE_DELAY_MS = 1000L // 1 second
        
        // Lock to ensure thread safety during initialization
        private val INSTANCE_LOCK = Any()
        
        @Volatile
        private var instance: ConnectionManager? = null
        
        fun getInstance(context: Context): ConnectionManager {
            return instance ?: synchronized(INSTANCE_LOCK) {
                instance ?: ConnectionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // Connection state enum
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
        CONNECTION_FAILED,
        CONNECTION_LOST
    }
    
    // LiveData for observing connection state changes
    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState
    
    // LiveData for connected device info
    private val _connectedDevice = MutableLiveData<DeviceInfo?>()
    val connectedDevice: LiveData<DeviceInfo?> = _connectedDevice
    
    // LiveData for connection errors
    private val _connectionError = MutableLiveData<String?>()
    val connectionError: LiveData<String?> = _connectionError
    
    // Connection lock to prevent concurrent connection attempts
    private val connectionLock = ReentrantLock()
    
    // Flag to track if a connection attempt is in progress
    private val isConnecting = AtomicBoolean(false)
    
    // Map to track retry counts for devices
    private val retryCountMap = ConcurrentHashMap<String, Int>()
    
    // Handler for timeouts and delayed operations
    private val handler = Handler(Looper.getMainLooper())
    
    // Bluetooth adapter
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    // Shared preferences for persistent storage
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Current connection timeout runnable
    private var connectionTimeoutRunnable: Runnable? = null
    
    // Data class for device info
    data class DeviceInfo(
        val address: String,
        val name: String,
        val rssi: Int = 0,
        val isConnectable: Boolean = true
    )
    
    // Bluetooth state receiver
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "Bluetooth turned off")
                            handleBluetoothTurnedOff()
                        }
                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG, "Bluetooth turned on")
                            // Attempt to reconnect to last device if we were previously connected
                            if (_connectionState.value == ConnectionState.CONNECTION_LOST) {
                                reconnectToLastDevice()
                            }
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val currentDevice = _connectedDevice.value
                    if (currentDevice != null && device?.address == currentDevice.address) {
                        Log.d(TAG, "Device disconnected: ${device.address}")
                        handleDeviceDisconnected(ConnectionState.CONNECTION_LOST)
                    }
                }
            }
        }
    }
    
    init {
        // Register for Bluetooth state changes
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothStateReceiver, filter)
        
        // Check for last connected device and update state
        checkLastConnectedDevice()
        
        Log.d(TAG, "ConnectionManager initialized")
    }
    
    /**
     * Connect to a device with the given address and name
     * Implements proper state management and prevents multiple connection attempts
     */
    fun connectToDevice(address: String, name: String, rssi: Int = 0) {
        // Use a lock to prevent concurrent connection attempts
        connectionLock.withLock {
            // Check if already connecting or connected to this device
            if (isConnecting.get() && _connectedDevice.value?.address == address) {
                Log.d(TAG, "Already connecting to device: $address")
                return
            }
            
            // If connected to a different device, disconnect first
            if (_connectionState.value == ConnectionState.CONNECTED && _connectedDevice.value?.address != address) {
                Log.d(TAG, "Disconnecting from current device before connecting to new one")
                disconnectCurrentDevice()
            }
            
            // Update state to connecting
            isConnecting.set(true)
            _connectionState.postValue(ConnectionState.CONNECTING)
            _connectionError.postValue(null)
            
            Log.d(TAG, "Connecting to device: $address ($name)")
            
            // Create device info
            val deviceInfo = DeviceInfo(address, name, rssi)
            _connectedDevice.postValue(deviceInfo)
            
            // Set connection timeout
            setConnectionTimeout(address)
            
            // Simulate successful connection for now
            // In a real implementation, this would use the VeePoo SDK's connection methods
            handler.postDelayed({
                connectionLock.withLock {
                    if (isConnecting.get() && _connectedDevice.value?.address == address) {
                        // Clear timeout
                        clearConnectionTimeout()
                        
                        // Update state to connected
                        isConnecting.set(false)
                        _connectionState.postValue(ConnectionState.CONNECTED)
                        
                        // Save connection info
                        saveConnection(address, name)
                        
                        // Reset retry count
                        retryCountMap.remove(address)
                        
                        Log.d(TAG, "Successfully connected to device: $address ($name)")
                    }
                }
            }, 2000) // Simulate 2 second connection time
        }
    }
    
    /**
     * Disconnect from the current device
     * Implements proper cleanup and state management
     */
    fun disconnectCurrentDevice() {
        connectionLock.withLock {
            val currentState = _connectionState.value
            val currentDevice = _connectedDevice.value
            
            if (currentState == ConnectionState.DISCONNECTED || currentDevice == null) {
                Log.d(TAG, "No device connected, nothing to disconnect")
                return
            }
            
            // Update state to disconnecting
            _connectionState.postValue(ConnectionState.DISCONNECTING)
            
            // Clear any pending timeouts
            clearConnectionTimeout()
            
            Log.d(TAG, "Disconnecting from device: ${currentDevice.address} (${currentDevice.name})")
            
            // Simulate disconnection for now
            // In a real implementation, this would use the VeePoo SDK's disconnection methods
            handler.postDelayed({
                connectionLock.withLock {
                    // Update state to disconnected
                    isConnecting.set(false)
                    _connectionState.postValue(ConnectionState.DISCONNECTED)
                    
                    Log.d(TAG, "Successfully disconnected from device: ${currentDevice.address}")
                }
            }, 500) // Simulate 500ms disconnection time
        }
    }
    
    /**
     * Save connection info to SharedPreferences
     */
    fun saveConnection(address: String, name: String) {
        sharedPreferences.edit().apply {
            putString(KEY_DEVICE_ADDRESS, address)
            putString(KEY_DEVICE_NAME, name)
            putLong(KEY_LAST_CONNECTED, System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "Saved connection info: $address ($name)")
    }
    
    /**
     * Clear saved connection info
     */
    fun clearSavedConnection() {
        sharedPreferences.edit().apply {
            remove(KEY_DEVICE_ADDRESS)
            remove(KEY_DEVICE_NAME)
            remove(KEY_LAST_CONNECTED)
            apply()
        }
        Log.d(TAG, "Cleared saved connection info")
    }
    
    /**
     * Check if a device is currently connected
     */
    fun isDeviceConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED && _connectedDevice.value != null
    }
    
    /**
     * Get the currently connected device
     */
    fun getConnectedDevice(): DeviceInfo? {
        return if (isDeviceConnected()) _connectedDevice.value else null
    }
    
    /**
     * Check if we have a saved connection and update state accordingly
     */
    private fun checkLastConnectedDevice() {
        val address = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null)
        val name = sharedPreferences.getString(KEY_DEVICE_NAME, null)
        
        if (address != null && name != null) {
            Log.d(TAG, "Found last connected device: $address ($name)")
            _connectedDevice.postValue(DeviceInfo(address, name))
            
            // Don't automatically set to connected state, but indicate we have a saved device
            if (_connectionState.value == ConnectionState.DISCONNECTED) {
                _connectionState.postValue(ConnectionState.DISCONNECTED)
            }
        }
    }
    
    /**
     * Attempt to reconnect to the last connected device
     */
    fun reconnectToLastDevice() {
        val address = sharedPreferences.getString(KEY_DEVICE_ADDRESS, null)
        val name = sharedPreferences.getString(KEY_DEVICE_NAME, null)
        
        if (address != null && name != null) {
            Log.d(TAG, "Attempting to reconnect to last device: $address ($name)")
            connectToDevice(address, name)
        } else {
            Log.d(TAG, "No last device to reconnect to")
        }
    }
    
    /**
     * Handle Bluetooth turned off
     */
    private fun handleBluetoothTurnedOff() {
        connectionLock.withLock {
            // Clear any pending timeouts
            clearConnectionTimeout()
            
            // Update state to disconnected
            isConnecting.set(false)
            
            if (_connectionState.value == ConnectionState.CONNECTED || 
                _connectionState.value == ConnectionState.CONNECTING) {
                _connectionState.postValue(ConnectionState.CONNECTION_LOST)
                _connectionError.postValue("Bluetooth turned off")
                Log.d(TAG, "Connection lost due to Bluetooth turned off")
            }
        }
    }
    
    /**
     * Handle device disconnected
     */
    private fun handleDeviceDisconnected(newState: ConnectionState) {
        connectionLock.withLock {
            // Clear any pending timeouts
            clearConnectionTimeout()
            
            // Update state
            isConnecting.set(false)
            _connectionState.postValue(newState)
            
            val currentDevice = _connectedDevice.value
            if (currentDevice != null) {
                Log.d(TAG, "Device disconnected: ${currentDevice.address} (${currentDevice.name})")
                
                // Attempt to reconnect if connection was lost unexpectedly
                if (newState == ConnectionState.CONNECTION_LOST) {
                    attemptReconnection(currentDevice)
                }
            }
        }
    }
    
    /**
     * Attempt to reconnect to a device with exponential backoff
     */
    private fun attemptReconnection(deviceInfo: DeviceInfo) {
        val address = deviceInfo.address
        val retryCount = retryCountMap.getOrDefault(address, 0)
        
        if (retryCount < MAX_RETRY_COUNT) {
            // Exponential backoff delay
            val delay = RECONNECT_BASE_DELAY_MS * (1 shl retryCount)
            
            Log.d(TAG, "Scheduling reconnection attempt ${retryCount + 1}/$MAX_RETRY_COUNT in ${delay}ms")
            
            // Increment retry count
            retryCountMap[address] = retryCount + 1
            
            // Schedule reconnection
            handler.postDelayed({
                Log.d(TAG, "Attempting reconnection to: ${deviceInfo.address} (${deviceInfo.name})")
                connectToDevice(deviceInfo.address, deviceInfo.name)
            }, delay)
        } else {
            Log.d(TAG, "Max reconnection attempts reached for device: $address")
            _connectionError.postValue("Failed to reconnect after $MAX_RETRY_COUNT attempts")
            
            // Reset retry count
            retryCountMap.remove(address)
        }
    }
    
    /**
     * Set connection timeout
     */
    private fun setConnectionTimeout(address: String) {
        // Clear any existing timeout
        clearConnectionTimeout()
        
        // Create new timeout runnable
        connectionTimeoutRunnable = Runnable {
            connectionLock.withLock {
                if (isConnecting.get() && _connectedDevice.value?.address == address) {
                    Log.d(TAG, "Connection timeout for device: $address")
                    
                    // Update state
                    isConnecting.set(false)
                    _connectionState.postValue(ConnectionState.CONNECTION_FAILED)
                    _connectionError.postValue("Connection timeout")
                    
                    // Increment retry count
                    val retryCount = retryCountMap.getOrDefault(address, 0)
                    retryCountMap[address] = retryCount + 1
                    
                    // Attempt reconnection if under max retry count
                    val deviceInfo = _connectedDevice.value
                    if (deviceInfo != null && retryCount < MAX_RETRY_COUNT) {
                        attemptReconnection(deviceInfo)
                    } else if (deviceInfo != null) {
                        Log.d(TAG, "Max reconnection attempts reached for device: $address")
                        _connectionError.postValue("Failed to connect after $MAX_RETRY_COUNT attempts")
                        
                        // Reset retry count
                        retryCountMap.remove(address)
                    }
                }
            }
        }
        
        // Schedule timeout
        handler.postDelayed(connectionTimeoutRunnable!!, CONNECTION_TIMEOUT_MS)
    }
    
    /**
     * Clear connection timeout
     */
    private fun clearConnectionTimeout() {
        connectionTimeoutRunnable?.let {
            handler.removeCallbacks(it)
            connectionTimeoutRunnable = null
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            // Unregister receiver
            context.unregisterReceiver(bluetoothStateReceiver)
            
            // Disconnect any connected device
            disconnectCurrentDevice()
            
            // Clear any pending handlers
            handler.removeCallbacksAndMessages(null)
            
            Log.d(TAG, "ConnectionManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}
