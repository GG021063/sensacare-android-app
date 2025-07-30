package com.sensacare.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.sqrt

/**
 * TemperatureActivity
 *
 * Displays body temperature data from connected health device.
 * Shows current temperature vs baseline trend, provides visual indicators
 * for abnormal readings, and offers health insights based on temperature patterns.
 */
class TemperatureActivity : AppCompatActivity(), OnChartValueSelectedListener {

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var tvCurrentTemp: TextView
    private lateinit var tvTempStatus: TextView
    private lateinit var tvTempAvg: TextView
    private lateinit var tvTempMin: TextView
    private lateinit var tvTempMax: TextView
    private lateinit var tvTempDeviation: TextView
    private lateinit var tvInsight: TextView
    private lateinit var tempChart: LineChart
    private lateinit var baselineChart: LineChart
    private lateinit var btnToggleUnit: Button
    private lateinit var cardCurrentTemp: CardView
    private lateinit var cardBaseline: CardView
    private lateinit var cardInsight: CardView
    
    // Data
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var tempValues = mutableListOf<Pair<Date, Float>>()
    private var baselineTempValues = mutableListOf<Pair<Date, Float>>()
    private var avgTemp = 0f
    private var minTemp = 0f
    private var maxTemp = 0f
    private var tempDeviation = 0f
    private var useCelsius = true // Default to Celsius
    
