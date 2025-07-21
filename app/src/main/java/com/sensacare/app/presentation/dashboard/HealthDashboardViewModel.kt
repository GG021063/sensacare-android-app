package com.sensacare.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.*
import com.sensacare.app.domain.usecase.device.SyncDeviceDataUseCase
import com.sensacare.app.domain.usecase.device.SyncParams
import com.sensacare.app.domain.usecase.device.SyncProgress
import com.sensacare.app.domain.usecase.device.SyncType
import com.sensacare.app.domain.usecase.health.GetHealthInsightsUseCase
import com.sensacare.app.domain.usecase.health.ManageHealthGoalsUseCase
import com.sensacare.app.domain.usecase.health.TimeRange
import com.sensacare.app.domain.usecase.health.model.HealthInsight
import com.sensacare.app.domain.usecase.health.model.HealthInsights
import com.sensacare.app.domain.usecase.health.model.HealthInsightsResult
import com.sensacare.app.domain.usecase.health.model.HealthScore
import com.sensacare.app.domain.usecase.health.model.InsightCategory
import com.sensacare.app.domain.usecase.health.model.InsightSeverity
import com.sensacare.app.domain.usecase.health.model.MetricType
import com.sensacare.app.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * HealthDashboardViewModel - Comprehensive health data aggregation and display
 *
 * This ViewModel implements MVVM architecture with reactive UI state management for
 * the main health dashboard of the SensaCare app:
 * 
 * Key features:
 * - Real-time health data aggregation and display
 * - Health metrics summary and cards display
 * - Recent activities and trends overview
 * - Goal progress tracking on dashboard
 * - Health score display with components
 * - Quick actions for device sync and data entry
 * - Data refresh and pull-to-refresh functionality
 * - Time period selection (daily, weekly, monthly)
 * - Health alerts and notifications display
 * - Performance optimization with data caching
 * - Error handling and loading states
 * - Integration with health insights
 * - Chart data preparation for UI
 */
