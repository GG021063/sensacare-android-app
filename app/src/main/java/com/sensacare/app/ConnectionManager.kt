package com.sensacare.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData
import com.veepoo.protocol.model.enums.EFunctionStatus
import java.util.concurrent.CopyOnWriteArrayList

/**
 * ConnectionManager
 * 
 * This class manages connections to HBand devices using the VeePoo SDK.
 * It provides methods for scanning, connecting, and managing device connections,
 * as well as reading health data from connected devices.
 * 
 * This implementation uses the VeePoo SDK which is mandatory for connecting to
 * ET4922, ET593, and other HBand devices.
 */
class ConnectionManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ConnectionManager? = null
        private const val TAG = "ConnectionManager"
        private const val CONNECTION_VERIFICATION_INTERVAL_MS = 5000L // 5 seconds
        private const val DATA_HEARTBEAT_TIMEOUT_MS = 30000L // 30 seconds
        private const val SCAN_TIMEOUT_MS = 10000L // 10 seconds scan timeout
        
        // Device name prefixes for VeePoo devices
        private val VEEPOO_DEVICE_PREFIXES = listOf("ET", "ID", "VPB", "HBand")

        fun getInstance(context: Context): ConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // VeePoo SDK manager
    private val vpOperateManager: VPOperateManager = VPOperateManager.getMangerInstance(context)
    
    // VeePoo Device Manager - the specialized manager for VeePoo SDK operations
    private val veePooDeviceManager: VeePooDeviceManager = VeePooDeviceManager.getInstance(context)
    
    // Handler for periodic verification
    private val handler = Handler(Looper.getMainLooper())
    
    // Last time data was received from device
    private var lastDataReceivedTimestamp = 0L
    
    // Bluetooth manager for checking actual Bluetooth state
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    
    // Current connection state
    private var isConnecting = false
    private var isConnected = false
    
    // Connected device information
    private var connectedDeviceAddress: String? = null
    private var connectedDeviceName: String? = null
    
    // Scan results list - type maintained for UI compatibility
    private val scanResults = CopyOnWriteArrayList<Any>()
    
    // Scan and connection callbacks
    private var onScanResultCallback: ((List<Any>) -> Unit)? = null
    private var onConnectedCallback: ((Boolean) -> Unit)? = null
    
    // Device support data
    private var deviceSupportData: FunctionDeviceSupportData? = null
    
    // Verification runnable for periodic checks
    private val verificationRunnable = object : Runnable {
        override fun run() {
            verifyRealConnection()
            handler.postDelayed(this, CONNECTION_VERIFICATION_INTERVAL_MS)
        }
    }

    init {
        // Start periodic verification
        startConnectionVerification()
        Log.d(TAG, "ConnectionManager initialized with VeePoo SDK integration")
    }

    /**
     * Start scanning for VeePoo devices
     */
    fun startScan(callback: (List<Any>) -> Unit) {
        try {
            // Clear previous scan results
            scanResults.clear()
            onScanResultCallback = callback
            
            Log.d(TAG, "Starting VeePoo device scan using VeePooDeviceManager")
            
            // Delegate to VeePooDeviceManager
            veePooDeviceManager.startScan(object : VeePooDeviceManager.ScanCallback {
                override fun onScanResult(devices: List<VeePooDeviceManager.DeviceInfo>) {
                    // Convert DeviceInfo to Any for UI compatibility
                    val deviceList = devices.map { it as Any }
                    scanResults.clear()
                    scanResults.addAll(deviceList)
                    callback(deviceList)
                }
                
                override fun onScanFinished() {
                    Log.d(TAG, "Scan finished")
                }
                
                override fun onScanError(errorMessage: String) {
                    Log.e(TAG, "Scan error: $errorMessage")
                    callback(emptyList())
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error starting scan: ${e.message}", e)
            callback(emptyList())
        }
    }

    /**
     * Stop scanning for devices
     */
    fun stopScan() {
        try {
            Log.d(TAG, "Stopping VeePoo device scan")
            veePooDeviceManager.stopScan()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}", e)
        }
    }

    /**
     * Connect to a device with the given address
     */
    fun connectDevice(address: String, name: String, callback: (Boolean) -> Unit) {
        try {
            if (isConnecting) {
                Log.d(TAG, "Already connecting to a device, ignoring request")
                callback(false)
                return
            }
            
            if (isConnected && address == connectedDeviceAddress) {
                Log.d(TAG, "Already connected to device $name ($address)")
                callback(true)
                return
            }
            
            // Disconnect from current device if connected
            if (isConnected) {
                disconnectDevice()
            }
            
            isConnecting = true
            onConnectedCallback = callback
            
            Log.d(TAG, "Connecting to VeePoo device: $name ($address)")
            
            // Delegate to VeePooDeviceManager
            veePooDeviceManager.connectDevice(address, name, object : VeePooDeviceManager.ConnectionCallback {
                override fun onConnected(device: VeePooDeviceManager.DeviceInfo) {
                    Log.d(TAG, "Successfully connected to ${device.deviceName} (${device.deviceAddress})")
                    
                    // Update internal state
                    isConnected = true
                    isConnecting = false
                    connectedDeviceAddress = device.deviceAddress
                    connectedDeviceName = device.deviceName
                    lastDataReceivedTimestamp = System.currentTimeMillis()
                    
                    // Save connection
                    saveConnection(device.deviceAddress, device.deviceName)
                    
                    // Notify callback
                    callback(true)
                }
                
                override fun onDisconnected() {
                    Log.d(TAG, "Device disconnected")
                    isConnected = false
                    isConnecting = false
                    clearConnection()
                    callback(false)
                }
                
                override fun onConnectionFailed(errorMessage: String) {
                    Log.e(TAG, "Connection failed: $errorMessage")
                    isConnecting = false
                    callback(false)
                }
                
                override fun onDataReceived(dataType: String, value: String) {
                    Log.d(TAG, "Data received: $dataType = $value")
                    lastDataReceivedTimestamp = System.currentTimeMillis()
                    notifyDataReceived()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device: ${e.message}", e)
            isConnecting = false
            callback(false)
        }
    }

    /**
     * Disconnect from the current device
     */
    fun disconnectDevice() {
        try {
            if (isConnected || isConnecting) {
                Log.d(TAG, "Disconnecting from VeePoo device")
                
                // Delegate to VeePooDeviceManager
                veePooDeviceManager.disconnectDevice()
                
                // Reset connection state
                isConnected = false
                isConnecting = false
                connectedDeviceAddress = null
                connectedDeviceName = null
                deviceSupportData = null
                clearConnection()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in disconnectDevice: ${e.message}", e)
            // Force reset connection state
            isConnected = false
            isConnecting = false
            clearConnection()
        }
    }
        
    /**
     * Check if a device name belongs to a VeePoo device
     */
    private fun isVeePooDevice(deviceName: String): Boolean {
        return VEEPOO_DEVICE_PREFIXES.any { prefix -> deviceName.startsWith(prefix) }
    }
        
    /**
     * Saves device connection state when successfully connected
     * Only saves if the device is actually connected via Bluetooth
     */
    fun saveConnection(deviceAddress: String, deviceName: String) {
        try {
            if (verifyBluetoothDevice(deviceAddress)) {
                Log.i(TAG, "Saving verified connection: $deviceName ($deviceAddress)")
                
                // Save in shared preferences for persistence
                val sharedPreferences = context.getSharedPreferences("sensacare_connection", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putString("device_address", deviceAddress)
                    .putString("device_name", deviceName)
                    .putBoolean("is_connected", true)
                    .apply()
                
                // Update internal state
                isConnected = true
                isConnecting = false
                connectedDeviceAddress = deviceAddress
                connectedDeviceName = deviceName
                lastDataReceivedTimestamp = System.currentTimeMillis()
            } else {
                Log.w(TAG, "Attempted to save unverified connection: $deviceName ($deviceAddress)")
                clearConnection()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving connection: ${e.message}", e)
        }
    }

    /**
     * Gets saved device address, returns null if no saved connection
     * or if the connection cannot be verified
     */
    fun getSavedDeviceAddress(): String? {
        if (!isConnected()) return null
        return connectedDeviceAddress
    }

    /**
     * Gets saved device name, returns null if no saved connection
     * or if the connection cannot be verified
     */
    fun getSavedDeviceName(): String? {
        if (!isConnected()) return null
        return connectedDeviceName
    }
    
    /**
     * Checks if device is REALLY connected by verifying:
     * 1. Bluetooth is enabled
     * 2. Device is actually paired in the Bluetooth adapter
     * 3. Data has been received recently (within timeout period)
     * 4. VeePooDeviceManager reports connected state
     */
    fun isConnected(): Boolean {
        try {
            // Check if Bluetooth is enabled
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.d(TAG, "Bluetooth is disabled or unavailable")
                return false
            }

            // Check if we have device info
            if (connectedDeviceAddress == null) {
                Log.d(TAG, "No device address stored")
                return false
            }
            
            // Check if device is actually paired in Bluetooth adapter
            if (!verifyBluetoothDevice(connectedDeviceAddress!!)) {
                Log.d(TAG, "Device $connectedDeviceAddress is not paired in Bluetooth adapter")
                return false
            }
            
            // Check if data has been received recently
            val dataTimeout = System.currentTimeMillis() - lastDataReceivedTimestamp > DATA_HEARTBEAT_TIMEOUT_MS
            if (dataTimeout && lastDataReceivedTimestamp > 0) {
                Log.d(TAG, "No data received from device $connectedDeviceAddress for ${DATA_HEARTBEAT_TIMEOUT_MS}ms")
                return false
            }
            
            // Check VeePooDeviceManager connection state
            if (!veePooDeviceManager.isConnected()) {
                Log.d(TAG, "VeePooDeviceManager reports device is not connected")
                return false
            }

            return isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking connection state: ${e.message}", e)
            return false
        }
    }

    /**
     * Verify that a device with the given address is actually paired in the Bluetooth adapter
     */
    private fun verifyBluetoothDevice(address: String): Boolean {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return false
            }

            // Check if device is in paired devices list
            val pairedDevices = bluetoothAdapter.bondedDevices
            return pairedDevices.any { it.address == address }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying Bluetooth device: ${e.message}", e)
            return false
        }
    }

    /**
     * Marks device as disconnected and clears connection state
     */
    fun markDisconnected() {
        Log.i(TAG, "Marking device as disconnected")
        disconnectDevice()
    }

    /**
     * Clears all connection state
     */
    fun clearConnection() {
        try {
            Log.i(TAG, "Clearing connection state")
            
            // Clear from shared preferences
            val sharedPreferences = context.getSharedPreferences("sensacare_connection", Context.MODE_PRIVATE)
            sharedPreferences.edit()
                .remove("device_address")
                .remove("device_name")
                .putBoolean("is_connected", false)
                .apply()
            
            // Update internal state
            isConnected = false
            isConnecting = false
            connectedDeviceAddress = null
            connectedDeviceName = null
            lastDataReceivedTimestamp = 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing connection: ${e.message}", e)
        }
    }

    /**
     * Gets device info if connected, returns Pair(address, name) or null
     */
    fun getConnectedDevice(): Pair<String, String>? {
        if (!isConnected()) {
            return null
        }
        
        // Delegate to VeePooDeviceManager
        val deviceInfo = veePooDeviceManager.getConnectedDevice()
        if (deviceInfo != null) {
            return Pair(deviceInfo.deviceAddress, deviceInfo.deviceName)
        }
        
        // Fallback to local state if VeePooDeviceManager fails
        val address = connectedDeviceAddress ?: return null
        val name = connectedDeviceName ?: "Unknown Device"
        return Pair(address, name)
    }

    /**
     * Notifies that data was received from the device
     * This updates the last data timestamp to prevent timeout
     */
    fun notifyDataReceived() {
        lastDataReceivedTimestamp = System.currentTimeMillis()
    }

    /**
     * Start periodic connection verification
     */
    private fun startConnectionVerification() {
        handler.post(verificationRunnable)
    }

    /**
     * Stop periodic connection verification
     */
    private fun stopConnectionVerification() {
        handler.removeCallbacks(verificationRunnable)
    }

    /**
     * Verify real connection and clean up if needed
     */
    private fun verifyRealConnection() {
        try {
            // If we think we're connected but can't verify the connection, clear it
            if (isConnected && !isConnected()) {
                Log.w(TAG, "Detected invalid connection state, cleaning up")
                disconnectDevice()
            }
            
            // Check VeePooDeviceManager connection state
            if (isConnected && !veePooDeviceManager.isConnected()) {
                Log.w(TAG, "VeePooDeviceManager reports disconnected state, cleaning up")
                disconnectDevice()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying connection: ${e.message}", e)
        }
    }

    /**
     * Check if a device supports a specific function
     */
    fun isDeviceSupportFunction(functionId: Int): Boolean {
        /*
         * TODO: Replace this stub once the exact property names in
         *       FunctionDeviceSupportData are confirmed for the
         *       bundled VeePoo SDK version.  For now we conservatively
         *       report that no optional function is supported in order
         *       to prevent crashes from unresolved references.
         */
        return false
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            stopConnectionVerification()
            handler.removeCallbacksAndMessages(null)
            disconnectDevice()
            veePooDeviceManager.cleanup()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources: ${e.message}", e)
        }
    }

    /**
     * Restore previous connection if available
     */
    fun restorePreviousConnection(callback: (Boolean) -> Unit) {
        try {
            val sharedPreferences = context.getSharedPreferences("sensacare_connection", Context.MODE_PRIVATE)
            val isConnected = sharedPreferences.getBoolean("is_connected", false)
            
            if (isConnected) {
                val address = sharedPreferences.getString("device_address", null)
                val name = sharedPreferences.getString("device_name", null)
                
                if (address != null && name != null) {
                    Log.d(TAG, "Attempting to restore connection to: $name ($address)")
                    connectDevice(address, name, callback)
                } else {
                    callback(false)
                }
            } else {
                callback(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring previous connection: ${e.message}", e)
            callback(false)
        }
    }
}
