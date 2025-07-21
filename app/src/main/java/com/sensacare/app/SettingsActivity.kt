package com.sensacare.app

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sensacare.app.R
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.model.datas.DeviceInfo

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var vpOperateManager: VPOperateManager
    private lateinit var tvBatteryLevel: TextView
    private lateinit var tvDeviceName: TextView
    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        setupToolbar()
        initializeViews()
        setupNavigation()
        initializeVeepooSDK()
        loadDeviceInfo()
    }
    
    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Settings"
        }
    }
    
    private fun initializeViews() {
        tvBatteryLevel = findViewById(R.id.tvBatteryLevel)
        tvDeviceName = findViewById(R.id.tvDeviceName)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Set up click listeners for settings options
        findViewById<android.view.View>(R.id.layoutRingDetail).setOnClickListener {
            startActivity(Intent(this, RingDetailActivity::class.java))
        }
        
        findViewById<android.view.View>(R.id.layoutCaring).setOnClickListener {
            showCaringDialog()
        }
        
        findViewById<android.view.View>(R.id.layoutCharging).setOnClickListener {
            showChargingDialog()
        }
        
        findViewById<android.view.View>(R.id.layoutWarranty).setOnClickListener {
            showWarrantyDialog()
        }
        
        findViewById<android.view.View>(R.id.layoutDeleteAccount).setOnClickListener {
            showDeleteAccountDialog()
        }
    }
    
    private fun setupNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_settings
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
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
    
    private fun loadDeviceInfo() {
        // Load device information
        tvDeviceName.text = "SensaCare Ring"
        tvBatteryLevel.text = "60%"
        
        // Get actual device info if connected
        val connectedDevice = vpOperateManager.getConnectedDevice()
        connectedDevice?.let { device ->
            tvDeviceName.text = device.deviceName
            // Get battery level from device
            // tvBatteryLevel.text = "${device.batteryLevel}%"
        }
    }
    
    private fun showCaringDialog() {
        AlertDialog.Builder(this)
            .setTitle("Caring for your Ring")
            .setMessage("""
                • Clean your ring regularly with a soft cloth
                • Avoid exposure to extreme temperatures
                • Remove before swimming or showering
                • Store in a dry, cool place when not in use
                • Avoid contact with chemicals or lotions
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showChargingDialog() {
        AlertDialog.Builder(this)
            .setTitle("Charging your Ring")
            .setMessage("""
                • Use the provided charging cable
                • Place ring on charging dock properly
                • Charge for 2-3 hours for full battery
                • Avoid overcharging
                • Keep charging area clean and dry
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showWarrantyDialog() {
        AlertDialog.Builder(this)
            .setTitle("Warranty Policy")
            .setMessage("""
                • 1-year limited warranty
                • Covers manufacturing defects
                • Normal wear and tear not covered
                • Contact support for warranty claims
                • Proof of purchase required
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.")
            .setPositiveButton("Delete Account") { _, _ ->
                // Handle account deletion
                // Navigate to login screen
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
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