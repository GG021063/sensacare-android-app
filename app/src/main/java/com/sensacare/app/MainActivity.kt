package com.sensacare.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleConnectStatusListener
import com.veepoo.protocol.listener.base.IBleNotifyResponse
import com.veepoo.protocol.model.datas.DeviceInfo
import com.veepoo.protocol.model.enums.EConnectStatus
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.inuker.bluetooth.library.search.SearchResult

class MainActivity : AppCompatActivity() {
    
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
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
    private val deviceList = mutableListOf<DeviceInfo>()
    private var isScanning = false
    private var sdkInitialized = false
    private var selectedDevice: DeviceInfo? = null
    
    companion object {
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
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            initializeViews()
            initializeBluetooth()
            checkPermissions()
            initializeVeepooSDK()
        } catch (e: Exception) {
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
            tvStatus.text = "Initialization failed - please restart app"
        }
    }
    
    private fun initializeViews() {
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
            startActivity(Intent(this, DashboardActivity::class.java))
        }
        
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // Initially disable connect button
        btnConnect.isEnabled = false
    }
    
    private fun showDeviceSelectionDialog(deviceInfo: DeviceInfo) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Device")
            .setMessage("Do you want to connect to ${deviceInfo.deviceName}?")
            .setPositiveButton("Connect") { _, _ ->
                selectedDevice = deviceInfo
                btnConnect.isEnabled = true
                tvStatus.text = "Selected: ${deviceInfo.deviceName}"
                Toast.makeText(this, "Device selected: ${deviceInfo.deviceName}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        
        dialog.show()
    }
    
    private fun initializeBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                @Suppress("DEPRECATION")
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        }
        
        // Register for Bluetooth state changes
        val filter = android.content.IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)
    }
    
    private val bluetoothStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF -> {
                    tvStatus.text = "Bluetooth is disabled"
                    if (isScanning) stopScanning()
                }
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    tvStatus.text = "Bluetooth turning off..."
                }
                BluetoothAdapter.STATE_ON -> {
                    tvStatus.text = "Bluetooth enabled - Ready to scan"
                }
                BluetoothAdapter.STATE_TURNING_ON -> {
                    tvStatus.text = "Bluetooth turning on..."
                }
            }
        }
    }
    
    private fun checkPermissions() {
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
        }
    }
    
    private fun initializeVeepooSDK() {
        try {
            vpOperateManager = VPOperateManager.getMangerInstance(this)
            sdkInitialized = true
            
            // Set up connection status listener
            vpOperateManager?.setBleConnectStatusListener(object : IBleConnectStatusListener {
                override fun onConnectStatusChanged(deviceInfo: DeviceInfo, status: EConnectStatus) {
                    runOnUiThread {
                        when (status) {
                            EConnectStatus.CONNECTED -> {
                                Toast.makeText(this@MainActivity, "Device connected!", Toast.LENGTH_SHORT).show()
                                tvStatus.text = "Connected to ${deviceInfo.deviceName}"
                                // Navigate to dashboard
                                startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                            }
                            EConnectStatus.DISCONNECTED -> {
                                Toast.makeText(this@MainActivity, "Device disconnected", Toast.LENGTH_SHORT).show()
                                tvStatus.text = "Device disconnected"
                            }
                            EConnectStatus.CONNECTING -> {
                                Toast.makeText(this@MainActivity, "Connecting to device...", Toast.LENGTH_SHORT).show()
                                tvStatus.text = "Connecting..."
                            }
                            else -> {
                                Toast.makeText(this@MainActivity, "Connection status: $status", Toast.LENGTH_SHORT).show()
                                tvStatus.text = "Status: $status"
                            }
                        }
                    }
                }
            })
            
            // Set up notification response listener
            vpOperateManager?.setBleNotifyResponse(object : IBleNotifyResponse {
                override fun onNotifyResponse(deviceInfo: DeviceInfo, response: ByteArray) {
                    // Handle device notifications
                    handleDeviceNotification(deviceInfo, response)
                }
            })
            
            tvStatus.text = "Ready to scan for devices"
        } catch (e: Exception) {
            // Handle Veepoo SDK initialization errors
            Toast.makeText(this, "Veepoo SDK error: ${e.message}", Toast.LENGTH_LONG).show()
            tvStatus.text = "SDK initialization failed - using basic mode"
            sdkInitialized = false
            btnScan.isEnabled = false
        }
    }
    
    private fun handleDeviceNotification(deviceInfo: DeviceInfo, response: ByteArray) {
        // Process device data notifications
        runOnUiThread {
            tvStatus.text = "Received data from ${deviceInfo.deviceName}"
        }
    }
    
    private fun startDeviceScanning() {
        if (isScanning) return
        
        if (!sdkInitialized) {
            Toast.makeText(this, "Veepoo SDK not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            isScanning = true
            deviceList.clear()
            deviceAdapter.notifyDataSetChanged()
            
            btnScan.text = "Stop Scanning"
            tvStatus.text = "Scanning for devices..."
            
            // Add timeout for scanning
            val scanTimeout = android.os.Handler(android.os.Looper.getMainLooper())
            scanTimeout.postDelayed({
                if (isScanning) {
                    stopScanning()
                    Toast.makeText(this@MainActivity, "Scan timeout - no devices found", Toast.LENGTH_SHORT).show()
                }
            }, 30000) // 30 second timeout
            
            // Start scanning for Veepoo devices using real SDK
            vpOperateManager?.startScanDevice(object : SearchResponse {
                override fun onSearchStarted() {
                    runOnUiThread {
                        tvStatus.text = "Scan started..."
                    }
                }
                
                override fun onDeviceFounded(device: SearchResult) {
                    runOnUiThread {
                        // Convert SearchResult to DeviceInfo
                        val deviceInfo = DeviceInfo().apply {
                            deviceName = device.name ?: "Unknown Device"
                            deviceAddress = device.address
                            deviceRSSI = device.rssi
                        }
                        
                        // Add to list if not already present
                        if (!deviceList.any { it.deviceAddress == deviceInfo.deviceAddress }) {
                            deviceList.add(deviceInfo)
                            deviceAdapter.notifyItemInserted(deviceList.size - 1)
                            tvStatus.text = "Found ${deviceList.size} device(s)"
                        }
                    }
                }
                
                override fun onSearchStopped() {
                    runOnUiThread {
                        isScanning = false
                        btnScan.text = "Scan for Devices"
                        tvStatus.text = "Scan completed. Found ${deviceList.size} device(s)"
                        scanTimeout.removeCallbacksAndMessages(null)
                    }
                }
                
                override fun onSearchCanceled() {
                    runOnUiThread {
                        isScanning = false
                        btnScan.text = "Scan for Devices"
                        tvStatus.text = "Scan cancelled"
                        scanTimeout.removeCallbacksAndMessages(null)
                    }
                }
            })
        } catch (e: Exception) {
            // Handle scanning errors
            isScanning = false
            btnScan.text = "Scan for Devices"
            tvStatus.text = "Scan failed: ${e.message}"
            Toast.makeText(this, "Scanning error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopScanning() {
        if (!isScanning) return
        
        try {
            vpOperateManager?.stopScanDevice()
        } catch (e: Exception) {
            // Handle stop scanning errors
        }
        
        isScanning = false
        btnScan.text = "Scan for Devices"
        tvStatus.text = "Scan stopped"
    }
    
    private fun connectToDevice(deviceInfo: DeviceInfo) {
        if (!sdkInitialized) {
            Toast.makeText(this, "Veepoo SDK not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        
        tvStatus.text = "Connecting to ${deviceInfo.deviceName}..."
        
        // Add connection timeout
        val connectionTimeout = android.os.Handler(android.os.Looper.getMainLooper())
        connectionTimeout.postDelayed({
            if (tvStatus.text.toString().contains("Connecting")) {
                tvStatus.text = "Connection timeout"
                Toast.makeText(this@MainActivity, "Connection timeout - please try again", Toast.LENGTH_SHORT).show()
            }
        }, 15000) // 15 second timeout
        
        try {
            // Connect to device using real SDK
            vpOperateManager?.connectDevice(deviceInfo.deviceAddress, object : com.veepoo.protocol.listener.base.IConnectResponse {
                override fun connectState(code: Int, profile: com.inuker.bluetooth.library.model.BleGattProfile?, isOadModel: Boolean) {
                    runOnUiThread {
                        connectionTimeout.removeCallbacksAndMessages(null)
                        
                        when (code) {
                            com.inuker.bluetooth.library.Code.REQUEST_SUCCESS -> {
                                tvStatus.text = "Connected to ${deviceInfo.deviceName}"
                                Toast.makeText(this@MainActivity, "Successfully connected!", Toast.LENGTH_SHORT).show()
                                
                                // Navigate directly to dashboard
                                val intent = Intent(this@MainActivity, DashboardActivity::class.java)
                                intent.putExtra("device_address", deviceInfo.deviceAddress)
                                intent.putExtra("device_name", deviceInfo.deviceName)
                                startActivity(intent)
                            }
                            com.inuker.bluetooth.library.Code.REQUEST_FAILED -> {
                                tvStatus.text = "Connection failed"
                                Toast.makeText(this@MainActivity, "Connection failed - device may be out of range", Toast.LENGTH_SHORT).show()
                            }
                            com.inuker.bluetooth.library.Code.REQUEST_TIMEOUT -> {
                                tvStatus.text = "Connection timeout"
                                Toast.makeText(this@MainActivity, "Connection timeout - please try again", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                tvStatus.text = "Connection failed (Error: $code)"
                                Toast.makeText(this@MainActivity, "Connection failed: $code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
        } catch (e: Exception) {
            connectionTimeout.removeCallbacksAndMessages(null)
            tvStatus.text = "Connection error: ${e.message}"
            Toast.makeText(this, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "Bluetooth enabled - Ready to scan"
                } else {
                    Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                tvStatus.text = "Permissions granted - Ready to scan"
            } else {
                Toast.makeText(this, "Permissions required for Bluetooth functionality", Toast.LENGTH_LONG).show()
                tvStatus.text = "Permissions denied"
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isScanning) {
            stopScanning()
        }
        
        try {
            vpOperateManager?.disconnectDevice()
        } catch (e: Exception) {
            // Handle disconnect errors
        }
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}

// Device adapter for device list
class DeviceAdapter(
    private val devices: List<DeviceInfo>,
    private val onDeviceClick: (DeviceInfo) -> Unit
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
        holder.deviceRSSI.text = "RSSI: ${device.deviceRSSI}"
        
        holder.itemView.setOnClickListener {
            onDeviceClick(device)
        }
    }
    
    override fun getItemCount() = devices.size
} 