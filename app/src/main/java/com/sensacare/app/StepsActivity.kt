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
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
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
import com.sensacare.app.data.HealthDataManager
import com.sensacare.app.data.StepsData
import androidx.lifecycle.lifecycleScope
import com.sensacare.app.views.CircularProgressView
import com.veepoo.protocol.VPOperateManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * StepsActivity
 *
 * Comprehensive activity tracking screen that displays:
 * - Current step count and goal progress
 * - Calories burned and distance traveled
 * - Daily, weekly, and monthly step charts
 * - Activity trends and insights
 */
class StepsActivity : AppCompatActivity() {

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
    private lateinit var dailyStepsChart: LineChart
    private lateinit var weeklyStepsChart: BarChart
    private lateinit var monthlyStepsChart: BarChart
    
    // Text views
    private lateinit var tvStepsCount: TextView
    private lateinit var tvStepsGoal: TextView
    private lateinit var tvCaloriesBurned: TextView
    private lateinit var tvDistanceTraveled: TextView
    private lateinit var tvActivityStatus: TextView
    private lateinit var tvLastUpdated: TextView
    
    // Progress view
    private lateinit var stepsProgressView: CircularProgressView
    
    // Steps goal (default 10,000, can be customized)
    private var dailyStepsGoal = 10000
    
    // Current steps data
    private var currentStepsData: StepsData? = null
    
