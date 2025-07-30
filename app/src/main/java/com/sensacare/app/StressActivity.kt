package com.sensacare.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import com.sensacare.app.data.HealthDataManager
import com.sensacare.app.data.StressData
import com.sensacare.app.views.CircularProgressView
import com.veepoo.protocol.VPOperateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * StressActivity
 *
 * Comprehensive stress monitoring screen that displays:
 * - Current stress level and category
 * - Daily, weekly, and monthly stress trends
 * - Stress insights and management techniques
 * - Manual measurement functionality
 */
class StressActivity : AppCompatActivity() {

    // Device info
    private var deviceAddress: String? = null
    private var deviceName: String? = null

    // VeePoo SDK
    private var vpOperateManager: VPOperateManager? = null

    // Health data manager
    private lateinit var healthDataManager: HealthDataManager

    // UI components
    private lateinit var backButton: ImageButton
    private lateinit var settingsButton: ImageButton
    private lateinit var tabLayout: TabLayout
    private lateinit var dailyChartCard: CardView
    private lateinit var weeklyChartCard: CardView
    private lateinit var monthlyChartCard: CardView
    private lateinit var measureButton: Button
    
    // Chart views
    private lateinit var dailyStressChart: LineChart
    private lateinit var weeklyStressChart: BarChart
    private lateinit var monthlyStressChart: BarChart
    
    // Text views
    private lateinit var tvStressLevel: TextView
    private lateinit var tvStressCategory: TextView
    private lateinit var tvLastMeasured: TextView
    private lateinit var tvHealthInsight: TextView
    private lateinit var tvMeasurementStatus: TextView
    
    // Progress view
    private lateinit var stressProgressView: CircularProgressView
    
    // Stress thresholds
    private val lowStressThreshold = 30
    private val moderateStressThreshold = 50
    private val highStressThreshold = 70
    
    // Current stress data
    private var currentStressData: StressData? = null
    
