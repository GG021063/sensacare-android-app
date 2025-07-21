package com.sensacare.app.domain.usecase.health

import com.sensacare.app.domain.model.*
import com.sensacare.app.domain.repository.HealthDataRepository
import com.sensacare.app.domain.repository.HealthGoalRepository
import com.sensacare.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ManageHealthGoalsUseCase - Comprehensive health goal management system
 *
 * This use case handles all aspects of health goal management:
 * - CRUD operations for health goals
 * - Goal progress tracking and analytics
 * - Smart goal suggestions based on user data
 * - Goal achievement notifications and rewards
 * - Adaptive goal adjustment based on progress
 * - Goal streaks and milestone tracking
 * - Support for different goal types (steps, sleep, heart rate, etc.)
 * - Goal reminders and motivation
 * - Social sharing capabilities for achievements
 * - Historical goal performance analysis
 *
 * It provides a reactive interface with Flow and comprehensive error handling.
 */
class ManageHealthGoalsUseCase(
    private val healthGoalRepository: HealthGoalRepository,
    private val healthDataRepository: HealthDataRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        // Goal adjustment thresholds
        private const val GOAL_ADJUSTMENT_THRESHOLD_HIGH = 0.95 // 95% achievement consistently
        private const val GOAL_ADJUSTMENT_THRESHOLD_LOW = 0.6 // 60% achievement consistently
        private const val GOAL_ADJUSTMENT_PERIOD_DAYS = 14 // Check for adjustment after 2 weeks
        
        // Goal streak thresholds
        private const val STREAK_MILESTONE_SMALL = 7 // 1 week
        private const val STREAK_MILESTONE_MEDIUM = 30 // 1 month
        private const val STREAK_MILESTONE_LARGE = 90 // 3 months
        
        // Default goals by type
        private const val DEFAULT_STEPS_GOAL = 10000
        private const val DEFAULT_SLEEP_GOAL_MINUTES = 480 // 8 hours
        private const val DEFAULT_ACTIVITY_GOAL_MINUTES = 30 // 30 minutes
        private const val DEFAULT_WATER_GOAL_ML = 2000 // 2 liters
        private const val DEFAULT_WEIGHT_LOSS_GOAL_KG = 0.5 // 0.5 kg per week
        
        // Goal adjustment percentages
        private const val GOAL_INCREASE_PERCENTAGE = 1.1 // 10% increase
        private const val GOAL_DECREASE_PERCENTAGE = 0.9 // 10% decrease
    }

    /**
     * Create a new health goal
     *
     * @param userId The user ID
     * @param goal The goal to create
     * @return Flow of GoalOperationResult
     */
    fun createGoal(userId: Long, goal: HealthGoal): Flow<GoalOperationResult> = flow {
        emit(GoalOperationResult.Loading)
        
        try {
            // Validate the goal
            val validationResult = validateGoal(goal)
            if (validationResult is GoalValidationResult.Invalid) {
                emit(GoalOperationResult.Error(GoalError.ValidationError(validationResult.reasons)))
                return@flow
            }
            
            // Check for duplicate goals
            val existingGoals = healthGoalRepository.getGoalsByType(userId, goal.type).first()
            val duplicateGoal = existingGoals.find { 
                it.type == goal.type && 
                it.status != GoalStatus.COMPLETED && 
                it.status != GoalStatus.ABANDONED 
            }
            
            if (duplicateGoal != null && !goal.allowMultiple) {
                emit(GoalOperationResult.Error(
                    GoalError.DuplicateGoalError("An active goal of this type already exists")
                ))
                return@flow
            }
            
            // Prepare goal for creation
            val goalToCreate = goal.copy(
                userId = userId,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                status = GoalStatus.ACTIVE,
                progress = 0.0,
                streakDays = 0,
                lastUpdated = LocalDateTime.now()
            )
            
            // Create the goal
            val createdGoal = healthGoalRepository.createGoal(goalToCreate)
            
            // Schedule initial progress update
            updateGoalProgress(createdGoal)
            
            emit(GoalOperationResult.Success(createdGoal))
            
        } catch (e: Exception) {
            Timber.e(e, "Error creating goal")
            emit(GoalOperationResult.Error(
                GoalError.RepositoryError("Failed to create goal: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Get all goals for a user
     *
     * @param userId The user ID
     * @return Flow of List<HealthGoal>
     */
    fun getGoals(userId: Long): Flow<List<HealthGoal>> {
        return healthGoalRepository.getGoals(userId)
    }

    /**
     * Get goals by status
     *
     * @param userId The user ID
     * @param status The goal status
     * @return Flow of List<HealthGoal>
     */
    fun getGoalsByStatus(userId: Long, status: GoalStatus): Flow<List<HealthGoal>> {
        return healthGoalRepository.getGoalsByStatus(userId, status)
    }

    /**
     * Get goals by type
     *
     * @param userId The user ID
     * @param type The goal type
     * @return Flow of List<HealthGoal>
     */
    fun getGoalsByType(userId: Long, type: GoalType): Flow<List<HealthGoal>> {
        return healthGoalRepository.getGoalsByType(userId, type)
    }

    /**
     * Get a specific goal
     *
     * @param goalId The goal ID
     * @return Flow of HealthGoal?
     */
    fun getGoal(goalId: Long): Flow<HealthGoal?> {
        return healthGoalRepository.getGoal(goalId)
    }

    /**
     * Update a health goal
     *
     * @param goal The goal to update
     * @return Flow of GoalOperationResult
     */
    fun updateGoal(goal: HealthGoal): Flow<GoalOperationResult> = flow {
        emit(GoalOperationResult.Loading)
        
        try {
            // Validate the goal
            val validationResult = validateGoal(goal)
            if (validationResult is GoalValidationResult.Invalid) {
                emit(GoalOperationResult.Error(GoalError.ValidationError(validationResult.reasons)))
                return@flow
            }
            
            // Get the existing goal
            val existingGoal = healthGoalRepository.getGoal(goal.id).firstOrNull()
            if (existingGoal == null) {
                emit(GoalOperationResult.Error(GoalError.GoalNotFoundError("Goal not found: ${goal.id}")))
                return@flow
            }
            
            // Prepare goal for update
            val goalToUpdate = goal.copy(
                updatedAt = LocalDateTime.now(),
                // Preserve these fields from the existing goal
                createdAt = existingGoal.createdAt,
                userId = existingGoal.userId,
                streakDays = existingGoal.streakDays,
                lastAchievedDate = existingGoal.lastAchievedDate
            )
            
            // Update the goal
            val updatedGoal = healthGoalRepository.updateGoal(goalToUpdate)
            
            // Update goal progress
            updateGoalProgress(updatedGoal)
            
            emit(GoalOperationResult.Success(updatedGoal))
            
        } catch (e: Exception) {
            Timber.e(e, "Error updating goal")
            emit(GoalOperationResult.Error(
                GoalError.RepositoryError("Failed to update goal: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Delete a health goal
     *
     * @param goalId The ID of the goal to delete
     * @return Flow of GoalOperationResult
     */
    fun deleteGoal(goalId: Long): Flow<GoalOperationResult> = flow {
        emit(GoalOperationResult.Loading)
        
        try {
            // Check if goal exists
            val existingGoal = healthGoalRepository.getGoal(goalId).firstOrNull()
            if (existingGoal == null) {
                emit(GoalOperationResult.Error(GoalError.GoalNotFoundError("Goal not found: $goalId")))
                return@flow
            }
            
            // Delete the goal
            val success = healthGoalRepository.deleteGoal(goalId)
            
            if (success) {
                emit(GoalOperationResult.Success(existingGoal))
            } else {
                emit(GoalOperationResult.Error(GoalError.RepositoryError("Failed to delete goal")))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error deleting goal")
            emit(GoalOperationResult.Error(
                GoalError.RepositoryError("Failed to delete goal: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Update progress for all active goals
     *
     * @param userId The user ID
     * @return Flow of GoalProgressUpdateResult
     */
    fun updateAllGoalsProgress(userId: Long): Flow<GoalProgressUpdateResult> = flow {
        emit(GoalProgressUpdateResult.Loading)
        
        try {
            // Get all active goals
            val activeGoals = healthGoalRepository.getGoalsByStatus(userId, GoalStatus.ACTIVE).first()
            
            if (activeGoals.isEmpty()) {
                emit(GoalProgressUpdateResult.Success(emptyList()))
                return@flow
            }
            
            val updatedGoals = mutableListOf<GoalProgressUpdate>()
            val achievedGoals = mutableListOf<HealthGoal>()
            
            // Update progress for each goal
            for (goal in activeGoals) {
                val (progress, isAchieved) = calculateGoalProgress(goal)
                
                // Update goal in repository
                val updatedGoal = updateGoalWithProgress(goal, progress, isAchieved)
                
                // Track updates
                updatedGoals.add(
                    GoalProgressUpdate(
                        goal = updatedGoal,
                        previousProgress = goal.progress,
                        newProgress = progress,
                        isNewlyAchieved = isAchieved && goal.status != GoalStatus.ACHIEVED
                    )
                )
                
                // Track achieved goals
                if (isAchieved && goal.status != GoalStatus.ACHIEVED) {
                    achievedGoals.add(updatedGoal)
                }
            }
            
            // Process goal achievements
            if (achievedGoals.isNotEmpty()) {
                processGoalAchievements(achievedGoals)
            }
            
            // Check for goal adjustments
            checkForGoalAdjustments(userId)
            
            emit(GoalProgressUpdateResult.Success(updatedGoals))
            
        } catch (e: Exception) {
            Timber.e(e, "Error updating goal progress")
            emit(GoalProgressUpdateResult.Error(
                GoalError.ProgressUpdateError("Failed to update goal progress: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Generate smart goal suggestions based on user data
     *
     * @param userId The user ID
     * @return Flow of GoalSuggestionResult
     */
    fun generateGoalSuggestions(userId: Long): Flow<GoalSuggestionResult> = flow {
        emit(GoalSuggestionResult.Loading)
        
        try {
            // Get user preferences
            val userPreferences = userPreferencesRepository.getUserPreferences(userId).first()
            
            // Get recent health data
            val endDate = LocalDateTime.now()
            val startDate = endDate.minusDays(30) // Last 30 days
            
            val heartRateData = healthDataRepository.getHeartRateMeasurements(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            val sleepData = healthDataRepository.getSleepRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            val stepData = healthDataRepository.getStepRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            val activityData = healthDataRepository.getActivityRecords(
                userId = userId,
                startTime = startDate,
                endTime = endDate
            ).first()
            
            // Get current active goals
            val activeGoals = healthGoalRepository.getGoalsByStatus(userId, GoalStatus.ACTIVE).first()
            val activeGoalTypes = activeGoals.map { it.type }.toSet()
            
            // Generate suggestions
            val suggestions = mutableListOf<HealthGoalSuggestion>()
            
            // Step goal suggestion
            if (GoalType.STEPS !in activeGoalTypes && stepData.isNotEmpty()) {
                val avgSteps = stepData.map { it.count }.average().roundToInt()
                val currentStepGoal = userPreferences.stepGoal ?: DEFAULT_STEPS_GOAL
                
                // Suggest step goal based on current performance
                val suggestedStepGoal = when {
                    avgSteps < 3000 -> 5000 // Very low activity
                    avgSteps < 5000 -> 7500 // Low activity
                    avgSteps < 7500 -> 10000 // Moderate activity
                    avgSteps < 10000 -> 12000 // Active
                    else -> max(avgSteps + 1000, currentStepGoal) // Very active
                }
                
                suggestions.add(
                    HealthGoalSuggestion(
                        type = GoalType.STEPS,
                        title = "Daily Step Goal",
                        description = "Based on your recent activity of $avgSteps steps per day, " +
                                "we suggest a goal of $suggestedStepGoal steps daily.",
                        suggestedValue = suggestedStepGoal.toDouble(),
                        currentAverage = avgSteps.toDouble(),
                        difficulty = calculateGoalDifficulty(avgSteps.toDouble(), suggestedStepGoal.toDouble()),
                        healthBenefits = listOf(
                            "Improved cardiovascular health",
                            "Better weight management",
                            "Reduced stress levels",
                            "Enhanced mood and energy"
                        ),
                        suggestedDuration = GoalDuration.ONGOING
                    )
                )
            }
            
            // Sleep goal suggestion
            if (GoalType.SLEEP_DURATION !in activeGoalTypes && sleepData.isNotEmpty()) {
                val avgSleepMinutes = sleepData.map { it.durationMinutes }.average().roundToInt()
                val currentSleepGoal = userPreferences.sleepGoalMinutes ?: DEFAULT_SLEEP_GOAL_MINUTES
                
                // Suggest sleep goal based on current performance
                val suggestedSleepGoal = when {
                    avgSleepMinutes < 6 * 60 -> 7 * 60 // Less than 6 hours
                    avgSleepMinutes < 7 * 60 -> 8 * 60 // 6-7 hours
                    else -> max(avgSleepMinutes, currentSleepGoal) // 7+ hours
                }
                
                suggestions.add(
                    HealthGoalSuggestion(
                        type = GoalType.SLEEP_DURATION,
                        title = "Sleep Duration Goal",
                        description = "Based on your recent sleep patterns of " +
                                "${String.format("%.1f", avgSleepMinutes / 60.0)} hours per night, " +
                                "we suggest aiming for ${suggestedSleepGoal / 60} hours of sleep.",
                        suggestedValue = suggestedSleepGoal.toDouble(),
                        currentAverage = avgSleepMinutes.toDouble(),
                        difficulty = calculateGoalDifficulty(avgSleepMinutes.toDouble(), suggestedSleepGoal.toDouble()),
                        healthBenefits = listOf(
                            "Improved cognitive function",
                            "Better mood regulation",
                            "Enhanced immune function",
                            "Improved energy levels",
                            "Better stress management"
                        ),
                        suggestedDuration = GoalDuration.ONGOING
                    )
                )
            }
            
            // Activity minutes goal suggestion
            if (GoalType.ACTIVITY_MINUTES !in activeGoalTypes && activityData.isNotEmpty()) {
                // Calculate weekly activity minutes
                val weeklyActivityMinutes = activityData
                    .groupBy { it.startTime.toLocalDate().let { date -> 
                        date.minusDays(date.dayOfWeek.value.toLong() - 1) 
                    }}
                    .map { (_, activities) -> activities.sumOf { it.durationMinutes } }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.roundToInt()
                    ?: 0
                
                val dailyActivityMinutes = weeklyActivityMinutes / 7
                
                // Suggest activity goal based on WHO recommendations
                val suggestedActivityGoal = when {
                    weeklyActivityMinutes < 60 -> 90 // Very low activity
                    weeklyActivityMinutes < 120 -> 150 // Low activity
                    weeklyActivityMinutes < 150 -> 180 // Below recommendations
                    weeklyActivityMinutes < 210 -> 210 // Meeting minimum recommendations
                    else -> max(weeklyActivityMinutes + 30, 240) // Above recommendations
                }
                
                suggestions.add(
                    HealthGoalSuggestion(
                        type = GoalType.ACTIVITY_MINUTES,
                        title = "Weekly Active Minutes Goal",
                        description = "Based on your recent activity of $weeklyActivityMinutes minutes per week " +
                                "(about $dailyActivityMinutes minutes per day), we suggest a goal of " +
                                "$suggestedActivityGoal active minutes weekly.",
                        suggestedValue = suggestedActivityGoal.toDouble(),
                        currentAverage = weeklyActivityMinutes.toDouble(),
                        difficulty = calculateGoalDifficulty(weeklyActivityMinutes.toDouble(), suggestedActivityGoal.toDouble()),
                        healthBenefits = listOf(
                            "Reduced risk of chronic diseases",
                            "Improved cardiovascular health",
                            "Better weight management",
                            "Increased muscle strength",
                            "Improved mental health"
                        ),
                        suggestedDuration = GoalDuration.WEEKLY
                    )
                )
            }
            
            // Heart rate zone goal suggestion
            if (GoalType.HEART_RATE_ZONE !in activeGoalTypes && heartRateData.isNotEmpty() && activityData.isNotEmpty()) {
                // Check if user has enough exercise data with elevated heart rate
                val hasCardioWorkouts = activityData.any { 
                    it.activityType == ActivityType.RUNNING || 
                    it.activityType == ActivityType.CYCLING ||
                    it.activityType == ActivityType.CARDIO
                }
                
                if (hasCardioWorkouts) {
                    // Calculate max heart rate based on age (or use a default)
                    val age = userPreferences.age ?: 40
                    val estimatedMaxHeartRate = 220 - age
                    
                    // Calculate target heart rate zone (moderate intensity: 64-76% of max HR)
                    val lowerBound = (estimatedMaxHeartRate * 0.64).roundToInt()
                    val upperBound = (estimatedMaxHeartRate * 0.76).roundToInt()
                    
                    // Suggest weekly minutes in target zone
                    val suggestedMinutesInZone = 90 // 30 minutes, 3 times per week
                    
                    suggestions.add(
                        HealthGoalSuggestion(
                            type = GoalType.HEART_RATE_ZONE,
                            title = "Cardio Heart Rate Zone Goal",
                            description = "We suggest spending $suggestedMinutesInZone minutes per week " +
                                    "in your moderate-intensity heart rate zone ($lowerBound-$upperBound bpm). " +
                                    "This helps optimize cardiovascular benefits from your workouts.",
                            suggestedValue = suggestedMinutesInZone.toDouble(),
                            currentAverage = 0.0, // Would need more detailed heart rate during exercise data
                            difficulty = GoalDifficulty.MODERATE,
                            healthBenefits = listOf(
                                "Improved cardiovascular efficiency",
                                "Enhanced endurance",
                                "Optimized calorie burning",
                                "Reduced risk of heart disease",
                                "Improved fitness level"
                            ),
                            suggestedDuration = GoalDuration.WEEKLY,
                            additionalData = mapOf(
                                "lowerBound" to lowerBound,
                                "upperBound" to upperBound
                            )
                        )
                    )
                }
            }
            
            emit(GoalSuggestionResult.Success(suggestions))
            
        } catch (e: Exception) {
            Timber.e(e, "Error generating goal suggestions")
            emit(GoalSuggestionResult.Error(
                GoalError.SuggestionError("Failed to generate goal suggestions: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Create a goal from a suggestion
     *
     * @param userId The user ID
     * @param suggestion The goal suggestion
     * @return Flow of GoalOperationResult
     */
    fun createGoalFromSuggestion(
        userId: Long,
        suggestion: HealthGoalSuggestion
    ): Flow<GoalOperationResult> = flow {
        emit(GoalOperationResult.Loading)
        
        try {
            // Create goal from suggestion
            val goal = when (suggestion.type) {
                GoalType.STEPS -> {
                    HealthGoal(
                        id = 0, // Will be assigned by repository
                        userId = userId,
                        type = GoalType.STEPS,
                        title = suggestion.title,
                        description = "Daily step goal",
                        targetValue = suggestion.suggestedValue,
                        unit = "steps",
                        frequency = GoalFrequency.DAILY,
                        duration = suggestion.suggestedDuration,
                        startDate = LocalDate.now(),
                        endDate = when (suggestion.suggestedDuration) {
                            GoalDuration.WEEKLY -> LocalDate.now().plusWeeks(1)
                            GoalDuration.MONTHLY -> LocalDate.now().plusMonths(1)
                            GoalDuration.QUARTERLY -> LocalDate.now().plusMonths(3)
                            GoalDuration.YEARLY -> LocalDate.now().plusYears(1)
                            else -> null // Ongoing
                        },
                        reminderTime = null,
                        reminderDays = setOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        ),
                        allowMultiple = false,
                        isPublic = false
                    )
                }
                GoalType.SLEEP_DURATION -> {
                    HealthGoal(
                        id = 0, // Will be assigned by repository
                        userId = userId,
                        type = GoalType.SLEEP_DURATION,
                        title = suggestion.title,
                        description = "Nightly sleep duration goal",
                        targetValue = suggestion.suggestedValue,
                        unit = "minutes",
                        frequency = GoalFrequency.DAILY,
                        duration = suggestion.suggestedDuration,
                        startDate = LocalDate.now(),
                        endDate = when (suggestion.suggestedDuration) {
                            GoalDuration.WEEKLY -> LocalDate.now().plusWeeks(1)
                            GoalDuration.MONTHLY -> LocalDate.now().plusMonths(1)
                            GoalDuration.QUARTERLY -> LocalDate.now().plusMonths(3)
                            GoalDuration.YEARLY -> LocalDate.now().plusYears(1)
                            else -> null // Ongoing
                        },
                        reminderTime = null,
                        reminderDays = setOf(
                            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, 
                            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
                            DayOfWeek.SATURDAY
                        ),
                        allowMultiple = false,
                        isPublic = false
                    )
                }
                GoalType.ACTIVITY_MINUTES -> {
                    HealthGoal(
                        id = 0, // Will be assigned by repository
                        userId = userId,
                        type = GoalType.ACTIVITY_MINUTES,
                        title = suggestion.title,
                        description = "Weekly active minutes goal",
                        targetValue = suggestion.suggestedValue,
                        unit = "minutes",
                        frequency = GoalFrequency.WEEKLY,
                        duration = suggestion.suggestedDuration,
                        startDate = LocalDate.now(),
                        endDate = when (suggestion.suggestedDuration) {
                            GoalDuration.MONTHLY -> LocalDate.now().plusMonths(1)
                            GoalDuration.QUARTERLY -> LocalDate.now().plusMonths(3)
                            GoalDuration.YEARLY -> LocalDate.now().plusYears(1)
                            else -> LocalDate.now().plusMonths(1) // Default to 1 month for weekly goals
                        },
                        reminderTime = null,
                        reminderDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        allowMultiple = false,
                        isPublic = false
                    )
                }
                GoalType.HEART_RATE_ZONE -> {
                    val lowerBound = suggestion.additionalData?.get("lowerBound") as? Int ?: 0
                    val upperBound = suggestion.additionalData?.get("upperBound") as? Int ?: 0
                    
                    HealthGoal(
                        id = 0, // Will be assigned by repository
                        userId = userId,
                        type = GoalType.HEART_RATE_ZONE,
                        title = suggestion.title,
                        description = "Weekly minutes in heart rate zone ($lowerBound-$upperBound bpm)",
                        targetValue = suggestion.suggestedValue,
                        unit = "minutes",
                        frequency = GoalFrequency.WEEKLY,
                        duration = suggestion.suggestedDuration,
                        startDate = LocalDate.now(),
                        endDate = when (suggestion.suggestedDuration) {
                            GoalDuration.MONTHLY -> LocalDate.now().plusMonths(1)
                            GoalDuration.QUARTERLY -> LocalDate.now().plusMonths(3)
                            GoalDuration.YEARLY -> LocalDate.now().plusYears(1)
                            else -> LocalDate.now().plusMonths(1) // Default to 1 month for weekly goals
                        },
                        reminderTime = null,
                        reminderDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                        allowMultiple = false,
                        isPublic = false,
                        additionalData = mapOf(
                            "lowerBound" to lowerBound,
                            "upperBound" to upperBound
                        )
                    )
                }
                else -> {
                    emit(GoalOperationResult.Error(
                        GoalError.ValidationError(listOf("Unsupported goal type for automatic creation"))
                    ))
                    return@flow
                }
            }
            
            // Create the goal
            val result = createGoal(userId, goal).first()
            emit(result)
            
        } catch (e: Exception) {
            Timber.e(e, "Error creating goal from suggestion")
            emit(GoalOperationResult.Error(
                GoalError.RepositoryError("Failed to create goal from suggestion: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Get goal progress history
     *
     * @param goalId The goal ID
     * @param limit The maximum number of history entries to return
     * @return Flow of List<GoalProgressHistory>
     */
    fun getGoalProgressHistory(goalId: Long, limit: Int = 30): Flow<List<GoalProgressHistory>> {
        return healthGoalRepository.getGoalProgressHistory(goalId, limit)
    }

    /**
     * Get goal achievement statistics
     *
     * @param userId The user ID
     * @param timeRange The time range to analyze
     * @return Flow of GoalStatistics
     */
    fun getGoalStatistics(
        userId: Long,
        timeRange: TimeRange = TimeRange.MONTH
    ): Flow<GoalStatistics> = flow {
        try {
            // Calculate date range
            val endDate = LocalDate.now()
            val startDate = when (timeRange) {
                is TimeRange.DAY -> endDate.minusDays(1)
                is TimeRange.WEEK -> endDate.minusDays(7)
                is TimeRange.MONTH -> endDate.minusDays(30)
                is TimeRange.QUARTER -> endDate.minusDays(90)
                is TimeRange.YEAR -> endDate.minusDays(365)
                is TimeRange.CUSTOM -> timeRange.startDate.toLocalDate()
            }
            
            // Get all goals in the time range
            val allGoals = healthGoalRepository.getGoalsInDateRange(userId, startDate, endDate).first()
            
            // Calculate statistics
            val totalGoals = allGoals.size
            val completedGoals = allGoals.count { it.status == GoalStatus.ACHIEVED || it.status == GoalStatus.COMPLETED }
            val abandonedGoals = allGoals.count { it.status == GoalStatus.ABANDONED }
            val activeGoals = allGoals.count { it.status == GoalStatus.ACTIVE }
            
            val completionRate = if (totalGoals > 0) {
                (completedGoals.toDouble() / totalGoals) * 100
            } else {
                0.0
            }
            
            // Group by type
            val goalsByType = allGoals.groupBy { it.type }
            
            // Calculate completion rate by type
            val completionRateByType = goalsByType.mapValues { (_, goals) ->
                val typeTotal = goals.size
                val typeCompleted = goals.count { it.status == GoalStatus.ACHIEVED || it.status == GoalStatus.COMPLETED }
                if (typeTotal > 0) (typeCompleted.toDouble() / typeTotal) * 100 else 0.0
            }
            
            // Find longest streak
            val longestStreak = allGoals.maxOfOrNull { it.streakDays } ?: 0
            
            // Find current streaks
            val currentStreaks = allGoals
                .filter { it.status == GoalStatus.ACTIVE }
                .associate { it.id to it.streakDays }
            
            // Create statistics object
            val statistics = GoalStatistics(
                userId = userId,
                timeRange = timeRange,
                totalGoals = totalGoals,
                completedGoals = completedGoals,
                abandonedGoals = abandonedGoals,
                activeGoals = activeGoals,
                completionRate = completionRate,
                completionRateByType = completionRateByType,
                longestStreak = longestStreak,
                currentStreaks = currentStreaks,
                goalCountByType = goalsByType.mapValues { it.value.size }
            )
            
            emit(statistics)
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting goal statistics")
            emit(
                GoalStatistics(
                    userId = userId,
                    timeRange = timeRange,
                    totalGoals = 0,
                    completedGoals = 0,
                    abandonedGoals = 0,
                    activeGoals = 0,
                    completionRate = 0.0,
                    completionRateByType = emptyMap(),
                    longestStreak = 0,
                    currentStreaks = emptyMap(),
                    goalCountByType = emptyMap(),
                    error = "Failed to retrieve goal statistics: ${e.message}"
                )
            )
        }
    }.flowOn(dispatcher)

    /**
     * Get goal reminders for today
     *
     * @param userId The user ID
     * @return Flow of List<GoalReminder>
     */
    fun getGoalRemindersForToday(userId: Long): Flow<List<GoalReminder>> = flow {
        try {
            // Get active goals
            val activeGoals = healthGoalRepository.getGoalsByStatus(userId, GoalStatus.ACTIVE).first()
            
            // Get today's day of week
            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek
            
            // Filter goals that have reminders for today
            val goalsWithRemindersToday = activeGoals.filter { goal ->
                goal.reminderDays.contains(dayOfWeek)
            }
            
            // Generate reminders
            val reminders = goalsWithRemindersToday.map { goal ->
                // Get current progress
                val (progress, _) = calculateGoalProgress(goal)
                
                // Generate appropriate reminder message
                val message = generateReminderMessage(goal, progress)
                
                GoalReminder(
                    goalId = goal.id,
                    goalTitle = goal.title,
                    goalType = goal.type,
                    reminderTime = goal.reminderTime,
                    message = message,
                    currentProgress = progress,
                    targetValue = goal.targetValue
                )
            }
            
            emit(reminders)
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting goal reminders")
            emit(emptyList())
        }
    }.flowOn(dispatcher)

    /**
     * Format goal achievement for sharing
     *
     * @param goalId The goal ID
     * @return Flow of SharingContent?
     */
    fun formatGoalForSharing(goalId: Long): Flow<SharingContent?> = flow {
        try {
            // Get the goal
            val goal = healthGoalRepository.getGoal(goalId).firstOrNull()
            if (goal == null || goal.status != GoalStatus.ACHIEVED && goal.status != GoalStatus.COMPLETED) {
                emit(null)
                return@flow
            }
            
            // Format achievement message
            val title = "Goal Achieved: ${goal.title}"
            
            val message = buildString {
                append("I achieved my ${goal.type.toString().lowercase().replace('_', ' ')} goal")
                
                if (goal.streakDays > 1) {
                    append(" with a streak of ${goal.streakDays} days")
                }
                
                append("! ")
                
                when (goal.type) {
                    GoalType.STEPS -> append("${goal.targetValue.toInt()} steps")
                    GoalType.SLEEP_DURATION -> append("${(goal.targetValue / 60).toInt()} hours of sleep")
                    GoalType.ACTIVITY_MINUTES -> append("${goal.targetValue.toInt()} minutes of activity")
                    GoalType.HEART_RATE_ZONE -> append("${goal.targetValue.toInt()} minutes in target heart rate zone")
                    GoalType.WEIGHT -> append("Reached my weight goal of ${goal.targetValue} kg")
                    GoalType.BLOOD_PRESSURE -> append("Maintained healthy blood pressure")
                    GoalType.WATER_INTAKE -> append("${goal.targetValue.toInt()} ml of water daily")
                    GoalType.CUSTOM -> append("${goal.title}")
                }
                
                append(" #HealthGoals #SensaCare")
            }
            
            // Create sharing content
            val sharingContent = SharingContent(
                title = title,
                message = message,
                imageType = when (goal.type) {
                    GoalType.STEPS -> SharingImageType.STEPS
                    GoalType.SLEEP_DURATION -> SharingImageType.SLEEP
                    GoalType.ACTIVITY_MINUTES -> SharingImageType.ACTIVITY
                    GoalType.HEART_RATE_ZONE -> SharingImageType.HEART_RATE
                    GoalType.WEIGHT -> SharingImageType.WEIGHT
                    GoalType.BLOOD_PRESSURE -> SharingImageType.BLOOD_PRESSURE
                    GoalType.WATER_INTAKE -> SharingImageType.WATER
                    GoalType.CUSTOM -> SharingImageType.GENERIC
                },
                goalType = goal.type,
                achievementDate = goal.lastAchievedDate ?: LocalDate.now(),
                streakDays = goal.streakDays,
                additionalData = mapOf(
                    "targetValue" to goal.targetValue,
                    "unit" to goal.unit
                )
            )
            
            emit(sharingContent)
            
        } catch (e: Exception) {
            Timber.e(e, "Error formatting goal for sharing")
            emit(null)
        }
    }.flowOn(dispatcher)

    /**
     * Abandon a goal
     *
     * @param goalId The goal ID
     * @return Flow of GoalOperationResult
     */
    fun abandonGoal(goalId: Long): Flow<GoalOperationResult> = flow {
        emit(GoalOperationResult.Loading)
        
        try {
            // Get the goal
            val goal = healthGoalRepository.getGoal(goalId).firstOrNull()
            if (goal == null) {
                emit(GoalOperationResult.Error(GoalError.GoalNotFoundError("Goal not found: $goalId")))
                return@flow
            }
            
            // Update goal status
            val updatedGoal = goal.copy(
                status = GoalStatus.ABANDONED,
                updatedAt = LocalDateTime.now()
            )
            
            // Update in repository
            val result = healthGoalRepository.updateGoal(updatedGoal)
            
            emit(GoalOperationResult.Success(result))
            
        } catch (e: Exception) {
            Timber.e(e, "Error abandoning goal")
            emit(GoalOperationResult.Error(
                GoalError.RepositoryError("Failed to abandon goal: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Complete a goal (mark as completed manually)
     *
     * @param goalId The goal ID
     * @return Flow of GoalOperationResult
     */
    fun completeGoal(goalId: Long): Flow<GoalOperationResult> = flow {
        emit(GoalOperationResult.Loading)
        
        try {
            // Get the goal
            val goal = healthGoalRepository.getGoal(goalId).firstOrNull()
            if (goal == null) {
                emit(GoalOperationResult.Error(GoalError.GoalNotFoundError("Goal not found: $goalId")))
                return@flow
            }
            
            // Update goal status
            val updatedGoal = goal.copy(
                status = GoalStatus.COMPLETED,
                progress = 100.0,
                updatedAt = LocalDateTime.now(),
                lastAchievedDate = LocalDate.now()
            )
            
            // Update in repository
            val result = healthGoalRepository.updateGoal(updatedGoal)
            
            // Add to goal history
            healthGoalRepository.addGoalProgressHistory(
                GoalProgressHistory(
                    id = 0, // Will be assigned by repository
                    goalId = goalId,
                    date = LocalDate.now(),
                    progress = 100.0,
                    value = goal.targetValue,
                    note = "Manually marked as completed"
                )
            )
            
            emit(GoalOperationResult.Success(result))
            
        } catch (e: Exception) {
            Timber.e(e, "Error completing goal")
            emit(GoalOperationResult.Error(
                GoalError.RepositoryError("Failed to complete goal: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Get goal achievements that should be celebrated
     *
     * @param userId The user ID
     * @return Flow of List<GoalAchievement>
     */
    fun getGoalAchievementsToday(userId: Long): Flow<List<GoalAchievement>> = flow {
        try {
            // Get goals achieved today
            val today = LocalDate.now()
            val achievedGoals = healthGoalRepository.getGoals(userId).first()
                .filter { it.lastAchievedDate == today }
            
            // Generate achievement notifications
            val achievements = achievedGoals.map { goal ->
                GoalAchievement(
                    goalId = goal.id,
                    goalTitle = goal.title,
                    goalType = goal.type,
                    achievementDate = today,
                    streakDays = goal.streakDays,
                    message = generateAchievementMessage(goal),
                    rewardPoints = calculateRewardPoints(goal),
                    milestoneAchieved = isMilestoneStreak(goal.streakDays),
                    targetValue = goal.targetValue,
                    unit = goal.unit
                )
            }
            
            emit(achievements)
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting goal achievements")
            emit(emptyList())
        }
    }.flowOn(dispatcher)

    /**
     * Get suggested goal adjustments
     *
     * @param userId The user ID
     * @return Flow of List<GoalAdjustmentSuggestion>
     */
    fun getSuggestedGoalAdjustments(userId: Long): Flow<List<GoalAdjustmentSuggestion>> = flow {
        try {
            // Get active goals
            val activeGoals = healthGoalRepository.getGoalsByStatus(userId, GoalStatus.ACTIVE).first()
            
            // Get goals that need adjustment
            val adjustmentSuggestions = mutableListOf<GoalAdjustmentSuggestion>()
            
            for (goal in activeGoals) {
                // Skip goals that are too new
                val daysSinceCreation = ChronoUnit.DAYS.between(goal.createdAt.toLocalDate(), LocalDate.now())
                if (daysSinceCreation < GOAL_ADJUSTMENT_PERIOD_DAYS) {
                    continue
                }
                
                // Get goal progress history
                val progressHistory = healthGoalRepository.getGoalProgressHistory(goal.id, 14).first()
                
                if (progressHistory.size < 7) {
                    // Not enough history to make adjustment
                    continue
                }
                
                // Calculate average progress
                val avgProgress = progressHistory.map { it.progress }.average()
                
                // Determine if adjustment is needed
                when {
                    avgProgress > GOAL_ADJUSTMENT_THRESHOLD_HIGH -> {
                        // Goal is too easy, suggest increase
                        val newTarget = (goal.targetValue * GOAL_INCREASE_PERCENTAGE).roundToInt().toDouble()
                        
                        adjustmentSuggestions.add(
                            GoalAdjustmentSuggestion(
                                goalId = goal.id,
                                goalTitle = goal.title,
                                goalType = goal.type,
                                currentTarget = goal.targetValue,
                                suggestedTarget = newTarget,
                                adjustmentReason = AdjustmentReason.TOO_EASY,
                                message = "You've been consistently achieving this goal. " +
                                        "Consider increasing your target to ${newTarget.toInt()} ${goal.unit} " +
                                        "to continue challenging yourself."
                            )
                        )
                    }
                    avgProgress < GOAL_ADJUSTMENT_THRESHOLD_LOW -> {
                        // Goal is too difficult, suggest decrease
                        val newTarget = (goal.targetValue * GOAL_DECREASE_PERCENTAGE).roundToInt().toDouble()
                        
                        adjustmentSuggestions.add(
                            GoalAdjustmentSuggestion(
                                goalId = goal.id,
                                goalTitle = goal.title,
                                goalType = goal.type,
                                currentTarget = goal.targetValue,
                                suggestedTarget = newTarget,
                                adjustmentReason = AdjustmentReason.TOO_DIFFICULT,
                                message = "You've been finding this goal challenging. " +
                                        "Consider adjusting your target to ${newTarget.toInt()} ${goal.unit} " +
                                        "to make it more achievable while still working toward improvement."
                            )
                        )
                    }
                }
            }
            
            emit(adjustmentSuggestions)
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting goal adjustment suggestions")
            emit(emptyList())
        }
    }.flowOn(dispatcher)

    /**
     * Apply a goal adjustment
     *
     * @param adjustment The adjustment to apply
     * @return Flow of GoalOperationResult
     */
    fun applyGoalAdjustment(adjustment: GoalAdjustmentSuggestion): Flow<GoalOperationResult> = flow {
        emit(GoalOperationResult.Loading)
        
        try {
            // Get the goal
            val goal = healthGoalRepository.getGoal(adjustment.goalId).firstOrNull()
            if (goal == null) {
                emit(GoalOperationResult.Error(GoalError.GoalNotFoundError("Goal not found: ${adjustment.goalId}")))
                return@flow
            }
            
            // Update goal with new target
            val updatedGoal = goal.copy(
                targetValue = adjustment.suggestedTarget,
                updatedAt = LocalDateTime.now(),
                additionalData = goal.additionalData + ("lastAdjusted" to LocalDate.now().toString())
            )
            
            // Update in repository
            val result = healthGoalRepository.updateGoal(updatedGoal)
            
            // Add adjustment to history
            healthGoalRepository.addGoalProgressHistory(
                GoalProgressHistory(
                    id = 0, // Will be assigned by repository
                    goalId = adjustment.goalId,
                    date = LocalDate.now(),
                    progress = goal.progress,
                    value = goal.progress * adjustment.suggestedTarget / 100,
                    note = "Goal adjusted from ${goal.targetValue.toInt()} to ${adjustment.suggestedTarget.toInt()} " +
                            "due to ${adjustment.adjustmentReason.toString().lowercase().replace('_', ' ')}"
                )
            )
            
            emit(GoalOperationResult.Success(result))
            
        } catch (e: Exception) {
            Timber.e(e, "Error applying goal adjustment")
            emit(GoalOperationResult.Error(
                GoalError.RepositoryError("Failed to apply goal adjustment: ${e.message}")
            ))
        }
    }.flowOn(dispatcher)

    /**
     * Calculate goal progress
     *
     * @param goal The goal to calculate progress for
     * @return Pair of (progress percentage, isAchieved)
     */
    private suspend fun calculateGoalProgress(goal: HealthGoal): Pair<Double, Boolean> {
        return withContext(dispatcher) {
            try {
                // Get current date
                val today = LocalDate.now()
                
                // Check if goal is still active
                if (goal.endDate != null && goal.endDate.isBefore(today)) {
                    return@withContext Pair(goal.progress, false) // Goal period has ended
                }
                
                // Calculate progress based on goal type
                when (goal.type) {
                    GoalType.STEPS -> calculateStepsProgress(goal)
                    GoalType.SLEEP_DURATION -> calculateSleepProgress(goal)
                    GoalType.ACTIVITY_MINUTES -> calculateActivityProgress(goal)
                    GoalType.HEART_RATE_ZONE -> calculateHeartRateZoneProgress(goal)
                    GoalType.WEIGHT -> calculateWeightProgress(goal)
                    GoalType.BLOOD_PRESSURE -> calculateBloodPressureProgress(goal)
                    GoalType.WATER_INTAKE -> calculateWaterIntakeProgress(goal)
                    GoalType.CUSTOM -> calculateCustomProgress(goal)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error calculating goal progress for goal ${goal.id}")
                Pair(goal.progress, false) // Return existing progress on error
            }
        }
    }

    /**
     * Calculate progress for steps goal
     */
    private suspend fun calculateStepsProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // Get relevant date range based on frequency
        val (startDate, endDate) = getDateRangeForFrequency(goal.frequency)
        
        // Get step data for the period
        val stepData = healthDataRepository.getStepRecords(
            userId = goal.userId,
            startTime = startDate.atStartOfDay(),
            endTime = endDate.plusDays(1).atStartOfDay()
        ).first()
        
        if (stepData.isEmpty()) {
            return Pair(0.0, false)
        }
        
        // Calculate progress based on frequency
        return when (goal.frequency) {
            GoalFrequency.DAILY -> {
                // Get today's steps
                val todaySteps = stepData
                    .filter { it.date == LocalDate.now() }
                    .sumOf { it.count }
                
                val progress = min(100.0, (todaySteps.toDouble() / goal.targetValue) * 100)
                val isAchieved = todaySteps >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
            GoalFrequency.WEEKLY -> {
                // Get this week's steps
                val weeklySteps = stepData.sumOf { it.count }
                
                val progress = min(100.0, (weeklySteps.toDouble() / goal.targetValue) * 100)
                val isAchieved = weeklySteps >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
            else -> {
                // For other frequencies, use average daily steps
                val avgDailySteps = stepData
                    .groupBy { it.date }
                    .map { (_, records) -> records.sumOf { it.count } }
                    .average()
                
                val progress = min(100.0, (avgDailySteps / goal.targetValue) * 100)
                val isAchieved = avgDailySteps >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
        }
    }

    /**
     * Calculate progress for sleep goal
     */
    private suspend fun calculateSleepProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // Get relevant date range based on frequency
        val (startDate, endDate) = getDateRangeForFrequency(goal.frequency)
        
        // Get sleep data for the period
        val sleepData = healthDataRepository.getSleepRecords(
            userId = goal.userId,
            startTime = startDate.atStartOfDay(),
            endTime = endDate.plusDays(1).atStartOfDay()
        ).first()
        
        if (sleepData.isEmpty()) {
            return Pair(0.0, false)
        }
        
        // Calculate progress based on frequency
        return when (goal.frequency) {
            GoalFrequency.DAILY -> {
                // Get last night's sleep
                val lastSleep = sleepData
                    .filter { it.startTime.toLocalDate() == LocalDate.now().minusDays(1) }
                    .maxByOrNull { it.durationMinutes }
                
                if (lastSleep != null) {
                    val progress = min(100.0, (lastSleep.durationMinutes.toDouble() / goal.targetValue) * 100)
                    val isAchieved = lastSleep.durationMinutes >= goal.targetValue
                    
                    Pair(progress, isAchieved)
                } else {
                    Pair(0.0, false)
                }
            }
            GoalFrequency.WEEKLY -> {
                // Calculate average sleep duration for the week
                val avgSleepDuration = sleepData
                    .groupBy { it.startTime.toLocalDate() }
                    .map { (_, records) -> records.maxByOrNull { it.durationMinutes }?.durationMinutes ?: 0 }
                    .average()
                
                val progress = min(100.0, (avgSleepDuration / goal.targetValue) * 100)
                val isAchieved = avgSleepDuration >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
            else -> {
                // For other frequencies, use average sleep duration
                val avgSleepDuration = sleepData
                    .groupBy { it.startTime.toLocalDate() }
                    .map { (_, records) -> records.maxByOrNull { it.durationMinutes }?.durationMinutes ?: 0 }
                    .average()
                
                val progress = min(100.0, (avgSleepDuration / goal.targetValue) * 100)
                val isAchieved = avgSleepDuration >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
        }
    }

    /**
     * Calculate progress for activity minutes goal
     */
    private suspend fun calculateActivityProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // Get relevant date range based on frequency
        val (startDate, endDate) = getDateRangeForFrequency(goal.frequency)
        
        // Get activity data for the period
        val activityData = healthDataRepository.getActivityRecords(
            userId = goal.userId,
            startTime = startDate.atStartOfDay(),
            endTime = endDate.plusDays(1).atStartOfDay()
        ).first()
        
        if (activityData.isEmpty()) {
            return Pair(0.0, false)
        }
        
        // Calculate progress based on frequency
        return when (goal.frequency) {
            GoalFrequency.DAILY -> {
                // Get today's activity minutes
                val todayActivities = activityData
                    .filter { it.startTime.toLocalDate() == LocalDate.now() }
                    .sumOf { it.durationMinutes }
                
                val progress = min(100.0, (todayActivities.toDouble() / goal.targetValue) * 100)
                val isAchieved = todayActivities >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
            GoalFrequency.WEEKLY -> {
                // Get this week's activity minutes
                val weeklyActivities = activityData.sumOf { it.durationMinutes }
                
                val progress = min(100.0, (weeklyActivities.toDouble() / goal.targetValue) * 100)
                val isAchieved = weeklyActivities >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
            else -> {
                // For other frequencies, use total activity minutes
                val totalActivities = activityData.sumOf { it.durationMinutes }
                
                val progress = min(100.0, (totalActivities.toDouble() / goal.targetValue) * 100)
                val isAchieved = totalActivities >= goal.targetValue
                
                Pair(progress, isAchieved)
            }
        }
    }

    /**
     * Calculate progress for heart rate zone goal
     */
    private suspend fun calculateHeartRateZoneProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // Get relevant date range based on frequency
        val (startDate, endDate) = getDateRangeForFrequency(goal.frequency)
        
        // Get heart rate data for the period
        val heartRateData = healthDataRepository.getHeartRateMeasurements(
            userId = goal.userId,
            startTime = startDate.atStartOfDay(),
            endTime = endDate.plusDays(1).atStartOfDay()
        ).first()
        
        if (heartRateData.isEmpty()) {
            return Pair(0.0, false)
        }
        
        // Get zone bounds from goal
        val lowerBound = goal.additionalData?.get("lowerBound")?.toString()?.toIntOrNull() ?: 0
        val upperBound = goal.additionalData?.get("upperBound")?.toString()?.toIntOrNull() ?: 0
        
        if (lowerBound == 0 || upperBound == 0) {
            return Pair(0.0, false)
        }
        
        // Get activity data to correlate with heart rate
        val activityData = healthDataRepository.getActivityRecords(
            userId = goal.userId,
            startTime = startDate.atStartOfDay(),
            endTime = endDate.plusDays(1).atStartOfDay()
        ).first()
        
        // Calculate minutes in zone during activities
        var minutesInZone = 0
        
        for (activity in activityData) {
            // Get heart rate measurements during this activity
            val activityHeartRates = heartRateData.filter { 
                it.timestamp >= activity.startTime && 
                it.timestamp <= activity.startTime.plusMinutes(activity.durationMinutes.toLong()) 
            }
            
            // Count measurements in target zone
            val measurementsInZone = activityHeartRates.count { 
                it.value in lowerBound..upperBound 
            }
            
            // Estimate minutes in zone (assuming measurements are taken regularly)
            if (activityHeartRates.isNotEmpty()) {
                val ratioInZone = measurementsInZone.toDouble() / activityHeartRates.size
                minutesInZone += (ratioInZone * activity.durationMinutes).roundToInt()
            }
        }
        
        // Calculate progress
        val progress = min(100.0, (minutesInZone.toDouble() / goal.targetValue) * 100)
        val isAchieved = minutesInZone >= goal.targetValue
        
        return Pair(progress, isAchieved)
    }

    /**
     * Calculate progress for weight goal
     */
    private suspend fun calculateWeightProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // For weight goals, we need to know the starting weight and target weight
        val startWeight = goal.additionalData?.get("startWeight")?.toString()?.toDoubleOrNull() ?: return Pair(0.0, false)
        val targetWeight = goal.targetValue
        
        // Get latest weight measurement
        val latestWeight = goal.additionalData?.get("currentWeight")?.toString()?.toDoubleOrNull() ?: startWeight
        
        // Calculate progress based on whether it's weight loss or gain
        val isWeightLoss = targetWeight < startWeight
        
        return if (isWeightLoss) {
            // Weight loss goal
            if (latestWeight <= targetWeight) {
                // Goal achieved
                Pair(100.0, true)
            } else {
                // Calculate progress
                val totalToLose = startWeight - targetWeight
                val lostSoFar = startWeight - latestWeight
                val progress = min(100.0, (lostSoFar / totalToLose) * 100)
                
                Pair(progress, false)
            }
        } else {
            // Weight gain goal
            if (latestWeight >= targetWeight) {
                // Goal achieved
                Pair(100.0, true)
            } else {
                // Calculate progress
                val totalToGain = targetWeight - startWeight
                val gainedSoFar = latestWeight - startWeight
                val progress = min(100.0, (gainedSoFar / totalToGain) * 100)
                
                Pair(progress, false)
            }
        }
    }

    /**
     * Calculate progress for blood pressure goal
     */
    private suspend fun calculateBloodPressureProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // Get relevant date range based on frequency
        val (startDate, endDate) = getDateRangeForFrequency(goal.frequency)
        
        // Get blood pressure data for the period
        val bpData = healthDataRepository.getBloodPressureMeasurements(
            userId = goal.userId,
            startTime = startDate.atStartOfDay(),
            endTime = endDate.plusDays(1).atStartOfDay()
        ).first()
        
        if (bpData.isEmpty()) {
            return Pair(0.0, false)
        }
        
        // Get target values from goal
        val targetSystolic = goal.additionalData?.get("targetSystolic")?.toString()?.toIntOrNull() ?: 120
        val targetDiastolic = goal.additionalData?.get("targetDiastolic")?.toString()?.toIntOrNull() ?: 80
        
        // Calculate percentage of readings within target
        val readingsWithinTarget = bpData.count { 
            it.systolic <= targetSystolic && it.diastolic <= targetDiastolic 
        }
        
        val percentWithinTarget = (readingsWithinTarget.toDouble() / bpData.size) * 100
        
        // For blood pressure, we consider the goal achieved if 80% of readings are within target
        val isAchieved = percentWithinTarget >= 80
        
        return Pair(percentWithinTarget, isAchieved)
    }

    /**
     * Calculate progress for water intake goal
     */
    private suspend fun calculateWaterIntakeProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // Water intake would typically come from manual entries or a specialized repository
        // For this example, we'll use the additionalData field to track water intake
        
        // Get today's date
        val today = LocalDate.now()
        
        // Get current water intake from additionalData
        val currentIntake = goal.additionalData?.get("intake_$today")?.toString()?.toDoubleOrNull() ?: 0.0
        
        // Calculate progress
        val progress = min(100.0, (currentIntake / goal.targetValue) * 100)
        val isAchieved = currentIntake >= goal.targetValue
        
        return Pair(progress, isAchieved)
    }

    /**
     * Calculate progress for custom goal
     */
    private suspend fun calculateCustomProgress(goal: HealthGoal): Pair<Double, Boolean> {
        // Custom goals rely on manual progress updates
        // We'll just return the current progress
        return Pair(goal.progress, goal.progress >= 100)
    }

    /**
     * Update a goal with new progress
     *
     * @param goal The goal to update
     * @param progress The new progress value
     * @param isAchieved Whether the goal has been achieved
     * @return The updated goal
     */
    private suspend fun updateGoalWithProgress(
        goal: HealthGoal,
        progress: Double,
        isAchieved: Boolean
    ): HealthGoal {
        // Prepare updated goal
        val today = LocalDate.now()
        val wasAchievedBefore = goal.status == GoalStatus.ACHIEVED
        
        val updatedGoal = goal.copy(
            progress = progress,
            status = when {
                isAchieved -> GoalStatus.ACHIEVED
                goal.endDate != null && goal.endDate.isBefore(today) -> GoalStatus.COMPLETED
                else -> GoalStatus.ACTIVE
            },
            updatedAt = LocalDateTime.now(),
            lastAchievedDate = if (isAchieved) today else goal.lastAchievedDate,
            streakDays = calculateStreakDays(goal, isAchieved, today)
        )
        
        // Update goal in repository
        val result = healthGoalRepository.updateGoal(updatedGoal)
        
        // Add to goal history if progress has changed significantly
        if (abs(goal.progress - progress) >= 5.0 || isAchieved != wasAchievedBefore) {
            healthGoalRepository.addGoalProgressHistory(
                GoalProgressHistory(
                    id = 0, // Will be assigned by repository
                    goalId = goal.id,
                    date = today,
                    progress = progress,
                    value = progress * goal.targetValue / 100,
                    note = if (isAchieved && !wasAchievedBefore) "Goal achieved" else null
                )
            )
        }
        
        return result
    }

    /**
     * Calculate streak days for a goal
     *
     * @param goal The goal
     * @param isAchievedToday Whether the goal is achieved today
     * @param today Today's date
     * @return The updated streak days
     */
    private fun calculateStreakDays(
        goal: HealthGoal,
        isAchievedToday: Boolean,
        today: LocalDate
    ): Int {
        if (!isAchievedToday) {
            return 0 // Reset streak if not achieved today
        }
        
        val lastAchievedDate = goal.lastAchievedDate
        
        return if (lastAchievedDate == null) {
            // First achievement
            1
        } else {
            val daysBetween = ChronoUnit.DAYS.between(lastAchievedDate, today)
            
            when {
                daysBetween == 1L -> goal.streakDays + 1 // Consecutive day
                daysBetween == 0L -> goal.streakDays // Same day, don't increment
                else -> 1 // Streak broken, start new streak
            }
        }
    }

    /**
     * Process goal achievements
     *
     * @param achievedGoals List of newly achieved goals
     */
    private suspend fun processGoalAchievements(achievedGoals: List<HealthGoal>) {
        // In a real implementation, this would:
        // 1. Generate notifications
        // 2. Award points/badges
        // 3. Update user statistics
        // 4. Trigger celebration animations
        
        for (goal in achievedGoals) {
            Timber.d("Goal achieved: ${goal.title} (ID: ${goal.id})")
            
            // Check for milestone streaks
            if (isMilestoneStreak(goal.streakDays)) {
                Timber.d("Milestone streak achieved: ${goal.streakDays} days for goal ${goal.title}")
                // Would trigger special milestone celebration
            }
        }
    }

    /**
     * Check for goals that need adjustment
     *
     * @param userId The user ID
     */
    private suspend fun checkForGoalAdjustments(userId: Long) {
        try {
            // Get adjustment suggestions
            val adjustmentSuggestions = getSuggestedGoalAdjustments(userId).first()
            
            // Log suggestions for now (in a real app, these would be shown to the user)
            for (suggestion in adjustmentSuggestions) {
                Timber.d("Goal adjustment suggested: ${suggestion.message}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking for goal adjustments")
        }
    }

    /**
     * Validate a health goal
     *
     * @param goal The goal to validate
     * @return GoalValidationResult
     */
    private fun validateGoal(goal: HealthGoal): GoalValidationResult {
        val validationErrors = mutableListOf<String>()
        
        // Check required fields
        if (goal.title.isBlank()) {
            validationErrors.add("Title is required")
        }
        
        if (goal.targetValue <= 0) {
            validationErrors.add("Target value must be greater than zero")
        }
        
        if (goal.unit.isBlank()) {
            validationErrors.add("Unit is required")
        }
        
        // Validate date range
        if (goal.endDate != null && goal.startDate.isAfter(goal.endDate)) {
            validationErrors.add("End date must be after start date")
        }
        
        // Validate type-specific requirements
        when (goal.type) {
            GoalType.STEPS -> {
                if (goal.targetValue > 50000) {
                    validationErrors.add("Step goal should be realistic (max 50,000)")
                }
            }
            GoalType.SLEEP_DURATION -> {
                if (goal.targetValue > 12 * 60) {
                    validationErrors.add("Sleep goal should be realistic (max 12 hours)")
                }
            }
            GoalType.WEIGHT -> {
                val startWeight = goal.additionalData?.get("startWeight")?.toString()?.toDoubleOrNull()
                if (startWeight == null) {
                    validationErrors.add("Starting weight is required for weight goals")
                }
            }
            GoalType.BLOOD_PRESSURE -> {
                val targetSystolic = goal.additionalData?.get("targetSystolic")?.toString()?.toIntOrNull()
                val targetDiastolic = goal.additionalData?.get("targetDiastolic")?.toString()?.toIntOrNull()
                
                if (targetSystolic == null || targetDiastolic == null) {
                    validationErrors.add("Target systolic and diastolic values are required for blood pressure goals")
                }
            }
            GoalType.HEART_RATE_ZONE -> {
                val lowerBound = goal.additionalData?.get("lowerBound")?.toString()?.toIntOrNull()
                val upperBound = goal.additionalData?.get("upperBound")?.toString()?.toIntOrNull()
                
                if (lowerBound == null || upperBound == null) {
                    validationErrors.add("Heart rate zone bounds are required")
                } else if (lowerBound >= upperBound) {
                    validationErrors.add("Upper bound must be greater than lower bound")
                }
            }
            else -> {
                // No specific validation for other types
            }
        }
        
        return if (validationErrors.isEmpty()) {
            GoalValidationResult.Valid
        } else {
            GoalValidationResult.Invalid(validationErrors)
        }
    }

    /**
     * Generate a reminder message for a goal
     *
     * @param goal The goal
     * @param progress Current progress
     * @return Reminder message
     */
    private fun generateReminderMessage(goal: HealthGoal, progress: Double): String {
        // Different messages based on progress
        return when {
            progress < 10 -> {
                // Just starting
                when (goal.type) {
                    GoalType.STEPS -> "Time to get moving! You've made little progress toward your step goal today."
                    GoalType.SLEEP_DURATION -> "Remember your sleep goal for tonight. A good night's rest is essential for health."
                    GoalType.ACTIVITY_MINUTES -> "Don't forget to get active today to reach your activity goal."
                    GoalType.HEART_RATE_ZONE -> "Plan a cardio workout today to reach your heart rate zone goal."
                    GoalType.WATER_INTAKE -> "Start hydrating! You've barely made progress on your water intake goal."
                    else -> "Remember your ${goal.title} goal today. You're just getting started!"
                }
            }
            progress < 50 -> {
                // Less than halfway
                when (goal.type) {
                    GoalType.STEPS -> "You're on your way! ${progress.roundToInt()}% toward your step goal."
                    GoalType.ACTIVITY_MINUTES -> "Keep moving! You're ${progress.roundToInt()}% toward your activity goal."
                    GoalType.WATER_INTAKE -> "Stay hydrated! You're ${progress.roundToInt()}% toward your water intake goal."
                    else -> "You're making progress on your ${goal.title} goal. Keep it up!"
                }
            }
            progress < 80 -> {
                // More than halfway
                when (goal.type) {
                    GoalType.STEPS -> "You're making great progress! Just ${(goal.targetValue - (progress * goal.targetValue / 100)).roundToInt()} more steps to go."
                    GoalType.ACTIVITY_MINUTES -> "Almost there! Just ${(goal.targetValue - (progress * goal.targetValue / 100)).roundToInt()} more minutes of activity to reach your goal."
                    GoalType.WATER_INTAKE -> "Keep hydrating! Just ${(goal.targetValue - (progress * goal.targetValue / 100)).roundToInt()} ml more to reach your goal."
                    else -> "You're ${progress.roundToInt()}% of the way to your ${goal.title} goal. Keep going!"
                }
            }
            progress < 100 -> {
                // Almost there
                "You're so close to achieving your ${goal.title} goal! Just a little more effort to reach 100%."
            }
            else -> {
                // Goal achieved
                "Congratulations! You've achieved your ${goal.title} goal. Keep up the great work!"
            }
        }
    }

    /**
     * Generate an achievement message for a goal
     *
     * @param goal The achieved goal
     * @return Achievement message
     */
    private fun generateAchievementMessage(goal: HealthGoal): String {
        val baseMessage = "Congratulations! You've achieved your ${goal.title} goal"
        
        val streakMessage = when {
            goal.streakDays >= STREAK_MILESTONE_LARGE -> " with an impressive ${goal.streakDays}-day streak!"
            goal.streakDays >= STREAK_MILESTONE_MEDIUM -> " with an excellent ${goal.streakDays}-day streak!"
            goal.streakDays >= STREAK_MILESTONE_SMALL -> " with a solid ${goal.streakDays}-day streak!"
            goal.streakDays > 1 -> " for ${goal.streakDays} days in a row!"
            else -> "!"
        }
        
        val typeSpecificMessage = when (goal.type) {
            GoalType.STEPS -> " You've taken ${goal.targetValue.toInt()} steps today."
            GoalType.SLEEP_DURATION -> " You've slept for ${(goal.targetValue / 60).toInt()} hours."
            GoalType.ACTIVITY_MINUTES -> " You've been active for ${goal.targetValue.toInt()} minutes."
            GoalType.HEART_RATE_ZONE -> " You've spent ${goal.targetValue.toInt()} minutes in your target heart rate zone."
            GoalType.WEIGHT -> " You've reached your target weight of ${goal.targetValue} ${goal.unit}."
            GoalType.WATER_INTAKE -> " You've consumed ${goal.targetValue.toInt()} ml of water."
            else -> ""
        }
        
        return baseMessage + streakMessage + typeSpecificMessage
    }

    /**
     * Calculate reward points for achieving a goal
     *
     * @param goal The achieved goal
     * @return Reward points
     */
    private fun calculateRewardPoints(goal: HealthGoal): Int {
        // Base points by goal type
        val basePoints = when (goal.type) {
            GoalType.STEPS -> 10
            GoalType.SLEEP_