package com.sensacare.app

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.model.datas.DeviceInfo

class RingDetailActivity : AppCompatActivity() {
    
    private lateinit var vpOperateManager: VPOperateManager
    private lateinit var tvModelSize: TextView
    private lateinit var tvGeneration: TextView
    private lateinit var tvFirmwareVersion: TextView
    private lateinit var tvMacAddress: TextView
    private lateinit var tvChargingMode: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ring_detail)
        
        setupToolbar()
        initializeViews()
        setupNavigation()
        initializeVeepooSDK()
        loadDeviceDetails()
    }
    
    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Ring Detail"
        }
    }
    
    private fun initializeViews() {
        tvModelSize = findViewById(R.id.tvModelSize)
        tvGeneration = findViewById(R.id.tvGeneration)
        tvFirmwareVersion = findViewById(R.id.tvFirmwareVersion)
        tvMacAddress = findViewById(R.id.tvMacAddress)
        tvChargingMode = findViewById(R.id.tvChargingMode)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }
    
    private fun setupNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_settings
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(android.content.Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_incrediness -> {
                    // Navigate to incrediness screen
                    true
                }
                R.id.nav_sleep -> {
                    // Navigate to sleep screen
                    true
                }
                R.id.nav_settings -> {
                    // Already on settings
                    true
                }
                else -> false
            }
        }
    }
    
    private fun initializeVeepooSDK() {
        vpOperateManager = VPOperateManager.getMangerInstance(this)
    }
    
    private fun loadDeviceDetails() {
        // Load sample device details (replace with actual device data)
        tvModelSize.text = "Silver, US 11"
        tvGeneration.text = "2"
        tvFirmwareVersion.text = "V2.2.0"
        tvMacAddress.text = "E4:33:FC:68:48:DB"
        tvChargingMode.text = "NFC Charging"
        
        // Get actual device info if connected
        val connectedDevice = vpOperateManager.getConnectedDevice()
        connectedDevice?.let { device ->
            // Update with real device data
            tvModelSize.text = "${device.deviceName}, ${device.deviceModel}"
            tvMacAddress.text = device.deviceAddress
            // Get other device properties from VeepooSDK
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
} 