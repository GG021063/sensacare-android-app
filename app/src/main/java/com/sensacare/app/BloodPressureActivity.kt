package com.sensacare.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.sensacare.app.data.BloodPressureData
import com.sensacare.app.data.HealthDataManager
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * BloodPressureActivity - Comprehensive blood pressure monitoring and analysis
 * 
 * Features:
 * - Day/Week/Month views
 * - BP gauge visualization
 * - Systolic/Diastolic readings
 * - BP categorization
 * - 7-day trends
 * - Educational content
 * - On-demand measurement
 */
class BloodPressureActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var tvSystolic: TextView
    private lateinit var tvDiastolic: TextView
    private lateinit var tvBpDate: TextView
    private lateinit var bpTrendChart: BarChart
    private lateinit var btnMeasure: MaterialButton
    
    // BP gauge visualization
    private lateinit var systolicGauge: View
    private lateinit var diastolicGauge: View
    
    // Data
    private lateinit var healthDataManager: HealthDataManager
    
    // Time range
    private enum class TimeRange { DAY, WEEK, MONTH }
    private var currentTimeRange = TimeRange.DAY
    
    // Device info
    private var deviceAddress: String? = null
    
    // Date formatters
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blood_pressure)
        
        // Get device address from intent
        deviceAddress = intent.getStringExtra("device_address")
        if (deviceAddress == null) {
            Toast.makeText(this, "No device connected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Initialize health data manager
        healthDataManager = HealthDataManager.getInstance(applicationContext)
        healthDataManager.initialize(deviceAddress!!)
        
        initializeViews()
        setupTabLayout()
        setupBpTrendChart()
        setupMeasureButton()
        loadBloodPressureData()
    }
    
    private fun initializeViews() {
        // Find views
        tabLayout = findViewById(R.id.tabLayout)
        tvSystolic = findViewById(R.id.tvSystolic)
        tvDiastolic = findViewById(R.id.tvDiastolic)
        tvBpDate = findViewById(R.id.tvBpDate)
        bpTrendChart = findViewById(R.id.bpTrendChart)
        btnMeasure = findViewById(R.id.btnMeasure)
        
        // BP gauge views
        systolicGauge = findViewById(R.id.systolicGauge)
        diastolicGauge = findViewById(R.id.diastolicGauge)
        
        // Setup back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupTabLayout() {
        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Day"))
        tabLayout.addTab(tabLayout.newTab().setText("Week"))
        tabLayout.addTab(tabLayout.newTab().setText("Month"))
        
        // Set tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> setTimeRange(TimeRange.DAY)
                    1 -> setTimeRange(TimeRange.WEEK)
                    2 -> setTimeRange(TimeRange.MONTH)
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupBpTrendChart() {
        bpTrendChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            // Configure legend
            legend.isEnabled = false
            
            // Configure X axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textSize = 10f
                textColor = Color.DKGRAY
                setDrawGridLines(false)
                setDrawAxisLine(true)
                granularity = 1f
            }
            
            // Configure left Y axis
            axisLeft.apply {
                textSize = 10f
                textColor = Color.DKGRAY
                setDrawGridLines(true)
                axisMinimum = 40f // Min BP value (diastolic)
                axisMaximum = 200f // Max BP value (systolic)
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Animate chart
            animateX(1000)
        }
    }
    
    private fun setupMeasureButton() {
        btnMeasure.setOnClickListener {
            // Start blood pressure measurement
            Toast.makeText(this, "Starting blood pressure measurement...", Toast.LENGTH_SHORT).show()
            
            // In a real app, this would trigger the device to measure blood pressure
            // For now, we'll simulate a measurement
            simulateBloodPressureMeasurement()
        }
    }
    
    private fun simulateBloodPressureMeasurement() {
        // Show measuring state
        btnMeasure.isEnabled = false
        btnMeasure.text = "Measuring..."
        
        // Simulate measurement delay
        btnMeasure.postDelayed({
            // Generate random blood pressure values
            val systolic = (110..140).random()
            val diastolic = (70..90).random()
            
            // Create blood pressure data
            val data = BloodPressureData(
                timestamp = System.currentTimeMillis(),
                systolic = systolic,
                diastolic = diastolic,
                pulse = (60..80).random(),
                deviceAddress = deviceAddress ?: ""
            )
            
            // Save to database
            healthDataManager.addBloodPressureData(data)
            
            // Update UI
            updateBloodPressureDisplay(data)
            
            // Reset button
            btnMeasure.isEnabled = true
            btnMeasure.text = "MEASURE"
            
            // Show success message
            Toast.makeText(this, "Blood pressure measured: $systolic/$diastolic mmHg", Toast.LENGTH_SHORT).show()
        }, 5000) // BP measurement takes longer than heart rate
    }
    
    private fun loadBloodPressureData() {
        // Calculate time range
        val endTime = System.currentTimeMillis()
        val startTime = when (currentTimeRange) {
            TimeRange.DAY -> endTime - TimeUnit.DAYS.toMillis(1)
            TimeRange.WEEK -> endTime - TimeUnit.DAYS.toMillis(7)
            TimeRange.MONTH -> endTime - TimeUnit.DAYS.toMillis(30)
        }
        
        // Observe blood pressure data
        healthDataManager.getBloodPressureData(startTime, endTime).observe(this, Observer { data ->
            if (data.isNotEmpty()) {
                // Update latest blood pressure display
                updateBloodPressureDisplay(data[0])
                
                // Update trend chart
                updateBloodPressureTrendChart(data)
            } else {
                // No data available
                resetBloodPressureDisplay()
            }
        })
    }
    
    private fun updateBloodPressureDisplay(bpData: BloodPressureData) {
        // Update systolic and diastolic values
        tvSystolic.text = bpData.systolic.toString()
        tvDiastolic.text = bpData.diastolic.toString()
        
        // Update date
        tvBpDate.text = dateFormatter.format(Date(bpData.timestamp))
        
        // Update BP gauge visualization
        updateBpGauge(bpData.systolic, bpData.diastolic)
        
        // Update BP category
        updateBpCategory(bpData.systolic, bpData.diastolic)
    }
    
    private fun updateBpGauge(systolic: Int, diastolic: Int) {
        // Calculate gauge heights based on BP values
        // The gauge is a visual representation of where the values fall in the range
        
        // Systolic gauge (normal range: 90-140 mmHg)
        val systolicMin = 60f
        val systolicMax = 200f
        val systolicRange = systolicMax - systolicMin
        val systolicPercentage = ((systolic - systolicMin) / systolicRange).coerceIn(0f, 1f)
        
        // Update systolic gauge height
        val systolicParams = systolicGauge.layoutParams
        systolicParams.height = (systolicPercentage * 200).toInt() // Scale factor for better visualization
        systolicGauge.layoutParams = systolicParams
        
        // Diastolic gauge (normal range: 60-90 mmHg)
        val diastolicMin = 40f
        val diastolicMax = 120f
        val diastolicRange = diastolicMax - diastolicMin
        val diastolicPercentage = ((diastolic - diastolicMin) / diastolicRange).coerceIn(0f, 1f)
        
        // Update diastolic gauge height
        val diastolicParams = diastolicGauge.layoutParams
        diastolicParams.height = (diastolicPercentage * 200).toInt() // Scale factor for better visualization
        diastolicGauge.layoutParams = diastolicParams
    }
    
    private fun updateBpCategory(systolic: Int, diastolic: Int) {
        // Determine BP category based on values
        val category = when {
            systolic < 120 && diastolic < 80 -> "Normal"
            systolic in 120..129 && diastolic < 80 -> "Elevated"
            systolic in 130..139 || diastolic in 80..89 -> "Stage 1 Hypertension"
            systolic >= 140 || diastolic >= 90 -> "Stage 2 Hypertension"
            systolic >= 180 || diastolic >= 120 -> "Hypertensive Crisis"
            else -> "Normal"
        }
        
        // Update category text and color
        try {
            val tvCategory = findViewById<TextView>(R.id.tvBpCategory)
            tvCategory.text = category
            
            // Set color based on category
            val categoryColor = when (category) {
                "Normal" -> ContextCompat.getColor(this, R.color.bp_normal)
                "Elevated" -> ContextCompat.getColor(this, R.color.bp_elevated)
                "Stage 1 Hypertension" -> ContextCompat.getColor(this, R.color.bp_stage1)
                "Stage 2 Hypertension" -> ContextCompat.getColor(this, R.color.bp_stage2)
                "Hypertensive Crisis" -> ContextCompat.getColor(this, R.color.bp_crisis)
                else -> ContextCompat.getColor(this, R.color.bp_normal)
            }
            
            tvCategory.setTextColor(categoryColor)
        } catch (e: Exception) {
            // TextView might not be available
        }
    }
    
    private fun updateBloodPressureTrendChart(data: List<BloodPressureData>) {
        // Create entries for the chart
        val systolicEntries = ArrayList<BarEntry>()
        val diastolicEntries = ArrayList<BarEntry>()
        val xLabels = ArrayList<String>()
        
        // Sort data by timestamp
        val sortedData = data.sortedBy { it.timestamp }
        
        // Process data based on time range
        when (currentTimeRange) {
            TimeRange.DAY -> {
                // For day view, show individual readings
                sortedData.forEachIndexed { index, bp ->
                    systolicEntries.add(BarEntry(index.toFloat(), bp.systolic.toFloat()))
                    diastolicEntries.add(BarEntry(index.toFloat(), bp.diastolic.toFloat()))
                    
                    // Format time for X axis
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(bp.timestamp))
                    xLabels.add(time)
                }
            }
            TimeRange.WEEK -> {
                // Group by day for week view
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                
                // Go back 7 days
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                
                // Create entries for each day
                for (day in 0..6) {
                    val dayStart = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val dayEnd = calendar.timeInMillis
                    
                    // Get data for this day
                    val dayData = sortedData.filter { it.timestamp in dayStart until dayEnd }
                    
                    if (dayData.isNotEmpty()) {
                        // Calculate average BP for this day
                        val avgSystolic = dayData.map { it.systolic }.average().toFloat()
                        val avgDiastolic = dayData.map { it.diastolic }.average().toFloat()
                        
                        systolicEntries.add(BarEntry(day.toFloat(), avgSystolic))
                        diastolicEntries.add(BarEntry(day.toFloat(), avgDiastolic))
                    } else {
                        // No data for this day
                        systolicEntries.add(BarEntry(day.toFloat(), 0f))
                        diastolicEntries.add(BarEntry(day.toFloat(), 0f))
                    }
                    
                    // Format day for X axis
                    val date = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(dayStart))
                    xLabels.add(date)
                }
            }
            TimeRange.MONTH -> {
                // Group by week for month view
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                
                // Go back to start of month
                calendar.add(Calendar.MONTH, -1)
                
                // Create entries for each week
                for (week in 0..3) {
                    val weekStart = calendar.timeInMillis
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    val weekEnd = calendar.timeInMillis
                    
                    // Get data for this week
                    val weekData = sortedData.filter { it.timestamp in weekStart until weekEnd }
                    
                    if (weekData.isNotEmpty()) {
                        // Calculate average BP for this week
                        val avgSystolic = weekData.map { it.systolic }.average().toFloat()
                        val avgDiastolic = weekData.map { it.diastolic }.average().toFloat()
                        
                        systolicEntries.add(BarEntry(week.toFloat(), avgSystolic))
                        diastolicEntries.add(BarEntry(week.toFloat(), avgDiastolic))
                    } else {
                        // No data for this week
                        systolicEntries.add(BarEntry(week.toFloat(), 0f))
                        diastolicEntries.add(BarEntry(week.toFloat(), 0f))
                    }
                    
                    // Format week for X axis
                    val startDate = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(weekStart))
                    val endDate = SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(weekEnd - 1))
                    xLabels.add("$startDate-$endDate")
                }
            }
        }
        
        // Configure X axis labels
        bpTrendChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        
        // Create datasets
        val systolicDataSet = BarDataSet(systolicEntries, "Systolic")
        systolicDataSet.apply {
            color = ContextCompat.getColor(this@BloodPressureActivity, R.color.bp_systolic)
            valueTextColor = Color.BLACK
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) value.toInt().toString() else ""
                }
            }
        }
        
        val diastolicDataSet = BarDataSet(diastolicEntries, "Diastolic")
        diastolicDataSet.apply {
            color = ContextCompat.getColor(this@BloodPressureActivity, R.color.bp_diastolic)
            valueTextColor = Color.BLACK
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) value.toInt().toString() else ""
                }
            }
        }
        
        // Group bars
        val groupSpace = 0.3f
        val barSpace = 0.05f
        val barWidth = 0.3f
        
        // Create bar data
        val barData = BarData(systolicDataSet, diastolicDataSet)
        barData.barWidth = barWidth
        
        // Set data to chart
        bpTrendChart.data = barData
        
        // Configure for grouped bars
        bpTrendChart.groupBars(0f, groupSpace, barSpace)
        
        // Refresh chart
        bpTrendChart.invalidate()
    }
    
    private fun resetBloodPressureDisplay() {
        // Reset BP values
        tvSystolic.text = "--"
        tvDiastolic.text = "--"
        
        // Reset date
        tvBpDate.text = "No data available"
        
        // Reset gauge
        updateBpGauge(0, 0)
        
        // Reset chart
        bpTrendChart.data = null
        bpTrendChart.invalidate()
    }
    
    private fun setTimeRange(timeRange: TimeRange) {
        if (currentTimeRange != timeRange) {
            currentTimeRange = timeRange
            loadBloodPressureData()
        }
    }
}