    // Date formatter
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps)
        
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
        
        // Load steps data
        loadStepsData()
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
        dailyStepsChart = findViewById(R.id.chartDailySteps)
        weeklyStepsChart = findViewById(R.id.chartWeeklySteps)
        monthlyStepsChart = findViewById(R.id.chartMonthlySteps)
        
        // Text views
        tvStepsCount = findViewById(R.id.tvStepsCount)
        tvStepsGoal = findViewById(R.id.tvStepsGoal)
        tvCaloriesBurned = findViewById(R.id.tvCaloriesBurned)
        tvDistanceTraveled = findViewById(R.id.tvDistanceTraveled)
        tvActivityStatus = findViewById(R.id.tvActivityStatus)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        
        // Progress view
        stepsProgressView = findViewById(R.id.stepsProgressView)
        stepsProgressView.apply {
            setTitle("Steps")
            setSubtitle("Today")
            setProgressColor(ContextCompat.getColor(this@StepsActivity, R.color.steps_turquoise))
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
            syncStepsData()
        }
    }
    
    /**
     * Load steps data from HealthDataManager
     */
    private fun loadStepsData() {
        // Observe latest steps data
        healthDataManager.steps.observe(this, Observer { stepsData ->
            stepsData?.let {
                updateStepsDisplay(it)
                currentStepsData = it
            }
        })
        
        // Load historical data for charts
        loadHistoricalStepsData()
    }
    
    /**
     * Update the steps display with the latest data
     */
    private fun updateStepsDisplay(stepsData: StepsData) {
        // Update steps count
        tvStepsCount.text = stepsData.steps.toString()
        tvStepsGoal.text = "/$dailyStepsGoal"
        
        // Update calories
        tvCaloriesBurned.text = "${stepsData.calories.roundToInt()} Cal"
        
        // Update distance
        tvDistanceTraveled.text = String.format("%.2f km", stepsData.distance)
        
        // Update progress view
        val progress = (stepsData.steps.toFloat() / dailyStepsGoal) * 100
        stepsProgressView.setProgress(min(progress, 100f))
        
        // Update activity status
        tvActivityStatus.text = when {
            stepsData.steps >= dailyStepsGoal -> "Goal achieved! Great job!"
            stepsData.steps >= dailyStepsGoal * 0.75 -> "Almost there! Keep going!"
            stepsData.steps >= dailyStepsGoal * 0.5 -> "Good progress today!"
            stepsData.steps >= dailyStepsGoal * 0.25 -> "You're on your way!"
            else -> "Start moving to reach your goal!"
        }
        
        // Update last updated time
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = stepsData.timestamp
        tvLastUpdated.text = "Last updated: ${timeFormat.format(calendar.time)}"
        
        // Check for goal achievement
        if (stepsData.steps >= dailyStepsGoal) {
            celebrateGoalAchievement()
        }
    }
    
    /**
     * Celebrate when user achieves their daily step goal
     */
    private fun celebrateGoalAchievement() {
        // Only celebrate once per goal achievement
        val sharedPrefs = getSharedPreferences("steps_prefs", MODE_PRIVATE)
        val lastCelebrationDate = sharedPrefs.getString("last_celebration_date", "")
        val today = dateFormat.format(Date())
        
        if (lastCelebrationDate != today) {
            Toast.makeText(
                this,
                "🎉 Congratulations! You've reached your daily step goal! 🎉",
                Toast.LENGTH_LONG
            ).show()
            
            // Save celebration date
            sharedPrefs.edit().putString("last_celebration_date", today).apply()
        }
    }
    
    /**
     * Load historical steps data for charts
     */
    private fun loadHistoricalStepsData() {
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
        healthDataManager.getStepsData(dailyStartTime.timeInMillis, endTime)
            .observe(this, Observer { dailyData ->
                updateDailyChart(dailyData)
            })
        
        healthDataManager.getStepsData(weeklyStartTime.timeInMillis, endTime)
            .observe(this, Observer { weeklyData ->
                updateWeeklyChart(weeklyData)
            })
        
        healthDataManager.getStepsData(monthlyStartTime.timeInMillis, endTime)
            .observe(this, Observer { monthlyData ->
                updateMonthlyChart(monthlyData)
            })
    }
    
    /**
     * Update daily steps chart with hourly breakdown
     */
    private fun updateDailyChart(stepsDataList: List<StepsData>) {
        if (stepsDataList.isEmpty()) {
            // If no data, show empty chart
            setupEmptyDailyChart()
            return
        }
        
        // Group data by hour
        val hourlyData = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Initialize all hours with 0 steps
        for (i in 0..23) {
            hourlyData[i] = 0
        }
        
        // Fill in actual data
        for (stepsData in stepsDataList) {
            calendar.timeInMillis = stepsData.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyData[hour] = max(hourlyData[hour] ?: 0, stepsData.steps)
        }
        
        // Create entries for line chart
        val entries = ArrayList<Entry>()
        val hourLabels = ArrayList<String>()
        
        for (i in 0..23) {
            entries.add(Entry(i.toFloat(), hourlyData[i]?.toFloat() ?: 0f))
            hourLabels.add("${i}:00")
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Steps by Hour")
        dataSet.apply {
            color = ContextCompat.getColor(this@StepsActivity, R.color.steps_turquoise)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(this@StepsActivity, R.color.steps_turquoise))
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@StepsActivity, R.color.steps_turquoise_light)
            fillAlpha = 100
        }
        
        // Configure chart
        dailyStepsChart.apply {
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
        dailyStepsChart.apply {
            clear()
            setNoDataText("No steps data available for today")
            setNoDataTextColor(Color.BLACK)
            invalidate()
        }
    }
    
    /**
     * Update weekly steps chart with daily totals
     */
    private fun updateWeeklyChart(stepsDataList: List<StepsData>) {
        if (stepsDataList.isEmpty()) {
            // If no data, show empty chart
            setupEmptyWeeklyChart()
            return
        }
        
        // Group data by day of week
        val dailyData = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dayLabels = ArrayList<String>()
        
        // Initialize all days with 0 steps
        for (i in 0..6) {
            val day = calendar.clone() as Calendar
            day.add(Calendar.DAY_OF_YEAR, -6 + i)
            dailyData[day.get(Calendar.DAY_OF_YEAR)] = 0
            dayLabels.add(dayFormat.format(day.time))
        }
        
        // Fill in actual data
        for (stepsData in stepsDataList) {
            calendar.timeInMillis = stepsData.timestamp
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            if (dailyData.containsKey(dayOfYear)) {
                dailyData[dayOfYear] = max(dailyData[dayOfYear] ?: 0, stepsData.steps)
            }
        }
        
        // Create entries for bar chart
        val entries = ArrayList<BarEntry>()
        val days = dailyData.keys.sorted()
        
        for (i in 0 until days.size) {
            entries.add(BarEntry(i.toFloat(), dailyData[days[i]]?.toFloat() ?: 0f))
        }
        
        // Create dataset
        val dataSet = BarDataSet(entries, "Steps by Day")
        dataSet.apply {
            color = ContextCompat.getColor(this@StepsActivity, R.color.steps_turquoise)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
        }
        
        // Configure chart
        weeklyStepsChart.apply {
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
                axisMaximum = max(dailyStepsGoal * 1.2f, (dailyData.values.maxOrNull() ?: 0) * 1.2f)
                
                // Add goal line
                val goalLine = com.github.mikephil.charting.components.LimitLine(dailyStepsGoal.toFloat(), "Goal")
                goalLine.lineWidth = 1f
                goalLine.lineColor = ContextCompat.getColor(this@StepsActivity, R.color.sensacare_yellow)
                goalLine.labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
                goalLine.textSize = 10f
                addLimitLine(goalLine)
            }
            
            // Refresh chart
            invalidate()
        }
    }
    
    /**
     * Setup empty weekly chart when no data is available
     */
    private fun setupEmptyWeeklyChart() {
        weeklyStepsChart.apply {
            clear()
            setNoDataText("No steps data available for this week")
            setNoDataTextColor(Color.BLACK)
            invalidate()
        }
    }
    
    /**
     * Update monthly steps chart with weekly averages
     */
    private fun updateMonthlyChart(stepsDataList: List<StepsData>) {
        if (stepsDataList.isEmpty()) {
            // If no data, show empty chart
            setupEmptyMonthlyChart()
            return
        }
        
        // Group data by week
        val weeklyData = mutableMapOf<Int, Int>()
        val calendar = Calendar.getInstance()
        val weekLabels = ArrayList<String>()
        
        // Initialize weeks with 0 steps
        for (i in 0..4) {
            val week = calendar.clone() as Calendar
            week.add(Calendar.WEEK_OF_YEAR, -4 + i)
            val weekOfYear = week.get(Calendar.WEEK_OF_YEAR)
            weeklyData[weekOfYear] = 0
            weekLabels.add("W${weekOfYear}")
        }
        
        // Fill in actual data
        for (stepsData in stepsDataList) {
            calendar.timeInMillis = stepsData.timestamp
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            if (weeklyData.containsKey(weekOfYear)) {
                weeklyData[weekOfYear] = max(weeklyData[weekOfYear] ?: 0, stepsData.steps)
            }
        }
        
        // Create entries for bar chart
        val entries = ArrayList<BarEntry>()
        val weeks = weeklyData.keys.sorted()
        
        for (i in 0 until weeks.size) {
            entries.add(BarEntry(i.toFloat(), weeklyData[weeks[i]]?.toFloat() ?: 0f))
        }
        
        // Create dataset
        val dataSet = BarDataSet(entries, "Steps by Week")
        dataSet.apply {
            color = ContextCompat.getColor(this@StepsActivity, R.color.steps_turquoise)
            valueTextSize = 10f
            valueTextColor = Color.BLACK
        }
        
        // Configure chart
        monthlyStepsChart.apply {
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
                axisMaximum = max(dailyStepsGoal * 1.2f, (weeklyData.values.maxOrNull() ?: 0) * 1.2f)
                
                // Add weekly goal line (7x daily goal)
                val weeklyGoal = dailyStepsGoal * 7
                val goalLine = com.github.mikephil.charting.components.LimitLine(weeklyGoal.toFloat(), "Weekly Goal")
                goalLine.lineWidth = 1f
                goalLine.lineColor = ContextCompat.getColor(this@StepsActivity, R.color.sensacare_yellow)
                goalLine.labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
                goalLine.textSize = 10f
                addLimitLine(goalLine)
            }
            
            // Refresh chart
            invalidate()
        }
    }
    
    /**
     * Setup empty monthly chart when no data is available
     */
    private fun setupEmptyMonthlyChart() {
        monthlyStepsChart.apply {
            clear()
            setNoDataText("No steps data available for this month")
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
        dailyStepsChart.apply {
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
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading steps data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Setup weekly chart with initial configuration
     */
    private fun setupWeeklyChart() {
        weeklyStepsChart.apply {
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
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading steps data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Setup monthly chart with initial configuration
     */
    private fun setupMonthlyChart() {
        monthlyStepsChart.apply {
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
                setDrawGridLines(true)
            }
            
            axisRight.isEnabled = false
            
            // Set empty state
            setNoDataText("Loading steps data...")
            setNoDataTextColor(Color.BLACK)
        }
    }
    
    /**
     * Sync steps data from the connected device
     */
    private fun syncStepsData() {
        if (deviceAddress == null) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(this, "Syncing steps data...", Toast.LENGTH_SHORT).show()
        
        // Update UI to show syncing
        tvLastUpdated.text = "Syncing..."

        // Trigger HealthDataManager sync (will fetch/simulate latest data)
        healthDataManager.startSync()

        // Update UI after a short delay to simulate sync completion
        lifecycleScope.launch {
            delay(2000)
            tvLastUpdated.text = "Last updated: ${timeFormat.format(Date())}"
            Toast.makeText(this@StepsActivity, "Steps data updated", Toast.LENGTH_SHORT).show()
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
        // Nothing to clean up for now
    }
    
    companion object {
        private const val TAG = "StepsActivity"
    }
}
