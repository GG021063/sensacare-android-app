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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
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
import kotlin.math.max
import kotlin.math.min

/**
 * HRVActivity
 *
 * Displays Heart Rate Variability (HRV) data from connected health device.
 * Shows time-series graph of HRV measurements across the day, historical averages,
 * and provides AI insights about the user's HRV patterns.
 */
class HRVActivity : AppCompatActivity(), OnChartValueSelectedListener {

    // UI Components
    private lateinit var toolbar: Toolbar
    private lateinit var tvCurrentHRV: TextView
    private lateinit var tvHRVStatus: TextView
    private lateinit var tvHRVAvg: TextView
    private lateinit var tvHRVMin: TextView
    private lateinit var tvHRVMax: TextView
    private lateinit var tvHRVDeviation: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvAIInsight: TextView
    private lateinit var tvNoDataMessage: TextView
    private lateinit var hrvChart: LineChart
    private lateinit var historicalChart: LineChart
    private lateinit var btnShare: Button
    private lateinit var btnRefresh: ImageView
    private lateinit var cardCurrentHRV: CardView
    private lateinit var cardHistorical: CardView
    private lateinit var cardAIInsight: CardView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var contentLayout: LinearLayout
    private lateinit var statusIndicator: View
    
    // Data
    private var deviceAddress: String? = null
    private var deviceName: String? = null
    private var hrvValues = mutableListOf<Pair<Date, Int>>()
    private var historicalHRVValues = mutableListOf<Pair<Date, Int>>()
    private var avgHRV = 0
    private var minHRV = 0
    private var maxHRV = 0
    private var hrvDeviation = 0
    