    // Date formatter
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    // Measurement in progress flag
    private var isMeasuring = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stress)
        
        // Get device info from intent
        deviceAddress = intent.getStringExtra("device_address")
        deviceName = intent.getStringExtra("device_name")
        
        // Check if device address is available
        if (deviceAddress == null) {
            val connectionManager = ConnectionManager.getInstance(this)
            val savedConnection = connectionManager.getConnectedDevice()
            if (savedConnection != null) {
                deviceAddress = savedConnection.first
                deviceName = savedConnection.second
            } else {
                Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
        }
        
        // Initialize health data manager
        healthDataManager = HealthDataManager.getInstance(applicationContext)
        
        // Initialize VeePoo SDK
        vpOperateManager = VPOperateManager.getInstance()
        
        // Initialize UI components
        initializeViews()
        setupTabLayout()
        setupCharts()
        setupListeners()
        
        // Load stress data
        loadStressData()
    }
    
    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        // Buttons
        backButton = findViewById(R.id.btnBack)
        settingsButton = findViewById(R.id.btnSettings)
        measureButton = findViewById(R.id.btnMeasure)
        
        // Tab layout
        tabLayout = findViewById(R.id.tabLayout)
        
        // Chart cards
        dailyChartCard = findViewById(R.id.cardDailyChart)
        weeklyChartCard = findViewById(R.id.cardWeeklyChart)
        monthlyChartCard = findViewById(R.id.cardMonthlyChart)
        
        // Charts
        dailyStressChart = findViewById(R.id.chartDailyStress)
        weeklyStressChart = findViewById(R.id.chartWeeklyStress)
        monthlyStressChart = findViewById(R.id.chartMonthlyStress)
        
        // Text views
        tvStressLevel = findViewById(R.id.tvStressLevel)
        tvStressCategory = findViewById(R.id.tvStressCategory)
        tvLastMeasured = findViewById(R.id.tvLastMeasured)
        tvHealthInsight = findViewById(R.id.tvHealthInsight)
        tvMeasurementStatus = findViewById(R.id.tvMeasurementStatus)
        
        // Progress view
        stressProgressView = findViewById(R.id.stressProgressView)
        stressProgressView.apply {
            setTitle("Stress")
            setSubtitle("Loading...")
            setProgressColor(ContextCompat.getColor(this@StressActivity, R.color.stress_green))
            setProgress(0f)
        }
    }
    
    /**
     * Setup tab layout for daily, weekly, monthly views
     */
    private fun setupTabLayout() {
        tabLayout.addTab(tabLayout.newTab().setText("Daily"))
        tabLayout.addTab(tabLayout.newTab().setText("Weekly"))
        tabLayout.addTab(tabLayout.newTab().setText("Monthly"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateChartVisibility(tab.position)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Not needed
            }
            
            override fun onTabReselected(tab: TabLayout.Tab) {
                // Not needed
            }
        })
        
        // Default to daily view
        updateChartVisibility(0)
    }
    
    /**
     * Update chart visibility based on selected tab
     */
    private fun updateChartVisibility(tabPosition: Int) {
        when (tabPosition) {
            0 -> { // Daily
                dailyChartCard.visibility = View.VISIBLE
                weeklyChartCard.visibility = View.GONE
                monthlyChartCard.visibility = View.GONE
            }
            1 -> { // Weekly
                dailyChartCard.visibility = View.GONE
                weeklyChartCard.visibility = View.VISIBLE
                monthlyChartCard.visibility = View.GONE
            }
            2 -> { // Monthly
                dailyChartCard.visibility = View.GONE
                weeklyChartCard.visibility = View.GONE
                monthlyChartCard.visibility = View.VISIBLE
            }
        }
    }
    
    /**
     * Setup click listeners for buttons
     */
    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }
        
        // Settings button
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("device_address", deviceAddress)
            intent.putExtra("device_name", deviceName)
            startActivity(intent)
        }
        
        // Measure button
        measureButton.setOnClickListener {
            if (!isMeasuring) {
                startStressMeasurement()
            } else {
                Toast.makeText(this, "Measurement already in progress", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Load stress data from HealthDataManager
     */
    private fun loadStressData() {
        // Observe latest stress data
        healthDataManager.stress.observe(this, Observer { stressData ->
            stressData?.let {
                updateStressDisplay(it)
                currentStressData = it
            }
        })
        
        // Load historical data for charts
        loadHistoricalStressData()
    }
    
    /**
     * Update the stress display with the latest data
     */
    private fun updateStressDisplay(stressData: StressData) {
        // Update stress level
        tvStressLevel.text = "${stressData.stressLevel}"
        
        // Calculate progress (0-100%)
        val progress = stressData.stressLevel.toFloat()
        
        // Update progress view
        stressProgressView.setProgress(progress)
        
        // Update status based on stress level
        when {
            stressData.stressLevel < lowStressThreshold -> {
                tvStressCategory.text = "Low"
                tvStressCategory.setTextColor(ContextCompat.getColor(this, R.color.success))
                stressProgressView.setSubtitle("Low")
                tvHealthInsight.text = "Your stress level is low. Keep up the good work with your stress management techniques."
            }
            stressData.stressLevel < moderateStressThreshold -> {
                tvStressCategory.text = "Moderate"
                tvStressCategory.setTextColor(ContextCompat.getColor(this, R.color.info))
                stressProgressView.setSubtitle("Moderate")
                tvHealthInsight.text = "Your stress level is moderate. Consider practicing mindfulness or deep breathing exercises."
            }
            stressData.stressLevel < highStressThreshold -> {
                tvStressCategory.text = "High"
                tvStressCategory.setTextColor(ContextCompat.getColor(this, R.color.warning))
                stressProgressView.setSubtitle("High")
                tvHealthInsight.text = "Your stress level is high. Try to take breaks and engage in stress-reducing activities."
            }
            else -> {
                tvStressCategory.text = "Very High"
                tvStressCategory.setTextColor(ContextCompat.getColor(this, R.color.error))
                stressProgressView.setSubtitle("Very High")
                tvHealthInsight.text = "Your stress level is very high. Consider speaking with a healthcare professional about stress management strategies."
            }
        }
        
        // Update last measured time
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = stressData.timestamp
        tvLastMeasured.text = "Last measured: ${timeFormat.format(calendar.time)}"
        
        // Show alert for high stress levels
        if (stressData.stressLevel >= highStressThreshold) {
            showHighStressAlert()
        }
    }
    
    /**
     * Show alert for high stress levels
     */
    private fun showHighStressAlert() {
        // Only show alert once per session for the same reading
        val sharedPrefs = getSharedPreferences("stress_prefs", MODE_PRIVATE)
        val lastAlertTimestamp = sharedPrefs.getLong("last_alert_timestamp", 0)
        val currentTimestamp = currentStressData?.timestamp ?: 0
        
        if (lastAlertTimestamp != currentTimestamp) {
            Toast.makeText(
                this,
                "⚠️ High stress detected. Consider taking a break and practicing deep breathing.",
                Toast.LENGTH_LONG
            ).show()
            
            // Save alert timestamp
            sharedPrefs.edit().putLong("last_alert_timestamp", currentTimestamp).apply()
        }
    }
    
    /**
     * Load historical stress data for charts
     */
    private fun loadHistoricalStressData() {
        // Get calendar instances for time ranges
        val endTime = Calendar.getInstance().timeInMillis
        
        // Daily chart - last 24 hours
        val dailyStartTime = Calendar.getInstance()
        dailyStartTime.add(Calendar.HOUR_OF_DAY, -24)
        
        // Weekly chart - last 7 days
        val weeklyStartTime = Calendar.getInstance()
        weeklyStartTime.add(Calendar.DAY_OF_YEAR, -7)
        
        // Monthly chart - last 30 days
        val monthlyStartTime = Calendar.getInstance()
        monthlyStartTime.add(Calendar.DAY_OF_YEAR, -30)
        
        // Load data for each time range
        healthDataManager.getStressData(dailyStartTime.timeInMillis, endTime)
            .observe(this, Observer { dailyData ->
                updateDailyChart(dailyData)
            })
        
        healthDataManager.getStressData(weeklyStartTime.timeInMillis, endTime)
            .observe(this, Observer { weeklyData ->
                updateWeeklyChart(weeklyData)
            })
        
        healthDataManager.getStressData(monthlyStartTime.timeInMillis, endTime)
            .observe(this, Observer { monthlyData ->
                updateMonthlyChart(monthlyData)
            })
    }
    
    /**
     * Update daily stress chart with hourly breakdown
     */
    private fun updateDailyChart(stressDataList: List<StressData>) {
        if (stressDataList.isEmpty()) {
            // If no data, show empty chart
            setupEmptyDailyChart()
            return
        }
        
        // Group data by hour
        val hourlyData = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Initialize all hours with 0 stress
        for (i in 0..23) {
            hourlyData[i] = 0
        }
        
        // Fill in actual data
        for (stressData in stressDataList) {
            calendar.timeInMillis = stressData.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hourlyData[hour] == 0 || stressData.stressLevel > 0) {
                hourlyData[hour] = stressData.stressLevel
            }
        }
        
        // Create entries for line chart
        val entries = ArrayList<Entry>()
        val hourLabels = ArrayList<String>()
        
        for (i in 0..23) {
            if (hourlyData[i] != null && hourlyData[i]!! > 0) {
                entries.add(Entry(i.toFloat(), hourlyData[i]!!.toFloat()))
            } else {
                // Add null entry for hours with no data to create gaps in the line
                entries.add(Entry(i.toFloat(), Float.NaN))
            }
            hourLabels.add("${i}:00")
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Stress by Hour")
        dataSet.apply {
            color = ContextCompat.getColor(this@StressActivity, R.color.stress_green)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(this@StressActivity, R.color.stress_green))
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@StressActivity, R.color.stress_green)
            fillAlpha = 50
        }
        
        // Configure chart
        dailyStressChart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 4f
                valueFormatter = IndexAxisValueFormatter(hourLabels)
                labelCount = 6
            }
            
            // Y-axis configuration
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                
                // Add threshold lines
                val lowLine = LimitLine(lowStressThreshold.toFloat(), "Low")
                lowLine.lineWidth = 1f
                lowLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.success)
                lowLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                lowLine.textSize = 10f
                addLimitLine(lowLine)
                
                val moderateLine = LimitLine(moderateStressThreshold.toFloat(), "Moderate")
                moderateLine.lineWidth = 1f
                moderateLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.info)
                moderateLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                moderateLine.textSize = 10f
                addLimitLine(moderateLine)
                
                val highLine = LimitLine(highStressThreshold.toFloat(), "High")
                highLine.lineWidth = 1f
                highLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.warning)
                highLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                highLine.textSize = 10f
                addLimitLine(highLine)
            }
            
            // Highlight current hour
            highlightValue(currentHour.toFloat(), 0)
            
            // Refresh chart
            invalidate()
        }
    }
    
    /**
     * Setup empty daily chart when no data is available
     */
    private fun setupEmptyDailyChart() {
        dailyStressChart.apply {
            clear()
            setNoDataText("No stress data available for today")
            setNoDataTextColor(Color.BLACK)
            invalidate()
        }
    }
    
    /**
     * Update weekly stress chart with daily averages
     */
    private fun updateWeeklyChart(stressDataList: List<StressData>) {
        if (stressDataList.isEmpty()) {
            // If no data, show empty chart
            setupEmptyWeeklyChart()
            return
        }
        
        // Group data by day of week
        val dailyData = mutableMapOf<Int, MutableList<Int>>()
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayLabels = ArrayList<String>()
        
        // Initialize all days with empty lists
        for (i in 0..6) {
            val day = calendar.clone() as Calendar
            day.add(Calendar.DAY_OF_YEAR, -6 + i)
            dailyData[day.get(Calendar.DAY_OF_YEAR)] = mutableListOf()
            dayLabels.add(dayFormat.format(day.time))
        }
        
        // Fill in actual data
        for (stressData in stressDataList) {
            calendar.timeInMillis = stressData.timestamp
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            if (dailyData.containsKey(dayOfYear) && stressData.stressLevel > 0) {
                dailyData[dayOfYear]?.add(stressData.stressLevel)
            }
        }
        
        // Calculate daily averages
        val dailyAverages = mutableMapOf<Int, Float>()
        for ((day, values) in dailyData) {
            if (values.isNotEmpty()) {
                dailyAverages[day] = values.average().toFloat()
            } else {
                dailyAverages[day] = 0f
            }
        }
        
        // Create entries for bar chart
        val entries = ArrayList<BarEntry>()
        val days = dailyData.keys.sorted()
        
        for (i in 0 until days.size) {
            val average = dailyAverages[days[i]] ?: 0f
            if (average > 0) {
                entries.add(BarEntry(i.toFloat(), average))
            } else {
                // Add zero entry for days with no data
                entries.add(BarEntry(i.toFloat(), 0f))
            }
        }
        
        // Create dataset
        val dataSet = BarDataSet(entries, "Average Stress by Day")
        dataSet.apply {
            color = ContextCompat.getColor(this@StressActivity, R.color.stress_green)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            
            // Set colors based on stress levels
            val colors = entries.map { entry ->
                when {
                    entry.y >= highStressThreshold -> ContextCompat.getColor(this@StressActivity, R.color.error)
                    entry.y >= moderateStressThreshold -> ContextCompat.getColor(this@StressActivity, R.color.warning)
                    entry.y >= lowStressThreshold -> ContextCompat.getColor(this@StressActivity, R.color.info)
                    entry.y > 0 -> ContextCompat.getColor(this@StressActivity, R.color.success)
                    else -> Color.GRAY // No data
                }
            }
            setColors(colors)
        }
        
        // Configure chart
        weeklyStressChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(dayLabels)
                labelCount = 7
            }
            
            // Y-axis configuration
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                
                // Add threshold lines
                val lowLine = LimitLine(lowStressThreshold.toFloat(), "Low")
                lowLine.lineWidth = 1f
                lowLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.success)
                lowLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                lowLine.textSize = 10f
                addLimitLine(lowLine)
                
                val moderateLine = LimitLine(moderateStressThreshold.toFloat(), "Moderate")
                moderateLine.lineWidth = 1f
                moderateLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.info)
                moderateLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                moderateLine.textSize = 10f
                addLimitLine(moderateLine)
                
                val highLine = LimitLine(highStressThreshold.toFloat(), "High")
                highLine.lineWidth = 1f
                highLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.warning)
                highLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                highLine.textSize = 10f
                addLimitLine(highLine)
            }
            
            // Refresh chart
            invalidate()
        }
    }
    
    /**
     * Setup empty weekly chart when no data is available
     */
    private fun setupEmptyWeeklyChart() {
        weeklyStressChart.apply {
            clear()
            setNoDataText("No stress data available for this week")
            setNoDataTextColor(Color.BLACK)
            invalidate()
        }
    }
    
    /**
     * Update monthly stress chart with weekly averages
     */
    private fun updateMonthlyChart(stressDataList: List<StressData>) {
        if (stressDataList.isEmpty()) {
            // If no data, show empty chart
            setupEmptyMonthlyChart()
            return
        }
        
        // Group data by week
        val weeklyData = mutableMapOf<Int, MutableList<Int>>()
        val calendar = Calendar.getInstance()
        val weekLabels = ArrayList<String>()
        
        // Initialize weeks with empty lists
        for (i in 0..4) {
            val week = calendar.clone() as Calendar
            week.add(Calendar.WEEK_OF_YEAR, -4 + i)
            val weekOfYear = week.get(Calendar.WEEK_OF_YEAR)
            weeklyData[weekOfYear] = mutableListOf()
            weekLabels.add("W${weekOfYear}")
        }
        
        // Fill in actual data
        for (stressData in stressDataList) {
            calendar.timeInMillis = stressData.timestamp
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            if (weeklyData.containsKey(weekOfYear) && stressData.stressLevel > 0) {
                weeklyData[weekOfYear]?.add(stressData.stressLevel)
            }
        }
        
        // Calculate weekly averages
        val weeklyAverages = mutableMapOf<Int, Float>()
        for ((week, values) in weeklyData) {
            if (values.isNotEmpty()) {
                weeklyAverages[week] = values.average().toFloat()
            } else {
                weeklyAverages[week] = 0f
            }
        }
        
        // Create entries for bar chart
        val entries = ArrayList<BarEntry>()
        val weeks = weeklyData.keys.sorted()
        
        for (i in 0 until weeks.size) {
            val average = weeklyAverages[weeks[i]] ?: 0f
            if (average > 0) {
                entries.add(BarEntry(i.toFloat(), average))
            } else {
                // Add zero entry for weeks with no data
                entries.add(BarEntry(i.toFloat(), 0f))
            }
        }
        
        // Create dataset
        val dataSet = BarDataSet(entries, "Average Stress by Week")
        dataSet.apply {
            color = ContextCompat.getColor(this@StressActivity, R.color.stress_green)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            
            // Set colors based on stress levels
            val colors = entries.map { entry ->
                when {
                    entry.y >= highStressThreshold -> ContextCompat.getColor(this@StressActivity, R.color.error)
                    entry.y >= moderateStressThreshold -> ContextCompat.getColor(this@StressActivity, R.color.warning)
                    entry.y >= lowStressThreshold -> ContextCompat.getColor(this@StressActivity, R.color.info)
                    entry.y > 0 -> ContextCompat.getColor(this@StressActivity, R.color.success)
                    else -> Color.GRAY // No data
                }
            }
            setColors(colors)
        }
        
        // Configure chart
        monthlyStressChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(weekLabels)
                labelCount = 5
            }
            
            // Y-axis configuration
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                
                // Add threshold lines
                val lowLine = LimitLine(lowStressThreshold.toFloat(), "Low")
                lowLine.lineWidth = 1f
                lowLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.success)
                lowLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                lowLine.textSize = 10f
                addLimitLine(lowLine)
                
                val moderateLine = LimitLine(moderateStressThreshold.toFloat(), "Moderate")
                moderateLine.lineWidth = 1f
                moderateLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.info)
                moderateLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                moderateLine.textSize = 10f
                addLimitLine(moderateLine)
                
                val highLine = LimitLine(highStressThreshold.toFloat(), "High")
                highLine.lineWidth = 1f
                highLine.lineColor = ContextCompat.getColor(this@StressActivity, R.color.warning)
                highLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                highLine.textSize = 10f
                addLimitLine(highLine)
            }
            
            // Refresh chart
            invalidate()
        }
    }
    
    /**
     * Setup empty monthly chart when no data is available
     */
    private fun setupEmptyMonthlyChart() {
        monthlyStressChart.apply {
            clear()
            setNoDataText("No stress data available for this month")
            setNoDataTextColor(Color.BLACK)
            invalidate()
        }
    }
    
    /**
     * Setup all charts with initial configuration
     */
    private fun setupCharts() {
        // Daily chart setup
        setupDailyChart()
        
        // Weekly chart setup
        setupWeeklyChart()
        
        // Monthly chart setup
        setupMonthlyChart()
    }
    
    /**
     * Setup daily chart with initial configuration
     */
    private fun setupDailyChart() {
        dailyStressChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            
            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 4f
                labelCount = 6
            }
            
            // Y-axis configuration
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading stress data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Setup weekly chart with initial configuration
     */
    private fun setupWeeklyChart() {
        weeklyStressChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelCount = 7
            }
            
            // Y-axis configuration
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading stress data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Setup monthly chart with initial configuration
     */
    private fun setupMonthlyChart() {
        monthlyStressChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // X-axis configuration
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelCount = 5
            }
            
            // Y-axis configuration
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading stress data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Start stress measurement
     */
    private fun startStressMeasurement() {
        if (deviceAddress == null) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Set measuring flag
        isMeasuring = true
        
        // Update UI to show measurement in progress
        tvMeasurementStatus.text = "Measuring stress level..."
        tvMeasurementStatus.visibility = View.VISIBLE
        measureButton.isEnabled = false
        
        // Simulate measurement with VeePoo SDK or use HealthDataManager
        simulateStressMeasurement()
    }
    
    /**
     * Simulate stress measurement (in real implementation, this would use VeePoo SDK)
     */
    private fun simulateStressMeasurement() {
        lifecycleScope.launch {
            // Simulate measurement delay
            delay(5000)
            
            // Generate realistic stress value (0-100 scale)
            val stressLevel = Random.nextInt(10, 90)
            
            // Create stress data
            val stressData = StressData(
                timestamp = System.currentTimeMillis(),
                stressLevel = stressLevel,
                heartRateVariability = Random.nextInt(20, 80), // HRV in ms
                deviceAddress = deviceAddress ?: ""
            )
            
            // Add to health data manager
            healthDataManager.addStressData(stressData)
            
            // Update UI
            updateStressDisplay(stressData)
            
            // Reset measuring state
            isMeasuring = false
            tvMeasurementStatus.visibility = View.GONE
            measureButton.isEnabled = true
            
            // Show completion toast
            Toast.makeText(this@StressActivity, "Stress measurement complete", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Clean up resources when activity is destroyed
     */
    override fun onDestroy() {
        super.onDestroy()
        // Cancel any ongoing measurements
        if (isMeasuring) {
            isMeasuring = false
        }
    }
    
    companion object {
        private const val TAG = "StressActivity"
    }
}
