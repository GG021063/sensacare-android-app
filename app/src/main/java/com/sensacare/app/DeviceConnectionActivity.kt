package com.sensacare.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// All VeePoo SDK imports are commented out to avoid compilation issues
// import com.veepoo.protocol.VPOperateManager
// import com.veepoo.protocol.listener.base.IBleConnectStatusListener
// import com.veepoo.protocol.model.datas.DeviceInfo
// import com.veepoo.protocol.model.enums.EConnectStatus
// import com.inuker.bluetooth.library.search.response.SearchResponse
// import com.inuker.bluetooth.library.search.SearchResult

/**
 * DeviceConnectionActivity - Temporarily disabled
 * 
 * This activity is temporarily replaced with a stub to allow the project to build
 * while we focus on getting the main connection working in MainActivity.
 */
class DeviceConnectionActivity : AppCompatActivity() {
    
    private lateinit var tvMessage: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_connection)
        
        // Find the status TextView to display our message
        tvMessage = findViewById(R.id.tvStatus)
        tvMessage.text = "Device Connection Screen - Coming Soon"
        
        // Hide any progress indicators or buttons that might be in the layout
        try {
            findViewById<android.view.View>(R.id.progressBar)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.btnScan)?.visibility = android.view.View.GONE
            findViewById<android.view.View>(R.id.btnConnect)?.visibility = android.view.View.GONE
        } catch (e: Exception) {
            // Ignore any view finding errors
        }
    }
    
    // Original implementation is commented out below
    /*
    private lateinit var vpOperateManager: VPOperateManager
    private lateinit var rvDevices: RecyclerView
    private lateinit var btnScan: Button
    private lateinit var btnConnect: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var deviceAdapter: DeviceAdapter
    
    private val deviceList = mutableListOf<DeviceInfo>()
    private var selectedDevice: DeviceInfo? = null
    private var isScanning = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_connection)
        
        initializeViews()
        initializeVeepooSDK()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        rvDevices = findViewById(R.id.rvDevices)
        btnScan = findViewById(R.id.btnScan)
        btnConnect = findViewById(R.id.btnConnect)
        tvStatus = findViewById(R.id.tvStatus)
        progressBar = findViewById(R.id.progressBar)
        
        // Set up RecyclerView
        deviceAdapter = DeviceAdapter(deviceList) { deviceInfo ->
            onDeviceSelected(deviceInfo)
        }
        rvDevices.layoutManager = LinearLayoutManager(this)
        rvDevices.adapter = deviceAdapter
        
        btnConnect.isEnabled = false
    }
    
    private fun initializeVeepooSDK() {
        vpOperateManager = VPOperateManager.getMangerInstance(this)
        
        // Set up connection status listener
        vpOperateManager.setBleConnectStatusListener(object : IBleConnectStatusListener {
            override fun onConnectStatusChanged(deviceInfo: DeviceInfo, status: EConnectStatus) {
                runOnUiThread {
                    handleConnectionStatusChange(deviceInfo, status)
                }
            }
        })
    }
    
    private fun setupClickListeners() {
        btnScan.setOnClickListener {
            if (isScanning) {
                stopScanning()
            } else {
                startScanning()
            }
        }
        
        btnConnect.setOnClickListener {
            selectedDevice?.let { device ->
                connectToDevice(device)
            }
        }
    }
    
    private fun startScanning() {
        if (isScanning) return
        
        isScanning = true
        deviceList.clear()
        deviceAdapter.notifyDataSetChanged()
        
        btnScan.text = "Stop Scan"
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Scanning for devices..."
        
        // Start scanning for Veepoo devices using real SDK
        vpOperateManager.startScanDevice(object : SearchResponse {
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
                    progressBar.visibility = View.GONE
                    tvStatus.text = "Scan completed. Found ${deviceList.size} device(s)"
                }
            }
            
            override fun onSearchCanceled() {
                runOnUiThread {
                    isScanning = false
                    btnScan.text = "Scan for Devices"
                    progressBar.visibility = View.GONE
                    tvStatus.text = "Scan cancelled"
                }
            }
        })
        
        // Stop scanning after 30 seconds
        btnScan.postDelayed({
            if (isScanning) {
                stopScanning()
            }
        }, 30000)
    }
    
    private fun stopScanning() {
        if (!isScanning) return
        
        vpOperateManager.stopScanDevice()
        isScanning = false
        btnScan.text = "Scan for Devices"
        progressBar.visibility = View.GONE
        tvStatus.text = "Scan stopped"
    }
    
    private fun onDeviceSelected(deviceInfo: DeviceInfo) {
        selectedDevice = deviceInfo
        btnConnect.isEnabled = true
        tvStatus.text = "Selected: ${deviceInfo.deviceName}"
    }
    
    private fun connectToDevice(deviceInfo: DeviceInfo) {
        btnConnect.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Connecting to ${deviceInfo.deviceName}..."
        
        // Connect to device using real SDK
        vpOperateManager.connectDevice(deviceInfo.deviceAddress, object : com.veepoo.protocol.listener.base.IConnectResponse {
            override fun connectState(code: Int, profile: com.inuker.bluetooth.library.model.BleGattProfile?, isOadModel: Boolean) {
                runOnUiThread {
                    when (code) {
                        com.inuker.bluetooth.library.Code.REQUEST_SUCCESS -> {
                            tvStatus.text = "Connected to ${deviceInfo.deviceName}"
                            progressBar.visibility = View.GONE
                            btnConnect.text = "Disconnect"
                            btnConnect.isEnabled = true
                            
                            // Navigate to dashboard after successful connection
                            startActivity(android.content.Intent(this@DeviceConnectionActivity, DashboardActivity::class.java))
                            finish()
                        }
                        com.inuker.bluetooth.library.Code.REQUEST_FAILED -> {
                            tvStatus.text = "Connection failed"
                            progressBar.visibility = View.GONE
                            btnConnect.isEnabled = true
                        }
                        com.inuker.bluetooth.library.Code.REQUEST_TIMEOUT -> {
                            tvStatus.text = "Connection timeout"
                            progressBar.visibility = View.GONE
                            btnConnect.isEnabled = true
                        }
                        else -> {
                            tvStatus.text = "Connection failed (Error: $code)"
                            progressBar.visibility = View.GONE
                            btnConnect.isEnabled = true
                        }
                    }
                }
            }
        })
    }
    
    private fun handleConnectionStatusChange(deviceInfo: DeviceInfo, status: EConnectStatus) {
        when (status) {
            EConnectStatus.CONNECTED -> {
                progressBar.visibility = View.GONE
                tvStatus.text = "Connected to ${deviceInfo.deviceName}"
                btnConnect.text = "Disconnect"
                btnConnect.isEnabled = true
                
                // Navigate to dashboard after successful connection
                startActivity(android.content.Intent(this, DashboardActivity::class.java))
                finish()
            }
            EConnectStatus.DISCONNECTED -> {
                progressBar.visibility = View.GONE
                tvStatus.text = "Disconnected from ${deviceInfo.deviceName}"
                btnConnect.text = "Connect"
                btnConnect.isEnabled = selectedDevice != null
            }
            EConnectStatus.CONNECTING -> {
                progressBar.visibility = View.VISIBLE
                tvStatus.text = "Connecting to ${deviceInfo.deviceName}..."
            }
            EConnectStatus.CONNECT_FAILED -> {
                progressBar.visibility = View.GONE
                tvStatus.text = "Connection failed"
                btnConnect.isEnabled = true
            }
            else -> {
                progressBar.visibility = View.GONE
                tvStatus.text = "Status: $status"
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh device list if needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (isScanning) {
            stopScanning()
        }
    }
    */
}

/*
// Device adapter for this activity
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
*/
