package com.sensacare.app.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.HealthGoalRepository
import com.sensacare.app.domain.repository.UserPreferencesRepository
import com.sensacare.app.domain.usecase.health.ManageHealthGoalsUseCase
import com.sensacare.app.domain.usecase.health.TimeRange
import com.sensacare.app.domain.usecase.health.model.*
import com.sensacare.app.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * GoalsManagementViewModel - Comprehensive health goals management system
 *
 * This ViewModel implements MVVM architecture with reactive UI state management for
 * the health goals feature of the SensaCare app:
 * 
 * Key features:
 * - Goals CRUD operations with validation
 * - Goal progress tracking with real-time updates
 * - Smart goal suggestions based on health data
 * - Goal achievement celebration and rewards
 * - Goal sharing functionality
 * - Goals statistics and performance analytics
 * - Adaptive goal adjustments based on performance
 * - Goal reminders and motivation features
 * - Multi-goal tracking with priority management
 * - Goals filtering and categorization
 * - Calendar integration and scheduling
 * - Background goal progress updates
 * - Error handling and loading states
 */
@HiltViewModel
class GoalsManagementViewModel @Inject constructor(
    private val manageHealthGoalsUseCase: ManageHealthGoalsUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val healthGoalRepository: HealthGoalRepository
) : ViewModel() {

    // User ID - In a real app, this would come from authentication
    private val userId = 1L
    
    // UI State
    private val _uiState = MutableStateFlow<GoalsUiState>(GoalsUiState.Loading)
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()
    
    // Goals list
    private val _allGoals = MutableStateFlow<List<HealthGoal>>(emptyList())
    val allGoals: StateFlow<List<HealthGoal>> = _allGoals.asStateFlow()
    
    // Filtered goals
    private val _filteredGoals = MutableStateFlow<List<HealthGoal>>(emptyList())
    val filteredGoals: StateFlow<List<HealthGoal>> = _filteredGoals.asStateFlow()
    
    // Active goals
    private val _activeGoals = MutableStateFlow<List<HealthGoal>>(emptyList())
    val activeGoals: StateFlow<List<HealthGoal>> = _activeGoals.asStateFlow()
    
    // Completed goals
    private val _completedGoals = MutableStateFlow<List<HealthGoal>>(emptyList())
    val completedGoals: StateFlow<List<HealthGoal>> = _completedGoals.asStateFlow()
    
    // Selected goal for detailed view
    private val _selectedGoal = MutableStateFlow<HealthGoal?>(null)
    val selectedGoal: StateFlow<HealthGoal?> = _selectedGoal.asStateFlow()
    
    // Goal suggestions
    private val _goalSuggestions = MutableStateFlow<List<HealthGoalSuggestion>>(emptyList())
    val goalSuggestions: StateFlow<List<HealthGoalSuggestion>> = _goalSuggestions.asStateFlow()
    
    // Goal adjustments
    private val _goalAdjustments = MutableStateFlow<List<GoalAdjustmentSuggestion>>(emptyList())
    val goalAdjustments: StateFlow<List<GoalAdjustmentSuggestion>> = _goalAdjustments.asStateFlow()
    
    // Goal achievements
    private val _goalAchievements = MutableStateFlow<List<GoalAchievement>>(emptyList())
    val goalAchievements: StateFlow<List<GoalAchievement>> = _goalAchievements.asStateFlow()
    
    // Goal statistics
    private val _goalStatistics = MutableStateFlow<GoalStatistics?>(null)
    val goalStatistics: StateFlow<GoalStatistics?> = _goalStatistics.asStateFlow()
    
    // Goal reminders
    private val _goalReminders = MutableStateFlow<List<GoalReminder>>(emptyList())
    val goalReminders: StateFlow<List<GoalReminder>> = _goalReminders.asStateFlow()
    
    // Calendar events
    private val _calendarEvents = MutableStateFlow<List<GoalCalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<GoalCalendarEvent>> = _calendarEvents.asStateFlow()
    
    // Selected date for calendar view
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    // Selected month for calendar view
    private val _selectedMonth = MutableStateFlow(YearMonth(LocalDate.now().year, LocalDate.now().monthValue))
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()
    
    // Goal filters
    private val _goalFilters = MutableStateFlow(GoalFilters())
    val goalFilters: StateFlow<GoalFilters> = _goalFilters.asStateFlow()
    
    // Goal sort option
    private val _goalSortOption = MutableStateFlow(GoalSortOption.PRIORITY)
    val goalSortOption: StateFlow<GoalSortOption> = _goalSortOption.asStateFlow()
    
    // New/edit goal form state
    private val _goalFormState = MutableStateFlow<GoalFormState>(GoalFormState.Empty)
    val goalFormState: StateFlow<GoalFormState> = _goalFormState.asStateFlow()
    
    // Error events
    private val _errorEvents = MutableSharedFlow<GoalsErrorEvent>()
    val errorEvents: SharedFlow<GoalsErrorEvent> = _errorEvents.asSharedFlow()
    
    // Navigation events
    val navigationEvent = SingleLiveEvent<GoalsNavigationEvent>()
    
    // Jobs
    private var goalsRefreshJob: Job? = null
    private var progressUpdateJob: Job? = null
    private var suggestionsJob: Job? = null
    private var statisticsJob: Job? = null
    private var remindersJob: Job? = null
    private var achievementsJob: Job? = null
    private var adjustmentsJob: Job? = null
    
    // Initialize
    init {
        Timber.d("GoalsManagementViewModel initialized")
        loadAllGoals()
        startProgressUpdateJob()
        loadGoalSuggestions()
        loadGoalStatistics()
        loadGoalReminders()
        loadGoalAchievements()
        loadGoalAdjustments()
        generateCalendarEvents()
    }
    
    /**
     * Load all goals
     */
    fun loadAllGoals() {
        // Cancel any existing refresh job
        goalsRefreshJob?.cancel()
        
        goalsRefreshJob = viewModelScope.launch {
            try {
                _uiState.value = GoalsUiState.Loading
                
                // Collect all goals
                manageHealthGoalsUseCase.getGoals(userId).collect { goals ->
                    _allGoals.value = goals
                    
                    // Separate active and completed goals
                    _activeGoals.value = goals.filter { it.status == GoalStatus.ACTIVE }
                    _completedGoals.value = goals.filter { 
                        it.status == GoalStatus.ACHIEVED || it.status == GoalStatus.COMPLETED 
                    }
                    
                    // Apply filters
                    applyGoalFilters()
                    
                    // Update UI state
                    _uiState.value = if (goals.isEmpty()) {
                        GoalsUiState.Empty
                    } else {
                        GoalsUiState.Success(
                            activeGoalsCount = _activeGoals.value.size,
                            completedGoalsCount = _completedGoals.value.size,
                            hasGoalSuggestions = _goalSuggestions.value.isNotEmpty(),
                            hasGoalAdjustments = _goalAdjustments.value.isNotEmpty(),
                            hasGoalAchievements = _goalAchievements.value.isNotEmpty()
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading goals")
                _uiState.value = GoalsUiState.Error("Failed to load goals: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.LoadingError("Failed to load goals", e))
            }
        }
    }
    
    /**
     * Create a new goal
     */
    fun createGoal(goal: HealthGoal) {
        viewModelScope.launch {
            try {
                // Update UI state
                _uiState.value = GoalsUiState.Loading
                
                // Create goal
                manageHealthGoalsUseCase.createGoal(userId, goal).collect { result ->
                    when (result) {
                        is GoalOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalOperationResult.Success -> {
                            // Goal created successfully
                            _errorEvents.emit(GoalsErrorEvent.Success("Goal created successfully"))
                            
                            // Clear form state
                            _goalFormState.value = GoalFormState.Empty
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Navigate back to goals list
                            navigationEvent.value = GoalsNavigationEvent.BackToGoalsList
                        }
                        is GoalOperationResult.Error -> {
                            // Goal creation failed
                            _uiState.value = GoalsUiState.Error("Failed to create goal: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.CreateError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating goal")
                _uiState.value = GoalsUiState.Error("Failed to create goal: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.CreateError("Failed to create goal: ${e.message}"))
            }
        }
    }
    
    /**
     * Update an existing goal
     */
    fun updateGoal(goal: HealthGoal) {
        viewModelScope.launch {
            try {
                // Update UI state
                _uiState.value = GoalsUiState.Loading
                
                // Update goal
                manageHealthGoalsUseCase.updateGoal(goal).collect { result ->
                    when (result) {
                        is GoalOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalOperationResult.Success -> {
                            // Goal updated successfully
                            _errorEvents.emit(GoalsErrorEvent.Success("Goal updated successfully"))
                            
                            // Clear form state
                            _goalFormState.value = GoalFormState.Empty
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Update selected goal if it's the one that was updated
                            if (_selectedGoal.value?.id == goal.id) {
                                _selectedGoal.value = result.goal
                            }
                            
                            // Navigate back to goals list
                            navigationEvent.value = GoalsNavigationEvent.BackToGoalsList
                        }
                        is GoalOperationResult.Error -> {
                            // Goal update failed
                            _uiState.value = GoalsUiState.Error("Failed to update goal: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.UpdateError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating goal")
                _uiState.value = GoalsUiState.Error("Failed to update goal: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.UpdateError("Failed to update goal: ${e.message}"))
            }
        }
    }
    
    /**
     * Delete a goal
     */
    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                // Update UI state
                _uiState.value = GoalsUiState.Loading
                
                // Delete goal
                manageHealthGoalsUseCase.deleteGoal(goalId).collect { result ->
                    when (result) {
                        is GoalOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalOperationResult.Success -> {
                            // Goal deleted successfully
                            _errorEvents.emit(GoalsErrorEvent.Success("Goal deleted successfully"))
                            
                            // Clear selected goal if it's the one that was deleted
                            if (_selectedGoal.value?.id == goalId) {
                                _selectedGoal.value = null
                            }
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Navigate back to goals list
                            navigationEvent.value = GoalsNavigationEvent.BackToGoalsList
                        }
                        is GoalOperationResult.Error -> {
                            // Goal deletion failed
                            _uiState.value = GoalsUiState.Error("Failed to delete goal: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.DeleteError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting goal")
                _uiState.value = GoalsUiState.Error("Failed to delete goal: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.DeleteError("Failed to delete goal: ${e.message}"))
            }
        }
    }
    
    /**
     * Complete a goal manually
     */
    fun completeGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                // Update UI state
                _uiState.value = GoalsUiState.Loading
                
                // Complete goal
                manageHealthGoalsUseCase.completeGoal(goalId).collect { result ->
                    when (result) {
                        is GoalOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalOperationResult.Success -> {
                            // Goal completed successfully
                            _errorEvents.emit(GoalsErrorEvent.Success("Goal completed successfully"))
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Update selected goal if it's the one that was completed
                            if (_selectedGoal.value?.id == goalId) {
                                _selectedGoal.value = result.goal
                            }
                            
                            // Show achievement celebration
                            showGoalAchievementCelebration(result.goal)
                        }
                        is GoalOperationResult.Error -> {
                            // Goal completion failed
                            _uiState.value = GoalsUiState.Error("Failed to complete goal: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.GoalOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error completing goal")
                _uiState.value = GoalsUiState.Error("Failed to complete goal: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.GoalOperationError("Failed to complete goal: ${e.message}"))
            }
        }
    }
    
    /**
     * Abandon a goal
     */
    fun abandonGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                // Update UI state
                _uiState.value = GoalsUiState.Loading
                
                // Abandon goal
                manageHealthGoalsUseCase.abandonGoal(goalId).collect { result ->
                    when (result) {
                        is GoalOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalOperationResult.Success -> {
                            // Goal abandoned successfully
                            _errorEvents.emit(GoalsErrorEvent.Success("Goal abandoned"))
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Update selected goal if it's the one that was abandoned
                            if (_selectedGoal.value?.id == goalId) {
                                _selectedGoal.value = result.goal
                            }
                            
                            // Navigate back to goals list
                            navigationEvent.value = GoalsNavigationEvent.BackToGoalsList
                        }
                        is GoalOperationResult.Error -> {
                            // Goal abandonment failed
                            _uiState.value = GoalsUiState.Error("Failed to abandon goal: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.GoalOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error abandoning goal")
                _uiState.value = GoalsUiState.Error("Failed to abandon goal: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.GoalOperationError("Failed to abandon goal: ${e.message}"))
            }
        }
    }
    
    /**
     * Update progress for all active goals
     */
    fun updateAllGoalsProgress() {
        viewModelScope.launch {
            try {
                // Update progress for all active goals
                manageHealthGoalsUseCase.updateAllGoalsProgress(userId).collect { result ->
                    when (result) {
                        is GoalProgressUpdateResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalProgressUpdateResult.Success -> {
                            // Progress updated successfully
                            val updatedGoals = result.updates
                            
                            // Check for newly achieved goals
                            val newlyAchievedGoals = updatedGoals.filter { it.isNewlyAchieved }
                            
                            // Show achievement celebrations
                            newlyAchievedGoals.forEach { update ->
                                showGoalAchievementCelebration(update.goal)
                            }
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Update selected goal if it's one of the updated goals
                            _selectedGoal.value?.let { selectedGoal ->
                                val updatedSelectedGoal = updatedGoals.find { it.goal.id == selectedGoal.id }?.goal
                                if (updatedSelectedGoal != null) {
                                    _selectedGoal.value = updatedSelectedGoal
                                }
                            }
                            
                            // Refresh achievements
                            loadGoalAchievements()
                        }
                        is GoalProgressUpdateResult.Error -> {
                            // Progress update failed
                            Timber.e("Failed to update goal progress: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.ProgressUpdateError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating goal progress")
                _errorEvents.emit(GoalsErrorEvent.ProgressUpdateError("Failed to update goal progress: ${e.message}"))
            }
        }
    }
    
    /**
     * Load goal suggestions
     */
    private fun loadGoalSuggestions() {
        // Cancel any existing suggestions job
        suggestionsJob?.cancel()
        
        suggestionsJob = viewModelScope.launch {
            try {
                // Get goal suggestions
                manageHealthGoalsUseCase.generateGoalSuggestions(userId).collect { result ->
                    when (result) {
                        is GoalSuggestionResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalSuggestionResult.Success -> {
                            // Got suggestions successfully
                            _goalSuggestions.value = result.suggestions
                        }
                        is GoalSuggestionResult.Error -> {
                            // Failed to get suggestions
                            Timber.e("Failed to get goal suggestions: ${result.error.message}")
                            _goalSuggestions.value = emptyList()
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading goal suggestions")
                _goalSuggestions.value = emptyList()
            }
        }
    }
    
    /**
     * Create a goal from a suggestion
     */
    fun createGoalFromSuggestion(suggestion: HealthGoalSuggestion) {
        viewModelScope.launch {
            try {
                // Update UI state
                _uiState.value = GoalsUiState.Loading
                
                // Create goal from suggestion
                manageHealthGoalsUseCase.createGoalFromSuggestion(userId, suggestion).collect { result ->
                    when (result) {
                        is GoalOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalOperationResult.Success -> {
                            // Goal created successfully
                            _errorEvents.emit(GoalsErrorEvent.Success("Goal created from suggestion"))
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Refresh suggestions
                            loadGoalSuggestions()
                        }
                        is GoalOperationResult.Error -> {
                            // Goal creation failed
                            _uiState.value = GoalsUiState.Error("Failed to create goal from suggestion: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.CreateError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error creating goal from suggestion")
                _uiState.value = GoalsUiState.Error("Failed to create goal from suggestion: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.CreateError("Failed to create goal from suggestion: ${e.message}"))
            }
        }
    }
    
    /**
     * Load goal adjustments
     */
    private fun loadGoalAdjustments() {
        // Cancel any existing adjustments job
        adjustmentsJob?.cancel()
        
        adjustmentsJob = viewModelScope.launch {
            try {
                // Get goal adjustments
                manageHealthGoalsUseCase.getSuggestedGoalAdjustments(userId).collect { adjustments ->
                    _goalAdjustments.value = adjustments
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading goal adjustments")
                _goalAdjustments.value = emptyList()
            }
        }
    }
    
    /**
     * Apply a goal adjustment
     */
    fun applyGoalAdjustment(adjustment: GoalAdjustmentSuggestion) {
        viewModelScope.launch {
            try {
                // Update UI state
                _uiState.value = GoalsUiState.Loading
                
                // Apply adjustment
                manageHealthGoalsUseCase.applyGoalAdjustment(adjustment).collect { result ->
                    when (result) {
                        is GoalOperationResult.Loading -> {
                            // Ignore loading state
                        }
                        is GoalOperationResult.Success -> {
                            // Adjustment applied successfully
                            _errorEvents.emit(GoalsErrorEvent.Success("Goal adjusted successfully"))
                            
                            // Refresh goals
                            loadAllGoals()
                            
                            // Refresh adjustments
                            loadGoalAdjustments()
                            
                            // Update selected goal if it's the one that was adjusted
                            if (_selectedGoal.value?.id == adjustment.goalId) {
                                _selectedGoal.value = result.goal
                            }
                        }
                        is GoalOperationResult.Error -> {
                            // Adjustment failed
                            _uiState.value = GoalsUiState.Error("Failed to adjust goal: ${result.error.message}")
                            _errorEvents.emit(GoalsErrorEvent.GoalOperationError(result.error.message))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error applying goal adjustment")
                _uiState.value = GoalsUiState.Error("Failed to adjust goal: ${e.message}")
                _errorEvents.emit(GoalsErrorEvent.GoalOperationError("Failed to adjust goal: ${e.message}"))
            }
        }
    }
    
    /**
     * Load goal statistics
     */
    private fun loadGoalStatistics() {
        // Cancel any existing statistics job
        statisticsJob?.cancel()
        
        statisticsJob = viewModelScope.launch {
            try {
                // Get goal statistics
                manageHealthGoalsUseCase.getGoalStatistics(userId).collect { statistics ->
                    _goalStatistics.value = statistics
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading goal statistics")
                _goalStatistics.value = null
            }
        }
    }
    
    /**
     * Load goal reminders
     */
    private fun loadGoalReminders() {
        // Cancel any existing reminders job
        remindersJob?.cancel()
        
        remindersJob = viewModelScope.launch {
            try {
                // Get goal reminders for today
                manageHealthGoalsUseCase.getGoalRemindersForToday(userId).collect { reminders ->
                    _goalReminders.value = reminders
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading goal reminders")
                _goalReminders.value = emptyList()
            }
        }
    }
    
    /**
     * Load goal achievements
     */
    private fun loadGoalAchievements() {
        // Cancel any existing achievements job
        achievementsJob?.cancel()
        
        achievementsJob = viewModelScope.launch {
            try {
                // Get goal achievements for today
                manageHealthGoalsUseCase.getGoalAchievementsToday(userId).collect { achievements ->
                    _goalAchievements.value = achievements
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading goal achievements")
                _goalAchievements.value = emptyList()
            }
        }
    }
    
    /**
     * Show goal achievement celebration
     */
    private fun showGoalAchievementCelebration(goal: HealthGoal) {
        viewModelScope.launch {
            try {
                // Format achievement for sharing
                val sharingContent = manageHealthGoalsUseCase.formatGoalForSharing(goal.id).firstOrNull()
                
                // Show achievement celebration
                navigationEvent.value = GoalsNavigationEvent.ShowAchievementCelebration(
                    goalId = goal.id,
                    goalTitle = goal.title,
                    goalType = goal.type,
                    streakDays = goal.streakDays,
                    sharingContent = sharingContent
                )
            } catch (e: Exception) {
                Timber.e(e, "Error showing goal achievement celebration")
            }
        }
    }
    
    /**
     * Share goal achievement
     */
    fun shareGoalAchievement(goalId: Long) {
        viewModelScope.launch {
            try {
                // Format achievement for sharing
                val sharingContent = manageHealthGoalsUseCase.formatGoalForSharing(goalId).firstOrNull()
                    ?: throw IllegalStateException("No sharing content available for goal")
                
                // Trigger share event
                navigationEvent.value = GoalsNavigationEvent.ShareGoalAchievement(
                    title = sharingContent.title,
                    message = sharingContent.getFormattedMessage(),
                    imageType = sharingContent.imageType
                )
            } catch (e: Exception) {
                Timber.e(e, "Error sharing goal achievement")
                _errorEvents.emit(GoalsErrorEvent.SharingError("Failed to share goal achievement", e))
            }
        }
    }
    
    /**
     * Generate calendar events from goals
     */
    private fun generateCalendarEvents() {
        viewModelScope.launch {
            try {
                // Get all active goals
                val activeGoals = _activeGoals.value
                
                // Generate calendar events
                val events = mutableListOf<GoalCalendarEvent>()
                
                // Add events for goals with deadlines
                activeGoals.forEach { goal ->
                    if (goal.endDate != null) {
                        events.add(
                            GoalCalendarEvent(
                                goalId = goal.id,
                                title = goal.title,
                                date = goal.endDate,
                                type = CalendarEventType.DEADLINE,
                                goalType = goal.type,
                                progress = goal.progress
                            )
                        )
                    }
                    
                    // Add events for recurring goals
                    if (goal.frequency != GoalFrequency.ONE_TIME) {
                        // Generate events for the next 30 days
                        val today = LocalDate.now()
                        val endDate = today.plusDays(30)
                        
                        var currentDate = today
                        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
                            if (isGoalScheduledForDate(goal, currentDate)) {
                                events.add(
                                    GoalCalendarEvent(
                                        goalId = goal.id,
                                        title = goal.title,
                                        date = currentDate,
                                        type = CalendarEventType.RECURRING_GOAL,
                                        goalType = goal.type,
                                        progress = if (currentDate == today) goal.progress else 0.0
                                    )
                                )
                            }
                            
                            currentDate = currentDate.plusDays(1)
                        }
                    }
                }
                
                // Update calendar events
                _calendarEvents.value = events
            } catch (e: Exception) {
                Timber.e(e, "Error generating calendar events")
            }
        }
    }
    
    /**
     * Check if a goal is scheduled for a specific date
     */
    private fun isGoalScheduledForDate(goal: HealthGoal, date: LocalDate): Boolean {
        // Check if goal is active on this date
        if (goal.endDate != null && date.isAfter(goal.endDate)) {
            return false
        }
        
        if (date.isBefore(goal.startDate)) {
            return false
        }
        
        // Check frequency
        return when (goal.frequency) {
            GoalFrequency.DAILY -> true
            GoalFrequency.WEEKLY -> {
                // Check if the day of week is in reminder days
                goal.reminderDays.contains(date.dayOfWeek)
            }
            GoalFrequency.MONTHLY -> {
                // Check if it's the same day of month as the start date
                date.dayOfMonth == goal.startDate.dayOfMonth
            }
            GoalFrequency.ONE_TIME -> {
                // Only scheduled for the end date
                goal.endDate == date
            }
        }
    }
    
    /**
     * Select a goal for detailed view
     */
    fun selectGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                // Get goal details
                manageHealthGoalsUseCase.getGoal(goalId).collect { goal ->
                    if (goal != null) {
                        _selectedGoal.value = goal
                        navigationEvent.value = GoalsNavigationEvent.ToGoalDetail(goal)
                    } else {
                        _errorEvents.emit(GoalsErrorEvent.GoalNotFoundError("Goal not found"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error selecting goal")
                _errorEvents.emit(GoalsErrorEvent.GoalNotFoundError("Failed to load goal details: ${e.message}"))
            }
        }
    }
    
    /**
     * Clear selected goal
     */
    fun clearSelectedGoal() {
        _selectedGoal.value = null
    }
    
    /**
     * Start new goal creation
     */
    fun startNewGoalCreation() {
        // Clear form state
        _goalFormState.value = GoalFormState.Empty
        
        // Navigate to goal form
        navigationEvent.value = GoalsNavigationEvent.ToGoalForm(null)
    }
    
    /**
     * Start goal editing
     */
    fun startGoalEditing(goalId: Long) {
        viewModelScope.launch {
            try {
                // Get goal details
                manageHealthGoalsUseCase.getGoal(goalId).collect { goal ->
                    if (goal != null) {
                        // Set form state
                        _goalFormState.value = GoalFormState.Editing(goal)
                        
                        // Navigate to goal form
                        navigationEvent.value = GoalsNavigationEvent.ToGoalForm(goal)
                    } else {
                        _errorEvents.emit(GoalsErrorEvent.GoalNotFoundError("Goal not found"))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting goal editing")
                _errorEvents.emit(GoalsErrorEvent.GoalNotFoundError("Failed to load goal details: ${e.message}"))
            }
        }
    }
    
    /**
     * Update goal form field
     */
    fun updateGoalFormField(field: GoalFormField, value: Any) {
        val currentForm = _goalFormState.value
        
        // Create updated form state
        val updatedForm = when (currentForm) {
            is GoalFormState.Empty -> {
                // Create new form with the field set
                val newGoal = createEmptyGoal().updateField(field, value)
                GoalFormState.Creating(newGoal)
            }
            is GoalFormState.Creating -> {
                // Update existing form
                val updatedGoal = currentForm.goal.updateField(field, value)
                GoalFormState.Creating(updatedGoal)
            }
            is GoalFormState.Editing -> {
                // Update existing form
                val updatedGoal = currentForm.goal.updateField(field, value)
                GoalFormState.Editing(updatedGoal)
            }
        }
        
        // Update form state
        _goalFormState.value = updatedForm
    }
    
    /**
     * Validate goal form
     */
    fun validateGoalForm(): Boolean {
        val currentForm = _goalFormState.value
        
        // Get goal from form
        val goal = when (currentForm) {
            is GoalFormState.Empty -> return false
            is GoalFormState.Creating -> currentForm.goal
            is GoalFormState.Editing -> currentForm.goal
        }
        
        // Basic validation
        if (goal.title.isBlank()) {
            _errorEvents.emit(GoalsErrorEvent.ValidationError("Title is required"))
            return false
        }
        
        if (goal.targetValue <= 0) {
            _errorEvents.emit(GoalsErrorEvent.ValidationError("Target value must be greater than zero"))
            return false
        }
        
        if (goal.unit.isBlank()) {
            _errorEvents.emit(GoalsErrorEvent.ValidationError("Unit is required"))
            return false
        }
        
        // Validate date range
        if (goal.endDate != null && goal.startDate.isAfter(goal.endDate)) {
            _errorEvents.emit(GoalsErrorEvent.ValidationError("End date must be after start date"))
            return false
        }
        
        // Validate type-specific requirements
        when (goal.type) {
            GoalType.STEPS -> {
                if (goal.targetValue > 50000) {
                    _errorEvents.emit(GoalsErrorEvent.ValidationError("Step goal should be realistic (max 50,000)"))
                    return false
                }
            }
            GoalType.SLEEP_DURATION -> {
                if (goal.targetValue > 12 * 60) {
                    _errorEvents.emit(GoalsErrorEvent.ValidationError("Sleep goal should be realistic (max 12 hours)"))
                    return false
                }
            }
            GoalType.WEIGHT -> {
                val startWeight = goal.additionalData["startWeight"]
                if (startWeight == null) {
                    _errorEvents.emit(GoalsErrorEvent.ValidationError("Starting weight is required for weight goals"))
                    return false
                }
            }
            GoalType.BLOOD_PRESSURE -> {
                val targetSystolic = goal.additionalData["targetSystolic"]
                val targetDiastolic = goal.additionalData["targetDiastolic"]
                
                if (targetSystolic == null || targetDiastolic == null) {
                    _errorEvents.emit(GoalsErrorEvent.ValidationError("Target systolic and diastolic values are required for blood pressure goals"))
                    return false
                }
            }
            GoalType.HEART_RATE_ZONE -> {
                val lowerBound = goal.additionalData["lowerBound"]
                val upperBound = goal.additionalData["upperBound"]
                
                if (lowerBound == null || upperBound == null) {
                    _errorEvents.emit(GoalsErrorEvent.ValidationError("Heart rate zone bounds are required"))
                    return false
                } else if (lowerBound is Int && upperBound is Int && lowerBound >= upperBound) {
                    _errorEvents.emit(GoalsErrorEvent.ValidationError("Upper bound must be greater than lower bound"))
                    return false
                }
            }
            else -> {
                // No specific validation for other types
            }
        }
        
        return true
    }
    
    /**
     * Submit goal form
     */
    fun submitGoalForm() {
        // Validate form
        if (!validateGoalForm()) {
            return
        }
        
        // Get goal from form
        val goal = when (val currentForm = _goalFormState.value) {
            is GoalFormState.Empty -> {
                _errorEvents.emit(GoalsErrorEvent.ValidationError("No goal data available"))
                return
            }
            is GoalFormState.Creating -> currentForm.goal
            is GoalFormState.Editing -> currentForm.goal
        }
        
        // Create or update goal
        if (goal.id == 0L) {
            createGoal(goal)
        } else {
            updateGoal(goal)
        }
    }
    
    /**
     * Cancel goal form
     */
    fun cancelGoalForm() {
        // Clear form state
        _goalFormState.value = GoalFormState.Empty
        
        // Navigate back to goals list
        navigationEvent.value = GoalsNavigationEvent.BackToGoalsList
    }
    
    /**
     * Filter goals
     */
    fun filterGoals(filters: GoalFilters) {
        _goalFilters.value = filters
        applyGoalFilters()
    }
    
    /**
     * Sort goals
     */
    fun sortGoals(sortOption: GoalSortOption) {
        _goalSortOption.value = sortOption
        applyGoalFilters() // This also applies sorting
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _goalFilters.value = GoalFilters()
        _goalSortOption.value = GoalSortOption.PRIORITY
        applyGoalFilters()
    }
    
    /**
     * Apply goal filters and sorting
     */
    private fun applyGoalFilters() {
        val allGoals = _allGoals.value
        val filters = _goalFilters.value
        val sortOption = _goalSortOption.value
        
        // Apply status filter
        val statusFiltered = when (filters.statusFilter) {
            GoalStatusFilter.ALL -> allGoals
            GoalStatusFilter.ACTIVE -> allGoals.filter { it.status == GoalStatus.ACTIVE }
            GoalStatusFilter.COMPLETED -> allGoals.filter { 
                it.status == GoalStatus.ACHIEVED || it.status == GoalStatus.COMPLETED 
            }
            GoalStatusFilter.ABANDONED -> allGoals.filter { it.status == GoalStatus.ABANDONED }
        }
        
        // Apply type filter
        val typeFiltered = if (filters.typeFilter != null) {
            statusFiltered.filter { it.type == filters.typeFilter }
        } else {
            statusFiltered
        }
        
        // Apply date filter
        val dateFiltered = if (filters.dateRange != null) {
            typeFiltered.filter { goal ->
                val goalDate = goal.endDate ?: goal.startDate
                goalDate in filters.dateRange.start..filters.dateRange.endInclusive
            }
        } else {
            typeFiltered
        }
        
        // Apply search filter
        val searchFiltered = if (!filters.searchQuery.isNullOrBlank()) {
            dateFiltered.filter { 
                it.title.contains(filters.searchQuery, ignoreCase = true) || 
                it.description.contains(filters.searchQuery, ignoreCase = true) 
            }
        } else {
            dateFiltered
        }
        
        // Apply sorting
        val sorted = when (sortOption) {
            GoalSortOption.PRIORITY -> searchFiltered.sortedBy { 
                // Higher priority goals come first
                when (it.status) {
                    GoalStatus.ACTIVE -> 0
                    GoalStatus.ACHIEVED -> 1
                    GoalStatus.COMPLETED -> 2
                    GoalStatus.ABANDONED -> 3
                }
            }
            GoalSortOption.DATE_ASCENDING -> searchFiltered.sortedBy { 
                it.endDate ?: it.startDate 
            }
            GoalSortOption.DATE_DESCENDING -> searchFiltered.sortedByDescending { 
                it.endDate ?: it.startDate 
            }
            GoalSortOption.PROGRESS_ASCENDING -> searchFiltered.sortedBy { 
                it.progress 
            }
            GoalSortOption.PROGRESS_DESCENDING -> searchFiltered.sortedByDescending { 
                it.progress 
            }
            GoalSortOption.ALPHABETICAL -> searchFiltered.sortedBy { 
                it.title 
            }
            GoalSortOption.TYPE -> searchFiltered.sortedBy { 
                it.type.name 
            }
        }
        
        // Update filtered goals
        _filteredGoals.value = sorted
    }
    
    /**
     * Select a date in the calendar
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        
        // Filter goals for the selected date
        val goalsForDate = _allGoals.value.filter { goal ->
            when {
                // Check if it's the goal's end date
                goal.endDate == date -> true
                
                // Check if it's a recurring goal scheduled for this date
                isGoalScheduledForDate(goal, date) -> true
                
                // Otherwise, not scheduled for this date
                else -> false
            }
        }
        
        // Update filtered goals
        _filteredGoals.value = goalsForDate
        
        // Navigate to date view
        navigationEvent.value = GoalsNavigationEvent.ToDateView(date, goalsForDate)
    }
    
    /**
     * Select a month in the calendar
     */
    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
        
        // Regenerate calendar events
        generateCalendarEvents()
    }
    
    /**
     * Navigate to statistics view
     */
    fun navigateToStatistics() {
        // Refresh statistics
        loadGoalStatistics()
        
        // Navigate to statistics view
        navigationEvent.value = GoalsNavigationEvent.ToStatisticsView
    }
    
    /**
     * Navigate to suggestions view
     */
    fun navigateToSuggestions() {
        // Refresh suggestions
        loadGoalSuggestions()
        
        // Navigate to suggestions view
        navigationEvent.value = GoalsNavigationEvent.ToSuggestionsView
    }
    
    /**
     * Navigate to adjustments view
     */
    fun navigateToAdjustments() {
        // Refresh adjustments
        loadGoalAdjustments()
        
        // Navigate to adjustments view
        navigationEvent.value = GoalsNavigationEvent.ToAdjustmentsView
    }
    
    /**
     * Navigate to calendar view
     */
    fun navigateToCalendar() {
        // Regenerate calendar events
        generateCalendarEvents()
        
        // Navigate to calendar view
        navigationEvent.value = GoalsNavigationEvent.ToCalendarView
    }
    
    /**
     * Navigate to reminders view
     */
    fun navigateToReminders() {
        // Refresh reminders
        loadGoalReminders()
        
        // Navigate to reminders view
        navigationEvent.value = GoalsNavigationEvent.ToRemindersView
    }
    
    /**
     * Navigate back to goals list
     */
    fun navigateBackToGoalsList() {
        // Clear selected goal
        clearSelectedGoal()
        
        // Reset filters to show all active goals
        _goalFilters.value = GoalFilters(statusFilter = GoalStatusFilter.ACTIVE)
        applyGoalFilters()
        
        // Navigate back to goals list
        navigationEvent.value = GoalsNavigationEvent.BackToGoalsList
    }
    
    /**
     * Start background progress update job
     */
    private fun startProgressUpdateJob() {
        // Cancel any existing progress update job
        progressUpdateJob?.cancel()
        
        // Start new progress update job
        progressUpdateJob = viewModelScope.launch {
            while (true) {
                delay(15 * 60 * 1000) // Update every 15 minutes
                updateAllGoalsProgress()
            }
        }
    }
    
    /**
     * Create an empty goal
     */
    private fun createEmptyGoal(): HealthGoal {
        return HealthGoal(
            id = 0L,
            userId = userId,
            type = GoalType.STEPS,
            title = "",
            description = "",
            targetValue = 0.0,
            unit = "",
            frequency = GoalFrequency.DAILY,
            duration = GoalDuration.ONGOING,
            startDate = LocalDate.now(),
            endDate = null,
            reminderTime = null,
            reminderDays = setOf(),
            progress = 0.0,
            status = GoalStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            lastAchievedDate = null,
            streakDays = 0,
            lastUpdated = LocalDateTime.now(),
            allowMultiple = false,
            isPublic = false,
            additionalData = mapOf()
        )
    }
    
    /**
     * Update a goal field
     */
    private fun HealthGoal.updateField(field: GoalFormField, value: Any): HealthGoal {
        return when (field) {
            GoalFormField.TITLE -> this.copy(title = value as String)
            GoalFormField.DESCRIPTION -> this.copy(description = value as String)
            GoalFormField.TYPE -> this.copy(type = value as GoalType)
            GoalFormField.TARGET_VALUE -> this.copy(targetValue = (value as Number).toDouble())
            GoalFormField.UNIT -> this.copy(unit = value as String)
            GoalFormField.FREQUENCY -> this.copy(frequency = value as GoalFrequency)
            GoalFormField.DURATION -> this.copy(duration = value as GoalDuration)
            GoalFormField.START_DATE -> this.copy(startDate = value as LocalDate)
            GoalFormField.END_DATE -> this.copy(endDate = value as LocalDate?)
            GoalFormField.REMINDER_TIME -> this.copy(reminderTime = value as String?)
            GoalFormField.REMINDER_DAYS -> this.copy(reminderDays = value as Set<DayOfWeek>)
            GoalFormField.ALLOW_MULTIPLE -> this.copy(allowMultiple = value as Boolean)
            GoalFormField.IS_PUBLIC -> this.copy(isPublic = value as Boolean)
            GoalFormField.ADDITIONAL_DATA -> {
                val additionalData = value as Map<String, Any>
                this.copy(additionalData = this.additionalData + additionalData)
            }
        }
    }
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        
        // Cancel all jobs
        goalsRefreshJob?.cancel()
        progressUpdateJob?.cancel()
        suggestionsJob?.cancel()
        statisticsJob?.cancel()
        remindersJob?.cancel()
        achievementsJob?.cancel()
        adjustmentsJob?.cancel()
    }
}

/**
 * Sealed class representing the UI state for goals management
 */
sealed class GoalsUiState {
    /**
     * Loading state
     */
    data object Loading : GoalsUiState()
    
    /**
     * Empty state (no goals)
     */
    data object Empty : GoalsUiState()
    
    /**
     * Success state
     */
    data class Success(
        val activeGoalsCount: Int,
        val completedGoalsCount: Int,
        val hasGoalSuggestions: Boolean,
        val hasGoalAdjustments: Boolean,
        val hasGoalAchievements: Boolean
    ) : GoalsUiState()
    
    /**
     * Error state
     */
    data class Error(val message: String) : GoalsUiState()
}

/**
 * Sealed class representing the goal form state
 */
sealed class GoalFormState {
    /**
     * Empty form state
     */
    data object Empty : GoalFormState()
    
    /**
     * Creating a new goal
     */
    data class Creating(val goal: HealthGoal) : GoalFormState()
    
    /**
     * Editing an existing goal
     */
    data class Editing(val goal: HealthGoal) : GoalFormState()
}

/**
 * Enum representing goal form fields
 */
enum class GoalFormField {
    TITLE,
    DESCRIPTION,
    TYPE,
    TARGET_VALUE,
    UNIT,
    FREQUENCY,
    DURATION,
    START_DATE,
    END_DATE,
    REMINDER_TIME,
    REMINDER_DAYS,
    ALLOW_MULTIPLE,
    IS_PUBLIC,
    ADDITIONAL_DATA
}

/**
 * Data class representing goal filters
 */
data class GoalFilters(
    val statusFilter: GoalStatusFilter = GoalStatusFilter.ACTIVE,
    val typeFilter: GoalType? = null,
    val dateRange: ClosedRange<LocalDate>? = null,
    val searchQuery: String? = null
)

/**
 * Enum representing goal status filters
 */
enum class GoalStatusFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    ABANDONED
}

/**
 * Enum representing goal sort options
 */
enum class GoalSortOption {
    PRIORITY,
    DATE_ASCENDING,
    DATE_DESCENDING,
    PROGRESS_ASCENDING,
    PROGRESS_DESCENDING,
    ALPHABETICAL,
    TYPE
}

/**
 * Data class representing a goal calendar event
 */
data class GoalCalendarEvent(
    val goalId: Long,
    val title: String,
    val date: LocalDate,
    val type: CalendarEventType,
    val goalType: GoalType,
    val progress: Double
) {
    /**
     * Get color based on event type and goal type
     */
    fun getColor(): String {
        return when (type) {
            CalendarEventType.DEADLINE -> "#F44336" // Red
            CalendarEventType.RECURRING_GOAL -> {
                when (goalType) {
                    GoalType.STEPS -> "#4CAF50" // Green
                    GoalType.SLEEP_DURATION -> "#5E35B1" // Purple
                    GoalType.ACTIVITY_MINUTES -> "#FF9800" // Orange
                    GoalType.HEART_RATE_ZONE -> "#E53935" // Red
                    GoalType.WEIGHT -> "#2196F3" // Blue
                    GoalType.BLOOD_PRESSURE -> "#F44336" // Red
                    GoalType.WATER_INTAKE -> "#03A9F4" // Light Blue
                    GoalType.CUSTOM -> "#9E9E9E" // Gray
                }
            }
            CalendarEventType.REMINDER -> "#FFC107" // Amber
        }
    }
    
    /**
     * Get icon based on goal type
     */
    fun getIcon(): String {
        return when (goalType) {
            GoalType.STEPS -> "directions_walk"
            GoalType.SLEEP_DURATION -> "bedtime"
            GoalType.ACTIVITY_MINUTES -> "fitness_center"
            GoalType.HEART_RATE_ZONE -> "favorite"
            GoalType.WEIGHT -> "monitor_weight"
            GoalType.BLOOD_PRESSURE -> "bloodtype"
            GoalType.WATER_INTAKE -> "water_drop"
            GoalType.CUSTOM -> "flag"
        }
    }
    
    /**
     * Format progress as a string
     */
    fun getProgressFormatted(): String {
        return "${progress.toInt()}%"
    }
}

/**
 * Enum representing calendar event types
 */
enum class CalendarEventType {
    DEADLINE,
    RECURRING_GOAL,
    REMINDER
}

/**
 * Data class representing a year and month
 */
data class YearMonth(
    val year: Int,
    val month: Int
) {
    /**
     * Get the next month
     */
    fun next(): YearMonth {
        return if (month == 12) {
            YearMonth(year + 1, 1)
        } else {
            YearMonth(year, month + 1)
        }
    }
    
    /**
     * Get the previous month
     */
    fun previous(): YearMonth {
        return if (month == 1) {
            YearMonth(year - 1, 12)
        } else {
            YearMonth(year, month - 1)
        }
    }
    
    /**
     * Format as a string
     */
    fun format(): String {
        val monthName = java.time.Month.of(month).name.lowercase().capitalize()
        return "$monthName $year"
    }
    
    /**
     * Get the first day of the month
     */
    fun firstDay(): LocalDate {
        return LocalDate.of(year, month, 1)
    }
    
    /**
     * Get the last day of the month
     */
    fun lastDay(): LocalDate {
        return LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)
    }
    
    /**
     * Get the number of days in the month
     */
    fun lengthOfMonth(): Int {
        return lastDay().dayOfMonth
    }
}

/**
 * Sealed class for goals error events
 */
sealed class GoalsErrorEvent {
    /**
     * Success event
     */
    data class Success(val message: String) : GoalsErrorEvent()
    
    /**
     * Loading error
     */
    data class LoadingError(
        val message: String,
        val cause: Throwable? = null
    ) : GoalsErrorEvent()
    
    /**
     * Create error
     */
    data class CreateError(val message: String) : GoalsErrorEvent()
    
    /**
     * Update error
     */
    data class UpdateError(val message: String) : GoalsErrorEvent()
    
    /**
     * Delete error
     */
    data class DeleteError(val message: String) : GoalsErrorEvent()
    
    /**
     * Goal not found error
     */
    data class GoalNotFoundError(val message: String) : GoalsErrorEvent()
    
    /**
     * Validation error
     */
    data class ValidationError(val message: String) : GoalsErrorEvent()
    
    /**
     * Progress update error
     */
    data class ProgressUpdateError(val message: String) : GoalsErrorEvent()
    
    /**
     * Goal operation error
     */
    data class GoalOperationError(val message: String) : GoalsErrorEvent()
    
    /**
     * Sharing error
     */
    data class SharingError(
        val message: String,
        val cause: Throwable? = null
    ) : GoalsErrorEvent()
}

/**
 * Sealed class for goals navigation events
 */
sealed class GoalsNavigationEvent {
    /**
     * Navigate to goal detail screen
     */
    data class ToGoalDetail(val goal: HealthGoal) : GoalsNavigationEvent()
    
    /**
     * Navigate to goal form screen
     */
    data class ToGoalForm(val goal: HealthGoal?) : GoalsNavigationEvent()
    
    /**
     * Navigate to date view
     */
    data class ToDateView(
        val date: LocalDate,
        val goals: List<HealthGoal>
    ) : GoalsNavigationEvent()
    
    /**
     * Navigate to statistics view
     */
    data object ToStatisticsView : GoalsNavigationEvent()
    
    /**
     * Navigate to suggestions view
     */
    data object ToSuggestionsView : GoalsNavigationEvent()
    
    /**
     * Navigate to adjustments view
     */
    data object ToAdjustmentsView : GoalsNavigationEvent()
    
    /**
     * Navigate to calendar view
     */
    data object ToCalendarView : GoalsNavigationEvent()
    
    /**
     * Navigate to reminders view
     */
    data object ToRemindersView : GoalsNavigationEvent()
    
    /**
     * Navigate back to goals list
     */
    data object BackToGoalsList : GoalsNavigationEvent()
    
    /**
     * Show achievement celebration
     */
    data class ShowAchievementCelebration(
        val goalId: Long,
        val goalTitle: String,
        val goalType: GoalType,
        val streakDays: Int,
        val sharingContent: SharingContent?
    ) : GoalsNavigationEvent()
    
    /**
     * Share goal achievement
     */
    data class ShareGoalAchievement(
        val title: String,
        val message: String,
        val imageType: SharingImageType
    ) : GoalsNavigationEvent()
}

/**
 * Extension function to capitalize the first letter of a string
 */
private fun String.capitalize(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase() else it.toString() 
    }
}
