package com.sensacare.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensacare.app.data.local.preferences.UserPreferences
import com.sensacare.app.data.model.device.DeviceStatusUpdateDto
import com.sensacare.app.data.remote.*
import com.sensacare.app.data.remote.api.*
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.util.Result
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
 * Dashboard ViewModel for SensaCare app
 * Integrates with RPM platform's dynamic vitals system
 * Manages real-time vital readings display, trends, and analytics
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val platformIntegrationManager: PlatformIntegrationManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    // Client ID
    private val _clientId = MutableStateFlow<String?>(null)
    val clientId: StateFlow<String?> = _clientId.asStateFlow()
    
    // Dashboard UI state
    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
    
    // Vital readings by type
    private val _vitalReadings = MutableStateFlow<Map<String, VitalTypeReadings>>(emptyMap())
    val vitalReadings: StateFlow<Map<String, VitalTypeReadings>> = _vitalReadings.asStateFlow()
    
    // Latest vital readings by type
    private val _latestVitalReadings = MutableStateFlow<Map<String, VitalReadingDto?>>(emptyMap())
    val latestVitalReadings: StateFlow<Map<String, VitalReadingDto?>> = _latestVitalReadings.asStateFlow()
    
    // Vital thresholds by type
    private val _vitalThresholds = MutableStateFlow<Map<String, ThresholdValues>>(emptyMap())
    val vitalThresholds: StateFlow<Map<String, ThresholdValues>> = _vitalThresholds.asStateFlow()
    
    // Vital configuration
    private val _vitalConfiguration = MutableStateFlow<ClientVitalConfigurationDto?>(null)
    val vitalConfiguration: StateFlow<ClientVitalConfigurationDto?> = _vitalConfiguration.asStateFlow()
    
    // Enabled vital types
    private val _enabledVitalTypes = MutableStateFlow<List<String>>(emptyList())
    val enabledVitalTypes: StateFlow<List<String>> = _enabledVitalTypes.asStateFlow()
    
    // Device status
    private val _deviceStatus = MutableStateFlow<Map<String, DeviceConnectionStatus>>(emptyMap())
    val deviceStatus: StateFlow<Map<String, DeviceConnectionStatus>> = _deviceStatus.asStateFlow()
    
    // Alerts
    private val _alerts = MutableStateFlow<List<AlertDto>>(emptyList())
    val alerts: StateFlow<List<AlertDto>> = _alerts.asStateFlow()
    
    // Sync status
    private val _syncStatus = MutableStateFlow<SyncStatusUiState>(SyncStatusUiState.Idle)
    val syncStatus: StateFlow<SyncStatusUiState> = _syncStatus.asStateFlow()
    
    // Selected time range
    private val _selectedTimeRange = MutableStateFlow(TimeRangeOption.DAY)
    val selectedTimeRange: StateFlow<TimeRangeOption> = _selectedTimeRange.asStateFlow()
    
    // Selected vital for detailed view
    private val _selectedVitalType = MutableStateFlow<String?>(null)
    val selectedVitalType: StateFlow<String?> = _selectedVitalType.asStateFlow()
    
    // Vital trends
    private val _vitalTrends = MutableStateFlow<Map<String, VitalTrend>>(emptyMap())
    val vitalTrends: StateFlow<Map<String, VitalTrend>> = _vitalTrends.asStateFlow()
    
    // Dashboard events
    private val _dashboardEvents = MutableSharedFlow<DashboardEvent>()
    val dashboardEvents: SharedFlow<DashboardEvent> = _dashboardEvents.asSharedFlow()
    
    // Auto refresh job
    private var autoRefreshJob: Job? = null
    
    // Time formatter
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    
    init {
        Timber.d("Initializing DashboardViewModel")
        
        // Load client ID
        viewModelScope.launch {
            userPreferences.getUserId()?.let { userId ->
                _clientId.value = userId
                loadDashboardData()
            }
        }
        
        // Observe platform events
        viewModelScope.launch {
            platformIntegrationManager.platformEvents.collect { event ->
                handlePlatformEvent(event)
            }
        }
        
        // Observe WebSocket events
        viewModelScope.launch {
            platformIntegrationManager.webSocketEvents.collect { event ->
                handleWebSocketEvent(event)
            }
        }
        
        // Observe sync status
        viewModelScope.launch {
            platformIntegrationManager.syncStatus.collect { status ->
                handleSyncStatus(status)
            }
        }
        
        // Start auto refresh
        startAutoRefresh()
    }
    
    /**
     * Load dashboard data
     */
    fun loadDashboardData() {
        Timber.d("Loading dashboard data")
        
        val clientId = _clientId.value ?: return
        
        // Update UI state
        _dashboardState.value = DashboardState.Loading
        
        viewModelScope.launch {
            try {
                // Fetch client vitals
                val timeRange = when (_selectedTimeRange.value) {
                    TimeRangeOption.DAY -> "24h"
                    TimeRangeOption.WEEK -> "7d"
                    TimeRangeOption.MONTH -> "30d"
                    TimeRangeOption.YEAR -> "365d"
                }
                
                val vitalsResult = platformIntegrationManager.getApiService().getClientVitals(
                    clientId = clientId,
                    timeRange = timeRange,
                    includeDeviceStatus = true,
                    includeAlerts = true
                )
                
                if (vitalsResult is Result.Success) {
                    val clientVitals = vitalsResult.data
                    
                    // Update vital readings
                    _vitalReadings.value = clientVitals.readingsByType
                    
                    // Update latest vital readings
                    _latestVitalReadings.value = clientVitals.latestReadingsByType
                    
                    // Update device status
                    updateDeviceStatus(clientVitals)
                    
                    // Calculate vital trends
                    calculateVitalTrends()
                    
                    // Update UI state
                    _dashboardState.value = DashboardState.Loaded
                } else if (vitalsResult is Result.Error) {
                    Timber.e(vitalsResult.exception, "Failed to fetch client vitals")
                    _dashboardState.value = DashboardState.Error("Failed to fetch client vitals: ${vitalsResult.exception.message}")
                }
                
                // Fetch client vital configuration
                val configResult = platformIntegrationManager.getApiService().getClientVitalConfiguration(clientId)
                
                if (configResult is Result.Success) {
                    val config = configResult.data.configuration
                    _vitalConfiguration.value = config
                    _enabledVitalTypes.value = config.enabledVitals
                } else if (configResult is Result.Error) {
                    Timber.e(configResult.exception, "Failed to fetch client vital configuration")
                }
                
                // Fetch client thresholds
                val thresholdsResult = platformIntegrationManager.getApiService().getClientThresholds(clientId)
                
                if (thresholdsResult is Result.Success) {
                    _vitalThresholds.value = thresholdsResult.data.thresholds
                } else if (thresholdsResult is Result.Error) {
                    Timber.e(thresholdsResult.exception, "Failed to fetch client thresholds")
                }
                
                // Fetch client alerts
                val alertsResult = platformIntegrationManager.getApiService().getClientAlerts(
                    clientId = clientId,
                    status = "active",
                    startDate = LocalDateTime.now().minusDays(7).toString()
                )
                
                if (alertsResult is Result.Success) {
                    _alerts.value = alertsResult.data.alerts
                } else if (alertsResult is Result.Error) {
                    Timber.e(alertsResult.exception, "Failed to fetch client alerts")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard data")
                _dashboardState.value = DashboardState.Error("Failed to load dashboard data: ${e.message}")
            }
        }
    }
    
    /**
     * Set selected time range
     */
    fun setSelectedTimeRange(timeRange: TimeRangeOption) {
        Timber.d("Setting selected time range: $timeRange")
        
        _selectedTimeRange.value = timeRange
        loadDashboardData()
    }
    
    /**
     * Set selected vital type
     */
    fun setSelectedVitalType(vitalType: String?) {
        Timber.d("Setting selected vital type: $vitalType")
        
        _selectedVitalType.value = vitalType
        
        if (vitalType != null) {
            loadDetailedVitalData(vitalType)
        }
    }
    
    /**
     * Load detailed vital data
     */
    private fun loadDetailedVitalData(vitalType: String) {
        Timber.d("Loading detailed vital data for: $vitalType")
        
        val clientId = _clientId.value ?: return
        
        viewModelScope.launch {
            try {
                val startDate = when (_selectedTimeRange.value) {
                    TimeRangeOption.DAY -> LocalDateTime.now().minusDays(1)
                    TimeRangeOption.WEEK -> LocalDateTime.now().minusWeeks(1)
                    TimeRangeOption.MONTH -> LocalDateTime.now().minusMonths(1)
                    TimeRangeOption.YEAR -> LocalDateTime.now().minusYears(1)
                }
                
                val result = platformIntegrationManager.getApiService().getClientVitalsByType(
                    clientId = clientId,
                    vitalType = vitalType,
                    startDate = startDate.toString(),
                    endDate = LocalDateTime.now().toString(),
                    limit = 500
                )
                
                if (result is Result.Success) {
                    // Update vital readings for selected type
                    val currentReadings = _vitalReadings.value.toMutableMap()
                    currentReadings[vitalType] = VitalTypeReadings(
                        type = vitalType,
                        unit = result.data.readings.firstOrNull()?.unit ?: "",
                        day = result.data.readings.filter { 
                            val timestamp = LocalDateTime.parse(it.timestamp)
                            timestamp.isAfter(LocalDateTime.now().minusDays(1))
                        },
                        week = result.data.readings.filter {
                            val timestamp = LocalDateTime.parse(it.timestamp)
                            timestamp.isAfter(LocalDateTime.now().minusWeeks(1))
                        },
                        month = result.data.readings.filter {
                            val timestamp = LocalDateTime.parse(it.timestamp)
                            timestamp.isAfter(LocalDateTime.now().minusMonths(1))
                        },
                        year = result.data.readings
                    )
                    _vitalReadings.value = currentReadings
                    
                    // Update vital trend for selected type
                    updateVitalTrend(vitalType, result.data.summary)
                    
                    // Emit event
                    _dashboardEvents.emit(DashboardEvent.VitalDetailLoaded(vitalType))
                } else if (result is Result.Error) {
                    Timber.e(result.exception, "Failed to fetch detailed vital data")
                    _dashboardEvents.emit(DashboardEvent.Error("Failed to fetch detailed vital data: ${result.exception.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load detailed vital data")
                _dashboardEvents.emit(DashboardEvent.Error("Failed to load detailed vital data: ${e.message}"))
            }
        }
    }
    
    /**
     * Start sync
     */
    fun startSync() {
        Timber.d("Starting sync")
        
        platformIntegrationManager.startSync(
            SyncConfig(
                syncVitals = true,
                syncVitalsTimeRange = when (_selectedTimeRange.value) {
                    TimeRangeOption.DAY -> SyncTimeRange.LAST_24_HOURS
                    TimeRangeOption.WEEK -> SyncTimeRange.LAST_7_DAYS
                    TimeRangeOption.MONTH -> SyncTimeRange.LAST_30_DAYS
                    TimeRangeOption.YEAR -> SyncTimeRange.CUSTOM
                },
                customTimeRange = if (_selectedTimeRange.value == TimeRangeOption.YEAR) "365d" else null,
                syncVitalConfiguration = true,
                syncThresholds = true,
                includeDeviceStatus = true,
                includeAlerts = true
            )
        )
    }
    
    /**
     * Stop sync
     */
    fun stopSync() {
        Timber.d("Stopping sync")
        
        platformIntegrationManager.stopSync()
    }
    
    /**
     * Connect to WebSocket for real-time updates
     */
    fun connectWebSocket() {
        Timber.d("Connecting to WebSocket")
        
        platformIntegrationManager.connectWebSocket()
    }
    
    /**
     * Disconnect from WebSocket
     */
    fun disconnectWebSocket() {
        Timber.d("Disconnecting from WebSocket")
        
        platformIntegrationManager.disconnectWebSocket()
    }
    
    /**
     * Acknowledge alert
     */
    fun acknowledgeAlert(alertId: String, notes: String? = null) {
        Timber.d("Acknowledging alert: $alertId")
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().acknowledgeAlert(
                    alertId = alertId,
                    acknowledgement = AlertAcknowledgementDto(
                        acknowledgedBy = userPreferences.getUserId() ?: "unknown",
                        notes = notes
                    )
                )
                
                if (result is Result.Success) {
                    // Update alerts list
                    val updatedAlerts = _alerts.value.toMutableList()
                    val index = updatedAlerts.indexOfFirst { it.id == alertId }
                    if (index >= 0) {
                        updatedAlerts[index] = result.data.alert
                    }
                    _alerts.value = updatedAlerts
                    
                    // Emit event
                    _dashboardEvents.emit(DashboardEvent.AlertAcknowledged(alertId))
                } else if (result is Result.Error) {
                    Timber.e(result.exception, "Failed to acknowledge alert")
                    _dashboardEvents.emit(DashboardEvent.Error("Failed to acknowledge alert: ${result.exception.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to acknowledge alert")
                _dashboardEvents.emit(DashboardEvent.Error("Failed to acknowledge alert: ${e.message}"))
            }
        }
    }
    
    /**
     * Resolve alert
     */
    fun resolveAlert(alertId: String, resolutionAction: String, notes: String? = null) {
        Timber.d("Resolving alert: $alertId")
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().resolveAlert(
                    alertId = alertId,
                    resolution = AlertResolutionDto(
                        resolvedBy = userPreferences.getUserId() ?: "unknown",
                        resolutionAction = resolutionAction,
                        notes = notes
                    )
                )
                
                if (result is Result.Success) {
                    // Update alerts list
                    val updatedAlerts = _alerts.value.toMutableList()
                    val index = updatedAlerts.indexOfFirst { it.id == alertId }
                    if (index >= 0) {
                        updatedAlerts[index] = result.data.alert
                    }
                    _alerts.value = updatedAlerts
                    
                    // Emit event
                    _dashboardEvents.emit(DashboardEvent.AlertResolved(alertId))
                } else if (result is Result.Error) {
                    Timber.e(result.exception, "Failed to resolve alert")
                    _dashboardEvents.emit(DashboardEvent.Error("Failed to resolve alert: ${result.exception.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to resolve alert")
                _dashboardEvents.emit(DashboardEvent.Error("Failed to resolve alert: ${e.message}"))
            }
        }
    }
    
    /**
     * Update device status
     */
    fun updateDeviceStatus(deviceId: String, status: String, batteryLevel: Int? = null) {
        Timber.d("Updating device status: $deviceId, status: $status, battery: $batteryLevel")
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().updateDeviceStatus(
                    deviceId = deviceId,
                    status = DeviceStatusUpdateDto(
                        status = status,
                        batteryLevel = batteryLevel,
                        connectionStatus = "connected",
                        lastSeen = LocalDateTime.now().toString(),
                        errorCode = null,
                        errorMessage = null
                    )
                )
                
                if (result is Result.Success) {
                    // Update device status
                    val updatedStatus = _deviceStatus.value.toMutableMap()
                    updatedStatus[deviceId] = DeviceConnectionStatus(
                        deviceId = deviceId,
                        status = status,
                        batteryLevel = batteryLevel,
                        lastSeen = LocalDateTime.now(),
                        isConnected = true
                    )
                    _deviceStatus.value = updatedStatus
                    
                    // Emit event
                    _dashboardEvents.emit(DashboardEvent.DeviceStatusUpdated(deviceId))
                } else if (result is Result.Error) {
                    Timber.e(result.exception, "Failed to update device status")
                    _dashboardEvents.emit(DashboardEvent.Error("Failed to update device status: ${result.exception.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update device status")
                _dashboardEvents.emit(DashboardEvent.Error("Failed to update device status: ${e.message}"))
            }
        }
    }
    
    /**
     * Update client vital configuration
     */
    fun updateClientVitalConfiguration(configuration: ClientVitalConfigurationDto) {
        Timber.d("Updating client vital configuration")
        
        val clientId = _clientId.value ?: return
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().updateClientVitalConfiguration(
                    clientId = clientId,
                    configuration = configuration
                )
                
                if (result is Result.Success) {
                    // Update vital configuration
                    _vitalConfiguration.value = result.data.configuration
                    _enabledVitalTypes.value = result.data.configuration.enabledVitals
                    
                    // Emit event
                    _dashboardEvents.emit(DashboardEvent.VitalConfigurationUpdated)
                } else if (result is Result.Error) {
                    Timber.e(result.exception, "Failed to update client vital configuration")
                    _dashboardEvents.emit(DashboardEvent.Error("Failed to update vital configuration: ${result.exception.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update client vital configuration")
                _dashboardEvents.emit(DashboardEvent.Error("Failed to update vital configuration: ${e.message}"))
            }
        }
    }
    
    /**
     * Update client thresholds
     */
    fun updateClientThresholds(thresholds: Map<String, ThresholdValues>) {
        Timber.d("Updating client thresholds")
        
        val clientId = _clientId.value ?: return
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().updateClientThresholds(
                    clientId = clientId,
                    thresholds = ClientThresholdsDto(
                        clientId = clientId,
                        thresholds = thresholds
                    )
                )
                
                if (result is Result.Success) {
                    // Update thresholds
                    _vitalThresholds.value = result.data.thresholds
                    
                    // Emit event
                    _dashboardEvents.emit(DashboardEvent.ThresholdsUpdated)
                } else if (result is Result.Error) {
                    Timber.e(result.exception, "Failed to update client thresholds")
                    _dashboardEvents.emit(DashboardEvent.Error("Failed to update thresholds: ${result.exception.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to update client thresholds")
                _dashboardEvents.emit(DashboardEvent.Error("Failed to update thresholds: ${e.message}"))
            }
        }
    }
    
    /**
     * Request threshold adjustment
     */
    fun requestThresholdAdjustment(
        vitalType: String,
        currentLower: Double?,
        currentUpper: Double?,
        requestedLower: Double?,
        requestedUpper: Double?,
        justification: String
    ) {
        Timber.d("Requesting threshold adjustment for: $vitalType")
        
        val clientId = _clientId.value ?: return
        
        viewModelScope.launch {
            try {
                val result = platformIntegrationManager.getApiService().requestThresholdAdjustment(
                    request = ThresholdAdjustmentRequestDto(
                        clientId = clientId,
                        vitalType = vitalType,
                        currentLower = currentLower,
                        currentUpper = currentUpper,
                        requestedLower = requestedLower,
                        requestedUpper = requestedUpper,
                        justification = justification,
                        requestedBy = userPreferences.getUserId() ?: "unknown"
                    )
                )
                
                if (result is Result.Success) {
                    // Emit event
                    _dashboardEvents.emit(DashboardEvent.ThresholdAdjustmentRequested(vitalType))
                } else if (result is Result.Error) {
                    Timber.e(result.exception, "Failed to request threshold adjustment")
                    _dashboardEvents.emit(DashboardEvent.Error("Failed to request threshold adjustment: ${result.exception.message}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to request threshold adjustment")
                _dashboardEvents.emit(DashboardEvent.Error("Failed to request threshold adjustment: ${e.message}"))
            }
        }
    }
    
    /**
     * Get vital status
     */
    fun getVitalStatus(vitalType: String): VitalStatus {
        val latestReading = _latestVitalReadings.value[vitalType]
        val threshold = _vitalThresholds.value[vitalType]
        
        if (latestReading == null) {
            return VitalStatus.UNKNOWN
        }
        
        if (threshold == null) {
            return VitalStatus.NORMAL
        }
        
        // Check if reading is numeric
        val numericValue = when (val value = latestReading.value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        } ?: return VitalStatus.UNKNOWN
        
        // Check against thresholds
        return when {
            threshold.criticalLow != null && numericValue < threshold.criticalLow -> VitalStatus.CRITICAL_LOW
            threshold.criticalHigh != null && numericValue > threshold.criticalHigh -> VitalStatus.CRITICAL_HIGH
            threshold.warningLow != null && numericValue < threshold.warningLow -> VitalStatus.WARNING_LOW
            threshold.warningHigh != null && numericValue > threshold.warningHigh -> VitalStatus.WARNING_HIGH
            threshold.lower != null && threshold.upper != null && 
                numericValue >= threshold.lower && numericValue <= threshold.upper -> VitalStatus.NORMAL
            else -> VitalStatus.UNKNOWN
        }
    }
    
    /**
     * Get formatted vital value
     */
    fun getFormattedVitalValue(vitalType: String): String {
        val latestReading = _latestVitalReadings.value[vitalType] ?: return "N/A"
        
        val value = latestReading.value
        val unit = latestReading.unit
        
        return when (value) {
            is Number -> {
                // Format number based on vital type
                when (vitalType) {
                    "blood_pressure_systolic", "blood_pressure_diastolic", "heart_rate", "steps" -> 
                        "${value.toInt()} $unit"
                    "temperature" -> String.format("%.1f %s", value.toDouble(), unit)
                    "blood_oxygen" -> "${value.toInt()}%"
                    else -> "$value $unit"
                }
            }
            is String -> {
                if (value.toDoubleOrNull() != null) {
                    // Format numeric string
                    when (vitalType) {
                        "blood_pressure_systolic", "blood_pressure_diastolic", "heart_rate", "steps" -> 
                            "${value.toInt()} $unit"
                        "temperature" -> String.format("%.1f %s", value.toDouble(), unit)
                        "blood_oxygen" -> "${value.toInt()}%"
                        else -> "$value $unit"
                    }
                } else {
                    // Non-numeric string
                    value
                }
            }
            else -> "N/A"
        }
    }
    
    /**
     * Get formatted time
     */
    fun getFormattedTime(timestamp: String?): String {
        if (timestamp == null) return "N/A"
        
        return try {
            val dateTime = LocalDateTime.parse(timestamp)
            val now = LocalDateTime.now()
            
            when {
                dateTime.toLocalDate() == now.toLocalDate() -> 
                    "Today at ${dateTime.format(timeFormatter)}"
                dateTime.toLocalDate() == now.toLocalDate().minusDays(1) -> 
                    "Yesterday at ${dateTime.format(timeFormatter)}"
                dateTime.isAfter(now.minusDays(7)) -> 
                    "${dateTime.dayOfWeek.toString().lowercase().capitalize()} at ${dateTime.format(timeFormatter)}"
                else -> 
                    dateTime.format(dateFormatter)
            }
        } catch (e: Exception) {
            "N/A"
        }
    }
    
    /**
     * Calculate vital trends
     */
    private fun calculateVitalTrends() {
        Timber.d("Calculating vital trends")
        
        val trends = mutableMapOf<String, VitalTrend>()
        
        for ((type, readings) in _vitalReadings.value) {
            val dayReadings = readings.day ?: emptyList()
            if (dayReadings.isEmpty()) continue
            
            // Extract numeric values
            val numericValues = dayReadings.mapNotNull { reading ->
                when (val value = reading.value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull()
                    else -> null
                }
            }
            
            if (numericValues.isEmpty()) continue
            
            // Calculate trend
            val firstValue = numericValues.firstOrNull() ?: continue
            val lastValue = numericValues.lastOrNull() ?: continue
            val change = lastValue - firstValue
            val percentChange = if (firstValue != 0.0) (change / firstValue) * 100 else 0.0
            
            val trendDirection = when {
                percentChange > 5.0 -> TrendDirection.UP
                percentChange < -5.0 -> TrendDirection.DOWN
                else -> TrendDirection.STABLE
            }
            
            val average = numericValues.average()
            val min = numericValues.minOrNull() ?: 0.0
            val max = numericValues.maxOrNull() ?: 0.0
            
            trends[type] = VitalTrend(
                vitalType = type,
                trendDirection = trendDirection,
                percentChange = percentChange,
                average = average,
                min = min,
                max = max,
                count = numericValues.size,
                unit = readings.unit
            )
        }
        
        _vitalTrends.value = trends
    }
    
    /**
     * Update vital trend
     */
    private fun updateVitalTrend(vitalType: String, summary: VitalSummary) {
        val currentTrends = _vitalTrends.value.toMutableMap()
        
        val trendDirection = when (summary.trend) {
            "up" -> TrendDirection.UP
            "down" -> TrendDirection.DOWN
            else -> TrendDirection.STABLE
        }
        
        currentTrends[vitalType] = VitalTrend(
            vitalType = vitalType,
            trendDirection = trendDirection,
            percentChange = 0.0, // Not provided in summary
            average = summary.average ?: 0.0,
            min = summary.min ?: 0.0,
            max = summary.max ?: 0.0,
            count = summary.count,
            unit = _vitalReadings.value[vitalType]?.unit ?: ""
        )
        
        _vitalTrends.value = currentTrends
    }
    
    /**
     * Update device status from client vitals response
     */
    private fun updateDeviceStatus(clientVitals: ClientVitalsResponse) {
        // Extract device status from metadata if available
        val deviceStatusMap = mutableMapOf<String, DeviceConnectionStatus>()
        
        // This is a placeholder - in a real implementation, we would extract device status
        // from the clientVitals response if it contains device status information
        
        // For now, just update with last seen timestamps from readings
        for ((type, readings) in clientVitals.latestReadingsByType) {
            if (readings != null && readings.deviceId != null) {
                val deviceId = readings.deviceId
                val lastSeen = try {
                    LocalDateTime.parse(readings.timestamp)
                } catch (e: Exception) {
                    LocalDateTime.now()
                }
                
                // Check if device is already in the map
                if (deviceStatusMap.containsKey(deviceId)) {
                    // Update last seen if newer
                    val currentStatus = deviceStatusMap[deviceId]!!
                    if (lastSeen.isAfter(currentStatus.lastSeen)) {
                        deviceStatusMap[deviceId] = currentStatus.copy(lastSeen = lastSeen)
                    }
                } else {
                    // Add new device status
                    deviceStatusMap[deviceId] = DeviceConnectionStatus(
                        deviceId = deviceId,
                        status = "active",
                        batteryLevel = null,
                        lastSeen = lastSeen,
                        isConnected = isDeviceConnected(lastSeen)
                    )
                }
            }
        }
        
        _deviceStatus.value = deviceStatusMap
    }
    
    /**
     * Check if device is connected based on last seen timestamp
     */
    private fun isDeviceConnected(lastSeen: LocalDateTime): Boolean {
        // Consider device connected if last seen within the last 15 minutes
        return lastSeen.isAfter(LocalDateTime.now().minusMinutes(15))
    }
    
    /**
     * Handle platform event
     */
    private fun handlePlatformEvent(event: PlatformEvent) {
        Timber.d("Handling platform event: $event")
        
        when (event) {
            is PlatformEvent.VitalReadingSubmitted -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.NewVitalReading(event.vitalType))
                }
            }
            is PlatformEvent.VitalReadingBatchSubmitted -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.VitalReadingBatchSubmitted(event.count))
                }
            }
            is PlatformEvent.SyncCompleted -> {
                loadDashboardData()
            }
            is PlatformEvent.DeviceRegistered -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.DeviceRegistered(event.deviceId))
                }
            }
            is PlatformEvent.ThresholdUpdated -> {
                // Reload thresholds
                viewModelScope.launch {
                    val clientId = _clientId.value ?: return@launch
                    val result = platformIntegrationManager.getApiService().getClientThresholds(clientId)
                    if (result is Result.Success) {
                        _vitalThresholds.value = result.data.thresholds
                    }
                }
            }
            is PlatformEvent.AlertCreated -> {
                // Reload alerts
                viewModelScope.launch {
                    val clientId = _clientId.value ?: return@launch
                    val result = platformIntegrationManager.getApiService().getClientAlerts(
                        clientId = clientId,
                        status = "active",
                        startDate = LocalDateTime.now().minusDays(7).toString()
                    )
                    if (result is Result.Success) {
                        _alerts.value = result.data.alerts
                    }
                }
            }
            is PlatformEvent.DeviceStatusChanged -> {
                // Reload device status
                loadDashboardData()
            }
            is PlatformEvent.SyncRequested -> {
                startSync()
            }
            is PlatformEvent.DeviceOnline -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.DeviceOnline)
                }
            }
            is PlatformEvent.DeviceOffline -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.DeviceOffline)
                }
            }
            else -> {
                // Ignore other events
            }
        }
    }
    
    /**
     * Handle WebSocket event
     */
    private fun handleWebSocketEvent(event: WebSocketEvent) {
        Timber.d("Handling WebSocket event: $event")
        
        when (event) {
            is WebSocketEvent.Connected -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.WebSocketConnected)
                }
            }
            is WebSocketEvent.Disconnected -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.WebSocketDisconnected(event.reason))
                }
            }
            is WebSocketEvent.MessageReceived -> {
                handleWebSocketMessage(event.message)
            }
            is WebSocketEvent.Error -> {
                viewModelScope.launch {
                    _dashboardEvents.emit(DashboardEvent.WebSocketError(event.error.message ?: "WebSocket error"))
                }
            }
        }
    }
    
    /**
     * Handle WebSocket message
     */
    private fun handleWebSocketMessage(message: WebSocketMessage) {
        Timber.d("Handling WebSocket message: ${message.type}")
        
        when (message.type) {
            "vital_reading" -> {
                // Parse vital reading from message payload
                try {
                    // In a real implementation, we would parse the message payload
                    // and update the vital readings
                    loadDashboardData()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse vital reading from WebSocket message")
                }
            }
            "alert" -> {
                // Parse alert from message payload
                try {
                    // In a real implementation, we would parse the message payload
                    // and update the alerts
                    viewModelScope.launch {
                        val clientId = _clientId.value ?: return@launch
                        val result = platformIntegrationManager.getApiService().getClientAlerts(
                            clientId = clientId,
                            status = "active",
                            startDate = LocalDateTime.now().minusDays(7).toString()
                        )
                        if (result is Result.Success) {
                            _alerts.value = result.data.alerts
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse alert from WebSocket message")
                }
            }
            "device_status" -> {
                // Parse device status from message payload
                try {
                    // In a real implementation, we would parse the message payload
                    // and update the device status
                    loadDashboardData()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse device status from WebSocket message")
                }
            }
            "threshold_update" -> {
                // Parse threshold update from message payload
                try {
                    // In a real implementation, we would parse the message payload
                    // and update the thresholds
                    viewModelScope.launch {
                        val clientId = _clientId.value ?: return@launch
                        val result = platformIntegrationManager.getApiService().getClientThresholds(clientId)
                        if (result is Result.Success) {
                            _vitalThresholds.value = result.data.thresholds
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse threshold update from WebSocket message")
                }
            }
            else -> {
                Timber.d("Unknown WebSocket message type: ${message.type}")
            }
        }
    }
    
    /**
     * Handle sync status
     */
    private fun handleSyncStatus(status: SyncStatus) {
        Timber.d("Handling sync status: $status")
        
        _syncStatus.value = when (status) {
            is SyncStatus.Idle -> SyncStatusUiState.Idle
            is SyncStatus.Syncing -> SyncStatusUiState.Syncing(status.progress, status.total)
            is SyncStatus.Completed -> SyncStatusUiState.Completed(status.result.durationMillis)
            is SyncStatus.Error -> SyncStatusUiState.Error(status.message)
        }
    }
    
    /**
     * Start auto refresh
     */
    private fun startAutoRefresh() {
        Timber.d("Starting auto refresh")
        
        // Cancel existing job if any
        autoRefreshJob?.cancel()
        
        // Start new job
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL)
                loadDashboardData()
            }
        }
    }
    
    /**
     * Stop auto refresh
     */
    private fun stopAutoRefresh() {
        Timber.d("Stopping auto refresh")
        
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }
    
    /**
     * Reset error state
     */
    fun resetErrorState() {
        if (_dashboardState.value is DashboardState.Error) {
            _dashboardState.value = DashboardState.Loaded
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        
        // Cancel auto refresh
        stopAutoRefresh()
        
        // Disconnect WebSocket
        disconnectWebSocket()
    }
    
    companion object {
        private const val AUTO_REFRESH_INTERVAL = 5 * 60 * 1000L // 5 minutes
    }
}

/**
 * Dashboard state
 */
sealed class DashboardState {
    object Loading : DashboardState()
    object Loaded : DashboardState()
    data class Error(val message: String) : DashboardState()
}

/**
 * Sync status UI state
 */
sealed class SyncStatusUiState {
    object Idle : SyncStatusUiState()
    data class Syncing(val progress: Int, val total: Int) : SyncStatusUiState()
    data class Completed(val durationMillis: Long) : SyncStatusUiState()
    data class Error(val message: String) : SyncStatusUiState()
}

/**
 * Time range option
 */
enum class TimeRangeOption {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

/**
 * Vital status
 */
enum class VitalStatus {
    NORMAL,
    WARNING_LOW,
    WARNING_HIGH,
    CRITICAL_LOW,
    CRITICAL_HIGH,
    UNKNOWN
}

/**
 * Trend direction
 */
enum class TrendDirection {
    UP,
    DOWN,
    STABLE
}

/**
 * Vital trend
 */
data class VitalTrend(
    val vitalType: String,
    val trendDirection: TrendDirection,
    val percentChange: Double,
    val average: Double,
    val min: Double,
    val max: Double,
    val count: Int,
    val unit: String
)

/**
 * Device connection status
 */
data class DeviceConnectionStatus(
    val deviceId: String,
    val status: String,
    val batteryLevel: Int?,
    val lastSeen: LocalDateTime,
    val isConnected: Boolean
)

/**
 * Dashboard events
 */
sealed class DashboardEvent {
    data class NewVitalReading(val vitalType: String) : DashboardEvent()
    data class VitalReadingBatchSubmitted(val count: Int) : DashboardEvent()
    data class DeviceRegistered(val deviceId: String) : DashboardEvent()
    data class DeviceStatusUpdated(val deviceId: String) : DashboardEvent()
    data class AlertAcknowledged(val alertId: String) : DashboardEvent()
    data class AlertResolved(val alertId: String) : DashboardEvent()
    data class ThresholdAdjustmentRequested(val vitalType: String) : DashboardEvent()
    data class VitalDetailLoaded(val vitalType: String) : DashboardEvent()
    object VitalConfigurationUpdated : DashboardEvent()
    object ThresholdsUpdated : DashboardEvent()
    object WebSocketConnected : DashboardEvent()
    data class WebSocketDisconnected(val reason: String) : DashboardEvent()
    data class WebSocketError(val message: String) : DashboardEvent()
    object DeviceOnline : DashboardEvent()
    object DeviceOffline : DashboardEvent()
    data class Error(val message: String) : DashboardEvent()
}
