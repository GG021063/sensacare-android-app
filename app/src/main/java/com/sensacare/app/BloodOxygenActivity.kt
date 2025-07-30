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
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import com.sensacare.app.data.BloodOxygenData
import com.sensacare.app.data.HealthDataManager
import com.sensacare.app.views.CircularProgressView
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * BloodOxygenActivity
 *
 * Comprehensive blood oxygen (SpO2) monitoring screen that displays:
 * - Current SpO2 level and health status
 * - Daily, weekly, and monthly SpO2 trends
 * - Health insights based on blood oxygen levels
 * - Manual measurement functionality
 */
class BloodOxygenActivity : AppCompatActivity() {

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
    private lateinit var dailySpO2Chart: LineChart
    private lateinit var weeklySpO2Chart: BarChart
    private lateinit var monthlySpO2Chart: BarChart
    
    // Text views
    private lateinit var tvSpO2Value: TextView
    private lateinit var tvSpO2Status: TextView
    private lateinit var tvLastMeasured: TextView
    private lateinit var tvHealthInsight: TextView
    private lateinit var tvMeasurementStatus: TextView
    
    // Progress view
    private lateinit var spO2ProgressView: CircularProgressView
    
    // SpO2 thresholds
    private val normalThreshold = 95
    private val lowThreshold = 90
    
    // Current SpO2 data
    private var currentSpO2Data: BloodOxygenData? = null
    
