package com.sensacare.app.presentation.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.AlertRepository
import com.sensacare.app.domain.repository.UserPreferencesRepository
import com.sensacare.app.domain.repository.EmergencyContactRepository
import com.sensacare.app.domain.usecase.alerts.*
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
 * AlertsViewModel - Comprehensive alerts and notifications management system
 *
 * This ViewModel implements MVVM architecture with reactive UI state management for
 * the alerts and notifications feature of the SensaCare app:
 * 
 * Key features:
 * - Health alerts monitoring and management
 * - Notification preference management
 * - Emergency alert detection and handling
 * - Critical health metrics monitoring
 * - Alert prioritization and categorization
 * - Custom alert rules configuration
 * - Alert history and tracking
 * - Smart alert filtering and snoozing
 * - Emergency contact integration
 * - Real-time alert processing
 * - Cross-device alert synchronization
 * - Alert acknowledgment and action tracking
 * - Health trend alerts
 * - Schedule-based alert management
 */
@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val monitorHealthAlertsUseCase: MonitorHealthAlertsUseCase,
    private val manageAlertPreferencesUseCase: ManageAlertPreferencesUseCase,
    private val processEmergencyAlertsUseCase: ProcessEmergencyAlertsUseCase,
    private val manageAlertRulesUseCase: ManageAlertRulesUseCase,
    private val manageEmergencyContactsUseCase: ManageEmergencyContactsUseCase,
    private val alertRepository: AlertRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val emergencyContactRepository: EmergencyContactRepository
) : ViewModel() {

    // User ID - In a real app, this would come from authentication
    private val userId = 1L
    
    // UI State
    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()
    
    // All alerts
    private val _allAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val allAlerts: StateFlow<List<HealthAlert>> = _allAlerts.asStateFlow()
    
    // Active alerts
    private val _activeAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val activeAlerts: StateFlow<List<HealthAlert>> = _activeAlerts.asStateFlow()
    
    // Emergency alerts
    private val _emergencyAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val emergencyAlerts: StateFlow<List<HealthAlert>> = _emergencyAlerts.asStateFlow()
    
    // Acknowledged alerts
    private val _acknowledgedAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val acknowledgedAlerts: StateFlow<List<HealthAlert>> = _acknowledgedAlerts.asStateFlow()
    
    // Snoozed alerts
    private val _snoozedAlerts = MutableStateFlow<List<HealthAlert>>(emptyList())
    val snoozedAlerts: StateFlow<List<HealthAlert>> = _snoozedAlerts.asStateFlow()
    
    // Alert history
    private val _alertHistory = MutableStateFlow<List<HealthAlert>>(emptyList())
    val alertHistory: StateFlow<List<HealthAlert>> = _alertHistory.asStateFlow()
    
    // Selected alert for detailed view
    private val _selectedAlert = MutableStateFlow<HealthAlert?>(null)
    val selectedAlert: StateFlow<HealthAlert?> = _selectedAlert.asStateFlow()
    
    // Alert rules
    private val _alertRules = MutableStateFlow<List<AlertRule>>(emptyList())
    val alertRules: StateFlow<List<AlertRule>> = _alertRules.asStateFlow()
    
    // Selected alert rule for editing
    private val _selectedAlertRule = MutableStateFlow<AlertRule?>(null)
    val selectedAlertRule: StateFlow<AlertRule?> = _selectedAlertRule.asStateFlow()
    
    // Alert rule form state
    private val _alertRuleFormState = MutableStateFlow<AlertRuleFormState>(AlertRuleFormState.Empty)
    val alertRuleFormState: StateFlow<AlertRuleFormState> = _alertRuleFormState.asStateFlow()
    
    // Alert preferences
    private val _alertPreferences = MutableStateFlow<AlertPreferences?>(null)
    val alertPreferences: StateFlow<AlertPreferences?> = _alertPreferences.asStateFlow()
    
    // Emergency contacts
    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()
    
    // Selected emergency contact for editing
    private val _selectedEmergencyContact = MutableStateFlow<EmergencyContact?>(null)
    val selectedEmergencyContact: StateFlow<EmergencyContact?> = _selectedEmergencyContact.asStateFlow()
    
    // Emergency contact form state
    private val _emergencyContactFormState = MutableStateFlow<EmergencyContactFormState>(EmergencyContactFormState.Empty)
    val emergencyContactFormState: StateFlow<EmergencyContactFormState> = _emergencyContactFormState.asStateFlow()
    
    // Alert statistics
    private val _alertStatistics = MutableStateFlow<AlertStatistics?>(null)
    val alertStatistics: StateFlow<AlertStatistics?> = _alertStatistics.asStateFlow()
    
    // Alert filters
    private val _alertFilters = MutableStateFlow(AlertFilters())
    val alertFilters: StateFlow<AlertFilters> = _alertFilters.asStateFlow()
    
    // Alert sort option
    private val _alertSortOption = MutableStateFlow(AlertSortOption.PRIORITY)
    val alertSortOption: StateFlow<AlertSortOption> = _alertSortOption.asStateFlow()
    
    // Error events
    private val _errorEvents = MutableSharedFlow<AlertErrorEvent>()
    val errorEvents: SharedFlow<AlertErrorEvent> = _errorEvents.asSharedFlow()
    
    // Navigation events
    val navigationEvent = SingleLiveEvent<AlertNavigationEvent>()
    
    // New alert events
    private val _newAlertEvents = MutableSharedFlow<HealthAlert>()
    val newAlertEvents: SharedFlow<HealthAlert> = _newAlertEvents.asSharedFlow()
    
    // Emergency alert events
    private val _emergencyAlertEvents = MutableSharedFlow<HealthAlert>()
    val emergencyAlertEvents: SharedFlow<HealthAlert> = _emergencyAlertEvents.asSharedFlow()
    
    // Jobs
    private var alertMonitoringJob: Job? = null
    private var alertRefreshJob: Job? = null
    private var emergencyMonitoringJob: Job? = null
    private var alertSyncJob: Job? = null
    private var alertStatisticsJob: Job? = null
    
    // Initialize
    init {
        Timber.d("AlertsViewModel initialized")
        loadAlertPreferences()
        loadAllAlerts()
        loadAlertRules()
        loadEmergencyContacts()
        loadAlertStatistics()
        startAlertMonitoring()
        startEmergencyMonitoring()
        startAlertSync()
    }
    
    /**
     * Load all alerts
     */
    fun loadAllAlerts() {
        // Cancel any existing refresh job
        alertRefreshJob?.cancel()
        
        alertRefreshJob = viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                // Collect all alerts
                alertRepository.getAllAlerts(userId).collect { alerts ->
                    _allAlerts.value = alerts
                    
                    // Categorize alerts
                    categorizeAlerts(alerts)
                    
                    // Apply filters
                    applyAlertFilters()
                    
                    // Update UI state
                    _uiState.value = if (alerts.isEmpty()) {
                        AlertsUiState.Empty
                    } else {
                        AlertsUiState.Success(
                            activeAlertsCount = _activeAlerts.value.size,
                            emergencyAlertsCount = _emergencyAlerts.value.size,
                            acknowledgedAlertsCount = _acknowledgedAlerts.value.size,
                            snoozedAlertsCount = _snoozedAlerts.value.size
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading alerts")
                _uiState.value = AlertsUiState.Error("Failed to load alerts: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.LoadingError("Failed to load alerts", e))
            }
        }
    }
    
    /**
     * Categorize alerts into different lists
     */
    private fun categorizeAlerts(alerts: List<HealthAlert>) {
        // Active alerts (not acknowledged, not snoozed, not resolved)
        _activeAlerts.value = alerts.filter { 
            !it.isAcknowledged && !it.isSnoozed && !it.isResolved
        }
        
        // Emergency alerts
        _emergencyAlerts.value = alerts.filter { 
            it.severity == AlertSeverity.EMERGENCY && !it.isResolved
        }
        
        // Acknowledged alerts
        _acknowledgedAlerts.value = alerts.filter { 
            it.isAcknowledged && !it.isResolved
        }
        
        // Snoozed alerts
        _snoozedAlerts.value = alerts.filter { 
            it.isSnoozed && !it.isResolved
        }
        
        // Alert history (all alerts, including resolved)
        _alertHistory.value = alerts.sortedByDescending { it.createdAt }
    }
    
    /**
     * Load alert preferences
     */
    private fun loadAlertPreferences() {
        viewModelScope.launch {
            try {
                manageAlertPreferencesUseCase.getAlertPreferences(userId).collect { preferences ->
                    _alertPreferences.value = preferences
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading alert preferences")
                _errorEvents.emit(AlertErrorEvent.PreferencesError("Failed to load alert preferences", e))
            }
        }
    }
    
    /**
     * Update alert preferences
     */
    fun updateAlertPreferences(preferences: AlertPreferences) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageAlertPreferencesUseCase.updateAlertPreferences(preferences).collect { result ->
                    when (result) {
                        is AlertPreferencesResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertPreferencesResult.Success -> {
                            // Preferences updated successfully
                            _alertPreferences.value = result.preferences
                            _errorEvents.emit(AlertErrorEvent.Success("Alert preferences updated successfully"))
                            _uiState.value = AlertsUiState.Success(
                                activeAlertsCount = _activeAlerts.value.size,
                                emergencyAlertsCount = _emergencyAlerts.value.size,
                                acknowledgedAlertsCount = _acknowledgedAlerts.value.size,
                                snoozedAlertsCount = _snoozedAlerts.value.size
                            )
                            
                            // Restart alert monitoring with new preferences
                            restartAlertMonitoring()
                        }
                        is AlertPreferencesResult.Error -> {
                            // Preferences update failed
                            _uiState.value = AlertsUiState.Error("Failed to update alert preferences: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.PreferencesError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating alert preferences")
                _uiState.value = AlertsUiState.Error("Failed to update alert preferences: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.PreferencesError("Failed to update alert preferences", e))
            }
        }
    }
    
    /**
     * Toggle a specific alert preference
     */
    fun toggleAlertPreference(preference: AlertPreferenceType, enabled: Boolean) {
        val currentPreferences = _alertPreferences.value ?: return
        
        // Create updated preferences
        val updatedPreferences = when (preference) {
            AlertPreferenceType.CRITICAL_ALERTS -> currentPreferences.copy(criticalAlertsEnabled = enabled)
            AlertPreferenceType.HEALTH_TRENDS -> currentPreferences.copy(healthTrendAlertsEnabled = enabled)
            AlertPreferenceType.DEVICE_CONNECTIVITY -> currentPreferences.copy(deviceConnectivityAlertsEnabled = enabled)
            AlertPreferenceType.MEDICATION_REMINDERS -> currentPreferences.copy(medicationRemindersEnabled = enabled)
            AlertPreferenceType.ACTIVITY_GOALS -> currentPreferences.copy(activityGoalAlertsEnabled = enabled)
            AlertPreferenceType.SLEEP_INSIGHTS -> currentPreferences.copy(sleepInsightAlertsEnabled = enabled)
            AlertPreferenceType.HEART_RATE -> currentPreferences.copy(heartRateAlertsEnabled = enabled)
            AlertPreferenceType.BLOOD_PRESSURE -> currentPreferences.copy(bloodPressureAlertsEnabled = enabled)
            AlertPreferenceType.BLOOD_GLUCOSE -> currentPreferences.copy(bloodGlucoseAlertsEnabled = enabled)
            AlertPreferenceType.EMERGENCY_CONTACTS -> currentPreferences.copy(emergencyContactAlertsEnabled = enabled)
        }
        
        // Update preferences
        updateAlertPreferences(updatedPreferences)
    }
    
    /**
     * Update notification channels
     */
    fun updateNotificationChannels(channels: Set<NotificationChannel>) {
        val currentPreferences = _alertPreferences.value ?: return
        
        // Create updated preferences
        val updatedPreferences = currentPreferences.copy(notificationChannels = channels)
        
        // Update preferences
        updateAlertPreferences(updatedPreferences)
    }
    
    /**
     * Update quiet hours
     */
    fun updateQuietHours(startTime: String, endTime: String, enabled: Boolean) {
        val currentPreferences = _alertPreferences.value ?: return
        
        // Create updated preferences
        val updatedPreferences = currentPreferences.copy(
            quietHoursEnabled = enabled,
            quietHoursStart = startTime,
            quietHoursEnd = endTime
        )
        
        // Update preferences
        updateAlertPreferences(updatedPreferences)
    }
    
    /**
     * Update emergency bypass settings
     */
    fun updateEmergencyBypass(bypassQuietHours: Boolean) {
        val currentPreferences = _alertPreferences.value ?: return
        
        // Create updated preferences
        val updatedPreferences = currentPreferences.copy(emergencyBypassQuietHours = bypassQuietHours)
        
        // Update preferences
        updateAlertPreferences(updatedPreferences)
    }
    
    /**
     * Load alert rules
     */
    private fun loadAlertRules() {
        viewModelScope.launch {
            try {
                manageAlertRulesUseCase.getAlertRules(userId).collect { rules ->
                    _alertRules.value = rules
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading alert rules")
                _alertRules.value = emptyList()
                _errorEvents.emit(AlertErrorEvent.RulesError("Failed to load alert rules", e))
            }
        }
    }
    
    /**
     * Create a new alert rule
     */
    fun createAlertRule(rule: AlertRule) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageAlertRulesUseCase.createAlertRule(userId, rule).collect { result ->
                    when (result) {
                        is AlertRuleOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertRuleOperationResult.Success -> {
                            // Rule created successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Alert rule created successfully"))
                            
                            // Clear form state
                            _alertRuleFormState.value = AlertRuleFormState.Empty
                            
                            // Refresh rules
                            loadAlertRules()
                            
                            // Restart alert monitoring with new rules
                            restartAlertMonitoring()
                            
                            // Navigate back to rules list
                            navigationEvent.value = AlertNavigationEvent.BackToRulesList
                        }
                        is AlertRuleOperationResult.Error -> {
                            // Rule creation failed
                            _uiState.value = AlertsUiState.Error("Failed to create alert rule: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.RulesError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating alert rule")
                _uiState.value = AlertsUiState.Error("Failed to create alert rule: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.RulesError("Failed to create alert rule", e))
            }
        }
    }
    
    /**
     * Update an existing alert rule
     */
    fun updateAlertRule(rule: AlertRule) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageAlertRulesUseCase.updateAlertRule(rule).collect { result ->
                    when (result) {
                        is AlertRuleOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertRuleOperationResult.Success -> {
                            // Rule updated successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Alert rule updated successfully"))
                            
                            // Clear form state
                            _alertRuleFormState.value = AlertRuleFormState.Empty
                            
                            // Refresh rules
                            loadAlertRules()
                            
                            // Restart alert monitoring with updated rules
                            restartAlertMonitoring()
                            
                            // Update selected rule if it's the one that was updated
                            if (_selectedAlertRule.value?.id == rule.id) {
                                _selectedAlertRule.value = result.rule
                            }
                            
                            // Navigate back to rules list
                            navigationEvent.value = AlertNavigationEvent.BackToRulesList
                        }
                        is AlertRuleOperationResult.Error -> {
                            // Rule update failed
                            _uiState.value = AlertsUiState.Error("Failed to update alert rule: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.RulesError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating alert rule")
                _uiState.value = AlertsUiState.Error("Failed to update alert rule: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.RulesError("Failed to update alert rule", e))
            }
        }
    }
    
    /**
     * Delete an alert rule
     */
    fun deleteAlertRule(ruleId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageAlertRulesUseCase.deleteAlertRule(ruleId).collect { result ->
                    when (result) {
                        is AlertRuleOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertRuleOperationResult.Success -> {
                            // Rule deleted successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Alert rule deleted successfully"))
                            
                            // Clear selected rule if it's the one that was deleted
                            if (_selectedAlertRule.value?.id == ruleId) {
                                _selectedAlertRule.value = null
                            }
                            
                            // Refresh rules
                            loadAlertRules()
                            
                            // Restart alert monitoring with updated rules
                            restartAlertMonitoring()
                            
                            // Navigate back to rules list
                            navigationEvent.value = AlertNavigationEvent.BackToRulesList
                        }
                        is AlertRuleOperationResult.Error -> {
                            // Rule deletion failed
                            _uiState.value = AlertsUiState.Error("Failed to delete alert rule: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.RulesError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting alert rule")
                _uiState.value = AlertsUiState.Error("Failed to delete alert rule: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.RulesError("Failed to delete alert rule", e))
            }
        }
    }
    
    /**
     * Toggle an alert rule (enable/disable)
     */
    fun toggleAlertRule(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Get the rule
                val rule = _alertRules.value.find { it.id == ruleId } ?: return@launch
                
                // Update the rule
                val updatedRule = rule.copy(isEnabled = enabled)
                updateAlertRule(updatedRule)
            } catch (e: Exception) {
                Timber.e(e, "Error toggling alert rule")
                _errorEvents.emit(AlertErrorEvent.RulesError("Failed to toggle alert rule", e))
            }
        }
    }
    
    /**
     * Select an alert rule for detailed view
     */
    fun selectAlertRule(ruleId: Long) {
        viewModelScope.launch {
            try {
                // Get rule details
                manageAlertRulesUseCase.getAlertRule(ruleId).collect { rule ->
                    if (rule != null) {
                        _selectedAlertRule.value = rule
                        navigationEvent.value = AlertNavigationEvent.ToRuleDetail(rule)
                    } else {
                        _errorEvents.emit(AlertErrorEvent.RuleNotFoundError("Alert rule not found"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error selecting alert rule")
                _errorEvents.emit(AlertErrorEvent.RuleNotFoundError("Failed to load alert rule details: ${e.message}"))
            }
        }
    }
    
    /**
     * Start new alert rule creation
     */
    fun startNewRuleCreation() {
        // Clear form state
        _alertRuleFormState.value = AlertRuleFormState.Empty
        
        // Navigate to rule form
        navigationEvent.value = AlertNavigationEvent.ToRuleForm(null)
    }
    
    /**
     * Start alert rule editing
     */
    fun startRuleEditing(ruleId: Long) {
        viewModelScope.launch {
            try {
                // Get rule details
                manageAlertRulesUseCase.getAlertRule(ruleId).collect { rule ->
                    if (rule != null) {
                        // Set form state
                        _alertRuleFormState.value = AlertRuleFormState.Editing(rule)
                        
                        // Navigate to rule form
                        navigationEvent.value = AlertNavigationEvent.ToRuleForm(rule)
                    } else {
                        _errorEvents.emit(AlertErrorEvent.RuleNotFoundError("Alert rule not found"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting rule editing")
                _errorEvents.emit(AlertErrorEvent.RuleNotFoundError("Failed to load alert rule details: ${e.message}"))
            }
        }
    }
    
    /**
     * Update alert rule form field
     */
    fun updateRuleFormField(field: AlertRuleFormField, value: Any) {
        val currentForm = _alertRuleFormState.value
        
        // Create updated form state
        val updatedForm = when (currentForm) {
            is AlertRuleFormState.Empty -> {
                // Create new form with the field set
                val newRule = createEmptyAlertRule().updateField(field, value)
                AlertRuleFormState.Creating(newRule)
            }
            is AlertRuleFormState.Creating -> {
                // Update existing form
                val updatedRule = currentForm.rule.updateField(field, value)
                AlertRuleFormState.Creating(updatedRule)
            }
            is AlertRuleFormState.Editing -> {
                // Update existing form
                val updatedRule = currentForm.rule.updateField(field, value)
                AlertRuleFormState.Editing(updatedRule)
            }
        }
        
        // Update form state
        _alertRuleFormState.value = updatedForm
    }
    
    /**
     * Validate alert rule form
     */
    fun validateRuleForm(): Boolean {
        val currentForm = _alertRuleFormState.value
        
        // Get rule from form
        val rule = when (currentForm) {
            is AlertRuleFormState.Empty -> return false
            is AlertRuleFormState.Creating -> currentForm.rule
            is AlertRuleFormState.Editing -> currentForm.rule
        }
        
        // Basic validation
        if (rule.name.isBlank()) {
            _errorEvents.emit(AlertErrorEvent.ValidationError("Name is required"))
            return false
        }
        
        if (rule.metricType == MetricType.NONE) {
            _errorEvents.emit(AlertErrorEvent.ValidationError("Metric type is required"))
            return false
        }
        
        // Validate condition
        if (rule.condition == AlertCondition.NONE) {
            _errorEvents.emit(AlertErrorEvent.ValidationError("Condition is required"))
            return false
        }
        
        // Validate threshold values
        when (rule.condition) {
            AlertCondition.ABOVE, AlertCondition.BELOW -> {
                if (rule.thresholdValue == null) {
                    _errorEvents.emit(AlertErrorEvent.ValidationError("Threshold value is required"))
                    return false
                }
            }
            AlertCondition.BETWEEN -> {
                if (rule.lowerThreshold == null || rule.upperThreshold == null) {
                    _errorEvents.emit(AlertErrorEvent.ValidationError("Both lower and upper thresholds are required"))
                    return false
                }
                
                if (rule.lowerThreshold >= rule.upperThreshold) {
                    _errorEvents.emit(AlertErrorEvent.ValidationError("Upper threshold must be greater than lower threshold"))
                    return false
                }
            }
            AlertCondition.OUTSIDE -> {
                if (rule.lowerThreshold == null || rule.upperThreshold == null) {
                    _errorEvents.emit(AlertErrorEvent.ValidationError("Both lower and upper thresholds are required"))
                    return false
                }
                
                if (rule.lowerThreshold >= rule.upperThreshold) {
                    _errorEvents.emit(AlertErrorEvent.ValidationError("Upper threshold must be greater than lower threshold"))
                    return false
                }
            }
            AlertCondition.CHANGE_BY -> {
                if (rule.changeThreshold == null) {
                    _errorEvents.emit(AlertErrorEvent.ValidationError("Change threshold is required"))
                    return false
                }
            }
            AlertCondition.NONE -> {
                // Already validated above
            }
        }
        
        return true
    }
    
    /**
     * Submit alert rule form
     */
    fun submitRuleForm() {
        // Validate form
        if (!validateRuleForm()) {
            return
        }
        
        // Get rule from form
        val rule = when (val currentForm = _alertRuleFormState.value) {
            is AlertRuleFormState.Empty -> {
                _errorEvents.emit(AlertErrorEvent.ValidationError("No rule data available"))
                return
            }
            is AlertRuleFormState.Creating -> currentForm.rule
            is AlertRuleFormState.Editing -> currentForm.rule
        }
        
        // Create or update rule
        if (rule.id == 0L) {
            createAlertRule(rule)
        } else {
            updateAlertRule(rule)
        }
    }
    
    /**
     * Cancel alert rule form
     */
    fun cancelRuleForm() {
        // Clear form state
        _alertRuleFormState.value = AlertRuleFormState.Empty
        
        // Navigate back to rules list
        navigationEvent.value = AlertNavigationEvent.BackToRulesList
    }
    
    /**
     * Load emergency contacts
     */
    private fun loadEmergencyContacts() {
        viewModelScope.launch {
            try {
                manageEmergencyContactsUseCase.getEmergencyContacts(userId).collect { contacts ->
                    _emergencyContacts.value = contacts
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading emergency contacts")
                _emergencyContacts.value = emptyList()
                _errorEvents.emit(AlertErrorEvent.ContactsError("Failed to load emergency contacts", e))
            }
        }
    }
    
    /**
     * Create a new emergency contact
     */
    fun createEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageEmergencyContactsUseCase.createEmergencyContact(userId, contact).collect { result ->
                    when (result) {
                        is EmergencyContactOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is EmergencyContactOperationResult.Success -> {
                            // Contact created successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Emergency contact added successfully"))
                            
                            // Clear form state
                            _emergencyContactFormState.value = EmergencyContactFormState.Empty
                            
                            // Refresh contacts
                            loadEmergencyContacts()
                            
                            // Navigate back to contacts list
                            navigationEvent.value = AlertNavigationEvent.BackToContactsList
                        }
                        is EmergencyContactOperationResult.Error -> {
                            // Contact creation failed
                            _uiState.value = AlertsUiState.Error("Failed to add emergency contact: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.ContactsError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating emergency contact")
                _uiState.value = AlertsUiState.Error("Failed to add emergency contact: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.ContactsError("Failed to add emergency contact", e))
            }
        }
    }
    
    /**
     * Update an existing emergency contact
     */
    fun updateEmergencyContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageEmergencyContactsUseCase.updateEmergencyContact(contact).collect { result ->
                    when (result) {
                        is EmergencyContactOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is EmergencyContactOperationResult.Success -> {
                            // Contact updated successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Emergency contact updated successfully"))
                            
                            // Clear form state
                            _emergencyContactFormState.value = EmergencyContactFormState.Empty
                            
                            // Refresh contacts
                            loadEmergencyContacts()
                            
                            // Update selected contact if it's the one that was updated
                            if (_selectedEmergencyContact.value?.id == contact.id) {
                                _selectedEmergencyContact.value = result.contact
                            }
                            
                            // Navigate back to contacts list
                            navigationEvent.value = AlertNavigationEvent.BackToContactsList
                        }
                        is EmergencyContactOperationResult.Error -> {
                            // Contact update failed
                            _uiState.value = AlertsUiState.Error("Failed to update emergency contact: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.ContactsError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating emergency contact")
                _uiState.value = AlertsUiState.Error("Failed to update emergency contact: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.ContactsError("Failed to update emergency contact", e))
            }
        }
    }
    
    /**
     * Delete an emergency contact
     */
    fun deleteEmergencyContact(contactId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageEmergencyContactsUseCase.deleteEmergencyContact(contactId).collect { result ->
                    when (result) {
                        is EmergencyContactOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is EmergencyContactOperationResult.Success -> {
                            // Contact deleted successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Emergency contact deleted successfully"))
                            
                            // Clear selected contact if it's the one that was deleted
                            if (_selectedEmergencyContact.value?.id == contactId) {
                                _selectedEmergencyContact.value = null
                            }
                            
                            // Refresh contacts
                            loadEmergencyContacts()
                            
                            // Navigate back to contacts list
                            navigationEvent.value = AlertNavigationEvent.BackToContactsList
                        }
                        is EmergencyContactOperationResult.Error -> {
                            // Contact deletion failed
                            _uiState.value = AlertsUiState.Error("Failed to delete emergency contact: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.ContactsError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting emergency contact")
                _uiState.value = AlertsUiState.Error("Failed to delete emergency contact: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.ContactsError("Failed to delete emergency contact", e))
            }
        }
    }
    
    /**
     * Select an emergency contact for detailed view
     */
    fun selectEmergencyContact(contactId: Long) {
        viewModelScope.launch {
            try {
                // Get contact details
                manageEmergencyContactsUseCase.getEmergencyContact(contactId).collect { contact ->
                    if (contact != null) {
                        _selectedEmergencyContact.value = contact
                        navigationEvent.value = AlertNavigationEvent.ToContactDetail(contact)
                    } else {
                        _errorEvents.emit(AlertErrorEvent.ContactNotFoundError("Emergency contact not found"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error selecting emergency contact")
                _errorEvents.emit(AlertErrorEvent.ContactNotFoundError("Failed to load emergency contact details: ${e.message}"))
            }
        }
    }
    
    /**
     * Start new emergency contact creation
     */
    fun startNewContactCreation() {
        // Clear form state
        _emergencyContactFormState.value = EmergencyContactFormState.Empty
        
        // Navigate to contact form
        navigationEvent.value = AlertNavigationEvent.ToContactForm(null)
    }
    
    /**
     * Start emergency contact editing
     */
    fun startContactEditing(contactId: Long) {
        viewModelScope.launch {
            try {
                // Get contact details
                manageEmergencyContactsUseCase.getEmergencyContact(contactId).collect { contact ->
                    if (contact != null) {
                        // Set form state
                        _emergencyContactFormState.value = EmergencyContactFormState.Editing(contact)
                        
                        // Navigate to contact form
                        navigationEvent.value = AlertNavigationEvent.ToContactForm(contact)
                    } else {
                        _errorEvents.emit(AlertErrorEvent.ContactNotFoundError("Emergency contact not found"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting contact editing")
                _errorEvents.emit(AlertErrorEvent.ContactNotFoundError("Failed to load emergency contact details: ${e.message}"))
            }
        }
    }
    
    /**
     * Update emergency contact form field
     */
    fun updateContactFormField(field: EmergencyContactFormField, value: Any) {
        val currentForm = _emergencyContactFormState.value
        
        // Create updated form state
        val updatedForm = when (currentForm) {
            is EmergencyContactFormState.Empty -> {
                // Create new form with the field set
                val newContact = createEmptyEmergencyContact().updateField(field, value)
                EmergencyContactFormState.Creating(newContact)
            }
            is EmergencyContactFormState.Creating -> {
                // Update existing form
                val updatedContact = currentForm.contact.updateField(field, value)
                EmergencyContactFormState.Creating(updatedContact)
            }
            is EmergencyContactFormState.Editing -> {
                // Update existing form
                val updatedContact = currentForm.contact.updateField(field, value)
                EmergencyContactFormState.Editing(updatedContact)
            }
        }
        
        // Update form state
        _emergencyContactFormState.value = updatedForm
    }
    
    /**
     * Validate emergency contact form
     */
    fun validateContactForm(): Boolean {
        val currentForm = _emergencyContactFormState.value
        
        // Get contact from form
        val contact = when (currentForm) {
            is EmergencyContactFormState.Empty -> return false
            is EmergencyContactFormState.Creating -> currentForm.contact
            is EmergencyContactFormState.Editing -> currentForm.contact
        }
        
        // Basic validation
        if (contact.name.isBlank()) {
            _errorEvents.emit(AlertErrorEvent.ValidationError("Name is required"))
            return false
        }
        
        if (contact.phone.isBlank() && contact.email.isBlank()) {
            _errorEvents.emit(AlertErrorEvent.ValidationError("At least one contact method (phone or email) is required"))
            return false
        }
        
        // Validate phone format if provided
        if (contact.phone.isNotBlank() && !isValidPhoneNumber(contact.phone)) {
            _errorEvents.emit(AlertErrorEvent.ValidationError("Invalid phone number format"))
            return false
        }
        
        // Validate email format if provided
        if (contact.email.isNotBlank() && !isValidEmail(contact.email)) {
            _errorEvents.emit(AlertErrorEvent.ValidationError("Invalid email format"))
            return false
        }
        
        return true
    }
    
    /**
     * Submit emergency contact form
     */
    fun submitContactForm() {
        // Validate form
        if (!validateContactForm()) {
            return
        }
        
        // Get contact from form
        val contact = when (val currentForm = _emergencyContactFormState.value) {
            is EmergencyContactFormState.Empty -> {
                _errorEvents.emit(AlertErrorEvent.ValidationError("No contact data available"))
                return
            }
            is EmergencyContactFormState.Creating -> currentForm.contact
            is EmergencyContactFormState.Editing -> currentForm.contact
        }
        
        // Create or update contact
        if (contact.id == 0L) {
            createEmergencyContact(contact)
        } else {
            updateEmergencyContact(contact)
        }
    }
    
    /**
     * Cancel emergency contact form
     */
    fun cancelContactForm() {
        // Clear form state
        _emergencyContactFormState.value = EmergencyContactFormState.Empty
        
        // Navigate back to contacts list
        navigationEvent.value = AlertNavigationEvent.BackToContactsList
    }
    
    /**
     * Test emergency contact
     */
    fun testEmergencyContact(contactId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                manageEmergencyContactsUseCase.testEmergencyContact(contactId).collect { result ->
                    when (result) {
                        is EmergencyContactTestResult.Loading -> {
                            // Ignore loading state
                        }
                        is EmergencyContactTestResult.Success -> {
                            // Test successful
                            _errorEvents.emit(AlertErrorEvent.Success("Test message sent successfully"))
                            _uiState.value = AlertsUiState.Success(
                                activeAlertsCount = _activeAlerts.value.size,
                                emergencyAlertsCount = _emergencyAlerts.value.size,
                                acknowledgedAlertsCount = _acknowledgedAlerts.value.size,
                                snoozedAlertsCount = _snoozedAlerts.value.size
                            )
                        }
                        is EmergencyContactTestResult.Error -> {
                            // Test failed
                            _uiState.value = AlertsUiState.Error("Failed to send test message: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.ContactsError("Failed to send test message: ${result.error.message}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error testing emergency contact")
                _uiState.value = AlertsUiState.Error("Failed to send test message: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.ContactsError("Failed to send test message", e))
            }
        }
    }
    
    /**
     * Select an alert for detailed view
     */
    fun selectAlert(alertId: Long) {
        viewModelScope.launch {
            try {
                // Get alert details
                alertRepository.getAlert(alertId).collect { alert ->
                    if (alert != null) {
                        _selectedAlert.value = alert
                        navigationEvent.value = AlertNavigationEvent.ToAlertDetail(alert)
                    } else {
                        _errorEvents.emit(AlertErrorEvent.AlertNotFoundError("Alert not found"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error selecting alert")
                _errorEvents.emit(AlertErrorEvent.AlertNotFoundError("Failed to load alert details: ${e.message}"))
            }
        }
    }
    
    /**
     * Acknowledge an alert
     */
    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                alertRepository.acknowledgeAlert(alertId).collect { result ->
                    when (result) {
                        is AlertOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertOperationResult.Success -> {
                            // Alert acknowledged successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Alert acknowledged"))
                            
                            // Refresh alerts
                            loadAllAlerts()
                            
                            // Update selected alert if it's the one that was acknowledged
                            if (_selectedAlert.value?.id == alertId) {
                                _selectedAlert.value = result.alert
                            }
                        }
                        is AlertOperationResult.Error -> {
                            // Alert acknowledgment failed
                            _uiState.value = AlertsUiState.Error("Failed to acknowledge alert: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.AlertOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error acknowledging alert")
                _uiState.value = AlertsUiState.Error("Failed to acknowledge alert: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.AlertOperationError("Failed to acknowledge alert: ${e.message}"))
            }
        }
    }
    
    /**
     * Snooze an alert
     */
    fun snoozeAlert(alertId: Long, snoozeDuration: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                alertRepository.snoozeAlert(alertId, snoozeDuration).collect { result ->
                    when (result) {
                        is AlertOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertOperationResult.Success -> {
                            // Alert snoozed successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Alert snoozed for $snoozeDuration minutes"))
                            
                            // Refresh alerts
                            loadAllAlerts()
                            
                            // Update selected alert if it's the one that was snoozed
                            if (_selectedAlert.value?.id == alertId) {
                                _selectedAlert.value = result.alert
                            }
                        }
                        is AlertOperationResult.Error -> {
                            // Alert snooze failed
                            _uiState.value = AlertsUiState.Error("Failed to snooze alert: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.AlertOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error snoozing alert")
                _uiState.value = AlertsUiState.Error("Failed to snooze alert: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.AlertOperationError("Failed to snooze alert: ${e.message}"))
            }
        }
    }
    
    /**
     * Resolve an alert
     */
    fun resolveAlert(alertId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                alertRepository.resolveAlert(alertId).collect { result ->
                    when (result) {
                        is AlertOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertOperationResult.Success -> {
                            // Alert resolved successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Alert resolved"))
                            
                            // Refresh alerts
                            loadAllAlerts()
                            
                            // Update selected alert if it's the one that was resolved
                            if (_selectedAlert.value?.id == alertId) {
                                _selectedAlert.value = result.alert
                            }
                            
                            // Navigate back to alerts list
                            navigationEvent.value = AlertNavigationEvent.BackToAlertsList
                        }
                        is AlertOperationResult.Error -> {
                            // Alert resolution failed
                            _uiState.value = AlertsUiState.Error("Failed to resolve alert: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.AlertOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error resolving alert")
                _uiState.value = AlertsUiState.Error("Failed to resolve alert: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.AlertOperationError("Failed to resolve alert: ${e.message}"))
            }
        }
    }
    
    /**
     * Dismiss an alert
     */
    fun dismissAlert(alertId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                alertRepository.dismissAlert(alertId).collect { result ->
                    when (result) {
                        is AlertOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertOperationResult.Success -> {
                            // Alert dismissed successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Alert dismissed"))
                            
                            // Refresh alerts
                            loadAllAlerts()
                            
                            // Clear selected alert if it's the one that was dismissed
                            if (_selectedAlert.value?.id == alertId) {
                                _selectedAlert.value = null
                            }
                            
                            // Navigate back to alerts list
                            navigationEvent.value = AlertNavigationEvent.BackToAlertsList
                        }
                        is AlertOperationResult.Error -> {
                            // Alert dismissal failed
                            _uiState.value = AlertsUiState.Error("Failed to dismiss alert: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.AlertOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error dismissing alert")
                _uiState.value = AlertsUiState.Error("Failed to dismiss alert: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.AlertOperationError("Failed to dismiss alert: ${e.message}"))
            }
        }
    }
    
    /**
     * Contact emergency services
     */
    fun contactEmergencyServices(alertId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                processEmergencyAlertsUseCase.contactEmergencyServices(alertId).collect { result ->
                    when (result) {
                        is EmergencyServicesResult.Loading -> {
                            // Ignore loading state
                        }
                        is EmergencyServicesResult.Success -> {
                            // Emergency services contacted successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Emergency services contacted"))
                            _uiState.value = AlertsUiState.Success(
                                activeAlertsCount = _activeAlerts.value.size,
                                emergencyAlertsCount = _emergencyAlerts.value.size,
                                acknowledgedAlertsCount = _acknowledgedAlerts.value.size,
                                snoozedAlertsCount = _snoozedAlerts.value.size
                            )
                            
                            // Update alert status
                            loadAllAlerts()
                        }
                        is EmergencyServicesResult.Error -> {
                            // Emergency services contact failed
                            _uiState.value = AlertsUiState.Error("Failed to contact emergency services: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.EmergencyError("Failed to contact emergency services: ${result.error.message}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error contacting emergency services")
                _uiState.value = AlertsUiState.Error("Failed to contact emergency services: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.EmergencyError("Failed to contact emergency services", e))
            }
        }
    }
    
    /**
     * Notify emergency contacts
     */
    fun notifyEmergencyContacts(alertId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                processEmergencyAlertsUseCase.notifyEmergencyContacts(alertId).collect { result ->
                    when (result) {
                        is EmergencyContactNotificationResult.Loading -> {
                            // Ignore loading state
                        }
                        is EmergencyContactNotificationResult.Success -> {
                            // Emergency contacts notified successfully
                            _errorEvents.emit(AlertErrorEvent.Success("Emergency contacts notified"))
                            _uiState.value = AlertsUiState.Success(
                                activeAlertsCount = _activeAlerts.value.size,
                                emergencyAlertsCount = _emergencyAlerts.value.size,
                                acknowledgedAlertsCount = _acknowledgedAlerts.value.size,
                                snoozedAlertsCount = _snoozedAlerts.value.size
                            )
                            
                            // Update alert status
                            loadAllAlerts()
                        }
                        is EmergencyContactNotificationResult.Error -> {
                            // Emergency contacts notification failed
                            _uiState.value = AlertsUiState.Error("Failed to notify emergency contacts: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.EmergencyError("Failed to notify emergency contacts: ${result.error.message}"))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error notifying emergency contacts")
                _uiState.value = AlertsUiState.Error("Failed to notify emergency contacts: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.EmergencyError("Failed to notify emergency contacts", e))
            }
        }
    }
    
    /**
     * Clear all alerts
     */
    fun clearAllAlerts() {
        viewModelScope.launch {
            try {
                _uiState.value = AlertsUiState.Loading
                
                alertRepository.clearAllAlerts(userId).collect { result ->
                    when (result) {
                        is AlertOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertOperationResult.Success -> {
                            // Alerts cleared successfully
                            _errorEvents.emit(AlertErrorEvent.Success("All alerts cleared"))
                            
                            // Refresh alerts
                            loadAllAlerts()
                            
                            // Clear selected alert
                            _selectedAlert.value = null
                        }
                        is AlertOperationResult.Error -> {
                            // Alert clearing failed
                            _uiState.value = AlertsUiState.Error("Failed to clear alerts: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.AlertOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error clearing alerts")
                _uiState.value = AlertsUiState.Error("Failed to clear alerts: ${e.message}")
                _errorEvents.emit(AlertErrorEvent.AlertOperationError("Failed to clear alerts: ${e.message}"))
            }
        }
    }
    
    /**
     * Load alert statistics
     */
    private fun loadAlertStatistics() {
        // Cancel any existing statistics job
        alertStatisticsJob?.cancel()
        
        alertStatisticsJob = viewModelScope.launch {
            try {
                // Get alert statistics
                alertRepository.getAlertStatistics(userId).collect { statistics ->
                    _alertStatistics.value = statistics
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading alert statistics")
                _alertStatistics.value = null
            }
        }
    }
    
    /**
     * Filter alerts
     */
    fun filterAlerts(filters: AlertFilters) {
        _alertFilters.value = filters
        applyAlertFilters()
    }
    
    /**
     * Sort alerts
     */
    fun sortAlerts(sortOption: AlertSortOption) {
        _alertSortOption.value = sortOption
        applyAlertFilters() // This also applies sorting
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _alertFilters.value = AlertFilters()
        _alertSortOption.value = AlertSortOption.PRIORITY
        applyAlertFilters()
    }
    
    /**
     * Apply alert filters and sorting
     */
    private fun applyAlertFilters() {
        // This would filter and sort alerts based on the current filters and sort option
        // For now, we'll just use the categorized alerts directly
    }
    
    /**
     * Start alert monitoring
     */
    private fun startAlertMonitoring() {
        // Cancel any existing monitoring job
        alertMonitoringJob?.cancel()
        
        // Start new monitoring job
        alertMonitoringJob = viewModelScope.launch {
            try {
                // Monitor health alerts
                monitorHealthAlertsUseCase.monitorHealthAlerts(userId).collect { result ->
                    when (result) {
                        is AlertMonitoringResult.Loading -> {
                            // Ignore loading state
                        }
                        is AlertMonitoringResult.NewAlert -> {
                            // New alert detected
                            Timber.d("New alert detected: ${result.alert.title}")
                            
                            // Emit new alert event
                            _newAlertEvents.emit(result.alert)
                            
                            // Refresh alerts
                            loadAllAlerts()
                            
                            // Check if it's an emergency alert
                            if (result.alert.severity == AlertSeverity.EMERGENCY) {
                                _emergencyAlertEvents.emit(result.alert)
                            }
                        }
                        is AlertMonitoringResult.Error -> {
                            // Alert monitoring error
                            Timber.e("Alert monitoring error: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.MonitoringError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in alert monitoring")
                _errorEvents.emit(AlertErrorEvent.MonitoringError("Alert monitoring error", e))
                
                // Restart monitoring after delay
                delay(5000)
                startAlertMonitoring()
            }
        }
    }
    
    /**
     * Restart alert monitoring
     */
    private fun restartAlertMonitoring() {
        alertMonitoringJob?.cancel()
        startAlertMonitoring()
    }
    
    /**
     * Start emergency monitoring
     */
    private fun startEmergencyMonitoring() {
        // Cancel any existing emergency monitoring job
        emergencyMonitoringJob?.cancel()
        
        // Start new emergency monitoring job
        emergencyMonitoringJob = viewModelScope.launch {
            try {
                // Monitor emergency alerts
                processEmergencyAlertsUseCase.monitorEmergencyAlerts(userId).collect { result ->
                    when (result) {
                        is EmergencyMonitoringResult.Loading -> {
                            // Ignore loading state
                        }
                        is EmergencyMonitoringResult.EmergencyDetected -> {
                            // Emergency detected
                            Timber.d("Emergency detected: ${result.alert.title}")
                            
                            // Emit emergency alert event
                            _emergencyAlertEvents.emit(result.alert)
                            
                            // Navigate to emergency screen
                            navigationEvent.value = AlertNavigationEvent.ToEmergencyScreen(result.alert)
                        }
                        is EmergencyMonitoringResult.Error -> {
                            // Emergency monitoring error
                            Timber.e("Emergency monitoring error: ${result.error.message}")
                            _errorEvents.emit(AlertErrorEvent.EmergencyError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in emergency monitoring")
                _errorEvents.emit(AlertErrorEvent.EmergencyError("Emergency monitoring error", e))
                
                // Restart monitoring after delay
                delay(5000)
                startEmergencyMonitoring()
            }
        }
    }
    
    /**
     * Start alert synchronization
     */
    private fun startAlertSync() {
        // Cancel any existing sync job
        alertSyncJob?.cancel()
        
        // Start new sync job
        alertSyncJob = viewModelScope.launch {
            while (true) {
                try {
                    // Sync alerts across devices
                    alertRepository.syncAlerts(userId).collect { result ->
                        when (result) {
                            is AlertSyncResult.Loading -> {
                                // Ignore loading state
                            }
                            is AlertSyncResult.Success -> {
                                // Alerts synced successfully
                                Timber.d("Alerts synced successfully")
                                
                                // Refresh alerts if there were changes
                                if (result.hasChanges) {
                                    loadAllAlerts()
                                }
                            }
                            is AlertSyncResult.Error -> {
                                // Alert sync error
                                Timber.e("Alert sync error: ${result.error.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error in alert sync")
                }
                
                // Wait before next sync
                delay(5 * 60 * 1000) // Sync every 5 minutes
            }
        }
    }
    
    /**
     * Navigate to alert history
     */
    fun navigateToAlertHistory() {
        // Refresh alert history
        loadAllAlerts()
        
        // Navigate to alert history
        navigationEvent.value = AlertNavigationEvent.ToAlertHistory
    }
    
    /**
     * Navigate to alert statistics
     */
    fun navigateToAlertStatistics() {
        // Refresh statistics
        loadAlertStatistics()
        
        // Navigate to statistics
        navigationEvent.value = AlertNavigationEvent.ToAlertStatistics
    }
    
    /**
     * Navigate to alert preferences
     */
    fun navigateToAlertPreferences() {
        // Refresh preferences
        loadAlertPreferences()
        
        // Navigate to preferences
        navigationEvent.value = AlertNavigationEvent.ToAlertPreferences
    }
    
    /**
     * Navigate to alert rules
     */
    fun navigateToAlertRules() {
        // Refresh rules
        loadAlertRules()
        
        // Navigate to rules
        navigationEvent.value = AlertNavigationEvent.ToAlertRules
    }
    
    /**
     * Navigate to emergency contacts
     */
    fun navigateToEmergencyContacts() {
        // Refresh contacts
        loadEmergencyContacts()
        
        // Navigate to contacts
        navigationEvent.value = AlertNavigationEvent.ToEmergencyContacts
    }
    
    /**
     * Navigate back to alerts list
     */
    fun navigateBackToAlertsList() {
        // Clear selected alert
        _selectedAlert.value = null
        
        // Navigate back to alerts list
        navigationEvent.value = AlertNavigationEvent.BackToAlertsList
    }
    
    /**
     * Create an empty alert rule
     */
    private fun createEmptyAlertRule(): AlertRule {
        return AlertRule(
            id = 0L,
            userId = userId,
            name = "",
            description = "",
            isEnabled = true,
            metricType = MetricType.NONE,
            condition = AlertCondition.NONE,
            thresholdValue = null,
            lowerThreshold = null,
            upperThreshold = null,
            changeThreshold = null,
            timeWindow = null,
            severity = AlertSeverity.MEDIUM,
            notifyEmergencyContacts = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
    
    /**
     * Create an empty emergency contact
     */
    private fun createEmptyEmergencyContact(): EmergencyContact {
        return EmergencyContact(
            id = 0L,
            userId = userId,
            name = "",
            relationship = "",
            phone = "",
            email = "",
            notificationPreference = NotificationPreference.BOTH,
            isPrimary = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
    
    /**
     * Update an alert rule field
     */
    private fun AlertRule.updateField(field: AlertRuleFormField, value: Any): AlertRule {
        return when (field) {
            AlertRuleFormField.NAME -> this.copy(name = value as String)
            AlertRuleFormField.DESCRIPTION -> this.copy(description = value as String)
            AlertRuleFormField.IS_ENABLED -> this.copy(isEnabled = value as Boolean)
            AlertRuleFormField.METRIC_TYPE -> this.copy(metricType = value as MetricType)
            AlertRuleFormField.CONDITION -> this.copy(condition = value as AlertCondition)
            AlertRuleFormField.THRESHOLD_VALUE -> this.copy(thresholdValue = value as Double?)
            AlertRuleFormField.LOWER_THRESHOLD -> this.copy(lowerThreshold = value as Double?)
            AlertRuleFormField.UPPER_THRESHOLD -> this.copy(upperThreshold = value as Double?)
            AlertRuleFormField.CHANGE_THRESHOLD -> this.copy(changeThreshold = value as Double?)
            AlertRuleFormField.TIME_WINDOW -> this.copy(timeWindow = value as Int?)
            AlertRuleFormField.SEVERITY -> this.copy(severity = value as AlertSeverity)
            AlertRuleFormField.NOTIFY_EMERGENCY_CONTACTS -> this.copy(notifyEmergencyContacts = value as Boolean)
        }
    }
    
    /**
     * Update an emergency contact field
     */
    private fun EmergencyContact.updateField(field: EmergencyContactFormField, value: Any): EmergencyContact {
        return when (field) {
            EmergencyContactFormField.NAME -> this.copy(name = value as String)
            EmergencyContactFormField.RELATIONSHIP -> this.copy(relationship = value as String)
            EmergencyContactFormField.PHONE -> this.copy(phone = value as String)
            EmergencyContactFormField.EMAIL -> this.copy(email = value as String)
            EmergencyContactFormField.NOTIFICATION_PREFERENCE -> this.copy(notificationPreference = value as NotificationPreference)
            EmergencyContactFormField.IS_PRIMARY -> this.copy(isPrimary = value as Boolean)
        }
    }
    
    /**
     * Validate phone number format
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic phone number validation
        val phoneRegex = "^[+]?[0-9]{10,15}$".toRegex()
        return phoneRegex.matches(phone)
    }
    
    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        // Basic email validation
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return emailRegex.matches(email)
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        
        // Cancel all jobs
        alertMonitoringJob?.cancel()
        alertRefreshJob?.cancel()
        emergencyMonitoringJob?.cancel()
        alertSyncJob?.cancel()
        alertStatisticsJob?.cancel()
    }
}

/**
 * Sealed class representing the UI state for alerts management
 */
sealed class AlertsUiState {
    /**
     * Loading state
     */
    data object Loading : AlertsUiState()
    
    /**
     * Empty state (no alerts)
     */
    data object Empty : AlertsUiState()
    
    /**
     * Success state
     */
    data class Success(
        val activeAlertsCount: Int,
        val emergencyAlertsCount: Int,
        val acknowledgedAlertsCount: Int,
        val snoozedAlertsCount: Int
    ) : AlertsUiState()
    
    /**
     * Error state
     */
    data class Error(val message: String) : AlertsUiState()
}

/**
 * Sealed class representing the alert rule form state
 */
sealed class AlertRuleFormState {
    /**
     * Empty form state
     */
    data object Empty : AlertRuleFormState()
    
    /**
     * Creating a new rule
     */
    data class Creating(val rule: AlertRule) : AlertRuleFormState()
    
    /**
     * Editing an existing rule
     */
    data class Editing(val rule: AlertRule) : AlertRuleFormState()
}

/**
 * Enum representing alert rule form fields
 */
enum class AlertRuleFormField {
    NAME,
    DESCRIPTION,
    IS_ENABLED,
    METRIC_TYPE,
    CONDITION,
    THRESHOLD_VALUE,
    LOWER_THRESHOLD,
    UPPER_THRESHOLD,
    CHANGE_THRESHOLD,
    TIME_WINDOW,
    SEVERITY,
    NOTIFY_EMERGENCY_CONTACTS
}

/**
 * Sealed class representing the emergency contact form state
 */
sealed class EmergencyContactFormState {
    /**
     * Empty form state
     */
    data object Empty : EmergencyContactFormState()
    
    /**
     * Creating a new contact
     */
    data class Creating(val contact: EmergencyContact) : EmergencyContactFormState()
    
    /**
     * Editing an existing contact
     */
    data class Editing(val contact: EmergencyContact) : EmergencyContactFormState()
}

/**
 * Enum representing emergency contact form fields
 */
enum class EmergencyContactFormField {
    NAME,
    RELATIONSHIP,
    PHONE,
    EMAIL,
    NOTIFICATION_PREFERENCE,
    IS_PRIMARY
}

/**
 * Data class representing alert filters
 */
data class AlertFilters(
    val severityFilter: AlertSeverity? = null,
    val statusFilter: AlertStatusFilter = AlertStatusFilter.ACTIVE,
    val typeFilter: MetricType? = null,
    val dateRange: ClosedRange<LocalDate>? = null,
    val searchQuery: String? = null
)

/**
 * Enum representing alert status filters
 */
enum class AlertStatusFilter {
    ALL,
    ACTIVE,
    ACKNOWLEDGED,
    SNOOZED,
    RESOLVED
}

/**
 * Enum representing alert sort options
 */
enum class AlertSortOption {
    PRIORITY,
    DATE_ASCENDING,
    DATE_DESCENDING,
    SEVERITY_ASCENDING,
    SEVERITY_DESCENDING,
    TYPE
}

/**
 * Sealed class for alert error events
 */
sealed class AlertErrorEvent {
    /**
     * Success event
     */
    data class Success(val message: String) : AlertErrorEvent()
    
    /**
     * Loading error
     */
    data class LoadingError(
        val message: String,
        val cause: Throwable? = null
    ) : AlertErrorEvent()
    
    /**
     * Preferences error
     */
    data class PreferencesError(
        val message: String,
        val cause: Throwable? = null
    ) : AlertErrorEvent()
    
    /**
     * Rules error
     */
    data class RulesError(
        val message: String,
        val cause: Throwable? = null
    ) : AlertErrorEvent()
    
    /**
     * Contacts error
     */
    data class ContactsError(
        val message: String,
        val cause: Throwable? = null
    ) : AlertErrorEvent()
    
    /**
     * Monitoring error
     */
    data class MonitoringError(
        val message: String,
        val cause: Throwable? = null
    ) : AlertErrorEvent()
    
    /**
     * Emergency error
     */
    data class EmergencyError(
        val message: String,
        val cause: Throwable? = null
    ) : AlertErrorEvent()
    
    /**
     * Alert operation error
     */
    data class AlertOperationError(val message: String) : AlertErrorEvent()
    
    /**
     * Alert not found error
     */
    data class AlertNotFoundError(val message: String) : AlertErrorEvent()
    
    /**
     * Rule not found error
     */
    data class RuleNotFoundError(val message: String) : AlertErrorEvent()
    
    /**
     * Contact not found error
     */
    data class ContactNotFoundError(val message: String) : AlertErrorEvent()
    
    /**
     * Validation error
     */
    data class ValidationError(val message: String) : AlertErrorEvent()
}

/**
 * Sealed class for alert navigation events
 */
sealed class AlertNavigationEvent {
    /**
     * Navigate to alert detail screen
     */
    data class ToAlertDetail(val alert: HealthAlert) : AlertNavigationEvent()
    
    /**
     * Navigate to rule detail screen
     */
    data class ToRuleDetail(val rule: AlertRule) : AlertNavigationEvent()
    
    /**
     * Navigate to rule form screen
     */
    data class ToRuleForm(val rule: AlertRule?) : AlertNavigationEvent()
    
    /**
     * Navigate to contact detail screen
     */
    data class ToContactDetail(val contact: EmergencyContact) : AlertNavigationEvent()
    
    /**
     * Navigate to contact form screen
     */
    data class ToContactForm(val contact: EmergencyContact?) : AlertNavigationEvent()
    
    /**
     * Navigate to alert history screen
     */
    data object ToAlertHistory : AlertNavigationEvent()
    
    /**
     * Navigate to alert statistics screen
     */
    data object ToAlertStatistics : AlertNavigationEvent()
    
    /**
     * Navigate to alert preferences screen
     */
    data object ToAlertPreferences : AlertNavigationEvent()
    
    /**
     * Navigate to alert rules screen
     */
    data object ToAlertRules : AlertNavigationEvent()
    
    /**
     * Navigate to emergency contacts screen
     */
    data object ToEmergencyContacts : AlertNavigationEvent()
    
    /**
     * Navigate to emergency screen
     */
    data class ToEmergencyScreen(val alert: HealthAlert) : AlertNavigationEvent()
    
    /**
     * Navigate back to alerts list
     */
    data object BackToAlertsList : AlertNavigationEvent()
    
    /**
     * Navigate back to rules list
     */
    data object BackToRulesList : AlertNavigationEvent()
    
    /**
     * Navigate back to contacts list
     */
    data object BackToContactsList : AlertNavigationEvent()
}

/**
 * Enum representing alert preference types
 */
enum class AlertPreferenceType {
    CRITICAL_ALERTS,
    HEALTH_TRENDS,
    DEVICE_CONNECTIVITY,
    MEDICATION_REMINDERS,
    ACTIVITY_GOALS,
    SLEEP_INSIGHTS,
    HEART_RATE,
    BLOOD_PRESSURE,
    BLOOD_GLUCOSE,
    EMERGENCY_CONTACTS
}
