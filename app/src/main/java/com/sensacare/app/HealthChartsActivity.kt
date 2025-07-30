package com.sensacare.app

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.sensacare.app.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class HealthChartsActivity : AppCompatActivity(), OnChartValueSelectedListener {

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var spinnerTimeRange: Spinner
    private lateinit var tvChartTitle: TextView
    private lateinit var tvNoDataMessage: TextView

    // Charts
    private lateinit var heartRateChart: LineChart
    private lateinit var stepsChart: BarChart
    private lateinit var sleepChart: BarChart
    private lateinit var bloodPressureChart: LineChart

    // Data Manager
    private lateinit var healthDataManager: HealthDataManager

    // Time periods
    private enum class TimePeriod {
        TODAY, WEEK, MONTH
    }

    private var currentTimePeriod = TimePeriod.TODAY

    // Date formatters
    private val hourFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_charts)

        // Get device address from intent
        val deviceAddress = intent.getStringExtra("device_address")
        if (deviceAddress == null) {
            finish()
            return
        }

        initializeViews()
        setupToolbar()
        setupTimeRangeSpinner()
        setupCharts()

        // Initialize health data manager and load data
        healthDataManager = HealthDataManager.getInstance(applicationContext)
        healthDataManager.initialize(deviceAddress)
        
        // Load initial data
        loadDataForTimePeriod(currentTimePeriod)
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        spinnerTimeRange = findViewById(R.id.spinnerTimeRange)
        tvChartTitle = findViewById(R.id.tvChartTitle)
        tvNoDataMessage = findViewById(R.id.tvNoDataMessage)
        
        // Initialize charts
        heartRateChart = findViewById(R.id.heartRateChart)
        stepsChart = findViewById(R.id.stepsChart)
        sleepChart = findViewById(R.id.sleepChart)
        bloodPressureChart = findViewById(R.id.bloodPressureChart)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Health Charts"
        }
    }

    private fun setupTimeRangeSpinner() {
        val timeRanges = arrayOf("Today", "This Week", "This Month")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timeRanges)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTimeRange.adapter = adapter

        spinnerTimeRange.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentTimePeriod = when (position) {
                    0 -> TimePeriod.TODAY
                    1 -> TimePeriod.WEEK
                    2 -> TimePeriod.MONTH
                    else -> TimePeriod.TODAY
                }
                loadDataForTimePeriod(currentTimePeriod)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupCharts() {
        // Heart Rate Chart
        setupHeartRateChart()
        
        // Steps Chart
        setupStepsChart()
        
        // Sleep Chart
        setupSleepChart()
        
        // Blood Pressure Chart
        setupBloodPressureChart()
    }

    private fun setupHeartRateChart() {
        heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            setDrawBorders(false)
            
            // Set chart value selected listener
            setOnChartValueSelectedListener(this@HealthChartsActivity)
            
            // Configure legend
            legend.apply {
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                textSize = 12f
                textColor = Color.BLACK
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            
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
                axisMinimum = 40f // Min heart rate
                axisMaximum = 160f // Max heart rate
                
                // Add heart rate zones
                val llZones = LimitLine(60f, "Low")
                llZones.lineWidth = 1f
                llZones.lineColor = Color.BLUE
                llZones.textSize = 8f
                addLimitLine(llZones)
                
                val llNormal = LimitLine(100f, "Normal")
                llNormal.lineWidth = 1f
                llNormal.lineColor = Color.GREEN
                llNormal.textSize = 8f
                addLimitLine(llNormal)
                
                val llHigh = LimitLine(120f, "High")
                llHigh.lineWidth = 1f
                llHigh.lineColor = Color.RED
                llHigh.textSize = 8f
                addLimitLine(llHigh)
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Animate chart
            animateX(1000)
        }
    }

    private fun setupStepsChart() {
        stepsChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            setDrawBorders(false)
            
            // Set chart value selected listener
            setOnChartValueSelectedListener(this@HealthChartsActivity)
            
            // Configure legend
            legend.apply {
                form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
                textSize = 12f
                textColor = Color.BLACK
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            
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
                axisMinimum = 0f
                
                // Add goal line
                val llGoal = LimitLine(10000f, "Goal")
                llGoal.lineWidth = 2f
                llGoal.lineColor = ContextCompat.getColor(this@HealthChartsActivity, R.color.activity_green)
                llGoal.textSize = 10f
                addLimitLine(llGoal)
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Animate chart
            animateY(1000)
        }
    }

    private fun setupSleepChart() {
        sleepChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            setDrawBorders(false)
            
            // Set chart value selected listener
            setOnChartValueSelectedListener(this@HealthChartsActivity)
            
            // Configure legend
            legend.apply {
                form = com.github.mikephil.charting.components.Legend.LegendForm.SQUARE
                textSize = 12f
                textColor = Color.BLACK
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            
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
                axisMinimum = 0f
                
                // Add recommended sleep line
                val llRecommended = LimitLine(480f, "8h Recommended")
                llRecommended.lineWidth = 2f
                llRecommended.lineColor = ContextCompat.getColor(this@HealthChartsActivity, R.color.sleep_purple)
                llRecommended.textSize = 10f
                addLimitLine(llRecommended)
                
                // Set value formatter to display hours and minutes
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val hours = (value / 60).toInt()
                        val minutes = (value % 60).toInt()
                        return "${hours}h ${minutes}m"
                    }
                }
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Animate chart
            animateY(1000)
        }
    }

    private fun setupBloodPressureChart() {
        bloodPressureChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            setDrawBorders(false)
            
            // Set chart value selected listener
            setOnChartValueSelectedListener(this@HealthChartsActivity)
            
            // Configure legend
            legend.apply {
                form = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                textSize = 12f
                textColor = Color.BLACK
                verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
            
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
                axisMinimum = 40f
                axisMaximum = 180f
                
                // Add blood pressure zones
                val llNormal = LimitLine(120f, "Normal")
                llNormal.lineWidth = 1f
                llNormal.lineColor = Color.GREEN
                llNormal.textSize = 8f
                addLimitLine(llNormal)
                
                val llElevated = LimitLine(140f, "Elevated")
                llElevated.lineWidth = 1f
                llElevated.lineColor = Color.YELLOW
                llElevated.textSize = 8f
                addLimitLine(llElevated)
                
                val llHigh = LimitLine(160f, "High")
                llHigh.lineWidth = 1f
                llHigh.lineColor = Color.RED
                llHigh.textSize = 8f
                addLimitLine(llHigh)
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Animate chart
            animateX(1000)
        }
    }

    private fun loadDataForTimePeriod(period: TimePeriod) {
        // Calculate start and end times based on selected period
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        
        // Set start time based on period
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        when (period) {
            TimePeriod.TODAY -> {
                // Start time is beginning of today
                tvChartTitle.text = "Today's Health Data"
            }
            TimePeriod.WEEK -> {
                // Start time is 7 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                tvChartTitle.text = "This Week's Health Data"
            }
            TimePeriod.MONTH -> {
                // Start time is 30 days ago
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                tvChartTitle.text = "This Month's Health Data"
            }
        }
        
        val startTime = calendar.timeInMillis
        
        // Load data for each chart
        loadHeartRateData(startTime, endTime, period)
        loadStepsData(startTime, endTime, period)
        loadSleepData(startTime, endTime, period)
        loadBloodPressureData(startTime, endTime, period)
    }

    private fun loadHeartRateData(startTime: Long, endTime: Long, period: TimePeriod) {
        healthDataManager.getHeartRateData(startTime, endTime).observe(this, Observer { heartRateList ->
            if (heartRateList.isNullOrEmpty()) {
                heartRateChart.visibility = View.GONE
                tvNoDataMessage.visibility = View.VISIBLE
                return@Observer
            }
            
            heartRateChart.visibility = View.VISIBLE
            tvNoDataMessage.visibility = View.GONE
            
            // Create entries for heart rate chart
            val entries = ArrayList<Entry>()
            val xLabels = ArrayList<String>()
            
            // Process data based on time period
            when (period) {
                TimePeriod.TODAY -> {
                    // Group by hour for today
                    val hourlyData = heartRateList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.HOUR_OF_DAY)
                    }
                    
                    // Create entries for each hour
                    for (hour in 0..23) {
                        val hourData = hourlyData[hour]
                        if (hourData != null) {
                            // Calculate average heart rate for this hour
                            val avgHeartRate = hourData.map { it.value }.average().toFloat()
                            entries.add(Entry(hour.toFloat(), avgHeartRate))
                            
                            // Format hour for X axis
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, 0)
                            xLabels.add(hourFormatter.format(cal.time))
                        } else {
                            // No data for this hour
                            xLabels.add("${hour}:00")
                        }
                    }
                }
                TimePeriod.WEEK -> {
                    // Group by day for week
                    val startCal = Calendar.getInstance()
                    startCal.timeInMillis = startTime
                    
                    val dailyData = heartRateList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..6) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Calculate average heart rate for this day
                            val avgHeartRate = dayData.map { it.value }.average().toFloat()
                            entries.add(Entry(day.toFloat(), avgHeartRate))
                        }
                        
                        // Format day for X axis
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = startTime
                        cal.add(Calendar.DAY_OF_YEAR, day)
                        xLabels.add(dayFormatter.format(cal.time))
                    }
                }
                TimePeriod.MONTH -> {
                    // Group by day for month
                    val startCal = Calendar.getInstance()
                    startCal.timeInMillis = startTime
                    
                    val dailyData = heartRateList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..29) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Calculate average heart rate for this day
                            val avgHeartRate = dayData.map { it.value }.average().toFloat()
                            entries.add(Entry(day.toFloat(), avgHeartRate))
                        }
                        
                        // Format day for X axis (only show every 5 days to avoid crowding)
                        if (day % 5 == 0) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = startTime
                            cal.add(Calendar.DAY_OF_YEAR, day)
                            xLabels.add(dayFormatter.format(cal.time))
                        } else {
                            xLabels.add("")
                        }
                    }
                }
            }
            
            // Set X axis labels
            heartRateChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            
            // Create dataset
            val dataSet = LineDataSet(entries, "Heart Rate (BPM)")
            dataSet.apply {
                color = ContextCompat.getColor(this@HealthChartsActivity, R.color.sensacare_yellow)
                lineWidth = 2f
                setDrawCircles(true)
                setDrawCircleHole(false)
                circleRadius = 3f
                setCircleColor(ContextCompat.getColor(this@HealthChartsActivity, R.color.sensacare_yellow))
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = ContextCompat.getColor(this@HealthChartsActivity, R.color.sensacare_yellow)
                fillAlpha = 50
                axisDependency = YAxis.AxisDependency.LEFT
            }
            
            // Create line data and set to chart
            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(dataSet)
            val lineData = LineData(dataSets)
            heartRateChart.data = lineData
            
            // Refresh chart
            heartRateChart.invalidate()
        })
    }

    private fun loadStepsData(startTime: Long, endTime: Long, period: TimePeriod) {
        healthDataManager.getStepsData(startTime, endTime).observe(this, Observer { stepsList ->
            if (stepsList.isNullOrEmpty()) {
                stepsChart.visibility = View.GONE
                return@Observer
            }
            
            stepsChart.visibility = View.VISIBLE
            
            // Create entries for steps chart
            val entries = ArrayList<BarEntry>()
            val xLabels = ArrayList<String>()
            
            // Process data based on time period
            when (period) {
                TimePeriod.TODAY -> {
                    // Group by hour for today
                    val hourlyData = stepsList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.HOUR_OF_DAY)
                    }
                    
                    // Create entries for each hour
                    for (hour in 0..23) {
                        val hourData = hourlyData[hour]
                        if (hourData != null) {
                            // Use the latest steps count for this hour
                            val latestSteps = hourData.maxByOrNull { it.timestamp }?.steps ?: 0
                            entries.add(BarEntry(hour.toFloat(), latestSteps.toFloat()))
                            
                            // Format hour for X axis
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, 0)
                            xLabels.add(hourFormatter.format(cal.time))
                        } else {
                            // No data for this hour
                            entries.add(BarEntry(hour.toFloat(), 0f))
                            xLabels.add("${hour}:00")
                        }
                    }
                }
                TimePeriod.WEEK -> {
                    // Group by day for week
                    val startCal = Calendar.getInstance()
                    startCal.timeInMillis = startTime
                    
                    val dailyData = stepsList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..6) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Use the latest steps count for this day
                            val latestSteps = dayData.maxByOrNull { it.timestamp }?.steps ?: 0
                            entries.add(BarEntry(day.toFloat(), latestSteps.toFloat()))
                        } else {
                            // No data for this day
                            entries.add(BarEntry(day.toFloat(), 0f))
                        }
                        
                        // Format day for X axis
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = startTime
                        cal.add(Calendar.DAY_OF_YEAR, day)
                        xLabels.add(dayFormatter.format(cal.time))
                    }
                }
                TimePeriod.MONTH -> {
                    // Group by day for month
                    val startCal = Calendar.getInstance()
                    startCal.timeInMillis = startTime
                    
                    val dailyData = stepsList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..29) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Use the latest steps count for this day
                            val latestSteps = dayData.maxByOrNull { it.timestamp }?.steps ?: 0
                            entries.add(BarEntry(day.toFloat(), latestSteps.toFloat()))
                        } else {
                            // No data for this day
                            entries.add(BarEntry(day.toFloat(), 0f))
                        }
                        
                        // Format day for X axis (only show every 5 days to avoid crowding)
                        if (day % 5 == 0) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = startTime
                            cal.add(Calendar.DAY_OF_YEAR, day)
                            xLabels.add(dayFormatter.format(cal.time))
                        } else {
                            xLabels.add("")
                        }
                    }
                }
            }
            
            // Set X axis labels
            stepsChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            
            // Create dataset
            val dataSet = BarDataSet(entries, "Steps")
            dataSet.apply {
                color = ContextCompat.getColor(this@HealthChartsActivity, R.color.activity_green)
                valueTextColor = Color.BLACK
                valueTextSize = 10f
                setDrawValues(false)
            }
            
            // Create bar data and set to chart
            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(dataSet)
            val barData = BarData(dataSets)
            barData.barWidth = 0.8f
            stepsChart.data = barData
            
            // Refresh chart
            stepsChart.invalidate()
        })
    }

    private fun loadSleepData(startTime: Long, endTime: Long, period: TimePeriod) {
        healthDataManager.getSleepData(startTime, endTime).observe(this, Observer { sleepList ->
            if (sleepList.isNullOrEmpty()) {
                sleepChart.visibility = View.GONE
                return@Observer
            }
            
            sleepChart.visibility = View.VISIBLE
            
            // Create entries for sleep chart
            val deepSleepEntries = ArrayList<BarEntry>()
            val lightSleepEntries = ArrayList<BarEntry>()
            val xLabels = ArrayList<String>()
            
            // Process data based on time period
            when (period) {
                TimePeriod.TODAY -> {
                    // For today, we'll just show one bar with deep and light sleep
                    val todaySleep = sleepList.maxByOrNull { it.timestamp }
                    if (todaySleep != null) {
                        deepSleepEntries.add(BarEntry(0f, todaySleep.deepSleepMinutes.toFloat()))
                        lightSleepEntries.add(BarEntry(0f, todaySleep.lightSleepMinutes.toFloat()))
                        xLabels.add("Today")
                    }
                }
                TimePeriod.WEEK -> {
                    // Group by day for week
                    val dailyData = sleepList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..6) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Use the latest sleep data for this day
                            val latestSleep = dayData.maxByOrNull { it.timestamp }
                            if (latestSleep != null) {
                                deepSleepEntries.add(BarEntry(day.toFloat(), latestSleep.deepSleepMinutes.toFloat()))
                                lightSleepEntries.add(BarEntry(day.toFloat(), latestSleep.lightSleepMinutes.toFloat()))
                            } else {
                                deepSleepEntries.add(BarEntry(day.toFloat(), 0f))
                                lightSleepEntries.add(BarEntry(day.toFloat(), 0f))
                            }
                        } else {
                            // No data for this day
                            deepSleepEntries.add(BarEntry(day.toFloat(), 0f))
                            lightSleepEntries.add(BarEntry(day.toFloat(), 0f))
                        }
                        
                        // Format day for X axis
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = startTime
                        cal.add(Calendar.DAY_OF_YEAR, day)
                        xLabels.add(dayFormatter.format(cal.time))
                    }
                }
                TimePeriod.MONTH -> {
                    // Group by day for month
                    val dailyData = sleepList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..29) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Use the latest sleep data for this day
                            val latestSleep = dayData.maxByOrNull { it.timestamp }
                            if (latestSleep != null) {
                                deepSleepEntries.add(BarEntry(day.toFloat(), latestSleep.deepSleepMinutes.toFloat()))
                                lightSleepEntries.add(BarEntry(day.toFloat(), latestSleep.lightSleepMinutes.toFloat()))
                            } else {
                                deepSleepEntries.add(BarEntry(day.toFloat(), 0f))
                                lightSleepEntries.add(BarEntry(day.toFloat(), 0f))
                            }
                        } else {
                            // No data for this day
                            deepSleepEntries.add(BarEntry(day.toFloat(), 0f))
                            lightSleepEntries.add(BarEntry(day.toFloat(), 0f))
                        }
                        
                        // Format day for X axis (only show every 5 days to avoid crowding)
                        if (day % 5 == 0) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = startTime
                            cal.add(Calendar.DAY_OF_YEAR, day)
                            xLabels.add(dayFormatter.format(cal.time))
                        } else {
                            xLabels.add("")
                        }
                    }
                }
            }
            
            // Set X axis labels
            sleepChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            
            // Create datasets
            val deepSleepDataSet = BarDataSet(deepSleepEntries, "Deep Sleep")
            deepSleepDataSet.apply {
                color = ContextCompat.getColor(this@HealthChartsActivity, R.color.sleep_purple)
                valueTextColor = Color.BLACK
                valueTextSize = 10f
                setDrawValues(false)
            }
            
            val lightSleepDataSet = BarDataSet(lightSleepEntries, "Light Sleep")
            lightSleepDataSet.apply {
                color = ContextCompat.getColor(this@HealthChartsActivity, R.color.sleep_light_purple)
                valueTextColor = Color.BLACK
                valueTextSize = 10f
                setDrawValues(false)
            }
            
            // Create stacked bar data
            val dataSets = ArrayList<IBarDataSet>()
            dataSets.add(deepSleepDataSet)
            dataSets.add(lightSleepDataSet)
            val barData = BarData(dataSets)
            barData.barWidth = 0.8f
            
            // Set data to chart
            sleepChart.data = barData
            
            // Configure for stacked bars
            sleepChart.setFitBars(true)
            
            // Refresh chart
            sleepChart.invalidate()
        })
    }

    private fun loadBloodPressureData(startTime: Long, endTime: Long, period: TimePeriod) {
        healthDataManager.getBloodPressureData(startTime, endTime).observe(this, Observer { bpList ->
            if (bpList.isNullOrEmpty()) {
                bloodPressureChart.visibility = View.GONE
                return@Observer
            }
            
            bloodPressureChart.visibility = View.VISIBLE
            
            // Create entries for blood pressure chart
            val systolicEntries = ArrayList<Entry>()
            val diastolicEntries = ArrayList<Entry>()
            val xLabels = ArrayList<String>()
            
            // Process data based on time period
            when (period) {
                TimePeriod.TODAY -> {
                    // Group by hour for today
                    val hourlyData = bpList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        cal.get(Calendar.HOUR_OF_DAY)
                    }
                    
                    // Create entries for each hour
                    for (hour in 0..23) {
                        val hourData = hourlyData[hour]
                        if (hourData != null) {
                            // Calculate average blood pressure for this hour
                            val avgSystolic = hourData.map { it.systolic }.average().toFloat()
                            val avgDiastolic = hourData.map { it.diastolic }.average().toFloat()
                            systolicEntries.add(Entry(hour.toFloat(), avgSystolic))
                            diastolicEntries.add(Entry(hour.toFloat(), avgDiastolic))
                            
                            // Format hour for X axis
                            val cal = Calendar.getInstance()
                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, 0)
                            xLabels.add(hourFormatter.format(cal.time))
                        } else {
                            // No data for this hour
                            xLabels.add("${hour}:00")
                        }
                    }
                }
                TimePeriod.WEEK -> {
                    // Group by day for week
                    val startCal = Calendar.getInstance()
                    startCal.timeInMillis = startTime
                    
                    val dailyData = bpList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..6) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Calculate average blood pressure for this day
                            val avgSystolic = dayData.map { it.systolic }.average().toFloat()
                            val avgDiastolic = dayData.map { it.diastolic }.average().toFloat()
                            systolicEntries.add(Entry(day.toFloat(), avgSystolic))
                            diastolicEntries.add(Entry(day.toFloat(), avgDiastolic))
                        }
                        
                        // Format day for X axis
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = startTime
                        cal.add(Calendar.DAY_OF_YEAR, day)
                        xLabels.add(dayFormatter.format(cal.time))
                    }
                }
                TimePeriod.MONTH -> {
                    // Group by day for month
                    val startCal = Calendar.getInstance()
                    startCal.timeInMillis = startTime
                    
                    val dailyData = bpList.groupBy { 
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = it.timestamp
                        val dayDiff = ((cal.timeInMillis - startTime) / (24 * 60 * 60 * 1000)).toInt()
                        dayDiff
                    }
                    
                    // Create entries for each day
                    for (day in 0..29) {
                        val dayData = dailyData[day]
                        if (dayData != null) {
                            // Calculate average blood pressure for this day
                            val avgSystolic = dayData.map { it.systolic }.average().toFloat()
                            val avgDiastolic = dayData.map { it.diastolic }.average().toFloat()
                            systolicEntries.add(Entry(day.toFloat(), avgSystolic))
                            diastolicEntries.add(Entry(day.toFloat(), avgDiastolic))
                        }
                        
                        // Format day for X axis (only show every 5 days to avoid crowding)
                        if (day % 5 == 0) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = startTime
                            cal.add(Calendar.DAY_OF_YEAR, day)
                            xLabels.add(dayFormatter.format(cal.time))
                        } else {
                            xLabels.add("")
                        }
                    }
                }
            }
            
            // Set X axis labels
            bloodPressureChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            
            // Create datasets
            val systolicDataSet = LineDataSet(systolicEntries, "Systolic")
            systolicDataSet.apply {
                color = Color.RED
                lineWidth = 2f
                setDrawCircles(true)
                setDrawCircleHole(false)
                circleRadius = 3f
                setCircleColor(Color.RED)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                axisDependency = YAxis.AxisDependency.LEFT
            }
            
            val diastolicDataSet = LineDataSet(diastolicEntries, "Diastolic")
            diastolicDataSet.apply {
                color = Color.BLUE
                lineWidth = 2f
                setDrawCircles(true)
                setDrawCircleHole(false)
                circleRadius = 3f
                setCircleColor(Color.BLUE)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                axisDependency = YAxis.AxisDependency.LEFT
            }
            
            // Create line data and set to chart
            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(systolicDataSet)
            dataSets.add(diastolicDataSet)
            val lineData = LineData(dataSets)
            bloodPressureChart.data = lineData
            
            // Refresh chart
            bloodPressureChart.invalidate()
        })
    }

    // OnChartValueSelectedListener implementation
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        if (e != null && h != null) {
            val value = e.y
            val xIndex = e.x.toInt()
            
            // Show toast with selected value
            when {
                h.dataSetIndex == 0 && heartRateChart.data.getDataSetByIndex(0).label == "Heart Rate (BPM)" -> {
                    Toast.makeText(this, "Heart Rate: ${value.toInt()} BPM", Toast.LENGTH_SHORT).show()
                }
                h.dataSetIndex == 0 && stepsChart.data.getDataSetByIndex(0).label == "Steps" -> {
                    Toast.makeText(this, "Steps: ${value.toInt()}", Toast.LENGTH_SHORT).show()
                }
                h.dataSetIndex == 0 && sleepChart.data.getDataSetByIndex(0).label == "Deep Sleep" -> {
                    val hours = (value / 60).toInt()
                    val minutes = (value % 60).toInt()
                    Toast.makeText(this, "Deep Sleep: ${hours}h ${minutes}m", Toast.LENGTH_SHORT).show()
                }
                h.dataSetIndex == 1 && sleepChart.data.getDataSetByIndex(1).label == "Light Sleep" -> {
                    val hours = (value / 60).toInt()
                    val minutes = (value % 60).toInt()
                    Toast.makeText(this, "Light Sleep: ${hours}h ${minutes}m", Toast.LENGTH_SHORT).show()
                }
                h.dataSetIndex == 0 && bloodPressureChart.data.getDataSetByIndex(0).label == "Systolic" -> {
                    Toast.makeText(this, "Systolic: ${value.toInt()} mmHg", Toast.LENGTH_SHORT).show()
                }
                h.dataSetIndex == 1 && bloodPressureChart.data.getDataSetByIndex(1).label == "Diastolic" -> {
                    Toast.makeText(this, "Diastolic: ${value.toInt()} mmHg", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(this, "Value: ${value.toInt()}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onNothingSelected() {
        // Do nothing
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
