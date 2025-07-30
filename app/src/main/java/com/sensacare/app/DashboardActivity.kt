package com.sensacare.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sensacare.app.data.*
import com.sensacare.app.views.CircularProgressView
import com.veepoo.protocol.VPOperateManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class DashboardActivity : AppCompatActivity() {
    
    private var vpOperateManager: VPOperateManager? = null
    private lateinit var healthDataManager: HealthDataManager
    
    // Connected device info
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    
    // Health data goals
    private val dailyStepsGoal = 10000
    private val dailyCaloriesGoal = 300f
    private val dailyDistanceGoal = 5.0f
    private val sleepGoalHours = 8
    
    // Circular Progress Views
    private lateinit var activityProgress: CircularProgressView
    private lateinit var incredinessProgress: CircularProgressView
    private lateinit var sleepProgress: CircularProgressView
    
    // Text Views
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepHeartRate: TextView
    
    // Navigation
    private lateinit var bottomNavigation: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        // Get device info from intent
        deviceAddress = intent.getStringExtra("device_address")
        deviceName = intent.getStringExtra("device_name")
        
        // **CRITICAL FIX**: Check ConnectionManager if no device info in intent
        if (deviceAddress == null) {
            val connectionManager = ConnectionManager.getInstance(this)
            val savedConnection = connectionManager.getConnectedDevice()
            if (savedConnection != null) {
                deviceAddress = savedConnection.first
                deviceName = savedConnection.second
                android.util.Log.i("DashboardActivity", "Restored device from saved state: $deviceName ($deviceAddress)")
            }
        }
        
        if (deviceAddress == null) {
            Toast.makeText(this, "No device address provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize health data manager
        healthDataManager = HealthDataManager.getInstance(applicationContext)
        healthDataManager.initialize(deviceAddress!!)
        
        initializeViews()
        setupCircularProgressViews()
        setupNavigation()
        initializeVeepooSDK()
        setupDataObservers()
    }
    
    private fun initializeViews() {
        // Circular Progress Views
        activityProgress = findViewById(R.id.activityProgress)
        incredinessProgress = findViewById(R.id.incredinessProgress)
        sleepProgress = findViewById(R.id.sleepProgress)
        
        // Text Views
        tvSteps = findViewById(R.id.tvSteps)
        tvCalories = findViewById(R.id.tvCalories)
        tvDistance = findViewById(R.id.tvDistance)
        tvSleepDuration = findViewById(R.id.tvSleepDuration)
        tvSleepHeartRate = findViewById(R.id.tvSleepHeartRate)
        
        // Navigation
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }
    
    private fun setupCircularProgressViews() {
        // Activity Progress
        activityProgress.apply {
            setTitle("Activity")
            setProgressColor(ContextCompat.getColor(this@DashboardActivity, R.color.activity_green))
            setProgress(0f) // Will be updated with real data
            
            // Add click listener to navigate to dedicated Steps/Activity screen
            setOnClickListener {
                navigateToStepsActivity()
            }
        }
        
        // Readiness Progress (Attention/Focus)
        incredinessProgress.apply {
            setTitle("Readiness")
            setSubtitle("Attention")
            setProgressColor(ContextCompat.getColor(this@DashboardActivity, R.color.sensacare_yellow))
            setProgress(0f) // Will be updated with real data
            
            // Add click listener to navigate to heart rate activity
            setOnClickListener {
                navigateToReadinessScreen()
            }
        }
        
        // Sleep Progress
        sleepProgress.apply {
            setTitle("Sleep")
            setSubtitle("Loading...")
            setProgressColor(ContextCompat.getColor(this@DashboardActivity, R.color.sleep_purple))
            setProgress(0f) // Will be updated with real data
            
            // Add click listener to navigate to sleep activity
            setOnClickListener {
                navigateToSleepScreen()
            }
        }
    }
    
    private fun setupNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already on home
                    true
                }
                R.id.nav_incrediness -> {
                    // Navigate to readiness screen
                    navigateToReadinessScreen()
                    true
                }
                R.id.nav_sleep -> {
                    // Navigate to sleep screen
                    navigateToSleepScreen()
                    true
                }
                R.id.nav_charts -> {
                    // Navigate to charts screen
                    navigateToCharts(null)
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to settings screen
                    val intent = Intent(this, SettingsActivity::class.java)
                    intent.putExtra("device_address", deviceAddress)
                    intent.putExtra("device_name", deviceName)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * Navigate to the HealthChartsActivity with an optional chart type to focus on
     * @param chartType The type of chart to focus on (activity, heart_rate, sleep, blood_pressure)
     */
    private fun navigateToCharts(chartType: String?) {
        val intent = Intent(this, HealthChartsActivity::class.java)
        intent.putExtra("device_address", deviceAddress)
        intent.putExtra("device_name", deviceName)
        
        // If a specific chart type is requested, add it to the intent
        chartType?.let {
            intent.putExtra("chart_type", it)
        }
        
        startActivity(intent)
    }
    
    /**
     * Navigate to the HeartRateActivity for readiness/heart rate monitoring
     */
    private fun navigateToReadinessScreen() {
        val intent = Intent(this, HeartRateActivity::class.java)
        intent.putExtra("device_address", deviceAddress)
        intent.putExtra("device_name", deviceName)
        startActivity(intent)
    }
    
    /**
     * Navigate to the SleepActivity for sleep monitoring
     */
    private fun navigateToSleepScreen() {
        val intent = Intent(this, SleepActivity::class.java)
        intent.putExtra("device_address", deviceAddress)
        intent.putExtra("device_name", deviceName)
        startActivity(intent)
    }
    
    /**
     * Navigate to the BloodPressureActivity for BP monitoring
     */
    private fun navigateToBloodPressureScreen() {
        val intent = Intent(this, BloodPressureActivity::class.java)
        intent.putExtra("device_address", deviceAddress)
        intent.putExtra("device_name", deviceName)
        startActivity(intent)
    }

    /**
     * Navigate to the StepsActivity for daily/weekly activity tracking
     */
    private fun navigateToStepsActivity() {
        val intent = Intent(this, StepsActivity::class.java)
        intent.putExtra("device_address", deviceAddress)
        intent.putExtra("device_name", deviceName)
        startActivity(intent)
    }
    
    private fun initializeVeepooSDK() {
        try {
            // Initialize the VeePoo SDK manager
            vpOperateManager = VPOperateManager.getInstance()
            android.util.Log.i("DashboardActivity", "VeePoo SDK initialized successfully")
        } catch (e: Exception) {
            // Log any initialization errors
            android.util.Log.e("DashboardActivity", "Error initializing VeePoo SDK: ${e.message}")
        }
    }
    
    private fun setupDataObservers() {
        // Observe sync status
        healthDataManager.isSyncing.observe(this, Observer { isSyncing ->
            if (isSyncing) {
                Toast.makeText(this, "Syncing data from ${deviceName ?: "device"}...", Toast.LENGTH_SHORT).show()
            }
        })
        
        // Observe heart rate data
        healthDataManager.heartRate.observe(this, Observer { heartRateData ->
            heartRateData?.let {
                updateHeartRateDisplay(it)
            }
        })
        
        // Observe steps data
        healthDataManager.steps.observe(this, Observer { stepsData ->
            stepsData?.let {
                updateStepsDisplay(it)
            }
        })
        
        // Observe sleep data
        healthDataManager.sleep.observe(this, Observer { sleepData ->
            sleepData?.let {
                updateSleepDisplay(it)
            }
        })
        
        // Observe blood pressure data
        healthDataManager.bloodPressure.observe(this, Observer { bpData ->
            bpData?.let {
                updateBloodPressureDisplay(it)
            }
        })
        
        // ────────────────────────────────────────────────────────────
        //  FAKE-DATA SIMULATION DISABLED
        //  ----------------------------------------------------------
        //  `startSync()` currently triggers the mock data generator
        //  inside HealthDataManager.  Until the real VeePoo SDK read/
        //  notify calls are fully integrated we do NOT start syncing
        //  from here to avoid displaying fake values to the user.
        // ────────────────────────────────────────────────────────────
        // healthDataManager.startSync()
    }
    
    private fun updateHeartRateDisplay(heartRateData: HeartRateData) {
        // Update heart rate text
        tvSleepHeartRate.text = "${heartRateData.value}bpm"
        
        // Temperature is now displayed statically in the layout
        // No need to update it dynamically
        
        // Update readiness progress based on heart rate (simplified algorithm)
        // Normal resting heart rate is 60-100 bpm, with 70-75 being optimal
        val heartRateOptimal = when {
            heartRateData.value in 65..85 -> 100 // Optimal range
            heartRateData.value in 60..64 || heartRateData.value in 86..90 -> 80 // Good
            heartRateData.value in 55..59 || heartRateData.value in 91..100 -> 60 // Acceptable
            heartRateData.value in 50..54 || heartRateData.value in 101..110 -> 40 // Concerning
            else -> 20 // Poor
        }
        
        incredinessProgress.setProgress(heartRateOptimal.toFloat())
        
        // Update subtitle based on heart rate quality
        when {
            heartRateOptimal >= 80 -> incredinessProgress.setSubtitle("Excellent")
            heartRateOptimal >= 60 -> incredinessProgress.setSubtitle("Good")
            heartRateOptimal >= 40 -> incredinessProgress.setSubtitle("Fair")
            else -> incredinessProgress.setSubtitle("Poor")
        }
    }
    
    private fun updateStepsDisplay(stepsData: StepsData) {
        // Update steps text (simplified format)
        tvSteps.text = "${stepsData.steps} / $dailyStepsGoal"
        
        // Update calories text
        tvCalories.text = "${stepsData.calories.roundToInt()} / $dailyCaloriesGoal Cal"
        
        // Update distance text
        tvDistance.text = "${String.format("%.1f", stepsData.distance)} / $dailyDistanceGoal km"
        
        // Update activity progress
        val stepsProgress = (stepsData.steps.toFloat() / dailyStepsGoal) * 100
        activityProgress.setProgress(stepsProgress.coerceAtMost(100f))
        
        // Update subtitle based on progress
        when {
            stepsProgress >= 75 -> activityProgress.setSubtitle("Excellent")
            stepsProgress >= 50 -> activityProgress.setSubtitle("Good")
            stepsProgress >= 25 -> activityProgress.setSubtitle("Fair")
            else -> activityProgress.setSubtitle("Getting Started")
        }
    }
    
    private fun updateSleepDisplay(sleepData: SleepData) {
        // Calculate hours and minutes
        val hours = sleepData.totalSleepMinutes / 60
        val minutes = sleepData.totalSleepMinutes % 60
        
        // Update sleep duration text
        tvSleepDuration.text = "${hours}h ${minutes}m"
        
        // Update sleep progress
        val sleepProgress = (sleepData.totalSleepMinutes.toFloat() / (sleepGoalHours * 60)) * 100
        this.sleepProgress.setProgress(sleepProgress.coerceAtMost(100f))
        
        // Update subtitle based on sleep quality score
        when (sleepData.sleepQualityScore) {
            in 80..100 -> this.sleepProgress.setSubtitle("Excellent")
            in 60..79 -> this.sleepProgress.setSubtitle("Good")
            in 40..59 -> this.sleepProgress.setSubtitle("Fair")
            else -> this.sleepProgress.setSubtitle("Poor")
        }
    }
    
    private fun updateBloodPressureDisplay(bpData: BloodPressureData) {
        // For now, we're not displaying blood pressure on the main dashboard
        // This could be added to a detailed health view
        android.util.Log.i("DashboardActivity", "Blood pressure updated: ${bpData.systolic}/${bpData.diastolic}")
    }
    
    override fun onResume() {
        super.onResume()
        // Do NOT restart sync here – prevents fake data from showing.
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
        healthDataManager.stopSync()
    }
}


