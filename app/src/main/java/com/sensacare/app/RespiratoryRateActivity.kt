package com.sensacare.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * RespiratoryRateActivity - Comprehensive respiratory rate monitoring and analysis
 * 
 * Features:
 * - Real-time respiratory rate display (breaths per minute)
 * - Historical respiratory rate chart showing 24-hour trend
 * - Statistics cards showing average, min, max respiratory rates
 * - Respiratory health insights and recommendations
 * - Normal range indicators (12-20 breaths/min for adults)
 * - Health status indicators (Normal, Low, High)
 * - Share functionality for respiratory rate data
 */
class RespiratoryRateActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var chartRespiratoryRate: LineChart
    private lateinit var tvCurrentRate: TextView
    private lateinit var tvRateStatus: TextView
    private lateinit var tvAvgRate: TextView
    private lateinit var tvMinRate: TextView
    private lateinit var tvMaxRate: TextView
    private lateinit var tvInsightTitle: TextView
    private lateinit var tvInsightContent: TextView
    private lateinit var btnShare: ImageButton
    private lateinit var ivStatusIndicator: ImageView
    private lateinit var cardCurrentRate: CardView
    private lateinit var cardStats: CardView
    private lateinit var cardInsights: CardView
    private lateinit var btnRefresh: Button
    private lateinit var progressLoading: View
    
    // Data
    private var respiratoryRateData = mutableListOf<RespiratoryRatePoint>()
    private var currentRate = 0
    private var avgRate = 0.0
    private var minRate = 0
    private var maxRate = 0
    
    // Constants
    companion object {
        private const val TAG = "RespiratoryRateActivity"
        private const val NORMAL_MIN_RATE = 12
        private const val NORMAL_MAX_RATE = 20
        private const val REFRESH_INTERVAL = 30000L // 30 seconds
        
        // Status categories
        private const val STATUS_LOW = "Low"
        private const val STATUS_NORMAL = "Normal"
        private const val STATUS_HIGH = "High"
    }
    
    // Handler for periodic updates
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            refreshData()
            handler.postDelayed(this, REFRESH_INTERVAL)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_respiratory_rate)
        
        try {
            // Set up action bar
            supportActionBar?.apply {
                title = "Respiratory Rate"
                setDisplayHomeAsUpEnabled(true)
            }
            
            // Initialize UI components
            initializeViews()
            
            // Set up chart
            setupChart()
            
            // Initial data load
            loadInitialData()
            
            // Set up refresh button
            btnRefresh.setOnClickListener {
                showLoading(true)
                refreshData()
            }
            
            // Set up share button
            btnShare.setOnClickListener {
                shareRespiratoryData()
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Start periodic updates
        handler.postDelayed(updateRunnable, REFRESH_INTERVAL)
    }
    
    override fun onPause() {
        super.onPause()
        // Stop periodic updates
        handler.removeCallbacks(updateRunnable)
    }
    
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
     * Initialize all UI components
     */
    private fun initializeViews() {
        chartRespiratoryRate = findViewById(R.id.chartRespiratoryRate)
        tvCurrentRate = findViewById(R.id.tvCurrentRate)
        tvRateStatus = findViewById(R.id.tvRateStatus)
        tvAvgRate = findViewById(R.id.tvAvgRate)
        tvMinRate = findViewById(R.id.tvMinRate)
        tvMaxRate = findViewById(R.id.tvMaxRate)
        tvInsightTitle = findViewById(R.id.tvInsightTitle)
        tvInsightContent = findViewById(R.id.tvInsightContent)
        btnShare = findViewById(R.id.btnShare)
        ivStatusIndicator = findViewById(R.id.ivStatusIndicator)
        cardCurrentRate = findViewById(R.id.cardCurrentRate)
        cardStats = findViewById(R.id.cardStats)
        cardInsights = findViewById(R.id.cardInsights)
        btnRefresh = findViewById(R.id.btnRefresh)
        progressLoading = findViewById(R.id.progressLoading)
    }
    
    /**
     * Configure the respiratory rate chart
     */
    private fun setupChart() {
        try {
            with(chartRespiratoryRate) {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                
                // X-axis setup
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = ContextCompat.getColor(this@RespiratoryRateActivity, R.color.text_primary)
                    setDrawGridLines(false)
                    valueFormatter = object : ValueFormatter() {
                        private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                        
                        override fun getFormattedValue(value: Float): String {
                            val date = Date(value.toLong())
                            return sdf.format(date)
                        }
                    }
                }
                
                // Left Y-axis setup
                axisLeft.apply {
                    textColor = ContextCompat.getColor(this@RespiratoryRateActivity, R.color.text_primary)
                    setDrawGridLines(true)
                    axisMinimum = 8f // Slightly below minimum normal
                    axisMaximum = 24f // Slightly above maximum normal
                }
                
                // Right Y-axis setup (disabled)
                axisRight.isEnabled = false
                
                // Legend setup
                legend.apply {
                    textColor = ContextCompat.getColor(this@RespiratoryRateActivity, R.color.text_primary)
                    verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
                    horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
                    orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
                    setDrawInside(false)
                }
                
                // Add normal range highlight
                val normalRangeSet = LineDataSet(listOf(), "")
                normalRangeSet.setDrawFilled(true)
                normalRangeSet.fillColor = ContextCompat.getColor(this@RespiratoryRateActivity, R.color.sensacare_blue_transparent)
                normalRangeSet.fillAlpha = 50
                
                // Animate chart
                animateX(1000)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error setting up chart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Load initial respiratory rate data (simulated for now)
     */
    private fun loadInitialData() {
        showLoading(true)
        
        // Simulate loading delay
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // Generate 24 hours of simulated data
                generateSimulatedData()
                
                // Update chart with data
                updateChart()
                
                // Update statistics
                updateStatistics()
                
                // Update current rate display
                updateCurrentRateDisplay()
                
                // Update insights based on data
                updateInsights()
                
                showLoading(false)
            } catch (e: Exception) {
                Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }, 1000)
    }
    
    /**
     * Refresh respiratory rate data (simulated for now, would use VeePoo SDK in production)
     */
    private fun refreshData() {
        try {
            // In a real implementation, this would fetch new data from the VeePoo SDK
            // For now, we'll just update the current rate with a simulated value
            
            // Get simulated current respiratory rate
            currentRate = getSimulatedCurrentRate()
            
            // Add to data list
            val calendar = Calendar.getInstance()
            respiratoryRateData.add(RespiratoryRatePoint(calendar.timeInMillis, currentRate))
            
            // Trim data to keep only last 24 hours
            val twentyFourHoursAgo = calendar.apply { add(Calendar.HOUR, -24) }.timeInMillis
            respiratoryRateData = respiratoryRateData.filter { it.timestamp >= twentyFourHoursAgo }.toMutableList()
            
            // Update UI
            updateChart()
            updateStatistics()
            updateCurrentRateDisplay()
            updateInsights()
            
            showLoading(false)
        } catch (e: Exception) {
            Toast.makeText(this, "Error refreshing data: ${e.message}", Toast.LENGTH_SHORT).show()
            showLoading(false)
        }
    }
    
    /**
     * Update the respiratory rate chart with current data
     */
    private fun updateChart() {
        try {
            if (respiratoryRateData.isEmpty()) return
            
            // Create entries for chart
            val entries = respiratoryRateData.map { 
                Entry(it.timestamp.toFloat(), it.rate.toFloat()) 
            }
            
            // Create dataset
            val dataSet = LineDataSet(entries, "Breaths per Minute")
            dataSet.apply {
                color = ContextCompat.getColor(this@RespiratoryRateActivity, R.color.sensacare_blue)
                setCircleColor(ContextCompat.getColor(this@RespiratoryRateActivity, R.color.sensacare_blue))
                lineWidth = 2f
                circleRadius = 3f
                setDrawCircleHole(false)
                valueTextSize = 9f
                setDrawValues(false)
                
                // Add gradient fill
                setDrawFilled(true)
                fillColor = ContextCompat.getColor(this@RespiratoryRateActivity, R.color.sensacare_blue_transparent)
                fillAlpha = 150
                
                // Highlight
                highLightColor = Color.RED
                setDrawHighlightIndicators(true)
            }
            
            // Add normal range reference lines
            val normalMinLine = LineDataSet(
                listOf(
                    Entry(respiratoryRateData.first().timestamp.toFloat(), NORMAL_MIN_RATE.toFloat()),
                    Entry(respiratoryRateData.last().timestamp.toFloat(), NORMAL_MIN_RATE.toFloat())
                ),
                "Min Normal"
            )
            normalMinLine.apply {
                color = Color.GREEN
                setDrawCircles(false)
                lineWidth = 1f
                enableDashedLine(10f, 5f, 0f)
                setDrawValues(false)
            }
            
            val normalMaxLine = LineDataSet(
                listOf(
                    Entry(respiratoryRateData.first().timestamp.toFloat(), NORMAL_MAX_RATE.toFloat()),
                    Entry(respiratoryRateData.last().timestamp.toFloat(), NORMAL_MAX_RATE.toFloat())
                ),
                "Max Normal"
            )
            normalMaxLine.apply {
                color = Color.GREEN
                setDrawCircles(false)
                lineWidth = 1f
                enableDashedLine(10f, 5f, 0f)
                setDrawValues(false)
            }
            
            // Create and set data
            val lineData = LineData(dataSet, normalMinLine, normalMaxLine)
            chartRespiratoryRate.data = lineData
            
            // Refresh chart
            chartRespiratoryRate.invalidate()
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating chart: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Update statistics based on current data
     */
    private fun updateStatistics() {
        try {
            if (respiratoryRateData.isEmpty()) return
            
            // Calculate statistics
            val rates = respiratoryRateData.map { it.rate }
            avgRate = rates.average()
            minRate = rates.minOrNull() ?: 0
            maxRate = rates.maxOrNull() ?: 0
            
            // Update UI
            tvAvgRate.text = String.format("%.1f", avgRate)
            tvMinRate.text = minRate.toString()
            tvMaxRate.text = maxRate.toString()
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating statistics: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Update current respiratory rate display and status
     */
    private fun updateCurrentRateDisplay() {
        try {
            // Update rate text
            tvCurrentRate.text = currentRate.toString()
            
            // Determine status based on current rate
            val status = when {
                currentRate < NORMAL_MIN_RATE -> STATUS_LOW
                currentRate > NORMAL_MAX_RATE -> STATUS_HIGH
                else -> STATUS_NORMAL
            }
            
            // Update status text
            tvRateStatus.text = status
            
            // Update status color and icon
            when (status) {
                STATUS_LOW -> {
                    tvRateStatus.setTextColor(ContextCompat.getColor(this, R.color.status_warning))
                    ivStatusIndicator.setImageResource(R.drawable.ic_status_warning)
                }
                STATUS_HIGH -> {
                    tvRateStatus.setTextColor(ContextCompat.getColor(this, R.color.status_alert))
                    ivStatusIndicator.setImageResource(R.drawable.ic_status_alert)
                }
                else -> {
                    tvRateStatus.setTextColor(ContextCompat.getColor(this, R.color.status_normal))
                    ivStatusIndicator.setImageResource(R.drawable.ic_status_normal)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating rate display: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Update insights based on respiratory rate data
     */
    private fun updateInsights() {
        try {
            // Determine insight based on current rate and trends
            val (title, content) = when {
                currentRate < NORMAL_MIN_RATE -> {
                    Pair(
                        "Low Respiratory Rate",
                        "Your current respiratory rate is below the normal range. This could indicate respiratory depression, " +
                        "which may be caused by certain medications, sleep apnea, or other conditions. If this persists or you " +
                        "experience shortness of breath, consult your healthcare provider."
                    )
                }
                currentRate > NORMAL_MAX_RATE -> {
                    Pair(
                        "Elevated Respiratory Rate",
                        "Your respiratory rate is above the normal range. This could be due to exercise, anxiety, fever, " +
                        "respiratory infections, or other conditions affecting your breathing. If accompanied by chest pain, " +
                        "severe shortness of breath, or confusion, seek medical attention."
                    )
                }
                avgRate > 18 -> {
                    Pair(
                        "Trending Higher Than Average",
                        "While your current respiratory rate is normal, your average is trending toward the higher end of the " +
                        "normal range. This could be related to stress, mild respiratory conditions, or increased physical activity. " +
                        "Consider practicing deep breathing exercises and monitoring for any changes."
                    )
                }
                else -> {
                    Pair(
                        "Healthy Respiratory Rate",
                        "Your respiratory rate is within the normal range of 12-20 breaths per minute. This indicates healthy " +
                        "lung function and good respiratory health. Continue monitoring and maintain good breathing practices " +
                        "through regular exercise and proper posture."
                    )
                }
            }
            
            // Update UI
            tvInsightTitle.text = title
            tvInsightContent.text = content
        } catch (e: Exception) {
            Toast.makeText(this, "Error updating insights: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Share respiratory rate data
     */
    private fun shareRespiratoryData() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            
            // Format date for sharing
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            
            // Create share text
            val shareText = """
                Sensacare Respiratory Rate Report - $currentDate
                
                Current Rate: $currentRate breaths/min (${tvRateStatus.text})
                Average: ${String.format("%.1f", avgRate)} breaths/min
                Range: $minRate - $maxRate breaths/min
                
                Health Insight: ${tvInsightTitle.text}
                
                Shared from Sensacare Health Monitoring App
            """.trimIndent()
            
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Sensacare Respiratory Rate Report")
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            
            startActivity(Intent.createChooser(shareIntent, "Share Respiratory Rate Data"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Generate simulated respiratory rate data for the past 24 hours
     */
    private fun generateSimulatedData() {
        respiratoryRateData.clear()
        val calendar = Calendar.getInstance()
        val random = Random()
        
        // Set to 24 hours ago
        calendar.add(Calendar.HOUR, -24)
        
        // Generate data points every hour for past 24 hours
        for (i in 0..24) {
            // Generate a realistic respiratory rate (12-20 is normal range)
            // With occasional outliers
            val baseRate = 16 // Center of normal range
            val variation = if (random.nextInt(10) < 8) {
                // 80% chance of normal variation
                random.nextInt(5) - 2 // -2 to +2 from base
            } else {
                // 20% chance of larger variation (potential outlier)
                (random.nextInt(9) - 4) * 2 // -8 to +8 from base
            }
            
            val rate = max(8, min(24, baseRate + variation)) // Clamp between 8-24
            
            // Add data point
            respiratoryRateData.add(RespiratoryRatePoint(calendar.timeInMillis, rate))
            
            // Move forward 1 hour
            calendar.add(Calendar.HOUR, 1)
        }
        
        // Set current rate to the most recent value
        currentRate = respiratoryRateData.last().rate
    }
    
    /**
     * Get a simulated current respiratory rate
     * In a real implementation, this would come from the VeePoo SDK
     */
    private fun getSimulatedCurrentRate(): Int {
        // Base on previous rate if available
        val baseRate = if (respiratoryRateData.isNotEmpty()) {
            respiratoryRateData.last().rate
        } else {
            16 // Default to center of normal range
        }
        
        // Add small random variation
        val variation = Random().nextInt(3) - 1 // -1 to +1
        
        // Ensure rate stays within realistic bounds
        return max(8, min(24, baseRate + variation))
    }
    
    /**
     * Show or hide loading indicator
     */
    private fun showLoading(show: Boolean) {
        progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        chartRespiratoryRate.visibility = if (show) View.INVISIBLE else View.VISIBLE
        cardCurrentRate.visibility = if (show) View.INVISIBLE else View.VISIBLE
        cardStats.visibility = if (show) View.INVISIBLE else View.VISIBLE
        cardInsights.visibility = if (show) View.INVISIBLE else View.VISIBLE
    }
    
    /**
     * Data class to hold respiratory rate measurements
     */
    data class RespiratoryRatePoint(
        val timestamp: Long,
        val rate: Int
    )
}
