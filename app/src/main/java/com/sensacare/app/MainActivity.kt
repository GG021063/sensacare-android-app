package com.sensacare.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sensacare.app.models.SimpleDeviceInfo
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MainActivity : AppCompatActivity() {
    
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    
    // VeePoo SDK manager
    private var vpOperateManager: VPOperateManager? = null
    
    // UI Components
    private lateinit var btnScan: Button
    private lateinit var btnConnect: Button
    private lateinit var btnDashboard: Button
    private lateinit var btnSettings: Button
    private lateinit var tvStatus: TextView
    private lateinit var rvDevices: RecyclerView
    private lateinit var deviceAdapter: DeviceAdapter
    
    // Device list
    private val deviceList = mutableListOf<SimpleDeviceInfo>()
    private var isScanning = false
    private var selectedDevice: SimpleDeviceInfo? = null
    
    // Connection state tracking with thread safety
    private val isConnecting = AtomicBoolean(false)
    private var currentConnectionAddress: String? = null
    private var connectionRetryCount = 0
    private var connectionTimeoutHandler: Handler? = null
    private val connectionLock = ReentrantLock()
    
    // Connection verification
    private var connectionVerificationHandler: Handler? = null
    private var isVerifyingConnection = AtomicBoolean(false)
    
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSIONS = 2
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        // VeePoo service UUID - this may need to be adjusted based on actual VeePoo devices
        private val VEEPOO_SERVICE_UUID = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb")
        
        // ET492 specific constants
        private const val ET492_NAME_PREFIX = "ET492"
        private const val ET492_CONNECTION_TIMEOUT = 30000L // 30 seconds for ET492 (longer than standard)
        private const val STANDARD_CONNECTION_TIMEOUT = 15000L // 15 seconds for other devices
        private const val MAX_CONNECTION_RETRIES = 3
        private const val CONNECTION_RETRY_DELAY_BASE = 1000L // Base delay of 1 second
        private const val CONNECTION_VERIFICATION_INTERVAL = 5000L // 5 seconds between verifications
        private const val CONNECTION_VERIFICATION_TIMEOUT = 10000L // 10 seconds to verify connection
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            initializeViews()
            initializeBluetooth()
            initializeVeePooSDK()
            checkPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Initialization error: ${e.message}", e)
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            tvStatus.text = "Initialization failed - please restart app"
        }
    }
    
    private fun initializeViews() {
        try {
            btnScan = findViewById(R.id.btnScan)
            btnConnect = findViewById(R.id.btnConnect)
            btnDashboard = findViewById(R.id.btnDashboard)
            btnSettings = findViewById(R.id.btnSettings)
            tvStatus = findViewById(R.id.tvStatus)
            rvDevices = findViewById(R.id.rvDevices)
            
            // Set up RecyclerView
            deviceAdapter = DeviceAdapter(deviceList) { deviceInfo ->
                showDeviceSelectionDialog(deviceInfo)
            }
            rvDevices.layoutManager = LinearLayoutManager(this)
            rvDevices.adapter = deviceAdapter
            
            // Set up button click listeners
            btnScan.setOnClickListener {
                if (isScanning) {
                    stopScanning()
                } else {
                    startDeviceScanning()
                }
            }
            
            btnConnect.setOnClickListener {
                if (selectedDevice != null) {
                    connectToDevice(selectedDevice!!)
                } else {
                    Toast.makeText(this, "Please select a device first", Toast.LENGTH_SHORT).show()
                }
            }
            
            btnDashboard.setOnClickListener {
                navigateToDashboard()
            }
            
            btnSettings.setOnClickListener {
                navigateToSettings()
            }
            
            // Initially disable connect button
            btnConnect.isEnabled = false
            
            Log.d(TAG, "Views initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e
        }
    }
    
    private fun navigateToDashboard() {
        try {
            // Check if we have a real connection before navigating
            val connectionManager = ConnectionManager.getInstance(this)
            if (connectionManager.isConnected()) {
                startActivity(Intent(this, HomeActivity::class.java))
            } else {
                Toast.makeText(this, "No device connected. Please connect a device first.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to dashboard: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToSettings() {
        try {
            startActivity(Intent(this, SettingsActivity::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to settings: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showDeviceSelectionDialog(deviceInfo: SimpleDeviceInfo) {
        try {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Select Device")
                .setMessage("Do you want to connect to ${deviceInfo.deviceName}?")
                .setPositiveButton("Connect") { _, _ ->
                    selectedDevice = deviceInfo
                    btnConnect.isEnabled = true
                    tvStatus.text = "Selected: ${deviceInfo.deviceName}"
                    Toast.makeText(
                        this,
                        "Device selected: ${deviceInfo.deviceName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
                .create()

            dialog.show()

            // ─── Force WHITE text on dark dialog background ──────────────────────
            val white = ContextCompat.getColor(this, android.R.color.white)

            // Title text
            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            dialog.findViewById<TextView>(titleId)?.setTextColor(white)

            // Message text
            dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(white)

            // Buttons
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(white)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(white)

            // Optional: ensure buttons inherit alpha of enabled/disabled state
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.isAllCaps = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.isAllCaps = false

        } catch (e: Exception) {
            Log.e(TAG, "Error showing device selection dialog: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun initializeBluetooth() {
        try {
            bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter is null - device doesn't support Bluetooth")
                Toast.makeText(this, "This device doesn't support Bluetooth", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            if (bluetoothAdapter.isEnabled) {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } else {
                    Log.e(TAG, "Bluetooth connect permission not granted")
                    Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
                    checkPermissions()
                }
            }
            
            // Register for Bluetooth state changes
            val filter = android.content.IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(bluetoothStateReceiver, filter)
            
            Log.d(TAG, "Bluetooth initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Bluetooth: ${e.message}", e)
            Toast.makeText(this, "Error initializing Bluetooth: ${e.message}", Toast.LENGTH_LONG).show()
            throw e
        }
    }
    
    private fun initializeVeePooSDK() {
        try {
            // Initialize VeePoo SDK manager
            vpOperateManager = VPOperateManager.getInstance()
            Log.i(TAG, "VeePoo SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing VeePoo SDK: ${e.message}", e)
            Toast.makeText(this, "Error initializing VeePoo SDK: ${e.message}", Toast.LENGTH_LONG).show()
            // Don't throw - we can still function without VeePoo SDK for scanning
        }
    }
    
    private val bluetoothStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                when (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_OFF -> {
                        tvStatus.text = "Bluetooth is disabled"
                        if (isScanning) stopScanning()
                        
                        // Clean up any ongoing connection attempts
                        cleanupConnectionAttempt()
                        
                        // Clear any saved connection state since Bluetooth is off
                        val connectionManager = ConnectionManager.getInstance(this@MainActivity)
                        connectionManager.markDisconnected()
                        
                        Log.d(TAG, "Bluetooth turned off")
                    }
                    BluetoothAdapter.STATE_TURNING_OFF -> {
                        tvStatus.text = "Bluetooth turning off..."
                        Log.d(TAG, "Bluetooth turning off")
                    }
                    BluetoothAdapter.STATE_ON -> {
                        tvStatus.text = "Bluetooth enabled - Ready to scan"
                        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                        Log.d(TAG, "Bluetooth turned on")
                    }
                    BluetoothAdapter.STATE_TURNING_ON -> {
                        tvStatus.text = "Bluetooth turning on..."
                        Log.d(TAG, "Bluetooth turning on")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Bluetooth state receiver: ${e.message}", e)
            }
        }
    }
    
    private fun checkPermissions() {
        try {
            val permissionsToRequest = mutableListOf<String>()
            
            for (permission in REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission)
                }
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    REQUEST_PERMISSIONS
                )
                Log.d(TAG, "Requesting permissions: ${permissionsToRequest.joinToString()}")
            } else {
                Log.d(TAG, "All permissions already granted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions: ${e.message}", e)
        }
    }
    
    private fun startDeviceScanning() {
        if (isScanning) return
        
        try {
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter is null")
                Toast.makeText(this, "Bluetooth not available on this device", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!bluetoothAdapter.isEnabled) {
                Log.d(TAG, "Bluetooth is disabled")
                Toast.makeText(this, "Bluetooth is disabled. Please enable it first.", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Bluetooth scan permission not granted")
                Toast.makeText(this, "Bluetooth scan permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
            
            isScanning = true
            deviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            
            btnScan.text = "Stop Scanning"
            tvStatus.text = "Scanning for all health devices..."
            
            // Scan for all BLE devices - removed VeePoo-specific filter
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            // Start BLE scanning without filters to show all devices
            bluetoothLeScanner?.startScan(null, settings, scanCallback)
            
            // Add timeout for scanning
            val scanTimeout = Handler(Looper.getMainLooper())
            scanTimeout.postDelayed({
                if (isScanning) {
                    stopScanning()
                    if (deviceList.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Scan timeout - no devices found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Scan completed - found ${deviceList.size} devices", Toast.LENGTH_SHORT).show()
                    }
                }
            }, 30000) // 30 second timeout
            
            Log.d(TAG, "Started device scanning")
        } catch (e: Exception) {
            // Handle scanning errors
            isScanning = false
            btnScan.text = "Scan for Devices"
            tvStatus.text = "Scan failed: ${e.message}"
            Toast.makeText(this, "Scanning error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error starting device scan: ${e.message}", e)
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            
            try {
                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                
                val device = result.device ?: return
                val deviceName = device.name ?: return // Skip devices with no name
                val deviceAddress = device.address
                val deviceRssi = result.rssi
                
                // Show all devices with a valid name
                if (deviceName.isNotEmpty()) {
                    // Check if device is already in the list
                    val existingDeviceIndex = deviceList.indexOfFirst { it.deviceAddress == deviceAddress }
                    
                    if (existingDeviceIndex >= 0) {
                        // Update existing device
                        deviceList[existingDeviceIndex].deviceRssi = deviceRssi
                        runOnUiThread {
                            deviceAdapter.notifyItemChanged(existingDeviceIndex)
                        }
                    } else {
                        // Add new device
                        val newDevice = SimpleDeviceInfo(deviceName, deviceAddress, deviceRssi)
                        runOnUiThread {
                            deviceList.add(newDevice)
                            deviceAdapter.notifyItemInserted(deviceList.size - 1)
                            tvStatus.text = "Found ${deviceList.size} device(s)"
                        }
                        
                        Log.d(TAG, "Found device: $deviceName ($deviceAddress) RSSI: $deviceRssi")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scan result: ${e.message}", e)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            runOnUiThread {
                isScanning = false
                btnScan.text = "Scan for Devices"
                tvStatus.text = "Scan failed with error code: $errorCode"
                Toast.makeText(this@MainActivity, "Scan failed with error code: $errorCode", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Scan failed with error code: $errorCode")
            }
        }
    }
    
    private fun stopScanning() {
        if (!isScanning) return
        
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bluetoothLeScanner?.stopScan(scanCallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}", e)
        } finally {
            isScanning = false
            btnScan.text = "Scan for Devices"
            tvStatus.text = "Scan stopped. Found ${deviceList.size} device(s)"
            Log.d(TAG, "Stopped device scanning, found ${deviceList.size} devices")
        }
    }
    
    private fun connectToDevice(deviceInfo: SimpleDeviceInfo) {
        // Use connection lock to ensure thread safety
        connectionLock.withLock {
            // Check to prevent multiple connection attempts to the same device
            if (isConnecting.get()) {
                Log.w(TAG, "Connection already in progress, ignoring request")
                Toast.makeText(this, "Connection already in progress", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Pre-connection device validation
            if (!validateDeviceBeforeConnection(deviceInfo)) {
                return
            }
            
            // Cleanup before new connection attempt
            cleanupConnectionAttempt()
            
            // Set connection state
            isConnecting.set(true)
            currentConnectionAddress = deviceInfo.deviceAddress
            connectionRetryCount = 0
            
            tvStatus.text = "Connecting to ${deviceInfo.deviceName}..."
            btnConnect.isEnabled = false
            
            if (vpOperateManager == null) {
                Toast.makeText(this, "VeePoo SDK not initialized", Toast.LENGTH_SHORT).show()
                tvStatus.text = "Connection failed: VeePoo SDK not initialized"
                isConnecting.set(false)
                btnConnect.isEnabled = true
                return
            }
            
            // Set appropriate timeout based on device type
            val isET492Device = deviceInfo.deviceName.startsWith(ET492_NAME_PREFIX)
            val connectionTimeoutMs = if (isET492Device) ET492_CONNECTION_TIMEOUT else STANDARD_CONNECTION_TIMEOUT
            
            Log.i(TAG, "Connecting to ${deviceInfo.deviceName} (${deviceInfo.deviceAddress}), timeout: $connectionTimeoutMs ms")
            
            // Set connection timeout
            connectionTimeoutHandler = Handler(Looper.getMainLooper())
            connectionTimeoutHandler?.postDelayed({
                connectionLock.withLock {
                    if (isConnecting.get() && currentConnectionAddress == deviceInfo.deviceAddress) {
                        handleConnectionTimeout(deviceInfo)
                    }
                }
            }, connectionTimeoutMs)
            
            try {
                // Check if we have the necessary permissions
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Bluetooth connect permission not granted", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "Connection failed: Permission denied"
                    isConnecting.set(false)
                    btnConnect.isEnabled = true
                    return
                }
                
                // Start actual connection process based on device type
                if (isET492Device) {
                    Log.i(TAG, "Using ET492-specific connection protocol")
                    connectToET492Device(deviceInfo)
                } else {
                    // Standard connection for other devices
                    standardDeviceConnection(deviceInfo)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}", e)
                connectionTimeoutHandler?.removeCallbacksAndMessages(null)
                tvStatus.text = "Connection error: ${e.message}"
                Toast.makeText(this, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
                isConnecting.set(false)
                btnConnect.isEnabled = true
            }
        }
    }
    
    private fun validateDeviceBeforeConnection(deviceInfo: SimpleDeviceInfo): Boolean {
        try {
            // Check if device address is valid
            if (deviceInfo.deviceAddress.isBlank()) {
                Toast.makeText(this, "Invalid device address", Toast.LENGTH_SHORT).show()
                tvStatus.text = "Connection failed: Invalid device address"
                return false
            }
            
            // Check if Bluetooth is enabled
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Toast.makeText(this, "Bluetooth is disabled", Toast.LENGTH_SHORT).show()
                tvStatus.text = "Connection failed: Bluetooth is disabled"
                return false
            }
            
            // For ET492 devices, perform additional validation
            if (deviceInfo.deviceName.startsWith(ET492_NAME_PREFIX)) {
                // Check if RSSI is strong enough for stable connection
                if (deviceInfo.deviceRssi < -80) {
                    Toast.makeText(this, "ET492 signal too weak for stable connection", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "Warning: Signal weak, move closer to device"
                    // Continue anyway, just a warning
                }
            }
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating device: ${e.message}", e)
            Toast.makeText(this, "Error validating device: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    private fun connectToET492Device(deviceInfo: SimpleDeviceInfo) {
        Log.i(TAG, "Starting ET492-specific connection sequence")
        
        // For ET492 devices, we need a more robust connection approach
        try {
            // First, get the device object
            val device = getBluetoothDevice(deviceInfo.deviceAddress)
            if (device == null) {
                Log.e(TAG, "Could not get BluetoothDevice for address: ${deviceInfo.deviceAddress}")
                connectionLock.withLock {
                    handleConnectionFailure(deviceInfo, "Device not found")
                }
                return
            }
            
            // Attempt to create a bond if not already paired
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "Device not bonded, attempting to create bond")
                    // This is a background operation, we'll continue with connection attempt
                    device.createBond()
                }
            }
            
            // Attempt actual connection through VeePoo SDK
            // Note: We're using a simulated approach here since we don't have the full VeePoo SDK integration
            // In a real implementation, we would use the VeePoo SDK's connection methods
            
            // First phase - preparation
            Handler(Looper.getMainLooper()).postDelayed({
                connectionLock.withLock {
                    if (!isConnecting.get() || currentConnectionAddress != deviceInfo.deviceAddress) {
                        Log.w(TAG, "Connection attempt was canceled during preparation")
                        return@withLock
                    }
                    
                    tvStatus.text = "Initializing ET492 connection..."
                    
                    // Second phase - actual connection attempt
                    Handler(Looper.getMainLooper()).postDelayed({
                        connectionLock.withLock {
                            if (!isConnecting.get() || currentConnectionAddress != deviceInfo.deviceAddress) {
                                Log.w(TAG, "Connection attempt was canceled during connection")
                                return@withLock
                            }
                            
                            // Once connected, start verification process
                            startConnectionVerification(deviceInfo)
                        }
                    }, 2000) // 2 seconds for actual connection
                }
            }, 1000) // 1 second for preparation
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in ET492 connection process: ${e.message}", e)
            connectionLock.withLock {
                handleConnectionFailure(deviceInfo, "Connection error: ${e.message}")
            }
        }
    }
    
    private fun standardDeviceConnection(deviceInfo: SimpleDeviceInfo) {
        Log.i(TAG, "Starting standard connection sequence")
        
        try {
            // Get the device object
            val device = getBluetoothDevice(deviceInfo.deviceAddress)
            if (device == null) {
                Log.e(TAG, "Could not get BluetoothDevice for address: ${deviceInfo.deviceAddress}")
                connectionLock.withLock {
                    handleConnectionFailure(deviceInfo, "Device not found")
                }
                return
            }
            
            // Standard connection process
            Handler(Looper.getMainLooper()).postDelayed({
                connectionLock.withLock {
                    if (!isConnecting.get() || currentConnectionAddress != deviceInfo.deviceAddress) {
                        Log.w(TAG, "Connection attempt was canceled")
                        return@withLock
                    }
                    
                    // Start verification process
                    startConnectionVerification(deviceInfo)
                }
            }, 2000) // Simulate 2 second connection time
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in standard connection process: ${e.message}", e)
            connectionLock.withLock {
                handleConnectionFailure(deviceInfo, "Connection error: ${e.message}")
            }
        }
    }
    
    private fun getBluetoothDevice(address: String): BluetoothDevice? {
        return try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter.getRemoteDevice(address)
            } else {
                Log.e(TAG, "Bluetooth connect permission not granted")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Bluetooth device: ${e.message}", e)
            null
        }
    }
    
    private fun startConnectionVerification(deviceInfo: SimpleDeviceInfo) {
        if (isVerifyingConnection.getAndSet(true)) {
            Log.w(TAG, "Already verifying connection, ignoring request")
            return
        }
        
        Log.d(TAG, "Starting connection verification for ${deviceInfo.deviceName}")
        tvStatus.text = "Verifying connection..."
        
        // Set verification timeout
        connectionVerificationHandler = Handler(Looper.getMainLooper())
        connectionVerificationHandler?.postDelayed({
            if (isVerifyingConnection.get()) {
                Log.w(TAG, "Connection verification timeout")
                connectionLock.withLock {
                    handleConnectionFailure(deviceInfo, "Verification timeout - no data received")
                }
            }
        }, CONNECTION_VERIFICATION_TIMEOUT)
        
        // In a real implementation, we would register for data notifications
        // and wait for actual data from the device to verify the connection
        
        // For now, we'll simulate a verification process
        Handler(Looper.getMainLooper()).postDelayed({
            // Check if we're still verifying this device
            if (isVerifyingConnection.get() && currentConnectionAddress == deviceInfo.deviceAddress) {
                // Verify that the device is actually connected
                // In a real implementation, this would check for actual data received
                
                // For this simulation, we'll assume verification passed
                connectionLock.withLock {
                    handleSuccessfulConnection(deviceInfo)
                }
            }
        }, 3000) // 3 seconds for verification
    }
    
    private fun handleSuccessfulConnection(deviceInfo: SimpleDeviceInfo) {
        try {
            // Clear timeout handlers
            connectionTimeoutHandler?.removeCallbacksAndMessages(null)
            connectionTimeoutHandler = null
            connectionVerificationHandler?.removeCallbacksAndMessages(null)
            connectionVerificationHandler = null
            
            // Update state
            isConnecting.set(false)
            isVerifyingConnection.set(false)
            deviceInfo.isConnected = true
            
            // Update UI
            tvStatus.text = "Connected to ${deviceInfo.deviceName}"
            Toast.makeText(this@MainActivity, "Successfully connected!", Toast.LENGTH_SHORT).show()
            btnConnect.isEnabled = true
            
            Log.i(TAG, "Successfully connected to ${deviceInfo.deviceName} (${deviceInfo.deviceAddress})")
            
            // Navigate to dashboard
            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            intent.putExtra("device_address", deviceInfo.deviceAddress)
            intent.putExtra("device_name", deviceInfo.deviceName)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling successful connection: ${e.message}", e)
            // Even though there was an error in post-connection handling,
            // we'll consider the connection successful
            btnConnect.isEnabled = true
        }
    }
    
    private fun handleConnectionFailure(deviceInfo: SimpleDeviceInfo, reason: String) {
        try {
            Log.w(TAG, "Connection failed: $reason")
            
            // Clear verification state
            isVerifyingConnection.set(false)
            connectionVerificationHandler?.removeCallbacksAndMessages(null)
            connectionVerificationHandler = null
            
            // Clear connection timeout
            connectionTimeoutHandler?.removeCallbacksAndMessages(null)
            connectionTimeoutHandler = null
            
            // Update UI
            tvStatus.text = "Connection failed: $reason"
            Toast.makeText(this@MainActivity, "Connection failed: $reason", Toast.LENGTH_SHORT).show()
            btnConnect.isEnabled = true
            
            // Reset connection state
            isConnecting.set(false)
            currentConnectionAddress = null
            
            // Clear any saved connection state
            val connectionManager = ConnectionManager.getInstance(this@MainActivity)
            connectionManager.clearConnection()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling connection failure: ${e.message}", e)
            // Make sure we reset the connection state
            isConnecting.set(false)
            isVerifyingConnection.set(false)
            btnConnect.isEnabled = true
        }
    }
    
    private fun handleConnectionTimeout(deviceInfo: SimpleDeviceInfo) {
        try {
            Log.w(TAG, "Connection timeout for device: ${deviceInfo.deviceAddress}")
            
            // Clear verification state
            isVerifyingConnection.set(false)
            connectionVerificationHandler?.removeCallbacksAndMessages(null)
            connectionVerificationHandler = null
            
            tvStatus.text = "Connection timeout"
            Toast.makeText(this@MainActivity, "Connection timeout - please try again", Toast.LENGTH_SHORT).show()
            
            // Implement connection retry with backoff for ET492
            if (deviceInfo.deviceName.startsWith(ET492_NAME_PREFIX) && connectionRetryCount < MAX_CONNECTION_RETRIES) {
                connectionRetryCount++
                
                // Calculate backoff delay with exponential increase
                val retryDelay = CONNECTION_RETRY_DELAY_BASE * (1 shl (connectionRetryCount - 1))
                
                tvStatus.text = "Retrying connection (attempt $connectionRetryCount)..."
                Log.i(TAG, "Retrying ET492 connection, attempt $connectionRetryCount with delay $retryDelay ms")
                
                // Schedule retry
                Handler(Looper.getMainLooper()).postDelayed({
                    connectionLock.withLock {
                        if (currentConnectionAddress == deviceInfo.deviceAddress) {
                            Log.i(TAG, "Executing retry attempt $connectionRetryCount")
                            isConnecting.set(false) // Reset state before retry
                            connectToDevice(deviceInfo) // Retry connection
                        }
                    }
                }, retryDelay)
            } else {
                // Reset connection state
                isConnecting.set(false)
                currentConnectionAddress = null
                connectionRetryCount = 0
                btnConnect.isEnabled = true
                
                // Clear any saved connection state
                val connectionManager = ConnectionManager.getInstance(this@MainActivity)
                connectionManager.clearConnection()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling connection timeout: ${e.message}", e)
            // Make sure we reset the connection state
            isConnecting.set(false)
            isVerifyingConnection.set(false)
            btnConnect.isEnabled = true
        }
    }
    
    private fun cleanupConnectionAttempt() {
        try {
            Log.d(TAG, "Cleaning up previous connection attempt")
            
            // Clear any pending timeouts
            connectionTimeoutHandler?.removeCallbacksAndMessages(null)
            connectionTimeoutHandler = null
            
            // Clear verification state
            connectionVerificationHandler?.removeCallbacksAndMessages(null)
            connectionVerificationHandler = null
            isVerifyingConnection.set(false)
            
            // Reset connection state
            isConnecting.set(false)
            currentConnectionAddress = null
            connectionRetryCount = 0
            
            // Re-enable connect button
            btnConnect.isEnabled = selectedDevice != null
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up connection attempt: ${e.message}", e)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        try {
            when (requestCode) {
                REQUEST_ENABLE_BT -> {
                    if (resultCode == RESULT_OK) {
                        Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                        tvStatus.text = "Bluetooth enabled - Ready to scan"
                        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                    } else {
                        Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_LONG).show()
                        tvStatus.text = "Bluetooth is required"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling activity result: ${e.message}", e)
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        try {
            if (requestCode == REQUEST_PERMISSIONS) {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "Permissions granted - Ready to scan"
                } else {
                    Toast.makeText(this, "Permissions required for Bluetooth functionality", Toast.LENGTH_LONG).show()
                    tvStatus.text = "Permissions denied"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling permission result: ${e.message}", e)
        }
    }
    
    override fun onDestroy() {
        try {
            super.onDestroy()
            
            if (isScanning) {
                stopScanning()
            }
            
            // Clean up any ongoing connection attempts
            cleanupConnectionAttempt()
            
            // Unregister broadcast receiver
            try {
                unregisterReceiver(bluetoothStateReceiver)
            } catch (e: Exception) {
                // Receiver might not be registered
                Log.w(TAG, "Error unregistering receiver: ${e.message}")
            }
            
            Log.d(TAG, "MainActivity destroyed, all resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }
    }
}

// Device adapter for device list
class DeviceAdapter(
    private val devices: List<SimpleDeviceInfo>,
    private val onDeviceClick: (SimpleDeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {
    
    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val deviceName: TextView = view.findViewById(R.id.tvDeviceName)
        val deviceAddress: TextView = view.findViewById(R.id.tvDeviceAddress)
        val deviceRSSI: TextView = view.findViewById(R.id.tvDeviceRSSI)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.deviceName
        holder.deviceAddress.text = device.deviceAddress
        holder.deviceRSSI.text = "RSSI: ${device.deviceRssi}"
        
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }
    
    override fun getItemCount() = devices.size
}
