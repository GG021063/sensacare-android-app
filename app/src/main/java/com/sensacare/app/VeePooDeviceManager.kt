package com.sensacare.app

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * VeePooDeviceManager
 * 
 * A comprehensive manager for VeePoo SDK integration that handles:
 * - Device scanning for ET4922, ET593, and other HBand devices
 * - Connection management with proper error handling
 * - Health data listeners for various metrics
 * - SDK version compatibility through reflection
 * - Connection verification and heartbeat checks
 * 
 * This manager provides a clean interface for the ConnectionManager to use
 * while handling all the complexities of the VeePoo SDK integration.
 */
class VeePooDeviceManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "VeePooDeviceManager"
        
        // Timeouts and intervals
        private const val SCAN_TIMEOUT_MS = 10000L // 10 seconds scan timeout
        private const val CONNECTION_TIMEOUT_MS = 15000L // 15 seconds connection timeout
        private const val HEARTBEAT_INTERVAL_MS = 5000L // 5 seconds heartbeat interval
        private const val DATA_TIMEOUT_MS = 30000L // 30 seconds data timeout
        
        // Device name prefixes for VeePoo devices
        private val VEEPOO_DEVICE_PREFIXES = listOf("ET", "ID", "VPB", "HBand")
        
        // Specific device model prefixes
        private val ET4922_PREFIX = "ET492"
        private val ET593_PREFIX = "ET593"
        
        // Debug flag to disable simulation and force real SDK calls
        private const val FORCE_REAL_SDK_CALLS = true
        
        // SDK interface constants
        private const val DISCONNECT_STATE = 3 // IConnectResponse.DISCONNECT value
        
        // Singleton instance
        @Volatile
        private var INSTANCE: VeePooDeviceManager? = null
        
        fun getInstance(context: Context): VeePooDeviceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VeePooDeviceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // -------------------------------------------------------------------------
    // SDK Manager and Bluetooth
    // -------------------------------------------------------------------------
    
    // VeePoo SDK manager
    private val vpOperateManager: VPOperateManager by lazy {
        try {
            val manager = VPOperateManager.getMangerInstance(context)
            Log.d(TAG, "Loaded SDK class = ${manager.javaClass.name}")
            manager
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing VPOperateManager: ${e.message}", e)
            throw e
        }
    }
    
    // Bluetooth manager for checking actual Bluetooth state
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter = bluetoothManager?.adapter
    
    // Handler for timeouts and periodic tasks
    private val handler = Handler(Looper.getMainLooper())
    
    // -------------------------------------------------------------------------
    // State Management
    // -------------------------------------------------------------------------
    
    // Connection state
    private val isScanning = AtomicBoolean(false)
    private val isConnecting = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    
    // Device information
    private var connectedDeviceAddress: String? = null
    private var connectedDeviceName: String? = null
    private var deviceSupportData: Any? = null // Using Any for flexibility with different SDK versions
    
    // Last data timestamp for heartbeat verification
    private var lastDataReceivedTimestamp = 0L
    
    // Scan results
    private val scanResults = CopyOnWriteArrayList<Any>()
    
    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------
    
    // Callback interfaces
    interface ScanCallback {
        fun onScanResult(devices: List<DeviceInfo>)
        fun onScanFinished()
        fun onScanError(errorMessage: String)
    }
    
    interface ConnectionCallback {
        fun onConnected(device: DeviceInfo)
        fun onDisconnected()
        fun onConnectionFailed(errorMessage: String)
        fun onDataReceived(dataType: String, value: String)
    }
    
    // Active callbacks
    private var scanCallback: ScanCallback? = null
    private var connectionCallback: ConnectionCallback? = null
    
    // -------------------------------------------------------------------------
    // SDK Callback Implementations
    // -------------------------------------------------------------------------
    
    // Heart rate data listener
    private val heartDataCallback = object : Any() {
        fun onDataChange(heartData: Any?) {
            try {
                heartData?.let {
                    // Extract heart rate using reflection
                    val heartRateField = findFieldByNameSuffix(it.javaClass, "heartRate")
                    if (heartRateField != null) {
                        heartRateField.isAccessible = true
                        val heartRate = heartRateField.get(it) as? Int ?: 0
                        val value = heartRate.toString()
                        
                        Log.d(TAG, "Heart rate data received: $value BPM")
                        lastDataReceivedTimestamp = System.currentTimeMillis()
                        connectionCallback?.onDataReceived("heartRate", value)
                    } else {
                        Log.e(TAG, "Could not find heartRate field in ${it.javaClass.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing heart rate data: ${e.message}", e)
            }
        }
    }
    
    // Blood oxygen data listener
    private val spo2Callback = object : Any() {
        fun onDataChange(spo2Data: Any?) {
            try {
                spo2Data?.let {
                    // Extract spo2h using reflection
                    val spo2hField = findFieldByNameSuffix(it.javaClass, "spo2h")
                    if (spo2hField != null) {
                        spo2hField.isAccessible = true
                        val spo2h = spo2hField.get(it) as? Int ?: 0
                        val value = spo2h.toString()
                        
                        Log.d(TAG, "Blood oxygen data received: $value%")
                        lastDataReceivedTimestamp = System.currentTimeMillis()
                        connectionCallback?.onDataReceived("bloodOxygen", value)
                    } else {
                        Log.e(TAG, "Could not find spo2h field in ${it.javaClass.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing blood oxygen data: ${e.message}", e)
            }
        }
        
        // Alternative method name that might be used in some SDK versions
        fun onSpO2HADataChange(spo2Data: Any?) {
            onDataChange(spo2Data)
        }
    }
    
    // Blood pressure data listener
    private val bpCallback = object : Any() {
        fun onDataChange(bpData: Any?) {
            try {
                bpData?.let {
                    // Extract blood pressure using reflection
                    val highField = findFieldByNameSuffix(it.javaClass, "highPressure")
                    val lowField = findFieldByNameSuffix(it.javaClass, "lowPressure")
                    
                    if (highField != null && lowField != null) {
                        highField.isAccessible = true
                        lowField.isAccessible = true
                        
                        val high = highField.get(it) as? Int ?: 0
                        val low = lowField.get(it) as? Int ?: 0
                        val value = "$high/$low"
                        
                        Log.d(TAG, "Blood pressure data received: $value mmHg")
                        lastDataReceivedTimestamp = System.currentTimeMillis()
                        connectionCallback?.onDataReceived("bloodPressure", value)
                    } else {
                        Log.e(TAG, "Could not find pressure fields in ${it.javaClass.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing blood pressure data: ${e.message}", e)
            }
        }
    }
    
    // Step data listener
    private val stepCallback = object : Any() {
        fun onDataChange(step: Int) {
            try {
                val value = step.toString()
                Log.d(TAG, "Step data received: $value steps")
                lastDataReceivedTimestamp = System.currentTimeMillis()
                connectionCallback?.onDataReceived("steps", value)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing step data: ${e.message}", e)
            }
        }
    }
    
    // Sleep data listener
    private val sleepCallback = object : Any() {
        fun onDataChange(sleepData: Any?) {
            try {
                sleepData?.let {
                    // Extract sleep data using reflection
                    val qualityTimeField = findFieldByNameSuffix(it.javaClass, "sleepQualityTime")
                    val qualityField = findFieldByNameSuffix(it.javaClass, "sleepQuality")
                    
                    if (qualityTimeField != null) {
                        qualityTimeField.isAccessible = true
                        val qualityTime = qualityTimeField.get(it) as? Int ?: 0
                        
                        val hours = qualityTime / 60
                        val minutes = qualityTime % 60
                        val value = "${hours}h ${minutes}m"
                        
                        // Get quality if available
                        var quality = ""
                        if (qualityField != null) {
                            qualityField.isAccessible = true
                            quality = " (quality: ${qualityField.get(it)})"
                        }
                        
                        Log.d(TAG, "Sleep data received: $value$quality")
                        lastDataReceivedTimestamp = System.currentTimeMillis()
                        connectionCallback?.onDataReceived("sleep", value)
                    } else {
                        Log.e(TAG, "Could not find sleepQualityTime field in ${it.javaClass.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing sleep data: ${e.message}", e)
            }
        }
        
        // Alternative method signature used in some SDK versions
        fun onSleepDataChange(date: String?, sleepData: Any?) {
            onDataChange(sleepData)
        }
    }
    
    // Temperature data listener
    private val temperatureCallback = object : Any() {
        fun onDataChange(tempData: Any?) {
            try {
                tempData?.let {
                    // Extract temperature using reflection
                    val tempField = findFieldByNameSuffix(it.javaClass, "temperature")
                    if (tempField != null) {
                        tempField.isAccessible = true
                        val temp = tempField.get(it) as? Float ?: 0f
                        val value = String.format("%.1f", temp)
                        
                        Log.d(TAG, "Temperature data received: $value°C")
                        lastDataReceivedTimestamp = System.currentTimeMillis()
                        connectionCallback?.onDataReceived("temperature", value)
                    } else {
                        Log.e(TAG, "Could not find temperature field in ${it.javaClass.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing temperature data: ${e.message}", e)
            }
        }
    }
    
    // HRV data listener
    private val hrvCallback = object : Any() {
        fun onDataChange(hrvData: Any?) {
            try {
                hrvData?.let {
                    // Extract HRV using reflection
                    val hrvValueField = findFieldByNameSuffix(it.javaClass, "hrvValue")
                    if (hrvValueField != null) {
                        hrvValueField.isAccessible = true
                        val hrv = hrvValueField.get(it) as? Int ?: 0
                        val value = hrv.toString()
                        
                        Log.d(TAG, "HRV data received: $value ms")
                        lastDataReceivedTimestamp = System.currentTimeMillis()
                        connectionCallback?.onDataReceived("hrv", value)
                    } else {
                        Log.e(TAG, "Could not find hrvValue field in ${it.javaClass.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing HRV data: ${e.message}", e)
            }
        }
        
        // Required by some SDK versions
        fun onReadOriginProgress(progress: Float) {
            Log.d(TAG, "HRV read progress: $progress")
        }
    }
    
    // Connection response
    private val connectResponse = object : Any() {
        fun connectState(state: Int, newState: Boolean) {
            Log.d(TAG, "Connect state changed: state=$state, newState=$newState")
            if (newState) {
                // Connected successfully
                isConnecting.set(false)
                isConnected.set(true)
                
                // Register data listeners and sync time
                registerDataListeners()
                syncDeviceTime()
                
                // Notify callback
                connectedDeviceAddress?.let { address ->
                    connectedDeviceName?.let { name ->
                        connectionCallback?.onConnected(DeviceInfo(name, address, 0))
                    }
                }
            } else {
                // Disconnected or failed to connect
                isConnecting.set(false)
                isConnected.set(false)
                
                if (state == DISCONNECT_STATE) {
                    Log.d(TAG, "Device disconnected")
                    connectionCallback?.onDisconnected()
                } else {
                    Log.e(TAG, "Failed to connect: state=$state")
                    connectionCallback?.onConnectionFailed("Failed to connect: state=$state")
                }
            }
        }
        
        // Alternative method signature used in some SDK versions
        fun connectState(state: Int, profile: Any?, newState: Boolean) {
            connectState(state, newState)
        }
    }
    
    // Notification response
    private val notifyResponse = object : Any() {
        fun notifyState(state: Int) {
            Log.d(TAG, "Notify state changed: state=$state")
        }
    }
    
    // Real-time data listener
    private val realTimeCallback = object : Any() {
        fun connectState(state: Boolean) {
            Log.d(TAG, "Real-time connection state: $state")
            if (!state && isConnected.get()) {
                // Device disconnected
                isConnected.set(false)
                connectionCallback?.onDisconnected()
            }
        }
    }
    
    // Write response
    private val writeResponse = object : IBleWriteResponse {
        override fun onResponse(code: Int) {
            Log.d(TAG, "Write response: code=$code")
        }
    }
    
    // -------------------------------------------------------------------------
    // Heartbeat and Verification
    // -------------------------------------------------------------------------
    
    // Heartbeat runnable for connection verification
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            verifyConnection()
            handler.postDelayed(this, HEARTBEAT_INTERVAL_MS)
        }
    }
    
    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------
    
    init {
        try {
            Log.d(TAG, "Initializing VeePooDeviceManager")
            
            // Start heartbeat verification
            startHeartbeat()
            
            // Initialize SDK with reflection to handle different versions
            initializeSdk()
            
            Log.d(TAG, "VeePooDeviceManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing VeePooDeviceManager: ${e.message}", e)
        }
    }
    
    /**
     * Initialize the SDK with reflection to handle different versions
     */
    private fun initializeSdk() {
        try {
            // Check if SDK is properly initialized
            Log.d(TAG, "Loaded SDK class = ${vpOperateManager.javaClass.name}")
            val sdkVersion = getSdkVersion()
            Log.d(TAG, "VeePoo SDK Version: $sdkVersion")
            
            // Check if we can access key SDK methods
            val scanMethod = findMethod(vpOperateManager.javaClass, "startScanDevice")
            Log.d(TAG, "Scan method available: ${scanMethod != null}")
            
            val connectMethod = findMethod(vpOperateManager.javaClass, "connectDevice")
            Log.d(TAG, "Connect method available: ${connectMethod != null}")
            
            // Check for heart rate method
            val readHeartMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readHeart")
            Log.d(TAG, "Read heart data method available: ${readHeartMethod != null}")
            
            // Check for blood oxygen method
            val readSpo2Method = findMethodByNamePrefix(vpOperateManager.javaClass, "readBloodOxygen")
            Log.d(TAG, "Read blood oxygen method available: ${readSpo2Method != null}")
            
            // Check for blood pressure method
            val readBpMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readBloodPressure")
            Log.d(TAG, "Read blood pressure method available: ${readBpMethod != null}")
            
            // Check for step data method
            val readStepMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readStep")
            Log.d(TAG, "Read step data method available: ${readStepMethod != null}")
            
            // Check for sleep data method
            val readSleepMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readSleep")
            Log.d(TAG, "Read sleep data method available: ${readSleepMethod != null}")
            
            // Check for temperature data method
            val readTempMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readTemp")
            Log.d(TAG, "Read temperature data method available: ${readTempMethod != null}")
            
            // Check for HRV data method
            val readHrvMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readHRV")
            Log.d(TAG, "Read HRV data method available: ${readHrvMethod != null}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SDK: ${e.message}", e)
        }
    }
    
    /**
     * Get the SDK version using reflection
     */
    private fun getSdkVersion(): String {
        return try {
            val versionMethod = findMethod(vpOperateManager.javaClass, "getVersion")
            if (versionMethod != null) {
                versionMethod.invoke(vpOperateManager) as? String ?: "Unknown"
            } else {
                Log.e(TAG, "Failed to find getVersion() method")
                "Unknown (Method not found)"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SDK version: ${e.message}", e)
            "Unknown (Error)"
        }
    }
    
    // -------------------------------------------------------------------------
    // Device Scanning
    // -------------------------------------------------------------------------
    
    /**
     * Start scanning for VeePoo devices
     */
    fun startScan(callback: ScanCallback) {
        if (isScanning.getAndSet(true)) {
            Log.d(TAG, "Scan already in progress, ignoring request")
            return
        }
        
        try {
            // Clear previous results
            scanResults.clear()
            scanCallback = callback
            
            Log.d(TAG, "Starting scan for VeePoo devices")
            
            // Set scan timeout
            handler.postDelayed({
                if (isScanning.get()) {
                    stopScan()
                    callback.onScanFinished()
                }
            }, SCAN_TIMEOUT_MS)
            
            // Try to use real SDK scan method first
            val scanMethod = findMethod(vpOperateManager.javaClass, "startScanDevice")
            if (scanMethod != null) {
                Log.d(TAG, "Using real SDK scan method")
                try {
                    // Try to find search request/response classes
                    val searchRequestClass = findClassByNameSuffix("SearchRequest")
                    val searchResponseClass = findClassByNameSuffix("SearchResponse")
                    
                    if (searchRequestClass != null && searchResponseClass != null) {
                        // Create search request instance
                        val searchRequest = searchRequestClass.newInstance()
                        
                        // Create search response instance with reflection
                        val searchResponseConstructor = searchResponseClass.getConstructor()
                        val searchResponse = searchResponseConstructor.newInstance()
                        
                        // Set callback with reflection
                        val setSearchCallbackMethod = findMethod(searchResponseClass, "setSearchCallback")
                        if (setSearchCallbackMethod != null) {
                            // Create callback
                            val searchCallback = object : Any() {
                                fun onSearchStarted() {
                                    Log.d(TAG, "Search started")
                                }
                                
                                fun onDeviceFound(device: Any?) {
                                    device?.let {
                                        Log.d(TAG, "Device found: $it")
                                        scanResults.add(it)
                                        
                                        // Extract device info
                                        val nameField = findFieldByNameSuffix(it.javaClass, "Name")
                                        val addressField = findFieldByNameSuffix(it.javaClass, "Address")
                                        val rssiField = findFieldByNameSuffix(it.javaClass, "Rssi")
                                        
                                        if (nameField != null && addressField != null) {
                                            nameField.isAccessible = true
                                            addressField.isAccessible = true
                                            
                                            val name = nameField.get(it) as? String ?: "Unknown"
                                            val address = addressField.get(it) as? String ?: "Unknown"
                                            val rssi = if (rssiField != null) {
                                                rssiField.isAccessible = true
                                                rssiField.getInt(it)
                                            } else {
                                                0
                                            }
                                            
                                            // Only add VeePoo devices
                                            if (isVeePooDevice(name)) {
                                                val devices = scanResults.mapNotNull { result ->
                                                    try {
                                                        val n = nameField.get(result) as? String ?: "Unknown"
                                                        val a = addressField.get(result) as? String ?: "Unknown"
                                                        val r = if (rssiField != null) {
                                                            rssiField.isAccessible = true
                                                            rssiField.getInt(result)
                                                        } else {
                                                            0
                                                        }
                                                        
                                                        if (isVeePooDevice(n)) {
                                                            DeviceInfo(n, a, r)
                                                        } else {
                                                            null
                                                        }
                                                    } catch (e: Exception) {
                                                        null
                                                    }
                                                }
                                                
                                                callback.onScanResult(devices)
                                            }
                                        }
                                    }
                                }
                                
                                fun onSearchStopped() {
                                    Log.d(TAG, "Search stopped")
                                    if (isScanning.get()) {
                                        isScanning.set(false)
                                        callback.onScanFinished()
                                    }
                                }
                                
                                fun onSearchCanceled() {
                                    Log.d(TAG, "Search canceled")
                                    if (isScanning.get()) {
                                        isScanning.set(false)
                                        callback.onScanFinished()
                                    }
                                }
                            }
                            
                            // Set callback
                            setSearchCallbackMethod.invoke(searchResponse, searchCallback)
                            
                            // Start scan
                            scanMethod.invoke(vpOperateManager, searchRequest, searchResponse)
                            return
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error using real SDK scan method: ${e.message}", e)
                }
            } else {
                Log.e(TAG, "Failed to find startScanDevice() method")
            }
            
            // Fall back to simulated scan if real scan failed and not forcing real SDK calls
            if (!FORCE_REAL_SDK_CALLS) {
                Log.d(TAG, "Falling back to simulated scan")
                simulateScanResults(callback)
            } else {
                Log.e(TAG, "Real SDK scan failed and simulation is disabled")
                isScanning.set(false)
                callback.onScanError("Failed to start scan: SDK method not available")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in startScan: ${e.message}", e)
            isScanning.set(false)
            callback.onScanError("Error: ${e.message}")
        }
    }
    
    /**
     * Stop scanning for devices
     */
    fun stopScan() {
        if (!isScanning.getAndSet(false)) {
            return
        }
        
        try {
            Log.d(TAG, "Stopping scan")
            
            // Cancel scan timeout
            handler.removeCallbacksAndMessages(null)
            
            // Try to stop scanning using reflection
            try {
                val stopScanMethod = findMethod(vpOperateManager.javaClass, "stopScanDevice")
                if (stopScanMethod != null) {
                    stopScanMethod.invoke(vpOperateManager)
                    Log.d(TAG, "Real SDK stopScanDevice() method called successfully")
                } else {
                    Log.e(TAG, "Failed to find stopScanDevice() method")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping scan: ${e.message}", e)
            }
            
            scanCallback?.onScanFinished()
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopScan: ${e.message}", e)
        }
    }
    
    /**
     * Simulate scan results for testing or when SDK methods fail
     */
    private fun simulateScanResults(callback: ScanCallback) {
        Log.d(TAG, "Simulating scan results for testing")
        
        // Create simulated device list
        val simulatedDevices = listOf(
            DeviceInfo("ET4922 Demo", "00:11:22:33:44:55", -65),
            DeviceInfo("ET593 Demo", "AA:BB:CC:DD:EE:FF", -70)
        )
        
        // Notify callback
        callback.onScanResult(simulatedDevices)
        
        // Simulate scan completion after a delay
        handler.postDelayed({
            if (isScanning.get()) {
                isScanning.set(false)
                callback.onScanFinished()
            }
        }, 3000)
    }
    
    // -------------------------------------------------------------------------
    // Device Connection
    // -------------------------------------------------------------------------
    
    /**
     * Connect to a VeePoo device
     */
    fun connectDevice(deviceAddress: String, deviceName: String, callback: ConnectionCallback) {
        if (isConnecting.getAndSet(true)) {
            Log.d(TAG, "Connection already in progress, ignoring request")
            callback.onConnectionFailed("Connection already in progress")
            return
        }
        
        try {
            // Disconnect if already connected
            if (isConnected.get()) {
                disconnectDevice()
            }
            
            connectionCallback = callback
            connectedDeviceAddress = deviceAddress
            connectedDeviceName = deviceName
            
            Log.d(TAG, "Connecting to device: $deviceName ($deviceAddress)")
            
            // Set connection timeout
            handler.postDelayed({
                if (isConnecting.get() && !isConnected.get()) {
                    Log.w(TAG, "Connection timeout")
                    isConnecting.set(false)
                    callback.onConnectionFailed("Connection timeout")
                }
            }, CONNECTION_TIMEOUT_MS)
            
            // Try to use real SDK connect method
            val connectMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "connectDevice")
            
            if (connectMethod != null) {
                Log.d(TAG, "Using real SDK connect method")
                try {
                    // Try to determine the correct parameter types
                    val paramTypes = connectMethod.parameterTypes
                    
                    if (paramTypes.size >= 3 && paramTypes[0] == String::class.java) {
                        // Standard connect method with address and callbacks
                        if (paramTypes.size == 3) {
                            // connectDevice(String, IConnectResponse, INotifyResponse)
                            connectMethod.invoke(vpOperateManager, deviceAddress, connectResponse, notifyResponse)
                            return
                        } else if (paramTypes.size == 4 && paramTypes[1] == Boolean::class.java) {
                            // connectDevice(String, boolean, IConnectResponse, INotifyResponse)
                            connectMethod.invoke(vpOperateManager, deviceAddress, true, connectResponse, notifyResponse)
                            return
                        }
                    }
                    
                    // If we get here, we couldn't determine the correct method signature
                    Log.e(TAG, "Could not determine correct connect method signature: ${connectMethod}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error using real SDK connect method: ${e.message}", e)
                }
            } else {
                Log.e(TAG, "Failed to find connectDevice() method")
            }
            
            // Fall back to simulated connection if real connection failed and not forcing real SDK calls
            if (!FORCE_REAL_SDK_CALLS) {
                Log.d(TAG, "Falling back to simulated connection")
                simulateConnection(deviceAddress, deviceName, callback)
            } else {
                Log.e(TAG, "Real SDK connection failed and simulation is disabled")
                isConnecting.set(false)
                callback.onConnectionFailed("Failed to connect: SDK method not available")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in connectDevice: ${e.message}", e)
            isConnecting.set(false)
            callback.onConnectionFailed("Error: ${e.message}")
        }
    }
    
    /**
     * Register data listeners after connection
     */
    private fun registerDataListeners() {
        try {
            Log.d(TAG, "Registering data listeners")
            
            // Heart rate data
            val readHeartMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readHeart")
            if (readHeartMethod != null) {
                readHeartMethod.invoke(vpOperateManager, heartDataCallback)
                Log.d(TAG, "Heart rate listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find readHeartData() method")
            }
            
            // Blood oxygen data
            val readSpo2Method = findMethodByNamePrefix(vpOperateManager.javaClass, "readBloodOxygen")
            if (readSpo2Method != null) {
                readSpo2Method.invoke(vpOperateManager, spo2Callback)
                Log.d(TAG, "Blood oxygen listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find readBloodOxygen() method")
            }
            
            // Blood pressure data
            val readBpMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readBloodPressure")
            if (readBpMethod != null) {
                readBpMethod.invoke(vpOperateManager, bpCallback)
                Log.d(TAG, "Blood pressure listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find readBloodPressure() method")
            }
            
            // Step data
            val readStepMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readStep")
            if (readStepMethod != null) {
                readStepMethod.invoke(vpOperateManager, stepCallback)
                Log.d(TAG, "Step listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find readStepData() method")
            }
            
            // Sleep data
            val readSleepMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readSleep")
            if (readSleepMethod != null) {
                readSleepMethod.invoke(vpOperateManager, sleepCallback)
                Log.d(TAG, "Sleep listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find readSleepData() method")
            }
            
            // Temperature data
            val readTempMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readTemp")
            if (readTempMethod != null) {
                readTempMethod.invoke(vpOperateManager, temperatureCallback)
                Log.d(TAG, "Temperature listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find readTemptureData() method")
            }
            
            // HRV data
            val readHrvMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "readHRV")
            if (readHrvMethod != null) {
                readHrvMethod.invoke(vpOperateManager, hrvCallback)
                Log.d(TAG, "HRV listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find readHRVOriginData() method")
            }
            
            // Real-time data listener
            val setRealTimeMethod = findMethodByNamePrefix(vpOperateManager.javaClass, "setRealTimeData")
            if (setRealTimeMethod != null) {
                setRealTimeMethod.invoke(vpOperateManager, realTimeCallback)
                Log.d(TAG, "Real-time data listener registered successfully")
            } else {
                Log.e(TAG, "Failed to find setRealTimeDataListener() method")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error registering data listeners: ${e.message}", e)
        }
    }
    
    /**
     * Sync device time after connection
     */
    private fun syncDeviceTime() {
        try {
            Log.d(TAG, "Syncing device time")
            
            val syncTimeMethod = findMethod(vpOperateManager.javaClass, "syncTime")
            if (syncTimeMethod != null) {
                syncTimeMethod.invoke(vpOperateManager)
                Log.d(TAG, "Device time synced successfully")
            } else {
                Log.e(TAG, "Failed to find syncTime() method")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing device time: ${e.message}", e)
        }
    }
    
    /**
     * Simulate a connection for testing or when SDK methods fail
     */
    private fun simulateConnection(deviceAddress: String, deviceName: String, callback: ConnectionCallback) {
        Log.d(TAG, "Simulating connection for testing")
        
        // Simulate connection delay
        handler.postDelayed({
            isConnecting.set(false)
            isConnected.set(true)
            connectedDeviceAddress = deviceAddress
            connectedDeviceName = deviceName
            lastDataReceivedTimestamp = System.currentTimeMillis()
            
            callback.onConnected(DeviceInfo(deviceName, deviceAddress, 0))
            
            // Simulate data
            startSimulatedDataUpdates(callback)
        }, 1500)
    }
    
    /**
     * Start simulated data updates for testing
     */
    private fun startSimulatedDataUpdates(callback: ConnectionCallback) {
        val dataTypes = listOf("heartRate", "bloodOxygen", "bloodPressure", "steps", "sleep", "temperature", "hrv")
        var index = 0
        
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isConnected.get()) {
                    val dataType = dataTypes[index % dataTypes.size]
                    val value = when (dataType) {
                        "heartRate" -> (60 + (Math.random() * 30).toInt()).toString()
                        "bloodOxygen" -> (94 + (Math.random() * 6).toInt()).toString()
                        "bloodPressure" -> "${110 + (Math.random() * 20).toInt()}/${70 + (Math.random() * 15).toInt()}"
                        "steps" -> (1000 + (Math.random() * 5000).toInt()).toString()
                        "sleep" -> "${(5 + (Math.random() * 3)).toInt()}h ${(Math.random() * 59).toInt()}m"
                        "temperature" -> String.format("%.1f", 36.5 + (Math.random() * 1.5 - 0.5))
                        "hrv" -> (30 + (Math.random() * 50).toInt()).toString()
                        else -> "Unknown"
                    }
                    
                    Log.d(TAG, "Simulated data received: $dataType = $value")
                    lastDataReceivedTimestamp = System.currentTimeMillis()
                    callback.onDataReceived(dataType, value)
                    
                    index++
                    handler.postDelayed(this, 5000)
                }
            }
        }, 2000)
    }
    
    /**
     * Disconnect from the current device
     */
    fun disconnectDevice() {
        try {
            if (!isConnected.get() && !isConnecting.get()) {
                Log.d(TAG, "Not connected, nothing to disconnect")
                return
            }
            
            Log.d(TAG, "Disconnecting from device")
            
            // Try to disconnect using reflection
            try {
                val disconnectMethod = findMethod(vpOperateManager.javaClass, "disconnectWatch")
                if (disconnectMethod != null) {
                    disconnectMethod.invoke(vpOperateManager)
                    Log.d(TAG, "Disconnect method invoked successfully")
                } else {
                    Log.e(TAG, "Failed to find disconnectWatch() method")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting: ${e.message}", e)
            }
            
            // Reset state regardless of disconnect success
            isConnected.set(false)
            isConnecting.set(false)
            connectedDeviceAddress = null
            connectedDeviceName = null
            deviceSupportData = null
            
            // Notify callback
            connectionCallback?.onDisconnected()
        } catch (e: Exception) {
            Log.e(TAG, "Error in disconnectDevice: ${e.message}", e)
            // Force reset state
            isConnected.set(false)
            isConnecting.set(false)
        }
    }
    
    // -------------------------------------------------------------------------
    // Connection Verification
    // -------------------------------------------------------------------------
    
    /**
     * Start heartbeat verification
     */
    private fun startHeartbeat() {
        try {
            Log.d(TAG, "Starting heartbeat verification")
            handler.post(heartbeatRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting heartbeat: ${e.message}", e)
        }
    }
    
    /**
     * Stop heartbeat verification
     */
    private fun stopHeartbeat() {
        try {
            Log.d(TAG, "Stopping heartbeat verification")
            handler.removeCallbacks(heartbeatRunnable)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping heartbeat: ${e.message}", e)
        }
    }
    
    /**
     * Verify connection status
     */
    private fun verifyConnection() {
        try {
            if (isConnected.get()) {
                // Check if data has been received recently
                val dataTimeout = System.currentTimeMillis() - lastDataReceivedTimestamp > DATA_TIMEOUT_MS
                if (dataTimeout && lastDataReceivedTimestamp > 0) {
                    Log.w(TAG, "No data received for ${DATA_TIMEOUT_MS}ms, disconnecting")
                    disconnectDevice()
                    return
                }
                
                // Try to check SDK connection state
                try {
                    val isConnectedMethod = findMethod(vpOperateManager.javaClass, "isConnected")
                    if (isConnectedMethod != null) {
                        val sdkConnected = isConnectedMethod.invoke(vpOperateManager) as? Boolean ?: false
                        if (!sdkConnected) {
                            Log.w(TAG, "SDK reports disconnected state, disconnecting")
                            disconnectDevice()
                            return
                        }
                    } else {
                        Log.e(TAG, "Failed to find isConnected() method")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not check SDK connection state: ${e.message}")
                }
                
                // Verify device is still paired
                if (connectedDeviceAddress != null && !isDevicePaired(connectedDeviceAddress!!)) {
                    Log.w(TAG, "Device is no longer paired, disconnecting")
                    disconnectDevice()
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying connection: ${e.message}", e)
        }
    }
    
    /**
     * Check if a device is paired
     */
    private fun isDevicePaired(address: String): Boolean {
        try {
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return false
            }
            
            val pairedDevices = bluetoothAdapter.bondedDevices
            return pairedDevices.any { it.address == address }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if device is paired: ${e.message}", e)
            return false
        }
    }
    
    // -------------------------------------------------------------------------
    // Device Function Support
    // -------------------------------------------------------------------------
    
    /**
     * Check if device supports a specific function
     */
    fun isDeviceSupportFunction(functionId: String): Boolean {
        try {
            // Try to use real SDK method
            val isSupportMethod = findMethodByNamePrefix(
                vpOperateManager.javaClass,
                "isSupportFunction"
            )
            
            if (isSupportMethod != null) {
                return try {
                    val result = isSupportMethod.invoke(vpOperateManager, functionId) as? Boolean ?: false
                    Log.d(TAG, "Device support for function $functionId: $result")
                    result
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking function support: ${e.message}", e)
                    false
                }
            } else {
                Log.e(TAG, "Failed to find isSupportFunction() method")
            }
            
            // Fallback based on device type
            return when (functionId) {
                "heartRate" -> true
                "bloodOxygen" -> true
                "bloodPressure" -> true
                "steps" -> true
                "sleep" -> true
                "temperature" -> connectedDeviceName?.startsWith(ET593_PREFIX) ?: false
                "hrv" -> connectedDeviceName?.startsWith(ET593_PREFIX) ?: false
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in isDeviceSupportFunction: ${e.message}", e)
            return false
        }
    }
    
    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------
    
    /**
     * Check if a device name belongs to a VeePoo device
     */
    private fun isVeePooDevice(deviceName: String): Boolean {
        return VEEPOO_DEVICE_PREFIXES.any { prefix -> deviceName.startsWith(prefix) }
    }
    
    /**
     * Get the device type based on name
     */
    private fun getDeviceType(deviceName: String): String {
        return when {
            deviceName.startsWith(ET4922_PREFIX) -> "ET4922"
            deviceName.startsWith(ET593_PREFIX) -> "ET593"
            else -> "HBand"
        }
    }
    
    /**
     * Check if the device is connected
     */
    fun isConnected(): Boolean {
        return isConnected.get() && connectedDeviceAddress != null
    }
    
    /**
     * Get connected device info
     */
    fun getConnectedDevice(): DeviceInfo? {
        if (!isConnected()) {
            return null
        }
        
        val address = connectedDeviceAddress ?: return null
        val name = connectedDeviceName ?: "Unknown Device"
        return DeviceInfo(name, address, 0)
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up resources")
            
            // Stop scanning if in progress
            if (isScanning.get()) {
                stopScan()
            }
            
            // Disconnect if connected
            if (isConnected.get() || isConnecting.get()) {
                disconnectDevice()
            }
            
            // Stop heartbeat
            stopHeartbeat()
            
            // Clear all callbacks
            handler.removeCallbacksAndMessages(null)
            
            // Clear callbacks
            scanCallback = null
            connectionCallback = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources: ${e.message}", e)
        }
    }
    
    // -------------------------------------------------------------------------
    // Reflection Utilities
    // -------------------------------------------------------------------------
    
    /**
     * Find a method by name and parameter types
     */
    private fun findMethod(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method? {
        return try {
            clazz.getMethod(methodName, *parameterTypes)
        } catch (e: Exception) {
            try {
                // Try to find the method with compatible parameter types
                val methods = clazz.methods
                methods.find { method ->
                    if (method.name == methodName && method.parameterCount == parameterTypes.size) {
                        val methodParams = method.parameterTypes
                        var match = true
                        for (i in methodParams.indices) {
                            if (!methodParams[i].isAssignableFrom(parameterTypes[i])) {
                                match = false
                                break
                            }
                        }
                        match
                    } else {
                        false
                    }
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Error finding method $methodName: ${e2.message}", e2)
                null
            }
        }
    }
    
    /**
     * Find a method by name prefix
     */
    private fun findMethodByNamePrefix(clazz: Class<*>, prefix: String): Method? {
        return try {
            val methods = clazz.methods
            methods.find { it.name.startsWith(prefix) }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding method with prefix $prefix: ${e.message}", e)
            null
        }
    }
    
    /**
     * Find a class by name suffix
     */
    private fun findClassByNameSuffix(suffix: String): Class<*>? {
        return try {
            // Try common packages
            val packages = listOf(
                "com.veepoo.protocol.listener.data",
                "com.veepoo.protocol.listener.base",
                "com.veepoo.protocol.model.datas",
                "com.veepoo.protocol.model.settings",
                "com.veepoo.protocol.model.enums"
            )
            
            for (pkg in packages) {
                try {
                    val className = "$pkg.$suffix"
                    return Class.forName(className)
                } catch (e: ClassNotFoundException) {
                    // Continue to next package
                }
            }
            
            // If not found in common packages, try to find by suffix in all loaded classes
            // This is a fallback and might be slow
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding class with suffix $suffix: ${e.message}", e)
            null
        }
    }
    
    /**
     * Find a field by name suffix
     */
    private fun findFieldByNameSuffix(clazz: Class<*>, suffix: String): java.lang.reflect.Field? {
        return try {
            val fields = clazz.declaredFields
            fields.find { it.name.endsWith(suffix) }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding field with suffix $suffix: ${e.message}", e)
            null
        }
    }
    
    // -------------------------------------------------------------------------
    // Data Classes
    // -------------------------------------------------------------------------
    
    /**
     * Device information data class
     */
    data class DeviceInfo(
        val deviceName: String,
        val deviceAddress: String,
        val deviceRssi: Int,
        val isConnected: Boolean = false
    )
}