    // Date formatter
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    // Measurement in progress flag
    private var isMeasuring = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blood_oxygen)
        
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
        
        // Load SpO2 data
        loadSpO2Data()
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
        dailySpO2Chart = findViewById(R.id.chartDailySpO2)
        weeklySpO2Chart = findViewById(R.id.chartWeeklySpO2)
        monthlySpO2Chart = findViewById(R.id.chartMonthlySpO2)
        
        // Text views
        tvSpO2Value = findViewById(R.id.tvSpO2Value)
        tvSpO2Status = findViewById(R.id.tvSpO2Status)
        tvLastMeasured = findViewById(R.id.tvLastMeasured)
        tvHealthInsight = findViewById(R.id.tvHealthInsight)
        tvMeasurementStatus = findViewById(R.id.tvMeasurementStatus)
        
        // Progress view
        spO2ProgressView = findViewById(R.id.spO2ProgressView)
        spO2ProgressView.apply {
            setTitle("SpO2")
            setSubtitle("Loading...")
            setProgressColor(ContextCompat.getColor(this@BloodOxygenActivity, R.color.blood_oxygen_green))
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
            onBackPressed()
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
                startSpO2Measurement()
            } else {
                Toast.makeText(this, "Measurement already in progress", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Load SpO2 data from HealthDataManager
     */
    private fun loadSpO2Data() {
        // Observe latest blood oxygen data
        healthDataManager.bloodOxygen.observe(this, Observer { bloodOxygenData ->
            bloodOxygenData?.let {
                updateSpO2Display(it)
                currentSpO2Data = it
            }
        })
        
        // Load historical data for charts
        loadHistoricalSpO2Data()
    }
    
    /**
     * Update the SpO2 display with the latest data
     */
    private fun updateSpO2Display(bloodOxygenData: BloodOxygenData) {
        // Update SpO2 value
        tvSpO2Value.text = "${bloodOxygenData.spO2}%"
        
        // Calculate progress (0-100%)
        val progress = when {
            bloodOxygenData.spO2 >= normalThreshold -> 100f
            bloodOxygenData.spO2 >= lowThreshold -> 
                ((bloodOxygenData.spO2 - lowThreshold).toFloat() / (normalThreshold - lowThreshold)) * 100
            else -> 
                (bloodOxygenData.spO2.toFloat() / lowThreshold) * 50 // Below lowThreshold is critical
        }
        
        // Update progress view
        spO2ProgressView.setProgress(progress)
        
        // Update status based on SpO2 value
        when {
            bloodOxygenData.spO2 >= normalThreshold -> {
                tvSpO2Status.text = "Normal"
                tvSpO2Status.setTextColor(ContextCompat.getColor(this, R.color.success))
                spO2ProgressView.setSubtitle("Normal")
                tvHealthInsight.text = "Your blood oxygen level is normal. This indicates good respiratory function."
            }
            bloodOxygenData.spO2 >= lowThreshold -> {
                tvSpO2Status.text = "Low"
                tvSpO2Status.setTextColor(ContextCompat.getColor(this, R.color.warning))
                spO2ProgressView.setSubtitle("Low")
                tvHealthInsight.text = "Your blood oxygen level is below normal. Consider resting and monitoring your breathing."
            }
            else -> {
                tvSpO2Status.text = "Critical"
                tvSpO2Status.setTextColor(ContextCompat.getColor(this, R.color.error))
                spO2ProgressView.setSubtitle("Critical")
                tvHealthInsight.text = "Your blood oxygen level is critically low. Please seek medical attention if this persists."
            }
        }
        
        // Update last measured time
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = bloodOxygenData.timestamp
        tvLastMeasured.text = "Last measured: ${timeFormat.format(calendar.time)}"
        
        // Show alert for low oxygen levels
        if (bloodOxygenData.spO2 < lowThreshold) {
            showLowOxygenAlert()
        }
    }
    
    /**
     * Show alert for low oxygen levels
     */
    private fun showLowOxygenAlert() {
        // Only show alert once per session for the same reading
        val sharedPrefs = getSharedPreferences("spo2_prefs", MODE_PRIVATE)
        val lastAlertTimestamp = sharedPrefs.getLong("last_alert_timestamp", 0)
        val currentTimestamp = currentSpO2Data?.timestamp ?: 0
        
        if (lastAlertTimestamp != currentTimestamp) {
            Toast.makeText(
                this,
                "⚠️ Low blood oxygen detected. Please monitor your breathing.",
                Toast.LENGTH_LONG
            ).show()
            
            // Save alert timestamp
            sharedPrefs.edit().putLong("last_alert_timestamp", currentTimestamp).apply()
        }
    }
    
    /**
     * Load historical SpO2 data for charts
     */
    private fun loadHistoricalSpO2Data() {
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
        healthDataManager.getBloodOxygenData(dailyStartTime.timeInMillis, endTime)
            .observe(this, Observer { dailyData ->
                updateDailyChart(dailyData)
            })
        
        healthDataManager.getBloodOxygenData(weeklyStartTime.timeInMillis, endTime)
            .observe(this, Observer { weeklyData ->
                updateWeeklyChart(weeklyData)
            })
        
        healthDataManager.getBloodOxygenData(monthlyStartTime.timeInMillis, endTime)
            .observe(this, Observer { monthlyData ->
                updateMonthlyChart(monthlyData)
            })
    }
    
    /**
     * Update daily SpO2 chart with hourly breakdown
     */
    private fun updateDailyChart(bloodOxygenDataList: List<BloodOxygenData>) {
        if (bloodOxygenDataList.isEmpty()) {
            // If no data, show empty chart
            setupEmptyDailyChart()
            return
        }
        
        // Group data by hour
        val hourlyData = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Initialize all hours with 0 SpO2
        for (i in 0..23) {
            hourlyData[i] = 0
        }
        
        // Fill in actual data
        for (bloodOxygenData in bloodOxygenDataList) {
            calendar.timeInMillis = bloodOxygenData.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hourlyData[hour] == 0 || bloodOxygenData.spO2 > 0) {
                hourlyData[hour] = bloodOxygenData.spO2
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
        val dataSet = LineDataSet(entries, "SpO2 by Hour")
        dataSet.apply {
            color = ContextCompat.getColor(this@BloodOxygenActivity, R.color.blood_oxygen_green)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(this@BloodOxygenActivity, R.color.blood_oxygen_green))
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@BloodOxygenActivity, R.color.blood_oxygen_green)
            fillAlpha = 50
        }
        
        // Configure chart
        dailySpO2Chart.apply {
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
                axisMinimum = 80f // Start from 80% SpO2
                axisMaximum = 100f // Max is 100%
                
                // Add threshold lines
                val normalLine = LimitLine(normalThreshold.toFloat(), "Normal")
                normalLine.lineWidth = 1f
                normalLine.lineColor = ContextCompat.getColor(this@BloodOxygenActivity, R.color.success)
                normalLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                normalLine.textSize = 10f
                addLimitLine(normalLine)
                
                val lowLine = LimitLine(lowThreshold.toFloat(), "Low")
                lowLine.lineWidth = 1f
                lowLine.lineColor = ContextCompat.getColor(this@BloodOxygenActivity, R.color.warning)
                lowLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                lowLine.textSize = 10f
                addLimitLine(lowLine)
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
        dailySpO2Chart.apply {
            clear()
            setNoDataText("No SpO2 data available for today")
            setNoDataTextColor(Color.BLACK)
            invalidate()
        }
    }
    
    /**
     * Update weekly SpO2 chart with daily averages
     */
    private fun updateWeeklyChart(bloodOxygenDataList: List<BloodOxygenData>) {
        if (bloodOxygenDataList.isEmpty()) {
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
        for (bloodOxygenData in bloodOxygenDataList) {
            calendar.timeInMillis = bloodOxygenData.timestamp
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            if (dailyData.containsKey(dayOfYear) && bloodOxygenData.spO2 > 0) {
                dailyData[dayOfYear]?.add(bloodOxygenData.spO2)
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
        val dataSet = BarDataSet(entries, "Average SpO2 by Day")
        dataSet.apply {
            color = ContextCompat.getColor(this@BloodOxygenActivity, R.color.blood_oxygen_green)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            
            // Set colors based on SpO2 levels
            val colors = entries.map { entry ->
                when {
                    entry.y >= normalThreshold -> ContextCompat.getColor(this@BloodOxygenActivity, R.color.success)
                    entry.y >= lowThreshold -> ContextCompat.getColor(this@BloodOxygenActivity, R.color.warning)
                    entry.y > 0 -> ContextCompat.getColor(this@BloodOxygenActivity, R.color.error)
                    else -> Color.GRAY // No data
                }
            }
            setColors(colors)
        }
        
        // Configure chart
        weeklySpO2Chart.apply {
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
                axisMinimum = 80f // Start from 80% SpO2
                axisMaximum = 100f // Max is 100%
                
                // Add threshold lines
                val normalLine = LimitLine(normalThreshold.toFloat(), "Normal")
                normalLine.lineWidth = 1f
                normalLine.lineColor = ContextCompat.getColor(this@BloodOxygenActivity, R.color.success)
                normalLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                normalLine.textSize = 10f
                addLimitLine(normalLine)
                
                val lowLine = LimitLine(lowThreshold.toFloat(), "Low")
                lowLine.lineWidth = 1f
                lowLine.lineColor = ContextCompat.getColor(this@BloodOxygenActivity, R.color.warning)
                lowLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                lowLine.textSize = 10f
                addLimitLine(lowLine)
            }
            
            // Refresh chart
            invalidate()
        }
    }
    
    /**
     * Setup empty weekly chart when no data is available
     */
    private fun setupEmptyWeeklyChart() {
        weeklySpO2Chart.apply {
            clear()
            setNoDataText("No SpO2 data available for this week")
            setNoDataTextColor(Color.BLACK)
            invalidate()
        }
    }
    
    /**
     * Update monthly SpO2 chart with weekly averages
     */
    private fun updateMonthlyChart(bloodOxygenDataList: List<BloodOxygenData>) {
        if (bloodOxygenDataList.isEmpty()) {
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
        for (bloodOxygenData in bloodOxygenDataList) {
            calendar.timeInMillis = bloodOxygenData.timestamp
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            if (weeklyData.containsKey(weekOfYear) && bloodOxygenData.spO2 > 0) {
                weeklyData[weekOfYear]?.add(bloodOxygenData.spO2)
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
        val dataSet = BarDataSet(entries, "Average SpO2 by Week")
        dataSet.apply {
            color = ContextCompat.getColor(this@BloodOxygenActivity, R.color.blood_oxygen_green)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            
            // Set colors based on SpO2 levels
            val colors = entries.map { entry ->
                when {
                    entry.y >= normalThreshold -> ContextCompat.getColor(this@BloodOxygenActivity, R.color.success)
                    entry.y >= lowThreshold -> ContextCompat.getColor(this@BloodOxygenActivity, R.color.warning)
                    entry.y > 0 -> ContextCompat.getColor(this@BloodOxygenActivity, R.color.error)
                    else -> Color.GRAY // No data
                }
            }
            setColors(colors)
        }
        
        // Configure chart
        monthlySpO2Chart.apply {
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
                axisMinimum = 80f // Start from 80% SpO2
                axisMaximum = 100f // Max is 100%
                
                // Add threshold lines
                val normalLine = LimitLine(normalThreshold.toFloat(), "Normal")
                normalLine.lineWidth = 1f
                normalLine.lineColor = ContextCompat.getColor(this@BloodOxygenActivity, R.color.success)
                normalLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                normalLine.textSize = 10f
                addLimitLine(normalLine)
                
                val lowLine = LimitLine(lowThreshold.toFloat(), "Low")
                lowLine.lineWidth = 1f
                lowLine.lineColor = ContextCompat.getColor(this@BloodOxygenActivity, R.color.warning)
                lowLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                lowLine.textSize = 10f
                addLimitLine(lowLine)
            }
            
            // Refresh chart
            invalidate()
        }
    }
    
    /**
     * Setup empty monthly chart when no data is available
     */
    private fun setupEmptyMonthlyChart() {
        monthlySpO2Chart.apply {
            clear()
            setNoDataText("No SpO2 data available for this month")
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
        dailySpO2Chart.apply {
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
                axisMinimum = 80f // Start from 80% SpO2
                axisMaximum = 100f // Max is 100%
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading SpO2 data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Setup weekly chart with initial configuration
     */
    private fun setupWeeklyChart() {
        weeklySpO2Chart.apply {
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
                axisMinimum = 80f // Start from 80% SpO2
                axisMaximum = 100f // Max is 100%
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading SpO2 data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Setup monthly chart with initial configuration
     */
    private fun setupMonthlyChart() {
        monthlySpO2Chart.apply {
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
                axisMinimum = 80f // Start from 80% SpO2
                axisMaximum = 100f // Max is 100%
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading SpO2 data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Start SpO2 measurement
     */
    private fun startSpO2Measurement() {
        if (deviceAddress == null) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Set measuring flag
        isMeasuring = true
        
        // Update UI to show measurement in progress
        tvMeasurementStatus.text = "Measuring SpO2..."
        tvMeasurementStatus.visibility = View.VISIBLE
        measureButton.isEnabled = false
        
        // Simulate measurement with VeePoo SDK or use HealthDataManager
        simulateSpO2Measurement()
    }
    
    /**
     * Simulate SpO2 measurement (in real implementation, this would use VeePoo SDK)
     */
    private fun simulateSpO2Measurement() {
        lifecycleScope.launch {
            // Simulate measurement delay
            delay(3000)
            
            // Generate realistic SpO2 value (95-99% normal range)
            val spO2 = (90..99).random()
            
            // Create blood oxygen data
            val bloodOxygenData = BloodOxygenData(
                timestamp = System.currentTimeMillis(),
                spO2 = spO2,
                pulse = (60..100).random(), // Random pulse between 60-100 bpm
                deviceAddress = deviceAddress ?: ""
            )
            
            // Add to health data manager
            healthDataManager.addBloodOxygenData(bloodOxygenData)
            
            // Update UI
            updateSpO2Display(bloodOxygenData)
            
            // Reset measuring state
            isMeasuring = false
            tvMeasurementStatus.visibility = View.GONE
            measureButton.isEnabled = true
            
            // Show completion toast
            Toast.makeText(this@BloodOxygenActivity, "SpO2 measurement complete", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle back button press
     */
    override fun onBackPressed() {
        super.onBackPressed()
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
        private const val TAG = "BloodOxygenActivity"
    }
}
