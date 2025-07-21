package com.sensacare.app.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensacare.app.domain.model.UserPreferences
import com.sensacare.app.domain.repository.HealthDataRepository
import com.sensacare.app.domain.repository.UserPreferencesRepository
import com.sensacare.app.domain.usecase.health.GetHealthInsightsUseCase
import com.sensacare.app.domain.usecase.health.TimeRange
import com.sensacare.app.domain.usecase.health.model.*
import com.sensacare.app.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * HealthInsightsViewModel - Comprehensive health insights and analytics
 *
 * This ViewModel implements MVVM architecture with reactive UI state management for
 * the health insights feature of the SensaCare app:
 * 
 * Key features:
 * - Health insights display with filtering and categorization
 * - Interactive insights with detailed explanations
 * - Health score breakdown and component analysis
 * - Trends and correlation analysis visualization
 * - Anomaly detection and alert management
 * - Risk assessment display with recommendations
 * - Personalized health recommendations
 * - Time period selection for insights analysis
 * - Insight sharing functionality
 * - Performance optimization with caching
 * - Real-time insight updates
 * - Error handling and loading states
 * - Medical disclaimer management
 */
@HiltViewModel
class HealthInsightsViewModel @Inject constructor(
    private val getHealthInsightsUseCase: GetHealthInsightsUseCase,
    private val healthDataRepository: HealthDataRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // User ID - In a real app, this would come from authentication
    private val userId = 1L
    
    // UI State
    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()
    
    // Selected time range
    private val _selectedTimeRange = MutableStateFlow<TimeRange>(TimeRange.WEEK)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    // Selected insight category filter
    private val _selectedCategory = MutableStateFlow<InsightCategory?>(null)
    val selectedCategory: StateFlow<InsightCategory?> = _selectedCategory.asStateFlow()
    
    // Selected insight severity filter
    private val _selectedSeverity = MutableStateFlow<InsightSeverity?>(null)
    val selectedSeverity: StateFlow<InsightSeverity?> = _selectedSeverity.asStateFlow()
    
    // Selected metric type filter
    private val _selectedMetricType = MutableStateFlow<MetricType?>(null)
    val selectedMetricType: StateFlow<MetricType?> = _selectedMetricType.asStateFlow()
    
    // Health insights
    private val _allInsights = MutableStateFlow<HealthInsights?>(null)
    val allInsights: StateFlow<HealthInsights?> = _allInsights.asStateFlow()
    
    // Filtered insights
    private val _filteredInsights = MutableStateFlow<List<HealthInsight>>(emptyList())
    val filteredInsights: StateFlow<List<HealthInsight>> = _filteredInsights.asStateFlow()
    
    // Selected insight for detailed view
    private val _selectedInsight = MutableStateFlow<HealthInsight?>(null)
    val selectedInsight: StateFlow<HealthInsight?> = _selectedInsight.asStateFlow()
    
    // Health score
    private val _healthScore = MutableStateFlow<HealthScore?>(null)
    val healthScore: StateFlow<HealthScore?> = _healthScore.asStateFlow()
    
    // Selected score component for detailed view
    private val _selectedScoreComponent = MutableStateFlow<HealthScoreComponent?>(null)
    val selectedScoreComponent: StateFlow<HealthScoreComponent?> = _selectedScoreComponent.asStateFlow()
    
    // Medical disclaimer
    private val _medicalDisclaimer = MutableStateFlow<String?>(null)
    val medicalDisclaimer: StateFlow<String?> = _medicalDisclaimer.asStateFlow()
    
    // User preferences
    private val _userPreferences = MutableStateFlow<UserPreferences?>(null)
    val userPreferences: StateFlow<UserPreferences?> = _userPreferences.asStateFlow()
    
    // Error events
    private val _errorEvents = MutableSharedFlow<InsightsErrorEvent>()
    val errorEvents: SharedFlow<InsightsErrorEvent> = _errorEvents.asSharedFlow()
    
    // Navigation events
    val navigationEvent = SingleLiveEvent<InsightsNavigationEvent>()
    
    // Jobs
    private var insightsJob: Job? = null
    private var autoRefreshJob: Job? = null
    
    // Initialize
    init {
        Timber.d("HealthInsightsViewModel initialized")
        loadUserPreferences()
        loadHealthInsights()
        startAutoRefresh()
    }
    
    /**
     * Load user preferences
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.getUserPreferences(userId).collect { preferences ->
                    _userPreferences.value = preferences
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading user preferences")
            }
        }
    }
    
    /**
     * Load health insights
     */
    fun loadHealthInsights(forceRefresh: Boolean = false) {
        // Cancel any existing insights job
        insightsJob?.cancel()
        
        insightsJob = viewModelScope.launch {
            try {
                _uiState.value = InsightsUiState.Loading
                
                // Get health insights
                getHealthInsightsUseCase(
                    userId = userId,
                    timeRange = _selectedTimeRange.value,
                    categories = InsightCategory.values(),
                    forceRefresh = forceRefresh
                ).collect { result ->
                    when (result) {
                        is HealthInsightsResult.Loading -> {
                            _uiState.value = InsightsUiState.Loading
                        }
                        is HealthInsightsResult.Success -> {
                            // Store all insights
                            _allInsights.value = result.insights
                            
                            // Store health score
                            _healthScore.value = result.insights.healthScore
                            
                            // Store medical disclaimer
                            _medicalDisclaimer.value = result.insights.disclaimer
                            
                            // Apply filters
                            applyInsightFilters()
                            
                            // Update UI state
                            _uiState.value = InsightsUiState.Success(
                                timeRange = result.insights.timeRange,
                                generatedAt = result.insights.generatedAt,
                                fromCache = result.fromCache,
                                insightCount = result.insights.insights.size,
                                criticalInsightsCount = result.insights.insights.count { 
                                    it.severity == InsightSeverity.HIGH || it.severity == InsightSeverity.CRITICAL 
                                }
                            )
                        }
                        is HealthInsightsResult.Error -> {
                            _uiState.value = InsightsUiState.Error(result.error.message)
                            _errorEvents.emit(InsightsErrorEvent.LoadingError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading health insights")
                _uiState.value = InsightsUiState.Error("Failed to load health insights: ${e.message}")
                _errorEvents.emit(InsightsErrorEvent.LoadingError("Failed to load health insights", e))
            }
        }
    }
    
    /**
     * Change the selected time range
     */
    fun changeTimeRange(timeRange: TimeRange) {
        if (_selectedTimeRange.value != timeRange) {
            _selectedTimeRange.value = timeRange
            loadHealthInsights(forceRefresh = true)
        }
    }
    
    /**
     * Filter insights by category
     */
    fun filterByCategory(category: InsightCategory?) {
        _selectedCategory.value = category
        applyInsightFilters()
    }
    
    /**
     * Filter insights by severity
     */
    fun filterBySeverity(severity: InsightSeverity?) {
        _selectedSeverity.value = severity
        applyInsightFilters()
    }
    
    /**
     * Filter insights by metric type
     */
    fun filterByMetricType(metricType: MetricType?) {
        _selectedMetricType.value = metricType
        applyInsightFilters()
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _selectedCategory.value = null
        _selectedSeverity.value = null
        _selectedMetricType.value = null
        applyInsightFilters()
    }
    
    /**
     * Apply insight filters
     */
    private fun applyInsightFilters() {
        val insights = _allInsights.value?.insights ?: emptyList()
        
        // Apply category filter
        val categoryFiltered = _selectedCategory.value?.let { category ->
            insights.filter { it.category == category }
        } ?: insights
        
        // Apply severity filter
        val severityFiltered = _selectedSeverity.value?.let { severity ->
            categoryFiltered.filter { it.severity == severity }
        } ?: categoryFiltered
        
        // Apply metric type filter
        val metricFiltered = _selectedMetricType.value?.let { metricType ->
            severityFiltered.filter { 
                when (it) {
                    is CorrelationInsight -> it.primaryMetricType == metricType || it.secondaryMetricType == metricType
                    else -> it.metricType == metricType
                }
            }
        } ?: severityFiltered
        
        // Update filtered insights
        _filteredInsights.value = metricFiltered
    }
    
    /**
     * Select an insight for detailed view
     */
    fun selectInsight(insightId: String) {
        val insight = _filteredInsights.value.find { it.id == insightId }
        _selectedInsight.value = insight
        
        if (insight != null) {
            navigationEvent.value = InsightsNavigationEvent.ToInsightDetail(insight)
        }
    }
    
    /**
     * Clear selected insight
     */
    fun clearSelectedInsight() {
        _selectedInsight.value = null
    }
    
    /**
     * Select a health score component for detailed view
     */
    fun selectScoreComponent(metricType: MetricType) {
        val component = _healthScore.value?.components?.find { it.metricType == metricType }
        _selectedScoreComponent.value = component
        
        if (component != null) {
            navigationEvent.value = InsightsNavigationEvent.ToScoreComponentDetail(component)
        }
    }
    
    /**
     * Clear selected score component
     */
    fun clearSelectedScoreComponent() {
        _selectedScoreComponent.value = null
    }
    
    /**
     * Share an insight
     */
    fun shareInsight(insightId: String) {
        viewModelScope.launch {
            try {
                val insight = _filteredInsights.value.find { it.id == insightId }
                    ?: throw IllegalArgumentException("Insight not found")
                
                // Create share content
                val shareTitle = "Health Insight from SensaCare"
                val shareText = buildString {
                    append(insight.title)
                    append("\n\n")
                    append(insight.description)
                    append("\n\n")
                    
                    // Add category and severity
                    append("Category: ${insight.category.name.lowercase().capitalize()}")
                    append(" | ")
                    append("Importance: ${insight.severity.name.lowercase().capitalize()}")
                    
                    // Add disclaimer
                    append("\n\n")
                    append(_medicalDisclaimer.value ?: "")
                }
                
                // Trigger share event
                navigationEvent.value = InsightsNavigationEvent.ShareInsight(shareTitle, shareText)
                
            } catch (e: Exception) {
                Timber.e(e, "Error sharing insight")
                _errorEvents.emit(InsightsErrorEvent.SharingError("Failed to share insight", e))
            }
        }
    }
    
    /**
     * Get related insights for an insight
     */
    fun getRelatedInsights(insightId: String): List<HealthInsight> {
        val insight = _allInsights.value?.insights?.find { it.id == insightId }
            ?: return emptyList()
        
        // Get related insights by ID
        val relatedByIds = insight.relatedInsightIds.mapNotNull { relatedId ->
            _allInsights.value?.insights?.find { it.id == relatedId }
        }
        
        // If no explicit relations, find insights with same metric type
        if (relatedByIds.isEmpty()) {
            return when (insight) {
                is CorrelationInsight -> {
                    _allInsights.value?.insights?.filter { 
                        it.id != insightId && 
                        (it.metricType == insight.primaryMetricType || it.metricType == insight.secondaryMetricType)
                    } ?: emptyList()
                }
                else -> {
                    _allInsights.value?.insights?.filter { 
                        it.id != insightId && it.metricType == insight.metricType
                    } ?: emptyList()
                }
            }.take(3) // Limit to 3 related insights
        }
        
        return relatedByIds
    }
    
    /**
     * Get insights by category
     */
    fun getInsightsByCategory(category: InsightCategory): List<HealthInsight> {
        return _allInsights.value?.getInsightsByCategory(category) ?: emptyList()
    }
    
    /**
     * Get insights by severity
     */
    fun getInsightsBySeverity(severity: InsightSeverity): List<HealthInsight> {
        return _allInsights.value?.getInsightsBySeverity(severity) ?: emptyList()
    }
    
    /**
     * Get insights by metric type
     */
    fun getInsightsByMetricType(metricType: MetricType): List<HealthInsight> {
        return _allInsights.value?.getInsightsByMetricType(metricType) ?: emptyList()
    }
    
    /**
     * Get critical insights
     */
    fun getCriticalInsights(): List<HealthInsight> {
        return _allInsights.value?.getCriticalInsights() ?: emptyList()
    }
    
    /**
     * Get trend insights
     */
    fun getTrendInsights(): List<TrendInsight> {
        return _allInsights.value?.insights?.filterIsInstance<TrendInsight>() ?: emptyList()
    }
    
    /**
     * Get anomaly insights
     */
    fun getAnomalyInsights(): List<AnomalyInsight> {
        return _allInsights.value?.insights?.filterIsInstance<AnomalyInsight>() ?: emptyList()
    }
    
    /**
     * Get correlation insights
     */
    fun getCorrelationInsights(): List<CorrelationInsight> {
        return _allInsights.value?.insights?.filterIsInstance<CorrelationInsight>() ?: emptyList()
    }
    
    /**
     * Get risk assessment insights
     */
    fun getRiskAssessmentInsights(): List<RiskAssessmentInsight> {
        return _allInsights.value?.insights?.filterIsInstance<RiskAssessmentInsight>() ?: emptyList()
    }
    
    /**
     * Get recommendation insights
     */
    fun getRecommendationInsights(): List<RecommendationInsight> {
        return _allInsights.value?.insights?.filterIsInstance<RecommendationInsight>() ?: emptyList()
    }
    
    /**
     * Get goal insights
     */
    fun getGoalInsights(): List<GoalInsight> {
        return _allInsights.value?.insights?.filterIsInstance<GoalInsight>() ?: emptyList()
    }
    
    /**
     * Get chart data for a trend insight
     */
    fun getTrendChartData(insight: TrendInsight): TrendChartData? {
        // This would typically fetch the underlying data for the trend
        // For now, we'll create some simulated data
        return when (insight.metricType) {
            MetricType.HEART_RATE -> createHeartRateTrendChart(insight)
            MetricType.BLOOD_OXYGEN -> createBloodOxygenTrendChart(insight)
            MetricType.BLOOD_PRESSURE -> createBloodPressureTrendChart(insight)
            MetricType.SLEEP -> createSleepTrendChart(insight)
            MetricType.STEPS -> createStepsTrendChart(insight)
            MetricType.ACTIVITY -> createActivityTrendChart(insight)
            MetricType.TEMPERATURE -> createTemperatureTrendChart(insight)
            MetricType.MULTIPLE -> null // Multiple metrics not supported for trend charts
        }
    }
    
    /**
     * Get chart data for a correlation insight
     */
    fun getCorrelationChartData(insight: CorrelationInsight): CorrelationChartData? {
        // This would typically fetch the underlying data for the correlation
        // For now, we'll create some simulated data
        return createCorrelationChart(insight)
    }
    
    /**
     * Create heart rate trend chart data
     */
    private fun createHeartRateTrendChart(insight: TrendInsight): TrendChartData {
        // In a real implementation, this would fetch actual heart rate data
        // For now, we'll create simulated data
        
        val points = (0..6).map { day ->
            val baseValue = 70.0
            val trend = when (insight.trendDirection) {
                TrendDirection.INCREASING -> day * 2.0
                TrendDirection.DECREASING -> -day * 2.0
                TrendDirection.STABLE -> 0.0
            }
            val value = baseValue + trend + (-5..5).random()
            
            val date = LocalDateTime.now().minusDays((6 - day).toLong())
            val label = date.format(DateTimeFormatter.ofPattern("MM-dd"))
            
            ChartPoint(label, value)
        }
        
        return TrendChartData(
            title = "Heart Rate Trend",
            points = points,
            yAxisLabel = "BPM",
            color = "#E53935", // Red
            trendDirection = insight.trendDirection,
            percentageChange = insight.percentageChange,
            referenceRange = 60.0..100.0 // Normal heart rate range
        )
    }
    
    /**
     * Create blood oxygen trend chart data
     */
    private fun createBloodOxygenTrendChart(insight: TrendInsight): TrendChartData {
        // Simulated data
        val points = (0..6).map { day ->
            val baseValue = 96.0
            val trend = when (insight.trendDirection) {
                TrendDirection.INCREASING -> day * 0.2
                TrendDirection.DECREASING -> -day * 0.2
                TrendDirection.STABLE -> 0.0
            }
            val value = baseValue + trend + (-0.5..0.5).random()
            
            val date = LocalDateTime.now().minusDays((6 - day).toLong())
            val label = date.format(DateTimeFormatter.ofPattern("MM-dd"))
            
            ChartPoint(label, value)
        }
        
        return TrendChartData(
            title = "Blood Oxygen Trend",
            points = points,
            yAxisLabel = "%",
            color = "#1E88E5", // Blue
            trendDirection = insight.trendDirection,
            percentageChange = insight.percentageChange,
            referenceRange = 95.0..100.0 // Normal blood oxygen range
        )
    }
    
    /**
     * Create blood pressure trend chart data
     */
    private fun createBloodPressureTrendChart(insight: TrendInsight): TrendChartData {
        // Simulated data for systolic pressure
        val points = (0..6).map { day ->
            val baseValue = 120.0
            val trend = when (insight.trendDirection) {
                TrendDirection.INCREASING -> day * 1.5
                TrendDirection.DECREASING -> -day * 1.5
                TrendDirection.STABLE -> 0.0
            }
            val value = baseValue + trend + (-5..5).random()
            
            val date = LocalDateTime.now().minusDays((6 - day).toLong())
            val label = date.format(DateTimeFormatter.ofPattern("MM-dd"))
            
            ChartPoint(label, value)
        }
        
        return TrendChartData(
            title = "Systolic Blood Pressure Trend",
            points = points,
            yAxisLabel = "mmHg",
            color = "#F44336", // Red
            trendDirection = insight.trendDirection,
            percentageChange = insight.percentageChange,
            referenceRange = 90.0..130.0 // Normal systolic range
        )
    }
    
    /**
     * Create sleep trend chart data
     */
    private fun createSleepTrendChart(insight: TrendInsight): TrendChartData {
        // Simulated data
        val points = (0..6).map { day ->
            val baseValue = 7.0 // 7 hours
            val trend = when (insight.trendDirection) {
                TrendDirection.INCREASING -> day * 0.1
                TrendDirection.DECREASING -> -day * 0.1
                TrendDirection.STABLE -> 0.0
            }
            val value = baseValue + trend + (-0.5..0.5).random()
            
            val date = LocalDateTime.now().minusDays((6 - day).toLong())
            val label = date.format(DateTimeFormatter.ofPattern("MM-dd"))
            
            ChartPoint(label, value)
        }
        
        return TrendChartData(
            title = "Sleep Duration Trend",
            points = points,
            yAxisLabel = "Hours",
            color = "#5E35B1", // Purple
            trendDirection = insight.trendDirection,
            percentageChange = insight.percentageChange,
            referenceRange = 7.0..9.0 // Recommended sleep range
        )
    }
    
    /**
     * Create steps trend chart data
     */
    private fun createStepsTrendChart(insight: TrendInsight): TrendChartData {
        // Simulated data
        val points = (0..6).map { day ->
            val baseValue = 8000.0
            val trend = when (insight.trendDirection) {
                TrendDirection.INCREASING -> day * 200.0
                TrendDirection.DECREASING -> -day * 200.0
                TrendDirection.STABLE -> 0.0
            }
            val value = baseValue + trend + (-500..500).random()
            
            val date = LocalDateTime.now().minusDays((6 - day).toLong())
            val label = date.format(DateTimeFormatter.ofPattern("MM-dd"))
            
            ChartPoint(label, value)
        }
        
        return TrendChartData(
            title = "Steps Trend",
            points = points,
            yAxisLabel = "Steps",
            color = "#43A047", // Green
            trendDirection = insight.trendDirection,
            percentageChange = insight.percentageChange,
            referenceRange = 7500.0..10000.0 // Recommended steps range
        )
    }
    
    /**
     * Create activity trend chart data
     */
    private fun createActivityTrendChart(insight: TrendInsight): TrendChartData {
        // Simulated data
        val points = (0..6).map { day ->
            val baseValue = 30.0 // 30 minutes
            val trend = when (insight.trendDirection) {
                TrendDirection.INCREASING -> day * 2.0
                TrendDirection.DECREASING -> -day * 2.0
                TrendDirection.STABLE -> 0.0
            }
            val value = baseValue + trend + (-5..5).random()
            
            val date = LocalDateTime.now().minusDays((6 - day).toLong())
            val label = date.format(DateTimeFormatter.ofPattern("MM-dd"))
            
            ChartPoint(label, value)
        }
        
        return TrendChartData(
            title = "Activity Duration Trend",
            points = points,
            yAxisLabel = "Minutes",
            color = "#FB8C00", // Orange
            trendDirection = insight.trendDirection,
            percentageChange = insight.percentageChange,
            referenceRange = 30.0..60.0 // Recommended activity range
        )
    }
    
    /**
     * Create temperature trend chart data
     */
    private fun createTemperatureTrendChart(insight: TrendInsight): TrendChartData {
        // Simulated data
        val points = (0..6).map { day ->
            val baseValue = 36.8
            val trend = when (insight.trendDirection) {
                TrendDirection.INCREASING -> day * 0.05
                TrendDirection.DECREASING -> -day * 0.05
                TrendDirection.STABLE -> 0.0
            }
            val value = baseValue + trend + (-0.1..0.1).random()
            
            val date = LocalDateTime.now().minusDays((6 - day).toLong())
            val label = date.format(DateTimeFormatter.ofPattern("MM-dd"))
            
            ChartPoint(label, value)
        }
        
        return TrendChartData(
            title = "Body Temperature Trend",
            points = points,
            yAxisLabel = "°C",
            color = "#D81B60", // Pink
            trendDirection = insight.trendDirection,
            percentageChange = insight.percentageChange,
            referenceRange = 36.1..37.2 // Normal temperature range
        )
    }
    
    /**
     * Create correlation chart data
     */
    private fun createCorrelationChart(insight: CorrelationInsight): CorrelationChartData {
        // Simulated data
        val dataPoints = (0..10).map { i ->
            // Create correlated or anti-correlated data based on correlation type
            val xBase = 50.0 + i * 5.0
            val xValue = xBase + (-2.0..2.0).random()
            
            val yBase = when (insight.correlationType) {
                CorrelationType.POSITIVE -> 50.0 + i * 5.0 * insight.correlationStrength
                CorrelationType.INVERSE -> 100.0 - i * 5.0 * insight.correlationStrength
                CorrelationType.NONE -> 75.0 + (-10.0..10.0).random()
            }
            val yValue = yBase + (-2.0..2.0).random()
            
            CorrelationDataPoint(xValue, yValue)
        }
        
        // Labels for the axes based on metric types
        val xAxisLabel = when (insight.primaryMetricType) {
            MetricType.HEART_RATE -> "Heart Rate (bpm)"
            MetricType.BLOOD_OXYGEN -> "Blood Oxygen (%)"
            MetricType.BLOOD_PRESSURE -> "Blood Pressure (mmHg)"
            MetricType.SLEEP -> "Sleep Duration (hours)"
            MetricType.STEPS -> "Steps"
            MetricType.ACTIVITY -> "Activity (minutes)"
            MetricType.TEMPERATURE -> "Temperature (°C)"
            MetricType.MULTIPLE -> "Value"
        }
        
        val yAxisLabel = when (insight.secondaryMetricType) {
            MetricType.HEART_RATE -> "Heart Rate (bpm)"
            MetricType.BLOOD_OXYGEN -> "Blood Oxygen (%)"
            MetricType.BLOOD_PRESSURE -> "Blood Pressure (mmHg)"
            MetricType.SLEEP -> "Sleep Duration (hours)"
            MetricType.STEPS -> "Steps"
            MetricType.ACTIVITY -> "Activity (minutes)"
            MetricType.TEMPERATURE -> "Temperature (°C)"
            MetricType.MULTIPLE -> "Value"
        }
        
        return CorrelationChartData(
            title = "${insight.primaryMetricType.description} vs ${insight.secondaryMetricType.description}",
            dataPoints = dataPoints,
            xAxisLabel = xAxisLabel,
            yAxisLabel = yAxisLabel,
            correlationType = insight.correlationType,
            correlationStrength = insight.correlationStrength,
            color = when (insight.correlationType) {
                CorrelationType.POSITIVE -> "#4CAF50" // Green
                CorrelationType.INVERSE -> "#F44336" // Red
                CorrelationType.NONE -> "#9E9E9E" // Gray
            }
        )
    }
    
    /**
     * Get explanation for a health score component
     */
    fun getScoreComponentExplanation(component: HealthScoreComponent): String {
        // Generate detailed explanation for score component
        val baseExplanation = when (component.metricType) {
            MetricType.HEART_RATE -> 
                "Your heart rate score is based on your resting heart rate, variability, and recovery patterns. " +
                "A lower resting heart rate generally indicates better cardiovascular fitness."
            
            MetricType.BLOOD_OXYGEN -> 
                "Your blood oxygen score reflects how well your lungs and circulatory system are delivering oxygen to your body. " +
                "Optimal levels are typically 95-100%."
            
            MetricType.BLOOD_PRESSURE -> 
                "Your blood pressure score is based on systolic and diastolic measurements. " +
                "Optimal blood pressure is typically below 120/80 mmHg."
            
            MetricType.SLEEP -> 
                "Your sleep score considers duration, quality, consistency, and sleep stages. " +
                "Adults typically need 7-9 hours of quality sleep per night."
            
            MetricType.STEPS -> 
                "Your activity score is based on daily step count and movement patterns. " +
                "A goal of 7,500-10,000 steps per day is often recommended."
            
            MetricType.ACTIVITY -> 
                "Your exercise score reflects the frequency, duration, and intensity of your workouts. " +
                "Adults should aim for at least 150 minutes of moderate activity per week."
            
            MetricType.TEMPERATURE -> 
                "Your temperature score is based on the stability and range of your body temperature measurements. " +
                "Normal body temperature is typically around 36.5-37.5°C."
            
            MetricType.MULTIPLE -> 
                "This score component combines multiple health metrics to provide a comprehensive assessment."
        }
        
        // Add score-specific feedback
        val scoreFeedback = when {
            component.score >= 90 -> "Your score is excellent in this area."
            component.score >= 80 -> "Your score is very good in this area."
            component.score >= 70 -> "Your score is good in this area."
            component.score >= 60 -> "Your score is fair in this area."
            else -> "This area has room for improvement."
        }
        
        // Add improvement suggestions
        val improvementSuggestions = when (component.metricType) {
            MetricType.HEART_RATE -> 
                if (component.score < 70) "Regular cardiovascular exercise and stress management can help improve this score."
                else "Continue with regular exercise to maintain your good heart health."
            
            MetricType.BLOOD_OXYGEN -> 
                if (component.score < 70) "Deep breathing exercises, good posture, and regular exercise can help improve oxygen levels."
                else "Continue with good respiratory practices to maintain your oxygen levels."
            
            MetricType.BLOOD_PRESSURE -> 
                if (component.score < 70) "Reducing sodium intake, regular exercise, and stress management can help improve blood pressure."
                else "Continue with heart-healthy habits to maintain your blood pressure."
            
            MetricType.SLEEP -> 
                if (component.score < 70) "Consistent sleep schedule, limiting screen time before bed, and creating a restful environment can improve sleep."
                else "Continue with good sleep hygiene to maintain your sleep quality."
            
            MetricType.STEPS -> 
                if (component.score < 70) "Try to incorporate more walking into your daily routine, take the stairs, or park farther away."
                else "Continue staying active throughout your day to maintain your step count."
            
            MetricType.ACTIVITY -> 
                if (component.score < 70) "Try to incorporate more structured exercise into your week, aiming for at least 30 minutes most days."
                else "Continue with your regular exercise routine to maintain your activity level."
            
            MetricType.TEMPERATURE -> 
                if (component.score < 70) "Consistent temperature readings can indicate good health stability."
                else "Continue monitoring your temperature for any significant changes."
            
            MetricType.MULTIPLE -> 
                if (component.score < 70) "Focus on the individual components that need the most improvement."
                else "Continue with your balanced approach to health maintenance."
        }
        
        return "$baseExplanation\n\n$scoreFeedback\n\n$improvementSuggestions"
    }
    
    /**
     * Get detailed explanation for an insight
     */
    fun getInsightExplanation(insight: HealthInsight): String {
        // Start with the insight description
        val baseExplanation = insight.description
        
        // Add type-specific details
        val typeSpecificDetails = when (insight) {
            is TrendInsight -> {
                "This trend shows a ${insight.trendDirection.description.lowercase()} pattern " +
                "with a ${String.format("%.1f", insight.percentageChange)}% change over the selected time period."
            }
            is AnomalyInsight -> {
                "This anomaly was detected on ${insight.detectedAt.format(DateTimeFormatter.ofPattern("MMM d"))}. " +
                "The value of ${String.format("%.1f", insight.affectedValue)} was outside the expected range of " +
                "${String.format("%.1f", insight.expectedRange.first)}-${String.format("%.1f", insight.expectedRange.second)}."
            }
            is CorrelationInsight -> {
                "This ${insight.correlationType.description.lowercase()} correlation between " +
                "${insight.primaryMetricType.description} and ${insight.secondaryMetricType.description} " +
                "has a strength of ${String.format("%.1f", insight.correlationStrength * 100)}%."
            }
            is RiskAssessmentInsight -> {
                buildString {
                    append("Risk factors include: ")
                    append(insight.riskFactors.joinToString(", "))
                    append(".\n\nRecommended actions:\n")
                    insight.recommendedActions.forEachIndexed { index, action ->
                        append("${index + 1}. $action\n")
                    }
                }
            }
            is RecommendationInsight -> {
                buildString {
                    append("Recommended actions:\n")
                    insight.recommendationActions.forEachIndexed { index, action ->
                        append("${index + 1}. $action\n")
                    }
                    append("\nExpected benefits:\n")
                    insight.expectedBenefits.forEachIndexed { index, benefit ->
                        append("${index + 1}. $benefit\n")
                    }
                }
            }
            is GoalInsight -> {
                "Current progress: ${String.format("%.1f", insight.progressPercentage)}% " +
                "(${String.format("%.1f", insight.currentValue)} out of ${String.format("%.1f", insight.goalTarget)}).\n" +
                if (insight.isAchieved) "Goal achieved with a streak of ${insight.streakDays} days!"
                else "Keep going to reach your goal!"
            }
            else -> ""
        }
        
        // Add severity explanation
        val severityExplanation = when (insight.severity) {
            InsightSeverity.CRITICAL -> 
                "This insight is marked as CRITICAL and may require immediate attention."
            InsightSeverity.HIGH -> 
                "This insight is marked as HIGH priority and should be addressed soon."
            InsightSeverity.MODERATE -> 
                "This insight is of MODERATE importance for your health."
            InsightSeverity.LOW -> 
                "This insight is of LOW priority but still relevant to your health."
        }
        
        // Add medical disclaimer
        val disclaimer = _medicalDisclaimer.value ?: ""
        
        return "$baseExplanation\n\n$typeSpecificDetails\n\n$severityExplanation\n\n$disclaimer"
    }
    
    /**
     * Share health score
     */
    fun shareHealthScore() {
        viewModelScope.launch {
            try {
                val healthScore = _healthScore.value
                    ?: throw IllegalStateException("Health score not available")
                
                // Create share content
                val shareTitle = "My SensaCare Health Score"
                val shareText = buildString {
                    append("My health score is ${healthScore.score}/100 (${healthScore.grade.letter} - ${healthScore.grade.description})")
                    append("\n\n")
                    
                    // Add top components
                    val topComponent = healthScore.getHighestComponent()
                    if (topComponent != null) {
                        append("Strongest area: ${topComponent.metricType.description} (${topComponent.score}/100)")
                        append("\n")
                    }
                    
                    // Add areas for improvement
                    val improvementComponents = healthScore.getComponentsNeedingImprovement()
                    if (improvementComponents.isNotEmpty()) {
                        append("Areas for improvement: ")
                        append(improvementComponents.joinToString(", ") { it.metricType.description })
                    }
                    
                    // Add disclaimer
                    append("\n\n")
                    append(_medicalDisclaimer.value ?: "")
                }
                
                // Trigger share event
                navigationEvent.value = InsightsNavigationEvent.ShareHealthScore(shareTitle, shareText)
                
            } catch (e: Exception) {
                Timber.e(e, "Error sharing health score")
                _errorEvents.emit(InsightsErrorEvent.SharingError("Failed to share health score", e))
            }
        }
    }
    
    /**
     * Navigate to metric detail screen
     */
    fun navigateToMetricDetail(metricType: MetricType) {
        navigationEvent.value = InsightsNavigationEvent.ToMetricDetail(metricType)
    }
    
    /**
     * Navigate back to insights list
     */
    fun navigateBackToList() {
        clearSelectedInsight()
        clearSelectedScoreComponent()
    }
    
    /**
     * Start auto-refresh job
     */
    private fun startAutoRefresh() {
        // Cancel any existing auto-refresh job
        autoRefreshJob?.cancel()
        
        // Start new auto-refresh job
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(15 * 60 * 1000) // Refresh every 15 minutes
                loadHealthInsights(forceRefresh = true)
            }
        }
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        
        // Cancel all jobs
        insightsJob?.cancel()
        autoRefreshJob?.cancel()
    }
}

/**
 * Sealed class representing the UI state for health insights
 */
sealed class InsightsUiState {
    /**
     * Loading state
     */
    data object Loading : InsightsUiState()
    
    /**
     * Success state
     */
    data class Success(
        val timeRange: TimeRange,
        val generatedAt: LocalDateTime,
        val fromCache: Boolean,
        val insightCount: Int,
        val criticalInsightsCount: Int
    ) : InsightsUiState() {
        /**
         * Format generated time as a string
         */
        fun getGeneratedAtFormatted(): String {
            val now = LocalDateTime.now()
            val minutes = java.time.temporal.ChronoUnit.MINUTES.between(generatedAt, now)
            
            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                minutes < 24 * 60 -> "${minutes / 60} hour${if (minutes / 60 > 1) "s" else ""} ago"
                else -> generatedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        }
        
        /**
         * Get cache status message
         */
        fun getCacheStatusMessage(): String {
            return if (fromCache) "Using cached insights" else "Using fresh insights"
        }
    }
    
    /**
     * Error state
     */
    data class Error(val message: String) : InsightsUiState()
}

/**
 * Data class representing trend chart data
 */
data class TrendChartData(
    val title: String,
    val points: List<ChartPoint>,
    val yAxisLabel: String,
    val color: String,
    val trendDirection: TrendDirection,
    val percentageChange: Double,
    val referenceRange: ClosedRange<Double>
) {
    /**
     * Get minimum Y value
     */
    fun getMinY(): Double {
        return points.minOfOrNull { it.value } ?: referenceRange.start
    }
    
    /**
     * Get maximum Y value
     */
    fun getMaxY(): Double {
        return points.maxOfOrNull { it.value } ?: referenceRange.endInclusive
    }
    
    /**
     * Get Y-axis range for chart
     */
    fun getYAxisRange(): ClosedRange<Double> {
        val minData = getMinY()
        val maxData = getMaxY()
        val padding = (maxData - minData) * 0.1
        
        return (minData - padding)..(maxData + padding)
    }
    
    /**
     * Format percentage change as a string
     */
    fun getPercentageChangeFormatted(): String {
        val prefix = when (trendDirection) {
            TrendDirection.INCREASING -> "+"
            TrendDirection.DECREASING -> "-"
            TrendDirection.STABLE -> "±"
        }
        
        return "$prefix${String.format("%.1f", kotlin.math.abs(percentageChange))}%"
    }
    
    /**
     * Get trend color
     */
    fun getTrendColor(): String {
        return when (trendDirection) {
            TrendDirection.INCREASING -> "#4CAF50" // Green
            TrendDirection.DECREASING -> "#F44336" // Red
            TrendDirection.STABLE -> "#9E9E9E" // Gray
        }
    }
}

/**
 * Data class representing a chart point
 */
data class ChartPoint(
    val label: String,
    val value: Double
)

/**
 * Data class representing correlation chart data
 */
data class CorrelationChartData(
    val title: String,
    val dataPoints: List<CorrelationDataPoint>,
    val xAxisLabel: String,
    val yAxisLabel: String,
    val correlationType: CorrelationType,
    val correlationStrength: Double,
    val color: String
) {
    /**
     * Get X-axis range
     */
    fun getXAxisRange(): ClosedRange<Double> {
        val minX = dataPoints.minOfOrNull { it.x } ?: 0.0
        val maxX = dataPoints.maxOfOrNull { it.x } ?: 100.0
        val padding = (maxX - minX) * 0.1
        
        return (minX - padding)..(maxX + padding)
    }
    
    /**
     * Get Y-axis range
     */
    fun getYAxisRange(): ClosedRange<Double> {
        val minY = dataPoints.minOfOrNull { it.y } ?: 0.0
        val maxY = dataPoints.maxOfOrNull { it.y } ?: 100.0
        val padding = (maxY - minY) * 0.1
        
        return (minY - padding)..(maxY + padding)
    }
    
    /**
     * Format correlation strength as a string
     */
    fun getCorrelationStrengthFormatted(): String {
        return String.format("%.1f", correlationStrength * 100) + "%"
    }
    
    /**
     * Get correlation strength description
     */
    fun getCorrelationStrengthDescription(): String {
        return when {
            correlationStrength >= 0.7 -> "Strong"
            correlationStrength >= 0.4 -> "Moderate"
            correlationStrength >= 0.2 -> "Weak"
            else -> "Very Weak"
        }
    }
    
    /**
     * Get trend line points
     */
    fun getTrendLinePoints(): Pair<CorrelationDataPoint, CorrelationDataPoint> {
        // Calculate simple linear regression
        val n = dataPoints.size
        val sumX = dataPoints.sumOf { it.x }
        val sumY = dataPoints.sumOf { it.y }
        val sumXY = dataPoints.sumOf { it.x * it.y }
        val sumXX = dataPoints.sumOf { it.x * it.x }
        
        val slope = if (n > 0) {
            (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        } else 0.0
        
        val intercept = if (n > 0) {
            (sumY - slope * sumX) / n
        } else 0.0
        
        // Get min and max X values
        val minX = dataPoints.minOfOrNull { it.x } ?: 0.0
        val maxX = dataPoints.maxOfOrNull { it.x } ?: 100.0
        
        // Calculate Y values for trend line
        val startY = slope * minX + intercept
        val endY = slope * maxX + intercept
        
        return Pair(
            CorrelationDataPoint(minX, startY),
            CorrelationDataPoint(maxX, endY)
        )
    }
}

/**
 * Data class representing a correlation data point
 */
data class CorrelationDataPoint(
    val x: Double,
    val y: Double
)

/**
 * Sealed class for insights error events
 */
sealed class InsightsErrorEvent {
    /**
     * Loading error
     */
    data class LoadingError(
        val message: String,
        val cause: Throwable? = null
    ) : InsightsErrorEvent()
    
    /**
     * Sharing error
     */
    data class SharingError(
        val message: String,
        val cause: Throwable? = null
    ) : InsightsErrorEvent()
    
    /**
     * Filtering error
     */
    data class FilteringError(val message: String) : InsightsErrorEvent()
}

/**
 * Sealed class for insights navigation events
 */
sealed class InsightsNavigationEvent {
    /**
     * Navigate to insight detail screen
     */
    data class ToInsightDetail(val insight: HealthInsight) : InsightsNavigationEvent()
    
    /**
     * Navigate to score component detail screen
     */
    data class ToScoreComponentDetail(val component: HealthScoreComponent) : InsightsNavigationEvent()
    
    /**
     * Navigate to metric detail screen
     */
    data class ToMetricDetail(val metricType: MetricType) : InsightsNavigationEvent()
    
    /**
     * Share an insight
     */
    data class ShareInsight(
        val title: String,
        val content: String
    ) : InsightsNavigationEvent()
    
    /**
     * Share health score
     */
    data class ShareHealthScore(
        val title: String,
        val content: String
    ) : InsightsNavigationEvent()
}

/**
 * Extension function to capitalize the first letter of a string
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
}
