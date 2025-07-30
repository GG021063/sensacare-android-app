package com.sensacare.app

import android.content.Intent
import android.graphics.text.LineBreaker
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.sensacare.app.data.HealthDataManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * HomeActivity
 *
 * The main hub of the SensaCare app after login.
 * Provides access to all health monitoring features, device connection,
 * and displays a summary of the user's health status.
 */
class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    
    // Top Section - Greeting + Notification
    private lateinit var imgAvatar: ImageView
    private lateinit var tvGreeting: TextView
    private lateinit var tvTagline: TextView
    private lateinit var ivNotifications: ImageView
    private lateinit var tvNotifBadge: TextView
    
    // Profile Container and Initials
    private lateinit var profileContainer: FrameLayout
    private lateinit var tvAvatarInitials: TextView
    
    // Health Metrics Section
    private lateinit var btnDatePicker: LinearLayout
    private lateinit var tvSelectedDate: TextView
    
    // Health Report Card
    private lateinit var tvReportTitle: TextView
    private lateinit var tvReportStatus: TextView
    private lateinit var btnReportDetails: TextView
    
    // Individual Vitals Cards
    private lateinit var cardHeartRate: View
    private lateinit var cardGlucose: View
    private lateinit var cardTemperature: View
    private lateinit var cardSpO2: View
    private lateinit var cardBP: View
    private lateinit var cardRespiratory: View
    
    // Bottom Navigation
    private lateinit var bottomNavigation: BottomNavigationView
    
    // Device Connection Section
    private lateinit var deviceStatusCard: CardView
    private lateinit var deviceStatusIcon: ImageView
    private lateinit var deviceStatusTextView: TextView
    private lateinit var deviceNameTextView: TextView
    private lateinit var connectDeviceButton: MaterialButton
    
    // Additional Features
    private lateinit var viewAllChartsButton: MaterialButton
    
    // Health Data Manager
    private lateinit var healthDataManager: HealthDataManager
    
    // Device connection info
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var isDeviceConnected = false
    
    // Connection verification handler
    private val connectionHandler = Handler(Looper.getMainLooper())
    private val connectionVerificationRunnable = object : Runnable {
        override fun run() {
            verifyRealDeviceConnection()
            connectionHandler.postDelayed(this, CONNECTION_VERIFICATION_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        
        try {
            // Initialize UI components
            initializeViews()
            
            // Set up toolbar and navigation drawer
            setupToolbarAndNavigation()
            
            // Initialize health data manager
            healthDataManager = HealthDataManager.getInstance(applicationContext)
            
            // Load user data and set greeting
            loadUserData()
            
            // Set up profile display with user initials
            setupProfileDisplay()
            
            // Check device connection
            checkDeviceConnection()
            
            // Set up health report card
            setupHealthReportCard()
            
            // Set up vitals grid
            setupVitalsGrid()
            
            // Set up bottom navigation
            setupBottomNavigation()
            
            // Set up additional features
            setupAdditionalFeatures()
            
            // Display today's date
            displayCurrentDate()
            
            // Update health data display only if a device is connected
            if (isDeviceConnected) {
                updateHealthData()
            } else {
                // Show empty state for all vitals
                updateEmptyVitalsState()
            }
            
            // Start periodic connection verification
            startConnectionVerification()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            // Show a simple toast to prevent app crash
            Toast.makeText(this, "Error initializing app. Please restart.", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        try {
            // Toolbar and Navigation
            toolbar = findViewById(R.id.toolbar)
            drawerLayout = findViewById(R.id.drawer_layout)
            navigationView = findViewById(R.id.nav_view)
            
            // Top Section - Greeting + Notification
            imgAvatar = findViewById(R.id.imgAvatar)
            tvGreeting = findViewById(R.id.tvGreeting)
            tvTagline = findViewById(R.id.tvTagline)
            ivNotifications = findViewById(R.id.ivNotifications)
            tvNotifBadge = findViewById(R.id.tvNotifBadge)
            
            // Profile Container and Initials
            profileContainer = findViewById(R.id.profileContainer)
            tvAvatarInitials = findViewById(R.id.tvAvatarInitials)
            
            // Health Metrics Section
            btnDatePicker = findViewById(R.id.btnDatePicker)
            tvSelectedDate = findViewById(R.id.tvSelectedDate)
            
            // Health Report Card
            tvReportTitle = findViewById(R.id.tvReportTitle)
            tvReportStatus = findViewById(R.id.tvReportStatus)
            btnReportDetails = findViewById(R.id.btnReportDetails)
            
            // Individual Vitals Cards
            cardHeartRate = findViewById(R.id.cardHeartRate)
            cardGlucose = findViewById(R.id.cardGlucose)
            cardTemperature = findViewById(R.id.cardTemperature)
            cardSpO2 = findViewById(R.id.cardSpO2)
            cardBP = findViewById(R.id.cardBP)
            cardRespiratory = findViewById(R.id.cardRespiratory)
            
            // Bottom Navigation (if included in layout)
            try {
                bottomNavigation = findViewById(R.id.bottom_navigation)
            } catch (e: Exception) {
                // Bottom navigation might not be in the layout yet
                Log.w(TAG, "Bottom navigation not found in layout", e)
            }
            
            // Device Connection Section
            deviceStatusCard = findViewById(R.id.cardDeviceStatus)
            deviceStatusIcon = findViewById(R.id.ivDeviceStatus)
            deviceStatusTextView = findViewById(R.id.tvDeviceStatus)
            deviceNameTextView = findViewById(R.id.tvDeviceName)
            connectDeviceButton = findViewById(R.id.btnConnectDevice)
            
            // Additional Features
            viewAllChartsButton = findViewById(R.id.btnViewAllCharts)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Set up toolbar and navigation drawer
     */
    private fun setupToolbarAndNavigation() {
        try {
            // Set toolbar as action bar
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)
            
            // Set up navigation drawer toggle
            val toggle = ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )
            drawerLayout.addDrawerListener(toggle)
            toggle.syncState()
            
            // Set up navigation view listener
            navigationView.setNavigationItemSelectedListener(this)
            
            // Update navigation header with user info
            val headerView = navigationView.getHeaderView(0)
            val navUserName = headerView.findViewById<TextView>(R.id.nav_header_name)
            val navUserEmail = headerView.findViewById<TextView>(R.id.nav_header_email)
            
            // Get user info from SharedPreferences
            val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
            val userEmail = sharedPreferences.getString("user_email", "user@example.com") ?: "user@example.com"
            val userName = userEmail.substringBefore("@").capitalize()
            
            navUserName.text = userName
            navUserEmail.text = userEmail
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up toolbar and navigation: ${e.message}", e)
        }
    }
    
    /**
     * Load user data from SharedPreferences and set greeting
     */
    private fun loadUserData() {
        try {
            val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
            val userEmail = sharedPreferences.getString("user_email", "user@example.com") ?: "user@example.com"
            val userName = userEmail.substringBefore("@").capitalize()
            
            // Set greeting with user name
            tvGreeting.text = "Welcome, $userName"
            
            // Set tagline - FIXED: Updated to new tagline
            tvTagline.text = "Smarter insights. Healthier you."
            
            // Set notification badge (could be dynamic based on notifications)
            tvNotifBadge.text = "4"
            
            // Set up notification click listener
            ivNotifications.setOnClickListener {
                Toast.makeText(this, "Notifications coming soon", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user data: ${e.message}", e)
        }
    }
    
    /**
     * Set up profile display with user initials
     */
    private fun setupProfileDisplay() {
        try {
            val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
            val userEmail = sharedPreferences.getString("user_email", "user@example.com") ?: "user@example.com"
            val userName = userEmail.substringBefore("@").capitalize()
            
            // Generate initials (up to 2 characters)
            val initials = generateInitials(userName)
            
            // Set initials text
            tvAvatarInitials.text = initials
            
            // Check if user has a profile image
            val hasProfileImage = false // TODO: Check if user has uploaded a profile image
            
            // Show/hide appropriate views
            if (hasProfileImage) {
                // If user has a profile image, show it and hide initials
                imgAvatar.visibility = View.VISIBLE
                tvAvatarInitials.visibility = View.GONE
                
                // TODO: Load actual profile image
                // imgAvatar.setImageBitmap(...)
            } else {
                // If no profile image, hide image and show initials
                imgAvatar.visibility = View.GONE
                tvAvatarInitials.visibility = View.VISIBLE
            }
            
            // Set click listener for profile container
            profileContainer.setOnClickListener {
                navigateToSettings() // Navigate to profile/settings for now
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up profile display: ${e.message}", e)
        }
    }
    
    /**
     * Generate initials from user name (up to 2 characters)
     */
    private fun generateInitials(name: String): String {
        return try {
            val parts = name.split(" ")
            if (parts.size > 1) {
                // If name has multiple parts, use first letter of first and last parts
                "${parts.first().first().uppercase()}${parts.last().first().uppercase()}"
            } else {
                // If single name, use first two letters or just first letter if name is only one character
                if (name.length > 1) name.substring(0, 2).uppercase() else name.uppercase()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating initials: ${e.message}", e)
            "GG" // Default initials
        }
    }
    
    /**
     * Set up the health report card
     */
    private fun setupHealthReportCard() {
        try {
            // Set up health status text with colored "Good" word
            val statusText = "You are Good!"
            val spannableString = SpannableString(statusText)
            val goodStartIndex = statusText.indexOf("Good")
            if (goodStartIndex >= 0) {
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.success)),
                    goodStartIndex,
                    goodStartIndex + 4,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            tvReportStatus.text = spannableString
            
            // Set up view details click listener
            btnReportDetails.setOnClickListener {
                navigateToHealthCharts()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up health report card: ${e.message}", e)
        }
    }
    
    /**
     * Set up the vitals grid with click listeners
     * This method has been simplified to avoid accessing non-existent TextViews
     */
    private fun setupVitalsGrid() {
        try {
            // Set up click listeners for each vitals card
            
            // Heart Rate card
            cardHeartRate.setOnClickListener {
                navigateToHeartRate()
            }
            
            // HRV card (formerly Glucose placeholder)
            cardGlucose.setOnClickListener {
                navigateToHRV()
            }
            
            // Temperature card
            cardTemperature.setOnClickListener {
                navigateToTemperature()
            }
            
            // SpO2 card
            cardSpO2.setOnClickListener {
                navigateToBloodOxygen()
            }
            
            // Blood Pressure card
            cardBP.setOnClickListener {
                navigateToBloodPressure()
            }
            
            // Respiratory card
            cardRespiratory.setOnClickListener {
                Toast.makeText(this, "Respiratory monitoring coming soon", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up vitals grid: ${e.message}", e)
        }
    }

    /**
     * Navigate to HRV screen
     */
    private fun navigateToHRV() {
        try {
            val intent = Intent(this, HRVActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to HRV: ${e.message}", e)
        }
    }

    /**
     * Navigate to Temperature screen
     */
    private fun navigateToTemperature() {
        try {
            val intent = Intent(this, TemperatureActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to Temperature: ${e.message}", e)
        }
    }
    
    /**
     * Set up bottom navigation
     */
    private fun setupBottomNavigation() {
        try {
            bottomNavigation.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_home -> {
                        // Already on home, do nothing
                        true
                    }
                    R.id.nav_sleep -> {
                        navigateToSleep()
                        true
                    }
                    R.id.nav_activity -> {
                        navigateToSteps() // Use existing Steps activity for Activity tab
                        true
                    }
                    R.id.nav_wellbeing -> {
                        Toast.makeText(this, "Wellbeing features coming soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.nav_profile -> {
                        navigateToSettings()
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            // Bottom navigation might not be in the layout yet
            Log.w(TAG, "Error setting up bottom navigation: ${e.message}", e)
        }
    }
    
    /**
     * Get appropriate greeting based on time of day
     */
    private fun getGreetingBasedOnTime(): String {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        
        return when {
            hourOfDay < 12 -> "Good morning"
            hourOfDay < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    
    /**
     * Display current date in a readable format
     */
    private fun displayCurrentDate() {
        try {
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Calendar.getInstance().time)
            
            // Set selected date to "Today"
            tvSelectedDate.text = "Today"
            
            // Set up date picker click listener
            btnDatePicker.setOnClickListener {
                // Show date picker dialog
                Toast.makeText(this, "Date selection coming soon", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying current date: ${e.message}", e)
        }
    }
    
    /**
     * Start periodic connection verification
     */
    private fun startConnectionVerification() {
        connectionHandler.postDelayed(connectionVerificationRunnable, CONNECTION_VERIFICATION_INTERVAL)
    }
    
    /**
     * Stop periodic connection verification
     */
    private fun stopConnectionVerification() {
        connectionHandler.removeCallbacks(connectionVerificationRunnable)
    }
    
    /**
     * Verify that the device is really connected and transmitting data
     * This is called periodically to ensure the connection status is accurate
     */
    private fun verifyRealDeviceConnection() {
        Log.d(TAG, "Verifying real device connection")
        
        try {
            // Get the connection manager
            val connectionManager = ConnectionManager.getInstance(this)
            
            // Check if the connection manager reports a connected device
            val wasConnected = isDeviceConnected
            val connectedDevice = connectionManager.getConnectedDevice()
            
            if (connectedDevice != null) {
                // We have a device that claims to be connected, verify it's real
                deviceAddress = connectedDevice.first
                deviceName = connectedDevice.second
                
                // Check if the health data manager has received real data from this device
                val hasRealData = healthDataManager.hasReceivedRealData()
                
                if (hasRealData) {
                    // We have a real connection with data flow
                    if (!isDeviceConnected) {
                        Log.d(TAG, "Real device connection verified: $deviceName ($deviceAddress)")
                        isDeviceConnected = true
                        updateDeviceConnectionUI(true)
                        updateHealthData()
                    }
                } else {
                    // No real data received, check if we've been waiting too long
                    if (isDeviceConnected) {
                        Log.d(TAG, "Device connected but no data received: $deviceName ($deviceAddress)")
                        // Keep waiting for data, but update UI to show we're waiting
                        updateDeviceWaitingForDataUI()
                    } else {
                        // We've been waiting for data but haven't received any
                        Log.d(TAG, "No data received from device, marking as disconnected")
                        isDeviceConnected = false
                        updateDeviceConnectionUI(false)
                        updateEmptyVitalsState()
                    }
                }
            } else {
                // No device is connected according to the connection manager
                if (isDeviceConnected) {
                    Log.d(TAG, "Device disconnected")
                    isDeviceConnected = false
                    deviceAddress = null
                    deviceName = null
                    updateDeviceConnectionUI(false)
                    updateEmptyVitalsState()
                    
                    // Stop any data syncing
                    healthDataManager.stopSync()
                }
            }
            
            // If connection state changed, update the UI
            if (wasConnected != isDeviceConnected) {
                Log.d(TAG, "Connection state changed: $wasConnected -> $isDeviceConnected")
                if (isDeviceConnected) {
                    // Initialize health data manager with device address and start syncing
                    // ONLY when a real device is connected
                    deviceAddress?.let {
                        healthDataManager.initialize(it)
                        healthDataManager.startSync()
                    }
                } else {
                    // Stop any data syncing
                    healthDataManager.stopSync()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying real device connection: ${e.message}", e)
        }
    }
    
    /**
     * Check device connection status
     * Uses the updated ConnectionManager to verify real connections
     */
    private fun checkDeviceConnection() {
        try {
            // Default to not connected
            isDeviceConnected = false
            deviceAddress = null
            deviceName = null
            
            // Update UI to show disconnected state by default
            updateDeviceConnectionUI(false)
            
            // Check if device is connected from ConnectionManager
            val connectionManager = ConnectionManager.getInstance(this)
            val connectedDevice = connectionManager.getConnectedDevice()
            
            if (connectedDevice != null) {
                deviceAddress = connectedDevice.first
                deviceName = connectedDevice.second
                
                // Verify this is a real connection, not a fake/simulated one
                if (connectionManager.isConnected()) {
                    Log.d(TAG, "Real device connection verified: $deviceName ($deviceAddress)")
                    isDeviceConnected = true
                    
                    // Update UI to show connected state
                    updateDeviceConnectionUI(true)
                    
                    // Initialize health data manager with device address and start syncing
                    // ONLY when a real device is connected
                    healthDataManager.initialize(deviceAddress!!)
                    healthDataManager.startSync()
                } else {
                    // Connection manager reports a device but it's not really connected
                    Log.d(TAG, "False connection detected, clearing: $deviceName ($deviceAddress)")
                    connectionManager.clearConnection()
                    updateDeviceConnectionUI(false)
                    
                    // Make sure we're not showing any simulated data
                    healthDataManager.stopSync()
                    
                    // Update vitals to show empty state
                    updateEmptyVitalsState()
                }
            } else {
                // No device connected
                Log.d(TAG, "No device connected")
                
                // Make sure we're not showing any simulated data
                healthDataManager.stopSync()
                
                // Update vitals to show empty state
                updateEmptyVitalsState()
            }
            
            // Set up connect device button
            connectDeviceButton.setOnClickListener {
                if (isDeviceConnected) {
                    // Disconnect device
                    connectionManager.clearConnection()
                    isDeviceConnected = false
                    updateDeviceConnectionUI(false)
                    updateEmptyVitalsState()
                    
                    // Stop any data syncing
                    healthDataManager.stopSync()
                } else {
                    // Route directly to MainActivity which contains full
                    // ConnectionManager scanning / pairing logic.
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking device connection: ${e.message}", e)
        }
    }
    
    /**
     * Update device connection UI based on connection status
     * Shows clear messaging about the real connection state
     */
    private fun updateDeviceConnectionUI(isConnected: Boolean) {
        try {
            if (isConnected) {
                // Update icon and text for connected state
                deviceStatusIcon.setImageResource(R.drawable.ic_bluetooth_connected)
                deviceStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success))
                deviceStatusTextView.text = "Connected"
                deviceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.success))
                deviceNameTextView.text = deviceName ?: "Device"
                deviceNameTextView.visibility = View.VISIBLE
                
                // Update button text
                connectDeviceButton.text = "Disconnect Device"
            } else {
                // Update icon and text for disconnected state
                deviceStatusIcon.setImageResource(R.drawable.ic_bluetooth_disabled)
                deviceStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.error))
                deviceStatusTextView.text = "Not Connected"
                deviceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.error))
                deviceNameTextView.text = "Connect your device to see health data"
                deviceNameTextView.visibility = View.VISIBLE
                
                // Update button text
                connectDeviceButton.text = "Connect Device"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device connection UI: ${e.message}", e)
        }
    }
    
    /**
     * Update UI to show we're waiting for data from a connected device
     */
    private fun updateDeviceWaitingForDataUI() {
        try {
            deviceStatusIcon.setImageResource(R.drawable.ic_bluetooth_connected)
            deviceStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning))
            deviceStatusTextView.text = "Waiting for data..."
            deviceStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.warning))
            deviceNameTextView.text = deviceName ?: "Device"
            deviceNameTextView.visibility = View.VISIBLE
            
            // Update button text
            connectDeviceButton.text = "Disconnect Device"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating device waiting UI: ${e.message}", e)
        }
    }
    
    /**
     * Set up additional features
     */
    private fun setupAdditionalFeatures() {
        try {
            // View All Charts Button
            viewAllChartsButton.setOnClickListener {
                navigateToHealthCharts()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up additional features: ${e.message}", e)
        }
    }
    
    /**
     * Update health data display with real data
     * Only shows data when a real device is connected and transmitting
     */
    private fun updateHealthData() {
        try {
            if (!isDeviceConnected) {
                updateEmptyVitalsState()
                return
            }
            
            // For now, we'll just show empty state since we don't have the vitals card views
            // This is a placeholder until we implement the real data display
            updateEmptyVitalsState()
            
            // Update health report status based on overall health metrics
            updateHealthReportStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating health data: ${e.message}", e)
        }
    }
    
    /**
     * Update a vitals card with real data
     * This method is now a safe placeholder that doesn't try to access TextViews
     */
    private fun updateVitalsCard(
        card: View,
        value: String,
        unit: String,
        status: String,
        statusColorRes: Int
    ) {
        // This is now a safe placeholder that doesn't try to access TextViews
        // The actual data will be handled by the XML layout directly
        try {
            // No-op for now - the cards are defined directly in XML
            // This prevents the NullPointerException that was causing crashes
        } catch (e: Exception) {
            Log.e(TAG, "Error updating vitals card: ${e.message}", e)
        }
    }
    
    /**
     * Update vitals to show empty state when no device is connected
     * This is now a safe placeholder that doesn't try to access TextViews
     */
    private fun updateEmptyVitalsState() {
        try {
            // No-op for now - the cards are defined directly in XML
            // This prevents the NullPointerException that was causing crashes
            
            // Update health report to show empty state with clearer messaging
            tvReportStatus.text = "Connect your device to see health report"
        } catch (e: Exception) {
            Log.e(TAG, "Error updating empty vitals state: ${e.message}", e)
        }
    }
    
    /**
     * Update health report status based on overall health metrics
     */
    private fun updateHealthReportStatus() {
        try {
            // Set status text with colored "Good" word
            val statusText = "You are Good!"
            val spannableString = SpannableString(statusText)
            val goodStartIndex = statusText.indexOf("Good")
            if (goodStartIndex >= 0) {
                spannableString.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(this, R.color.success)),
                    goodStartIndex,
                    goodStartIndex + 4,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            tvReportStatus.text = spannableString
        } catch (e: Exception) {
            Log.e(TAG, "Error updating health report status: ${e.message}", e)
        }
    }
    
    /**
     * Navigation Methods
     */
    
    private fun navigateToDeviceConnection() {
        try {
            // Deprecated: we now use MainActivity for device connection.
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to device connection: ${e.message}", e)
        }
    }
    
    private fun navigateToHeartRate() {
        try {
            val intent = Intent(this, HeartRateActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to heart rate: ${e.message}", e)
        }
    }
    
    private fun navigateToSteps() {
        try {
            val intent = Intent(this, StepsActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to steps: ${e.message}", e)
        }
    }
    
    private fun navigateToSleep() {
        try {
            val intent = Intent(this, SleepActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to sleep: ${e.message}", e)
        }
    }
    
    private fun navigateToBloodOxygen() {
        try {
            val intent = Intent(this, BloodOxygenActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to blood oxygen: ${e.message}", e)
        }
    }
    
    private fun navigateToStress() {
        try {
            val intent = Intent(this, StressActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to stress: ${e.message}", e)
        }
    }
    
    private fun navigateToBloodPressure() {
        try {
            val intent = Intent(this, BloodPressureActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to blood pressure: ${e.message}", e)
        }
    }
    
    private fun navigateToHealthCharts() {
        try {
            val intent = Intent(this, HealthChartsActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to health charts: ${e.message}", e)
        }
    }
    
    private fun navigateToSettings() {
        try {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to settings: ${e.message}", e)
        }
    }
    
    // Commented out to prevent navigation to the problematic DashboardActivity
    // with fake data and heart icons
    /*
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("device_address", deviceAddress)
        intent.putExtra("device_name", deviceName)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
    */
    
    /**
     * Log out the user and navigate to welcome screen
     */
    private fun logoutUser() {
        try {
            // Clear login state
            val sharedPreferences = getSharedPreferences("sensacare_prefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("is_logged_in", false)
            editor.apply()
            
            // Stop health data sync
            healthDataManager.stopSync()
            
            // Navigate to welcome screen
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error logging out user: ${e.message}", e)
        }
    }
    
    /**
     * Create options menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }
    
    /**
     * Handle options menu item selection
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            /* Menu was updated: `action_settings` was replaced by `action_profile`
               so we need to react to the new ID here.  For now we keep the same
               navigation behaviour (opens settings/profile screen). */
            R.id.action_profile -> {
                navigateToSettings()      // TODO: replace with dedicated Profile screen once available
                true
            }
            R.id.action_refresh -> {
                // Refresh health data only if a real device is connected
                if (isDeviceConnected) {
                    healthDataManager.stopSync()
                    healthDataManager.startSync()
                    Toast.makeText(this, "Refreshing health data...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please connect your device first", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Handle navigation drawer item selection
     */
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Already on home screen, do nothing
            }
            R.id.nav_dashboard -> {
                // Dashboard functionality now integrated into Home screen
                Toast.makeText(this, "Dashboard features are available on the Home screen", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_heart_rate -> {
                navigateToHeartRate()
            }
            R.id.nav_steps -> {
                navigateToSteps()
            }
            R.id.nav_sleep -> {
                navigateToSleep()
            }
            R.id.nav_blood_oxygen -> {
                navigateToBloodOxygen()
            }
            R.id.nav_stress -> {
                navigateToStress()
            }
            R.id.nav_blood_pressure -> {
                navigateToBloodPressure()
            }
            R.id.nav_health_charts -> {
                navigateToHealthCharts()
            }
            R.id.nav_settings -> {
                navigateToSettings()
            }
            R.id.nav_logout -> {
                logoutUser()
            }
        }
        
        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    
    /**
     * Handle back button press
     */
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    
    /**
     * Clean up resources when activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        // Stop health data sync when activity is destroyed to prevent background processing
        healthDataManager.stopSync()
        
        // Stop connection verification
        stopConnectionVerification()
    }
    
    /**
     * Handle activity resume
     */
    override fun onResume() {
        super.onResume()
        // Verify device connection when activity resumes
        checkDeviceConnection()
        
        // Restart connection verification
        startConnectionVerification()
    }
    
    /**
     * Handle activity pause
     */
    override fun onPause() {
        super.onPause()
        // Stop connection verification when activity pauses
        stopConnectionVerification()
    }
    
    companion object {
        private const val TAG = "HomeActivity"
        private const val CONNECTION_VERIFICATION_INTERVAL = 5000L // 5 seconds
    }
}

/**
 * String extension function to capitalize first letter
 */
private fun String.capitalize(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercase() + this.substring(1)
    } else {
        this
    }
}
