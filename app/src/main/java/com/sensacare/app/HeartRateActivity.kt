package com.sensacare.app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.sensacare.app.data.HeartRateData
import com.sensacare.app.data.HealthDataManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.view.LayoutInflater
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.concurrent.TimeUnit

/**
 * HeartRateActivity - Comprehensive heart rate monitoring and analysis
 * 
 * Features:
 * - Day/Week/Month views
 * - Heart rate zone analysis
 * - Historical data list
 * - Educational content
 * - On-demand measurement
 */
class HeartRateActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var tvHeartRate: TextView
    private lateinit var tvHeartRateDate: TextView
    private lateinit var heartRateChart: LineChart
    private lateinit var rvHeartRateHistory: RecyclerView
    private lateinit var btnMeasure: MaterialButton
    
    // Zone progress views
    private lateinit var progressLight: View
    private lateinit var progressWeight: View
    private lateinit var progressAerobic: View
    private lateinit var progressAnaerobic: View
    private lateinit var progressVO2Max: View
    
    // Zone time labels
    private lateinit var tvLightTime: TextView
    private lateinit var tvWeightTime: TextView
    private lateinit var tvAerobicTime: TextView
    private lateinit var tvAnaerobicTime: TextView
    private lateinit var tvVO2MaxTime: TextView
    
    // Data
    private lateinit var healthDataManager: HealthDataManager
    private lateinit var heartRateAdapter: HeartRateHistoryAdapter
    private val heartRateHistory = mutableListOf<HeartRateData>()
    
    // Time range
    private enum class TimeRange { DAY, WEEK, MONTH }
    private var currentTimeRange = TimeRange.DAY
    
    // Device info
    private var deviceAddress: String? = null
    
    // Date formatters
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate)
        
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
        setupHeartRateChart()
        setupHeartRateHistory()
        setupMeasureButton()
        loadHeartRateData()
    }
    
    private fun initializeViews() {
        // Find views
        tabLayout = findViewById(R.id.tabLayout)
        tvHeartRate = findViewById(R.id.tvHeartRate)
        tvHeartRateDate = findViewById(R.id.tvHeartRateDate)
        heartRateChart = findViewById(R.id.heartRateChart)
        rvHeartRateHistory = findViewById(R.id.rvHeartRateHistory)
        btnMeasure = findViewById(R.id.btnMeasure)
        
        // Zone progress views
        progressLight = findViewById(R.id.progressLight)
        progressWeight = findViewById(R.id.progressWeight)
        progressAerobic = findViewById(R.id.progressAerobic)
        progressAnaerobic = findViewById(R.id.progressAnaerobic)
        progressVO2Max = findViewById(R.id.progressVO2Max)
        
        // Zone time labels
        tvLightTime = findViewById(R.id.tvLightTime)
        tvWeightTime = findViewById(R.id.tvWeightTime)
        tvAerobicTime = findViewById(R.id.tvAerobicTime)
        tvAnaerobicTime = findViewById(R.id.tvAnaerobicTime)
        tvVO2MaxTime = findViewById(R.id.tvVO2MaxTime)
        
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
    
    private fun setupHeartRateChart() {
        heartRateChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)
            
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
                axisMinimum = 40f // Min heart rate
                axisMaximum = 180f // Max heart rate
            }
            
            // Disable right Y axis
            axisRight.isEnabled = false
            
            // Animate chart
            animateX(1000)
        }
    }
    
    private fun setupHeartRateHistory() {
        // Initialize adapter
        heartRateAdapter = HeartRateHistoryAdapter(heartRateHistory)
        
        // Setup RecyclerView
        rvHeartRateHistory.apply {
            layoutManager = LinearLayoutManager(this@HeartRateActivity)
            adapter = heartRateAdapter
        }
    }
    
    private fun setupMeasureButton() {
        btnMeasure.setOnClickListener {
            // Start heart rate measurement
            Toast.makeText(this, "Starting heart rate measurement...", Toast.LENGTH_SHORT).show()
            
            // In a real app, this would trigger the device to measure heart rate
            // For now, we'll simulate a measurement
            simulateHeartRateMeasurement()
        }
    }
    
    private fun simulateHeartRateMeasurement() {
        // Show measuring state
        btnMeasure.isEnabled = false
        btnMeasure.text = "Measuring..."
        
        // Simulate measurement delay
        btnMeasure.postDelayed({
            // Generate random heart rate between 60-100
            val heartRate = (60..100).random()
            
            // Create heart rate data
            val data = HeartRateData(
                timestamp = System.currentTimeMillis(),
                value = heartRate,
                deviceAddress = deviceAddress ?: ""
            )
            
            // Save to database
            healthDataManager.addHeartRateData(data)
            
            // Update UI
            updateHeartRateDisplay(data)
            
            // Add to history and refresh adapter
            heartRateHistory.add(0, data)
            heartRateAdapter.notifyItemInserted(0)
            
            // Reset button
            btnMeasure.isEnabled = true
            btnMeasure.text = "MEASURE"
            
            // Show success message
            Toast.makeText(this, "Heart rate measured: $heartRate BPM", Toast.LENGTH_SHORT).show()
        }, 3000)
    }
    
    private fun loadHeartRateData() {
        // Calculate time range
        val endTime = System.currentTimeMillis()
        val startTime = when (currentTimeRange) {
            TimeRange.DAY -> endTime - TimeUnit.DAYS.toMillis(1)
            TimeRange.WEEK -> endTime - TimeUnit.DAYS.toMillis(7)
            TimeRange.MONTH -> endTime - TimeUnit.DAYS.toMillis(30)
        }
        
        // Observe heart rate data
        healthDataManager.getHeartRateData(startTime, endTime).observe(this, Observer { data ->
            if (data.isNotEmpty()) {
                // Update latest heart rate display
                updateHeartRateDisplay(data[0])
                
                // Update chart
                updateHeartRateChart(data)
                
                // Update history
                updateHeartRateHistory(data)
                
                // Update heart rate zones
                updateHeartRateZones(data)
            } else {
                // No data available
                tvHeartRate.text = "--"
                tvHeartRateDate.text = "No data available"
                
                // Clear chart
                heartRateChart.clear()
                heartRateChart.invalidate()
                
                // Clear history
                heartRateHistory.clear()
                heartRateAdapter.notifyDataSetChanged()
                
                // Reset zones
                resetHeartRateZones()
            }
        })
    }
    
    private fun updateHeartRateDisplay(data: HeartRateData) {
        tvHeartRate.text = data.value.toString()
        tvHeartRateDate.text = dateFormatter.format(Date(data.timestamp))
    }
    
    private fun updateHeartRateChart(data: List<HeartRateData>) {
        // Create entries for the chart
        val entries = ArrayList<Entry>()
        val xLabels = ArrayList<String>()
        
        // Sort data by timestamp
        val sortedData = data.sortedBy { it.timestamp }
        
        // Process data based on time range
        when (currentTimeRange) {
            TimeRange.DAY -> {
                // Group by hour for day view
                val hourlyData = sortedData.groupBy { 
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
                        xLabels.add("${hour}:00")
                    } else {
                        // Add empty label for hours with no data
                        xLabels.add("${hour}:00")
                    }
                }
            }
            TimeRange.WEEK -> {
                // Group by day for week view
                val startCal = Calendar.getInstance()
                startCal.timeInMillis = sortedData.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                
                // Create entries for each day
                for (day in 0..6) {
                    val dayStart = startCal.timeInMillis + TimeUnit.DAYS.toMillis(day.toLong())
                    val dayEnd = dayStart + TimeUnit.DAYS.toMillis(1)
                    
                    // Get data for this day
                    val dayData = sortedData.filter { it.timestamp in dayStart until dayEnd }
                    
                    if (dayData.isNotEmpty()) {
                        // Calculate average heart rate for this day
                        val avgHeartRate = dayData.map { it.value }.average().toFloat()
                        entries.add(Entry(day.toFloat(), avgHeartRate))
                    }
                    
                    // Format day for X axis
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = dayStart
                    xLabels.add(dayFormatter.format(cal.time))
                }
            }
            TimeRange.MONTH -> {
                // Group by 3-day periods for month view
                val daysPerPeriod = 3
                val periods = 30 / daysPerPeriod
                
                val startCal = Calendar.getInstance()
                startCal.timeInMillis = sortedData.firstOrNull()?.timestamp ?: System.currentTimeMillis()
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                
                // Create entries for each period
                for (period in 0 until periods) {
                    val periodStart = startCal.timeInMillis + TimeUnit.DAYS.toMillis((period * daysPerPeriod).toLong())
                    val periodEnd = periodStart + TimeUnit.DAYS.toMillis(daysPerPeriod.toLong())
                    
                    // Get data for this period
                    val periodData = sortedData.filter { it.timestamp in periodStart until periodEnd }
                    
                    if (periodData.isNotEmpty()) {
                        // Calculate average heart rate for this period
                        val avgHeartRate = periodData.map { it.value }.average().toFloat()
                        entries.add(Entry(period.toFloat(), avgHeartRate))
                    }
                    
                    // Format period for X axis
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = periodStart
                    xLabels.add(dayFormatter.format(cal.time))
                }
            }
        }
        
        // Configure X axis labels
        heartRateChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < xLabels.size) xLabels[index] else ""
            }
        }
        
        // Create dataset
        if (entries.isNotEmpty()) {
            val dataSet = LineDataSet(entries, "Heart Rate")
            dataSet.apply {
                color = ContextCompat.getColor(this@HeartRateActivity, R.color.heart_rate_red)
                lineWidth = 2f
                setDrawCircles(true)
                setDrawCircleHole(false)
                circleRadius = 3f
                setCircleColor(ContextCompat.getColor(this@HeartRateActivity, R.color.heart_rate_red))
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = ContextCompat.getColor(this@HeartRateActivity, R.color.heart_rate_red)
                fillAlpha = 50
            }
            
            // Create line data and set to chart
            val dataSets = ArrayList<ILineDataSet>()
            dataSets.add(dataSet)
            val lineData = LineData(dataSets)
            heartRateChart.data = lineData
        } else {
            // No data
            heartRateChart.data = null
        }
        
        // Refresh chart
        heartRateChart.invalidate()
    }
    
    private fun updateHeartRateHistory(data: List<HeartRateData>) {
        // Clear current history
        heartRateHistory.clear()
        
        // Add all data to history
        heartRateHistory.addAll(data.sortedByDescending { it.timestamp })
        
        // Notify adapter
        heartRateAdapter.notifyDataSetChanged()
    }
    
    private fun updateHeartRateZones(data: List<HeartRateData>) {
        // Calculate time spent in each zone
        var lightTime = 0L
        var weightTime = 0L
        var aerobicTime = 0L
        var anaerobicTime = 0L
        var vo2maxTime = 0L
        
        // Assume each reading represents 5 minutes
        val timePerReading = TimeUnit.MINUTES.toMillis(5)
        
        // Count readings in each zone
        for (heartRate in data) {
            when (heartRate.value) {
                in 0..112 -> lightTime += timePerReading
                in 113..131 -> weightTime += timePerReading
                in 132..150 -> aerobicTime += timePerReading
                in 151..168 -> anaerobicTime += timePerReading
                else -> vo2maxTime += timePerReading
            }
        }
        
        // Calculate total time
        val totalTime = lightTime + weightTime + aerobicTime + anaerobicTime + vo2maxTime
        
        // Update progress views (width based on percentage)
        if (totalTime > 0) {
            updateZoneProgress(progressLight, lightTime, totalTime)
            updateZoneProgress(progressWeight, weightTime, totalTime)
            updateZoneProgress(progressAerobic, aerobicTime, totalTime)
            updateZoneProgress(progressAnaerobic, anaerobicTime, totalTime)
            updateZoneProgress(progressVO2Max, vo2maxTime, totalTime)
        } else {
            // Reset all zones
            resetHeartRateZones()
        }
        
        // Update time labels
        tvLightTime.text = formatDuration(lightTime)
        tvWeightTime.text = formatDuration(weightTime)
        tvAerobicTime.text = formatDuration(aerobicTime)
        tvAnaerobicTime.text = formatDuration(anaerobicTime)
        tvVO2MaxTime.text = formatDuration(vo2maxTime)
    }
    
    private fun updateZoneProgress(view: View, zoneTime: Long, totalTime: Long) {
        val percentage = (zoneTime.toFloat() / totalTime.toFloat()) * 100f
        val params = view.layoutParams
        params.width = (percentage * 6).toInt() // Scale factor for better visualization
        view.layoutParams = params
    }
    
    private fun resetHeartRateZones() {
        // Reset all zone progress views
        val params = progressLight.layoutParams
        params.width = 0
        progressLight.layoutParams = params
        
        // Create new LayoutParams for each view with width=0
        val weightParams = ViewGroup.LayoutParams(0, params.height)
        val aerobicParams = ViewGroup.LayoutParams(0, params.height)
        val anaerobicParams = ViewGroup.LayoutParams(0, params.height)
        val vo2maxParams = ViewGroup.LayoutParams(0, params.height)
        
        progressWeight.layoutParams = weightParams
        progressAerobic.layoutParams = aerobicParams
        progressAnaerobic.layoutParams = anaerobicParams
        progressVO2Max.layoutParams = vo2maxParams
        
        // Reset time labels
        tvLightTime.text = "0 minutes"
        tvWeightTime.text = "0 minutes"
        tvAerobicTime.text = "0 minutes"
        tvAnaerobicTime.text = "0 minutes"
        tvVO2MaxTime.text = "0 minutes"
    }
    
    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        
        return when {
            hours > 0 -> "${hours}H ${minutes}M"
            else -> "${minutes} minutes"
        }
    }
    
    private fun setTimeRange(timeRange: TimeRange) {
        if (currentTimeRange != timeRange) {
            currentTimeRange = timeRange
            loadHeartRateData()
        }
    }
    
    /**
     * Adapter for heart rate history list
     */
    inner class HeartRateHistoryAdapter(private val items: List<HeartRateData>) : 
            RecyclerView.Adapter<HeartRateHistoryAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvHeartRate: TextView = view.findViewById(R.id.tvHeartRate)
            val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_heart_rate_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvHeartRate.text = item.value.toString()
            
            // Format date and time
            val date = Date(item.timestamp)
            holder.tvTimestamp.text = timeFormatter.format(date)
            
            // Format date as MM/DD
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = item.timestamp
            holder.tvDate.text = String.format("%02d/%02d", 
                calendar.get(Calendar.MONTH) + 1, 
                calendar.get(Calendar.DAY_OF_MONTH))
            
            // Set click listener
            holder.itemView.setOnClickListener {
                // Show details or navigate to detailed view
                Toast.makeText(this@HeartRateActivity, 
                    "Heart Rate: ${item.value} BPM at ${dateFormatter.format(date)}", 
                    Toast.LENGTH_SHORT).show()
            }
        }
        
        override fun getItemCount() = items.size
    }
}