@HiltViewModel
class HealthDashboardViewModel @Inject constructor(
    private val healthDataRepository: HealthDataRepository,
    private val deviceRepository: DeviceRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthGoalRepository: HealthGoalRepository,
    private val getHealthInsightsUseCase: GetHealthInsightsUseCase,
    private val manageHealthGoalsUseCase: ManageHealthGoalsUseCase,
    private val syncDeviceUseCase: SyncDeviceDataUseCase
) : ViewModel() {

    // User ID - In a real app, this would come from authentication
    private val userId = 1L
    
    // UI State
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Selected time range
    private val _selectedTimeRange = MutableStateFlow<TimeRange>(TimeRange.DAY)
    val selectedTimeRange: StateFlow<TimeRange> = _selectedTimeRange.asStateFlow()
    
    // Health metrics
    private val _healthMetrics = MutableStateFlow<HealthMetricsSummary?>(null)
    val healthMetrics: StateFlow<HealthMetricsSummary?> = _healthMetrics.asStateFlow()
    
    // Health score
    private val _healthScore = MutableStateFlow<HealthScore?>(null)
    val healthScore: StateFlow<HealthScore?> = _healthScore.asStateFlow()
    
    // Health insights
    private val _healthInsights = MutableStateFlow<List<HealthInsight>>(emptyList())
    val healthInsights: StateFlow<List<HealthInsight>> = _healthInsights.asStateFlow()
    
    // Critical alerts
    private val _criticalAlerts = MutableStateFlow<List<HealthInsight>>(emptyList())
    val criticalAlerts: StateFlow<List<HealthInsight>> = _criticalAlerts.asStateFlow()
    
    // Active goals
    private val _activeGoals = MutableStateFlow<List<HealthGoal>>(emptyList())
    val activeGoals: StateFlow<List<HealthGoal>> = _activeGoals.asStateFlow()
    
    // Recent activities
    private val _recentActivities = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val recentActivities: StateFlow<List<ActivityRecord>> = _recentActivities.asStateFlow()
    
    // Device sync status
    private val _deviceSyncStatus = MutableStateFlow<DeviceSyncStatus>(DeviceSyncStatus.Idle)
    val deviceSyncStatus: StateFlow<DeviceSyncStatus> = _deviceSyncStatus.asStateFlow()
    
    // Chart data
    private val _heartRateChartData = MutableStateFlow<ChartData?>(null)
    val heartRateChartData: StateFlow<ChartData?> = _heartRateChartData.asStateFlow()
    
    private val _stepsChartData = MutableStateFlow<ChartData?>(null)
    val stepsChartData: StateFlow<ChartData?> = _stepsChartData.asStateFlow()
    
    private val _sleepChartData = MutableStateFlow<ChartData?>(null)
    val sleepChartData: StateFlow<ChartData?> = _sleepChartData.asStateFlow()
    
    private val _bloodOxygenChartData = MutableStateFlow<ChartData?>(null)
    val bloodOxygenChartData: StateFlow<ChartData?> = _bloodOxygenChartData.asStateFlow()
    
    // Error events
    private val _errorEvents = MutableSharedFlow<DashboardErrorEvent>()
    val errorEvents: SharedFlow<DashboardErrorEvent> = _errorEvents.asSharedFlow()
    
    // Navigation events
    val navigationEvent = SingleLiveEvent<DashboardNavigationEvent>()
    
    // Jobs
    private var dataRefreshJob: Job? = null
    private var syncJob: Job? = null
    private var autoRefreshJob: Job? = null
    
    // Initialize
    init {
        Timber.d("HealthDashboardViewModel initialized")
        loadDashboardData()
        startAutoRefresh()
    }
    
    /**
     * Load all dashboard data
     */
    fun loadDashboardData(showLoading: Boolean = true) {
        // Cancel any existing refresh job
        dataRefreshJob?.cancel()
        
        dataRefreshJob = viewModelScope.launch {
            try {
                if (showLoading) {
                    _uiState.value = DashboardUiState.Loading
                }
                
                // Load data in parallel for efficiency
                val timeRange = _selectedTimeRange.value
                
                // Calculate date range
                val (startDate, endDate) = calculateDateRange(timeRange)
                
                // Load user preferences
                val userPreferences = userPreferencesRepository.getUserPreferences(userId).first()
                
                // Load health data
                loadHealthData(startDate, endDate)
                
                // Load health insights
                loadHealthInsights(timeRange)
                
                // Load active goals
                loadActiveGoals()
                
                // Load recent activities
                loadRecentActivities(startDate, endDate)
                
                // Load connected devices
                val connectedDevices = deviceRepository.getConnectedDevices().first()
                
                // Update UI state
                _uiState.value = DashboardUiState.Success(
                    userName = userPreferences.name ?: "User",
                    timeRange = timeRange,
                    lastUpdated = LocalDateTime.now(),
                    hasConnectedDevices = connectedDevices.isNotEmpty(),
                    hasActiveGoals = _activeGoals.value.isNotEmpty(),
                    hasHealthInsights = _healthInsights.value.isNotEmpty(),
                    hasCriticalAlerts = _criticalAlerts.value.isNotEmpty()
                )
                
            } catch (e: Exception) {
                Timber.e(e, "Error loading dashboard data")
                _uiState.value = DashboardUiState.Error("Failed to load dashboard data: ${e.message}")
                _errorEvents.emit(DashboardErrorEvent.LoadingError("Failed to load dashboard data", e))
            }
        }
    }
    
    /**
     * Change the selected time range
     */
    fun changeTimeRange(timeRange: TimeRange) {
        if (_selectedTimeRange.value != timeRange) {
            _selectedTimeRange.value = timeRange
            loadDashboardData(showLoading = false)
        }
    }
    
    /**
     * Refresh dashboard data (for pull-to-refresh)
     */
    fun refreshData() {
        loadDashboardData(showLoading = false)
    }
    
    /**
     * Sync data from connected devices
     */
    fun syncDevices() {
        // Cancel any existing sync job
        syncJob?.cancel()
        
        syncJob = viewModelScope.launch {
            try {
                // Get connected devices
                val connectedDevices = deviceRepository.getConnectedDevices().first()
                
                if (connectedDevices.isEmpty()) {
                    _errorEvents.emit(DashboardErrorEvent.SyncError("No connected devices found"))
                    return@launch
                }
                
                // Update sync status
                _deviceSyncStatus.value = DeviceSyncStatus.Syncing(0, connectedDevices.size)
                
                // Sync each device
                var completedDevices = 0
                var syncedItemsCount = 0
                
                for (device in connectedDevices) {
                    // Create sync parameters
                    val syncParams = SyncParams(
                        deviceId = device.id,
                        syncType = SyncType.INCREMENTAL,
                        dataTypes = device.features.mapNotNull { feature ->
                            when (feature) {
                                DeviceFeature.HEART_RATE -> DeviceDataType.HEART_RATE
                                DeviceFeature.BLOOD_OXYGEN -> DeviceDataType.BLOOD_OXYGEN
                                DeviceFeature.BLOOD_PRESSURE -> DeviceDataType.BLOOD_PRESSURE
                                DeviceFeature.STEPS -> DeviceDataType.STEPS
                                DeviceFeature.SLEEP -> DeviceDataType.SLEEP
                                DeviceFeature.ACTIVITY_TRACKING -> DeviceDataType.ACTIVITY
                                DeviceFeature.TEMPERATURE -> DeviceDataType.TEMPERATURE
                                else -> null
                            }
                        },
                        disconnectAfterSync = false
                    )
                    
                    // Sync device
                    syncDeviceUseCase(syncParams).collect { progress ->
                        when (progress) {
                            is SyncProgress.OverallProgress -> {
                                // Update progress
                                _deviceSyncStatus.value = DeviceSyncStatus.Syncing(
                                    completedDevices + (progress.progress / 100.0).toInt(),
                                    connectedDevices.size
                                )
                            }
                            is SyncProgress.Completed -> {
                                // Device sync completed
                                completedDevices++
                                syncedItemsCount += progress.summary.totalItemsSynced
                                
                                // Update progress
                                _deviceSyncStatus.value = DeviceSyncStatus.Syncing(
                                    completedDevices,
                                    connectedDevices.size
                                )
                            }
                            is SyncProgress.Error -> {
                                // Device sync failed
                                completedDevices++
                                
                                // Update progress
                                _deviceSyncStatus.value = DeviceSyncStatus.Syncing(
                                    completedDevices,
                                    connectedDevices.size
                                )
                                
                                // Emit error
                                _errorEvents.emit(DashboardErrorEvent.SyncError(
                                    "Failed to sync device ${device.name}: ${progress.error.message}"
                                ))
                            }
                            else -> {
                                // Ignore other progress updates
                            }
                        }
                    }
                }
                
                // All devices synced
                _deviceSyncStatus.value = DeviceSyncStatus.Completed(syncedItemsCount)
                
                // Refresh dashboard data
                delay(500) // Small delay to ensure data is available
                loadDashboardData(showLoading = false)
                
                // Reset sync status after a delay
                delay(3000)
                _deviceSyncStatus.value = DeviceSyncStatus.Idle
                
            } catch (e: Exception) {
                Timber.e(e, "Error syncing devices")
                _deviceSyncStatus.value = DeviceSyncStatus.Error("Sync failed: ${e.message}")
                _errorEvents.emit(DashboardErrorEvent.SyncError("Failed to sync devices: ${e.message}"))
                
                // Reset sync status after a delay
                delay(3000)
                _deviceSyncStatus.value = DeviceSyncStatus.Idle
            }
        }
    }
    
    /**
     * Navigate to health insights screen
     */
    fun navigateToHealthInsights() {
        navigationEvent.value = DashboardNavigationEvent.ToHealthInsights
    }
    
    /**
     * Navigate to health goals screen
     */
    fun navigateToHealthGoals() {
        navigationEvent.value = DashboardNavigationEvent.ToHealthGoals
    }
    
    /**
     * Navigate to device management screen
     */
    fun navigateToDeviceManagement() {
        navigationEvent.value = DashboardNavigationEvent.ToDeviceManagement
    }
    
    /**
     * Navigate to health metric detail screen
     */
    fun navigateToMetricDetail(metricType: MetricType) {
        navigationEvent.value = DashboardNavigationEvent.ToMetricDetail(metricType)
    }
    
    /**
     * Navigate to activity detail screen
     */
    fun navigateToActivityDetail(activityId: Long) {
        navigationEvent.value = DashboardNavigationEvent.ToActivityDetail(activityId)
    }
    
    /**
     * Navigate to manual data entry screen
     */
    fun navigateToManualDataEntry() {
        navigationEvent.value = DashboardNavigationEvent.ToManualDataEntry
    }
    
    /**
     * Load health data for the selected time range
     */
    private suspend fun loadHealthData(startDate: LocalDateTime, endDate: LocalDateTime) {
        try {
            // Load heart rate data
            val heartRateData = healthDataRepository.getHeartRateMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            // Load blood oxygen data
            val bloodOxygenData = healthDataRepository.getBloodOxygenMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            // Load blood pressure data
            val bloodPressureData = healthDataRepository.getBloodPressureMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            // Load step data
            val stepData = healthDataRepository.getStepRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            // Load sleep data
            val sleepData = healthDataRepository.getSleepRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            // Load temperature data
            val temperatureData = healthDataRepository.getTemperatureMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            // Calculate health metrics summary
            val healthMetricsSummary = calculateHealthMetricsSummary(
                heartRateData = heartRateData,
                bloodOxygenData = bloodOxygenData,
                bloodPressureData = bloodPressureData,
                stepData = stepData,
                sleepData = sleepData,
                temperatureData = temperatureData
            )
            
            // Update health metrics
            _healthMetrics.value = healthMetricsSummary
            
            // Prepare chart data
            prepareHeartRateChartData(heartRateData)
            prepareStepsChartData(stepData)
            prepareSleepChartData(sleepData)
            prepareBloodOxygenChartData(bloodOxygenData)
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading health data")
            _errorEvents.emit(DashboardErrorEvent.LoadingError("Failed to load health data", e))
        }
    }
    
    /**
     * Load health insights for the selected time range
     */
    private suspend fun loadHealthInsights(timeRange: TimeRange) {
        try {
            // Get health insights
            getHealthInsightsUseCase(
                userId = userId,
                timeRange = timeRange,
                categories = InsightCategory.values(),
                forceRefresh = false
            ).collect { result ->
                when (result) {
                    is HealthInsightsResult.Success -> {
                        // Update health insights
                        _healthInsights.value = result.insights.insights
                        
                        // Update health score
                        _healthScore.value = result.insights.healthScore
                        
                        // Extract critical alerts
                        _criticalAlerts.value = result.insights.insights.filter { 
                            it.severity == InsightSeverity.HIGH || it.severity == InsightSeverity.CRITICAL 
                        }
                    }
                    is HealthInsightsResult.Error -> {
                        Timber.e("Error loading health insights: ${result.error.message}")
                        _errorEvents.emit(DashboardErrorEvent.InsightsError(result.error.message))
                    }
                    else -> {
                        // Ignore loading state
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading health insights")
            _errorEvents.emit(DashboardErrorEvent.InsightsError("Failed to load health insights: ${e.message}"))
        }
    }
    
    /**
     * Load active goals
     */
    private suspend fun loadActiveGoals() {
        try {
            // Get active goals
            manageHealthGoalsUseCase.getGoalsByStatus(userId, GoalStatus.ACTIVE).collect { goals ->
                _activeGoals.value = goals
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading active goals")
            _errorEvents.emit(DashboardErrorEvent.GoalsError("Failed to load active goals: ${e.message}"))
        }
    }
    
    /**
     * Load recent activities
     */
    private suspend fun loadRecentActivities(startDate: LocalDateTime, endDate: LocalDateTime) {
        try {
            // Get recent activities
            healthDataRepository.getActivityRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).collect { activities ->
                _recentActivities.value = activities.sortedByDescending { it.startTime }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading recent activities")
            _errorEvents.emit(DashboardErrorEvent.LoadingError("Failed to load recent activities: ${e.message}"))
        }
    }
    
    /**
     * Calculate health metrics summary
     */
    private fun calculateHealthMetricsSummary(
        heartRateData: List<HeartRateMeasurement>,
        bloodOxygenData: List<BloodOxygenMeasurement>,
        bloodPressureData: List<BloodPressureMeasurement>,
        stepData: List<StepRecord>,
        sleepData: List<SleepRecord>,
        temperatureData: List<TemperatureMeasurement>
    ): HealthMetricsSummary {
        // Heart rate summary
        val avgHeartRate = heartRateData.map { it.value }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        val minHeartRate = heartRateData.minByOrNull { it.value }?.value
        val maxHeartRate = heartRateData.maxByOrNull { it.value }?.value
        val restingHeartRate = heartRateData
            .filter { it.measurementContext == MeasurementContext.RESTING }
            .takeIf { it.isNotEmpty() }
            ?.map { it.value }
            ?.average()
            ?.toInt()
        
        // Blood oxygen summary
        val avgBloodOxygen = bloodOxygenData.map { it.percentage }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        val minBloodOxygen = bloodOxygenData.minByOrNull { it.percentage }?.percentage
        
        // Blood pressure summary
        val avgSystolic = bloodPressureData.map { it.systolic }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        val avgDiastolic = bloodPressureData.map { it.diastolic }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        val latestBloodPressure = bloodPressureData.maxByOrNull { it.timestamp }
        
        // Steps summary
        val totalSteps = stepData.sumOf { it.count }
        val avgSteps = stepData
            .groupBy { it.date }
            .map { it.value.sumOf { record -> record.count } }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toInt()
        val todaySteps = stepData
            .filter { it.date == LocalDate.now() }
            .sumOf { it.count }
        
        // Sleep summary
        val avgSleepDuration = sleepData
            .map { it.durationMinutes }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toInt()
        val lastNightSleep = sleepData
            .filter { it.startTime.toLocalDate() == LocalDate.now().minusDays(1) }
            .maxByOrNull { it.durationMinutes }
        
        // Temperature summary
        val avgTemperature = temperatureData
            .map { it.value }
            .takeIf { it.isNotEmpty() }
            ?.average()
        val latestTemperature = temperatureData.maxByOrNull { it.timestamp }
        
        return HealthMetricsSummary(
            heartRate = HeartRateSummary(
                average = avgHeartRate,
                min = minHeartRate,
                max = maxHeartRate,
                resting = restingHeartRate,
                latest = heartRateData.maxByOrNull { it.timestamp }
            ),
            bloodOxygen = BloodOxygenSummary(
                average = avgBloodOxygen,
                min = minBloodOxygen,
                latest = bloodOxygenData.maxByOrNull { it.timestamp }
            ),
            bloodPressure = BloodPressureSummary(
                averageSystolic = avgSystolic,
                averageDiastolic = avgDiastolic,
                latest = latestBloodPressure
            ),
            steps = StepsSummary(
                total = totalSteps,
                average = avgSteps,
                today = todaySteps
            ),
            sleep = SleepSummary(
                averageDuration = avgSleepDuration,
                lastNight = lastNightSleep
            ),
            temperature = TemperatureSummary(
                average = avgTemperature,
                latest = latestTemperature
            )
        )
    }
    
    /**
     * Prepare heart rate chart data
     */
    private fun prepareHeartRateChartData(heartRateData: List<HeartRateMeasurement>) {
        if (heartRateData.isEmpty()) {
            _heartRateChartData.value = null
            return
        }
        
        // Group by hour or day depending on time range
        val groupedData = when (_selectedTimeRange.value) {
            is TimeRange.DAY -> {
                // Group by hour
                heartRateData.groupBy { 
                    it.timestamp.format(DateTimeFormatter.ofPattern("HH:00"))
                }
            }
            else -> {
                // Group by day
                heartRateData.groupBy { 
                    it.timestamp.format(DateTimeFormatter.ofPattern("MM-dd"))
                }
            }
        }
        
        // Calculate average for each group
        val chartPoints = groupedData.map { (label, measurements) ->
            val avgValue = measurements.map { it.value }.average().toInt()
            ChartPoint(label, avgValue.toDouble())
        }.sortedBy { it.label }
        
        // Create chart data
        _heartRateChartData.value = ChartData(
            title = "Heart Rate",
            points = chartPoints,
            yAxisLabel = "BPM",
            color = "#E53935" // Red
        )
    }
    
    /**
     * Prepare steps chart data
     */
    private fun prepareStepsChartData(stepData: List<StepRecord>) {
        if (stepData.isEmpty()) {
            _stepsChartData.value = null
            return
        }
        
        // Group by day
        val groupedData = stepData.groupBy { 
            it.date.format(DateTimeFormatter.ofPattern("MM-dd"))
        }
        
        // Calculate total for each day
        val chartPoints = groupedData.map { (label, records) ->
            val totalSteps = records.sumOf { it.count }
            ChartPoint(label, totalSteps.toDouble())
        }.sortedBy { it.label }
        
        // Create chart data
        _stepsChartData.value = ChartData(
            title = "Steps",
            points = chartPoints,
            yAxisLabel = "Steps",
            color = "#43A047" // Green
        )
    }
    
    /**
     * Prepare sleep chart data
     */
    private fun prepareSleepChartData(sleepData: List<SleepRecord>) {
        if (sleepData.isEmpty()) {
            _sleepChartData.value = null
            return
        }
        
        // Group by day
        val groupedData = sleepData.groupBy { 
            it.startTime.format(DateTimeFormatter.ofPattern("MM-dd"))
        }
        
        // Get maximum duration for each day
        val chartPoints = groupedData.map { (label, records) ->
            val maxDuration = records.maxOfOrNull { it.durationMinutes } ?: 0
            ChartPoint(label, maxDuration.toDouble() / 60.0) // Convert to hours
        }.sortedBy { it.label }
        
        // Create chart data
        _sleepChartData.value = ChartData(
            title = "Sleep Duration",
            points = chartPoints,
            yAxisLabel = "Hours",
            color = "#5E35B1" // Purple
        )
    }
    
    /**
     * Prepare blood oxygen chart data
     */
    private fun prepareBloodOxygenChartData(bloodOxygenData: List<BloodOxygenMeasurement>) {
        if (bloodOxygenData.isEmpty()) {
            _bloodOxygenChartData.value = null
            return
        }
        
        // Group by hour or day depending on time range
        val groupedData = when (_selectedTimeRange.value) {
            is TimeRange.DAY -> {
                // Group by hour
                bloodOxygenData.groupBy { 
                    it.timestamp.format(DateTimeFormatter.ofPattern("HH:00"))
                }
            }
            else -> {
                // Group by day
                bloodOxygenData.groupBy { 
                    it.timestamp.format(DateTimeFormatter.ofPattern("MM-dd"))
                }
            }
        }
        
        // Calculate average for each group
        val chartPoints = groupedData.map { (label, measurements) ->
            val avgValue = measurements.map { it.percentage }.average()
            ChartPoint(label, avgValue)
        }.sortedBy { it.label }
        
        // Create chart data
        _bloodOxygenChartData.value = ChartData(
            title = "Blood Oxygen",
            points = chartPoints,
            yAxisLabel = "%",
            color = "#1E88E5" // Blue
        )
    }
    
    /**
     * Calculate date range for the selected time range
     */
    private fun calculateDateRange(timeRange: TimeRange): Pair<LocalDateTime, LocalDateTime> {
        val endDate = LocalDateTime.now()
        val startDate = when (timeRange) {
            is TimeRange.DAY -> endDate.minusDays(1)
            is TimeRange.WEEK -> endDate.minusDays(7)
            is TimeRange.MONTH -> endDate.minusDays(30)
            is TimeRange.QUARTER -> endDate.minusDays(90)
            is TimeRange.YEAR -> endDate.minusDays(365)
            is TimeRange.CUSTOM -> timeRange.startDate
        }
        return Pair(startDate, endDate)
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
                delay(5 * 60 * 1000) // Refresh every 5 minutes
                loadDashboardData(showLoading = false)
            }
        }
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        
        // Cancel all jobs
        dataRefreshJob?.cancel()
        syncJob?.cancel()
        autoRefreshJob?.cancel()
    }
}

/**
 * Sealed class representing the UI state for the health dashboard
 */
sealed class DashboardUiState {
    /**
     * Loading state
     */
    data object Loading : DashboardUiState()
    
    /**
     * Success state
     */
    data class Success(
        val userName: String,
        val timeRange: TimeRange,
        val lastUpdated: LocalDateTime,
        val hasConnectedDevices: Boolean,
        val hasActiveGoals: Boolean,
        val hasHealthInsights: Boolean,
        val hasCriticalAlerts: Boolean
    ) : DashboardUiState() {
        /**
         * Format last updated time as a string
         */
        fun getLastUpdatedFormatted(): String {
            val now = LocalDateTime.now()
            val minutes = java.time.temporal.ChronoUnit.MINUTES.between(lastUpdated, now)
            
            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
                minutes < 24 * 60 -> "${minutes / 60} hour${if (minutes / 60 > 1) "s" else ""} ago"
                else -> lastUpdated.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            }
        }
    }
    
    /**
     * Error state
     */
    data class Error(val message: String) : DashboardUiState()
}

/**
 * Sealed class representing the device sync status
 */
sealed class DeviceSyncStatus {
    /**
     * Idle state (not syncing)
     */
    data object Idle : DeviceSyncStatus()
    
    /**
     * Syncing state
     */
    data class Syncing(
        val completedDevices: Int,
        val totalDevices: Int
    ) : DeviceSyncStatus() {
        /**
         * Get progress as a percentage
         */
        fun getProgressPercentage(): Int {
            return if (totalDevices > 0) {
                (completedDevices * 100) / totalDevices
            } else {
                0
            }
        }
        
        /**
         * Format progress as a string
         */
        fun getProgressFormatted(): String {
            return "$completedDevices/$totalDevices devices"
        }
    }
    
    /**
     * Completed state
     */
    data class Completed(val itemsSynced: Int) : DeviceSyncStatus() {
        /**
         * Format completion message
         */
        fun getCompletionMessage(): String {
            return "Synced $itemsSynced items"
        }
    }
    
    /**
     * Error state
     */
    data class Error(val message: String) : DeviceSyncStatus()
}

/**
 * Data class representing health metrics summary
 */
data class HealthMetricsSummary(
    val heartRate: HeartRateSummary,
    val bloodOxygen: BloodOxygenSummary,
    val bloodPressure: BloodPressureSummary,
    val steps: StepsSummary,
    val sleep: SleepSummary,
    val temperature: TemperatureSummary
)

/**
 * Data class representing heart rate summary
 */
data class HeartRateSummary(
    val average: Int?,
    val min: Int?,
    val max: Int?,
    val resting: Int?,
    val latest: HeartRateMeasurement?
) {
    /**
     * Format average heart rate as a string
     */
    fun getAverageFormatted(): String {
        return average?.toString() ?: "N/A"
    }
    
    /**
     * Format resting heart rate as a string
     */
    fun getRestingFormatted(): String {
        return resting?.toString() ?: "N/A"
    }
    
    /**
     * Format range as a string
     */
    fun getRangeFormatted(): String {
        return if (min != null && max != null) {
            "$min-$max bpm"
        } else {
            "N/A"
        }
    }
    
    /**
     * Format latest heart rate as a string
     */
    fun getLatestFormatted(): String {
        return latest?.value?.toString() ?: "N/A"
    }
    
    /**
     * Get latest measurement time formatted
     */
    fun getLatestTimeFormatted(): String {
        return latest?.timestamp?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "N/A"
    }
    
    /**
     * Get heart rate zone
     */
    fun getHeartRateZone(): String {
        val value = average ?: return "N/A"
        
        return when {
            value < 60 -> "Low"
            value < 100 -> "Normal"
            value < 140 -> "Elevated"
            else -> "High"
        }
    }
    
    /**
     * Get color based on heart rate zone
     */
    fun getZoneColor(): String {
        val value = average ?: return "#9E9E9E" // Gray
        
        return when {
            value < 60 -> "#2196F3" // Blue
            value < 100 -> "#4CAF50" // Green
            value < 140 -> "#FFC107" // Yellow
            else -> "#F44336" // Red
        }
    }
}

/**
 * Data class representing blood oxygen summary
 */
data class BloodOxygenSummary(
    val average: Int?,
    val min: Int?,
    val latest: BloodOxygenMeasurement?
) {
    /**
     * Format average blood oxygen as a string
     */
    fun getAverageFormatted(): String {
        return average?.toString() ?: "N/A"
    }
    
    /**
     * Format minimum blood oxygen as a string
     */
    fun getMinFormatted(): String {
        return min?.toString() ?: "N/A"
    }
    
    /**
     * Format latest blood oxygen as a string
     */
    fun getLatestFormatted(): String {
        return latest?.percentage?.toString() ?: "N/A"
    }
    
    /**
     * Get latest measurement time formatted
     */
    fun getLatestTimeFormatted(): String {
        return latest?.timestamp?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "N/A"
    }
    
    /**
     * Get blood oxygen status
     */
    fun getStatus(): String {
        val value = average ?: return "N/A"
        
        return when {
            value >= 95 -> "Normal"
            value >= 90 -> "Low"
            else -> "Very Low"
        }
    }
    
    /**
     * Get color based on blood oxygen status
     */
    fun getStatusColor(): String {
        val value = average ?: return "#9E9E9E" // Gray
        
        return when {
            value >= 95 -> "#4CAF50" // Green
            value >= 90 -> "#FFC107" // Yellow
            else -> "#F44336" // Red
        }
    }
}

/**
 * Data class representing blood pressure summary
 */
data class BloodPressureSummary(
    val averageSystolic: Int?,
    val averageDiastolic: Int?,
    val latest: BloodPressureMeasurement?
) {
    /**
     * Format average blood pressure as a string
     */
    fun getAverageFormatted(): String {
        return if (averageSystolic != null && averageDiastolic != null) {
            "$averageSystolic/$averageDiastolic"
        } else {
            "N/A"
        }
    }
    
    /**
     * Format latest blood pressure as a string
     */
    fun getLatestFormatted(): String {
        return latest?.let { "${it.systolic}/${it.diastolic}" } ?: "N/A"
    }
    
    /**
     * Get latest measurement time formatted
     */
    fun getLatestTimeFormatted(): String {
        return latest?.timestamp?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "N/A"
    }
    
    /**
     * Get blood pressure category
     */
    fun getCategory(): String {
        val systolic = averageSystolic ?: return "N/A"
        val diastolic = averageDiastolic ?: return "N/A"
        
        return when {
            systolic < 120 && diastolic < 80 -> "Normal"
            systolic < 130 && diastolic < 85 -> "Elevated"
            systolic < 140 && diastolic < 90 -> "Stage 1 Hypertension"
            systolic < 180 && diastolic < 120 -> "Stage 2 Hypertension"
            else -> "Hypertensive Crisis"
        }
    }
    
    /**
     * Get color based on blood pressure category
     */
    fun getCategoryColor(): String {
        val systolic = averageSystolic ?: return "#9E9E9E" // Gray
        val diastolic = averageDiastolic ?: return "#9E9E9E" // Gray
        
        return when {
            systolic < 120 && diastolic < 80 -> "#4CAF50" // Green
            systolic < 130 && diastolic < 85 -> "#8BC34A" // Light Green
            systolic < 140 && diastolic < 90 -> "#FFC107" // Yellow
            systolic < 180 && diastolic < 120 -> "#FF9800" // Orange
            else -> "#F44336" // Red
        }
    }
}

/**
 * Data class representing steps summary
 */
data class StepsSummary(
    val total: Int,
    val average: Int?,
    val today: Int
) {
    /**
     * Format total steps as a string
     */
    fun getTotalFormatted(): String {
        return total.toString()
    }
    
    /**
     * Format average steps as a string
     */
    fun getAverageFormatted(): String {
        return average?.toString() ?: "N/A"
    }
    
    /**
     * Format today's steps as a string
     */
    fun getTodayFormatted(): String {
        return today.toString()
    }
    
    /**
     * Get progress towards daily goal (assuming 10,000 steps)
     */
    fun getProgressTowardsGoal(goal: Int = 10000): Int {
        return ((today.toDouble() / goal) * 100).toInt().coerceIn(0, 100)
    }
    
    /**
     * Get activity level based on average steps
     */
    fun getActivityLevel(): String {
        val avg = average ?: return "N/A"
        
        return when {
            avg < 5000 -> "Sedentary"
            avg < 7500 -> "Low Active"
            avg < 10000 -> "Somewhat Active"
            avg < 12500 -> "Active"
            else -> "Very Active"
        }
    }
    
    /**
     * Get color based on activity level
     */
    fun getActivityLevelColor(): String {
        val avg = average ?: return "#9E9E9E" // Gray
        
        return when {
            avg < 5000 -> "#F44336" // Red
            avg < 7500 -> "#FF9800" // Orange
            avg < 10000 -> "#FFC107" // Yellow
            avg < 12500 -> "#8BC34A" // Light Green
            else -> "#4CAF50" // Green
        }
    }
}

/**
 * Data class representing sleep summary
 */
data class SleepSummary(
    val averageDuration: Int?,
    val lastNight: SleepRecord?
) {
    /**
     * Format average sleep duration as a string
     */
    fun getAverageFormatted(): String {
        return averageDuration?.let { 
            val hours = it / 60
            val minutes = it % 60
            "$hours h $minutes min"
        } ?: "N/A"
    }
    
    /**
     * Format last night's sleep duration as a string
     */
    fun getLastNightFormatted(): String {
        return lastNight?.let { 
            val hours = it.durationMinutes / 60
            val minutes = it.durationMinutes % 60
            "$hours h $minutes min"
        } ?: "N/A"
    }
    
    /**
     * Get sleep quality as a string
     */
    fun getSleepQuality(): String {
        return lastNight?.sleepQuality?.name?.lowercase()?.capitalize() ?: "N/A"
    }
    
    /**
     * Get color based on sleep quality
     */
    fun getSleepQualityColor(): String {
        return when (lastNight?.sleepQuality) {
            SleepQuality.EXCELLENT -> "#4CAF50" // Green
            SleepQuality.GOOD -> "#8BC34A" // Light Green
            SleepQuality.FAIR -> "#FFC107" // Yellow
            SleepQuality.POOR -> "#FF9800" // Orange
            SleepQuality.VERY_POOR -> "#F44336" // Red
            null -> "#9E9E9E" // Gray
        }
    }
    
    /**
     * Get sleep status based on duration
     */
    fun getSleepStatus(): String {
        val avg = averageDuration ?: return "N/A"
        
        return when {
            avg >= 480 -> "Optimal" // 8+ hours
            avg >= 420 -> "Good" // 7+ hours
            avg >= 360 -> "Fair" // 6+ hours
            else -> "Insufficient"
        }
    }
}

/**
 * Data class representing temperature summary
 */
data class TemperatureSummary(
    val average: Double?,
    val latest: TemperatureMeasurement?
) {
    /**
     * Format average temperature as a string
     */
    fun getAverageFormatted(): String {
        return average?.let { String.format("%.1f°C", it) } ?: "N/A"
    }
    
    /**
     * Format latest temperature as a string
     */
    fun getLatestFormatted(): String {
        return latest?.let { String.format("%.1f°C", it.value) } ?: "N/A"
    }
    
    /**
     * Get latest measurement time formatted
     */
    fun getLatestTimeFormatted(): String {
        return latest?.timestamp?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: "N/A"
    }
    
    /**
     * Get temperature status
     */
    fun getStatus(): String {
        val value = latest?.value ?: return "N/A"
        
        return when {
            value < 36.1 -> "Low"
            value <= 37.2 -> "Normal"
            value <= 38.0 -> "Elevated"
            else -> "High"
        }
    }
    
    /**
     * Get color based on temperature status
     */
    fun getStatusColor(): String {
        val value = latest?.value ?: return "#9E9E9E" // Gray
        
        return when {
            value < 36.1 -> "#2196F3" // Blue
            value <= 37.2 -> "#4CAF50" // Green
            value <= 38.0 -> "#FFC107" // Yellow
            else -> "#F44336" // Red
        }
    }
}

/**
 * Data class representing chart data
 */
data class ChartData(
    val title: String,
    val points: List<ChartPoint>,
    val yAxisLabel: String,
    val color: String
) {
    /**
     * Get minimum Y value
     */
    fun getMinY(): Double {
        return points.minOfOrNull { it.value } ?: 0.0
    }
    
    /**
     * Get maximum Y value
     */
    fun getMaxY(): Double {
        return points.maxOfOrNull { it.value } ?: 0.0
    }
    
    /**
     * Get average Y value
     */
    fun getAverageY(): Double {
        return points.map { it.value }.average()
    }
    
    /**
     * Get trend direction
     */
    fun getTrendDirection(): TrendDirection {
        if (points.size < 2) return TrendDirection.STABLE
        
        val first = points.first().value
        val last = points.last().value
        val difference = last - first
        
        return when {
            difference > 0 -> TrendDirection.INCREASING
            difference < 0 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
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
 * Enum representing trend directions
 */
enum class TrendDirection {
    INCREASING,
    DECREASING,
    STABLE
}

/**
 * Sealed class for dashboard error events
 */
sealed class DashboardErrorEvent {
    /**
     * Loading error
     */
    data class LoadingError(
        val message: String,
        val cause: Throwable? = null
    ) : DashboardErrorEvent()
    
    /**
     * Sync error
     */
    data class SyncError(val message: String) : DashboardErrorEvent()
    
    /**
     * Insights error
     */
    data class InsightsError(val message: String) : DashboardErrorEvent()
    
    /**
     * Goals error
     */
    data class GoalsError(val message: String) : DashboardErrorEvent()
}

/**
 * Sealed class for dashboard navigation events
 */
sealed class DashboardNavigationEvent {
    /**
     * Navigate to health insights screen
     */
    data object ToHealthInsights : DashboardNavigationEvent()
    
    /**
     * Navigate to health goals screen
     */
    data object ToHealthGoals : DashboardNavigationEvent()
    
    /**
     * Navigate to device management screen
     */
    data object ToDeviceManagement : DashboardNavigationEvent()
    
    /**
     * Navigate to metric detail screen
     */
    data class ToMetricDetail(val metricType: MetricType) : DashboardNavigationEvent()
    
    /**
     * Navigate to activity detail screen
     */
    data class ToActivityDetail(val activityId: Long) : DashboardNavigationEvent()
    
    /**
     * Navigate to manual data entry screen
     */
    data object ToManualDataEntry : DashboardNavigationEvent()
}

/**
 * Extension function to capitalize the first letter of a string
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
}
