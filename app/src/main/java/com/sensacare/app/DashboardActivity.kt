package com.sensacare.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sensacare.app.R
import com.sensacare.app.views.CircularProgressView
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.data.IHeartDataListener
import com.veepoo.protocol.listener.data.ISportDataListener
import com.veepoo.protocol.listener.data.ISleepDataListener
import com.veepoo.protocol.model.datas.HeartData
import com.veepoo.protocol.model.datas.SportData
import com.veepoo.protocol.model.datas.SleepData

class DashboardActivity : AppCompatActivity() {
    
    private lateinit var vpOperateManager: VPOperateManager
    
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
    private lateinit var fabAddSleep: FloatingActionButton
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        initializeViews()
        setupCircularProgressViews()
        setupNavigation()
        initializeVeepooSDK()
        setupDataListeners()
        loadHealthData()
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
        fabAddSleep = findViewById(R.id.fabAddSleep)
    }
    
    private fun setupCircularProgressViews() {
        // Activity Progress
        activityProgress.apply {
            setTitle("Activity")
            setProgressColor(ContextCompat.getColor(this@DashboardActivity, R.color.activity_green))
            setProgress(10f) // 10% progress
        }
        
        // Incrediness Progress
        incredinessProgress.apply {
            setTitle("Incrediness")
            setSubtitle("Attention")
            setProgressColor(ContextCompat.getColor(this@DashboardActivity, R.color.incrediness_orange))
            setProgress(15f) // 15% progress
        }
        
        // Sleep Progress
        sleepProgress.apply {
            setTitle("Sleep")
            setSubtitle("Excellent")
            setProgressColor(ContextCompat.getColor(this@DashboardActivity, R.color.sleep_purple))
            setProgress(80f) // 80% progress
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
                    // Navigate to incrediness screen
                    true
                }
                R.id.nav_sleep -> {
                    // Navigate to sleep screen
                    true
                }
                R.id.nav_settings -> {
                    // Navigate to settings screen
                    true
                }
                else -> false
            }
        }
        
        fabAddSleep.setOnClickListener {
            // Add sleep data
        }
    }
    
    private fun initializeVeepooSDK() {
        vpOperateManager = VPOperateManager.getMangerInstance(this)
    }
    
    private fun setupDataListeners() {
        // Heart rate data listener
        vpOperateManager.setHeartDataListener(object : IHeartDataListener {
            override fun onHeartDataChange(heartData: HeartData) {
                runOnUiThread {
                    updateHeartRateData(heartData)
                }
            }
        })
        
        // Sport data listener
        vpOperateManager.setSportDataListener(object : ISportDataListener {
            override fun onSportDataChange(sportData: SportData) {
                runOnUiThread {
                    updateSportData(sportData)
                }
            }
        })
        
        // Sleep data listener
        vpOperateManager.setSleepDataListener(object : ISleepDataListener {
            override fun onSleepDataChange(sleepData: SleepData) {
                runOnUiThread {
                    updateSleepData(sleepData)
                }
            }
        })
    }
    
    private fun loadHealthData() {
        // Request current health data from device
        vpOperateManager.readHeartData()
        vpOperateManager.readSportData()
        vpOperateManager.readSleepData()
        
        // Load sample data for demonstration
        loadSampleData()
    }
    
    private fun loadSampleData() {
        // Activity data
        tvSteps.text = "1000 / 10000 steps"
        tvCalories.text = "2 / 300 Cal"
        tvDistance.text = "10.0 / 5.0 km"
        
        // Sleep data
        tvSleepDuration.text = "7h 30m"
        tvSleepHeartRate.text = "73bpm"
        
        // Update progress
        activityProgress.setProgress(10f)
        incredinessProgress.setProgress(15f)
        sleepProgress.setProgress(80f)
    }
    
    private fun updateHeartRateData(heartData: HeartData) {
        // Update heart rate related data
        tvSleepHeartRate.text = "${heartData.heartRate}bpm"
    }
    
    private fun updateSportData(sportData: SportData) {
        // Update activity data
        val stepsProgress = (sportData.steps.toFloat() / 10000f) * 100f
        activityProgress.setProgress(stepsProgress)
        
        tvSteps.text = "${sportData.steps} / 10000 steps"
        tvCalories.text = "${sportData.calories} / 300 Cal"
        tvDistance.text = "${sportData.distance} / 5.0 km"
    }
    
    private fun updateSleepData(sleepData: SleepData) {
        // Update sleep data
        val sleepProgress = (sleepData.sleepHours.toFloat() / 8f) * 100f
        sleepProgress.setProgress(sleepProgress)
        
        tvSleepDuration.text = "${sleepData.sleepHours}h ${sleepData.sleepMinutes}m"
        
        // Update sleep quality subtitle
        val quality = when {
            sleepProgress >= 80 -> "Excellent"
            sleepProgress >= 60 -> "Good"
            sleepProgress >= 40 -> "Fair"
            else -> "Poor"
        }
        sleepProgress.setSubtitle(quality)
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when activity resumes
        loadHealthData()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up listeners if needed
    }
} 