    // Data refresh handler
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshTemperatureData()
            handler.postDelayed(this, REFRESH_INTERVAL)
        }
    }
    
    // Constants
    companion object {
        private const val TAG = "TemperatureActivity"
        private const val REFRESH_INTERVAL = 30000L // 30 seconds
        
        // Temperature thresholds in Celsius
        private const val TEMP_HYPOTHERMIA_THRESHOLD = 35.0f
        private const val TEMP_LOW_THRESHOLD = 36.1f
        private const val TEMP_NORMAL_MIN = 36.5f
        private const val TEMP_NORMAL_MAX = 37.5f
        private const val TEMP_ELEVATED_THRESHOLD = 38.0f
        private const val TEMP_FEVER_THRESHOLD = 38.5f
        private const val TEMP_HIGH_FEVER_THRESHOLD = 39.5f
        
        // Temperature insights
        private val TEMP_INSIGHTS = arrayOf(
            "Your body temperature is within normal range, indicating good overall health.",
            "Slight elevation detected – consider resting and staying hydrated.",
            "Your temperature is elevated. Consider taking a fever reducer if you're feeling uncomfortable.",
            "Temperature is above normal range. Monitor for other symptoms and consider contacting your healthcare provider if it persists.",
            "Temperature is below normal range. This could be due to environmental factors or measurement error. If you feel cold or unwell, consider warming up.",
            "Your temperature shows normal daily fluctuations, typically lower in the morning and higher in the evening.",
            "Temperature readings are consistent throughout the day, indicating stable body regulation.",
            "Recent temperature trend shows improvement, returning to your normal baseline."
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature)
        
        try {
            // Get device info from intent
            deviceAddress = intent.getStringExtra("device_address")
            deviceName = intent.getStringExtra("device_name")
            
            // Initialize UI components
            initializeViews()
            
            // Set up toolbar
            setupToolbar()
            
            // Load initial data
            loadTemperatureData()
            
            // Set up charts
            setupTemperatureChart()
            setupBaselineChart()
            
            // Set up unit toggle button
            btnToggleUnit.setOnClickListener {
                toggleTemperatureUnit()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing temperature view", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Initialize all UI components
     */
    private fun initializeViews() {
        try {
            // Toolbar
            toolbar = findViewById(R.id.toolbar)
            
            // TextViews
            tvCurrentTemp = findViewById(R.id.tvCurrentTemp)
            tvTempStatus = findViewById(R.id.tvTempStatus)
            tvTempAvg = findViewById(R.id.tvTempAvg)
            tvTempMin = findViewById(R.id.tvTempMin)
            tvTempMax = findViewById(R.id.tvTempMax)
            tvTempDeviation = findViewById(R.id.tvTempDeviation)
            tvInsight = findViewById(R.id.tvInsight)
            
            // Charts
            tempChart = findViewById(R.id.tempChart)
            baselineChart = findViewById(R.id.baselineChart)
            
            // Buttons
            btnToggleUnit = findViewById(R.id.btnToggleUnit)
            
            // Cards
            cardCurrentTemp = findViewById(R.id.cardCurrentTemp)
            cardBaseline = findViewById(R.id.cardBaseline)
            cardInsight = findViewById(R.id.cardInsight)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Set up toolbar with back button
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Body Temperature"
    }
    
    /**
     * Load temperature data from health data manager
     */
    private fun loadTemperatureData() {
        try {
            // Check if device is connected
            if (deviceAddress == null) {
                Toast.makeText(this, "Please connect your device to view temperature data", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Get temperature data
            val tempData = getTemperatureData()
            
            if (tempData.isNotEmpty()) {
                // Process temperature data
                processTemperatureData(tempData)
                
                // Update UI with temperature data
                updateTemperatureUI()
            } else {
                // No data available, show message
                Toast.makeText(this, "No temperature data available", Toast.LENGTH_SHORT).show()
            }
            
            // Start periodic refresh
            startPeriodicRefresh()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading temperature data: ${e.message}", e)
            Toast.makeText(this, "Error loading temperature data", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Get temperature data - placeholder method
     */
    private fun getTemperatureData(): List<Pair<Date, Float>> {
        // This is a placeholder method that would normally fetch data from HealthDataManager
        // For now, return simulated data
        val result = mutableListOf<Pair<Date, Float>>()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7) // Start from 7 days ago
        
        for (i in 0 until 24) {
            calendar.add(Calendar.HOUR, 1)
            val temp = 36.5f + (Random().nextFloat() * 1.5f - 0.75f) // Random temp between 35.75 and 37.25
            result.add(Pair(calendar.time, temp))
        }
        
        return result
    }
    
    /**
     * Process temperature data
     */
    private fun processTemperatureData(data: List<Pair<Date, Float>>) {
        try {
            // Clear existing data
            tempValues.clear()
            
            // Add new data
            tempValues.addAll(data)
            
            // Sort by date
            tempValues.sortBy { it.first }
            
            // Calculate statistics
            calculateTemperatureStatistics()
            
            // Generate baseline data if not available
            generateBaselineData()
            
            // Generate insight
            generateInsight()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing temperature data: ${e.message}", e)
        }
    }
    
    /**
     * Calculate temperature statistics (average, min, max, deviation)
     */
    private fun calculateTemperatureStatistics() {
        try {
            if (tempValues.isEmpty()) {
                avgTemp = 0f
                minTemp = 0f
                maxTemp = 0f
                tempDeviation = 0f
                return
            }
            
            // Calculate average - Fix: cast Float to Double for sumOf
            val sum = tempValues.sumOf { it.second.toDouble() }
            avgTemp = (sum / tempValues.size).toFloat()
            
            // Calculate min and max
            minTemp = tempValues.minOf { it.second }
            maxTemp = tempValues.maxOf { it.second }
            
            // Calculate deviation
            val squaredDifferences = tempValues.sumOf { ((it.second - avgTemp) * (it.second - avgTemp)).toDouble() }
            tempDeviation = sqrt(squaredDifferences / tempValues.size).toFloat()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating temperature statistics: ${e.message}", e)
        }
    }
    
    /**
     * Generate baseline data for comparison
     * In a real app, this would come from a database or API
     */
    private fun generateBaselineData() {
        try {
            // Clear existing baseline data
            baselineTempValues.clear()
            
            // Get current date
            val calendar = Calendar.getInstance()
            val today = calendar.time
            
            // Reset to start of day
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            
            // Generate baseline data for a typical day (24 hours)
            for (hour in 0..23) {
                // Set hour
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                val time = calendar.time
                
                // Normal body temperature follows a circadian rhythm
                // Typically lowest around 4-5 AM and highest in late afternoon/evening
                val baseTemp = when (hour) {
                    in 0..5 -> 36.4f + (Random().nextFloat() * 0.2f - 0.1f) // Early morning: lower
                    in 6..11 -> 36.6f + (Random().nextFloat() * 0.2f - 0.1f) // Morning: rising
                    in 12..17 -> 36.8f + (Random().nextFloat() * 0.2f - 0.1f) // Afternoon: higher
                    else -> 36.7f + (Random().nextFloat() * 0.2f - 0.1f) // Evening: starting to decline
                }
                
                baselineTempValues.add(Pair(time, baseTemp))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating baseline data: ${e.message}", e)
        }
    }
    
    /**
     * Generate insight based on temperature data
     */
    private fun generateInsight() {
        try {
            if (tempValues.isEmpty()) {
                tvInsight.text = "Connect your device to get temperature insights."
                return
            }
            
            val currentTemp = tempValues.last().second
            
            // Generate insight based on current temperature
            val insight = when {
                currentTemp < TEMP_HYPOTHERMIA_THRESHOLD -> "Your temperature is significantly below normal range. This could indicate hypothermia. Please seek medical attention if you're feeling unwell."
                currentTemp < TEMP_LOW_THRESHOLD -> "Your temperature is below normal range. This could be due to environmental factors or measurement error. If you feel cold or unwell, consider warming up."
                currentTemp in TEMP_NORMAL_MIN..TEMP_NORMAL_MAX -> "Your body temperature is within normal range, indicating good overall health."
                currentTemp < TEMP_ELEVATED_THRESHOLD -> "Slight elevation detected – consider resting and staying hydrated."
                currentTemp < TEMP_FEVER_THRESHOLD -> "Your temperature is elevated. Consider taking a fever reducer if you're feeling uncomfortable."
                currentTemp < TEMP_HIGH_FEVER_THRESHOLD -> "You have a fever. Rest, stay hydrated, and consider contacting your healthcare provider if it persists or worsens."
                else -> "You have a high fever. Please contact your healthcare provider for guidance, especially if accompanied by other symptoms."
            }
            
            // Set insight text
            tvInsight.text = insight
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating insight: ${e.message}", e)
        }
    }
    
    /**
     * Update UI with temperature data
     */
    private fun updateTemperatureUI() {
        try {
            // Get current temperature (latest value)
            val currentTemp = if (tempValues.isNotEmpty()) tempValues.last().second else 0f
            
            // Update current temperature based on selected unit
            if (useCelsius) {
                tvCurrentTemp.text = String.format("%.1f°C", currentTemp)
            } else {
                // Convert to Fahrenheit
                val tempF = celsiusToFahrenheit(currentTemp)
                tvCurrentTemp.text = String.format("%.1f°F", tempF)
            }
            
            // Update status
            updateTemperatureStatus(currentTemp)
            
            // Update statistics based on selected unit
            if (useCelsius) {
                tvTempAvg.text = String.format("%.1f°C", avgTemp)
                tvTempMin.text = String.format("%.1f°C", minTemp)
                tvTempMax.text = String.format("%.1f°C", maxTemp)
                tvTempDeviation.text = String.format("±%.1f°C", tempDeviation)
            } else {
                tvTempAvg.text = String.format("%.1f°F", celsiusToFahrenheit(avgTemp))
                tvTempMin.text = String.format("%.1f°F", celsiusToFahrenheit(minTemp))
                tvTempMax.text = String.format("%.1f°F", celsiusToFahrenheit(maxTemp))
                tvTempDeviation.text = String.format("±%.1f°F", tempDeviation * 1.8f)
            }
            
            // Update charts
            updateTemperatureChart()
            updateBaselineChart()
            
            // Update unit toggle button text
            btnToggleUnit.text = if (useCelsius) "°F" else "°C"
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating temperature UI: ${e.message}", e)
        }
    }
    
    /**
     * Update temperature status based on current value
     */
    private fun updateTemperatureStatus(tempValue: Float) {
        try {
            val statusText: String
            val statusColor: Int
            
            when {
                tempValue < TEMP_HYPOTHERMIA_THRESHOLD -> {
                    statusText = "Hypothermia Risk"
                    statusColor = ContextCompat.getColor(this, R.color.error)
                }
                tempValue < TEMP_LOW_THRESHOLD -> {
                    statusText = "Below Normal"
                    statusColor = ContextCompat.getColor(this, R.color.warning)
                }
                tempValue in TEMP_NORMAL_MIN..TEMP_NORMAL_MAX -> {
                    statusText = "Normal"
                    statusColor = ContextCompat.getColor(this, R.color.success)
                }
                tempValue < TEMP_ELEVATED_THRESHOLD -> {
                    statusText = "Slightly Elevated"
                    statusColor = ContextCompat.getColor(this, R.color.warning)
                }
                tempValue < TEMP_FEVER_THRESHOLD -> {
                    statusText = "Elevated"
                    statusColor = ContextCompat.getColor(this, R.color.warning)
                }
                tempValue < TEMP_HIGH_FEVER_THRESHOLD -> {
                    statusText = "Fever"
                    statusColor = ContextCompat.getColor(this, R.color.error)
                }
                else -> {
                    statusText = "High Fever"
                    statusColor = ContextCompat.getColor(this, R.color.error)
                }
            }
            
            // Update status text and color
            tvTempStatus.text = statusText
            tvTempStatus.setBackgroundColor(statusColor)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating temperature status: ${e.message}", e)
        }
    }
    
    /**
     * Set up temperature chart
     */
    private fun setupTemperatureChart() {
        try {
            // Configure chart appearance
            with(tempChart) {
                description.isEnabled = false
                setTouchEnabled(true)
                setDrawGridBackground(false)
                setDrawBorders(false)
                setScaleEnabled(true)
                setPinchZoom(true)
                setBackgroundColor(Color.WHITE)
                setOnChartValueSelectedListener(this@TemperatureActivity)
                
                // Configure legend
                legend.isEnabled = true
                
                // Configure X axis
                val xAxis = xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                
                // Configure left Y axis
                val leftAxis = axisLeft
                leftAxis.setDrawGridLines(true)
                
                // Configure right Y axis
                axisRight.isEnabled = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up temperature chart: ${e.message}", e)
        }
    }
    
    /**
     * Set up baseline chart
     */
    private fun setupBaselineChart() {
        try {
            // Configure chart appearance
            with(baselineChart) {
                description.isEnabled = false
                setTouchEnabled(true)
                setDrawGridBackground(false)
                setDrawBorders(false)
                setScaleEnabled(true)
                setPinchZoom(true)
                setBackgroundColor(Color.WHITE)
                
                // Configure legend
                legend.isEnabled = true
                
                // Configure X axis
                val xAxis = xAxis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.granularity = 1f
                
                // Configure left Y axis
                val leftAxis = axisLeft
                leftAxis.setDrawGridLines(true)
                
                // Configure right Y axis
                axisRight.isEnabled = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up baseline chart: ${e.message}", e)
        }
    }
    
    /**
     * Update temperature chart with current data
     */
    private fun updateTemperatureChart() {
        try {
            if (tempValues.isEmpty()) {
                tempChart.clear()
                tempChart.setNoDataText("No temperature data available")
                return
            }
            
            // Create entries for chart
            val entries = ArrayList<Entry>()
            val timeLabels = ArrayList<String>()
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            // Add entries for each temperature value
            for (i in tempValues.indices) {
                val tempValue = tempValues[i]
                val yValue = if (useCelsius) tempValue.second else celsiusToFahrenheit(tempValue.second)
                entries.add(Entry(i.toFloat(), yValue))
                timeLabels.add(dateFormat.format(tempValue.first))
            }
            
            // Create dataset
            val dataSet = LineDataSet(entries, if (useCelsius) "Temperature (°C)" else "Temperature (°F)")
            dataSet.color = ContextCompat.getColor(this, R.color.sensacare_blue)
            dataSet.setCircleColor(ContextCompat.getColor(this, R.color.sensacare_blue))
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 3f
            dataSet.setDrawCircleHole(false)
            dataSet.valueTextSize = 9f
            dataSet.setDrawFilled(true)
            dataSet.fillColor = ContextCompat.getColor(this, R.color.sensacare_blue_transparent)
            dataSet.fillAlpha = 50
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            
            // Create line data
            val lineData = LineData(dataSet)
            
            // Set data to chart
            tempChart.data = lineData
            
            // Set X axis labels
            tempChart.xAxis.valueFormatter = IndexAxisValueFormatter(timeLabels)
            
            // Set Y axis range based on temperature unit
            val yAxis = tempChart.axisLeft
            if (useCelsius) {
                yAxis.axisMinimum = 35f
                yAxis.axisMaximum = 40f
            } else {
                yAxis.axisMinimum = celsiusToFahrenheit(35f)
                yAxis.axisMaximum = celsiusToFahrenheit(40f)
            }
            
            // Refresh chart
            tempChart.invalidate()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating temperature chart: ${e.message}", e)
        }
    }
    
    /**
     * Update baseline chart with comparison data
     */
    private fun updateBaselineChart() {
        try {
            if (baselineTempValues.isEmpty() || tempValues.isEmpty()) {
                baselineChart.clear()
                baselineChart.setNoDataText("No baseline data available")
                return
            }
            
            // Create entries for baseline
            val baselineEntries = ArrayList<Entry>()
            val currentEntries = ArrayList<Entry>()
            val timeLabels = ArrayList<String>()
            val hourFormat = SimpleDateFormat("HH:00", Locale.getDefault())
            
            // Add entries for baseline data
            for (i in baselineTempValues.indices) {
                val baselineValue = baselineTempValues[i]
                val yValue = if (useCelsius) baselineValue.second else celsiusToFahrenheit(baselineValue.second)
                baselineEntries.add(Entry(i.toFloat(), yValue))
                timeLabels.add(hourFormat.format(baselineValue.first))
            }
            
            // Create baseline dataset
            val baselineDataSet = LineDataSet(baselineEntries, "Baseline")
            baselineDataSet.color = ContextCompat.getColor(this, R.color.text_secondary)
            baselineDataSet.setCircleColor(ContextCompat.getColor(this, R.color.text_secondary))
            baselineDataSet.lineWidth = 1.5f
            baselineDataSet.circleRadius = 2f
            baselineDataSet.setDrawCircleHole(false)
            baselineDataSet.valueTextSize = 0f // Hide values
            baselineDataSet.enableDashedLine(10f, 5f, 0f)
            baselineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            
            // Create entries for current day data
            // Map current data to hourly buckets for comparison
            val hourlyData = mutableMapOf<Int, MutableList<Float>>()
            
            for (tempValue in tempValues) {
                val calendar = Calendar.getInstance()
                calendar.time = tempValue.first
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                
                if (!hourlyData.containsKey(hour)) {
                    hourlyData[hour] = mutableListOf()
                }
                
                hourlyData[hour]?.add(tempValue.second)
            }
            
            // Calculate average for each hour that has data
            for (hour in 0..23) {
                if (hourlyData.containsKey(hour)) {
                    val values = hourlyData[hour]!!
                    val avgValue = values.sumOf { it.toDouble() }.toFloat() / values.size
                    val yValue = if (useCelsius) avgValue else celsiusToFahrenheit(avgValue)
                    currentEntries.add(Entry(hour.toFloat(), yValue))
                }
            }
            
            // Create current dataset
            val currentDataSet = LineDataSet(currentEntries, "Today")
            currentDataSet.color = ContextCompat.getColor(this, R.color.sensacare_primary_light)
            currentDataSet.setCircleColor(ContextCompat.getColor(this, R.color.sensacare_primary_light))
            currentDataSet.lineWidth = 2f
            currentDataSet.circleRadius = 3f
            currentDataSet.setDrawCircleHole(false)
            currentDataSet.valueTextSize = 9f
            currentDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            
            // Create line data with both datasets
            val lineData = LineData(baselineDataSet, currentDataSet)
            
            // Set data to chart
            baselineChart.data = lineData
            
            // Set X axis labels (show only every 3 hours to avoid crowding)
            val filteredLabels = timeLabels.filterIndexed { index, _ -> index % 3 == 0 }
            baselineChart.xAxis.valueFormatter = IndexAxisValueFormatter(filteredLabels)
            baselineChart.xAxis.labelCount = filteredLabels.size
            
            // Set Y axis range based on temperature unit
            val yAxis = baselineChart.axisLeft
            if (useCelsius) {
                yAxis.axisMinimum = 35f
                yAxis.axisMaximum = 40f
            } else {
                yAxis.axisMinimum = celsiusToFahrenheit(35f)
                yAxis.axisMaximum = celsiusToFahrenheit(40f)
            }
            
            // Refresh chart
            baselineChart.invalidate()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating baseline chart: ${e.message}", e)
        }
    }
    
    /**
     * Toggle between Celsius and Fahrenheit
     */
    private fun toggleTemperatureUnit() {
        try {
            // Toggle unit
            useCelsius = !useCelsius
            
            // Update UI with new unit
            updateTemperatureUI()
            
            // Show toast with new unit
            val unitName = if (useCelsius) "Celsius" else "Fahrenheit"
            Toast.makeText(this, "Switched to $unitName", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling temperature unit: ${e.message}", e)
        }
    }
    
    /**
     * Convert Celsius to Fahrenheit
     */
    private fun celsiusToFahrenheit(celsius: Float): Float {
        return celsius * 9f / 5f + 32f
    }
    
    /**
     * Refresh temperature data
     */
    private fun refreshTemperatureData() {
        try {
            // Check if device is connected
            if (deviceAddress == null) {
                Toast.makeText(this, "Please connect your device to refresh temperature data", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Get fresh temperature data
            val newTempData = getTemperatureData()
            
            if (newTempData.isNotEmpty()) {
                // Process new temperature data
                processTemperatureData(newTempData)
                
                // Update UI with new data
                updateTemperatureUI()
            } else {
                // No new data available
                Toast.makeText(this, "No new temperature data available", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing temperature data: ${e.message}", e)
            Toast.makeText(this, "Error refreshing temperature data", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Share temperature data
     */
    private fun shareTemperatureData() {
        try {
            // Create share text
            val shareText = buildShareText()
            
            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Temperature Data from Sensacare")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            
            // Start share activity
            startActivity(Intent.createChooser(shareIntent, "Share temperature data via"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing temperature data: ${e.message}", e)
            Toast.makeText(this, "Error sharing temperature data", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Build share text
     */
    private fun buildShareText(): String {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        val currentTemp = if (tempValues.isNotEmpty()) tempValues.last().second else 0f
        val tempUnit = if (useCelsius) "°C" else "°F"
        val tempValue = if (useCelsius) currentTemp else celsiusToFahrenheit(currentTemp)
        val avgValue = if (useCelsius) avgTemp else celsiusToFahrenheit(avgTemp)
        val minValue = if (useCelsius) minTemp else celsiusToFahrenheit(minTemp)
        val maxValue = if (useCelsius) maxTemp else celsiusToFahrenheit(maxTemp)
        
        return """
            Body Temperature Data - $currentDate
            
            Current Temperature: ${String.format("%.1f", tempValue)}$tempUnit
            Status: ${tvTempStatus.text}
            
            Daily Statistics:
            - Average: ${String.format("%.1f", avgValue)}$tempUnit
            - Minimum: ${String.format("%.1f", minValue)}$tempUnit
            - Maximum: ${String.format("%.1f", maxValue)}$tempUnit
            
            Health Insight:
            ${tvInsight.text}
            
            Shared from Sensacare Health Monitoring App
        """.trimIndent()
    }
    
    /**
     * Start periodic refresh of temperature data
     */
    private fun startPeriodicRefresh() {
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL)
    }
    
    /**
     * Stop periodic refresh of temperature data
     */
    private fun stopPeriodicRefresh() {
        handler.removeCallbacks(refreshRunnable)
    }
    
    /**
     * Handle chart value selected
     */
    override fun onValueSelected(e: Entry?, h: Highlight?) {
        // Handle chart value selection if needed
    }
    
    /**
     * Handle nothing selected on chart
     */
    override fun onNothingSelected() {
        // Handle nothing selected on chart if needed
    }
    
    /**
     * Handle options menu item selection
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    /**
     * Handle back button press
     */
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
    
    /**
     * Handle activity resume
     */
    override fun onResume() {
        super.onResume()
        // Refresh data when activity resumes
        refreshTemperatureData()
        // Start periodic refresh
        startPeriodicRefresh()
    }
    
    /**
     * Handle activity pause
     */
    override fun onPause() {
        super.onPause()
        // Stop periodic refresh when activity pauses
        stopPeriodicRefresh()
    }
    
    /**
     * Handle activity destroy
     */
    override fun onDestroy() {
        super.onDestroy()
        // Stop periodic refresh when activity is destroyed
        stopPeriodicRefresh()
    }
}