    // Data refresh handler
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshHRVData()
            handler.postDelayed(this, REFRESH_INTERVAL)
        }
    }
    
    // Constants
    companion object {
        private const val TAG = "HRVActivity"
        private const val REFRESH_INTERVAL = 30000L // 30 seconds
        
        // HRV status thresholds (ms)
        private const val HRV_LOW_THRESHOLD = 20
        private const val HRV_NORMAL_THRESHOLD = 50
        private const val HRV_HIGH_THRESHOLD = 100
        
        // AI Insights
        private val HRV_INSIGHTS = arrayOf(
            "Your HRV is trending lower than your average. This could indicate increased stress levels. Consider taking a break for relaxation.",
            "HRV is within your normal range, suggesting good autonomic balance and recovery.",
            "Your HRV is higher than usual, which often indicates good recovery and low stress levels.",
            "HRV shows some variability throughout the day. This is normal and reflects your body's response to different activities.",
            "Your nighttime HRV is higher than daytime, which is a healthy pattern indicating good recovery during sleep.",
            "Recent trend shows improving HRV, suggesting your body is adapting well to your current lifestyle.",
            "Lower morning HRV may indicate incomplete recovery. Consider additional rest today.",
            "Your HRV pattern suggests good parasympathetic nervous system activity, associated with relaxation and recovery."
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hrv)
        
        try {
            // Get device info from intent
            deviceAddress = intent.getStringExtra("device_address")
            deviceName = intent.getStringExtra("device_name")
            
            // Initialize UI components
            initializeViews()
            
            // Set up toolbar
            setupToolbar()
            
            // Initialize health data manager
            
            // Load initial data
            loadHRVData()
            
            // Set up charts
            setupHRVChart()
            setupHistoricalChart()
            
            // Set up refresh button
            btnRefresh.setOnClickListener {
                refreshHRVData()
                Toast.makeText(this, "Refreshing HRV data...", Toast.LENGTH_SHORT).show()
            }
            
            // Set up share button
            btnShare.setOnClickListener {
                shareHRVData()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing HRV view", Toast.LENGTH_SHORT).show()
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
            tvCurrentHRV = findViewById(R.id.tvCurrentHRV)
            tvHRVStatus = findViewById(R.id.tvHRVStatus)
            tvHRVAvg = findViewById(R.id.tvHRVAvg)
            tvHRVMin = findViewById(R.id.tvHRVMin)
            tvHRVMax = findViewById(R.id.tvHRVMax)
            tvHRVDeviation = findViewById(R.id.tvHRVDeviation)
            tvLastUpdated = findViewById(R.id.tvLastUpdated)
            tvAIInsight = findViewById(R.id.tvAIInsight)
            tvNoDataMessage = findViewById(R.id.tvNoDataMessage)
            
            // Charts
            hrvChart = findViewById(R.id.hrvChart)
            historicalChart = findViewById(R.id.historicalChart)
            
            // Buttons
            btnShare = findViewById(R.id.btnShare)
            btnRefresh = findViewById(R.id.btnRefresh)
            
            // Cards
            cardCurrentHRV = findViewById(R.id.cardCurrentHRV)
            cardHistorical = findViewById(R.id.cardHistorical)
            cardAIInsight = findViewById(R.id.cardAIInsight)
            
            // Layouts
            loadingLayout = findViewById(R.id.loadingLayout)
            contentLayout = findViewById(R.id.contentLayout)
            
            // Status indicator
            statusIndicator = findViewById(R.id.statusIndicator)
            
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
        supportActionBar?.title = "Heart Rate Variability"
    }
    
    /**
     * Load HRV data from health data manager
     */
    private fun loadHRVData() {
        try {
            // Show loading state
            showLoading(true)
            
            // Check if device is connected
            if (deviceAddress == null) {
                showNoDataMessage("Please connect your device to view HRV data")
                return
            }
            
            // Get HRV data from health data manager
            val hrvData = getHRVData()
            
            if (hrvData.isNotEmpty()) {
                // Process HRV data
                processHRVData(hrvData)
                
                // Update UI with HRV data
                updateHRVUI()
                
                // Show content
                showLoading(false)
            } else {
                // No data available, show message
                showNoDataMessage("No HRV data available. Please make sure your device is connected and supports HRV measurement.")
            }
            
            // Start periodic refresh
            startPeriodicRefresh()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading HRV data: ${e.message}", e)
            showNoDataMessage("Error loading HRV data. Please try again.")
        }
    }
    
    /**
     * Process HRV data from health data manager
     */
    private fun processHRVData(data: List<Pair<Date, Int>>) {
        try {
            // Clear existing data
            hrvValues.clear()
            
            // Add new data
            hrvValues.addAll(data)
            
            // Sort by date
            hrvValues.sortBy { it.first }
            
            // Calculate statistics
            calculateHRVStatistics()
            
            // Generate historical data if not available
            generateHistoricalData()
            
            // Generate AI insight
            generateAIInsight()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing HRV data: ${e.message}", e)
        }
    }
    
    /**
     * Calculate HRV statistics (average, min, max, deviation)
     */
    private fun calculateHRVStatistics() {
        try {
            if (hrvValues.isEmpty()) {
                avgHRV = 0
                minHRV = 0
                maxHRV = 0
                hrvDeviation = 0
                return
            }
            
            // Calculate average
            val sum = hrvValues.sumOf { it.second }
            avgHRV = sum / hrvValues.size
            
            // Calculate min and max
            minHRV = hrvValues.minOf { it.second }
            maxHRV = hrvValues.maxOf { it.second }
            
            // Calculate deviation
            val squaredDifferences = hrvValues.sumOf { (it.second - avgHRV) * (it.second - avgHRV) }
            hrvDeviation = kotlin.math.sqrt(squaredDifferences.toDouble() / hrvValues.size).toInt()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating HRV statistics: ${e.message}", e)
        }
    }
    
    /**
     * Generate historical data for comparison
     * In a real app, this would come from a database or API
     */
    private fun generateHistoricalData() {
        try {
            // Clear existing historical data
            historicalHRVValues.clear()
            
            // Get current date
            val calendar = Calendar.getInstance()
            
            // Generate data for the past 30 days
            for (i in 30 downTo 1) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                val date = calendar.time
                
                // Generate a random HRV value between 20 and 80
                val baseHRV = 50
                val randomVariation = Random().nextInt(31) - 15 // -15 to +15
                val hrvValue = max(20, min(80, baseHRV + randomVariation))
                
                historicalHRVValues.add(Pair(date, hrvValue))
            }
            
            // Sort by date
            historicalHRVValues.sortBy { it.first }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating historical data: ${e.message}", e)
        }
    }
    
    /**
     * Generate AI insight based on HRV data
     */
    private fun generateAIInsight() {
        try {
            // Get a random insight for now
            // In a real app, this would be based on actual analysis of the data
            val randomIndex = Random().nextInt(HRV_INSIGHTS.size)
            val insight = HRV_INSIGHTS[randomIndex]
            
            // Set insight text
            tvAIInsight.text = insight
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating AI insight: ${e.message}", e)
        }
    }
    
    /**
     * Update UI with HRV data
     */
    private fun updateHRVUI() {
        try {
            // Get current HRV (latest value)
            val currentHRV = if (hrvValues.isNotEmpty()) hrvValues.last().second else 0
            
            // Update current HRV
            tvCurrentHRV.text = "$currentHRV ms"
            
            // Update status
            updateHRVStatus(currentHRV)
            
            // Update statistics
            tvHRVAvg.text = "$avgHRV ms"
            tvHRVMin.text = "$minHRV ms"
            tvHRVMax.text = "$maxHRV ms"
            tvHRVDeviation.text = "±$hrvDeviation ms"
            
            // Update last updated time
            val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val lastUpdatedTime = if (hrvValues.isNotEmpty()) {
                dateFormat.format(hrvValues.last().first)
            } else {
                "N/A"
            }
            tvLastUpdated.text = "Last updated: $lastUpdatedTime"
            
            // Update charts
            updateHRVChart()
            updateHistoricalChart()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating HRV UI: ${e.message}", e)
        }
    }
    
    /**
     * Update HRV status based on current value
     */
    private fun updateHRVStatus(hrvValue: Int) {
        try {
            val statusText: String
            val statusColor: Int
            
            when {
                hrvValue < HRV_LOW_THRESHOLD -> {
                    statusText = "Low"
                    statusColor = ContextCompat.getColor(this, R.color.warning)
                }
                hrvValue < HRV_NORMAL_THRESHOLD -> {
                    statusText = "Normal"
                    statusColor = ContextCompat.getColor(this, R.color.success)
                }
                hrvValue < HRV_HIGH_THRESHOLD -> {
                    statusText = "Good"
                    statusColor = ContextCompat.getColor(this, R.color.success)
                }
                else -> {
                    statusText = "Excellent"
                    statusColor = ContextCompat.getColor(this, R.color.success)
                }
            }
            
            // Update status text and color
            tvHRVStatus.text = statusText
            tvHRVStatus.setTextColor(statusColor)
            
            // Update status indicator
            statusIndicator.setBackgroundColor(statusColor)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating HRV status: ${e.message}", e)
        }
    }
    
    /**
     * Set up HRV chart
     */
    private fun setupHRVChart() {
        try {
            // Configure chart appearance
            with(hrvChart) {
                description.isEnabled = false
                setTouchEnabled(true)
                setDrawGridBackground(false)
                setDrawBorders(false)
                setScaleEnabled(true)
                setPinchZoom(true)
                setBackgroundColor(Color.WHITE)
                setOnChartValueSelectedListener(this@HRVActivity)
                
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
                leftAxis.axisMinimum = 0f
                
                // Configure right Y axis
                axisRight.isEnabled = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up HRV chart: ${e.message}", e)
        }
    }
    
    /**
     * Set up historical chart
     */
    private fun setupHistoricalChart() {
        try {
            // Configure chart appearance
            with(historicalChart) {
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
                leftAxis.axisMinimum = 0f
                
                // Configure right Y axis
                axisRight.isEnabled = false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up historical chart: ${e.message}", e)
        }
    }
    
    /**
     * Update HRV chart with current data
     */
    private fun updateHRVChart() {
        try {
            if (hrvValues.isEmpty()) {
                hrvChart.clear()
                hrvChart.setNoDataText("No HRV data available")
                return
            }
            
            // Create entries for chart
            val entries = ArrayList<Entry>()
            val timeLabels = ArrayList<String>()
            val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            
            // Add entries for each HRV value
            for (i in hrvValues.indices) {
                val hrvValue = hrvValues[i]
                entries.add(Entry(i.toFloat(), hrvValue.second.toFloat()))
                timeLabels.add(dateFormat.format(hrvValue.first))
            }
            
            // Create dataset
            val dataSet = LineDataSet(entries, "HRV (ms)")
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
            hrvChart.data = lineData
            
            // Set X axis labels
            hrvChart.xAxis.valueFormatter = IndexAxisValueFormatter(timeLabels)
            
            // Refresh chart
            hrvChart.invalidate()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating HRV chart: ${e.message}", e)
        }
    }
    
    /**
     * Update historical chart with 30-day data
     */
    private fun updateHistoricalChart() {
        try {
            if (historicalHRVValues.isEmpty()) {
                historicalChart.clear()
                historicalChart.setNoDataText("No historical data available")
                return
            }
            
            // Create entries for chart
            val entries = ArrayList<Entry>()
            val dateLabels = ArrayList<String>()
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            
            // Add entries for each historical HRV value
            for (i in historicalHRVValues.indices) {
                val hrvValue = historicalHRVValues[i]
                entries.add(Entry(i.toFloat(), hrvValue.second.toFloat()))
                dateLabels.add(dateFormat.format(hrvValue.first))
            }
            
            // Create dataset
            val dataSet = LineDataSet(entries, "30-Day HRV Trend")
            dataSet.color = ContextCompat.getColor(this, R.color.sensacare_primary_light)
            dataSet.setCircleColor(ContextCompat.getColor(this, R.color.sensacare_primary_light))
            dataSet.lineWidth = 2f
            dataSet.circleRadius = 2f
            dataSet.setDrawCircleHole(false)
            dataSet.valueTextSize = 0f // Hide values
            dataSet.setDrawFilled(true)
            dataSet.fillColor = ContextCompat.getColor(this, R.color.sensacare_primary_light)
            dataSet.fillAlpha = 30
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            
            // Create line data
            val lineData = LineData(dataSet)
            
            // Set data to chart
            historicalChart.data = lineData
            
            // Set X axis labels (show only every 5 days to avoid crowding)
            val filteredLabels = dateLabels.filterIndexed { index, _ -> index % 5 == 0 }
            historicalChart.xAxis.valueFormatter = IndexAxisValueFormatter(filteredLabels)
            historicalChart.xAxis.labelCount = filteredLabels.size
            
            // Refresh chart
            historicalChart.invalidate()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating historical chart: ${e.message}", e)
        }
    }
    
    /**
     * Refresh HRV data
     */
    private fun refreshHRVData() {
        try {
            // Check if device is connected
            if (deviceAddress == null) {
                Toast.makeText(this, "Please connect your device to refresh HRV data", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Show loading indicator
            btnRefresh.isEnabled = false
            
            // Get fresh HRV data from health data manager
            val newHRVData = getHRVData()
            
            if (newHRVData.isNotEmpty()) {
                // Process new HRV data
                processHRVData(newHRVData)
                
                // Update UI with new data
                updateHRVUI()
                
                // Show content
                showLoading(false)
            } else {
                // No new data available
                Toast.makeText(this, "No new HRV data available", Toast.LENGTH_SHORT).show()
            }
            
            // Re-enable refresh button
            btnRefresh.isEnabled = true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing HRV data: ${e.message}", e)
            Toast.makeText(this, "Error refreshing HRV data", Toast.LENGTH_SHORT).show()
            btnRefresh.isEnabled = true
        }
    }
    
    /**
     * Share HRV data
     */
    private fun shareHRVData() {
        try {
            // Create share text
            val shareText = buildShareText()
            
            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My HRV Data from Sensacare")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            
            // Start share activity
            startActivity(Intent.createChooser(shareIntent, "Share HRV data via"))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing HRV data: ${e.message}", e)
            Toast.makeText(this, "Error sharing HRV data", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Build share text
     */
    private fun buildShareText(): String {
        val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        return """
            Heart Rate Variability (HRV) Data - $currentDate
            
            Current HRV: ${if (hrvValues.isNotEmpty()) hrvValues.last().second else 0} ms
            Status: ${tvHRVStatus.text}
            
            Daily Statistics:
            - Average: $avgHRV ms
            - Minimum: $minHRV ms
            - Maximum: $maxHRV ms
            - Deviation: ±$hrvDeviation ms
            
            AI Insight:
            ${tvAIInsight.text}
            
            Shared from Sensacare Health Monitoring App
        """.trimIndent()
    }
    
    /**
     * Show loading state
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingLayout.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
            tvNoDataMessage.visibility = View.GONE
        } else {
            loadingLayout.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
            tvNoDataMessage.visibility = View.GONE
        }
    }
    
    /**
     * Show no data message
     */
    private fun showNoDataMessage(message: String) {
        loadingLayout.visibility = View.GONE
        contentLayout.visibility = View.GONE
        tvNoDataMessage.visibility = View.VISIBLE
        tvNoDataMessage.text = message
    }
    
    /**
     * Start periodic refresh of HRV data
     */
    private fun startPeriodicRefresh() {
        handler.postDelayed(refreshRunnable, REFRESH_INTERVAL)
    }
    
    /**
     * Stop periodic refresh of HRV data
     */
    private fun stopPeriodicRefresh() {
        handler.removeCallbacks(refreshRunnable)
    }

    /**
     * Placeholder method to retrieve HRV data.
     * In the real implementation this would query the data manager / SDK.
     */
    private fun getHRVData(): List<Pair<Date, Int>> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, -24)
        val list = mutableListOf<Pair<Date, Int>>()

        // generate 24 hourly points of simulated HRV between 20-100 ms
        for (i in 0 until 24) {
            calendar.add(Calendar.HOUR, 1)
            val hrvValue = 40 + Random().nextInt(41)   // 40‒80 ms typical range
            list.add(Pair(calendar.time, hrvValue))
        }
        return list
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
        refreshHRVData()
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
