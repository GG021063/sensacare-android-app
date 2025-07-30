package com.sensacare.app

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var switchAutoSync: SwitchCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        setupToolbar()
        loadSettings()
        setupListeners()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        switchAutoSync = findViewById(R.id.switchAutoSync)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Settings"
        }
    }

    private fun loadSettings() {
        // Load settings from SharedPreferences directly
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        
        // Set default values for now - in a real app these would be loaded from preferences
        switchNotifications.isChecked = sharedPreferences.getBoolean("notifications_enabled", true)
        switchDarkMode.isChecked = sharedPreferences.getBoolean("dark_mode_enabled", false)
        switchAutoSync.isChecked = sharedPreferences.getBoolean("auto_sync_enabled", true)
    }

    private fun setupListeners() {
        // Set up listeners for settings changes
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("notifications_enabled", isChecked)
            editor.apply()
            
            // Show feedback to user
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("dark_mode_enabled", isChecked)
            editor.apply()
            
            // Show feedback to user
            val message = if (isChecked) "Dark mode will be applied on restart" else "Light mode will be applied on restart"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            
            // Note: In a real app, we would apply the theme change immediately
        }

        switchAutoSync.setOnCheckedChangeListener { _, isChecked ->
            editor.putBoolean("auto_sync_enabled", isChecked)
            editor.apply()
            
            // Show feedback to user
            val message = if (isChecked) "Auto sync enabled" else "Auto sync disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the back button
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        // Constants for settings keys could be defined here
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_AUTO_SYNC = "auto_sync_enabled"
        
        // VeePoo specific settings keys
        const val KEY_HEART_RATE_MONITORING = "heart_rate_monitoring"
        const val KEY_SLEEP_TRACKING = "sleep_tracking"
        const val KEY_STEP_COUNTING = "step_counting"
    }
}
